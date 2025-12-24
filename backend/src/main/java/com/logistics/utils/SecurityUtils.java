package com.logistics.utils;

import com.logistics.security.UserPrincipal;

import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

        public static final Map<String, List<String>> PATH_ROLES = Map.of(
            "/api/user/", List.of("User", "Driver", "Shipper", "Manager", "Admin"),
            "/api/manager/", List.of("Manager"),
            "/api/admin/", List.of("Admin"),
            "/api/shipper/", List.of("Shipper"),
            "/api/driver/", List.of("Driver")
        );

    public static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/api/public/",
            "/ws/"
    );

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

    public static String getAuthenticatedUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.getCurrentRole() != null) {
            return principal.getCurrentRole().getName();
        }
        return null;
    }

    public static boolean hasRole(String roleName) {
        try {
            String userRole = getAuthenticatedUserRole();
            return roleName.equalsIgnoreCase(userRole);
        } catch (Exception e) {
            return false;
        }
    }
}