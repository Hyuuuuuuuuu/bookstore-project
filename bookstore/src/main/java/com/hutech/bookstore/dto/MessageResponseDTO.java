package com.hutech.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {
    private Long id;
    private String conversationId;
    private Long fromUserId;
    private String fromUserName;
    private String fromUserEmail;
    private Long toUserId;
    private String toUserName;
    private String toUserEmail;
    private String content;
    private String messageType;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
