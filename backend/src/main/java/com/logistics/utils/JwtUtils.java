package com.logistics.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.logistics.entity.Account;
import com.logistics.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

        private final Key key;
        private final long jwtExpirationMs;

        public JwtUtils(
                        @Value("${jwt.secret}") String secret,
                        @Value("${jwt.expiration}") long jwtExpirationMs) {
                this.key = Keys.hmacShaKeyFor(secret.getBytes());
                this.jwtExpirationMs = jwtExpirationMs;
        }

        public String generateToken(Account account, User user) {
                Date now = new Date();
                Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());

                Map<String, Object> accountMap = new HashMap<>();
                accountMap.put("id", account.getId());
                accountMap.put("email", account.getEmail());
                accountMap.put("role", account.getRole() != null ? account.getRole().getName() : null);

                return Jwts.builder()
                                .setSubject(account.getEmail())
                                .claim("user", userMap)
                                .claim("account", accountMap)
                                .setIssuedAt(now)
                                .setExpiration(expiryDate)
                                .signWith(key)
                                .compact();
        }
}