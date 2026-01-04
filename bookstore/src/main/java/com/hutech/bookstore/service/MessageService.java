package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.MessageResponseDTO;
import com.hutech.bookstore.model.Message;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.MessageRepository;
import com.hutech.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    @Transactional
    public Message sendMessage(User fromUser, User toUser, String content) {
        Message message = new Message();
        message.setConversationId(generateConversationId(fromUser.getId(), toUser.getId()));
        message.setFromUser(fromUser);
        message.setToUser(toUser);
        message.setContent(content);
        message.setMessageType(Message.MessageType.TEXT);

        return messageRepository.save(message);
    }

    @Transactional
    public Message sendMessageToAdmin(User fromUser, String content) {
        // Find admin user (assuming admin has role with name "ADMIN")
        // For now, we'll create a dummy admin user - this should be implemented properly
        User admin = new User();
        admin.setId(999L); // Admin user ID
        admin.setName("Admin");
        admin.setEmail("admin@bookstore.com");

        return sendMessage(fromUser, admin, content);
    }

    public List<MessageResponseDTO> getConversationMessages(Long userId1, Long userId2, int page, int limit) {
        String conversationId = generateConversationId(userId1, userId2);
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        Page<Message> messages = messageRepository.findByConversationIdAndIsDeletedFalse(conversationId, pageable);

        return messages.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getUserMessages(Long userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        Page<Message> messages = messageRepository.findByFromUser_IdOrToUser_IdAndIsDeletedFalse(userId, userId, pageable);

        return messages.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getUnreadMessages(Long userId) {
        List<Message> messages = messageRepository.findByToUser_IdAndIsDeletedFalse(userId);

        return messages.stream()
                .filter(message -> message.getToUser() != null && message.getToUser().getId().equals(userId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadMessageCount(Long userId) {
        return messageRepository.countByToUser_IdAndIsDeletedFalse(userId);
    }

    @Transactional
    public void markMessagesAsRead(Long userId, Long fromUserId) {
        String conversationId = generateConversationId(userId, fromUserId);
        List<Message> messages = messageRepository.findByConversationIdAndToUser_IdAndIsDeletedFalse(conversationId, userId);

        // Note: In a real application, you might want to add a 'read' flag to the Message entity
        // For now, we'll just leave this as a placeholder
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();
            // Only allow sender to delete their own messages
            if (message.getFromUser().getId().equals(userId)) {
                message.setIsDeleted(true);
                messageRepository.save(message);
            }
        }
    }

    public List<User> getChatUsers(Long currentUserId) {
        return messageRepository.findDistinctChatUsers(currentUserId);
    }

    private String generateConversationId(Long userId1, Long userId2) {
        // Create consistent conversation ID by sorting user IDs
        Long minId = Math.min(userId1, userId2);
        Long maxId = Math.max(userId1, userId2);
        return minId + "_" + maxId;
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    public List<Map<String, Object>> getAdminConversations(Long adminId, int page, int limit) {
        // Get all conversations where admin is involved
        Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending()); // Get many messages to group
        List<Message> adminMessages = messageRepository.findByFromUser_IdOrToUser_IdAndIsDeletedFalse(adminId, adminId, pageable).getContent();

        // Group by conversationId and get latest message for each
        Map<String, Message> latestMessagesByConversation = adminMessages.stream()
                .collect(Collectors.toMap(
                        Message::getConversationId,
                        msg -> msg,
                        (msg1, msg2) -> msg1.getCreatedAt().isAfter(msg2.getCreatedAt()) ? msg1 : msg2
                ));

        // Convert to response format
        return latestMessagesByConversation.values().stream()
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt())) // Sort by latest first
                .skip((page - 1) * limit)
                .limit(limit)
                .map(message -> {
                    // Determine the other user (not admin) with null checks
                    User fromUser = message.getFromUser();
                    User toUser = message.getToUser();

                    if (fromUser == null || toUser == null) {
                        // Skip messages with missing user data
                        return null;
                    }

                    User otherUser = fromUser.getId().equals(adminId) ? toUser : fromUser;

                    return Map.of(
                            "conversationId", message.getConversationId(),
                            "user", Map.of(
                                    "userId", otherUser.getId(),
                                    "name", otherUser.getName(),
                                    "email", otherUser.getEmail(),
                                    "avatar", otherUser.getAvatar()
                            ),
                            "lastMessage", convertToDTO(message),
                            "lastMessageTime", message.getCreatedAt()
                    );
                })
                .filter(java.util.Objects::nonNull) // Remove null entries
                .collect(Collectors.toList());
    }

    public MessageResponseDTO convertToDTO(Message message) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType().toString());
        dto.setImageUrl(message.getImageUrl());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setUpdatedAt(message.getUpdatedAt());

        if (message.getFromUser() != null) {
            dto.setFromUserId(message.getFromUser().getId());
            dto.setFromUserName(message.getFromUser().getName());
            dto.setFromUserEmail(message.getFromUser().getEmail());
        }

        if (message.getToUser() != null) {
            dto.setToUserId(message.getToUser().getId());
            dto.setToUserName(message.getToUser().getName());
            dto.setToUserEmail(message.getToUser().getEmail());
        }

        return dto;
    }
}
