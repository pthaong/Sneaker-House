package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    // Đăng ký tài khoản
    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 2, max = 120) String fullName
    ) {}

    // Đăng nhập bằng email và mật khẩu
    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    // Đăng nhập bằng Google
    public record GoogleRequest(
        @NotBlank String idToken
    ) {}

    // Yêu cầu gửi OTP qua Gmail
    public record ForgotPasswordRequest(
        @NotBlank @Email String email
    ) {}

    // Xác minh OTP 6 số
    public record VerifyOtpRequest(
        @NotBlank @Email String email,

        @NotBlank
        @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP phải gồm đúng 6 chữ số"
        )
        String otp
    ) {}

    // Kết quả xác minh OTP thành công
    public record VerifyOtpResponse(
        String resetToken,
        String message
    ) {}

    // Yêu cầu gửi lại OTP
    public record ResendOtpRequest(
        @NotBlank @Email String email
    ) {}

    // Đặt lại mật khẩu sau khi xác minh OTP
    public record ResetPasswordRequest(
        @NotBlank String token,

        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
    ) {}

    // Thông tin người dùng
    public record UserResponse(
        Long id,
        String email,
        String fullName,
        String role
    ) {}

    public record MessageResponse(String message) {}

    public record ErrorResponse(
        String error,
        String message
    ) {}
}