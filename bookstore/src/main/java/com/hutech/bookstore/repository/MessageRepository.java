package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Message;
import com.hutech.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(String conversationId);
    Page<Message> findByConversationIdAndIsDeletedFalse(String conversationId, Pageable pageable);
    List<Message> findByToUserAndIsDeletedFalse(User toUser);
}

