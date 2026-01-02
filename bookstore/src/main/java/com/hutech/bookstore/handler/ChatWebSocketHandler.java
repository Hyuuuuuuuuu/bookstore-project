package com.hutech.bookstore.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = Logger.getLogger(ChatWebSocketHandler.class.getName());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOGGER.log(Level.INFO, "WebSocket connection established: " + session.getId() + " URI: " + session.getUri());
        // You can store session in a map if needed for broadcasting
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LOGGER.log(Level.INFO, "Received WebSocket message from session " + session.getId() + ": " + message.getPayload());
        // Echo back for now
        session.sendMessage(new TextMessage("{\"type\":\"echo\",\"payload\":" + message.getPayload() + "}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOGGER.log(Level.INFO, "WebSocket connection closed: " + session.getId() + " Status: " + status);
    }
}