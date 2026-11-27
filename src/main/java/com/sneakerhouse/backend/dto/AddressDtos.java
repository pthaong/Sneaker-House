
package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AddressDtos {

    private AddressDtos() {}

    public record AddressRequest(
            @NotBlank
            @Size(max = 120)
            String recipientName,

            @NotBlank
            @Pattern(regexp = "^[0-9+() .-]{9,20}$",
                    message = "Số điện thoại không hợp lệ.")
            String phoneNumber,

            @NotBlank @Size(max = 100)
            String province,

            @NotBlank @Size(max = 100)
            String district,

            @NotBlank @Size(max = 100)
            String ward,

            @NotBlank @Size(max = 255)
            String detailAddress,

            Boolean defaultAddress
    ) {}

    public record AddressResponse(
            Long id,
            String recipientName,
            String phoneNumber,
            String province,
            String district,
            String ward,
            String detailAddress,
            boolean defaultAddress,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
