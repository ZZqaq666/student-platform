import axios from 'axios';

// 流式服务类
class StreamingService {
  constructor() {
    this.wsConnections = new Map();
    this.sseConnections = new Map();
    this.baseUrl = window.location.origin;
  }

  // WebSocket连接管理
  connectWebSocket(endpoint, onMessage, onError, onClose) {
    const wsUrl = `${this.baseUrl.replace('http', 'ws')}/api${endpoint}`;
    const socket = new WebSocket(wsUrl);
    
    socket.onopen = () => {
      console.log(`WebSocket connected: ${wsUrl}`);
    };
    
    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };
    
    socket.onerror = (error) => {
      console.error(`WebSocket error: ${wsUrl}`, error);
      if (onError) onError(error);
    };
    
    socket.onclose = (event) => {
      console.log(`WebSocket closed: ${wsUrl}`, event);
      this.wsConnections.delete(endpoint);
      if (onClose) onClose(event);
    };
    
    this.wsConnections.set(endpoint, socket);
    return socket;
  }

  // 关闭WebSocket连接
  closeWebSocket(endpoint) {
    const socket = this.wsConnections.get(endpoint);
    if (socket) {
      socket.close();
      this.wsConnections.delete(endpoint);
    }
  }

  // 发送WebSocket消息
  sendWebSocketMessage(endpoint, message) {
    const socket = this.wsConnections.get(endpoint);
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      console.error('WebSocket not connected or closed');
    }
  }

  // SSE连接管理
  connectSSE(endpoint, onMessage, onError, onClose, options = {}) {
    const sseUrl = `${this.baseUrl}/api${endpoint}`;
    const {
      reconnectInterval = 3000, // 重连间隔（毫秒）
      maxReconnectAttempts = 5, // 最大重连次数
      headers = {}
    } = options;
    
    let reconnectAttempts = 0;
    let eventSource = null;
    let reconnectTimer = null;
    
    const createEventSource = () => {
      try {
        // 创建EventSource实例
        eventSource = new EventSource(sseUrl);
        
        eventSource.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            onMessage(data);
          } catch (error) {
            console.error('Error parsing SSE message:', error);
            if (onError) onError(error);
          }
        };
        
        eventSource.onerror = (error) => {
          console.error(`SSE error: ${sseUrl}`, error);
          if (onError) onError(error);
          
          // 处理连接错误，尝试重连
          if (eventSource.readyState === EventSource.CLOSED) {
            handleReconnect();
          }
        };
        
        eventSource.onclose = () => {
          console.log(`SSE closed: ${sseUrl}`);
          this.sseConnections.delete(endpoint);
          if (onClose) onClose();
        };
        
        // 重置重连尝试次数
        reconnectAttempts = 0;
        this.sseConnections.set(endpoint, eventSource);
      } catch (error) {
        console.error('Failed to create EventSource:', error);
        if (onError) onError(error);
        handleReconnect();
      }
    };
    
    const handleReconnect = () => {
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        console.log(`SSE reconnect attempt ${reconnectAttempts}/${maxReconnectAttempts}...`);
        
        reconnectTimer = setTimeout(() => {
          createEventSource();
        }, reconnectInterval);
      } else {
        console.error(`SSE max reconnect attempts reached for ${sseUrl}`);
        if (onError) onError(new Error('Max reconnect attempts reached'));
      }
    };
    
    // 初始创建连接
    createEventSource();
    
    // 返回连接对象，包含手动控制方法
    return {
      eventSource,
      close: () => {
        if (reconnectTimer) {
          clearTimeout(reconnectTimer);
        }
        if (eventSource) {
          eventSource.close();
        }
        this.sseConnections.delete(endpoint);
      },
      reconnect: () => {
        if (reconnectTimer) {
          clearTimeout(reconnectTimer);
        }
        if (eventSource) {
          eventSource.close();
        }
        reconnectAttempts = 0;
        createEventSource();
      }
    };
  }

  // 关闭SSE连接
  closeSSE(endpoint) {
    const connection = this.sseConnections.get(endpoint);
    if (connection) {
      if (connection.close) {
        connection.close();
      } else if (connection.eventSource && connection.eventSource.close) {
        connection.eventSource.close();
      }
      this.sseConnections.delete(endpoint);
    }
  }

  // 基于EventSource的SSE流式请求
  streamSSE(endpoint, params, onData, onComplete, onError, options = {}) {
    // 检查登录状态
    const token = localStorage.getItem('token');
    if (!token) {
      // 未登录，跳转到登录页面
      window.location.href = '/login';
      return {
        close: () => {},
        reconnect: () => {}
      };
    }
    
    // 对于 /qa/ask/stream 端点，使用 POST 方法
    if (endpoint === '/qa/ask/stream' || endpoint === '/qa/ask/stream/test') {
      // 直接传递params作为data，确保格式正确
      return this.streamPost(endpoint, params, onData, onComplete, onError);
    }
    
    // 其他 SSE 端点使用 GET 方法
    const queryString = Object.keys(params)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
      .join('&');
    const sseUrl = `${this.baseUrl}/api${endpoint}${queryString ? `?${queryString}` : ''}`;
    
    const {
      reconnectInterval = 3000,
      maxReconnectAttempts = 5
    } = options;
    
    let reconnectAttempts = 0;
    let eventSource = null;
    let reconnectTimer = null;
    let isCompleted = false;
    
    const createEventSource = () => {
      try {
        eventSource = new EventSource(sseUrl);
        
        eventSource.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            onData(data);
            
            // 检查是否完成
            if (data.completed) {
              isCompleted = true;
              if (onComplete) onComplete();
              closeEventSource();
            }
          } catch (error) {
            console.error('Error parsing SSE message:', error);
            if (onError) onError(error);
          }
        };
        
        eventSource.onerror = (error) => {
          console.error(`SSE stream error: ${sseUrl}`, error);
          if (onError) onError(error);
          
          if (eventSource.readyState === EventSource.CLOSED && !isCompleted) {
            handleReconnect();
          }
        };
        
        eventSource.onclose = () => {
          console.log(`SSE stream closed: ${sseUrl}`);
          if (!isCompleted && onError) {
            onError(new Error('Connection closed unexpectedly'));
          }
        };
        
        reconnectAttempts = 0;
      } catch (error) {
        console.error('Failed to create SSE stream:', error);
        if (onError) onError(error);
        handleReconnect();
      }
    };
    
    const handleReconnect = () => {
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        console.log(`SSE reconnect attempt ${reconnectAttempts}/${maxReconnectAttempts}...`);
        
        reconnectTimer = setTimeout(() => {
          createEventSource();
        }, reconnectInterval);
      } else {
        console.error(`SSE max reconnect attempts reached for ${sseUrl}`);
        if (onError) onError(new Error('Max reconnect attempts reached'));
      }
    };
    
    const closeEventSource = () => {
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
      if (eventSource) {
        eventSource.close();
      }
    };
    
    createEventSource();
    
    // 返回控制对象
    return {
      close: closeEventSource,
      reconnect: () => {
        closeEventSource();
        reconnectAttempts = 0;
        createEventSource();
      }
    };
  }

  // 基于fetch的流式POST请求
  streamPost(endpoint, data, onData, onComplete, onError) {
    // 检查登录状态
    const token = localStorage.getItem('token');
    if (!token) {
      // 未登录，跳转到登录页面
      window.location.href = '/login';
      return Promise.reject(new Error('Not logged in'));
    }
    
    const url = `${this.baseUrl}/api${endpoint}`;
    
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(data)
    })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      
      const processChunk = ({ done, value }) => {
        if (done) {
          if (onComplete) onComplete();
          return;
        }
        
        buffer += decoder.decode(value, { stream: true });
        
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          if (line.startsWith('data:')) {
            let dataStr = line.substring(5).trim();
            if (dataStr) {
              try {
                const data = JSON.parse(dataStr);
                onData(data);
                if (data.completed) {
                  if (onComplete) onComplete();
                }
              } catch (error) {
                console.error('Error parsing SSE data:', error);
              }
            }
          }
        }
        
        return reader.read().then(processChunk);
      };
      
      return reader.read().then(processChunk);
    })
    .catch(error => {
      console.error('POST stream error:', error);
      if (onError) onError(error);
    });
  }
  
  // 基于fetch的流式POST请求（测试端点）
  streamPostTest(endpoint, data, onData, onComplete, onError) {
    // 检查登录状态
    const token = localStorage.getItem('token');
    if (!token) {
      // 未登录，跳转到登录页面
      window.location.href = '/login';
      return Promise.reject(new Error('Not logged in'));
    }
    
    // 直接调用后端测试端点，携带认证token
    const testUrl = `http://localhost:8080${endpoint}`;
    
    return fetch(testUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(data)
    })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      
      const processChunk = ({ done, value }) => {
        if (done) {
          if (onComplete) onComplete();
          return;
        }
        
        buffer += decoder.decode(value, { stream: true });
        
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          if (line.startsWith('data:')) {
            let dataStr = line.substring(5).trim();
            if (dataStr) {
              try {
                const data = JSON.parse(dataStr);
                onData(data);
                if (data.completed) {
                  if (onComplete) onComplete();
                }
              } catch (error) {
                console.error('Error parsing SSE data:', error);
              }
            }
          }
        }
        
        return reader.read().then(processChunk);
      };
      
      return reader.read().then(processChunk);
    })
    .catch(error => {
      console.error('POST stream error:', error);
      if (onError) onError(error);
    });
  }

  // 清理所有连接
  cleanup() {
    // 关闭所有WebSocket连接
    for (const [endpoint, socket] of this.wsConnections.entries()) {
      socket.close();
    }
    this.wsConnections.clear();
    
    // 关闭所有SSE连接
    for (const [endpoint, eventSource] of this.sseConnections.entries()) {
      eventSource.close();
    }
    this.sseConnections.clear();
  }
}

