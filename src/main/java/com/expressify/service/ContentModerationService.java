package com.expressify.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class ContentModerationService {

    private final Set<String> bannedWords = new HashSet<>();

    public ContentModerationService() {
        loadBannedWords();
    }

    private void loadBannedWords() {
        ClassPathResource resource = new ClassPathResource("moderation/banned-words.txt");
        if (!resource.exists()) {
            // Fallback to a small default list if file missing
            bannedWords.add("idiot");
            bannedWords.add("bastard");
            bannedWords.add("fool");
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    bannedWords.add(trimmed);
                }
            }
        } catch (IOException e) {
            // On error, fall back to a minimal in-memory list
            bannedWords.clear();
            bannedWords.add("idiot");
            bannedWords.add("bastard");
            bannedWords.add("fool");
        }
    }

    public boolean violatesGuidelines(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        for (String word : bannedWords) {
            if (normalized.contains(word)) {
                return true;
            }
        }
        return false;
    }
}

