package com.terrarent.dto.user;

import com.terrarent.entity.Role;
import com.terrarent.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO for user details")
public class UserResponse {
    @Schema(description = "Unique identifier of the user", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private UUID id;
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;
    @Schema(description = "User's phone number", example = "+15551234567")
    private String phoneNumber;
    @Schema(description = "User's role", example = "ROLE_RENTER", allowableValues = {"ROLE_RENTER", "ROLE_LANDLORD", "ROLE_ADMIN"})
    private Role.RoleName role;
    @Schema(description = "User's account status", example = "VERIFIED", allowableValues = {"PENDING_VERIFICATION", "VERIFIED", "SUSPENDED"})
    private User.UserStatus status;
}
