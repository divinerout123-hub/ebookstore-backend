package com.bookstore.service.impl;

import com.bookstore.dto.request.AddressRequest;
import com.bookstore.dto.response.AddressResponse;
import com.bookstore.entity.Address;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.AddressRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.AccessChecker;
import com.bookstore.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AccessChecker accessChecker;

    @Override
    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        accessChecker.checkUser(request.userId());
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.userId()));
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            addressRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                    .filter(a -> a.getType() == request.type() && a.isDefaultAddress())
                    .forEach(a -> a.setDefaultAddress(false));
        }

        Address saved = addressRepository.save(Address.builder()
                .user(user)
                .type(request.type())
                .fullName(request.fullName())
                .phone(request.phone())
                .line1(request.line1())
                .line2(request.line2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country().toUpperCase())
                .defaultAddress(makeDefault)
                .build());
        return AddressResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        accessChecker.checkUser(userId);
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.of("User", userId);
        }
        return addressRepository.findByUserIdOrderByIdAsc(userId).stream().map(AddressResponse::from).toList();
    }
}
