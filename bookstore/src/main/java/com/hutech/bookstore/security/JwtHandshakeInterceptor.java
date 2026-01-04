package com.hutech.bookstore.security;

import com.hutech.bookstore.repository.UserRepository;
import com.hutech.bookstore.util.JwtUtil;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            String query = ((ServletServerHttpRequest) request).getServletRequest().getQueryString();
            String token = UriComponentsBuilder.fromUriString("?" + (query == null ? "" : query)).build().getQueryParams().getFirst("token");
            if (token == null || !jwtUtil.validateToken(token)) {
                return false;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            attributes.put("userId", userId);
            // Resolve role from DB (safer) if not present in token
            String role = "USER";
            try {
                var userOpt = userRepository.findById(userId);
                if (userOpt.isPresent() && userOpt.get().getRole() != null) {
                    role = userOpt.get().getRole().getName().toUpperCase();
                }
            } catch (Exception ignored) {}
            attributes.put("role", role);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}


