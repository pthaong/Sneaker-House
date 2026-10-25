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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(AuthService service, AuthenticationManager authenticationManager,
                          SecurityContextRepository contextRepository) {
        this.service = service; this.authenticationManager = authenticationManager; this.contextRepository = contextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.response(service.register(request)));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim().toLowerCase(), request.password()));
        } catch (Exception e) {
            throw new AuthException(401, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng.");
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);
        return service.response(service.findByEmail(authentication.getName()));
    }

    @PostMapping("/google")
    public UserResponse google(@Valid @RequestBody GoogleRequest request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        var user = service.googleLogin(request.idToken());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            user.getEmail(), null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);
        return service.response(user);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        service.requestPasswordReset(request.email());
        return new MessageResponse("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi.");
    }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request);
        return new MessageResponse("Đặt lại mật khẩu thành công.");
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return new MessageResponse("Đăng xuất thành công.");
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return service.response(service.findByEmail(authentication.getName()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> authError(AuthException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationError() {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ."));
    }
}
