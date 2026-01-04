import React, { useState, useEffect, useRef } from 'react';
import chatService from '../../../services/chatService';
import { useAuth } from '../../../contexts/AuthContext';
import { chatAPI, messageAPI } from '../../../services/apiService';

const ChatsPage = () => {
  const { user, token } = useAuth();
  const [connected, setConnected] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const incomingBufferRef = useRef([]); // buffer for messages before UI ready
  const [loading, setLoading] = useState(true);
  const [newMessage, setNewMessage] = useState('');
  const [editingMessageId, setEditingMessageId] = useState(null);
  const [editingText, setEditingText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [typingUsers, setTypingUsers] = useState([]);
  const [uploadingImage, setUploadingImage] = useState(false);
  const messagesEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const fileInputRef = useRef(null);
  const selectedConversationRef = useRef(null); // Lưu selectedConversation hiện tại
  const conversationsLoadedRef = useRef(false); // Track xem đã load conversations chưa
  const currentConversationIdRef = useRef(null); // Track conversationId hiện tại đang load
  const isUserScrollingRef = useRef(false); // Track user scroll state
  const lastMessagesLengthRef = useRef(0); // Track số lượng messages để detect tin nhắn mới
  const conversationSubRef = useRef(null); // STOMP subscription for selected conversation
  // FIX: Sử dụng Set để track messageId (O(1) lookup) - tránh duplicate messages
  const messageIdsSetRef = useRef(new Set()); // Track messageId đã thêm vào state
  // FIX: Sử dụng Set để track messageId đang được xử lý (lock mechanism - tránh race condition)
  const processingMessagesRef = useRef(new Set()); // Track messageId đang được xử lý

  // Helper function to generate conversation ID
  const generateConversationId = (userId1, userId2) => {
    const minId = Math.min(userId1, userId2);
    const maxId = Math.max(userId1, userId2);
    return `${minId}_${maxId}`;
  };

  // Initialize WebSocket connection for admin
  useEffect(() => {
    if (!token || !user) return

    try {
      // Connect to WebSocket with admin user ID
      const userId = user?._id || user?.id || user?.userId;
      chatService.connect(token, userId)
      setConnected(chatService.isConnected)

      // global message handler for admin
      const handleIncoming = (msg) => {
        // Only handle chat messages; ignore control frames (PING, PONG, etc.)
        if (!msg || (msg.type !== 'CHAT' && msg.type !== 'CHAT_MESSAGE')) return
        // Buffer if user or conversations not ready yet
        if (!user || !conversationsLoadedRef.current) {
          incomingBufferRef.current.push(msg)
          if (incomingBufferRef.current.length > 200) incomingBufferRef.current.shift()
          return
        }

        // Normalize incoming message (server sends CHAT_MESSAGE with conversationId and sender object)
        const senderId = msg.sender?.id || msg.sender
        const senderRole = msg.sender?.role || (msg.sender && (msg.sender === 'admin' ? 'SUPPORT' : 'USER')) || 'USER'
        const convId = msg.conversationId || null

        // Convert to expected format for admin UI
        const convertedMessage = {
          messageId: msg.messageId || msg.id || `ws_${Date.now()}_${Math.random()}`,
          text: msg.content || msg.text || '',
          timestamp: new Date(msg.timestamp || Date.now()),
          sender: senderId,
          fromUser: {
            userId: senderId,
            name: senderRole === 'SUPPORT' ? 'Support' : `User ${senderId}`,
            email: senderRole === 'SUPPORT' ? 'support@bookstore.com' : `user${senderId}@bookstore.com`
          },
          toUser: { userId: null },
          messageType: 'text',
          isRead: false
        }

        const conversationId = String(convId)

        // Update conversations list
        setConversations(prev => {
          const idx = prev.findIndex(c => c.conversationId === conversationId)
          if (idx === -1) {
          // Add new conversation
          const newConv = {
            conversationId,
            user: {
              userId: msg.sender,
              name: msg.sender === '1' ? 'Admin' : 'User',
              email: msg.sender === '1' ? 'admin@bookstore.com' : 'user@bookstore.com'
            },
            lastMessage: convertedMessage.text,
            lastMessageTime: convertedMessage.timestamp,
            messages: [convertedMessage]
          }
          // auto-select new conversation if none selected
          if (!selectedConversationRef.current) {
            setSelectedConversation(newConv)
          }
          return [...prev, newConv]
          } else {
            // Update existing conversation
            const copy = [...prev]
            copy[idx] = {
              ...copy[idx],
              lastMessage: convertedMessage.text,
              lastMessageTime: convertedMessage.timestamp,
              messages: [...(copy[idx].messages || []), convertedMessage]
            }
          // if this is the currently selected conversation, append to messages pane
          if (selectedConversationRef.current && selectedConversationRef.current.conversationId === conversationId) {
            setMessages(prevMsgs => [...prevMsgs, convertedMessage])
          }
            return copy
          }
        })
      }

      const handleError = (err) => {
        console.error('WebSocket error (admin):', err)
        setConnected(false)
      }
      const handleOpen = () => {
        setConnected(true)
      }

      chatService.onMessage(handleIncoming)
      chatService.onError(handleError)
      chatService.onOpen(handleOpen)
      const handleVisibility = () => {
        if (document.visibilityState === 'visible' && token && user) {
          const userId = user?._id || user?.id || user?.userId;
          if (!chatService.isConnected) {
            chatService.connect(token, userId)
            setConnected(chatService.isConnected)
          }
        }
      }

      window.addEventListener('visibilitychange', handleVisibility)

      return () => {
        window.removeEventListener('visibilitychange', handleVisibility)
        chatService.offMessage(handleIncoming)
        chatService.offError(handleError)
        chatService.offOpen(handleOpen)
        chatService.disconnect()
        setConnected(false)
      }
    } catch (e) {
      console.error('Admin WebSocket init error', e)
    }
  }, [token, user])

  // Load conversations - FIX: Chỉ set loading lần đầu, tránh reload không cần thiết
  useEffect(() => {
    const loadConversations = async () => {
      // Chỉ set loading khi chưa load lần nào
      if (!conversationsLoadedRef.current) {
        setLoading(true)
      }
      
      try {
        const response = await chatAPI.getAdminConversations()
        const conversations = response.data.data.conversations || []
        setConversations(conversations)
        conversationsLoadedRef.current = true
        
        // FIX: Không join tất cả conversations khi load list
        // Chỉ join khi select conversation để tránh duplicate
      // Flush any buffered incoming messages now that conversations are loaded
      if (incomingBufferRef.current.length > 0) {
        const buffered = incomingBufferRef.current.splice(0)
        // Process buffered chat messages: update conversations list/messages
            buffered.forEach(m => {
          try {
            if (m.type !== 'CHAT' && m.type !== 'CHAT_MESSAGE') return
            const senderId = m.sender?.id || m.sender
            const convId = String(m.conversationId || '')
            const convertedMessage = {
              messageId: m.messageId || `ws_${Date.now()}_${Math.random()}`,
              text: m.content || m.text || '',
              timestamp: new Date(m.timestamp || Date.now()),
              fromUser: { userId: senderId },
              toUser: { userId: null },
              messageType: 'text',
              isRead: false
            }
            const conversationId = String(convId)
            setConversations(prev => {
              const idx = prev.findIndex(c => c.conversationId === conversationId)
              if (idx === -1) {
                const newConv = {
                  conversationId,
                  user: { userId: m.sender, name: `User ${m.sender}`, email: '' },
                  lastMessage: convertedMessage.text,
                  lastMessageTime: convertedMessage.timestamp,
                  messages: [convertedMessage]
                }
                if (!selectedConversationRef.current) {
                  setSelectedConversation(newConv)
                }
                return [...prev, newConv]
              } else {
                const copy = [...prev]
                copy[idx] = {
                  ...copy[idx],
                  lastMessage: convertedMessage.text,
                  lastMessageTime: convertedMessage.timestamp,
                  messages: [...(copy[idx].messages || []), convertedMessage]
                }
                if (selectedConversationRef.current && selectedConversationRef.current.conversationId === conversationId) {
                  setMessages(prevMsgs => [...prevMsgs, convertedMessage])
                }
                return copy
              }
            })
          } catch (e) {
            console.error('Error processing buffered message', e)
          }
        })
      }
      } catch (error) {
        console.error('Error loading conversations:', error)
      } finally {
        setLoading(false)
      }
    }

    if (connected && user) {
      loadConversations()
    }
  }, [connected, user?._id]) // FIX: Chỉ dùng user._id thay vì toàn bộ user object
  // NOTE: updated dependency to connected; keep previous behavior but rely on STOMP

  // Load messages when conversation is selected - FIX: Tránh reload khi user object thay đổi reference
  useEffect(() => {
      const loadMessages = async () => {
        if (!selectedConversation) {
          setMessages([])
          currentConversationIdRef.current = null
          // FIX: Reset messageIdsSet khi không có conversation được chọn
          messageIdsSetRef.current.clear()
          return
        }

      const convId = selectedConversation.conversationId || selectedConversation._id
      
      // Tránh reload nếu đang load cùng conversation
      if (currentConversationIdRef.current === convId) {
        return
      }
      
      currentConversationIdRef.current = convId

      try {
        const response = await chatAPI.getConversationMessages(convId, 1, 1000)
        const raw = response?.data?.data?.messages || response?.data?.messages || []

        // Chuẩn hóa dữ liệu message để UI hiển thị ổn định
        const normalized = raw.map((msg) => {
          const fromUser = msg.fromUser || (msg.fromId ? {
            userId: msg.fromId._id || msg.fromId,
            name: msg.fromId.name,
            email: msg.fromId.email,
            avatar: msg.fromId.avatar
          } : null)

          const toUser = msg.toUser || (msg.toId ? {
            userId: msg.toId._id || msg.toId,
            name: msg.toId.name,
            email: msg.toId.email,
            avatar: msg.toId.avatar
          } : null)

          // Determine canonical sender id and role from possible backend shapes
          const senderId = msg.fromUserId || msg.senderId || (fromUser && fromUser.userId) || (msg.fromId && (msg.fromId._id || msg.fromId)) || null
          const senderRole = (msg.senderType || msg.sender_type || (msg.sender && msg.sender.role) || (msg.fromUser && msg.fromUser.role) || null)

          // FIX: Đơn giản hóa - loại bỏ phân biệt role, chỉ dùng userId
          // FIX: Đảm bảo messageId luôn là string để so sánh chính xác
          const messageId = String(msg.messageId || msg._id || msg.id || '')
          
          return {
            messageId,
            text: msg.text || msg.content || '',
            timestamp: msg.timestamp || msg.createdAt || new Date(),
            isRead: msg.isRead ?? false,
            messageType: msg.messageType || 'text',
            imageUrl: msg.imageUrl || null,
            fromUser,
            toUser,
            senderId,
            senderRole,
            raw: msg
          }
        })
        // Sắp xếp tăng dần theo thời gian để tin cũ ở trên, tin mới ở dưới
        .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())

        setMessages(normalized)
        
        // FIX: Reset và rebuild messageIdsSet khi load messages mới
        messageIdsSetRef.current.clear()
        normalized.forEach(msg => {
          const msgId = String(msg.messageId || '')
          if (msgId && !msgId.startsWith('temp_')) {
            messageIdsSetRef.current.add(msgId)
          }
        })
        
        // For STOMP: subscribe to conversation topic when selected (handled elsewhere)
      } catch (error) {
        console.error('Error loading messages:', error)
        currentConversationIdRef.current = null // Reset on error
      }
    }

    loadMessages()
  }, [selectedConversation?.conversationId || selectedConversation?._id, connected]) // FIX: Chỉ dùng conversationId thay vì toàn bộ object
  
  // Update ref với selected conversation hiện tại
  useEffect(() => {
    selectedConversationRef.current = selectedConversation
  }, [selectedConversation])

  // Restore chat UI state (selected conversation + messages) from localStorage on mount
  useEffect(() => {
    try {
      const raw = localStorage.getItem('admin_chat_state')
      if (!raw) return
      const parsed = JSON.parse(raw)
      if (parsed?.selectedConversation) {
        // convert any timestamp strings in messages back to Date objects
        const restoredMessages = (parsed.messages || []).map(m => ({
          ...m,
          timestamp: m.timestamp ? new Date(m.timestamp) : new Date()
        }))
        setSelectedConversation(parsed.selectedConversation)
        setMessages(restoredMessages)
        // rebuild messageIdsSet to avoid duplicates
        messageIdsSetRef.current.clear()
        restoredMessages.forEach(m => {
          const id = String(m.messageId || m.id || '')
          if (id && !id.startsWith('temp_')) messageIdsSetRef.current.add(id)
        })
      }
    } catch (e) {
      console.warn('Failed to restore admin chat state', e)
    }
  }, [])

  // Persist selectedConversation + messages to localStorage so reload preserves UI
  useEffect(() => {
    try {
      const payload = {
        selectedConversation,
        messages: messages.map(m => ({ ...m, timestamp: m.timestamp ? new Date(m.timestamp).toISOString() : null }))
      }
      localStorage.setItem('admin_chat_state', JSON.stringify(payload))
    } catch (e) {
      console.warn('Failed to persist admin chat state', e)
    }
  }, [selectedConversation, messages])

  // WebSocket message handler - simplified for raw WebSocket
  useEffect(() => {
    if (!connected) return

      const handleNewMessage = (data) => {
      if (!data) return
      if (data.type !== 'CHAT' && data.type !== 'CHAT_MESSAGE') return

      const senderId = data.sender?.id || data.sender
      const convId = String(data.conversationId || '')
      const uiMessage = {
        messageId: data.messageId || data.id || `ws_${Date.now()}_${Math.random()}`,
        text: data.content || data.text || '',
        timestamp: new Date(data.timestamp || Date.now()),
        sender: senderId,
        fromUser: { userId: senderId },
        toUser: { userId: null },
        messageType: 'text',
        isRead: false,
        conversationId: convId
      }

      const current = selectedConversationRef.current
      if (!current) return
      if (String(current.conversationId) !== String(convId)) return
      setMessages(prev => {
        const serverId = data.messageId || data.id

        // If server id already processed, skip
        if (serverId && messageIdsSetRef.current.has(String(serverId))) return prev

        // If this is an echo of a temp message sent by current admin, replace the temp one
        if (String(senderId) === String(user._id || user?.id)) {
          const tempIdx = prev.findIndex(m =>
            String(m.messageId || '').startsWith('temp_') &&
            m.text === uiMessage.text &&
            Math.abs(new Date(m.timestamp).getTime() - new Date(uiMessage.timestamp).getTime()) < 5000
          )
          if (tempIdx !== -1) {
            const copy = [...prev]
            copy[tempIdx] = { ...copy[tempIdx], ...uiMessage, messageId: serverId || copy[tempIdx].messageId }
            if (serverId) messageIdsSetRef.current.add(String(serverId))
            return copy
          }
        }

        // Dedupe by server id or by text+timestamp proximity
        const exists = prev.some(msg =>
          serverId ? String(msg.messageId || msg.id) === String(serverId)
                   : (msg.text === uiMessage.text && Math.abs(new Date(msg.timestamp).getTime() - uiMessage.timestamp.getTime()) < 1000)
        )
        if (exists) return prev
        if (serverId) messageIdsSetRef.current.add(String(serverId))
        return [...prev, uiMessage]
      })

      // Scroll to bottom
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
      }, 100)

      // Scroll to bottom sau khi thêm message
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
      }, 100)
    }

    // Add message handler to chat service
    chatService.onMessage(handleNewMessage)

    return () => {
      chatService.offMessage(handleNewMessage)
    }
  }, [connected, user])

  // Auto scroll to bottom - FIX: Chỉ scroll khi thực sự cần thiết
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    // Chỉ scroll nếu:
    // 1. Messages tăng (tin nhắn mới được thêm)
    // 2. User đang ở gần cuối scroll hoặc chưa scroll
    if (messages.length > lastMessagesLengthRef.current) {
      // Tin nhắn mới được thêm
      const messagesContainer = messagesEndRef.current?.parentElement
      if (messagesContainer) {
        const isNearBottom = 
          messagesContainer.scrollHeight - messagesContainer.scrollTop - messagesContainer.clientHeight < 200
        
        if (isNearBottom || messages.length === 1) {
          // Chỉ scroll nếu user đang ở gần cuối hoặc là tin nhắn đầu tiên
          setTimeout(() => {
            scrollToBottom()
          }, 100)
        }
      }
    }
    
    lastMessagesLengthRef.current = messages.length
  }, [messages.length]) // FIX: Chỉ trigger khi length thay đổi, không phải toàn bộ messages array

  const handleSendMessage = async (e) => {
    e.preventDefault()
    if (!newMessage.trim() || !connected || !selectedConversation) return

    try {
      // FIX: Đơn giản hóa - loại bỏ phân biệt role, chỉ dùng userId
      // Thêm tin nhắn vào state ngay lập tức (Optimistic UI)
      const tempMessage = {
        messageId: `temp_${Date.now()}`,
        text: newMessage.trim(),
        timestamp: new Date(),
        isRead: false,
        messageType: 'text',
        imageUrl: null,
        fromUser: {
          userId: user._id,
          name: user.name,
          email: user.email,
          avatar: user.avatar
        },
        toUser: selectedConversation.user ? {
          userId: selectedConversation.user.userId,
          name: selectedConversation.user.name,
          email: selectedConversation.user.email,
          avatar: selectedConversation.user.avatar
        } : null
      }
      
      // FIX: Chiến lược B - Thêm temp message vào state (Optimistic UI)
      // Lưu ý: Khi nhận lại từ socket, logic deduplication sẽ thay thế temp message
      setMessages(prev => [...prev, tempMessage])
      scrollToBottom()

      // Send via WebSocket
      const convId = selectedConversation.conversationId || selectedConversation._id
      const targetUserId = selectedConversation.user?.userId
      try {
        chatService.sendChatMessage(convId, newMessage.trim())
      } catch (e) {
        // fallback
        if (targetUserId) {
          chatService.sendMessage(targetUserId, newMessage.trim())
        } else {
          chatService.sendToAdmin(newMessage.trim())
        }
      }

      setNewMessage('')
      
      // Stop typing indicator
      // typing events not implemented for STOMP here
    } catch (error) {
      console.error('Error sending message:', error)
      alert('Có lỗi xảy ra khi gửi tin nhắn!')
    }
  }

  // Handle image upload
  const handleImageUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    // Validate file type
    if (!file.type.startsWith('image/')) {
      alert('Chỉ được gửi file ảnh')
      return
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('Kích thước ảnh không được vượt quá 5MB')
      return
    }

    try {
      setUploadingImage(true)
      
      // Upload image
      const formData = new FormData()
      formData.append('image', file)
      
      const uploadResponse = await chatAPI.uploadImage(formData)
      const imageUrl = uploadResponse.data.data.imageUrl

      // FIX: Đơn giản hóa - loại bỏ phân biệt role, chỉ dùng userId
      // Thêm tin nhắn ảnh vào state ngay lập tức (Optimistic UI)
      const tempImageMessage = {
        messageId: `temp_${Date.now()}`,
        text: 'Đã gửi ảnh',
        timestamp: new Date(),
        isRead: false,
        messageType: 'image',
        imageUrl: imageUrl,
        fromUser: {
          userId: user._id,
          name: user.name,
          email: user.email,
          avatar: user.avatar
        },
        toUser: selectedConversation.user ? {
          userId: selectedConversation.user.userId,
          name: selectedConversation.user.name,
          email: selectedConversation.user.email,
          avatar: selectedConversation.user.avatar
        } : null
      }
      
      // FIX: Chiến lược B - Thêm temp image message vào state (Optimistic UI)
      setMessages(prev => [...prev, tempImageMessage])
      scrollToBottom()

      // Send image message via WebSocket
      const convId = selectedConversation.conversationId || selectedConversation._id
      const targetUserId = selectedConversation.user?.userId
      if (targetUserId) {
        chatService.sendMessage(targetUserId, 'Đã gửi ảnh', { messageType: 'image', imageUrl })
      } else {
        chatService.sendToAdmin('Đã gửi ảnh', { messageType: 'image', imageUrl })
      }

      // Clear file input
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (error) {
      console.error('Error uploading image:', error)
      alert('Không thể tải ảnh lên')
    } finally {
      setUploadingImage(false)
    }
  }

  // Handle typing
  const handleTyping = (e) => {
    setNewMessage(e.target.value)
    
    if (!connected || !selectedConversation) return

    // Clear existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current)
    }

    // Start typing indicator
    if (!isTyping) {
      setIsTyping(true)
      // typing events not implemented for STOMP here
    }

    // Stop typing indicator after 2 seconds of inactivity
    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false)
      // typing events not implemented for STOMP here
    }, 2000)
  }

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDate = (timestamp) => {
    return new Date(timestamp).toLocaleDateString('vi-VN');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="ml-4">Đang tải dữ liệu...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200"> 
        <div className="flex h-[83vh]">
          {/* Chat List */}
          <div className="w-1/3 border-r border-gray-200">
            <div className="p-4 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium text-gray-900">Cuộc trò chuyện</h3>
                <div className="flex items-center space-x-4">
                  <div className="text-sm text-gray-500">
                    {conversations.length} cuộc trò chuyện
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className={`w-3 h-3 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`}></div>
                    <span className="text-sm text-gray-500">
                      {connected ? 'Đã kết nối' : 'Mất kết nối'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div className="overflow-y-auto">
              {conversations.length === 0 ? (
                <div className="p-4 text-center text-gray-500">
                  Chưa có cuộc trò chuyện nào
                </div>
              ) : (
                <div className="space-y-1">
                  {conversations.map((conversation, idx) => (
                    <div
                      key={`${conversation.conversationId ?? conversation.user?.userId ?? 'conv'}_${idx}`}
                      onClick={() => setSelectedConversation(conversation)}
                      className={`p-3 cursor-pointer hover:bg-gray-50 ${
                        selectedConversation?.conversationId === conversation.conversationId ? 'bg-blue-50 border-r-2 border-blue-500' : ''
                      }`}
                    >
                      <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white font-medium">
                        {conversation.user?.name?.charAt(0)?.toUpperCase() || conversation.user?.email?.charAt(0)?.toUpperCase() || 'U'}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {conversation.user?.name || 'User'}
                        </p>
                          <p className="text-xs text-gray-500 truncate">
                            {conversation.messages?.[0]?.text || 'Chưa có tin nhắn'}
                          </p>
                        </div>
                        <div className="text-xs text-gray-400">
                          {conversation.lastMessageTime ? formatTime(conversation.lastMessageTime) : ''}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Chat Messages */}
          <div className="flex-1 flex flex-col">
            {selectedConversation ? (
              <>
                <div className="p-4 border-b border-gray-200 bg-gray-50">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white font-medium">
                      {selectedConversation.user?.name?.charAt(0)?.toUpperCase() || selectedConversation.user?.email?.charAt(0)?.toUpperCase() || 'U'}
                    </div>
                    <div>
                      <h4 className="text-sm font-medium text-gray-900">
                        {selectedConversation.user?.name || 'User'}
                      </h4>
                      <p className="text-xs text-gray-500">
                        {selectedConversation.user?.email || 'No email'}
                      </p>
                    </div>
                  </div>
                </div>

                <div className="flex-1 p-4 overflow-y-auto">
                  {messages.length === 0 ? (
                    <div className="text-center text-gray-500 py-8">
                      <div className="text-4xl mb-4">💬</div>
                      <p>Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện!</p>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {messages.map((message, index) => {
                        // FIX #7: Tạo key unique cho mỗi message để React render đúng
                        // VẤN ĐỀ: Nếu có duplicate messageId, React có thể render sai hoặc warning
                        // GIẢI PHÁP: Sử dụng messageId + index để đảm bảo unique
                        // Lưu ý: Nếu messageId là ObjectId, convert sang string
                        const messageIdStr = String(message.messageId || `temp_${index}`)
                        const uniqueKey = `${messageIdStr}_${index}`
                        
                        // FIX: Đơn giản hóa - chỉ so sánh userId để xác định tin nhắn của mình
                        // - Tin do current user gửi (fromUser.userId === current userId) → hiển thị bên phải
                        // - Tin từ user khác (fromUser.userId !== current userId) → hiển thị bên trái
                        // Determine sender id/type robustly
                        const senderId = message.senderId || message.fromUser?.userId || message.raw?.fromUserId || message.raw?.senderId || message.sender || message.fromId || null;
                        const senderType = (message.senderRole || message.senderRole || message.raw?.senderType || message.raw?.sender_type || '').toString().toUpperCase();
                        const currentUserId = user?._id || user?.id || user?.userId;
                        // On admin UI: messages sent by support (senderType === 'SUPPORT') should appear on right.
                        const isFromCurrentUser = (senderId && String(senderId) === String(currentUserId)) || (senderType === 'SUPPORT');
                        
                  // FIX: Render message với key unique
                  // Tham khảo ChatWidget.jsx line 408: `key={message.messageId || message._id || `msg_${Date.now()}`}`
                  // Ta sử dụng uniqueKey để đảm bảo unique ngay cả khi có duplicate messageId
                  return (
                    <div
                      key={uniqueKey}
                      className={`flex ${isFromCurrentUser ? 'justify-end' : 'justify-start'}`}
                    >
                            <div
                              className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                                isFromCurrentUser
                                  ? 'bg-blue-500 text-white'
                                  : 'bg-gray-100 text-gray-900'
                              }`}
                            >
                            {message.messageType === 'image' ? (
                              <div>
                                {message.imageUrl ? (
                                  <img 
                                    src={`http://localhost:5000${message.imageUrl}`} 
                                    alt="Uploaded image" 
                                    className="max-w-full h-auto rounded mb-2"
                                    style={{ maxHeight: '200px' }}
                                    onError={(e) => {
                                      console.error('❌ Admin image load error:', e)
                                    }}
                                  />
                                ) : (
                                  <div className="bg-gray-100 border border-gray-300 rounded-lg p-4 text-center">
                                    <div className="text-gray-500 text-sm">📷 Ảnh không khả dụng</div>
                                  </div>
                                )}
                                <p className="text-sm">{message.text}</p>
                              </div>
                            ) : message.text.includes('📦 **Thông tin đơn hàng cần hỗ trợ:**') ? (
                              <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                                <div className="text-sm whitespace-pre-line">{message.text}</div>
                              </div>
                            ) : (
                              <p className="text-sm">{message.text}</p>
                            )}
                              <p className={`text-xs mt-1 ${
                                isFromCurrentUser ? 'text-blue-100' : 'text-gray-500'
                              }`}>
                                {formatTime(message.timestamp)}
                              </p>
                      {/* Edit/Delete removed for message immutability */}
                            </div>
                          </div>
                        )
                      })}
                      
                      {/* Typing indicator */}
                      {typingUsers.length > 0 && (
                        <div className="flex justify-start">
                          <div className="bg-gray-100 px-4 py-2 rounded-lg">
                            <p className="text-sm text-gray-500">
                              {typingUsers.map(u => u.userName).join(', ')} đang nhập...
                            </p>
                          </div>
                        </div>
                      )}
                      
                      <div ref={messagesEndRef} />
                    </div>
                  )}
                </div>

                <div className="p-4 border-t border-gray-200">
                  <form onSubmit={handleSendMessage} className="flex space-x-2">
                    <input
                      type="text"
                      value={newMessage}
                      onChange={handleTyping}
                      placeholder="Nhập tin nhắn..."
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      disabled={!connected || uploadingImage}
                    />
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      onChange={handleImageUpload}
                      className="hidden"
                      disabled={!connected || uploadingImage}
                    />
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={!connected || uploadingImage}
                      className="px-3 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
                      title="Gửi ảnh"
                    >
                      {uploadingImage ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      ) : (
                        '📷'
                      )}
                    </button>
                    <button
                      type="submit"
                      disabled={!newMessage.trim() || !connected || uploadingImage}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Gửi
                    </button>
                  </form>
                </div>
              </>
            ) : (
              <div className="flex-1 flex items-center justify-center text-gray-500">
                <div className="text-center">
                  <div className="text-4xl mb-4">💬</div>
                  <p>Chọn một cuộc trò chuyện để bắt đầu</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatsPage;
