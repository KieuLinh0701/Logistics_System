package com.logistics.config;

import com.logistics.entity.Account;
import com.logistics.entity.Role;
import com.logistics.entity.User;
import com.logistics.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Key key;

    public JwtAuthenticationFilter(
            @Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();

                Object accountObj = claims.get("account");
                Object userObj = claims.get("user");

                @SuppressWarnings("unchecked")
                Map<String, Object> accountMap = (accountObj instanceof Map<?, ?>) ? (Map<String, Object>) accountObj
                        : null;

                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (userObj instanceof Map<?, ?>) ? (Map<String, Object>) userObj : null;

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null
                        && accountMap != null) {

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    String roleName = null;
                    if (accountMap.get("role") != null) {
                        roleName = (String) accountMap.get("role");
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    }

                    Account account = new Account();
                    if (accountMap.get("id") != null) {
                        account.setId(((Number) accountMap.get("id")).intValue());
                    }

                    if (roleName != null) {
                        Role role = new Role();
                        role.setName(roleName);
                        account.setRole(role);
                    }

                    User user = new User();
                    if (userMap != null && userMap.get("id") != null) {
                        user.setId(((Number) userMap.get("id")).intValue());
                    }

                    UserPrincipal principal = new UserPrincipal(account, user, authorities);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(principal,
                            null, authorities);

                    Map<String, Object> details = new HashMap<>();
                    details.put("request", new WebAuthenticationDetailsSource().buildDetails(request));
                    details.put("account", account);
                    details.put("user", user);
                    authToken.setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}