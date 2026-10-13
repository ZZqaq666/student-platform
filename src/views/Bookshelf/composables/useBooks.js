import { ref, computed, onUnmounted } from 'vue'
import { apiService } from '@/api/api'

// 书籍数据 + 搜索筛选 + 删除：书架核心数据域
export function useBooks({ showToast }) {
  // 书籍数据
  const books = ref([])

  // 搜索查询
  const searchQuery = ref('')

  // 专业筛选
  const majorFilter = ref('')
  const showMajorDropdown = ref(false)

  // 学期筛选
  const semesterFilter = ref('')
  const showSemesterDropdown = ref(false)

  // 课程类型筛选
  const courseTypeFilter = ref('')
  const showCourseTypeDropdown = ref(false)

  // 专业选项
  const majorOptions = ref([
    { value: '', label: '全部专业' },
    { value: '计算机科学与技术', label: '计算机科学与技术' },
    { value: '数学', label: '数学' },
    { value: '物理', label: '物理' },
    { value: '经济学', label: '经济学' },
    { value: '管理学', label: '管理学' },
    { value: '通识教育', label: '通识教育' }
  ])

  // 学期选项
  const semesterOptions = ref([
    { value: '', label: '全部学期' },
    { value: 'Year 1', label: '大一' },
    { value: 'Year 2', label: '大二' },
    { value: 'Year 3', label: '大三' },
    { value: 'Year 4', label: '大四' }
  ])

  // 课程类型选项
  const courseTypeOptions = ref([
    { value: '', label: '全部类型' },
    { value: 'CORE', label: '专业核心课' },
    { value: 'FOUNDATION', label: '专业基础课' },
    { value: 'ELECTIVE', label: '选修课' },
    { value: 'GENERAL', label: '通识课' }
  ])

  // 用于跟踪当前正在进行的书籍搜索请求
  let booksSearchAbortController = null
  const abortPendingSearch = () => {
    if (booksSearchAbortController) {
      booksSearchAbortController.abort()
    }
  }
  onUnmounted(abortPendingSearch)

  // 获取书籍列表
  const fetchBooks = async () => {
    // 取消之前的请求
    abortPendingSearch()

    // 创建新的AbortController
    booksSearchAbortController = new AbortController()

    try {
      const response = await apiService.bookshelf.getAllBooks(booksSearchAbortController.signal)
      // 检查响应格式，确保是后端返回的Result对象
      if (response && response.code === 200 && response.data) {
        books.value = response.data
        console.log('成功从后端获取书籍数据:', books.value.length, '本书籍')
      } else {
        console.error('获取书籍响应格式错误:', response)
        showToast('获取书籍数据格式错误，请稍后重试', false)
      }
    } catch (error) {
      // 忽略取消错误
      if (error.name !== 'AbortError') {
        console.error('获取书籍失败:', error)
        let errorMessage = '获取书籍失败，请稍后重试'
        if (error.errorMessage) {
          errorMessage = error.errorMessage
        }
        showToast(errorMessage, false)
      }
    } finally {
      // 清理AbortController
      if (booksSearchAbortController) {
        booksSearchAbortController = null
      }
    }
  }

  // 搜索书籍
  const searchBooks = async () => {
    // 取消之前的请求
    abortPendingSearch()

    // 创建新的AbortController
    booksSearchAbortController = new AbortController()

    try {
      const response = await apiService.bookshelf.searchBooks(searchQuery.value, majorFilter.value, semesterFilter.value, courseTypeFilter.value, booksSearchAbortController.signal)
      // 检查响应格式，确保是后端返回的Result对象
      if (response && response.code === 200 && response.data) {
        books.value = response.data
        console.log('成功从后端搜索书籍数据:', books.value.length, '本书籍')
      } else {
        console.error('搜索书籍响应格式错误:', response)
        showToast('搜索书籍数据格式错误，请稍后重试', false)
      }
    } catch (error) {
      // 忽略取消错误
      if (error.name !== 'AbortError') {
        console.error('搜索教材失败:', error)
        let errorMessage = '搜索书籍失败，请稍后重试'
        if (error.errorMessage) {
          errorMessage = error.errorMessage
        }
        showToast(errorMessage, false)
      }
    } finally {
      // 清理AbortController
      if (booksSearchAbortController) {
        booksSearchAbortController = null
      }
    }
  }

  // 选择专业
  const selectMajor = (majorValue) => {
    majorFilter.value = majorValue
    showMajorDropdown.value = false
    searchBooks()
  }

  // 选择学期
  const selectSemester = (semesterValue) => {
    semesterFilter.value = semesterValue
    showSemesterDropdown.value = false
    searchBooks()
  }

  // 选择课程类型
  const selectCourseType = (courseTypeValue) => {
    courseTypeFilter.value = courseTypeValue
    showCourseTypeDropdown.value = false
    searchBooks()
  }

  // 切换专业下拉菜单
  const toggleMajorDropdown = () => {
    showMajorDropdown.value = !showMajorDropdown.value
  }

  // 切换学期下拉菜单
  const toggleSemesterDropdown = () => {
    showSemesterDropdown.value = !showSemesterDropdown.value
  }

  // 切换课程类型下拉菜单
  const toggleCourseTypeDropdown = () => {
    showCourseTypeDropdown.value = !showCourseTypeDropdown.value
  }

  // 获取选中的专业名称
  const getSelectedMajorName = () => {
    const major = majorOptions.value.find(m => m.value === majorFilter.value)
    return major ? major.label : '全部专业'
  }

  // 获取选中的学期名称
  const getSelectedSemesterName = () => {
    const semester = semesterOptions.value.find(s => s.value === semesterFilter.value)
    return semester ? semester.label : '全部学期'
  }

  // 获取选中的课程类型名称
  const getSelectedCourseTypeName = () => {
    const courseType = courseTypeOptions.value.find(c => c.value === courseTypeFilter.value)
    return courseType ? courseType.label : '全部类型'
  }

  // 筛选后的书籍
  const filteredBooks = computed(() => {
    let result = books.value

    // 按专业筛选
    if (majorFilter.value) {
      result = result.filter(book => book.major === majorFilter.value)
    }

    // 按学期筛选
    if (semesterFilter.value) {
      result = result.filter(book => book.semester === semesterFilter.value)
    }

    // 按课程类型筛选
    if (courseTypeFilter.value) {
      result = result.filter(book => book.courseType === courseTypeFilter.value)
    }

    // 按搜索筛选
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter(book =>
        book.title.toLowerCase().includes(query) ||
        book.author.toLowerCase().includes(query) ||
        book.major?.toLowerCase().includes(query) ||
        book.subject?.toLowerCase().includes(query)
      )
    }

    // 按最近点击时间排序（降序）
    result.sort((a, b) => b.lastClicked - a.lastClicked)

    return result
  })

  // 更新最近点击时间
  const updateLastClicked = (book) => {
    book.lastClicked = Date.now()
  }

  // 确认删除对话框
  const showDeleteConfirm = ref(false)
  let bookToDelete = null

  // 删除书籍
  const deleteBook = (bookId) => {
    bookToDelete = bookId
    showDeleteConfirm.value = true
  }

  // 确认删除
  const confirmDelete = async () => {
    if (bookToDelete) {
      try {
        const response = await apiService.bookshelf.deleteBook(bookToDelete)
        if (response.code === 200) {
          showToast('已从书架移除')
          await fetchBooks()
        }
      } catch (error) {
        console.error('删除书籍失败:', error)
        showToast('删除书籍失败', false)
      } finally {
        showDeleteConfirm.value = false
        bookToDelete = null
      }
    }
  }

  return {
    books, searchQuery,
    majorFilter, showMajorDropdown, semesterFilter, showSemesterDropdown, courseTypeFilter, showCourseTypeDropdown,
    majorOptions, semesterOptions, courseTypeOptions,
    fetchBooks, searchBooks,
    selectMajor, selectSemester, selectCourseType,
    toggleMajorDropdown, toggleSemesterDropdown, toggleCourseTypeDropdown,
    getSelectedMajorName, getSelectedSemesterName, getSelectedCourseTypeName,
    filteredBooks, updateLastClicked,
    showDeleteConfirm, deleteBook, confirmDelete
  }
}
