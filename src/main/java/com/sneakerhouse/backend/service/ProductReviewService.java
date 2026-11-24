
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.ProductReviewDtos.*;
import com.sneakerhouse.backend.entity.ProductReview;
import com.sneakerhouse.backend.repository.ProductRepository;
import com.sneakerhouse.backend.repository.ProductReviewRepository;
import com.sneakerhouse.backend.repository.UserAccountRepository;
import com.sneakerhouse.backend.security.CustomUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProductReviewService {

    private final ProductReviewRepository reviews;
    private final ProductRepository products;
    private final UserAccountRepository users;

    public ProductReviewService(
            ProductReviewRepository reviews,
            ProductRepository products,
            UserAccountRepository users
    ) {
        this.reviews = reviews;
        this.products = products;
        this.users = users;
    }

    public List<ReviewResponse> getReviews(Long productId) {
        if (!products.existsById(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sản phẩm"
            );
        }

        return reviews.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RatingSummary getSummary(Long productId) {
        if (!products.existsById(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sản phẩm"
            );
        }

        Double average = reviews.getAverageRating(productId);

        return new RatingSummary(
                average == null ? 0.0 : Math.round(average * 100.0) / 100.0,
                reviews.countByProduct_Id(productId)
        );
    }

    public ReviewResponse create(
            Long productId,
            CreateRequest request,
            Authentication authentication
    ) {
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails currentUser)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập"
            );
        }

        var product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm"
                ));

        var user = users.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tài khoản"
                ));

        if (reviews.findByProduct_IdAndUser_Id(
                productId, currentUser.getId()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bạn đã đánh giá sản phẩm này"
            );
        }

        ProductReview review = new ProductReview();
        review.product = product;
        review.user = user;
        review.rating = request.rating();
        review.comment = request.comment();

        return toResponse(reviews.save(review));
    }

    private ReviewResponse toResponse(ProductReview review) {
    return new ReviewResponse(
            review.id,
            review.product.id,
            review.user.getId(),
            review.user.getFullName(),
            review.rating,
            review.comment,
            review.createdAt
    );
}
}
