import { ref } from 'vue'

// 夜间模式状态管理（localStorage 持久化）
export function useDarkMode() {
  const isDarkMode = ref(false)

  // 切换夜间模式
  const toggleDarkMode = () => {
    isDarkMode.value = !isDarkMode.value
    localStorage.setItem('bookshelfDarkMode', isDarkMode.value)
  }

  // 从 localStorage 加载夜间模式设置
  const loadDarkModeSetting = () => {
    const savedMode = localStorage.getItem('bookshelfDarkMode')
    if (savedMode !== null) {
      isDarkMode.value = savedMode === 'true'
    }
  }

  return { isDarkMode, toggleDarkMode, loadDarkModeSetting }
}
