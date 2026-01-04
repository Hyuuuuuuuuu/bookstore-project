package com.hutech.bookstore.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hutech.bookstore.util.JwtUtil;
import com.hutech.bookstore.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public ChatWebSocketHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract and validate JWT token from query parameters
        String token = UriComponentsBuilder.fromUri(session.getUri())
            .build()
            .getQueryParams()
            .getFirst("token");

        if (token == null || token.isEmpty()) {
            System.out.println("No token provided for WebSocket connection: " + session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
            return;
        }

        // Validate JWT token
        if (!jwtUtil.validateToken(token)) {
            System.out.println("Invalid JWT token for WebSocket connection: " + session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid authentication token"));
            return;
        }

        // Extract user ID from token
        Long userId = jwtUtil.getUserIdFromToken(token);

        // Verify user exists
        if (userRepository.findById(userId).isEmpty()) {
            System.out.println("User not found for WebSocket connection: " + userId);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("User not found"));
            return;
        }

        // Store session with user mapping
        sessions.put(session.getId(), session);
        sessionUserMap.put(session.getId(), userId.toString());

        System.out.println("User " + userId + " connected via WebSocket: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String userId = sessionUserMap.get(session.getId());

        System.out.println("Received message from user " + userId + ": " + payload);

        try {
            // Parse JSON message
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "CHAT";

            if ("PING".equalsIgnoreCase(type)) {
                // Reply with PONG to the sender only (keepalive)
                Map<String, Object> pong = Map.of(
                    "type", "PONG",
                    "timestamp", System.currentTimeMillis()
                );
                String pongJson = objectMapper.writeValueAsString(pong);
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(pongJson));
                }
                return;
            }

            if (!"CHAT".equalsIgnoreCase(type)) {
                // Unknown control type - ignore
                return;
            }

            // Build standardized chat message
            String content = jsonNode.has("content") ? jsonNode.get("content").asText() : "";
            String toUserId = jsonNode.has("toUserId") && !jsonNode.get("toUserId").isNull()
                    ? jsonNode.get("toUserId").asText()
                    : null;
            long timestamp = jsonNode.has("timestamp") ? jsonNode.get("timestamp").asLong() : System.currentTimeMillis();

            Map<String, Object> standardizedMessage = Map.of(
                    "type", "CHAT",
                    "sender", userId,
                    "content", content,
                    "timestamp", timestamp,
                    "toUserId", toUserId
            );

            String jsonMessage = objectMapper.writeValueAsString(standardizedMessage);
            TextMessage chatMsg = new TextMessage(jsonMessage);

            // Send to sender (echo) if open
            if (session.isOpen()) {
                session.sendMessage(chatMsg);
            }

            // Send to recipient(s) matching toUserId
            if (toUserId != null) {
                for (Map.Entry<String, String> entry : sessionUserMap.entrySet()) {
                    String sessId = entry.getKey();
                    String uid = entry.getValue();
                    if (toUserId.equals(uid)) {
                        WebSocketSession recipient = sessions.get(sessId);
                        if (recipient != null && recipient.isOpen()) {
                            recipient.sendMessage(chatMsg);
                        }
                    }
                }
            } else {
                // If no toUserId provided, broadcast to everyone except sender
                for (WebSocketSession s : sessions.values()) {
                    if (s.isOpen() && !s.getId().equals(session.getId())) {
                        s.sendMessage(chatMsg);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());
        sessions.remove(session.getId());
        System.out.println("User " + userId + " disconnected: " + session.getId());
    }
}
