
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.ChangePasswordRequest;
import com.sneakerhouse.backend.entity.UserAccount;
import com.sneakerhouse.backend.repository.UserAccountRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSecurityService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    public AccountSecurityService(
            UserAccountRepository users,
            PasswordEncoder encoder
    ) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        UserAccount user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException(
                        404,
                        "USER_NOT_FOUND",
                        "Không tìm thấy tài khoản."
                ));

        // Tài khoản Google chưa có mật khẩu
        if (user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()) {

            throw new AuthException(
                    409,
                    "PASSWORD_NOT_SET",
                    "Tài khoản chưa có mật khẩu. " +
                    "Vui lòng sử dụng chức năng khôi phục mật khẩu bằng OTP email."
            );
        }

        // Kiểm tra mật khẩu hiện tại
        if (!encoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new AuthException(
                    400,
                    "CURRENT_PASSWORD_INCORRECT",
                    "Mật khẩu hiện tại không chính xác."
            );
        }

        // Không cho phép dùng lại mật khẩu cũ
        if (encoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new AuthException(
                    400,
                    "PASSWORD_UNCHANGED",
                    "Mật khẩu mới phải khác mật khẩu hiện tại."
            );
        }

        // Kiểm tra xác nhận mật khẩu
        if (!request.newPassword().equals(
                request.confirmPassword()
        )) {
            throw new AuthException(
                    400,
                    "PASSWORD_CONFIRM_MISMATCH",
                    "Mật khẩu xác nhận không khớp."
            );
        }

        // Mã hóa mật khẩu mới
        user.setPasswordHash(
                encoder.encode(request.newPassword())
        );

        users.save(user);
    }
}
