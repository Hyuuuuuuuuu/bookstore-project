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
      // Get or create conversation for current user from backend, then send CHAT_MESSAGE
      const resp = await fetch('/api/conversations/me', {
        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
      });
      const json = await resp.json();
      const convId = json?.data?.conversationId || null;
      chatService.sendChatMessage(convId, content);
    } catch (error) {
      console.error('Failed to send message to admin:', error);
      throw error;
    }
  }, []);

  // Load conversation messages (by conversationId or current user's conversation)
  const loadConversation = useCallback(async (conversationId, page = 0, limit = 20) => {
    try {
      let convId = conversationId;
      if (!convId) {
        const r = await fetch('/api/conversations/me', {
          headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
        });
        const j = await r.json();
        convId = j?.data?.conversationId || null;
      }
      if (!convId) return [];
      const response = await fetch(`/api/conversations/${convId}/messages?page=${page}&limit=${limit}`, {
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

  // Load current user's conversation messages
  const loadMessages = useCallback(async (page = 0, limit = 20) => {
    try {
      const r = await fetch('/api/conversations/me', {
        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
      });
      const j = await r.json();
      const convId = j?.data?.conversationId;
      if (!convId) return [];
      const response = await fetch(`/api/conversations/${convId}/messages?page=${page}&limit=${limit}`, {
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

  // Load chat users (admin helper) - not implemented server-side in new API
  const loadChatUsers = useCallback(async () => {
    console.warn('loadChatUsers: not implemented in new API');
    return [];
  }, []);

  // Get unread message count - not available in new API, return local state
  const getUnreadCount = useCallback(async () => {
    return unreadCount;
  }, [unreadCount]);

  // Mark messages as read - local only until API exists
  const markAsRead = useCallback(async (userId) => {
    setUnreadCount(0);
  }, []);

  // Delete message (local removal only)
  const deleteMessage = useCallback(async (messageId) => {
    setMessages(prev => prev.filter(msg => msg.id !== messageId));
  }, []);

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
