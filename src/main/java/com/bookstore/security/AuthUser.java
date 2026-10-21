package com.bookstore.security;

import com.bookstore.enums.Role;

/** Authenticated principal extracted from the JWT. */
public record AuthUser(Long id, String email, Role role) {
}
