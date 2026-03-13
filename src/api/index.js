// API模块入口文件
import apiModules from './api';
import * as apiTypes from './type';

// 导出API模块
export { apiModules as api };

// 导出类型和常量
export { apiTypes as types };

// 导出默认API实例
export default apiModules;