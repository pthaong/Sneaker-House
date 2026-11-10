
package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, max = 72,
              message = "Mật khẩu mới phải từ 8 đến 72 ký tự")
        String newPassword,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
        String confirmPassword

) {
}
