package com.bookstore.service;

import com.bookstore.dto.response.UserResponse;

public interface UserService {

    UserResponse getUserProfile(Long userId);
}
