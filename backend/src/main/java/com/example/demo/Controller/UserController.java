package com.example.demo.Controller;


import com.example.demo.DTOs.LocationDTOs.ManagedLocationDTO;
import com.example.demo.DTOs.ReviewDTOs.UserReviewDTO;
import com.example.demo.DTOs.UserDTOs.ChangePasswordRequest;
import com.example.demo.DTOs.UserDTOs.UserDTO;
import com.example.demo.DTOs.UserDTOs.UserUpdateRequestDTO;
import com.example.demo.Model.User;
import com.example.demo.Services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final IUserService userService;

    @Autowired
    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(Principal principal) {
        logger.info("Fetching profile for user: {}", principal.getName());
        UserDTO userDTO = userService.getUserProfile(principal.getName());
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody UserUpdateRequestDTO request, Principal principal) {
        Map<String, String> response = new HashMap<>();
        String userEmail = principal.getName();
        try {
            userService.updateProfile(userEmail, request);
            response.put("message", "Profil uspešno ažuriran!");
            logger.info("User profile successfully updated: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Ažuriranje profila nije uspelo: " + e.getMessage());
            logger.error("Failed to update profile for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<UserReviewDTO>> getUserReviews(Principal principal) {
        logger.info("Fetching reviews for user: {}", principal.getName());
        List<UserReviewDTO> reviews = userService.getUserReviews(principal.getName());
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/managed-locations")
    public ResponseEntity<List<ManagedLocationDTO>> getManagedLocations(Principal principal) {
        logger.info("Fetching managed locations for user: {}", principal.getName());
        List<ManagedLocationDTO> locations = userService.getManagedLocations(principal.getName());
        return ResponseEntity.ok(locations);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        Map<String, String> response = new HashMap<>();
        String userEmail = principal.getName();
        try {
            userService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
            response.put("message", "Lozinka uspešno promenjena!");
            logger.info("Password successfully changed for user: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("message", e.getMessage());
            logger.warn("Password change failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile/image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestPart("file") MultipartFile file,
            Principal principal)
    {
        Map<String, String> response = new HashMap<>();
        String userEmail = principal.getName();
        try {
            userService.uploadProfileImage(userEmail, file);
            response.put("message", "Profilna slika uspešno ažurirana!");
            logger.info("Profile image successfully uploaded/updated for user: {}", userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Greška pri uploadu slike: " + e.getMessage());
            logger.error("Failed to upload profile image for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}