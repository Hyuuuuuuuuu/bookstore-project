package com.hutech.bookstore.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WebSocketSessionManager {
    // userId -> set of sessionIds
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessions.put(session.getId(), session);
        userSessions.compute(userId, (k, v) -> {
            Set<String> s = (v == null) ? ConcurrentHashMap.newKeySet() : v;
            s.add(session.getId());
            return s;
        });
    }

    public void unregister(Long userId, String sessionId) {
        sessions.remove(sessionId);
        userSessions.computeIfPresent(userId, (k, v) -> {
            v.remove(sessionId);
            return v.isEmpty() ? null : v;
        });
    }

    public Set<WebSocketSession> getSessionsForUser(Long userId) {
        Set<String> ids = userSessions.getOrDefault(userId, Collections.emptySet());
        return ids.stream().map(sessions::get).filter(s -> s != null && s.isOpen()).collect(Collectors.toSet());
    }

    public Set<WebSocketSession> getAllSessions() {
        return sessions.values().stream().filter(s -> s != null && s.isOpen()).collect(Collectors.toSet());
    }
}


