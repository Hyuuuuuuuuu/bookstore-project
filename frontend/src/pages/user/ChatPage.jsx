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
  const messagesEndRef = useRef(null)
  const typingTimeoutRef = useRef(null)
  const fileInputRef = useRef(null)

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
      const client = chatService.connect(token, userId)
      // optimistic connected flag; chatService will call onError if it fails
      setConnected(chatService.isConnected)

      // subscribe to incoming messages via chatService
      const handleIncoming = (message) => {
        // message may be DTO object
        setMessages(prev => {
          const exists = prev.some(msg => msg.messageId === message.messageId)
          if (exists) return prev
          return [...prev, message]
        })
        scrollToBottom()
      }
      const handleError = (err) => {
        console.error('STOMP error callback', err)
        setError('Không thể kết nối đến server chat: ' + (err?.error || err?.message || JSON.stringify(err)))
        setConnected(false)
      }

      chatService.onMessage(handleIncoming)
      chatService.onError(handleError)

      return () => {
        chatService.offMessage(handleIncoming)
        chatService.offError(handleError)
        chatService.disconnect()
        setConnected(false)
      }
    } catch (e) {
      console.error('❌ WebSocket init error', e)
      setError('Không thể khởi tạo kết nối chat')
    }
  }, [token, user])

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
        
        // Load messages
        await loadMessages(conversationId)
      } catch (error) {
        console.error('❌ Error getting conversation:', error)
        setError('Không thể tải cuộc trò chuyện')
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
      setMessages(response.data.data.messages || [])
    } catch (error) {
      console.error('❌ Error loading messages:', error)
    }
  }

  // Socket event listeners are handled via chatService in the connect effect

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = async (e) => {
    e.preventDefault()
    if (!newMessage.trim() || !connected || !conversationId) return

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

      // send via STOMP
      if (adminUser && adminUser.userId) {
        chatService.sendMessage(adminUser.userId, newMessage.trim(), { messageType: 'text', conversationId })
      } else {
        chatService.sendToAdmin(newMessage.trim(), { messageType: 'text', conversationId })
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
      if (adminUser && adminUser.userId) {
        chatService.sendMessage(adminUser.userId, 'Đã gửi ảnh', { messageType: 'image', imageUrl, conversationId })
      } else {
        chatService.sendToAdmin('Đã gửi ảnh', { messageType: 'image', imageUrl, conversationId })
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
              {messages.map((message) => {
                // Logic hiển thị tin nhắn cho USER:
                // - Tin do user gửi (fromId === userId) → hiển thị bên phải
                // - Tin từ admin/staff (fromId !== userId) → hiển thị bên trái
                const isFromCurrentUser = message.fromUser && message.fromUser.userId?.toString() === user._id?.toString();
                
                console.log('🔍 ChatPage: Message positioning:', {
                  messageId: message.messageId,
                  fromUser: message.fromUser?.userId,
                  currentUser: user._id,
                  isFromCurrentUser,
                  sender: message.sender
                });
                
                return (
                  <div key={message.messageId || message._id || `msg_${Date.now()}`} className={`flex ${isFromCurrentUser ? 'justify-end' : 'justify-start'}`}>
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
