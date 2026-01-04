import React, { createContext, useContext, useEffect, useState, useRef } from 'react'
import chatService from '../services/chatService'
import { useAuth } from './AuthContext'

const ChatContext = createContext(null)

export const useChat = () => {
  const ctx = useContext(ChatContext)
  if (!ctx) throw new Error('useChat must be used inside ChatProvider')
  return ctx
}

export const ChatProvider = ({ children }) => {
  const { user, token, loading: authLoading } = useAuth()
  const [connected, setConnected] = useState(false)
  const [conversationId, setConversationId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const messageIds = useRef(new Set())
  const buffer = useRef([])

  // Helper to fetch conversation for current user
  const fetchConversation = async () => {
    if (!token) return null
    try {
      setLoading(true)
      const res = await fetch('/api/conversations/me', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const json = await res.json()
      const convId = json?.data?.conversationId || null
      setConversationId(convId)
      return convId
    } catch (e) {
      setError('Không thể lấy conversation')
      return null
    } finally {
      setLoading(false)
    }
  }

  // Fetch messages for a conversation
  const fetchMessages = async (convId) => {
    if (!convId || !token) return
    try {
      const res = await fetch(`/api/conversations/${convId}/messages`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      const json = await res.json()
      const msgs = json?.data?.messages || []
      // normalize and dedup
      const normalized = msgs.map(m => ({
        id: m.id,
        conversationId: convId,
        sender: { id: m.senderId, role: m.senderType },
        content: m.content,
        timestamp: m.createdAt
      }))
      setMessages(normalized)
      messageIds.current = new Set(normalized.map(m => String(m.id)))
    } catch (e) {
      console.error('Fetch messages failed', e)
    }
  }

  // Handle incoming ws messages
  useEffect(() => {
    const onMsg = (payload) => {
      if (!payload) return
      // only handle CHAT_MESSAGE
      if (payload.type !== 'CHAT_MESSAGE') return
      const convId = payload.conversationId
      // buffer if conversation not ready
      if (!conversationId || convId !== conversationId) {
        // keep for later if it's for our conversationId or if we have no conversation yet
        buffer.current.push(payload)
        if (buffer.current.length > 200) buffer.current.shift()
        return
      }
      const mid = String(payload.messageId || payload.id || (payload.timestamp + '_' + Math.random()))
      if (messageIds.current.has(mid)) return
      messageIds.current.add(mid)
      const ui = {
        id: mid,
        conversationId: convId,
        sender: payload.sender,
        content: payload.content,
        timestamp: payload.timestamp
      }
      setMessages(prev => [...prev, ui])
    }

    const onErr = (err) => {
      console.error('Chat socket error', err)
      setConnected(false)
    }

    chatService.onMessage(onMsg)
    chatService.onError(onErr)
    return () => {
      chatService.offMessage(onMsg)
      chatService.offError(onErr)
    }
  }, [conversationId])

  // Connect when auth ready
  useEffect(() => {
    if (authLoading) return
    if (!user || !token) return
    // ensure chatService connected
    const userId = user._id || user.id || user.userId
    try {
      chatService.connect(token, userId)
      setConnected(chatService.isConnected)
    } catch (e) {
      console.error('Chat connect error', e)
      setError('Không thể kết nối chat')
    }
  }, [authLoading, user, token])

  // load conversation and messages on auth or when conversationId becomes available
  useEffect(() => {
    let mounted = true
    const init = async () => {
      if (!user || !token) return
      const convId = await fetchConversation()
      if (!mounted) return
      if (convId) {
        await fetchMessages(convId)
      }
      // flush buffer for this conversation
      if (buffer.current.length > 0) {
        const toFlush = buffer.current.splice(0)
        toFlush.forEach(p => {
          if (p.conversationId === convId) {
            const mid = String(p.messageId || p.id || (p.timestamp + '_' + Math.random()))
            if (!messageIds.current.has(mid)) {
              messageIds.current.add(mid)
              setMessages(prev => [...prev, {
                id: mid, conversationId: convId,
                sender: p.sender, content: p.content, timestamp: p.timestamp
              }])
            }
          }
        })
      }
    }
    init()
    return () => { mounted = false }
  }, [user, token])

  const sendMessage = async (content) => {
    try {
      // do not create conversation client-side; send conversationId (may be null) per spec
      chatService.sendChatMessage(conversationId, content)
      // optimistic UI
      const tempId = `temp_${Date.now()}_${Math.random()}`
      const temp = { id: tempId, conversationId, sender: { id: user?.id || user?._id }, content, timestamp: Date.now() }
      messageIds.current.add(tempId)
      setMessages(prev => [...prev, temp])
    } catch (e) {
      console.error('Send message failed', e)
      setError('Không thể gửi tin nhắn')
    }
  }

  const value = {
    connected,
    conversationId,
    messages,
    loading,
    error,
    sendMessage,
    refreshConversation: fetchConversation,
    fetchMessages
  }

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}

export default ChatContext


