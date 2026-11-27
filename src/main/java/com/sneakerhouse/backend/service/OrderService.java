
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.OrderDtos;
import com.sneakerhouse.backend.dto.ShippingDtos;
import com.sneakerhouse.backend.entity.*;
import com.sneakerhouse.backend.repository.*;
import com.sneakerhouse.backend.security.CustomUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {

    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository histories;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final AddressRepository addresses;
    private final CouponRepository coupons;
    private final ShippingService shippingService;

    public OrderService(
            CustomerOrderRepository orders,
            OrderItemRepository orderItems,
            OrderStatusHistoryRepository histories,
            CartRepository carts,
            CartItemRepository cartItems,
            ProductRepository products,
            ProductVariantRepository variants,
            AddressRepository addresses,
            CouponRepository coupons,
            ShippingService shippingService
    ) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.histories = histories;
        this.carts = carts;
        this.cartItems = cartItems;
        this.products = products;
        this.variants = variants;
        this.addresses = addresses;
        this.coupons = coupons;
        this.shippingService = shippingService;
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập."
            );
        }

        return user.getId();
    }

    // ================= CREATE ORDER =================

    @Transactional
    public OrderDtos.OrderResponse createOrder(
            Authentication authentication,
            OrderDtos.CreateOrderRequest request
    ) {
        Long userId = getUserId(authentication);

        Address address = addresses.findByIdAndUser_Id(
                request.addressId(), userId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Địa chỉ giao hàng không tồn tại."
        ));

        Cart cart = carts.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giỏ hàng đang trống."
                ));

        List<CartItem> selectedItems =
                cartItems.findByCart_IdAndSelectedTrueOrderByIdAsc(cart.id);

        if (selectedItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ít nhất một sản phẩm."
            );
        }

        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        Map<Long, Integer> quantities = new LinkedHashMap<>();

        Map<Long, ProductVariant> lockedVariants = new LinkedHashMap<>();
        Map<Long, Integer> variantQuantities = new LinkedHashMap<>();
        Map<Long, Boolean> productHasVariants = new HashMap<>();

        // Khóa sản phẩm và biến thể, kiểm tra tồn kho.
        for (CartItem item : selectedItems) {

            if (item.quantity == null || item.quantity <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số lượng sản phẩm không hợp lệ."
                );
            }

            if (item.product == null || item.product.id == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm trong giỏ hàng không hợp lệ."
                );
            }

            Long productId = item.product.id;

            Product product = lockedProducts.get(productId);

            if (product == null) {
                product = products.findByIdForUpdate(productId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Một sản phẩm trong giỏ hàng không còn tồn tại."
                        ));

                if (!Boolean.TRUE.equals(product.active)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Sản phẩm " + product.name + " đã ngừng kinh doanh."
                    );
                }

                lockedProducts.put(productId, product);
            }

            quantities.merge(productId, item.quantity, Integer::sum);

            // Kiểm tra sản phẩm có sử dụng biến thể hay không.
            boolean hasVariants = productHasVariants.computeIfAbsent(
                    productId,
                    id -> !variants.findByProduct_IdAndActiveTrue(id).isEmpty()
            );

            if (item.variant == null) {
                if (hasVariants) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Vui lòng chọn size và màu cho sản phẩm "
                                    + product.name + "."
                    );
                }

                continue;
            }

            Long variantId = item.variant.id;

            if (variantId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Biến thể sản phẩm không hợp lệ."
                );
            }

            ProductVariant variant = lockedVariants.get(variantId);

            if (variant == null) {
                variant = variants.findActiveByIdForUpdate(variantId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Biến thể sản phẩm không còn kinh doanh."
                        ));

                if (variant.product == null
                        || !Objects.equals(variant.product.id, productId)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Biến thể không thuộc sản phẩm đã chọn."
                    );
                }

                lockedVariants.put(variantId, variant);
            }

            if (!Objects.equals(variant.product.id, productId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Biến thể không thuộc sản phẩm đã chọn."
                );
            }

            variantQuantities.merge(
                    variantId,
                    item.quantity,
                    Integer::sum
            );
        }

        // Kiểm tra tồn kho tổng của từng sản phẩm.
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());

            if (product.stockQuantity == null
                    || product.stockQuantity < entry.getValue()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + product.name
                                + " không đủ số lượng tồn kho."
                );
            }
        }

        // Kiểm tra tồn kho từng size/màu.
        for (Map.Entry<Long, Integer> entry : variantQuantities.entrySet()) {
            ProductVariant variant = lockedVariants.get(entry.getKey());

            if (variant.stockQuantity == null
                    || variant.stockQuantity < entry.getValue()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Size " + variant.size
                                + ", màu " + variant.color
                                + " không đủ số lượng tồn kho."
                );
            }
        }

        // Tính tiền từ giá sản phẩm trong Database.
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> newOrderItems = new ArrayList<>();

        for (CartItem cartItem : selectedItems) {
            Product product = lockedProducts.get(cartItem.product.id);

            BigDecimal unitPrice = getProductPrice(product);
            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(cartItem.quantity)
            );

            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.product = product;
            orderItem.variant = cartItem.variant == null
                    ? null
                    : lockedVariants.get(cartItem.variant.id);

            orderItem.productName = product.name;
            orderItem.productSku = product.sku;
            orderItem.imageUrl = product.imageUrl;
            orderItem.size = cartItem.size;
            orderItem.color = cartItem.color;
            orderItem.quantity = cartItem.quantity;
            orderItem.unitPrice = unitPrice;
            orderItem.lineTotal = lineTotal;

            newOrderItems.add(orderItem);
        }

        // Kiểm tra và tính coupon.
        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = null;

        if (request.couponCode() != null
                && !request.couponCode().isBlank()) {

            couponCode = request.couponCode().trim().toUpperCase();

            coupon = coupons.findByCodeIgnoreCaseForUpdate(couponCode)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Mã giảm giá không tồn tại."
                    ));

            validateCoupon(coupon, subtotal);
            discount = calculateDiscount(coupon, subtotal);
        }

        BigDecimal amountAfterDiscount = subtotal.subtract(discount);

        ShippingDtos.ShippingResponse shipping =
                shippingService.calculateShipping(
                        new ShippingDtos.ShippingRequest(amountAfterDiscount)
                );

        // Tạo đơn hàng.
        CustomerOrder order = new CustomerOrder();

        order.orderCode = "SH" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 12)
                .toUpperCase();

        order.user = address.getUser();

        order.recipientName = address.getRecipientName();
        order.phoneNumber = address.getPhoneNumber();
        order.province = address.getProvince();
        order.district = address.getDistrict();
        order.ward = address.getWard();
        order.detailAddress = address.getDetailAddress();

        order.subtotal = subtotal;
        order.discountAmount = discount;
        order.shippingFee = shipping.shippingFee();
        order.totalAmount = shipping.finalAmount();
        order.couponCode = couponCode;

        String paymentMethod = request.paymentMethod();

        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = "COD";
        } else {
            paymentMethod = paymentMethod.trim().toUpperCase();
        }

        if (!paymentMethod.equals("COD")
                && !paymentMethod.equals("PAYOS")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phương thức thanh toán không hợp lệ."
            );
        }

        order.paymentMethod = paymentMethod;
        order.paymentStatus = "UNPAID";
        order.status = "PENDING";
        order.note = request.note();

        order = orders.save(order);

        // Lưu các sản phẩm trong đơn hàng.
        for (OrderItem item : newOrderItems) {
            item.order = order;
        }

        orderItems.saveAll(newOrderItems);

        // Trừ tồn kho tổng và tăng số lượng đã bán.
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());

            product.stockQuantity -= entry.getValue();

            if (product.soldCount == null) {
                product.soldCount = 0;
            }

            product.soldCount += entry.getValue();

            products.save(product);
        }

        // Trừ tồn kho từng biến thể.
        for (Map.Entry<Long, Integer> entry : variantQuantities.entrySet()) {
            ProductVariant variant = lockedVariants.get(entry.getKey());

            variant.stockQuantity -= entry.getValue();

            variants.save(variant);
        }

        // Tăng lượt sử dụng coupon.
        if (coupon != null) {
            coupon.setUsedCount(
                    (coupon.getUsedCount() == null
                            ? 0
                            : coupon.getUsedCount()) + 1
            );

            coupons.save(coupon);
        }

        // Lưu lịch sử trạng thái đầu tiên.
        OrderStatusHistory history = new OrderStatusHistory();
        history.order = order;
        history.oldStatus = null;
        history.newStatus = "PENDING";
        history.note = "Khách hàng đặt hàng bằng phương thức "
                + paymentMethod + ".";

        histories.save(history);

        // Chỉ xóa sản phẩm đã chọn.
        cartItems.deleteAll(selectedItems);

        return getOrderResponse(order);
    }

    private BigDecimal getProductPrice(Product product) {
        if (product.originalPrice == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm " + product.name + " chưa có giá bán."
            );
        }

        if (product.salePrice != null
                && product.salePrice.compareTo(BigDecimal.ZERO) > 0
                && product.salePrice.compareTo(product.originalPrice) < 0) {
            return product.salePrice;
        }

        return product.originalPrice;
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotal) {
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
                && (coupon.getUsedCount() == null
                || coupon.getUsedCount() >= coupon.getUsageLimit())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã giảm giá đã hết lượt sử dụng."
            );
        }

        if (coupon.getMinOrderValue() != null
                && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã."
            );
        }

        if (!"PERCENT".equalsIgnoreCase(coupon.getDiscountType())
                && !"FIXED".equalsIgnoreCase(coupon.getDiscountType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại mã giảm giá không hợp lệ."
            );
        }

        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())
                && coupon.getDiscountValue().compareTo(
                        new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mức giảm phần trăm không hợp lệ."
            );
        }
    }

    private BigDecimal calculateDiscount(
            Coupon coupon,
            BigDecimal subtotal
    ) {
        BigDecimal discount;

        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = subtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (coupon.getMaxDiscount() != null) {
                discount = discount.min(coupon.getMaxDiscount());
            }
        } else {
            discount = coupon.getDiscountValue().min(subtotal);
        }

        return discount.setScale(0, RoundingMode.HALF_UP);
    }

    // ================= GET ORDERS =================

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderHistoryResponse> getMyOrders(
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        return orders.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> new OrderDtos.OrderHistoryResponse(
                        order.id,
                        order.orderCode,
                        order.status,
                        order.paymentMethod,
                        order.paymentStatus,
                        order.totalAmount,
                        order.createdAt
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getOrderDetail(
            Authentication authentication,
            Long orderId
    ) {
        Long userId = getUserId(authentication);

        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng."
                ));

        return getOrderResponse(order);
    }

    private OrderDtos.OrderResponse getOrderResponse(CustomerOrder order) {
        List<OrderDtos.OrderItemResponse> items =
                orderItems.findByOrder_IdOrderByIdAsc(order.id)
                        .stream()
                        .map(item -> new OrderDtos.OrderItemResponse(
                                item.id,
                                item.product == null ? null : item.product.id,
                                item.productName,
                                item.productSku,
                                item.imageUrl,
                                item.size,
                                item.color,
                                item.quantity,
                                item.unitPrice,
                                item.lineTotal
                        ))
                        .toList();

        return new OrderDtos.OrderResponse(
                order.id,
                order.orderCode,
                order.status,
                order.paymentMethod,
                order.paymentStatus,
                order.recipientName,
                order.phoneNumber,
                order.province,
                order.district,
                order.ward,
                order.detailAddress,
                order.subtotal,
                order.discountAmount,
                order.shippingFee,
                order.totalAmount,
                order.couponCode,
                order.note,
                order.createdAt,
                items
        );
    }

    // ================= CANCEL ORDER =================

    @Transactional
    public OrderDtos.OrderResponse cancelOrder(
            Authentication authentication,
            Long orderId
    ) {
        Long userId = getUserId(authentication);

        // Khóa đơn hàng để tránh hai yêu cầu hủy cùng lúc.
        CustomerOrder order = orders.findByIdAndUser_IdForUpdate(
                orderId, userId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy đơn hàng."
        ));

        if (!"PENDING".equalsIgnoreCase(order.status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể hủy đơn hàng đang chờ xác nhận."
            );
        }

        if ("PAID".equalsIgnoreCase(order.paymentStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã thanh toán. Vui lòng liên hệ quản trị viên để được hỗ trợ hoàn tiền."
            );
        }

        List<OrderItem> items =
                orderItems.findByOrder_IdOrderByIdAsc(order.id);

        // Gom số lượng theo sản phẩm và biến thể để tránh xử lý trùng.
        Map<Long, Integer> productRestoreQuantities = new LinkedHashMap<>();
        Map<Long, Integer> variantRestoreQuantities = new LinkedHashMap<>();

        Map<Long, ProductVariant> lockedVariants = new LinkedHashMap<>();

        for (OrderItem item : items) {
            if (item.quantity == null || item.quantity <= 0) {
                continue;
            }

            if (item.product != null) {
                productRestoreQuantities.merge(
                        item.product.id,
                        item.quantity,
                        Integer::sum
                );
            }

            if (item.variant != null) {
                Long variantId = item.variant.id;

                ProductVariant variant = lockedVariants.get(variantId);

                if (variant == null) {
                    variant = variants.findById(variantId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Không tìm thấy biến thể để hoàn kho."
                            ));

                    lockedVariants.put(variantId, variant);
                }

                variantRestoreQuantities.merge(
                        variantId,
                        item.quantity,
                        Integer::sum
                );
            }
        }

        // Khóa và hoàn kho tổng của sản phẩm.
        for (Map.Entry<Long, Integer> entry
                : productRestoreQuantities.entrySet()) {

            Product product = products.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Không tìm thấy sản phẩm để hoàn kho."
                    ));

            product.stockQuantity =
                    (product.stockQuantity == null
                            ? 0
                            : product.stockQuantity) + entry.getValue();

            product.soldCount = Math.max(
                    0,
                    (product.soldCount == null
                            ? 0
                            : product.soldCount) - entry.getValue()
            );

            products.save(product);
        }

        // Hoàn kho đúng size/màu.
        for (Map.Entry<Long, Integer> entry
                : variantRestoreQuantities.entrySet()) {

            ProductVariant variant = lockedVariants.get(entry.getKey());

            variant.stockQuantity =
                    (variant.stockQuantity == null
                            ? 0
                            : variant.stockQuantity) + entry.getValue();

            variants.save(variant);
        }

        // Trả lại lượt sử dụng coupon nếu đơn có dùng mã.
        if (order.couponCode != null && !order.couponCode.isBlank()) {
            coupons.findByCodeIgnoreCaseForUpdate(order.couponCode)
                    .ifPresent(coupon -> {
                        int usedCount = coupon.getUsedCount() == null
                                ? 0
                                : coupon.getUsedCount();

                        if (usedCount > 0) {
                            coupon.setUsedCount(usedCount - 1);
                            coupons.save(coupon);
                        }
                    });
        }

        String oldStatus = order.status;
        order.status = "CANCELLED";
        orders.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.order = order;
        history.oldStatus = oldStatus;
        history.newStatus = "CANCELLED";
        history.note = "Khách hàng đã hủy đơn hàng. Hệ thống đã hoàn lại tồn kho.";

        histories.save(history);

        return getOrderResponse(order);
    }

    // ================= STATUS HISTORY =================

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderStatusHistoryResponse> getOrderStatusHistory(
            Authentication authentication,
            Long orderId
    ) {
        Long userId = getUserId(authentication);

        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng."
                ));

        return histories.findByOrder_IdOrderByCreatedAtAsc(order.id)
                .stream()
                .map(history -> new OrderDtos.OrderStatusHistoryResponse(
                        history.oldStatus,
                        history.newStatus,
                        history.note,
                        history.createdAt
                ))
                .toList();
    }
}
