package com.logistics.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.net.URI;
import java.security.Principal;
import java.util.Map;

public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        String userId = "anonymous";
        if (query != null && query.startsWith("userId=")) {
            userId = query.substring(7);
        }
        String finalUserId = userId;
        return () -> finalUserId;

    }
}