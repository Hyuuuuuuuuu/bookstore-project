import React from 'react'

const MessageList = ({ messages = [], currentUserId }) => {
  return (
    <div className="space-y-4">
      {messages.map((m, idx) => {
        const isMine = String(m.sender?.id) === String(currentUserId)
        const key = m.id || `msg_${idx}`
        return (
          <div key={key} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${isMine ? 'bg-amber-600 text-white' : 'bg-gray-100 text-gray-900'}`}>
              <div className="text-sm whitespace-pre-line">{m.content}</div>
              <div className={`text-xs mt-1 ${isMine ? 'text-amber-100' : 'text-gray-500'}`}>
                {new Date(m.timestamp).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default MessageList


