import axios from 'axios';

// 环境配置
const env = process.env.NODE_ENV || 'development';

const config = {
  development: {
    baseURL: '/api',
  },
  staging: {
    baseURL: 'https://staging-api.example.com/api',
  },
  production: {
    baseURL: 'https://api.example.com/api',
  },
};

// 创建axios实例
const api = axios.create({
  baseURL: config[env].baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 简单的内存缓存
const cache = new Map();
const CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存

// 生成缓存键
const generateCacheKey = (config) => {
  if (config.method !== 'get') return null;
  return `${config.baseURL}${config.url}${JSON.stringify(config.params)}`;
};

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 检查缓存
    const cacheKey = generateCacheKey(config);
    if (cacheKey) {
      const cachedItem = cache.get(cacheKey);
      if (cachedItem) {
        const { data, timestamp } = cachedItem;
        if (Date.now() - timestamp < CACHE_DURATION) {
          // 返回缓存的响应
          return Promise.resolve({ data });
        } else {
          // 缓存过期，移除
          cache.delete(cacheKey);
        }
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 只返回响应数据
    const data = response.data;
    
    // 缓存GET请求的响应
    const cacheKey = generateCacheKey(response.config);
    if (cacheKey) {
      cache.set(cacheKey, {
        data,
        timestamp: Date.now()
      });
    }
    
    return data;
  },
  (error) => {
    // 统一错误处理
    if (error.response) {
      // 服务器返回错误状态码
      switch (error.response.status) {
        case 401:
          // 未授权，跳转到登录页
          window.location.href = '/login';
          break;
        case 403:
          console.error('禁止访问');
          break;
        case 404:
          console.error('资源不存在');
          break;
        case 500:
          console.error('服务器内部错误');
          break;
        default:
          console.error('请求失败:', error.response.data.message);
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('网络错误，无法连接到服务器');
    } else {
      // 请求配置出错
      console.error('请求配置错误:', error.message);
    }
    return Promise.reject(error);
  }
);

// API模块
const apiModules = {
  // 认证模块
  auth: {
    // 用户注册
    register: (data) => api.post('/auth/register', data),
    // 用户登录
    login: (data) => api.post('/auth/login', data),
    // 刷新令牌
    refresh: (data) => api.post('/auth/refresh', data),
    // 用户登出
    logout: (data) => api.post('/auth/logout', data),
    // 忘记密码
    forgotPassword: (data) => api.post('/auth/forgot-password', data),
    // 重置密码
    resetPassword: (data) => api.post('/auth/reset-password', data),
    // 修改密码
    changePassword: (data) => api.put('/auth/change-password', data),
    // 获取用户信息
    getUserInfo: (userId) => api.get(`/auth/user?user_id=${userId}`),
    // 更新用户信息
    updateUserInfo: (data) => api.put('/auth/user', data),
  },

  // 多场景问答中心
  qa: {
    // 专业课问答
    professional: (data) => api.post('/qa/professional', data),
    // 章节内容查询
    getChapterContent: (params) => api.get('/qa/professional/chapter', { params }),
    // 文字作业答疑
    homeworkText: (data) => api.post('/qa/homework/text', data),
    // 图片作业答疑
    homeworkImage: (data) => api.post('/qa/homework/image', data),
    // 作业批量答疑
    homeworkBatch: (data) => api.post('/qa/homework/batch', data),
    // 考研/考证问答
    exam: (data) => api.post('/qa/exam', data),
    // 考试大纲解析
    getExamSyllabus: (params) => api.get('/qa/exam/syllabus', { params }),
    // 备考计划生成
    generateStudyPlan: (data) => api.post('/qa/exam/study-plan', data),
  },

  // 知识图谱可视化
  knowledgeGraph: {
    // 获取知识点关联图谱
    getGraph: (params) => api.get('/knowledge-graph', { params }),
    // 获取知识点原文片段
    getOriginalText: (params) => api.get('/knowledge-graph/original-text', { params }),
    // 知识点关联分析
    analyzeRelationship: (data) => api.post('/knowledge-graph/analysis', data),
    // 知识图谱导出
    exportGraph: (params) => api.get('/knowledge-graph/export', { params }),
  },

  // 问答记录管理
  qaHistory: {
    // 获取历史提问
    getHistory: (params) => api.get('/qa-history', { params }),
    // 删除历史提问
    deleteHistory: (qaId, data) => api.delete(`/qa-history/${qaId}`, { data }),
    // 批量删除历史提问
    batchDeleteHistory: (data) => api.delete('/qa-history/batch', { data }),
    // 收藏优质答案
    collectAnswer: (data) => api.post('/qa-history/collect', data),
    // 取消收藏
    uncollectAnswer: (collectionId, data) => api.delete(`/qa-history/collect/${collectionId}`, { data }),
    // 生成学习笔记
    generateNote: (data) => api.post('/qa-history/generate-note', data),
    // 导出PDF
    exportPDF: (params) => api.get('/qa-history/export-pdf', { params }),
    // 导出其他格式
    exportOther: (params) => api.get('/qa-history/export', { params }),
  },

  // 资源拓展推荐
  resources: {
    // 获取教材配套资源
    getTextbookResources: (params) => api.get('/resources/textbook', { params }),
    // 图书馆资源查询
    searchLibrary: (params) => api.get('/resources/library', { params }),
    // 资源搜索
    searchResources: (params) => api.get('/resources/search', { params }),
    // 资源推荐
    getRecommendations: (params) => api.get('/resources/recommendations', { params }),
    // 资源收藏
    collectResource: (data) => api.post('/resources/collect', data),
  },

  // 个人学习书架
  bookshelf: {
    // 获取书架列表
    getBookshelf: (params) => api.get('/bookshelf', { params }),
    // 添加教材到书架
    addBook: (data) => api.post('/bookshelf/add', data),
    // 从书架移除教材
    removeBook: (data) => api.delete('/bookshelf/remove', { data }),
    // 更新教材学习状态
    updateBookStatus: (data) => api.put('/bookshelf/status', data),
    // 获取教材详情
    getBookDetail: (params) => api.get('/bookshelf/book', { params }),
    // 获取教材关联信息
    getRelatedInfo: (params) => api.get('/bookshelf/related', { params }),
    // 更新教材信息
    updateBookInfo: (data) => api.put('/bookshelf/book', data),
  },

  // 学长学姐答疑专区
  mentor: {
    // 发布求助帖
    postQuestion: (data) => api.post('/mentor/question', data),
    // 获取求助帖列表
    getQuestions: (params) => api.get('/mentor/questions', { params }),
    // 获取求助帖详情
    getQuestionDetail: (questionId, params) => api.get(`/mentor/question/${questionId}`, { params }),
    // AI自动解答
    getAIAnswer: (data) => api.post('/mentor/ai-answer', data),
    // 学长人工解答
    postAnswer: (data) => api.post('/mentor/answer', data),
    // 标记最佳解答
    markBestAnswer: (data) => api.put('/mentor/best-answer', data),
    // 回答点赞/点踩
    rateAnswer: (data) => api.post('/mentor/answer/rate', data),
    // 学长列表
    getMentors: (params) => api.get('/mentor/list', { params }),
    // 求助帖管理
    updateQuestion: (questionId, data) => api.put(`/mentor/question/${questionId}`, data),
    // 删除求助帖
    deleteQuestion: (questionId, data) => api.delete(`/mentor/question/${questionId}`, { data }),
  },

  // 文件上传模块
  upload: {
    // 上传图片
    uploadImage: (data) => api.post('/upload/image', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
    // 上传文档
    uploadDocument: (data) => api.post('/upload/document', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
    // 批量上传
    batchUpload: (data) => api.post('/upload/batch', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
    // 删除文件
    deleteFile: (fileId, data) => api.delete(`/upload/file/${fileId}`, { data }),
  },

  // 通知模块
  notifications: {
    // 获取通知列表
    getNotifications: (params) => api.get('/notifications', { params }),
    // 标记通知为已读
    markAsRead: (data) => api.put('/notifications/read', data),
    // 删除通知
    deleteNotification: (notificationId, data) => api.delete(`/notifications/${notificationId}`, { data }),
    // 获取通知设置
    getSettings: (params) => api.get('/notifications/settings', { params }),
    // 更新通知设置
    updateSettings: (data) => api.put('/notifications/settings', data),
  },

  // 统计分析模块
  analytics: {
    // 获取学习统计
    getStudyStats: (params) => api.get('/analytics/study', { params }),
    // 获取问答统计
    getQaStats: (params) => api.get('/analytics/qa', { params }),
    // 获取书架统计
    getBookshelfStats: (params) => api.get('/analytics/bookshelf', { params }),
    // 获取学长答疑统计
    getMentorStats: (params) => api.get('/analytics/mentor', { params }),
  },

  // 系统配置模块
  system: {
    // 获取系统配置
    getConfig: (params) => api.get('/system/config', { params }),
    // 获取学科列表
    getSubjects: () => api.get('/system/subjects'),
    // 获取教材列表
    getTextbooks: (params) => api.get('/system/textbooks', { params }),
    // 反馈提交
    submitFeedback: (data) => api.post('/system/feedback', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
    // 获取帮助文档
    getHelpDocs: (params) => api.get('/system/help', { params }),
  },

  // 搜索模块
  search: {
    // 全局搜索
    globalSearch: (params) => api.get('/search', { params }),
    // 搜索建议
    getSuggestions: (params) => api.get('/search/suggestions', { params }),
    // 热门搜索
    getHotKeywords: (params) => api.get('/search/hot', { params }),
  },
};

export default apiModules;