import { useState, useEffect, useCallback, useRef } from 'react';
import chatService from '../services/chatService';
import { useAuth } from '../contexts/AuthContext';

const useChat = () => {
  const { user, token } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [chatUsers, setChatUsers] = useState([]);
  const messageCallbackRef = useRef(null);
  const errorCallbackRef = useRef(null);

  // Connect to chat when user is authenticated
  useEffect(() => {
    if (user && token) {
      try {
        chatService.connect(token, user.id);
        setIsConnected(true);

        // Set up message listener
        messageCallbackRef.current = (message) => {
          setMessages(prev => [...prev, message]);
          setUnreadCount(prev => prev + 1);
        };

        // Set up error listener
        errorCallbackRef.current = (error) => {
          console.error('Chat error:', error);
        };

        chatService.onMessage(messageCallbackRef.current);
        chatService.onError(errorCallbackRef.current);

      } catch (error) {
        console.error('Failed to connect to chat:', error);
        setIsConnected(false);
      }
    } else {
      // Disconnect when user logs out
      chatService.disconnect();
      setIsConnected(false);
    }

    // Cleanup on unmount
    return () => {
      if (messageCallbackRef.current) {
        chatService.offMessage(messageCallbackRef.current);
      }
      if (errorCallbackRef.current) {
        chatService.offError(errorCallbackRef.current);
      }
    };
  }, [user, token]);

  // Send message to another user
  const sendMessage = useCallback(async (toUserId, content) => {
    try {
      chatService.sendMessage(toUserId, content);
    } catch (error) {
      console.error('Failed to send message:', error);
      throw error;
    }
  }, []);

  // Send message to admin
  const sendToAdmin = useCallback(async (content) => {
    try {
      chatService.sendToAdmin(content);
    } catch (error) {
      console.error('Failed to send message to admin:', error);
      throw error;
    }
  }, []);

  // Load conversation messages
  const loadConversation = useCallback(async (userId, page = 0, limit = 20) => {
    try {
      const response = await fetch(`/api/chat/conversation/${userId}?page=${page}&limit=${limit}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to load conversation');
      }

      const data = await response.json();
      return data.data.messages;
    } catch (error) {
      console.error('Failed to load conversation:', error);
      throw error;
    }
  }, [token]);

  // Load user's messages
  const loadMessages = useCallback(async (page = 0, limit = 20) => {
    try {
      const response = await fetch(`/api/chat/messages?page=${page}&limit=${limit}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to load messages');
      }

      const data = await response.json();
      setMessages(data.data.messages);
      return data.data.messages;
    } catch (error) {
      console.error('Failed to load messages:', error);
      throw error;
    }
  }, [token]);

  // Load chat users
  const loadChatUsers = useCallback(async () => {
    try {
      const response = await fetch('/api/chat/users', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to load chat users');
      }

      const data = await response.json();
      setChatUsers(data.data);
      return data.data;
    } catch (error) {
      console.error('Failed to load chat users:', error);
      throw error;
    }
  }, [token]);

  // Get unread message count
  const getUnreadCount = useCallback(async () => {
    try {
      const response = await fetch('/api/chat/unread-count', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to get unread count');
      }

      const data = await response.json();
      setUnreadCount(data.data.unreadCount);
      return data.data.unreadCount;
    } catch (error) {
      console.error('Failed to get unread count:', error);
      throw error;
    }
  }, [token]);

  // Mark messages as read
  const markAsRead = useCallback(async (userId) => {
    try {
      const response = await fetch(`/api/chat/mark-read/${userId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to mark messages as read');
      }

      // Reset unread count for this conversation
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Failed to mark messages as read:', error);
      throw error;
    }
  }, [token]);

  // Delete message
  const deleteMessage = useCallback(async (messageId) => {
    try {
      const response = await fetch(`/api/chat/messages/${messageId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to delete message');
      }

      // Remove message from local state
      setMessages(prev => prev.filter(msg => msg.id !== messageId));
    } catch (error) {
      console.error('Failed to delete message:', error);
      throw error;
    }
  }, [token]);

  // Clear messages (for cleanup)
  const clearMessages = useCallback(() => {
    setMessages([]);
  }, []);

  // Disconnect chat
  const disconnect = useCallback(() => {
    chatService.disconnect();
    setIsConnected(false);
    setMessages([]);
    setUnreadCount(0);
  }, []);

  return {
    // State
    isConnected,
    messages,
    unreadCount,
    chatUsers,

    // Actions
    sendMessage,
    sendToAdmin,
    loadConversation,
    loadMessages,
    loadChatUsers,
    getUnreadCount,
    markAsRead,
    deleteMessage,
    clearMessages,
    disconnect
  };
};

export default useChat;
