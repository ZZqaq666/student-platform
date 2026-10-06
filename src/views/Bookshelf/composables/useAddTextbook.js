import { ref, computed } from 'vue'
import { apiService } from '@/api/api'

// 添加教材模态数据域：分类/教材列表/多选/批量添加
export function useAddTextbook({ showToast, fetchBooks }) {
  // 模态框状态
  const showAddTextbookModal = ref(false)

  // 教材搜索查询
  const textbookSearchQuery = ref('')

  // 分类
  const categories = ref([])
  const selectedCategory = ref('')

  // 选中的书籍
  const selectedBooks = ref([])

  // 教材数据
  const textbooks = ref([])

  // 获取分类列表
  const fetchCategories = async () => {
    try {
      const response = await apiService.bookshelf.getAllBooks()
      // 从书籍数据中提取分类
      const uniqueCategories = [...new Set(response.data.map(book => book.category).filter(Boolean))]
      categories.value = uniqueCategories
      if (categories.value.length > 0) {
        selectedCategory.value = categories.value[0]
      }
    } catch (error) {
      console.error('获取分类失败:', error)
    }
  }

  // 获取教材列表
  const fetchTextbooks = async () => {
    try {
      const response = await apiService.bookshelf.getTextbooks({
        category: selectedCategory.value,
        keyword: textbookSearchQuery.value
      })
      textbooks.value = response.data
    } catch (error) {
      console.error('获取教材失败:', error)
    }
  }

  // 过滤后的教材
  const filteredTextbooks = computed(() => {
    let result = textbooks.value

    // 按搜索筛选
    if (textbookSearchQuery.value) {
      const query = textbookSearchQuery.value.toLowerCase()
      result = result.filter(book =>
        book.title.toLowerCase().includes(query)
      )
    }

    return result
  })

  // 添加按钮文本
  const addButtonText = computed(() => {
    const count = selectedBooks.value.length
    if (count === 0) {
      return '添加教材'
    } else if (count === 1) {
      return '添加'
    } else {
      return '批量添加'
    }
  })

  // 选择分类
  const selectCategory = async (category) => {
    selectedCategory.value = category
    await fetchTextbooks()
  }

  // 切换选中状态
  const toggleSelect = (index) => {
    const selectedIndex = selectedBooks.value.indexOf(index)
    if (selectedIndex > -1) {
      selectedBooks.value.splice(selectedIndex, 1)
    } else {
      selectedBooks.value.push(index)
    }
  }

  // 批量添加
  const batchAdd = async () => {
    if (selectedBooks.value.length === 0) {
      showToast('请选择要添加的教材')
      return
    }

    try {
      const booksToAdd = selectedBooks.value.map(index => textbooks.value[index])
      const userId = localStorage.getItem('userId')
      if (!userId) {
        showToast('用户未登录', false)
        return
      }
      const response = await apiService.bookshelf.addBooks(userId, { books: booksToAdd })

      if (response.code === 200) {
        showToast(`已成功添加 ${selectedBooks.value.length} 本教材到个人书架`)
        await fetchBooks()

        showAddTextbookModal.value = false
        selectedBooks.value = []
        textbookSearchQuery.value = ''
      }
    } catch (error) {
      console.error('添加教材失败:', error)
      showToast('添加教材失败', false)
    }
  }

  // 打开添加教材模态框
  const addTextbook = async () => {
    showAddTextbookModal.value = true
    await fetchCategories()
    await fetchTextbooks()
  }

  return {
    showAddTextbookModal, textbookSearchQuery,
    categories, selectedCategory, selectedBooks, textbooks,
    filteredTextbooks, addButtonText,
    selectCategory, toggleSelect, batchAdd, addTextbook
  }
}
