package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8, max=72) String password,
        @NotBlank @Size(min=2, max=120) String fullName) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record GoogleRequest(@NotBlank String idToken) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min=8, max=72) String newPassword) {}
    public record UserResponse(Long id, String email, String fullName, String role) {}
    public record MessageResponse(String message) {}
    public record ErrorResponse(String error, String message) {}
}
