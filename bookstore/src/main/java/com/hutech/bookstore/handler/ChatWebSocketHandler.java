package com.hutech.bookstore.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hutech.bookstore.model.Conversation;
import com.hutech.bookstore.model.Message;
import com.hutech.bookstore.model.Message.SenderType;
import com.hutech.bookstore.repository.ConversationRepository;
import com.hutech.bookstore.repository.MessageRepository;
import com.hutech.bookstore.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSessionManager sessionManager;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatWebSocketHandler(WebSocketSessionManager sessionManager,
                                ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                UserRepository userRepository) {
        this.sessionManager = sessionManager;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing userId"));
            return;
        }
        sessionManager.register(userId, session);
        System.out.println("User " + userId + " connected via WebSocket: " + session.getId() + " role=" + role);
    }

    @Override
    @Transactional
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");
        String payload = message.getPayload();
        System.out.println("Received raw payload from " + userId + ": " + payload);

        JsonNode node = objectMapper.readTree(payload);
        String type = node.has("type") ? node.get("type").asText() : "";
        if (!"CHAT_MESSAGE".equalsIgnoreCase(type)) {
            // ignore non-chat messages
            return;
        }

        Long conversationId = node.has("conversationId") && !node.get("conversationId").isNull() ? node.get("conversationId").asLong() : null;
        String content = node.has("content") ? node.get("content").asText() : "";
        String orderCode = node.has("orderCode") && !node.get("orderCode").isNull() ? node.get("orderCode").asText() : null;

        // Role-specific rules
        if ("USER".equalsIgnoreCase(role)) {
            // If conversationId null -> create or fetch existing open conversation for user
            Conversation conversation;
            if (conversationId == null) {
                conversation = conversationRepository.findByUserIdAndStatus(userId, Conversation.Status.OPEN)
                        .orElseGet(() -> {
                            Conversation c = new Conversation();
                            c.setUserId(userId);
                            c.setStatus(Conversation.Status.OPEN);
                            return conversationRepository.save(c);
                        });
            } else {
                conversation = conversationRepository.findById(conversationId).orElse(null);
                if (conversation == null || !conversation.getUserId().equals(userId)) {
                    // invalid conversation ownership
                    session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Invalid conversation\"}"));
                    return;
                }
            }

            // persist message
            Message msg = new Message();
            msg.setConversation(conversation);
            msg.setSenderType(SenderType.USER);
            msg.setSenderId(userId);
            msg.setContent(content);
            Message saved = messageRepository.save(msg);
            System.out.println("Saved message id=" + saved.getId() + " conversation=" + conversation.getId());

            // build outgoing message
            Map<String, Object> out = Map.ofEntries(
                    Map.entry("type", "CHAT_MESSAGE"),
                    Map.entry("conversationId", conversation.getId()),
                    Map.entry("sender", Map.of("id", userId, "role", "USER")),
                    Map.entry("content", content),
                    Map.entry("timestamp", msg.getCreatedAt().toString()),
                    Map.entry("orderCode", orderCode)
            );
            String outJson = objectMapper.writeValueAsString(out);

            // send to user's sessions (echo) and all online supports
            sessionManager.getSessionsForUser(userId).forEach(s -> {
                try { s.sendMessage(new TextMessage(outJson)); } catch (Exception ignored) {}
            });
            // broadcast to supports: find online users whose role is ADMIN or STAFF
            sessionManager.getAllSessions().forEach(s -> {
                try {
                    Long sid = (Long) s.getAttributes().get("userId");
                    String srole = (String) s.getAttributes().get("role");
                    if (sid != null && srole != null && (srole.equalsIgnoreCase("ADMIN") || srole.equalsIgnoreCase("STAFF"))) {
                        s.sendMessage(new TextMessage(outJson));
                    }
                } catch (Exception ignored) {}
            });

        } else if ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            // staff/admin MUST provide conversationId
            if (conversationId == null) {
                session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"conversationId required for staff\"}"));
                return;
            }
            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Conversation not found\"}"));
                return;
            }
            // persist message as SUPPORT
            Message msg = new Message();
            msg.setConversation(conversation);
            msg.setSenderType(SenderType.SUPPORT);
            msg.setSenderId(userId);
            msg.setContent(content);
            Message saved = messageRepository.save(msg);
            System.out.println("Saved support message id=" + saved.getId() + " conversation=" + conversation.getId());

            Map<String, Object> out = Map.ofEntries(
                    Map.entry("type", "CHAT_MESSAGE"),
                    Map.entry("conversationId", conversation.getId()),
                    Map.entry("sender", Map.of("id", userId, "role", "SUPPORT")),
                    Map.entry("content", content),
                    Map.entry("timestamp", msg.getCreatedAt().toString()),
                    Map.entry("orderCode", orderCode)
            );
            String outJson = objectMapper.writeValueAsString(out);

            // send to user and all supports
            sessionManager.getSessionsForUser(conversation.getUserId()).forEach(s -> {
                try { s.sendMessage(new TextMessage(outJson)); } catch (Exception ignored) {}
            });
            sessionManager.getAllSessions().forEach(s -> {
                try {
                    Long sid = (Long) s.getAttributes().get("userId");
                    String srole = (String) s.getAttributes().get("role");
                    if (sid != null && srole != null && (srole.equalsIgnoreCase("ADMIN") || srole.equalsIgnoreCase("STAFF"))) {
                        s.sendMessage(new TextMessage(outJson));
                    }
                } catch (Exception ignored) {}
            });
        } else {
            // unknown role
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Unauthorized role\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.unregister(userId, session.getId());
            System.out.println("User " + userId + " disconnected: " + session.getId());
        }
    }
}
