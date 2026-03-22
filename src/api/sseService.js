// SSE服务，使用Fetch API实现

/**
 * 建立SSE连接
 * @param {string} url - SSE服务的URL
 * @param {Object} options - 配置选项
 * @param {Function} options.onMessage - 接收消息的回调函数
 * @param {Function} options.onComplete - 连接完成的回调函数
 * @param {Function} options.onError - 错误处理的回调函数
 * @param {number} options.reconnectInterval - 重连间隔（毫秒）
 * @param {number} options.maxReconnectAttempts - 最大重连次数
 * @param {Object} options.data - 请求数据（用于POST请求）
 * @param {string} options.method - 请求方法（默认为POST）
 * @returns {Object} - 连接控制对象，包含close方法
 */
export const establishSSEConnection = (url, options = {}) => {
  const {
    onMessage = () => {},
    onComplete = () => {},
    onError = () => {},
    reconnectInterval = 3000,
    maxReconnectAttempts = 5,
    data = null,
    method = 'POST'
  } = options;

  let reconnectAttempts = 0;
  let controller = new AbortController();
  let isConnected = false;

  const connect = () => {
    const fetchOptions = {
      method: method,
      headers: {
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache'
      },
      signal: controller.signal
    };

    // 如果有数据且方法为POST，添加Content-Type和body
    if (data && method === 'POST') {
      fetchOptions.headers['Content-Type'] = 'application/json';
      fetchOptions.body = JSON.stringify(data);
    }

    fetch(url, fetchOptions)
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        if (!response.body) {
          throw new Error('No response body');
        }

        isConnected = true;
        reconnectAttempts = 0;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        const processStream = async () => {
          try {
            const { done, value } = await reader.read();

            if (done) {
              onComplete();
              return;
            }

            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');

            lines.forEach(line => {
              if (line.startsWith('data: ')) {
                const dataStr = line.substring(6);
                if (dataStr === '[DONE]') {
                  return;
                }
                try {
                  const data = JSON.parse(dataStr);
                  onMessage(data);
                } catch (error) {
                  console.error('Error parsing SSE message:', error);
                }
              }
            });

            return processStream();
          } catch (error) {
            console.error('Stream processing error:', error);
            if (!controller.signal.aborted) {
              handleReconnect();
            }
          }
        };

        return processStream();
      })
      .catch(error => {
        console.error('SSE connection error:', error);
        if (!controller.signal.aborted) {
          handleReconnect();
        }
      });
  };

  const handleReconnect = () => {
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++;
      console.log(`Attempting to reconnect (${reconnectAttempts}/${maxReconnectAttempts})...`);
      setTimeout(connect, reconnectInterval);
    } else {
      onError(new Error('Max reconnect attempts reached'));
    }
  };

  const close = () => {
    controller.abort();
    isConnected = false;
  };

  // 开始连接
  connect();

  return {
    close,
    get isConnected() {
      return isConnected;
    }
  };
};

/**
 * 构建SSE URL
 * @param {string} baseUrl - 基础URL
 * @param {Object} params - 查询参数
 * @returns {string} - 完整的SSE URL
 */
export const buildSSEUrl = (baseUrl, params) => {
  const url = new URL(baseUrl);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      url.searchParams.append(key, value);
    }
  });
  return url.toString();
};
