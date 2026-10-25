package com.sneakerhouse.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sneakerhouse.backend.dto.AuthDtos.*;
import com.sneakerhouse.backend.entity.PasswordResetToken;
import com.sneakerhouse.backend.entity.UserAccount;
import com.sneakerhouse.backend.repository.PasswordResetTokenRepository;
import com.sneakerhouse.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    @Value("${google.oauth.client-id:}") private String googleClientId;
    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${app.mail-from}") private String mailFrom;

    public AuthService(UserAccountRepository users, PasswordResetTokenRepository tokens,
                       PasswordEncoder encoder, JavaMailSender mailSender) {
        this.users = users; this.tokens = tokens; this.encoder = encoder; this.mailSender = mailSender;
    }

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) throw new AuthException(409, "EMAIL_EXISTS", "Email đã được sử dụng.");
        UserAccount user = new UserAccount();
        user.setEmail(email); user.setFullName(request.fullName().trim());
        user.setPasswordHash(encoder.encode(request.password()));
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {
        return users.findByEmailIgnoreCase(normalize(email))
            .orElseThrow(() -> new AuthException(401, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng."));
    }

    @Transactional
    public UserAccount googleLogin(String idToken) {
        if (googleClientId == null || googleClientId.isBlank())
            throw new AuthException(503, "GOOGLE_NOT_CONFIGURED", "Chưa cấu hình GOOGLE_CLIENT_ID.");
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId)).build();
            GoogleIdToken verified = verifier.verify(idToken);
            if (verified == null) throw new AuthException(401, "INVALID_GOOGLE_TOKEN", "Google ID Token không hợp lệ hoặc đã hết hạn.");
            GoogleIdToken.Payload payload = verified.getPayload();
            String sub = payload.getSubject();
            String email = normalize(payload.getEmail());
            if (!Boolean.TRUE.equals(payload.getEmailVerified()))
                throw new AuthException(401, "GOOGLE_EMAIL_NOT_VERIFIED", "Email Google chưa được xác minh.");
            var existingGoogle = users.findByGoogleSub(sub);
            if (existingGoogle.isPresent()) return existingGoogle.get();
            var existingEmail = users.findByEmailIgnoreCase(email);
            if (existingEmail.isPresent())
                throw new AuthException(409, "EMAIL_COLLISION", "Email đã có tài khoản. Hãy đăng nhập bằng mật khẩu; không tự động liên kết Google.");
            UserAccount user = new UserAccount();
            user.setEmail(email);
            String name = (String) payload.get("name");
            user.setFullName(name == null || name.isBlank() ? email : name);
            user.setGoogleSub(sub);
            return users.save(user);
        } catch (AuthException e) { throw e; }
        catch (Exception e) {
            throw new AuthException(401, "INVALID_GOOGLE_TOKEN", "Không thể xác minh Google ID Token.");
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        // Generic response avoids disclosing whether an account exists.
        users.findByEmailIgnoreCase(normalize(email)).ifPresent(user -> {
            tokens.deleteByUser_Id(user.getId());
            byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String hash = sha256(raw);
            tokens.save(new PasswordResetToken(user, hash, Instant.now().plus(30, ChronoUnit.MINUTES)));
            String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + raw;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom); message.setTo(user.getEmail());
            message.setSubject("Đặt lại mật khẩu Sneaker House");
            message.setText("Mở liên kết sau để đặt lại mật khẩu (có hiệu lực 30 phút, chỉ dùng một lần):\n" + link);
            try { mailSender.send(message); }
            catch (MailException e) { throw new AuthException(503, "MAIL_NOT_CONFIGURED", "Không gửi được email đặt lại mật khẩu. Hãy kiểm tra cấu hình SMTP."); }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var token = tokens.findByTokenHash(sha256(request.token()))
            .orElseThrow(() -> new AuthException(400, "INVALID_RESET_TOKEN", "Token đặt lại mật khẩu không hợp lệ."));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now()))
            throw new AuthException(400, "EXPIRED_RESET_TOKEN", "Token đã hết hạn hoặc đã được sử dụng.");
        UserAccount user = token.getUser();
        user.setPasswordHash(encoder.encode(request.newPassword()));
        token.markUsed();
        users.save(user);
        tokens.save(token);
    }

    public UserResponse response(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
    private String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(); }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
