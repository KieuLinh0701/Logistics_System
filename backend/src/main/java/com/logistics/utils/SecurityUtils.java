package com.logistics.utils;

import com.logistics.entity.Role;
import com.logistics.security.UserPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static final List<String> PUBLIC_PATHS = List.of("/api/auth/", "/api/public/", "/ws/", "/api/health");

    public static Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getUser()
                .getId();
    }

    public static Integer getAuthenticatedAccountId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getAccount()
                .getId();
    }

    public static Role getAuthenticatedUserRole() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.getCurrentRole() != null) {
            return principal.getCurrentRole();
        }
        return null;
    }

    public static boolean hasRole(String roleName) {
        try {
            String userRole = Objects.requireNonNull(getAuthenticatedUserRole())
                    .getName();
            return roleName.equalsIgnoreCase(userRole);
        } catch (Exception e) {
            return false;
        }
    }
}