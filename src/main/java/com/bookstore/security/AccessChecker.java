package com.bookstore.security;

import com.bookstore.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Ensures a caller only accesses their own resources (admins may access any). */
@Component
public class AccessChecker {

    public AuthUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new AccessDeniedException("Authentication required");
        }
        return user;
    }

    public void checkUser(Long userId) {
        AuthUser user = currentUser();
        if (user.role() != Role.ADMIN && !user.id().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to access this resource");
        }
    }
}
