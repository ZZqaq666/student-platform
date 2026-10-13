import { ref } from 'vue'
import { apiService } from '@/api/api'
import { useAuthStore } from '@/stores/auth'

// 编辑书籍模态数据域
export function useEditBook({ showToast, books, selectedBook }) {
  const authStore = useAuthStore()

  // book 是全局共享目录（后端无归属列），服务端只放行 ADMIN；
  // 这里做同样的门禁，避免普通用户点了才发现没权限
  const canEditCatalog = () => authStore.role === 'ADMIN'

  // 编辑书籍相关
  const showEditBookModal = ref(false)
  const editBookForm = ref({
    title: '',
    author: '',
    description: '',
    major: '',
    semester: '',
    courseType: ''
  })
  const editLoading = ref(false)

  // 打开编辑书籍模态框
  const openEditBookModal = (book) => {
    if (!canEditCatalog()) {
      showToast('仅管理员可编辑书籍目录', false)
      return
    }
    selectedBook.value = book
    // 填充编辑表单
    editBookForm.value = {
      title: book.title || '',
      author: book.author || '',
      description: book.description || '',
      major: book.major || '',
      semester: book.semester || '',
      courseType: book.courseType || ''
    }
    showEditBookModal.value = true
  }

  // 保存书籍更改
  const saveBookChanges = async () => {
    if (!selectedBook.value) return
    if (!canEditCatalog()) {
      showToast('仅管理员可编辑书籍目录', false)
      return
    }

    editLoading.value = true
    try {
      const response = await apiService.bookshelf.updateBook(
        selectedBook.value.id,
        { ...editBookForm.value }
      )
      if (response.code !== 200) {
        showToast(response.message || '更新书籍失败', false)
        return
      }

      // 更新本地书籍数据
      const bookIndex = books.value.findIndex(b => b.id === selectedBook.value.id)
      if (bookIndex !== -1) {
        books.value[bookIndex] = {
          ...books.value[bookIndex],
          ...editBookForm.value
        }
      }

      showToast('书籍信息更新成功')

      // 关闭编辑模态框
      showEditBookModal.value = false
    } catch (error) {
      console.error('更新书籍失败:', error)
      showToast(error.errorMessage || '更新书籍失败', false)
    } finally {
      editLoading.value = false
    }
  }

  return { showEditBookModal, editBookForm, editLoading, openEditBookModal, saveBookChanges }
}
