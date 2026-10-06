import { ref, onUnmounted } from 'vue'

// 全局 Toast 提示管理
export function useToast() {
  // 组件挂载状态（Toast 定时器回调中用于防止卸载后写状态）
  const isMounted = ref(false)
  const markMounted = () => { isMounted.value = true }
  const markUnmounted = () => { isMounted.value = false }

  // 成功提示状态
  const showSuccessToast = ref(false)
  const successMessage = ref('')
  const toastType = ref('success')

  // 显示提示
  const showToast = (message, isSuccess = true) => {
    successMessage.value = message
    showSuccessToast.value = true
    toastType.value = isSuccess ? 'success' : 'error'
    setTimeout(() => {
      // 检查组件是否已卸载
      if (isMounted.value) {
        showSuccessToast.value = false
      }
    }, 3000)
  }

  onUnmounted(markUnmounted)

  return { isMounted, markMounted, showSuccessToast, successMessage, toastType, showToast }
}
