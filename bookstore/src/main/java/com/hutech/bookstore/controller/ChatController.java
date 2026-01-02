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
        try {
            User currentUser = getCurrentUser();

            // For now use dummy admin id (should be resolved properly)
            Long adminId = 999L;

            // Generate conversation id same as MessageService.generateConversationId (min_max)
            Long minId = Math.min(currentUser.getId(), adminId);
            Long maxId = Math.max(currentUser.getId(), adminId);
            String conversationId = minId + "_" + maxId;

            // Prepare admin user info (dummy)
            Map<String, Object> adminUser = Map.of(
                    "userId", adminId,
                    "name", "Admin User",
                    "email", "admin@bookstore.com"
            );

            Map<String, Object> data = Map.of(
                    "conversationId", conversationId,
                    "adminUser", adminUser,
                    "staffUser", null
            );

            return ResponseEntity.ok(ApiResponse.success(data, "Conversation created"));
        } catch (Exception e) {
            System.err.println("Exception in getOrCreateConversation: " + e.toString());
            e.printStackTrace();
            String exName = e.getClass().getSimpleName();
            String msg = e.getMessage() != null ? e.getMessage() : "no message";
            // Include exception class in response to aid debugging in development
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Internal server error: " + exName + ": " + msg));
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

}
