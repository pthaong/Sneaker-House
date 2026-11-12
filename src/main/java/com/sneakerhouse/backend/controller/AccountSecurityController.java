
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ChangePasswordRequest;
import com.sneakerhouse.backend.dto.AuthDtos.MessageResponse;
import com.sneakerhouse.backend.dto.AuthDtos.ErrorResponse;
import com.sneakerhouse.backend.service.AccountSecurityService;
import com.sneakerhouse.backend.service.AuthException;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountSecurityController {

    private final AccountSecurityService service;

    public AccountSecurityController(
            AccountSecurityService service
    ) {
        this.service = service;
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new AuthException(
                    401,
                    "UNAUTHORIZED",
                    "Bạn cần đăng nhập để đổi mật khẩu."
            );
        }

        service.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                new MessageResponse(
                        "Đổi mật khẩu thành công."
                )
        );
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> authError(
            AuthException e
    ) {
        return ResponseEntity
                .status(e.getStatus())
                .body(new ErrorResponse(
                        e.getCode(),
                        e.getMessage()
                ));
    }
}
