// Use plain WebSocket to talk to backend ChatWebSocketHandler (JSON messages)
class ChatService {
  constructor() {
    this.ws = null;
    this._isConnected = false;
    this.messageCallbacks = [];
    this.errorCallbacks = [];
    this.reconnectAttempts = 0;
    this.userId = null;
    this.token = null;
  }

  connect(token, userId) {
    if (this.ws && this._isConnected) return this.ws;
    this.token = token;
    this.userId = userId;

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';
    const backendRoot = apiUrl.replace(/\/api\/?$/, '');
    const protoRoot = backendRoot.replace(/^http/, 'ws');
    const tokenQuery = token ? `&token=${encodeURIComponent(token)}` : '';
    const wsEndpoint = `${protoRoot}/ws-chat?userId=${userId || ''}${tokenQuery}`;

    try {
      this.ws = new WebSocket(wsEndpoint);
    } catch (e) {
      console.error('Failed to create WebSocket', e);
      this._isConnected = false;
      this._scheduleReconnect();
      return null;
    }

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this._isConnected = true;
      this.reconnectAttempts = 0;
    };

    this.ws.onmessage = (evt) => {
      try {
        const data = JSON.parse(evt.data);
        if (data.type === 'ERROR') {
          this.errorCallbacks.forEach(cb => cb(data));
        } else {
          this.messageCallbacks.forEach(cb => cb(data));
        }
      } catch (e) {
        console.error('Error parsing WS message', e);
      }
    };

    this.ws.onerror = (err) => {
      console.error('WebSocket error', err);
      this.errorCallbacks.forEach(cb => cb(err));
    };

    this.ws.onclose = (evt) => {
      console.log('WebSocket disconnected', evt);
      this._isConnected = false;
      this._scheduleReconnect();
    };

    return this.ws;
  }

  disconnect() {
    if (this.ws) {
      try {
        this.ws.close();
      } catch (e) {
        console.warn('Error during WS disconnect', e);
      }
    }
    this.ws = null;
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
    if (!this.ws || !this._isConnected) throw new Error('Not connected');
    const message = { type: 'SEND_MESSAGE', toUserId, content, ...options };
    this.ws.send(JSON.stringify(message));
  }

  sendToAdmin(content, options = {}) {
    if (!this.ws || !this._isConnected) throw new Error('Not connected');
    const message = { type: 'SEND_TO_ADMIN', content, ...options };
    this.ws.send(JSON.stringify(message));
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
    return !!(this.ws && this._isConnected);
  }
}

const chatService = new ChatService();
export default chatService;
