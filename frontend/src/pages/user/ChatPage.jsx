import React, { useState, useEffect, useRef } from 'react'
import chatService from '../../services/chatService'
import { useAuth } from '../../contexts/AuthContext'
import { chatAPI } from '../../services/apiService'

const ChatPage = () => {
  console.log('🔍 ChatPage component rendering...')
  const { user, token } = useAuth()
  const [connected, setConnected] = useState(false)
  const [conversationId, setConversationId] = useState(null)
  const [newMessage, setNewMessage] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [typingUsers, setTypingUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [uploadingImage, setUploadingImage] = useState(false)
  const [messages, setMessages] = useState([])
  const [adminUser, setAdminUser] = useState(null)
  const [selectedOrderInfo, setSelectedOrderInfo] = useState(null)
  const messagesEndRef = useRef(null)
  const typingTimeoutRef = useRef(null)
  const fileInputRef = useRef(null)
  const incomingBufferRef = useRef([]) // Buffer messages received before conversation/user ready
  const messageIdsSetRef = useRef(new Set()) // Track server message ids to avoid duplicates
  const pendingTempMapRef = useRef(new Map()) // tempId -> true (for optimistic messages)

  // Initialize socket connection
  useEffect(() => {
    // Scroll to top when component mounts
    window.scrollTo(0, 0);
    if (!token) {
      console.log('❌ No token available for socket connection')
      return
    }

    console.log('🔌 Initializing WebSocket connection...')
    try {
      const userId = user?._id || user?.id || user?.userId;
      if (!userId) {
        console.warn('ChatPage: userId not available yet, skipping socket connect')
        return;
      }
      // Prevent duplicate connects (idempotent)
      if (!chatService.isConnected) {
        chatService.connect(token, userId)
      }
      // optimistic connected flag; chatService will call onError if it fails
      setConnected(chatService.isConnected)

      // subscribe to incoming messages via chatService
      const handleIncoming = (message) => {
        // Only handle chat messages; ignore control frames (PING, PONG, etc.)
        if (!message || (message.type !== 'CHAT' && message.type !== 'CHAT_MESSAGE')) return

        const senderId = message.sender?.id || message.sender
        const msgConversationId = message.conversationId || message.conversation || null

        // If we don't have conversationId yet but message contains conversationId,
        // accept it and set the conversationId for this client (server targeted this session).
        if (!conversationId && msgConversationId) {
          setConversationId(msgConversationId)
          // load history for this conversation if we haven't
          try { loadMessages(msgConversationId) } catch (e) { /* ignore */ }
        }

        // Buffer if user is not ready
        if (!user) {
          incomingBufferRef.current.push(message)
          if (incomingBufferRef.current.length > 200) incomingBufferRef.current.shift()
          return
        }
        // Only accept messages that belong to current conversation (if we have one)
        if (conversationId && msgConversationId && String(msgConversationId) !== String(conversationId)) return

        const serverId = message.messageId || message.id
        const uiMessage = {
          messageId: serverId || `ws_${Date.now()}_${Math.random()}`,
          text: message.content || message.text || '',
          timestamp: new Date(message.timestamp || Date.now()),
          sender: senderId,
          fromUser: {
            userId: senderId,
            name: (message.sender && message.sender.role === 'SUPPORT') ? 'Support' : (senderId === '1' ? 'Admin' : 'User'),
            email: (message.sender && message.sender.role === 'SUPPORT') ? 'support@bookstore.com' : 'user@bookstore.com'
          },
          toUser: { userId: null },
          messageType: message.messageType || 'text',
          isRead: false
        }

        // Deduplicate: if server id already processed, skip
        if (serverId && messageIdsSetRef.current.has(String(serverId))) return

        setMessages(prev => {
          // If message is an echo of a temp message we created (same text and near timestamp), replace temp
          if (String(senderId) === String(user._id || user?.id)) {
            const idx = prev.findIndex(m =>
              String(m.messageId || '').startsWith('temp_') &&
              m.text === uiMessage.text &&
              Math.abs(new Date(m.timestamp).getTime() - uiMessage.timestamp.getTime()) < 5000
            )
            if (idx !== -1) {
              const copy = [...prev]
              copy[idx] = { ...copy[idx], ...uiMessage, messageId: serverId || copy[idx].messageId }
              if (serverId) messageIdsSetRef.current.add(String(serverId))
              // remove pendingTemp map entry
              const tempKey = copy[idx].messageId
              if (tempKey && pendingTempMapRef.current.has(tempKey)) pendingTempMapRef.current.delete(tempKey)
              return copy
            }
          }

          // Avoid duplicates by server id or text+timestamp proximity
          const exists = prev.some(msg =>
            serverId ? String(msg.messageId || msg.id) === String(serverId)
                     : (msg.text === uiMessage.text && Math.abs(new Date(msg.timestamp).getTime() - uiMessage.timestamp.getTime()) < 1000)
          )
          if (exists) return prev
          if (serverId) messageIdsSetRef.current.add(String(serverId))
          return [...prev, uiMessage]
        })
        scrollToBottom()
      }
      const handleError = (err) => {
        console.error('STOMP error callback', err)
        setError('Không thể kết nối đến server chat: ' + (err?.error || err?.message || JSON.stringify(err)))
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
      console.error('❌ WebSocket init error', e)
      setError('Không thể khởi tạo kết nối chat')
    }
  }, [token, user])

  // Restore any order info passed from OrdersListPage (support context)
  // Restore any order info passed from OrdersListPage (support context)
  useEffect(() => {
    try {
      const raw = localStorage.getItem('supportOrderInfo')
      if (!raw) return
      const obj = JSON.parse(raw)
      setSelectedOrderInfo(obj)
      // keep in localStorage until we successfully send initial support message
    } catch (e) {
      console.warn('Failed to read supportOrderInfo', e)
    }
  }, [])

  const initialSupportSentRef = useRef(false)

  // Auto-send initial support message (once) when connected and support info exists
  useEffect(() => {
    const trySendInitial = async () => {
      if (!selectedOrderInfo) return
      if (!connected) return
      if (initialSupportSentRef.current) return

      const order = selectedOrderInfo
      const orderCode = order.orderCode || order.orderId || ''
      const content = `Yêu cầu hỗ trợ đơn ${orderCode}`

      try {
        // conversationId may be null; backend will create conversation for user messages
        chatService.sendChatMessage(conversationId, content, { orderCode })
        initialSupportSentRef.current = true
        // remove stored supportOrderInfo to avoid re-sending
        try { localStorage.removeItem('supportOrderInfo') } catch (e) {}
      } catch (e) {
        console.error('Failed to send initial support message', e)
      }
    }

    trySendInitial()
  }, [connected, selectedOrderInfo, conversationId])

  // Get or create conversation
  useEffect(() => {
    const getConversation = async () => {
      try {
        console.log('🔍 Getting conversation for user:', user)
        setLoading(true)
        const response = await chatAPI.getOrCreateConversation()
        console.log('📨 Conversation response:', response)
        const { conversationId, adminUser } = response.data.data
        setConversationId(conversationId)
        setAdminUser(adminUser)
        
        // Load messages only if conversationId is present.
        if (conversationId) {
          await loadMessages(conversationId)
        } else {
          // No conversation yet - clear messages and wait for user to send first message
          setMessages([])
        }
        // flush any buffered incoming messages for this conversation by re-processing them
        if (incomingBufferRef.current.length > 0) {
          const buffered = incomingBufferRef.current.splice(0)
          buffered.forEach(m => {
            try {
              handleIncoming(m)
            } catch (e) {
              console.error('Error processing buffered incoming message', e)
            }
          })
        }
      } catch (error) {
        console.error('❌ Error getting conversation:', error)
        // Fallback: create client-side conversation with admin id 999 to keep UI working
        const fallbackAdmin = { userId: 999, name: 'Admin User', email: 'admin@bookstore.com' }
        const fallbackConversationId = `${Math.min(user?.id || user?._id || 1, 999)}_${Math.max(user?.id || user?._id || 1, 999)}`
        setConversationId(fallbackConversationId)
        setAdminUser(fallbackAdmin)
        // Clear messages (no messages yet)
        setMessages([])
        // Don't set a blocking error; show friendly message
        setError(null)
      } finally {
        setLoading(false)
      }
    }

    if (user) {
      getConversation()
    }
  }, [user])

  // no explicit join needed for STOMP; backend sends messages to user queues

  // Load messages
  const loadMessages = async (convId) => {
    try {
      const response = await chatAPI.getUserConversationMessages(convId, 1, 1000)
      const msgs = response.data.data.messages || []
      // Normalize each server message into UI shape:
      const normalized = msgs.map(m => {
        const msgId = m.messageId || m.id || m._id || null
        const senderId = m.fromUserId || m.senderId || (m.fromUser && m.fromUser.userId) || (m.sender && (m.sender.id || m.sender))
        const senderType = m.messageType || m.senderType || (m.sender && m.sender.role) || null
        const created = m.createdAt || m.created_at || m.timestamp || new Date()
        return {
          messageId: msgId,
          text: m.content || m.text || m.contentText || '',
          timestamp: created,
          messageType: senderType ? (senderType.toString ? senderType.toString() : senderType) : 'text',
          imageUrl: m.imageUrl || null,
          fromUser: { userId: senderId },
          raw: m
        }
      }).sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())

      setMessages(normalized)
      // rebuild processed server ids set
      messageIdsSetRef.current.clear()
      normalized.forEach(m => {
        const id = String(m.messageId || '')
        if (id && !id.startsWith('temp_')) messageIdsSetRef.current.add(id)
      })
    } catch (error) {
      console.error('❌ Error loading messages:', error)
    }
  }

  // Restore persisted user chat state (messages + conversationId) from localStorage
  useEffect(() => {
    try {
      const raw = localStorage.getItem('user_chat_state')
      if (!raw) return
      const parsed = JSON.parse(raw)
      if (parsed?.conversationId) {
        setConversationId(parsed.conversationId)
      }
      if (parsed?.messages) {
        const restored = parsed.messages.map(m => ({ ...m, timestamp: m.timestamp ? new Date(m.timestamp) : new Date() }))
        setMessages(restored)
        messageIdsSetRef.current.clear()
        restored.forEach(m => {
          const id = String(m.messageId || m.id || '')
          if (id && !id.startsWith('temp_')) messageIdsSetRef.current.add(id)
        })
      }
    } catch (e) {
      console.warn('Failed to restore user chat state', e)
    }
  }, [])

  // Persist user chat state to localStorage
  useEffect(() => {
    try {
      const payload = {
        conversationId,
        messages: messages.map(m => ({ ...m, timestamp: m.timestamp ? new Date(m.timestamp).toISOString() : null }))
      }
      localStorage.setItem('user_chat_state', JSON.stringify(payload))
    } catch (e) {
      console.warn('Failed to persist user chat state', e)
    }
  }, [conversationId, messages])

  // Socket event listeners are handled via chatService in the connect effect

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = async (e) => {
    e.preventDefault()
    if (!newMessage.trim() || !connected) return

    try {
      const tempMessage = {
        messageId: `temp_${Date.now()}`,
        sender: 'user',
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
        toUser: adminUser ? {
          userId: adminUser.userId,
          name: adminUser.name,
          email: adminUser.email,
          avatar: adminUser.avatar
        } : null
      }
      
      setMessages(prev => [...prev, tempMessage])
      scrollToBottom()

      // send via new spec: sendChatMessage with conversationId (backend will create conversation if needed)
      try {
        const options = {}
        if (selectedOrderInfo?.orderCode) options.orderCode = selectedOrderInfo.orderCode
        chatService.sendChatMessage(conversationId, newMessage.trim(), options)
      } catch (e) {
        // fallback to previous methods if available
        try {
          if (adminUser && adminUser.userId) {
            chatService.sendMessage(adminUser.userId, newMessage.trim(), { messageType: 'text', conversationId, orderCode: selectedOrderInfo?.orderCode })
          } else {
            chatService.sendToAdmin(newMessage.trim(), { messageType: 'text', conversationId, orderCode: selectedOrderInfo?.orderCode })
          }
        } catch (e2) {
          console.error('Fallback send failed', e2)
        }
      }

      setNewMessage('')
    } catch (error) {
      console.error('Error sending message:', error)
      setError('Không thể gửi tin nhắn')
    }
  }

  const handleImageUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    if (!file.type.startsWith('image/')) { setError('Chỉ được gửi file ảnh'); return }
    if (file.size > 5 * 1024 * 1024) { setError('Kích thước ảnh không được vượt quá 5MB'); return }

    try {
      setUploadingImage(true)
      const formData = new FormData()
      formData.append('image', file)
      const uploadResponse = await chatAPI.uploadImage(formData)
      const imageUrl = uploadResponse.data.data.imageUrl
      // send image as message content note (backend will interpret options)
      try {
        chatService.sendChatMessage(conversationId, '[IMAGE]', { messageType: 'image', imageUrl })
      } catch (e) {
        console.error('Image send failed', e)
      }
      if (fileInputRef.current) fileInputRef.current.value = ''
    } catch (error) {
      console.error('Error uploading image:', error)
      setError('Không thể tải ảnh lên')
    } finally {
      setUploadingImage(false)
    }
  }

  const handleTyping = (e) => {
    setNewMessage(e.target.value)
    if (!connected || !conversationId) return
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current)
    if (!isTyping) {
      setIsTyping(true)
      // typing events not implemented over STOMP yet
    }
    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false)
      // typing events not implemented over STOMP yet
    }, 2000)
  }

  const formatTime = (timestamp) => new Date(timestamp).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  const formatDate = (timestamp) => new Date(timestamp).toLocaleDateString('vi-VN')

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 bg-white">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-amber-600"></div>
        <p className="ml-4">Đang tải...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-8 bg-white">
        <div className="text-red-500 text-lg mb-4">❌ {error}</div>
        <button onClick={() => window.location.reload()} className="bg-amber-600 text-white px-4 py-2 rounded hover:bg-amber-700">Thử lại</button>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto p-6 bg-white">
      {/* Header */}
      <div className="mb-4">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Hỗ trợ khách hàng</h1>
            <p className="text-gray-500 mt-1">Chúng tôi sẽ phản hồi trong thời gian sớm nhất</p>
          </div>
          <div className="flex items-center space-x-2">
            <div className={`w-3 h-3 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`}></div>
            <span className="text-sm text-gray-500">{connected ? 'Đã kết nối' : 'Mất kết nối'}</span>
          </div>
        </div>
      </div>

      {/* Order context (if any) */}
      {selectedOrderInfo && (
        <div className="mb-4 p-4 bg-gray-50 border rounded">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-gray-500">Đơn hàng</div>
              <div className="text-lg font-medium">{selectedOrderInfo.orderCode}</div>
              <div className="text-sm text-gray-600">Tổng: {selectedOrderInfo.finalPrice ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(selectedOrderInfo.finalPrice) : ''}</div>
            </div>
            <div>
              <button onClick={() => setNewMessage(prev => `${prev} #${selectedOrderInfo.orderCode} `)} className="px-3 py-1 bg-blue-50 text-blue-600 rounded">Chèn mã đơn</button>
            </div>
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="h-96 overflow-y-auto border border-gray-200 rounded-lg">
        <div className="p-4">
          {messages.length === 0 ? (
            <div className="text-center text-gray-500 py-8">
              <div className="text-4xl mb-4">💬</div>
              <p>Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện!</p>
            </div>
          ) : (
            <div className="space-y-4">
              {messages.map((message, index) => {
                // Normalize fallback fields to avoid undefined in render
                const msgId = String(message.messageId || message._id || `temp_${index}_${Date.now()}`);
                const uniqueKey = `${msgId}_${index}`;

                const fromUser = message.fromUser || message.fromId || (message.sender ? { userId: message.sender } : null);
                const currentUserId = user?._id || user?.id || user?.userId;
                const isFromCurrentUser = fromUser && String(fromUser.userId) === String(currentUserId);

                // Lightweight debug - show minimal info
                console.debug('ChatPage: Message', { id: msgId, fromUser: fromUser?.userId, currentUserId, isFromCurrentUser });

                return (
                  <div key={uniqueKey} className={`flex ${isFromCurrentUser ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-xs lg:max-w-md px-4 py-2 rounded-2xl ${isFromCurrentUser ? 'bg-amber-600 text-white' : 'bg-gray-100 text-gray-900'}`}>
                      {message.messageType === 'image' ? (
                        <div>
                          {message.imageUrl ? (
                            <img src={`http://localhost:5000${message.imageUrl}`} alt="Uploaded image" className="max-w-full h-auto rounded mb-2" style={{ maxHeight: '200px' }} />
                          ) : (
                            <div className="bg-gray-100 border border-gray-300 rounded-lg p-4 text-center">
                              <div className="text-gray-500 text-sm">📷 Ảnh không khả dụng</div>
                            </div>
                          )}
                          <p className="text-sm">{message.text}</p>
                        </div>
                      ) : (
                        <p className="text-sm whitespace-pre-line">{message.text}</p>
                      )}
                      <p className={`text-xs mt-1 ${isFromCurrentUser ? 'text-amber-100' : 'text-gray-500'}`}>{formatTime(message.timestamp)}</p>
                    </div>
                  </div>
                )
              })}

              {typingUsers.length > 0 && (
                <div className="flex justify-start">
                  <div className="bg-gray-100 px-4 py-2 rounded-lg">
                    <p className="text-sm text-gray-500">{typingUsers.map(u => u.userName).join(', ')} đang nhập...</p>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
          )}
        </div>
      </div>

      {/* Message input */}
      <div className="mt-4">
        <form onSubmit={handleSendMessage} className="flex space-x-2">
          <input type="text" value={newMessage} onChange={handleTyping} placeholder="Nhập tin nhắn..." className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-amber-600 focus:border-amber-600" disabled={!connected || uploadingImage} />
          <input ref={fileInputRef} type="file" accept="image/*" onChange={handleImageUpload} className="hidden" disabled={!connected || uploadingImage} />
          <button type="button" onClick={() => fileInputRef.current?.click()} disabled={!connected || uploadingImage} className="px-3 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50" title="Gửi ảnh">{uploadingImage ? <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-700"></div> : '📷'}</button>
          <button type="submit" disabled={!newMessage.trim() || !connected || uploadingImage} className="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 disabled:opacity-50">Gửi</button>
        </form>
      </div>
    </div>
  )
}

export default ChatPage
