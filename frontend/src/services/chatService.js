// Basic WebSocket client for chat
class ChatService {
  constructor() {
    this.socket = null;
    this._isConnected = false;
    this.messageCallbacks = [];
    this.errorCallbacks = [];
    this.reconnectAttempts = 0;
    this.userId = null;
    this.token = null;
    this._manualClose = false;
    this._heartbeatInterval = null;
  }

  connect(token, userId) {
    if (this.socket && this._isConnected) return this.socket;
    this.token = token;
    this.userId = userId;
    this._manualClose = false;

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';
    const backendRoot = apiUrl.replace(/\/api\/?$/, '');
    const wsUrl = `${backendRoot.replace(/^http/, 'ws')}/ws-chat?token=${encodeURIComponent(token)}`;

    try {
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        console.log('Connected to WebSocket');
        this._isConnected = true;
        this.reconnectAttempts = 0;
      };

      this.socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        this.messageCallbacks.forEach(cb => cb(message));
      };

      this.socket.onclose = () => {
        console.log('WebSocket disconnected');
        this._isConnected = false;
        if (this._heartbeatInterval) { clearInterval(this._heartbeatInterval); this._heartbeatInterval = null; }
        if (!this._manualClose) {
          this._scheduleReconnect();
        }
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error', error);
        this.errorCallbacks.forEach(cb => cb(error));
      };

      return this.socket;
    } catch (e) {
      console.error('Failed to create WebSocket', e);
      this._scheduleReconnect();
      return null;
    }
  }

  disconnect() {
    this._manualClose = true;
    if (this.socket) {
      try {
        this.socket.close();
      } catch (e) {
        console.warn('Error during WebSocket disconnect', e);
      }
    }
    if (this._heartbeatInterval) { clearInterval(this._heartbeatInterval); this._heartbeatInterval = null; }
    this.socket = null;
    this._isConnected = false;
  }

  _scheduleReconnect() {
    const delay = Math.min(30000, 1000 * Math.pow(2, this.reconnectAttempts));
    this.reconnectAttempts += 1;
    console.log(`Reconnect attempt ${this.reconnectAttempts} in ${delay}ms`);
    setTimeout(() => {
      this.connect(this.token, this.userId);
    }, delay);
  }

  sendMessage(toUserId, content, options = {}) {
    if (!this.socket || !this._isConnected) throw new Error('Not connected');
    const message = {
      type: "CHAT",
      sender: this.userId,
      toUserId: toUserId,
      content: content,
      timestamp: Date.now(),
      ...options
    };
    this.socket.send(JSON.stringify(message));
  }

  sendToAdmin(content, options = {}) {
    if (!this.socket || !this._isConnected) throw new Error('Not connected');
    const message = {
      type: "CHAT",
      sender: this.userId,
      toUserId: "admin",
      content: content,
      timestamp: Date.now(),
      ...options
    };
    this.socket.send(JSON.stringify(message));
  }

  onMessage(callback) {
    this.messageCallbacks.push(callback);
  }

  onError(callback) {
    this.errorCallbacks.push(callback);
  }

  offMessage(callback) {
    this.messageCallbacks = this.messageCallbacks.filter(cb => cb !== callback);
  }

  offError(callback) {
    this.errorCallbacks = this.errorCallbacks.filter(cb => cb !== callback);
  }

  get isConnected() {
    return !!(this.socket && this._isConnected);
  }
}

const chatService = new ChatService();
export default chatService;

// Auto-connect on module load if auth data exists in localStorage
try {
  const storedToken = localStorage.getItem('token');
  const storedUser = localStorage.getItem('user');
  if (storedToken && storedUser) {
    let parsedUser = null;
    try {
      parsedUser = JSON.parse(storedUser);
    } catch (e) {
      parsedUser = null;
    }
    const userId = parsedUser ? (parsedUser._id || parsedUser.id || parsedUser.userId) : null;
    if (storedToken && userId) {
      // attempt to connect immediately; connect() is idempotent
      chatService.connect(storedToken, userId);
    }
  }
} catch (e) {
  // ignore errors on access localStorage
}
