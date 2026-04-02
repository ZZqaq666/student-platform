import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 添加全局错误处理器
app.config.errorHandler = (error, instance, info) => {
  // 忽略所有组件销毁后的DOM操作错误
  const ignoreErrors = [
    'insertBefore',
    'removeChild',
    'appendChild',
    '__vnode',
    'Cannot read properties of null',
    'Cannot set properties of null'
  ]

  if (ignoreErrors.some(keyword => error.message?.includes(keyword))) {
    console.log('已忽略无害的DOM操作错误（组件已销毁）')
    return // 完全忽略，不记录到控制台
  }

  // 其他错误正常处理
  console.error('Vue错误:', error)
  console.error('错误信息:', info)
}

// 挂载依赖
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.mount('#app')