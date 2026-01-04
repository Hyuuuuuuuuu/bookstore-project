package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.MessageResponseDTO;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.MessageService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final com.hutech.bookstore.repository.UserRepository userRepository;

    // REST API endpoints for chat functionality

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            Principal principal) {

        User currentUser = getCurrentUser();
        List<MessageResponseDTO> messages = messageService.getUserMessages(currentUser.getId(), page, limit);

        Map<String, Object> data = Map.of(
                "messages", messages,
                "page", page,
                "limit", limit,
                "total", messages.size()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "Messages retrieved successfully"));
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            Principal principal) {

        User currentUser = getCurrentUser();
        List<MessageResponseDTO> messages = messageService.getConversationMessages(
                currentUser.getId(), userId, page, limit);

        Map<String, Object> data = Map.of(
                "messages", messages,
                "page", page,
                "limit", limit,
                "total", messages.size()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "Conversation retrieved successfully"));
    }

    // Get or create conversation for current user (with admin/support)
    @GetMapping("/conversation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrCreateConversation(Principal principal) {
        User currentUser = getCurrentUser();
        try {

            // Try to find admin user from database; fallback to hard-coded admin if not available
            Long adminId = 999L;
            Map<String, Object> adminUser;
            try {
                User adminUserEntity = userRepository.findByEmailAndIsDeletedFalse("admin@bookstore.com")
                        .orElse(null);
                if (adminUserEntity != null) {
                    adminId = adminUserEntity.getId();
                    java.util.Map<String, Object> tmp = new java.util.HashMap<>();
                    tmp.put("userId", adminUserEntity.getId());
                    tmp.put("name", adminUserEntity.getName());
                    tmp.put("email", adminUserEntity.getEmail());
                    tmp.put("avatar", adminUserEntity.getAvatar());
                    adminUser = tmp;
                } else {
                    java.util.Map<String, Object> tmp = new java.util.HashMap<>();
                    tmp.put("userId", adminId);
                    tmp.put("name", "Admin User");
                    tmp.put("email", "admin@bookstore.com");
                    tmp.put("avatar", null);
                    adminUser = tmp;
                }
            } catch (Exception ex) {
                // If repository is unavailable or any other error, fallback to defaults
                adminUser = Map.of(
                        "userId", adminId,
                        "name", "Admin User",
                        "email", "admin@bookstore.com",
                        "avatar", null
                );
            }

            // Generate conversation id same as MessageService.generateConversationId (min_max)
            Long minId = Math.min(currentUser.getId(), adminId);
            Long maxId = Math.max(currentUser.getId(), adminId);
            String conversationId = minId + "_" + maxId;

                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("conversationId", conversationId);
                data.put("adminUser", adminUser);
                data.put("staffUser", null);

            return ResponseEntity.ok(ApiResponse.success(data, "Conversation created"));
        } catch (Exception e) {
            // Log full exception for debugging
            System.err.println("Exception in getOrCreateConversation: " + e.toString());
            e.printStackTrace();

            // Fallback: return a safe default conversation so frontend can continue to work
            try {
                Long fallbackAdminId = 999L;
                Long minId = Math.min(currentUser.getId(), fallbackAdminId);
                Long maxId = Math.max(currentUser.getId(), fallbackAdminId);
                String conversationId = minId + "_" + maxId;

                Map<String, Object> adminUser = Map.of(
                        "userId", fallbackAdminId,
                        "name", "Admin User",
                        "email", "admin@bookstore.com"
                );

                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("conversationId", conversationId);
                data.put("adminUser", adminUser);
                data.put("staffUser", null);

                System.err.println("getOrCreateConversation: returning fallback conversation due to error");
                return ResponseEntity.ok(ApiResponse.success(data, "Conversation created (fallback due to error)"));
            } catch (Exception ex) {
                // If fallback also fails, return generic error
                System.err.println("Fallback in getOrCreateConversation also failed: " + ex.toString());
                String exName = e.getClass().getSimpleName();
                String msg = e.getMessage() != null ? e.getMessage() : "no message";
                return ResponseEntity.status(500).body(ApiResponse.error(500, "Internal server error: " + exName + ": " + msg));
            }
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getChatUsers(Principal principal) {
        User currentUser = getCurrentUser();
        List<User> chatUsers = messageService.getChatUsers(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(chatUsers, "Chat users retrieved successfully"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Principal principal) {
        User currentUser = getCurrentUser();
        long unreadCount = messageService.getUnreadMessageCount(currentUser.getId());

        Map<String, Object> data = Map.of("unreadCount", unreadCount);

        return ResponseEntity.ok(ApiResponse.success(data, "Unread count retrieved successfully"));
    }

    @PostMapping("/mark-read/{userId}")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long userId,
            Principal principal) {

        User currentUser = getCurrentUser();
        messageService.markMessagesAsRead(currentUser.getId(), userId);

        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", "Messages marked as read"));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @PathVariable Long messageId,
            Principal principal) {

        User currentUser = getCurrentUser();
        messageService.deleteMessage(messageId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Message deleted", "Message deleted successfully"));
    }

    // Admin endpoints
    @GetMapping("/admin/chats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {

        try {
            // Find admin user from database
            User adminUser = userRepository.findByEmailAndIsDeletedFalse("admin@bookstore.com")
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));

            List<Map<String, Object>> conversations = messageService.getAdminConversations(adminUser.getId(), page, limit);
            // If no conversations (no messages yet), fall back to list of users who chatted with admin
            if (conversations == null || conversations.isEmpty()) {
                List<User> users = messageService.getChatUsers(adminUser.getId());
                Long adminUid = adminUser.getId();
                conversations = users.stream().map(u -> {
                    java.util.Map<String, Object> conv = new java.util.HashMap<>();
                    conv.put("conversationId", generateConversationId(u.getId(), adminUid));
                    conv.put("user", java.util.Map.of(
                            "userId", u.getId(),
                            "name", u.getName(),
                            "email", u.getEmail(),
                            "avatar", u.getAvatar()
                    ));
                    conv.put("lastMessage", null);
                    conv.put("lastMessageTime", null);
                    return conv;
                }).collect(java.util.stream.Collectors.toList());
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("conversations", conversations);
            data.put("page", page);
            data.put("limit", limit);
            data.put("total", conversations.size());

            return ResponseEntity.ok(ApiResponse.success(data, "Admin conversations retrieved successfully"));
        } catch (Exception e) {
            System.err.println("Exception in getAdminConversations: " + e.toString());
            e.printStackTrace();
            String exName = e.getClass().getSimpleName();
            String msg = e.getMessage() != null ? e.getMessage() : "no message";
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Internal server error: " + exName + ": " + msg));
        }
    }

    // Helper method to get current user from Principal
    private User getCurrentUser() {
        // This is a simplified implementation. In real application,
        // you should inject UserRepository and get user by email/username
        // For now, we'll create a dummy user - this should be replaced with proper authentication
        User user = new User();
        user.setId(1L); // This should come from JWT token
        user.setName("Current User");
        user.setEmail("user@example.com");
        return user;
    }

    // Local conversation id generator (same logic as MessageService)
    private String generateConversationId(Long userId1, Long userId2) {
        Long minId = Math.min(userId1, userId2);
        Long maxId = Math.max(userId1, userId2);
        return minId + "_" + maxId;
    }

}
