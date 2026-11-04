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
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${app.mail-from}")
    private String mailFrom;

    @Value("${app.otp.secret:}")
    private String otpSecret;

    @Value("${app.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.otp.reset-grant-minutes:5}")
    private long resetGrantMinutes;

    public AuthService(
            UserAccountRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder encoder,
            JavaMailSender mailSender
    ) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSender = mailSender;
    }

    // =====================================================
    // 1. REGISTER
    // =====================================================

    @Transactional
    public UserAccount register(RegisterRequest request) {

        String email = normalize(request.email());

        if (users.existsByEmailIgnoreCase(email)) {
            throw new AuthException(
                    409,
                    "EMAIL_EXISTS",
                    "Email đã được sử dụng."
            );
        }

        UserAccount user = new UserAccount();

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(
                encoder.encode(request.password())
        );

        return users.save(user);
    }

    // =====================================================
    // 2. FIND USER BY EMAIL
    // =====================================================

    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {

        return users.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new AuthException(
                        401,
                        "INVALID_CREDENTIALS",
                        "Email hoặc mật khẩu không đúng."
                ));
    }

    // =====================================================
    // 3. GOOGLE LOGIN
    // =====================================================

    @Transactional
    public UserAccount googleLogin(String idToken) {

        if (googleClientId == null ||
                googleClientId.isBlank()) {

            throw new AuthException(
                    503,
                    "GOOGLE_NOT_CONFIGURED",
                    "Chưa cấu hình GOOGLE_CLIENT_ID."
            );
        }

        try {

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance()
                    )
                    .setAudience(List.of(googleClientId))
                    .build();

            GoogleIdToken verified =
                    verifier.verify(idToken);

            if (verified == null) {
                throw new AuthException(
                        401,
                        "INVALID_GOOGLE_TOKEN",
                        "Google ID Token không hợp lệ hoặc đã hết hạn."
                );
            }

            GoogleIdToken.Payload payload =
                    verified.getPayload();

            String sub = payload.getSubject();
            String email = normalize(payload.getEmail());

            if (!Boolean.TRUE.equals(
                    payload.getEmailVerified())) {

                throw new AuthException(
                        401,
                        "GOOGLE_EMAIL_NOT_VERIFIED",
                        "Email Google chưa được xác minh."
                );
            }

            var existingGoogle =
                    users.findByGoogleSub(sub);

            if (existingGoogle.isPresent()) {
                return existingGoogle.get();
            }

            var existingEmail =
                    users.findByEmailIgnoreCase(email);

            if (existingEmail.isPresent()) {

                throw new AuthException(
                        409,
                        "EMAIL_COLLISION",
                        "Email đã có tài khoản. " +
                        "Hãy đăng nhập bằng mật khẩu; " +
                        "không tự động liên kết Google."
                );
            }

            UserAccount user = new UserAccount();

            user.setEmail(email);

            String name = (String) payload.get("name");

            user.setFullName(
                    name == null || name.isBlank()
                            ? email
                            : name
            );

            user.setGoogleSub(sub);

            return users.save(user);

        } catch (AuthException e) {
            throw e;

        } catch (Exception e) {

            throw new AuthException(
                    401,
                    "INVALID_GOOGLE_TOKEN",
                    "Không thể xác minh Google ID Token."
            );
        }
    }

    // =====================================================
    // 4. FORGOT PASSWORD - SEND OTP
    // =====================================================

    @Transactional
    public void requestPasswordReset(String email) {

        String normalizedEmail = normalize(email);

        // Không tiết lộ email có tồn tại hay không.
        users.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(user -> {

            Instant now = Instant.now();

            var latest = tokens
                    .findTopByUser_IdOrderByIdDesc(
                            user.getId()
                    );

            // Kiểm tra thời gian gửi OTP gần nhất.
            if (latest.isPresent()) {

                PasswordResetToken previous =
                        latest.get();

                Instant createdAt =
                        previous.getExpiresAt()
                                .minus(
                                        otpExpirationMinutes,
                                        ChronoUnit.MINUTES
                                );

                if (now.isBefore(
                        createdAt.plusSeconds(
                                resendCooldownSeconds
                        ))) {

                    throw new AuthException(
                            429,
                            "OTP_TOO_FREQUENT",
                            "Vui lòng chờ 60 giây " +
                            "trước khi yêu cầu OTP mới."
                    );
                }
            }

            // Tạo OTP ngẫu nhiên 6 chữ số.
            String otp = String.format(
                    "%06d",
                    SECURE_RANDOM.nextInt(1_000_000)
            );

            // Mã hóa OTP bằng HMAC-SHA256.
            String otpHash =
                    hmacOtp(user.getId(), otp);

            // Xóa các OTP cũ.
            tokens.deleteByUser_Id(user.getId());
            tokens.flush();

            // Lưu OTP mới.
            PasswordResetToken resetToken =
                    new PasswordResetToken(
                            user,
                            otpHash,
                            now.plus(
                                    otpExpirationMinutes,
                                    ChronoUnit.MINUTES
                            )
                    );

            tokens.save(resetToken);

            // Gửi OTP qua Gmail.
            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setFrom(mailFrom);
            message.setTo(user.getEmail());

            message.setSubject(
                    "Mã OTP khôi phục mật khẩu Sneaker House"
            );

            message.setText(
                    "Xin chào,\n\n" +

                    "Bạn đã yêu cầu đặt lại mật khẩu " +
                    "tài khoản Sneaker House.\n\n" +

                    "Mã OTP của bạn là:\n\n" +

                    otp + "\n\n" +

                    "Mã có hiệu lực trong " +
                    otpExpirationMinutes +
                    " phút.\n\n" +

                    "Không chia sẻ mã OTP " +
                    "với bất kỳ ai.\n\n" +

                    "Nếu bạn không yêu cầu, " +
                    "hãy bỏ qua email này.\n\n" +

                    "Trân trọng,\n" +
                    "Sneaker House"
            );

            try {

                mailSender.send(message);

            } catch (MailException e) {

                throw new AuthException(
                        503,
                        "MAIL_SEND_FAILED",
                        "Không gửi được OTP. " +
                        "Vui lòng thử lại sau."
                );
            }
        });
    }

    // =====================================================
    // 5. RESEND OTP
    // =====================================================

    public void resendOtp(String email) {

        requestPasswordReset(email);
    }

    // =====================================================
    // 6. VERIFY OTP
    // =====================================================

    @Transactional(noRollbackFor = AuthException.class)
    public VerifyOtpResponse verifyOtp(
            VerifyOtpRequest request
    ) {

        String email = normalize(request.email());

        UserAccount user =
                users.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new AuthException(
                                400,
                                "INVALID_OTP",
                                "Mã OTP không hợp lệ " +
                                "hoặc đã hết hạn."
                        ));

        PasswordResetToken token =
                tokens.findTopByUser_IdOrderByIdDesc(
                        user.getId()
                )
                .orElseThrow(() -> new AuthException(
                        400,
                        "INVALID_OTP",
                        "Mã OTP không hợp lệ " +
                        "hoặc đã hết hạn."
                ));

        Instant now = Instant.now();

        // OTP đã sử dụng, đã xác minh hoặc hết hạn.
        if (token.getUsedAt() != null
                || token.getVerifiedAt() != null
                || !token.getExpiresAt().isAfter(now)) {

            throw new AuthException(
                    400,
                    "INVALID_OTP",
                    "Mã OTP không hợp lệ " +
                    "hoặc đã hết hạn."
            );
        }

        // Giới hạn số lần nhập sai.
        if (token.getFailedAttempts()
                >= otpMaxAttempts) {

            throw new AuthException(
                    429,
                    "OTP_LOCKED",
                    "Bạn đã nhập sai OTP " +
                    "quá số lần cho phép."
            );
        }

        // Tạo HMAC của OTP người dùng nhập.
        String expectedHash =
                hmacOtp(user.getId(), request.otp());

        // So sánh mã băm.
        boolean valid = MessageDigest.isEqual(
                token.getTokenHash()
                        .getBytes(StandardCharsets.US_ASCII),

                expectedHash
                        .getBytes(StandardCharsets.US_ASCII)
        );

        if (!valid) {

            token.incrementFailedAttempts();

            tokens.save(token);

            throw new AuthException(
                    400,
                    "INVALID_OTP",
                    "Mã OTP không chính xác."
            );
        }

        // OTP hợp lệ: tạo resetToken ngẫu nhiên.
        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        String resetGrant =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes);

        // Lưu hash của resetToken.
        token.markVerified(
                sha256(resetGrant),
                now.plus(
                        resetGrantMinutes,
                        ChronoUnit.MINUTES
                )
        );

        tokens.save(token);

        return new VerifyOtpResponse(
                resetGrant,
                "Xác minh OTP thành công."
        );
    }

    // =====================================================
    // 7. RESET PASSWORD
    // =====================================================

    @Transactional
    public void resetPassword(
            ResetPasswordRequest request
    ) {

        String grantHash =
                sha256(request.token());

        PasswordResetToken token =
                tokens.findByResetGrantHash(grantHash)
                        .orElseThrow(() -> new AuthException(
                                400,
                                "INVALID_RESET_TOKEN",
                                "Mã xác nhận đặt lại " +
                                "mật khẩu không hợp lệ."
                        ));

        Instant now = Instant.now();

        // Kiểm tra resetToken.
        if (token.getUsedAt() != null
                || token.getVerifiedAt() == null
                || token.getResetGrantExpiresAt() == null
                || !token.getResetGrantExpiresAt()
                        .isAfter(now)) {

            throw new AuthException(
                    400,
                    "EXPIRED_RESET_TOKEN",
                    "Mã xác nhận đã hết hạn " +
                    "hoặc đã được sử dụng."
            );
        }

        // Đổi mật khẩu.
        UserAccount user = token.getUser();

        user.setPasswordHash(
                encoder.encode(request.newPassword())
        );

        // Đánh dấu resetToken đã sử dụng.
        token.markUsed();

        users.save(user);
        tokens.save(token);
    }

    // =====================================================
    // 8. USER RESPONSE
    // =====================================================

    public UserResponse response(UserAccount user) {

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    // =====================================================
    // 9. NORMALIZE EMAIL
    // =====================================================

    private String normalize(String email) {

        return email == null
                ? ""
                : email.trim().toLowerCase();
    }

    // =====================================================
    // 10. SHA-256 FOR RESET TOKEN
    // =====================================================

    private String sha256(String value) {

        try {

            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    value.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
            );

        } catch (Exception e) {

            throw new IllegalStateException(e);
        }
    }

    // =====================================================
    // 11. HMAC-SHA256 FOR OTP
    // =====================================================

    private String hmacOtp(
            Long userId,
            String otp
    ) {

        try {

            if (otpSecret == null
                    || otpSecret.isBlank()) {

                throw new IllegalStateException(
                        "OTP_SECRET chưa được cấu hình."
                );
            }

            byte[] key =
                    Base64.getDecoder().decode(
                            otpSecret
                    );

            if (key.length < 32) {

                throw new IllegalStateException(
                        "OTP_SECRET phải có ít nhất 256 bit."
                );
            }

            Mac mac =
                    Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(
                            key,
                            "HmacSHA256"
                    )
            );

            String data =
                    userId + ":" + otp;

            return HexFormat.of().formatHex(
                    mac.doFinal(
                            data.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Không thể tạo mã băm OTP.",
                    e
            );
        }
    }
}