// 导出单例实例
export const streamingService = new StreamingService();

// 导出流式API服务
export const streamingApiService = {
  // WebSocket接口
  ws: {
    // 实时练习数据
    getRealTimeExerciseData: (onMessage, onError, onClose) => {
      return streamingService.connectWebSocket('/ws/exercise', onMessage, onError, onClose);
    },
    
    // 实时知识图谱更新
    getRealTimeKnowledgeGraph: (bookId, onMessage, onError, onClose) => {
      return streamingService.connectWebSocket(`/ws/knowledge-graph/${bookId}`, onMessage, onError, onClose);
    },
    
    // 实时用户状态
    getUserStatus: (onMessage, onError, onClose) => {
      return streamingService.connectWebSocket('/ws/user/status', onMessage, onError, onClose);
    }
  },
  
  // SSE接口
  sse: {
    // 书籍更新通知
    getBookUpdates: (onMessage, onError, onClose) => {
      return streamingService.connectSSE('/sse/books/updates', onMessage, onError, onClose);
    },
    
    // 系统通知
    getSystemNotifications: (onMessage, onError, onClose) => {
      return streamingService.connectSSE('/sse/notifications', onMessage, onError, onClose);
    }
  },
  
  // 流式HTTP接口
  stream: {
    // AI问答流式输出
    askQuestionStream: (data, onData, onComplete, onError) => {
      return streamingService.streamPost('/qa/ask/stream', data, onData, onComplete, onError);
    },
    
    // AI问答流式输出（测试端点）
    askQuestionStreamTest: (data, onData, onComplete, onError) => {
      return streamingService.streamPostTest('/qa/ask/stream/test', data, onData, onComplete, onError);
    },
    
    // 大文件下载
    downloadLargeFile: (fileId, onData, onComplete, onError) => {
      return streamingService.streamPost('/files/download/stream', { fileId }, onData, onComplete, onError);
    },
    
    // 批量数据导出
    exportData: (data, onData, onComplete, onError) => {
      return streamingService.streamPost('/export/stream', data, onData, onComplete, onError);
    }
  },
  
  // SSE流式接口
  sseStream: {
    // AI问答SSE流式输出
    askQuestionSSE: (data, onData, onComplete, onError, options) => {
      // 构建查询参数
      const params = {
        question: data.question,
        context: data.context || '',
        subject: data.subject || ''
      };
      return streamingService.streamSSE('/qa/ask/stream', params, onData, onComplete, onError, options);
    },
    
    // AI问答SSE流式输出（测试端点）
    askQuestionSSETest: (data, onData, onComplete, onError, options) => {
      // 构建查询参数
      const params = {
        question: data.question,
        context: data.context || '',
        subject: data.subject || ''
      };
      return streamingService.streamSSE('/qa/ask/stream/test', params, onData, onComplete, onError, options);
    }
  }
};