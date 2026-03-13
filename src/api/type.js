/**
 * API相关类型定义、接口声明和常量
 * 注意：本文件使用TypeScript语法，建议将文件扩展名改为.ts以获得完整的类型检查
 */

// 环境配置类型
export const ENVIRONMENTS = {
  DEVELOPMENT: 'development',
  STAGING: 'staging',
  PRODUCTION: 'production',
};

// API响应状态码
export const STATUS_CODES = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503,
};

// 通知类型
export const NOTIFICATION_TYPES = {
  ANSWER: 'answer',
  COMMENT: 'comment',
  SYSTEM: 'system',
  MENTOR: 'mentor',
};

// 资源类型
export const RESOURCE_TYPES = {
  COURSEWARE: 'courseware',
  VIDEO: 'video',
  EXERCISE: 'exercise',
  BOOK: 'book',
};

// 学科类型
export const SUBJECTS = {
  MATH: '数学',
  PHYSICS: '物理',
  ENGLISH: '英语',
  COMPUTER: '计算机',
  CHEMISTRY: '化学',
  BIOLOGY: '生物',
};

// 学习状态
export const STUDY_STATUS = {
  NOT_STARTED: 'not_started',
  LEARNING: 'learning',
  COMPLETED: 'completed',
};

// 问题类型
export const QUESTION_TYPES = {
  TEXT: 'text',
  IMAGE: 'image',
};

// 考试类型
export const EXAM_TYPES = {
  POSTGRADUATE: '考研',
  CERTIFICATION: '考证',
};

// 响应数据结构
export class ApiResponse {
  constructor(code, message, data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }
}

// 用户信息结构
export class UserInfo {
  constructor({
    user_id,
    username,
    email,
    phone = '',
    avatar = '',
    student_id = '',
    school = '',
    major = '',
    grade = '',
    user_role,
    permissions = [],
    created_at,
    last_login_at,
    profile_completion = 0,
    study_stats = {
      total_questions: 0,
      total_answers: 0,
      books_in_shelf: 0,
      study_days: 0
    }
  }) {
    this.user_id = user_id;
    this.username = username;
    this.email = email;
    this.phone = phone;
    this.avatar = avatar;
    this.student_id = student_id;
    this.school = school;
    this.major = major;
    this.grade = grade;
    this.user_role = user_role;
    this.permissions = permissions;
    this.created_at = created_at;
    this.last_login_at = last_login_at;
    this.profile_completion = profile_completion;
    this.study_stats = study_stats;
  }
}

// 登录请求参数
export class LoginParams {
  constructor({
    username = '',
    email = '',
    phone = '',
    password,
    remember_me = false
  }) {
    this.username = username;
    this.email = email;
    this.phone = phone;
    this.password = password;
    this.remember_me = remember_me;
  }
}

// 注册请求参数
export class RegisterParams {
  constructor({
    username,
    email,
    password,
    phone = '',
    student_id = '',
    school = '',
    major = '',
    grade = ''
  }) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.phone = phone;
    this.student_id = student_id;
    this.school = school;
    this.major = major;
    this.grade = grade;
  }
}

// 分页参数
export class PaginationParams {
  constructor({
    page = 1,
    page_size = 10
  }) {
    this.page = page;
    this.page_size = page_size;
  }
}

// 排序参数
export class SortParams {
  constructor({
    sort_by = 'created_at',
    sort_order = 'desc'
  }) {
    this.sort_by = sort_by;
    this.sort_order = sort_order;
  }
}

// 通用查询参数
export class QueryParams {
  constructor({
    page = 1,
    page_size = 10,
    sort_by = 'created_at',
    sort_order = 'desc',
    keyword = ''
  }) {
    this.page = page;
    this.page_size = page_size;
    this.sort_by = sort_by;
    this.sort_order = sort_order;
    this.keyword = keyword;
  }
}

// 错误处理类型
export class ApiError extends Error {
  constructor(message, code = 500, data = null) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.data = data;
  }
}

// 通用工具函数
export const validateResponse = (response) => {
  if (!response || typeof response !== 'object') {
    throw new ApiError('无效的响应数据', 400);
  }
  if (response.code !== STATUS_CODES.SUCCESS) {
    throw new ApiError(response.message || '请求失败', response.code || 500, response.data);
  }
  return response.data;
};

// 导出所有类型和常量作为默认对象
export default {
  ENVIRONMENTS,
  STATUS_CODES,
  NOTIFICATION_TYPES,
  RESOURCE_TYPES,
  SUBJECTS,
  STUDY_STATUS,
  QUESTION_TYPES,
  EXAM_TYPES,
  ApiResponse,
  UserInfo,
  LoginParams,
  RegisterParams,
  PaginationParams,
  SortParams,
  QueryParams,
  ApiError,
  validateResponse
};