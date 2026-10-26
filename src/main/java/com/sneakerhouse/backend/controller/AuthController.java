package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.AuthDtos.*;
import com.sneakerhouse.backend.service.AuthException;
import com.sneakerhouse.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(
            AuthService service,
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository
    ) {
        this.service = service;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    // =====================================================
    // 1. CSRF TOKEN
    // =====================================================

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {

        return Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        );
    }

    // =====================================================
    // 2. REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.response(
                                service.register(request)
                        )
                );
    }

    // =====================================================
    // 3. LOGIN
    // =====================================================

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        Authentication authentication;

        try {

            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken
                            .unauthenticated(
                                    request.email().trim().toLowerCase(),
                                    request.password()
                            )
            );

        } catch (
                org.springframework.security.core.AuthenticationException e
        ) {

            throw new AuthException(
                    401,
                    "INVALID_CREDENTIALS",
                    "Email hoặc mật khẩu không đúng."
            );
        }

                // Chống Session Fixation sau khi đăng nhập thành công
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return service.response(
                service.findByEmail(
                        authentication.getName()
                )
        );
    }

    // =====================================================
    // 4. GOOGLE LOGIN
    // =====================================================

    @PostMapping("/google")
    public UserResponse google(
            @Valid @RequestBody GoogleRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        var user = service.googleLogin(
                request.idToken()
        );

        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getEmail(),
                        null,
                        java.util.List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority(
                                                "ROLE_" + user.getRole()
                                        )
                        )
                );

                // Chống Session Fixation sau khi đăng nhập Google thành công
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return service.response(user);
    }

    // =====================================================
    // 5. FORGOT PASSWORD - SEND OTP
    // =====================================================

    @PostMapping("/forgot-password")
    public MessageResponse forgot(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        service.requestPasswordReset(
                request.email()
        );

        return new MessageResponse(
                "Nếu email đã đăng ký, mã OTP sẽ được gửi đến email của bạn."
        );
    }

    // =====================================================
    // 6. VERIFY OTP
    // =====================================================

    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {

        return service.verifyOtp(request);
    }

    // =====================================================
    // 7. RESEND OTP
    // =====================================================

    @PostMapping("/resend-otp")
    public MessageResponse resendOtp(
            @Valid @RequestBody ResendOtpRequest request
    ) {

        // Gọi trực tiếp phương thức có @Transactional.
        service.requestPasswordReset(
                request.email()
        );

        return new MessageResponse(
                "Nếu email đã đăng ký và đủ điều kiện, mã OTP mới sẽ được gửi."
        );
    }

    // =====================================================
    // 8. RESET PASSWORD
    // =====================================================

    @PostMapping("/reset-password")
    public MessageResponse reset(
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        service.resetPassword(request);

        return new MessageResponse(
                "Đặt lại mật khẩu thành công."
        );
    }

    // =====================================================
    // 9. LOGOUT
    // =====================================================

    @PostMapping("/logout")
    public MessageResponse logout(
            HttpServletRequest request
    ) {

        SecurityContextHolder.clearContext();

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return new MessageResponse(
                "Đăng xuất thành công."
        );
    }

    // =====================================================
    // 10. CURRENT USER
    // =====================================================

    @GetMapping("/me")
    public UserResponse me(
            Authentication authentication
    ) {

        return service.response(
                service.findByEmail(
                        authentication.getName()
                )
        );
    }

    // =====================================================
    // 11. AUTH ERROR HANDLER
    // =====================================================

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> authError(
            AuthException e
    ) {

        return ResponseEntity
                .status(e.getStatus())
                .body(
                        new ErrorResponse(
                                e.getCode(),
                                e.getMessage()
                        )
                );
    }

    // =====================================================
    // 12. VALIDATION ERROR HANDLER
    // =====================================================

    @ExceptionHandler(
            org.springframework.web.bind.MethodArgumentNotValidException.class
    )
    public ResponseEntity<ErrorResponse> validationError() {

        return ResponseEntity
                .badRequest()
                .body(
                        new ErrorResponse(
                                "VALIDATION_ERROR",
                                "Dữ liệu đầu vào không hợp lệ."
                        )
                );
    }
}