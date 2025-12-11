package com.logistics.config;

import com.logistics.entity.User;
import com.logistics.repository.UserRepository;
import com.logistics.utils.SecurityUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RoleCheckFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (SecurityUtils.PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(e.getMessage());
            return;
        }

        request.setAttribute("currentUserId", userId);

        boolean isNotificationApi = path.equals("/api/notifications") || path.startsWith("/api/notifications/");

        if (isNotificationApi) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByIdWithRoles(userId).orElse(null);
        if (user == null || user.getAccount() == null || user.getAccount().getAccountRoles() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User hoặc account không tồn tại");
            return;
        }

        String currentRoleName = SecurityUtils.getAuthenticatedUserRole();
        if (currentRoleName == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Role hiện tại không tồn tại");
            return;
        }

        boolean roleBelongsToAccount = user.getAccount().getAccountRoles().stream()
        .anyMatch(ar -> ar.getRole() != null 
                    && ar.getIsActive()       
                    && currentRoleName.equals(ar.getRole().getName()));

        if (!roleBelongsToAccount) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Role không thuộc account này");
            return;
        }

        request.setAttribute("currentRoleName", currentRoleName);

        boolean authorized = SecurityUtils.PATH_ROLES.entrySet().stream()
            .filter(entry -> path.startsWith(entry.getKey()))
            .anyMatch(entry -> entry.getValue().contains(currentRoleName));

        if (!authorized) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {
                      "success": false,
                      "message": "Bạn không có quyền truy cập chức năng này."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}