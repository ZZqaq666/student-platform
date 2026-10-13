import axios from 'axios';
import { useAuthStore } from '@/stores/auth';
import router from '@/router/index.js';

// 缓存系统
const cache = {
  data: {},
  // 添加缓存
  set(key, value, expire = 5 * 60 * 1000) { // 默认缓存5分钟
    this.data[key] = {
      value,
      expire: Date.now() + expire
    };
  },
  // 获取缓存
  get(key) {
    const item = this.data[key];
    if (!item) return null;
    if (Date.now() > item.expire) {
      this.remove(key);
      return null;
    }
    return item.value;
  },
  // 移除缓存
  remove(key) {
    delete this.data[key];
  },
  // 清空所有缓存
  clear() {
    this.data = {};
  }
};

// 书架列表与章节树有 5 分钟缓存：写操作成功后必须失效，
// 否则删掉的书、改过的教材还会在缓存过期前继续出现在列表里。
// 请求失败时不清——服务端什么都没改。
function withCacheInvalidation(mutate) {
  return async (...args) => {
    const result = await mutate(...args);
    cache.clear();
    return result;
  };
}

// 创建axios实例
const api = axios.create({
  baseURL: '/api', // 基础URL，根据实际情况修改
  timeout: 180000, // 请求超时时间，增加到180秒（3分钟）以适应AI响应，比后端120秒更长
  // 处理大数字ID，避免精度丢失
  transformResponse: [function (data) {
    // 尝试解析JSON
    try {
      // 自定义JSON解析，将大数字转换为字符串
      return JSON.parse(data, (key, value) => {
        // 检查是否是可能的大数字ID
        if (typeof value === 'number' && (key === 'id' || key === 'userId' || key === 'bookId' || key === 'knowledgeNodeId' || key === 'exerciseId' || key === 'wrongBookId' || key === 'questionId' || key === 'answerId' || key === 'seniorId' || key === 'sourceNodeId' || key === 'targetNodeId')) {
          // 检查是否超过JavaScript安全整数范围
          if (Math.abs(value) > Number.MAX_SAFE_INTEGER) {
            return String(value);
          }
        }
        return value;
      });
    } catch (e) {
      // 如果解析失败，返回原始数据
      return data;
    }
  }]
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    const authStore = useAuthStore();
    const token = authStore.token;
    
    // 对于FormData请求，不设置Content-Type
    if (config.data instanceof FormData) {
      // 让浏览器自动设置Content-Type，包含正确的boundary
      delete config.headers['Content-Type'];
    } else {
      // 对于普通请求，设置Content-Type为application/json
      config.headers['Content-Type'] = 'application/json';
    }
    
    // 匿名接口不带令牌；/auth/logout 必须带，否则后端拿不到 jti 无法吊销
    const anonymousAuth = ['/auth/login', '/auth/register', '/auth/refresh'];
    const isAnonymous = anonymousAuth.some(path => config.url.includes(path));

    if (token && !isAnonymous) {
      // 不在本地预判过期：过期交给服务端返回 401，由响应拦截器静默续期后重放。
      // 原先在这里直接踢回登录页，access 令牌一过期就会打断用户操作。
      config.headers.Authorization = `Bearer ${token}`;
      
      // 添加userId到查询参数
      const userId = authStore.userId;
      if (userId) {
        if (!config.params) {
          config.params = {};
        }
        config.params.userId = userId;
      }
    } else {
      // 匿名接口确保不带 Authorization 头
      delete config.headers.Authorization;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 访问令牌有效期只有 15 分钟，过期后在这里静默续期；并发请求共用同一次刷新。
let refreshPromise = null;

function refreshTokensOnce() {
  if (!refreshPromise) {
    refreshPromise = axios.post('/api/auth/refresh', {
      refreshToken: localStorage.getItem('refreshToken')
    }).then(res => {
      const data = res.data?.data;
      if (!data?.token) return null;
      const authStore = useAuthStore();
      authStore.setToken(data.token, data.expiry);
      // 后端已轮换，旧刷新令牌同时作废，必须用返回值覆盖本地副本
      authStore.setRefreshToken(data.refreshToken);
      return data.token;
    }).catch(() => null).finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function retryAfterRefresh(config) {
  if (!config || config._retriedAfterRefresh || config.url.includes('/auth/')) return null;
  if (!localStorage.getItem('refreshToken')) return null;

  const newToken = await refreshTokensOnce();
  if (!newToken) return null;

  config._retriedAfterRefresh = true;
  config.headers.Authorization = `Bearer ${newToken}`;
  return api(config);
}

// 响应拦截器
api.interceptors.response.use(
  response => {
    // For stream responses, return the entire response object
    if (response.config.responseType === 'stream') {
      return response;
    }
    // For regular responses, return just the data
    return response.data;
  },
  async error => {
    // 统一错误处理
    console.error('API Error:', error);
    
    // 分类处理不同类型的错误
    let errorMessage = '网络错误，请稍后重试';
    
    if (error.response) {
      // 服务器返回错误状态码
      switch (error.response.status) {
        case 400:
          errorMessage = error.response.data?.message || '请求参数错误';
          break;
        case 401: {
          // 先用刷新令牌续期并重放一次，避免 15 分钟的短令牌把用户踢下线
          const replayed = await retryAfterRefresh(error.config);
          if (replayed) return replayed;

          errorMessage = '未授权，请重新登录';
          // 跳转到登录页
          const authStore = useAuthStore();
          authStore.logout();
          router.push('/login');
          break;
        }
        case 403:
          errorMessage = '权限不足，无法访问';
          break;
        case 404:
          errorMessage = '请求的资源不存在';
          break;
        case 500:
          errorMessage = '服务器内部错误';
          break;
        case 502:
          errorMessage = '网关错误';
          break;
        case 503:
          errorMessage = '服务不可用';
          break;
        case 504:
          errorMessage = '请求超时';
          break;
        default:
          errorMessage = `请求失败 (${error.response.status})`;
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      errorMessage = '网络连接失败，请检查网络设置';
    } else {
      // 请求配置出错
      errorMessage = error.message || '请求配置错误';
    }
    
    // 增强错误对象，添加错误信息
    error.errorMessage = errorMessage;
    return Promise.reject(error);
  }
);

// 生成缓存键
const generateCacheKey = (url, params) => {
  return `${url}_${JSON.stringify(params || {})}`;
};

// 参数验证函数
const validateParams = (params, rules) => {
  if (!params) return { valid: true };
  
  for (const [key, rule] of Object.entries(rules)) {
    const value = params[key];
    
    // 检查必填字段
    if (rule.required && (value === undefined || value === null || value === '')) {
      return { valid: false, message: `参数 ${key} 是必填项` };
    }
    
    // 检查数据类型
    if (value !== undefined && value !== null && rule.type) {
      const isValidType = typeof value === rule.type;
      if (!isValidType) {
        return { valid: false, message: `参数 ${key} 类型错误，期望 ${rule.type} 类型` };
      }
    }
    
    // 检查长度
    if (value !== undefined && value !== null && rule.minLength) {
      if (value.length < rule.minLength) {
        return { valid: false, message: `参数 ${key} 长度不能小于 ${rule.minLength}` };
      }
    }
    
    if (value !== undefined && value !== null && rule.maxLength) {
      if (value.length > rule.maxLength) {
        return { valid: false, message: `参数 ${key} 长度不能大于 ${rule.maxLength}` };
      }
    }
    
    // 检查范围
    if (value !== undefined && value !== null && rule.min) {
      if (value < rule.min) {
        return { valid: false, message: `参数 ${key} 不能小于 ${rule.min}` };
      }
    }
    
    if (value !== undefined && value !== null && rule.max) {
      if (value > rule.max) {
        return { valid: false, message: `参数 ${key} 不能大于 ${rule.max}` };
      }
    }
  }
  
  return { valid: true };
};

// 封装常用的请求方法
const request = {
  get: async (url, params, options = {}) => {
    try {
      const { useCache = false, expire, headers, rules, signal, responseType } = options;

      // 验证参数
      if (rules) {
        const validation = validateParams(params, rules);
        if (!validation.valid) {
          throw new Error(validation.message);
        }
      }

      // blob 等非 JSON 响应不缓存
      if (useCache && !responseType) {
        const cacheKey = generateCacheKey(url, params);
        const cachedData = cache.get(cacheKey);
        if (cachedData) {
          return Promise.resolve(cachedData);
        }
      }

      const config = {};
      if (params) {
        config.params = params;
      }
      if (headers) {
        config.headers = headers;
      }
      if (signal) {
        config.signal = signal;
      }
      if (responseType) {
        config.responseType = responseType;
      }

      const data = await api.get(url, config);
      if (useCache && !responseType) {
        const cacheKey = generateCacheKey(url, params);
        cache.set(cacheKey, data, expire);
      }
      return data;
    } catch (error) {
      console.error(`GET ${url} error:`, error);
      throw error;
    }
  },
  post: async (url, data, options = {}) => {
    try {
      const { headers, rules } = options;
      
      // 验证参数
      if (rules) {
        const validation = validateParams(data, rules);
        if (!validation.valid) {
          throw new Error(validation.message);
        }
      }
      
      const config = {};
      if (headers) {
        config.headers = headers;
      }
      
      return await api.post(url, data, config);
    } catch (error) {
      console.error(`POST ${url} error:`, error);
      throw error;
    }
  },
  put: async (url, data, options = {}) => {
    try {
      const { headers, rules } = options;
      
      // 验证参数
      if (rules) {
        const validation = validateParams(data, rules);
        if (!validation.valid) {
          throw new Error(validation.message);
        }
      }
      
      const config = {};
      if (headers) {
        config.headers = headers;
      }
      
      return await api.put(url, data, config);
    } catch (error) {
      console.error(`PUT ${url} error:`, error);
      throw error;
    }
  },
  delete: async (url, params, options = {}) => {
    try {
      const { headers, rules } = options;
      
      // 验证参数
      if (rules) {
        const validation = validateParams(params, rules);
        if (!validation.valid) {
          throw new Error(validation.message);
        }
      }
      
      const config = { params };
      if (headers) {
        config.headers = headers;
      }
      
      return await api.delete(url, config);
    } catch (error) {
      console.error(`DELETE ${url} error:`, error);
      throw error;
    }
  }
};

// 导出API模块
export default request;

// 可以在这里定义具体的API接口
export const apiService = {
  // 认证相关
  auth: {
    login: (data) => request.post('/auth/login', data),
    register: (data) => request.post('/auth/register', data),
    validate: () => request.get('/auth/validate'),
    // 同时提交刷新令牌，让后端把这一对令牌都吊销
    logout: (refreshToken) => request.post('/auth/logout', { refreshToken })
  },
  
  // 个人书架相关
  bookshelf: {
    getAllBooks: (signal) => request.get('/bookshelf/books', null, { useCache: true, signal }), // 启用缓存
    getBookById: (id) => request.get(`/bookshelf/books/${id}`, null, { useCache: true }), // 启用缓存
    searchBooks: (keyword, major, semester, courseType, signal) => request.get('/bookshelf/books/search', { keyword, major, semester, courseType }, { signal }), // 不缓存搜索结果
    getBooksBySubject: (subject) => request.get(`/bookshelf/books/subject/${subject}`, null, { useCache: true }), // 启用缓存
    getUserBooks: () => request.get('/bookshelf/my'), // 不缓存用户数据
    getUserBook: (bookId) => request.get(`/bookshelf/my/${bookId}`), // 不缓存用户数据
    addBookToShelf: withCacheInvalidation(bookId => request.post(`/bookshelf/my/${bookId}`)),
    removeBookFromShelf: withCacheInvalidation(bookId => request.delete(`/bookshelf/my/${bookId}`)),
    updateProgress: (bookId, data) => request.put(`/bookshelf/my/${bookId}/progress`, data),
    getBookChapters: (bookId) => request.get(`/books/${bookId}/chapters`, null, { useCache: true }), // 启用缓存
    getBookChapterTree: (bookId) => request.get(`/books/${bookId}/chapters/tree`, null, { useCache: true }), // 启用缓存
    searchChapters: (bookId, keyword) => request.get(`/books/${bookId}/chapters/search`, { keyword }), // 不缓存搜索结果
    getBookRecommendations: (bookId) => request.get(`/books/${bookId}/recommendations`, null, { useCache: true }), // 启用缓存
    getHighFrequencyKnowledge: (bookId, signal) => request.get(`/books/${bookId}/high-frequency`, null, { signal }), // 获取书籍高频知识点
    getRelatedNodes: (bookId, signal) => request.get(`/books/${bookId}/related-nodes`, null, { signal }), // 获取书籍关联知识点
    getTextbooks: (params) => request.get('/bookshelf/books/search', params), // 获取教材列表
    addBooks: withCacheInvalidation((userId, data) => request.post(`/books/add?userId=${userId}`, data)), // 批量添加教材
    deleteBook: withCacheInvalidation(bookId => request.delete(`/books/${bookId}`)) // 删除书籍
  },
  
  // 练习系统相关
  exercise: {
    getExercisesByBook: (bookId, pageable) => request.get(`/exercise/book/${bookId}`, pageable),
    getExercisesByKnowledgeNode: (knowledgeNodeId, pageable) => request.get(`/exercise/knowledge-node/${knowledgeNodeId}`, pageable),
    getExerciseById: (id) => request.get(`/exercise/${id}`, null, true), // 启用缓存
    submitAnswer: (data) => request.post('/exercise/submit', data),
    getAnswerHistory: (pageable) => request.get('/exercise/history', pageable),
    getWrongBook: (pageable) => request.get('/exercise/wrong-book', pageable),
    getExerciseStats: () => request.get('/exercise/stats'),
    updateWrongBookNotes: (wrongBookId, notes) => request.put(`/exercise/wrong-book/${wrongBookId}/notes`, notes),
    removeFromWrongBook: (wrongBookId) => request.delete(`/exercise/wrong-book/${wrongBookId}`)
  },
  
  // 知识图谱相关
  knowledgeGraph: {
    getKnowledgeGraphByBook: (bookId, signal) => request.get(`/knowledge-graph/book/${bookId}`, null, { signal }), // 不启用缓存，确保获取最新数据
    getKnowledgeTreeByBook: (bookId, signal) => request.get(`/knowledge-graph/book/${bookId}/tree`, null, { signal }), // 不启用缓存，确保获取最新数据
    getKnowledgeNodeById: (id, signal) => request.get(`/knowledge-graph/node/${id}`, null, { signal }), // 不启用缓存，确保获取最新数据
    getRelationsByNode: (nodeId, signal) => request.get(`/knowledge-graph/node/${nodeId}/relations`, null, { signal }), // 不启用缓存，确保获取最新数据
    searchKnowledgeNodes: (bookId, keyword, signal) => request.get(`/knowledge-graph/book/${bookId}/search`, { keyword }, { signal })
  },
  
  // AI问答相关
  qa: {
    /**
     * 向AI提问
     * @param {Object} data - 请求数据
     * @param {string} data.question - 问题文本
     * @param {string} [data.subject] - 学科信息
     * @param {string} [data.context] - 上下文信息
     * @returns {Promise} AI回答
     * @description 向AI发送问题并获取回答
     */
    askQuestion: (data, options = {}) => {
      // 参数验证规则
      const rules = {
        question: {
          required: true,
          type: 'string',
          minLength: 1,
          maxLength: 5000
        },
        subject: {
          type: 'string',
          maxLength: 100
        },
        context: {
          type: 'string',
          maxLength: 10000
        }
      };
      
      return request.post('/qa/ask', data, {
        ...options,
        rules
      });
    },
    askQuestionStream: (data) => {
      // 流式输出不需要设置超时，因为它是持续的流
      return api.post('/qa/ask/stream', data, {
        responseType: 'stream'
      });
    },
    /**
     * 获取历史问答记录
     * @returns {Promise} 历史问答记录列表
     * @description 从后端获取用户的历史问答记录
     * @param {Object} options - 可选参数
     * @param {Object} options.headers - 请求头
     */
    getHistory: (options = {}) => {
      const authStore = useAuthStore();
      const params = options.params || {};
      return request.get('/qa/history', params, options);
    },
    getHistorySessions: (options = {}) => {
      const params = options.params || {};
      return request.get('/qa/history/sessions', params, options);
    },
    deleteHistory: (id) => request.delete(`/qa/history/${id}`),
    saveHistory: (data) => request.post('/qa/history', data),
    // 书籍相关
    getBooks: () => request.get('/qa/books'),
    getChapters: (bookId) => request.get(`/qa/books/${bookId}/chapters`),
    // 课程相关
    getCourses: () => request.get('/qa/courses'),
    getRecommendedCourses: (limit = 5) => request.get('/qa/courses/recommend', { limit }),
    // 考试相关
    getExamSubjects: () => request.get('/qa/exam/subjects'),
    getKeyPoints: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/key-points`),
    getExamPapers: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/papers`),
    getExamCourses: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/courses`),
    getExamHistory: () => request.get('/qa/exam/history'),
    // AI生成真题推荐
    generateExamPapersByAI: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/papers/ai-generate`),
    // 获取试卷题目
    getExamPaperQuestions: (paperId) => request.get(`/qa/exam/papers/${paperId}/questions`),
    // 考试准备Q&A历史记录
    getExamQaHistory: (options = {}) => {
      const authStore = useAuthStore();
      const params = options.params || {};
      return request.get('/qa/exam/qa/history', params, options);
    },
    deleteExamQaHistory: (id) => request.delete(`/qa/exam/qa/history/${id}`),
    /**
     * 保存考试准备Q&A历史记录
     * @param {Object} data - 请求数据
     * @param {string} data.question - 问题文本
     * @param {string} data.answer - 回答文本
     * @param {string} [data.subject] - 学科信息
     * @param {number} [data.userId] - 用户ID
     * @returns {Promise} 保存结果
     * @description 将考试准备Q&A历史记录保存到后端数据库
     */
    saveExamQaHistory: (data, options = {}) => {
      const authStore = useAuthStore();
      const userId = authStore.user?.id;
      
      // 确保userId是数字类型
      let finalUserId = data.userId || userId;
      if (finalUserId) {
        finalUserId = typeof finalUserId === 'string' ? parseInt(finalUserId, 10) : finalUserId;
        if (isNaN(finalUserId)) {
          throw new Error('userId必须是有效的数字');
        }
      }
      
      const requestData = {
        ...data,
        userId: finalUserId
      };
      
      const rules = {
        question: {
          required: true,
          type: 'string',
          minLength: 1,
          maxLength: 5000
        },
        answer: {
          required: true,
          type: 'string',
          minLength: 1,
          maxLength: 50000
        },
        subject: {
          type: 'string',
          maxLength: 100
        },
        userId: {
          required: false,
          type: 'number'
        }
      };
      
      return request.post('/qa/exam/qa/save', requestData, {
        ...options,
        rules
      });
    },
    // 考试准备课程推荐
    getExamRecommendedCourses: (limit = 5) => request.get('/qa/courses/recommend/exam', { limit }),
    // 图片问答
    askImageQuestion: (formData) => request.post('/qa/ask/image', formData),
    /**
     * 保存处理后的文本
     * @param {Object} data - 请求数据
     * @param {string} data.question - 问题文本
     * @param {string} data.processedText - 处理后的文本
     * @param {string} [data.subject] - 学科信息
     * @returns {Promise} 保存结果
     * @description 将处理后的文本保存到后端数据库
     */
    saveProcessedText: (data, options = {}) => {
      const authStore = useAuthStore();
      const userId = authStore.user?.id;
      
      const requestData = {
        ...data,
        userId: userId
      };
      
      const rules = {
        question: {
          required: true,
          type: 'string',
          minLength: 1,
          maxLength: 5000
        },
        processedText: {
          required: true,
          type: 'string',
          minLength: 1,
          maxLength: 50000
        },
        subject: {
          type: 'string',
          maxLength: 100
        },
        userId: {
          required: false,
          type: 'number'
        }
      };
      
      return request.post('/qa/save-processed-text', requestData, {
        ...options,
        rules
      });
    }
  },
  
  // 缓存管理
  cache: {
    clear: () => cache.clear()
  },
  
  // 学长学姐相关
  senior: {
    // 获取统计数据
    getStats: () => request.get('/senior/stats'),
    // 获取所有学长学姐
    getSeniors: () => request.get('/senior/list'),
    // 获取学长学姐详情
    getSeniorById: (id) => request.get(`/senior/${id}`),
    // 获取问题列表
    getQuestions: (params) => request.get('/senior/questions', params),
    // 获取问题详情
    getQuestionById: (id) => request.get(`/senior/questions/${id}`),
    // 发布问题
    publishQuestion: (data) => request.post('/senior/questions', data),
    // 点赞回答
    likeAnswer: (answerId) => request.post(`/senior/answers/${answerId}/like`),
    // 采纳回答
    acceptAnswer: (questionId, answerId) => {
      if (!questionId || questionId === 'undefined') {
        return Promise.reject(new Error('问题ID不能为空'));
      }
      return request.put(`/senior/questions/${questionId}/accept`, { answerId });
    },
    // 提交追问
    submitFollowUp: (questionId, content) => {
      if (!questionId || questionId === 'undefined') {
        return Promise.reject(new Error('问题ID不能为空'));
      }
      return request.post(`/senior/questions/${questionId}/follow-up`, { content });
    },
    // AI 生成失败后重新请求学长解答
    regenerateAnswer: (questionId) => {
      if (!questionId || questionId === 'undefined') {
        return Promise.reject(new Error('问题ID不能为空'));
      }
      return request.post(`/senior/questions/${questionId}/regenerate`);
    }
  },
  
  // 笔记相关
  note: {
    // 获取用户的所有笔记
    getAllNotes: () => request.get('/notes'),
    // 获取指定书籍的笔记
    getNotesByBook: (bookId) => request.get(`/notes/book/${bookId}`),
    // 获取指定章节的笔记
    getNotesByChapter: (chapterId) => request.get(`/notes/chapter/${chapterId}`),
    // 创建笔记
    createNote: (data) => request.post('/notes', data),
    // 更新笔记
    updateNote: (noteId, data) => request.put(`/notes/${noteId}`, data),
    // 删除笔记
    deleteNote: (noteId) => request.delete(`/notes/${noteId}`),
    // 获取笔记详情
    getNoteById: (noteId) => request.get(`/notes/${noteId}`)
  },
  
  // 学习相关
  learning: {
    // 保存学习进度
    saveProgress: (data) => request.post('/learning/progress', data),
    // 获取学习历史
    getHistory: (params) => request.get('/learning/history', params),
    // 获取学习曲线
    getCurve: (bookId, params) => request.get(`/learning/curve?bookId=${bookId}`, params)
  }
};