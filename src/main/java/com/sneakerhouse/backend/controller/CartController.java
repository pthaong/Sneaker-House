
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.CartItemRequest;
import com.sneakerhouse.backend.dto.CartItemResponse;
import com.sneakerhouse.backend.dto.CartResponse;
import com.sneakerhouse.backend.service.CartService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Xem giỏ hàng
    // GET /api/cart

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(authentication);
    }

    // Thêm sản phẩm
    // POST /api/cart/items

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addItem(
            Authentication authentication,
            @RequestBody CartItemRequest request
    ) {
        return cartService.addItem(authentication, request);
    }

    // Cập nhật số lượng
    // PUT /api/cart/items/{id}

    @PutMapping("/items/{id}")
    public CartItemResponse updateQuantity(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request
    ) {
        return cartService.updateQuantity(
                authentication,
                id,
                request.get("quantity")
        );
    }

    // Chọn hoặc bỏ chọn sản phẩm
    // PATCH /api/cart/items/{id}/selection

    @PatchMapping("/items/{id}/selection")
    public CartItemResponse updateSelection(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request
    ) {
        return cartService.updateSelection(
                authentication,
                id,
                request.get("selected")
        );
    }

    // Xóa một sản phẩm
    // DELETE /api/cart/items/{id}

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            Authentication authentication,
            @PathVariable Long id
    ) {
        cartService.deleteItem(authentication, id);
    }

    // Xóa toàn bộ giỏ hàng
    // DELETE /api/cart/items

    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Authentication authentication) {
        cartService.clearCart(authentication);
    }
}
