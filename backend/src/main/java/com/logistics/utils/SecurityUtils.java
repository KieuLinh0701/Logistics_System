package com.logistics.utils;

import com.logistics.entity.Role;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AccountErrorCode;
import com.logistics.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/api/public/",
            "/ws/",
            "/api/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs"
    );

    public static Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(AccountErrorCode.ACCOUNT_UNAUTHORIZED_ACCESS);
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
            throw new AppException(AccountErrorCode.ACCOUNT_UNAUTHORIZED_ACCESS);
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
            throw new AppException(AccountErrorCode.ACCOUNT_UNAUTHORIZED_ACCESS);
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