
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.PaymentDtos;
import com.sneakerhouse.backend.entity.CustomerOrder;
import com.sneakerhouse.backend.entity.Payment;
import com.sneakerhouse.backend.repository.CustomerOrderRepository;
import com.sneakerhouse.backend.repository.PaymentRepository;
import com.sneakerhouse.backend.security.CustomUserDetails;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PayOSService {

    private final CustomerOrderRepository orders;
    private final PaymentRepository payments;

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payos.return-url:http://localhost:5173/payment/result}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:5173/payment/cancel}")
    private String cancelUrl;

    public PayOSService(
            CustomerOrderRepository orders,
            PaymentRepository payments
    ) {
        this.orders = orders;
        this.payments = payments;
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập"
            );
        }

        return user.getId();
    }

    private PayOS getPayOS() {
        if (clientId == null || clientId.isBlank()
                || apiKey == null || apiKey.isBlank()
                || checksumKey == null || checksumKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PayOS chưa được cấu hình đầy đủ"
            );
        }

        return new PayOS(clientId, apiKey, checksumKey);
    }

    /**
     * Tạo link thanh toán cho đơn hàng thuộc người dùng hiện tại.
     */
    @Transactional
    public PaymentDtos.CreatePaymentResponse createPayment(
            Authentication authentication,
            Long orderId
    ) {
        Long userId = getUserId(authentication);

        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        if (!"PAYOS".equalsIgnoreCase(order.paymentMethod)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng không sử dụng phương thức PayOS"
            );
        }

        if ("PAID".equalsIgnoreCase(order.paymentStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã được thanh toán"
            );
        }

        if ("CANCELLED".equalsIgnoreCase(order.status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã bị hủy"
            );
        }

        var existing = payments
                .findFirstByOrder_IdAndStatusOrderByCreatedAtDesc(
                        orderId, "PENDING"
                );

        if (existing.isPresent()
                && existing.get().paymentUrl != null
                && !existing.get().paymentUrl.isBlank()) {
            return toCreateResponse(existing.get());
        }

        long amount;
        try {
            amount = order.totalAmount.longValueExact();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền thanh toán phải là số nguyên VND"
            );
        }

        if (amount <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền thanh toán không hợp lệ"
            );
        }

        // Mã giao dịch số, giới hạn 15 chữ số.
        long providerOrderCode =
                (System.currentTimeMillis() % 1_000_000_000_000_000L);

        String description = "SH" + order.id;

        CreatePaymentLinkRequest request =
                CreatePaymentLinkRequest.builder()
                        .orderCode(providerOrderCode)
                        .amount(amount)
                        .description(description)
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .build();

        try {
            var response = getPayOS()
                    .paymentRequests()
                    .create(request);

            Payment payment = new Payment();
            payment.order = order;
            payment.paymentMethod = "PAYOS";
            payment.transactionCode = "PAYOS-" + UUID.randomUUID();
            payment.providerOrderCode = providerOrderCode;
            payment.amount = BigDecimal.valueOf(amount);
            payment.status = "PENDING";
            payment.paymentLinkId = response.getPaymentLinkId();
            payment.paymentUrl = response.getCheckoutUrl();

            payment = payments.save(payment);

            return toCreateResponse(payment);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể tạo link thanh toán PayOS"
            );
        }
    }

    private PaymentDtos.CreatePaymentResponse toCreateResponse(
            Payment payment
    ) {
        return new PaymentDtos.CreatePaymentResponse(
                payment.id,
                payment.order.id,
                payment.providerOrderCode,
                payment.paymentMethod,
                payment.amount,
                payment.status,
                payment.paymentUrl
        );
    }

    /**
     * Nhận JSON gốc và xác minh chữ ký bằng SDK PayOS.
     */
    @Transactional
    public void handleWebhook(String rawBody) {

        if (rawBody == null || rawBody.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook không có dữ liệu"
            );
        }

        try {
            var webhookData = getPayOS()
                    .webhooks()
                    .verify(rawBody);

            Long providerOrderCode = webhookData.getOrderCode();
            Long receivedAmount = webhookData.getAmount();

            if (providerOrderCode == null || receivedAmount == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Dữ liệu giao dịch không hợp lệ"
                );
            }

            Payment payment = payments
                    .findByProviderOrderCode(providerOrderCode)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy giao dịch"
                    ));

            if (payment.amount.compareTo(
                    BigDecimal.valueOf(receivedAmount)
            ) != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số tiền thanh toán không khớp"
                );
            }

            // Webhook có thể được PayOS gửi lại nhiều lần.
            if ("PAID".equalsIgnoreCase(payment.status)) {
                return;
            }

            CustomerOrder order = payment.order;

            if (!"PAYOS".equalsIgnoreCase(order.paymentMethod)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Phương thức thanh toán không hợp lệ"
                );
            }

            if ("CANCELLED".equalsIgnoreCase(order.status)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng đã bị hủy"
                );
            }

            // Chỉ xác nhận thanh toán thành công khi mã giao dịch là 00.
            if (!"00".equals(webhookData.getCode())) {
                payment.status = "FAILED";
                payments.save(payment);
                return;
            }

            payment.status = "PAID";
            payment.providerTransactionId = webhookData.getReference();

            order.paymentStatus = "PAID";

            payments.save(payment);
            orders.save(order);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook PayOS không hợp lệ"
            );
        }
    }

    /**
     * Lấy trạng thái thanh toán của đơn hàng thuộc người dùng hiện tại.
     */
    @Transactional
    public PaymentDtos.PaymentStatusResponse getPaymentStatus(
            Authentication authentication,
            Long orderId
    ) {
        Long userId = getUserId(authentication);

        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        Payment payment = payments
                .findFirstByOrder_IdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Đơn hàng chưa có giao dịch thanh toán"
                ));

        return new PaymentDtos.PaymentStatusResponse(
                payment.id,
                order.id,
                payment.paymentMethod,
                payment.amount,
                payment.status,
                order.paymentStatus,
                payment.paymentUrl,
                payment.createdAt
        );
    }
}
