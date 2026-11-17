
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.AuthDtos.ErrorResponse;
import com.sneakerhouse.backend.dto.AuthDtos.MessageResponse;
import com.sneakerhouse.backend.service.AccountSessionRegistry;
import com.sneakerhouse.backend.service.AuthException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account/sessions")
public class AccountSessionController {

    private final AccountSessionRegistry registry;

    public AccountSessionController(
            AccountSessionRegistry registry
    ) {
        this.registry = registry;
    }

    // GET /api/account/sessions
    @GetMapping
    public ResponseEntity<List<AccountSessionRegistry.SessionView>>
    getSessions(
            Authentication authentication,
            HttpServletRequest request
    ) {
        requireAuthentication(authentication);

        HttpSession currentSession =
                request.getSession(false);

        return ResponseEntity.ok(
                registry.list(
                        authentication.getName(),
                        currentSession
                )
        );
    }

    // DELETE /api/account/sessions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> revokeSession(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requireAuthentication(authentication);

        HttpSession currentSession =
                request.getSession(false);

        registry.revoke(
                authentication.getName(),
                currentSession,
                id
        );

        return ResponseEntity.ok(
                new MessageResponse(
                        "Đã thu hồi phiên đăng nhập thành công."
                )
        );
    }

    // Chi cho phep tai khoan da dang nhap
    private void requireAuthentication(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                        authentication.getPrincipal()
                )) {

            throw new AuthException(
                    401,
                    "UNAUTHORIZED",
                    "Bạn cần đăng nhập để quản lý phiên."
            );
        }
    }

    // Tra loi theo Error Format hien tai cua Backend
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
}
