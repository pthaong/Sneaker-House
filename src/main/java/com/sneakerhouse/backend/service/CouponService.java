
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.CouponDtos.*;
import com.sneakerhouse.backend.entity.Coupon;
import com.sneakerhouse.backend.repository.CouponRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class CouponService {

    private final CouponRepository coupons;

    public CouponService(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @Transactional(readOnly = true)
    public CouponCheckResponse checkCoupon(CouponCheckRequest request) {

        String code = request.code().trim().toUpperCase();

        Coupon coupon = coupons.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mã giảm giá không tồn tại."
                ));

        Instant now = Instant.now();

        if (!coupon.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã giảm giá đã bị vô hiệu hóa."
            );
        }

        if (coupon.getStartAt() != null
                && now.isBefore(coupon.getStartAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã giảm giá chưa bắt đầu."
            );
        }

        if (coupon.getEndAt() != null
                && now.isAfter(coupon.getEndAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã giảm giá đã hết hạn."
            );
        }

        if (coupon.getUsageLimit() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã giảm giá đã hết lượt sử dụng."
            );
        }

        BigDecimal orderAmount = request.orderAmount();

        if (coupon.getMinOrderValue() != null
                && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa đạt giá trị tối thiểu."
            );
        }

        BigDecimal discount;

        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = orderAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (coupon.getMaxDiscount() != null) {
                discount = discount.min(coupon.getMaxDiscount());
            }
        } else {
            discount = coupon.getDiscountValue().min(orderAmount);
        }

        discount = discount.setScale(0, RoundingMode.HALF_UP);

        BigDecimal finalAmount = orderAmount.subtract(discount);

        return new CouponCheckResponse(
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                orderAmount,
                discount,
                finalAmount,
                "Áp dụng mã giảm giá thành công."
        );
    }

    @Transactional
    public Coupon createCoupon(CouponCreateRequest request) {

        String code = request.code().trim().toUpperCase();

        if (coupons.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã giảm giá đã tồn tại."
            );
        }

        if (request.startAt() != null
                && request.endAt() != null
                && !request.endAt().isAfter(request.startAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

        if ("PERCENT".equals(request.discountType())
                && request.discountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mức giảm phần trăm không được vượt quá 100."
            );
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(request.discountType().toUpperCase());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderValue(
                request.minOrderValue() == null
                        ? BigDecimal.ZERO
                        : request.minOrderValue()
        );
        coupon.setMaxDiscount(request.maxDiscount());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setUsedCount(0);
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setActive(true);

        return coupons.save(coupon);
    }
}
