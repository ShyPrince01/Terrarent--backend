package com.terrarent.service;

import com.terrarent.dto.user.UserResponse;
import com.terrarent.entity.User;
import com.terrarent.exception.ResourceNotFoundException;
import com.terrarent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Added for data safety

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Optimization: all methods here are reads
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserById(UUID id) {
        return mapUserToUserResponse(getUserEntityById(id));
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                return getUserIdByEmail(userDetails.getUsername());
            }
        }
        throw new RuntimeException("User not authenticated");
    }

    public UUID getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public UserResponse getUserByEmail(String email) {
        return mapUserToUserResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email)));
    }

    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // Changed to package-private or public, kept static if needed by Mappers
    public static UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().getName() : null) // Added null check
                .status(user.getStatus())
                .build();
    }
}