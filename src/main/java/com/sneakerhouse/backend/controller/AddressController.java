
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.AddressDtos.AddressRequest;
import com.sneakerhouse.backend.dto.AddressDtos.AddressResponse;
import com.sneakerhouse.backend.service.AddressService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddressResponse> getAddresses(
            Authentication authentication
    ) {
        return service.getAddresses(authentication);
    }

    @GetMapping("/{id}")
    public AddressResponse getAddress(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.getAddress(id, authentication);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAddress(request, authentication));
    }

    @PutMapping("/{id}")
    public AddressResponse updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication
    ) {
        return service.updateAddress(id, request, authentication);
    }

    @PatchMapping("/{id}/default")
    public AddressResponse setDefault(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.setDefault(id, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAddress(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.deleteAddress(id, authentication);

        return ResponseEntity.ok(
                Map.of("message", "Xóa địa chỉ thành công.")
        );
    }
}
