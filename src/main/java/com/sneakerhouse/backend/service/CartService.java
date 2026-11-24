
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.CartItemRequest;
import com.sneakerhouse.backend.dto.CartItemResponse;
import com.sneakerhouse.backend.dto.CartResponse;
import com.sneakerhouse.backend.entity.Cart;
import com.sneakerhouse.backend.entity.CartItem;
import com.sneakerhouse.backend.entity.Product;
import com.sneakerhouse.backend.repository.CartItemRepository;
import com.sneakerhouse.backend.repository.CartRepository;
import com.sneakerhouse.backend.repository.ProductRepository;
import com.sneakerhouse.backend.repository.UserAccountRepository;
import com.sneakerhouse.backend.security.CustomUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final UserAccountRepository users;

    public CartService(
            CartRepository carts,
            CartItemRepository cartItems,
            ProductRepository products,
            UserAccountRepository users
    ) {
        this.carts = carts;
        this.cartItems = cartItems;
        this.products = products;
        this.users = users;
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập"
            );
        }

        return user.getId();
    }

    private Cart getOrCreateCart(Long userId) {
        return carts.findByUser_Id(userId).orElseGet(() -> {
            var user = users.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"
                    ));

            Cart cart = new Cart();
            cart.user = user;
            return carts.save(cart);
        });
    }

    private Product getActiveProduct(Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"
                ));

        if (!Boolean.TRUE.equals(product.active)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Sản phẩm hiện không kinh doanh"
            );
        }

        return product;
    }

    private void validateVariant(String requested, String available, String label) {
        if (available == null || available.isBlank()) {
            if (requested != null && !requested.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        label + " không hợp lệ với sản phẩm"
                );
            }
            return;
        }

        if (requested == null || requested.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn " + label
            );
        }

        boolean valid = Arrays.stream(available.split(","))
                .map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(requested.trim()));

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " không tồn tại trong sản phẩm"
            );
        }
    }

    public CartResponse getCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        Cart cart = getOrCreateCart(userId);

        List<CartItem> items =
                cartItems.findByCart_IdOrderByIdDesc(cart.id);

        List<CartItemResponse> result = items.stream()
                .map(CartItemResponse::from)
                .toList();

        int totalItems = items.stream()
                .mapToInt(item -> item.quantity)
                .sum();

        List<CartItem> selected = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.selected))
                .toList();

        int selectedItems = selected.stream()
                .mapToInt(item -> item.quantity)
                .sum();

        BigDecimal subtotal = selected.stream()
                .map(item -> CartItemResponse.from(item).lineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.id,
                result,
                totalItems,
                selectedItems,
                subtotal
        );
    }

    public CartItemResponse addItem(
            Authentication authentication,
            CartItemRequest request
    ) {
        if (request.productId() == null
                || request.quantity() == null
                || request.quantity() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm và số lượng không hợp lệ"
            );
        }

        Long userId = getUserId(authentication);
        Cart cart = getOrCreateCart(userId);
        Product product = getActiveProduct(request.productId());

        validateVariant(request.size(), product.sizes, "Size");
        validateVariant(request.color(), product.colors, "Màu sắc");

        if (product.stockQuantity == null
                || product.stockQuantity < request.quantity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng sản phẩm trong kho không đủ"
            );
        }

        CartItem item = cartItems
                .findByCart_IdAndProduct_IdAndSizeAndColor(
                        cart.id,
                        product.id,
                        request.size(),
                        request.color()
                )
                .orElse(null);

        int newQuantity = request.quantity();

        if (item != null) {
            newQuantity += item.quantity;
        } else {
            item = new CartItem();
            item.cart = cart;
            item.product = product;
            item.size = request.size();
            item.color = request.color();
            item.selected = true;
        }

        if (newQuantity > product.stockQuantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tổng số lượng trong giỏ vượt quá tồn kho"
            );
        }

        item.quantity = newQuantity;

        return CartItemResponse.from(cartItems.save(item));
    }

    public CartItemResponse updateQuantity(
            Authentication authentication,
            Long itemId,
            Integer quantity
    ) {
        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng phải lớn hơn 0"
            );
        }

        Long userId = getUserId(authentication);
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItems.findByIdAndCart_Id(itemId, cart.id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm trong giỏ hàng"
                ));

        if (item.product.stockQuantity == null
                || quantity > item.product.stockQuantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng vượt quá tồn kho"
            );
        }

        item.quantity = quantity;
        return CartItemResponse.from(cartItems.save(item));
    }

    public CartItemResponse updateSelection(
            Authentication authentication,
            Long itemId,
            Boolean selected
    ) {
        if (selected == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái chọn không hợp lệ"
            );
        }

        Long userId = getUserId(authentication);
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItems.findByIdAndCart_Id(itemId, cart.id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm trong giỏ hàng"
                ));

        item.selected = selected;
        return CartItemResponse.from(cartItems.save(item));
    }

    public void deleteItem(Authentication authentication, Long itemId) {
        Long userId = getUserId(authentication);
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItems.findByIdAndCart_Id(itemId, cart.id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm trong giỏ hàng"
                ));

        cartItems.delete(item);
    }

    public void clearCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        Cart cart = getOrCreateCart(userId);

        List<CartItem> items =
                cartItems.findByCart_IdOrderByIdDesc(cart.id);

        cartItems.deleteAll(items);
    }
}
