package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserIdAndStatus(Long userId, Conversation.Status status);
    Optional<Conversation> findByUserId(Long userId);
}


