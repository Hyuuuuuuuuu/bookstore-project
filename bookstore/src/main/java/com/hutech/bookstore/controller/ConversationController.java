package com.hutech.bookstore.controller;

import com.hutech.bookstore.model.Conversation;
import com.hutech.bookstore.model.Message;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.ConversationRepository;
import com.hutech.bookstore.repository.MessageRepository;
import com.hutech.bookstore.repository.UserRepository;
import com.hutech.bookstore.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hutech.bookstore.service.MessageService;
import com.hutech.bookstore.util.ApiResponse;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public ConversationController(ConversationRepository conversationRepository,
                                  MessageRepository messageRepository,
                                  JwtUtil jwtUtil,
                                  UserRepository userRepository,
                                  MessageService messageService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    // Admin endpoint: list conversations with last message
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminConversations(@RequestHeader("Authorization") String authHeader,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "50") int limit) {
        try {
            String token = authHeader.replaceFirst("Bearer ", "");
            if (!jwtUtil.validateToken(token)) return ResponseEntity.status(401).build();
            Long userId = jwtUtil.getUserIdFromToken(token);
            // Check role
            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
            String role = userOpt.get().getRole() != null ? userOpt.get().getRole().getName() : "user";
            if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
                return ResponseEntity.status(403).build();
            }

            List<Map<String, Object>> convs = messageService.getAdminConversations(userId, page, limit);
            Map<String, Object> data = new HashMap<>();
            data.put("conversations", convs);
            data.put("page", page);
            data.put("limit", limit);
            data.put("total", convs.size());
            return ResponseEntity.ok(ApiResponse.success(data, "Admin conversations retrieved"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, e.getMessage()));
        }
    }

    // GET /api/conversations/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyConversation(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replaceFirst("Bearer ", "");
            if (!jwtUtil.validateToken(token)) return ResponseEntity.status(401).build();
            Long userId = jwtUtil.getUserIdFromToken(token);
            Conversation conv = conversationRepository.findByUserIdAndStatus(userId, Conversation.Status.OPEN).orElse(null);
            if (conv == null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("conversationId", null);
                return ResponseEntity.ok(Map.of("data", payload));
            }
            return ResponseEntity.ok(Map.of("data", Map.of("conversationId", conv.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/conversations/{id}/messages
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable("id") Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replaceFirst("Bearer ", "");
            if (!jwtUtil.validateToken(token)) return ResponseEntity.status(401).build();
            Long userId = jwtUtil.getUserIdFromToken(token);
            Conversation conv = conversationRepository.findById(id).orElse(null);
            if (conv == null) return ResponseEntity.status(404).build();
            // Only allow user or support/admin to read
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(401).build();
            // If the requester is USER, must own conversation
            String role = user.getRole() != null ? user.getRole().getName() : "USER";
            if ("USER".equalsIgnoreCase(role) && !conv.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conv);
            return ResponseEntity.ok(Map.of("data", Map.of("messages", messages)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}


