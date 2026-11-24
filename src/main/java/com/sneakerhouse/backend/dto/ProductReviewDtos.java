
package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ProductReviewDtos {

    private ProductReviewDtos() {}

    public record CreateRequest(
            @NotNull
            @Min(1)
            @Max(5)
            Integer rating,

            @Size(max = 2000)
            String comment
    ) {}

    public record ReviewResponse(
            Long id,
            Long productId,
            Long userId,
            String fullName,
            Integer rating,
            String comment,
            LocalDateTime createdAt
    ) {}
    
    public record RatingSummary(
            Double averageRating,
            Long totalReviews
    ) {}
}
