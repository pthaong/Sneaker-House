
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProfileDtos.*;
import com.sneakerhouse.backend.service.AuthException;
import com.sneakerhouse.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final long MAX = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;

    private final ProfileService profiles;
    private final Path uploadDir;

    public ProfileController(
            ProfileService profiles,
            @Value("${app.upload-dir:uploads/avatars}") String directory) {

        this.profiles = profiles;
        this.uploadDir = Paths.get(directory)
                .toAbsolutePath()
                .normalize();
    }

    // GET: Thông tin tài khoản đang đăng nhập
    @GetMapping
    public ProfileResponse me(Authentication authentication) {
        return profiles.get(authentication.getName());
    }

    // PUT: Cập nhật thông tin cá nhân
    @PutMapping
    public ProfileResponse update(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        if (request.birthDate() != null
                && request.birthDate().isAfter(LocalDate.now())) {

            throw new AuthException(
                    400,
                    "INVALID_BIRTH_DATE",
                    "Ngày sinh không được ở tương lai."
            );
        }

        return profiles.update(authentication.getName(), request);
    }

    // POST: Upload ảnh đại diện
    @PostMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ProfileResponse avatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {

        // 1. Kiểm tra file rỗng
        if (file.isEmpty()) {
            throw new AuthException(
                    400, "EMPTY_FILE", "Vui lòng chọn ảnh."
            );
        }

        // 2. Giới hạn 5MB
        if (file.getSize() > MAX) {
            throw new AuthException(
                    413, "FILE_TOO_LARGE",
                    "Ảnh đại diện tối đa 5MB."
            );
        }

        // 3. Kiểm tra Content-Type
        String type = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);

        if (!Set.of("image/jpeg", "image/png").contains(type)) {
            throw new AuthException(
                    415, "UNSUPPORTED_IMAGE_TYPE",
                    "Chỉ chấp nhận ảnh JPG hoặc PNG."
            );
        }

        // 4. Xác minh nội dung và định dạng ảnh thực tế
        try {
            try (ImageInputStream input =
                    ImageIO.createImageInputStream(file.getInputStream())) {

                if (input == null) {
                    throw new AuthException(
                            400, "INVALID_IMAGE",
                            "Tệp không phải ảnh hợp lệ."
                    );
                }

                Iterator<ImageReader> readers =
                        ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new AuthException(
                            400, "INVALID_IMAGE",
                            "Tệp không phải ảnh hợp lệ."
                    );
                }

                ImageReader reader = readers.next();

                try {
                    reader.setInput(input);

                    String format = reader.getFormatName()
                            .toLowerCase(Locale.ROOT);

                    String actualType =
                            format.equals("png") ? "image/png"
                            : (format.equals("jpeg") || format.equals("jpg"))
                            ? "image/jpeg"
                            : "";

                    if (!actualType.equals(type)) {
                        throw new AuthException(
                                400, "INVALID_IMAGE",
                                "Định dạng ảnh không hợp lệ."
                        );
                    }

                    long pixels = (long) reader.getWidth(0)
                            * reader.getHeight(0);

                    if (pixels > MAX_PIXELS) {
                        throw new AuthException(
                                400, "INVALID_IMAGE",
                                "Kích thước ảnh quá lớn."
                        );
                    }

                    // Đọc toàn bộ ảnh để kiểm tra dữ liệu
                    if (reader.read(0) == null) {
                        throw new AuthException(
                                400, "INVALID_IMAGE",
                                "Không thể đọc ảnh."
                        );
                    }

                } finally {
                    reader.dispose();
                }

            }

        } catch (AuthException e) {
            throw e;
        } catch (IOException e) {
            throw new AuthException(
                    400, "INVALID_IMAGE",
                    "Tệp ảnh bị hỏng hoặc không hợp lệ."
            );
        }

        // 5. Lưu ảnh và cập nhật đường dẫn database
        String extension = type.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path target = uploadDir.resolve(filename).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new AuthException(
                    400, "INVALID_FILE",
                    "Đường dẫn tệp không hợp lệ."
            );
        }

        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), target);

            return profiles.avatar(
                    authentication.getName(),
                    "/uploads/avatars/" + filename
            );

        } catch (IOException e) {
            throw new AuthException(
                    500, "AVATAR_UPLOAD_FAILED",
                    "Không thể lưu ảnh đại diện."
            );
        }
    }
}
