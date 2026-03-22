import axios from 'axios';
import { useAuthStore } from '@/store/auth.js';
import router from '@/router';

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

// 创建axios实例
const api = axios.create({
  baseURL: '/api', // 基础URL，根据实际情况修改
  timeout: 60000, // 请求超时时间，增加到60秒以适应AI响应
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    const token = localStorage.getItem('token');
    // 对于认证相关的路径，不添加token
    if (token && !config.url.includes('/auth/')) {
      // 检查token是否过期
      const tokenExpiry = localStorage.getItem('tokenExpiry');
      if (tokenExpiry && Date.now() > Number(tokenExpiry)) {
        // token过期，清除token并跳转到登录页
        const authStore = useAuthStore();
        authStore.logout();
        router.push('/login');
        return Promise.reject(new Error('Token已过期'));
      }
      config.headers.Authorization = `Bearer ${token}`;
    } else {
      // 对于认证相关的路径，确保不添加Authorization头
      delete config.headers.Authorization;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('Response config:', response.config);
    console.log('Response config responseType:', response.config.responseType);
    // For stream responses, return the entire response object
    if (response.config.responseType === 'stream') {
      console.log('Returning entire response object for stream');
      return response;
    }
    // For regular responses, return just the data
    console.log('Returning response.data for regular response');
    return response.data;
  },
  error => {
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
        case 401:
          errorMessage = '未授权，请重新登录';
          // 跳转到登录页
          const authStore = useAuthStore();
          authStore.logout();
          router.push('/login');
          break;
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

// 封装常用的请求方法
const request = {
  get: async (url, params, useCache = false, expire) => {
    try {
      if (useCache) {
        const cacheKey = generateCacheKey(url, params);
        const cachedData = cache.get(cacheKey);
        if (cachedData) {
          return Promise.resolve(cachedData);
        }
      }
      
      const data = await api.get(url, { params });
      if (useCache) {
        const cacheKey = generateCacheKey(url, params);
        cache.set(cacheKey, data, expire);
      }
      return data;
    } catch (error) {
      console.error(`GET ${url} error:`, error);
      throw error;
    }
  },
  post: async (url, data) => {
    try {
      return await api.post(url, data);
    } catch (error) {
      console.error(`POST ${url} error:`, error);
      throw error;
    }
  },
  put: async (url, data) => {
    try {
      return await api.put(url, data);
    } catch (error) {
      console.error(`PUT ${url} error:`, error);
      throw error;
    }
  },
  delete: async (url, params) => {
    try {
      return await api.delete(url, { params });
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
    validate: () => request.get('/auth/validate')
  },
  
  // 个人书架相关
  bookshelf: {
    getAllBooks: () => request.get('/bookshelf/books', null, true), // 启用缓存
    getBookById: (id) => request.get(`/bookshelf/books/${id}`, null, true), // 启用缓存
    searchBooks: (keyword) => request.get('/bookshelf/books/search', { keyword }), // 不缓存搜索结果
    getBooksBySubject: (subject) => request.get(`/bookshelf/books/subject/${subject}`, null, true), // 启用缓存
    getUserBooks: () => request.get('/bookshelf/my'), // 不缓存用户数据
    getUserBook: (bookId) => request.get(`/bookshelf/my/${bookId}`), // 不缓存用户数据
    addBookToShelf: (bookId) => request.post(`/bookshelf/my/${bookId}`),
    removeBookFromShelf: (bookId) => request.delete(`/bookshelf/my/${bookId}`),
    updateProgress: (bookId, data) => request.put(`/bookshelf/my/${bookId}/progress`, data)
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
    getKnowledgeGraphByBook: (bookId) => request.get(`/knowledge-graph/book/${bookId}`, null, true), // 启用缓存
    getKnowledgeTreeByBook: (bookId) => request.get(`/knowledge-graph/book/${bookId}/tree`, null, true), // 启用缓存
    getKnowledgeNodeById: (id) => request.get(`/knowledge-graph/node/${id}`, null, true), // 启用缓存
    getRelationsByNode: (nodeId) => request.get(`/knowledge-graph/node/${nodeId}/relations`, null, true), // 启用缓存
    searchKnowledgeNodes: (bookId, keyword) => request.get(`/knowledge-graph/book/${bookId}/search`, { keyword })
  },
  
  // AI问答相关
  qa: {
    askQuestion: (data) => request.post('/qa/ask', data),
    askQuestionStream: (data) => {
      // 流式输出不需要设置超时，因为它是持续的流
      return api.post('/qa/ask/stream', data, {
        responseType: 'stream'
      });
    },
    getHistory: () => request.get('/qa/history'),
    deleteHistory: (id) => request.delete(`/qa/history/${id}`),
    // 书籍相关
    getBooks: () => request.get('/qa/books'),
    getChapters: (bookId) => request.get(`/qa/books/${bookId}/chapters`),
    // 课程相关
    getCourses: () => request.get('/qa/courses'),
    // 考试相关
    getExamSubjects: () => request.get('/qa/exam/subjects'),
    getKeyPoints: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/key-points`),
    getExamPapers: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/papers`),
    getExamCourses: (subjectId) => request.get(`/qa/exam/subjects/${subjectId}/courses`),
    getExamHistory: () => request.get('/qa/exam/history'),
    // 图片问答
    askImageQuestion: (formData) => request.post('/qa/ask/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // 缓存管理
  cache: {
    clear: () => cache.clear()
  },
  
  // 学长学姐相关
  senior: {
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
    acceptAnswer: (questionId, answerId) => request.put(`/senior/questions/${questionId}/accept`, { answerId }),
    // 提交追问
    submitFollowUp: (questionId, content) => request.post(`/senior/questions/${questionId}/follow-up`, { content })
  }
};