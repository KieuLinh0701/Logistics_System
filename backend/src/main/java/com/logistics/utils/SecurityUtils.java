package com.logistics.utils;

import com.logistics.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    public static Integer getAuthenticatedAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getAccount().getId();
    }
}