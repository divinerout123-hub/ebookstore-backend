package com.bookstore.service.impl;

import com.bookstore.dto.response.UserResponse;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.AccessChecker;
import com.bookstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccessChecker accessChecker;

    @Override
    public UserResponse getUserProfile(Long userId) {
        accessChecker.checkUser(userId);
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
