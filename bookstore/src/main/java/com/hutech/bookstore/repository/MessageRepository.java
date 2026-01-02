package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Message;
import com.hutech.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(String conversationId);
    Page<Message> findByConversationIdAndIsDeletedFalse(String conversationId, Pageable pageable);

    // Find messages for a user (sent or received)
    Page<Message> findByFromUser_IdOrToUser_IdAndIsDeletedFalse(Long fromUserId, Long toUserId, Pageable pageable);

    // Find unread messages for a user
    List<Message> findByToUser_IdAndIsDeletedFalse(Long toUserId);

    // Count unread messages for a user
    long countByToUser_IdAndIsDeletedFalse(Long toUserId);

    // Find messages for marking as read
    List<Message> findByConversationIdAndToUser_IdAndIsDeletedFalse(String conversationId, Long toUserId);

    // Find distinct users that have chatted with the current user
    @Query("SELECT DISTINCT u FROM User u WHERE u.id IN (" +
           "SELECT DISTINCT m.fromUser.id FROM Message m WHERE m.toUser.id = :userId AND m.isDeleted = false) " +
           "OR u.id IN (" +
           "SELECT DISTINCT m.toUser.id FROM Message m WHERE m.fromUser.id = :userId AND m.isDeleted = false)")
    List<User> findDistinctChatUsers(@Param("userId") Long userId);
}

