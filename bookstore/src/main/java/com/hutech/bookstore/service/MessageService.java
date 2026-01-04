package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.MessageResponseDTO;
import com.hutech.bookstore.model.Conversation;
import com.hutech.bookstore.model.Message;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.ConversationRepository;
import com.hutech.bookstore.repository.MessageRepository;
import com.hutech.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    @Transactional
    public Message sendMessage(User fromUser, User toUser, String content) {
        // Determine conversation owner (the regular user)
        Long ownerId = fromUser.getRole() != null && "user".equalsIgnoreCase(fromUser.getRole().getName()) ? fromUser.getId() : toUser.getId();
        Conversation conv = conversationRepository.findByUserId(ownerId).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setUserId(ownerId);
            c.setStatus(Conversation.Status.OPEN);
            return conversationRepository.save(c);
        });

        Message message = new Message();
        message.setConversation(conv);
        message.setSenderType(fromUser.getRole() != null && "user".equalsIgnoreCase(fromUser.getRole().getName()) ? Message.SenderType.USER : Message.SenderType.SUPPORT);
        message.setSenderId(fromUser.getId());
        message.setContent(content);
        return messageRepository.save(message);
    }

    @Transactional
    public Message sendMessageToAdmin(User fromUser, String content) {
        // Ensure conversation exists for this user
        Conversation conv = conversationRepository.findByUserId(fromUser.getId()).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setUserId(fromUser.getId());
            c.setStatus(Conversation.Status.OPEN);
            return conversationRepository.save(c);
        });
        Message message = new Message();
        message.setConversation(conv);
        message.setSenderType(Message.SenderType.USER);
        message.setSenderId(fromUser.getId());
        message.setContent(content);
        return messageRepository.save(message);
    }

    public List<MessageResponseDTO> getConversationMessagesByConversationId(Long conversationId) {
        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (convOpt.isEmpty()) return List.of();
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(convOpt.get());
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getUserMessages(Long userId) {
        Optional<Conversation> convOpt = conversationRepository.findByUserId(userId);
        if (convOpt.isEmpty()) return List.of();
        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(convOpt.get());
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getUnreadMessages(Long userId) {
        // Not implemented (no read flag). Return empty.
        return List.of();
    }

    public long getUnreadMessageCount(Long userId) {
        return 0L;
    }

    @Transactional
    public void markMessagesAsRead(Long userId, Long fromUserId) {
        // No-op for now (schema doesn't include read flag)
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();
            if (message.getSenderId() != null && message.getSenderId().equals(userId)) {
                message.setIsDeleted(true);
                messageRepository.save(message);
            }
        }
    }

    public List<User> getChatUsers(Long currentUserId) {
        // Not implemented in new schema; return empty
        return List.of();
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    public List<Map<String, Object>> getAdminConversations(Long adminId, int page, int limit) {
        // Build admin view: list conversations with last message
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations.stream().map(conv -> {
            // Get user info
            User user = userRepository.findById(conv.getUserId()).orElse(null);

            // Get last message
            List<Message> msgs = messageRepository.findByConversationOrderByCreatedAtAsc(conv);
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);

            // Build user map
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", conv.getUserId());
            if (user != null) {
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                userMap.put("avatar", user.getAvatar());
            }

            // Build conversation map
            Map<String, Object> convMap = new HashMap<>();
            convMap.put("conversationId", conv.getId());
            convMap.put("user", userMap);
            convMap.put("status", conv.getStatus().toString());
            if (last != null) {
                convMap.put("lastMessage", last.getContent());
                convMap.put("lastMessageTime", last.getCreatedAt());
            } else {
                convMap.put("lastMessage", null);
                convMap.put("lastMessageTime", conv.getCreatedAt());
            }

            return convMap;
        }).collect(Collectors.toList());
    }

    public MessageResponseDTO convertToDTO(Message message) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation() != null ? String.valueOf(message.getConversation().getId()) : null);
        dto.setContent(message.getContent());
        dto.setMessageType(message.getSenderType() != null ? message.getSenderType().toString() : null);
        dto.setImageUrl(null);
        dto.setCreatedAt(message.getCreatedAt());
        dto.setUpdatedAt(message.getUpdatedAt());

        dto.setFromUserId(message.getSenderId());
        dto.setFromUserName(null);
        dto.setFromUserEmail(null);
        dto.setToUserId(null);
        dto.setToUserName(null);
        dto.setToUserEmail(null);

        return dto;
    }
}
