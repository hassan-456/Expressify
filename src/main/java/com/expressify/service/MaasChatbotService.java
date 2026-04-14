package com.expressify.service;

import com.expressify.dto.ChatMessageDto;
import com.expressify.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class MaasChatbotService {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${maas.gemini.api-key}")
    private String geminiApiKey;

    // List of Gemini models to try (in order of preference)
    private static final String[] GEMINI_MODELS = {
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",
            "gemini-1.5-pro"
    };

    public MaasChatbotService(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    public void processMessage(User sender, Long maasUserId, String messageText) {
        try {
            // Small delay so the user's own message renders first
            Thread.sleep(500);

            String response = generateResponse(messageText, sender.getUsername());

            // Save the AI reply to database
            ChatMessageDto aiReplyDto = chatService.sendMessage(maasUserId, sender.getId(), response);

            // Send back via WebSocket
            messagingTemplate.convertAndSendToUser(
                    sender.getEmail(),
                    "/queue/messages",
                    aiReplyDto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateResponse(String input, String username) {
        String lowerInput = input.toLowerCase().trim();

        // Quick conversational short-circuits (no API call needed)
        if (lowerInput.equals("hi") || lowerInput.equals("hello") || lowerInput.equals("hey")) {
            return "Hello, " + username + "! I'm MAAS, your AI assistant powered by Google Gemini. Ask me anything!";
        }

        // Try Gemini API with multiple model fallbacks
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            for (String model : GEMINI_MODELS) {
                String geminiResponse = callGeminiApi(input, model);
                if (geminiResponse != null && !geminiResponse.isEmpty()) {
                    return geminiResponse;
                }
            }
        }

        // Fallback to Wikipedia API (always free, no key needed)
        String wikiResponse = callWikipediaApi(input);
        if (wikiResponse != null && !wikiResponse.isEmpty()) {
            return wikiResponse;
        }

        // Final fallback
        return getFallbackResponse(input, username);
    }

    private String callGeminiApi(String userMessage, String model) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model
                    + ":generateContent?key=" + geminiApiKey;

            // Build JSON safely (avoid broken JSON from string concatenation)
            ObjectNode root = objectMapper.createObjectNode();

            // Prompt strategy: keep instructions stable + pass user message cleanly
            String prompt = "You are MAAS, a friendly AI assistant inside the Expressify social media app.\n"
                    + "Answer the user's question with real-world, helpful information.\n"
                    + "Be concise but complete (typically 4-8 sentences, use bullet points if useful).\n"
                    + "If the user asks for steps, provide a short ordered list.\n"
                    + "If you are uncertain, say so and provide the best next steps.\n\n"
                    + "User message: " + userMessage;

            ArrayNode contents = root.putArray("contents");
            ObjectNode content0 = contents.addObject();
            ArrayNode parts = content0.putArray("parts");
            parts.addObject().put("text", prompt);

            ObjectNode generationConfig = root.putObject("generationConfig");
            generationConfig.put("temperature", 0.6);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 512);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(root), headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode body = response.getBody();
                if (body.has("candidates")) {
                    JsonNode candidates = body.get("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        JsonNode candidate0 = candidates.get(0);
                        JsonNode content = candidate0.get("content");
                        if (content != null && content.has("parts")) {
                            JsonNode respParts = content.get("parts");
                            if (respParts.isArray() && respParts.size() > 0 && respParts.get(0).has("text")) {
                                String text = respParts.get(0).get("text").asText().trim();
                                if (!text.isEmpty()) {
                                    System.out.println("MAAS: Gemini (" + model + ") responded successfully.");
                                    return text;
                                }
                            }
                        }

                        // Some responses put text in other shapes; do a best-effort scan
                        if (candidate0.has("output")) {
                            String text = candidate0.get("output").asText("").trim();
                            if (!text.isEmpty()) {
                                System.out.println("MAAS: Gemini (" + model + ") responded successfully (output).");
                                return text;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini API (" + model + ") failed: " + e.getMessage());
        }
        return null;
    }

    private String callWikipediaApi(String input) {
        try {
            // Extract the query subject
            String query = input;
            String lower = input.toLowerCase();
            if (lower.startsWith("what is "))
                query = input.substring(8).replace("?", "").trim();
            else if (lower.startsWith("what are "))
                query = input.substring(9).replace("?", "").trim();
            else if (lower.startsWith("who is "))
                query = input.substring(7).replace("?", "").trim();
            else if (lower.startsWith("who was "))
                query = input.substring(8).replace("?", "").trim();
            else if (lower.startsWith("tell me about "))
                query = input.substring(14).replace("?", "").trim();
            else if (lower.startsWith("define "))
                query = input.substring(7).replace("?", "").trim();

            // Capitalize first letter for better Wikipedia matching
            if (!query.isEmpty()) {
                query = query.substring(0, 1).toUpperCase() + query.substring(1);
            }

            String url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts&exintro=1&explaintext=1&redirects=1&titles="
                    + java.net.URLEncoder.encode(query, "UTF-8");

            // Wikipedia requires a User-Agent header, otherwise returns 403
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "ExpressifyApp/1.0 (MAAS Chatbot; educational project)");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String jsonStr = response.getBody();
            if (jsonStr != null && !jsonStr.isEmpty()) {
                JsonNode root = objectMapper.readTree(jsonStr);

                if (root.has("query") && root.get("query").has("pages")) {
                    JsonNode pages = root.get("query").get("pages");
                    java.util.Iterator<JsonNode> it = pages.elements();
                    while (it.hasNext()) {
                        JsonNode page = it.next();
                        if (page.has("missing"))
                            continue;
                        if (page.has("extract")) {
                            String extract = page.get("extract").asText().trim();
                            if (!extract.isEmpty()) {
                                System.out.println("MAAS: Wikipedia responded for: " + query);
                                String[] sentences = extract.split("(?<=[.!?])\\s+");
                                if (sentences.length > 2) {
                                    return sentences[0] + " " + sentences[1];
                                }
                                return extract;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Wikipedia API failed: " + e.getMessage());
        }
        return null;
    }

    private String getFallbackResponse(String input, String username) {
        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("joke")) {
            List<String> jokes = Arrays.asList(
                    "Why do programmers prefer dark mode? Because light attracts bugs! 😄",
                    "How many programmers does it take to change a light bulb? None, that's a hardware problem! 💡",
                    "Why did the database administrator leave his wife? She had one-to-many relationships! 😂");
            return jokes.get(random.nextInt(jokes.size()));
        }
        if (lowerInput.contains("who are you") || lowerInput.contains("maas")) {
            return "I am MAAS (My AI Assistant System), your AI buddy inside Expressify! I use Google Gemini for smart answers and Wikipedia for backup knowledge. Ask me anything!";
        }

        List<String> defaultResponses = Arrays.asList(
                "That's an interesting question! Could you rephrase it so I can search for an answer?",
                "I couldn't find specific information about that. Try asking in a different way!",
                "Hmm, I'm not sure about that one. Try asking me 'What is [topic]?' for best results!");
        return defaultResponses.get(random.nextInt(defaultResponses.size()));
    }
}
