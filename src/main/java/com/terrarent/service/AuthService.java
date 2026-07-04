package com.terrarent.service;

import com.terrarent.dto.auth.AuthResponse;
import com.terrarent.dto.auth.LoginRequest;
import com.terrarent.dto.auth.RegisterRequest;
import com.terrarent.dto.auth.ResendVerificationRequest;
import com.terrarent.dto.auth.VerifyEmailRequest;
import com.terrarent.dto.user.UserResponse;
import com.terrarent.entity.Role;
import com.terrarent.entity.User;
import com.terrarent.security.TerraRentUserDetails;
import com.terrarent.exception.CustomAuthenticationException;
import com.terrarent.exception.ResourceNotFoundException;
import com.terrarent.repository.RoleRepository;
import com.terrarent.repository.UserRepository;
import com.terrarent.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService; // Assuming an EmailService for sending verification emails

    // In-memory store for verification codes. In a real application, this should be persisted (e.g., Redis, database).
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomAuthenticationException("Email already registered: " + request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .status(User.UserStatus.PENDING_VERIFICATION) // Set initial status
                .build();

        User savedUser = userRepository.save(user);

        // Generate and send verification code
        String verificationCode = generateVerificationCode();
        verificationCodes.put(savedUser.getEmail(), verificationCode);
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationCode);

        return UserResponse.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole().getName())
                .status(savedUser.getStatus())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new CustomAuthenticationException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")); // Should not happen if authenticate succeeds

        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            throw new CustomAuthenticationException("Account not verified. Please verify your email.");
        }
        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new CustomAuthenticationException("Account suspended. Please contact support.");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRole().getName().name());
        claims.put("userId", user.getId().toString()); // Add user ID to claims

        // Create a Spring Security UserDetails object from our User entity
        UserDetails userDetails = new TerraRentUserDetails(user);

        String jwtToken = jwtService.generateToken(claims, userDetails, jwtService.getAccessTokenExpiration());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .build();

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String storedCode = verificationCodes.get(request.getEmail());

        if (storedCode == null || !storedCode.equals(request.getCode())) {
            throw new CustomAuthenticationException("Invalid verification code.");
        }

        user.setStatus(User.UserStatus.VERIFIED); // Changed from ACTIVE to VERIFIED
        userRepository.save(user);
        verificationCodes.remove(request.getEmail()); // Remove code after successful verification
    }

    public void resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (user.getStatus() != User.UserStatus.PENDING_VERIFICATION) {
            throw new CustomAuthenticationException("Email is already verified.");
        }

        String newVerificationCode = generateVerificationCode();
        verificationCodes.put(user.getEmail(), newVerificationCode);
        emailService.sendVerificationEmail(user.getEmail(), newVerificationCode);
    }

    // This method handles the refresh token request.
    public AuthResponse refreshToken(String refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail == null) {
            throw new CustomAuthenticationException("Invalid refresh token.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDetails userDetails = new TerraRentUserDetails(user);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new CustomAuthenticationException("Expired or invalid refresh token. Please log in again.");
        }

        // Generate new access token
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRole().getName().name());
        claims.put("userId", user.getId().toString());
        String newAccessToken = jwtService.generateToken(claims, userDetails, jwtService.getAccessTokenExpiration());

        // Generate a new refresh token (optional, but good practice for rotation)
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .build();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userResponse)
                .build();
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // Generates a 6-digit code
        return String.valueOf(code);
    }
}
