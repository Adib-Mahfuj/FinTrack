package com.adib.fintrack.controller;

import com.adib.fintrack.dto.AuthDto;
import com.adib.fintrack.dto.ChangePasswordRequest;
import com.adib.fintrack.dto.ProfileDto;
import com.adib.fintrack.dto.UpdateProfileRequest;
import com.adib.fintrack.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<ProfileDto> registerProfile(@RequestBody ProfileDto profileDto) {
        ProfileDto registeredProfile = profileService.registerProfile(profileDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateProfile(@RequestParam String token) {
        boolean isActivated = profileService.activateProfile(token);
        if (isActivated) {
            return ResponseEntity.status(HttpStatus.OK).body("Profile activated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid activation Token");
        }
    }

    // Failures (401 bad credentials, 403 inactive account) are returned as JSON by GlobalExceptionHandler
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthDto authDto) {
        return ResponseEntity.ok(profileService.authenticateAndGenerateToken(authDto));
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> getPublicProfile() {
        ProfileDto profileDto = profileService.getPublicProfile(null);
        return ResponseEntity.ok(profileDto);
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileDto> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}