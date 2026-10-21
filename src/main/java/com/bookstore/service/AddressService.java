package com.bookstore.service;

import com.bookstore.dto.request.AddressRequest;
import com.bookstore.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    AddressResponse addAddress(AddressRequest request);

    List<AddressResponse> getUserAddresses(Long userId);
}
