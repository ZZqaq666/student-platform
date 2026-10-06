import { ref } from 'vue'

// 编辑书籍模态数据域
export function useEditBook({ showToast, books, selectedBook }) {
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

    editLoading.value = true
    try {
      // 这里应该调用API更新书籍信息
      // 由于API可能还没有实现，这里先模拟成功
      showToast('书籍信息更新成功')

      // 更新本地书籍数据
      const bookIndex = books.value.findIndex(b => b.id === selectedBook.value.id)
      if (bookIndex !== -1) {
        books.value[bookIndex] = {
          ...books.value[bookIndex],
          ...editBookForm.value
        }
      }

      // 关闭编辑模态框
      showEditBookModal.value = false
    } catch (error) {
      console.error('更新书籍失败:', error)
      showToast('更新书籍失败', false)
    } finally {
      editLoading.value = false
    }
  }

  return { showEditBookModal, editBookForm, editLoading, openEditBookModal, saveBookChanges }
}
