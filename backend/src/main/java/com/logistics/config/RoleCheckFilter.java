package com.logistics.config;

import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.repository.UserRepository;
import com.logistics.utils.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
        String requestMethod = request.getMethod();

        // Public: nộp đơn tuyển dụng
        if ("POST".equalsIgnoreCase(requestMethod) && "/api/job-applications".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Public paths (vd: /api/auth/**, /api/public/**)
        if (SecurityUtils.PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Public: xem danh sách job
        if ("GET".equalsIgnoreCase(requestMethod)
                && ("/api/jobs".equals(path) || path.startsWith("/api/jobs/"))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Lấy userId từ token
        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(e.getMessage());
            return;
        }

        request.setAttribute("currentUserId", userId);

        // Notification: chỉ cần đăng nhập, không cần check permission
        if (path.equals("/api/notifications") || path.startsWith("/api/notifications/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Kiểm tra user + account tồn tại
        User user = userRepository.findByIdWithRoles(userId).orElse(null);
        if (user == null || user.getAccount() == null || user.getAccount().getAccountRoles() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("User hoặc account không tồn tại");
            return;
        }

        // Kiểm tra role hợp lệ
        Role currentRole = SecurityUtils.getAuthenticatedUserRole();
        if (currentRole == null || currentRole.getId() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Role hiện tại không tồn tại");
            return;
        }

        Integer currentRoleId = currentRole.getId();

        // Kiểm tra role có thuộc account và đang active không
        boolean roleBelongsToAccount = user.getAccount().getAccountRoles().stream()
                .anyMatch(ar -> ar.getRole() != null
                        && ar.getIsActive()
                        && currentRoleId.equals(ar.getRole().getId()));

        if (!roleBelongsToAccount) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Role không thuộc account này");
            return;
        }

        request.setAttribute("currentRoleName", currentRole.getName());
        request.setAttribute("currentRoleId", currentRoleId);

        // Kiểm tra permission theo url + method từ authorities đã load trong JWT filter
        AntPathMatcher matcher = new AntPathMatcher();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean hasPermission = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.contains("::"))
                .anyMatch(a -> {
                    String[] parts = a.split("::", 2);
                    if (!parts[0].equalsIgnoreCase(requestMethod)) {
                        return false;
                    }
                    String permUrl = parts[1].trim();
                    return matcher.match(permUrl, path)
                            || matcher.match(permUrl, path.replaceFirst("^/api", ""))
                            || matcher.match(permUrl.replaceFirst("^/api", ""), path);
                });

        if (!hasPermission) {
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