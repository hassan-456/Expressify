package com.expressify.controller;

import com.expressify.entity.User;
import com.expressify.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private static final String PROFILE_UPLOAD_DIR = "uploads/profiles";

    private final UserService userService;

    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String settings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        model.addAttribute("currentUser", user);
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "light");
        return "settings";
    }

    @PostMapping("/account")
    public String updateAccount(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "username") String username,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "bio", required = false) String bio,
            RedirectAttributes ra) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        try {
            userService.updateProfile(user.getId(), username, email, bio != null ? bio : "");
            ra.addFlashAttribute("message", "Profile updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "current_password") String current_password,
            @RequestParam(name = "new_password") String new_password,
            @RequestParam(name = "confirm_password") String confirm_password,
            RedirectAttributes ra) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        if (!new_password.equals(confirm_password)) {
            ra.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/settings";
        }
        try {
            if (userService.changePassword(user.getId(), current_password, new_password)) {
                ra.addFlashAttribute("message", "Password updated successfully!");
            } else {
                ra.addFlashAttribute("errorMessage", "Current password is incorrect.");
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/privacy")
    public String updatePrivacy(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "private_account", required = false) Boolean privateAccount,
            @RequestParam(name = "show_activity", required = false) Boolean showActivity,
            RedirectAttributes ra) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        userService.updatePrivacy(user.getId(), Boolean.TRUE.equals(privateAccount), Boolean.TRUE.equals(showActivity));
        ra.addFlashAttribute("message", "Privacy settings updated.");
        return "redirect:/settings";
    }

    @PostMapping("/theme")
    public String updateTheme(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "theme") String theme,
            RedirectAttributes ra) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        userService.updateTheme(user.getId(), theme);
        ra.addFlashAttribute("message", "Theme updated successfully!");
        return "redirect:/settings";
    }

    @PostMapping("/profile-picture")
    public String updateProfilePicture(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "profile_picture") MultipartFile profile_picture,
            RedirectAttributes ra) {
        User user = userService.loadUserEntityByEmail(userDetails.getUsername());
        if (user == null)
            return "redirect:/auth";
        if (profile_picture.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Please select an image.");
            return "redirect:/settings";
        }
        try {
            String ext = profile_picture.getOriginalFilename();
            if (ext != null && ext.contains("."))
                ext = ext.substring(ext.lastIndexOf('.'));
            else
                ext = ".jpg";
            Path dir = Paths.get(PROFILE_UPLOAD_DIR);
            if (!Files.exists(dir))
                Files.createDirectories(dir);
            String fileName = "profile_" + user.getId() + ext;
            Path path = dir.resolve(fileName);
            Files.copy(profile_picture.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = PROFILE_UPLOAD_DIR + "/" + fileName;
            userService.updateProfilePicture(user.getId(), relativePath);
            ra.addFlashAttribute("message", "Profile picture updated!");
        } catch (IOException e) {
            ra.addFlashAttribute("errorMessage", "Failed to upload image.");
        }
        return "redirect:/settings";
    }
}
