package com.expressify.controller;

import com.expressify.entity.User;
import com.expressify.service.CustomUserDetailsService;
import com.expressify.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(UserService userService, CustomUserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping
    public String authPage(@RequestParam(name = "error", required = false) Boolean error,
            @RequestParam(name = "banned", required = false) Boolean banned,
            @RequestParam(name = "admin", required = false) Boolean admin,
            @RequestParam(name = "adminError", required = false) Boolean adminError,
            @RequestParam(name = "register", required = false) String register,
            Model model,
            Principal principal,
            jakarta.servlet.http.HttpSession session) {
        if (principal != null && !Boolean.TRUE.equals(admin) && !Boolean.TRUE.equals(adminError)) {
            return "redirect:/home";
        }
        model.addAttribute("error", Boolean.TRUE.equals(error));
        model.addAttribute("banned", Boolean.TRUE.equals(banned));
        model.addAttribute("admin", Boolean.TRUE.equals(admin));
        model.addAttribute("adminError", Boolean.TRUE.equals(adminError));
        model.addAttribute("openRegister",
                register != null && ("1".equals(register) || "true".equalsIgnoreCase(register)));
        return "auth";
    }

    @PostMapping("/register")
    public String register(@RequestParam(name = "username") String username,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "password") String password,
            RedirectAttributes ra) {
        try {
            userService.register(username, email, password);
            ra.addFlashAttribute("message", "Registration successful. Please login.");
            return "redirect:/auth";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth?register=true";
        }
    }
}
