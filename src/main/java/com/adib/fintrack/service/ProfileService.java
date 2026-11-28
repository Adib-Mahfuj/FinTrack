package com.adib.fintrack.service;

import com.adib.fintrack.dto.AuthDto;
import com.adib.fintrack.dto.ChangePasswordRequest;
import com.adib.fintrack.dto.ProfileDto;
import com.adib.fintrack.dto.UpdateProfileRequest;
import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.exception.ApiException;
import com.adib.fintrack.repository.ProfileRepository;
import com.adib.fintrack.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt only uses the first 72 bytes, so longer passwords are rejected up front
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final int MAX_IMAGE_URL_LENGTH = 255;

    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    // Minimum gap between two reset emails for the same account
    private static final Duration RESET_REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.activation.url}")
    private String activationURL;

    @Value("${finance.tracker.frontend.url}")
    private String frontendUrl;

    // Registers an inactive profile and emails the activation link
    public ProfileDto registerProfile(ProfileDto profileDto) {
        String fullName = requireFullName(profileDto.getFullName());
        String email = requireEmail(profileDto.getEmail());
        requireValidPassword(profileDto.getPassword());

        if (profileRepository.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        // Built field by field: client-supplied ids or flags must never reach the database
        ProfileEntity newProfile = ProfileEntity.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(profileDto.getPassword()))
                .profileImageUrl(normalizeImageUrl(profileDto.getProfileImageUrl()))
                .isActive(false)
                .activationToken(UUID.randomUUID().toString())
                .build();

        try {
            newProfile = profileRepository.save(newProfile);
        } catch (DataIntegrityViolationException e) {
            // Two simultaneous sign-ups with the same email: the unique constraint catches the second
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        // Send activation email
        String activationLink = activationURL + "/api/v1.0/activate?token=" + newProfile.getActivationToken();
        String subject = "Activate your FinTrack account";
        String body = "Click on the following link to activate your FinTrack account: " + activationLink;
        emailService.sendEmail(newProfile.getEmail(), subject, body);
        return toDto(newProfile);
    }

    // Kept for callers that still map a DTO to an entity; registration no longer uses it
    public ProfileEntity toEntity(ProfileDto profileDto) {
        return ProfileEntity.builder()
                .id(profileDto.getId())
                .fullName(profileDto.getFullName())
                .email(profileDto.getEmail())
                .password(passwordEncoder.encode(profileDto.getPassword()))
                .profileImageUrl(profileDto.getProfileImageUrl())
                .createdAt(profileDto.getCreatedAt())
                .updatedAt(profileDto.getUpdatedAt())
                .build();
    }

    public ProfileDto toDto(ProfileEntity profileEntity) {
        return ProfileDto.builder()
                .id(profileEntity.getId())
                .fullName(profileEntity.getFullName())
                .email(profileEntity.getEmail())
                .profileImageUrl(profileEntity.getProfileImageUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();
    }

    public boolean activateProfile(String activationToken) {
        return profileRepository.findByActivationToken(activationToken)
                .map(profile -> {
                    profile.setIsActive(true);
                    profileRepository.save(profile);
                    return true;
                })
                .orElse(false);
    }

    public boolean isAccountActive(String email) {
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    public ProfileEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + authentication.getName()));
    }

    public ProfileDto getPublicProfile(String email) {
        ProfileEntity currentUser;
        if (email == null) {
            currentUser = getCurrentProfile();
        } else {
            currentUser = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + email));
        }
        return toDto(currentUser);
    }

    // Verifies credentials, then the activation status, and issues a JWT
    public Map<String, Object> authenticateAndGenerateToken(AuthDto authDto) {
        if (authDto.getEmail() == null || authDto.getEmail().isBlank()
                || authDto.getPassword() == null || authDto.getPassword().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDto.getEmail(), authDto.getPassword()));
        } catch (AuthenticationException e) {
            // One message for unknown emails and wrong passwords, so accounts cannot be enumerated
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Only reached with correct credentials
        if (!isAccountActive(authDto.getEmail())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is not active. Please activate your account first.");
        }

        String token = jwtUtil.generateToken(authDto.getEmail());
        return Map.of(
                "token", token,
                "user", getPublicProfile(authDto.getEmail())
        );
    }

    // Updates the display name and photo of the current user (the email is immutable)
    public ProfileDto updateProfile(UpdateProfileRequest request) {
        ProfileEntity profile = getCurrentProfile();

        profile.setFullName(requireFullName(request.getFullName()));
        profile.setProfileImageUrl(normalizeImageUrl(request.getProfileImageUrl()));

        return toDto(profileRepository.save(profile));
    }

    // Changes the password after verifying the current one. Every older token stops working,
    // so a fresh token is returned to keep the current session signed in.
    public String changePassword(ChangePasswordRequest request) {
        ProfileEntity profile = getCurrentProfile();

        String currentPassword = request.getCurrentPassword();
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, profile.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        requireValidPassword(request.getNewPassword());
        if (request.getNewPassword().equals(currentPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }

        profile.setPassword(passwordEncoder.encode(request.getNewPassword()));
        profile.setPasswordChangedAt(LocalDateTime.now());
        profileRepository.save(profile);

        // Issued after passwordChangedAt, so it passes the filter's check
        return jwtUtil.generateToken(profile.getEmail());
    }

    // Emails a one-hour reset link. Unknown, inactive and rate-limited requests return silently,
    // so the response never reveals whether an account exists.
    public void requestPasswordReset(String rawEmail) {
        String email = requireEmail(rawEmail);

        ProfileEntity profile = profileRepository.findByEmail(email).orElse(null);
        if (profile == null || !Boolean.TRUE.equals(profile.getIsActive())) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = profile.getPasswordResetExpiresAt();
        if (expiresAt != null && expiresAt.minus(RESET_TOKEN_TTL).plus(RESET_REQUEST_COOLDOWN).isAfter(now)) {
            return;
        }

        String token = generateResetToken();
        profile.setPasswordResetToken(hashToken(token));
        profile.setPasswordResetExpiresAt(now.plus(RESET_TOKEN_TTL));
        profileRepository.save(profile);

        String resetLink = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        String subject = "Reset your FinTrack password";
        String body = """
                <p>Hello,</p>
                <p>We received a request to reset your FinTrack password. Use the link below to choose a new one. \
                The link expires in 1 hour.</p>
                <p><a href="%s">Reset your password</a></p>
                <p>If you did not request this, you can safely ignore this email.</p>
                """.formatted(resetLink);

        try {
            emailService.sendEmail(profile.getEmail(), subject, body);
        } catch (RuntimeException e) {
            // A mail failure must not reveal that the account exists
            log.error("Password reset email could not be sent to {}", profile.getEmail(), e);
        }
    }

    // Sets a new password for a valid, unexpired token and consumes the token
    public void resetPassword(String token, String newPassword) {
        ProfileEntity profile = (token == null || token.isBlank())
                ? null
                : profileRepository.findByPasswordResetToken(hashToken(token.trim())).orElse(null);

        if (profile == null
                || profile.getPasswordResetExpiresAt() == null
                || profile.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired");
        }

        requireValidPassword(newPassword);

        profile.setPassword(passwordEncoder.encode(newPassword));
        // Every JWT issued before this moment stops working
        profile.setPasswordChangedAt(LocalDateTime.now());
        profile.setPasswordResetToken(null);
        profile.setPasswordResetExpiresAt(null);
        profileRepository.save(profile);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    // Validation helpers
    private String requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        String trimmed = fullName.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private String requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        String trimmed = email.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        }
        return trimmed;
    }

    private void requireValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is too long");
        }
    }

    // Blank means "no photo"; anything else must be a secure URL
    private String normalizeImageUrl(String url) {
        if (url == null || url.isBlank()) return null;

        String trimmed = url.trim();
        if (!trimmed.startsWith("https://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image must be a secure (https) URL");
        }
        if (trimmed.length() > MAX_IMAGE_URL_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image URL is too long");
        }
        return trimmed;
    }
}