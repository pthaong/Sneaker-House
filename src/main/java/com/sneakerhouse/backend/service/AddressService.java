
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.AddressDtos.AddressRequest;
import com.sneakerhouse.backend.dto.AddressDtos.AddressResponse;
import com.sneakerhouse.backend.entity.Address;
import com.sneakerhouse.backend.repository.AddressRepository;
import com.sneakerhouse.backend.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addresses;
    private final UserAccountRepository users;

    public AddressService(
            AddressRepository addresses,
            UserAccountRepository users
    ) {
        this.addresses = addresses;
        this.users = users;
    }

    private Long getUserId(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản."
                ))
                .getId();
    }

    private Address getOwnedAddress(Long id, Long userId) {
        return addresses.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy địa chỉ."
                ));
    }

    private void setDefaultAddress(Long userId, Long selectedId) {
        List<Address> list =
                addresses.findByUser_IdOrderByDefaultAddressDescCreatedAtDesc(
                        userId
                );

        for (Address address : list) {
            address.setDefaultAddress(
                    address.getId().equals(selectedId)
            );
        }

        addresses.saveAll(list);
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setRecipientName(request.recipientName().trim());
        address.setPhoneNumber(request.phoneNumber().trim());
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setWard(request.ward().trim());
        address.setDetailAddress(request.detailAddress().trim());
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getProvince(),
                address.getDistrict(),
                address.getWard(),
                address.getDetailAddress(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Authentication authentication) {
        Long userId = getUserId(authentication);

        return addresses
                .findByUser_IdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddress(
            Long id,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        return toResponse(getOwnedAddress(id, userId));
    }

    @Transactional
    public AddressResponse createAddress(
            AddressRequest request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        var user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản."
                ));

        Address address = new Address();
        address.setUser(user);
        applyRequest(address, request);

        boolean firstAddress =
                addresses.findByUser_IdOrderByDefaultAddressDescCreatedAtDesc(
                        userId
                ).isEmpty();

        boolean makeDefault =
                Boolean.TRUE.equals(request.defaultAddress()) || firstAddress;

        address.setDefaultAddress(false);
        address = addresses.save(address);

        if (makeDefault) {
            setDefaultAddress(userId, address.getId());
        }

        return toResponse(addresses.findByIdAndUser_Id(
                address.getId(), userId
        ).orElseThrow());
    }

    @Transactional
    public AddressResponse updateAddress(
            Long id,
            AddressRequest request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        Address address = getOwnedAddress(id, userId);

        applyRequest(address, request);

        if (Boolean.TRUE.equals(request.defaultAddress())) {
            setDefaultAddress(userId, id);
        }

        return toResponse(addresses.save(address));
    }

    @Transactional
    public AddressResponse setDefault(
            Long id,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        getOwnedAddress(id, userId);

        setDefaultAddress(userId, id);

        return toResponse(getOwnedAddress(id, userId));
    }

    @Transactional
    public void deleteAddress(
            Long id,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        Address address = getOwnedAddress(id, userId);

        boolean wasDefault = address.isDefaultAddress();

        addresses.delete(address);

        if (wasDefault) {
            addresses.findFirstByUser_IdOrderByCreatedAtAsc(userId)
                    .ifPresent(next -> {
                        setDefaultAddress(userId, next.getId());
                    });
        }
    }
}
