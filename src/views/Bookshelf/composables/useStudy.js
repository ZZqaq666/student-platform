import { ref } from 'vue'
import { apiService } from '@/api/api'

// 学习模态数据域：目录树操作 / 阅读区 / 文本选择提问 / 学习进度
// 注意：loadNotes 通过依赖注入传入（笔记域在编排层晚于本域创建，用闭包延迟解引用避免循环依赖）
export function useStudy({ router, showToast, books, chapters, loadNotes }) {
  // 学习模态框
  const showStudyModal = ref(false)
  const studyBook = ref(null)
  const currentChapter = ref(null)
  const selectedChapter = ref(null)
  const currentContent = ref('')

  // 目录搜索
  const searchKeyword = ref('')
  const searchResults = ref([])
  const showSearchResults = ref(false)

  // 学习记录相关
  const studyStartTime = ref(null)
  const studyEndTime = ref(null)

  // 章节内容加载状态
  const contentLoading = ref(false)

  // 文本选择弹出菜单
  const showSelectionMenu = ref(false)
  const selectionMenuTop = ref(0)
  const selectionMenuLeft = ref(0)
  const selectedText = ref('')

  // 提问侧边栏
  const showQuestionSidebar = ref(false)
  const userQuestion = ref('')
  const showAIAnswer = ref(false)
  const aiAnswer = ref('')

  // 选择章节
  const selectChapter = (chapter) => {
    currentChapter.value = chapter
    selectedChapter.value = chapter
    currentContent.value = chapter.content || ''
    showSearchResults.value = false
  }

  // 搜索章节
  const searchChapters = async () => {
    if (!searchKeyword.value.trim()) {
      showSearchResults.value = false
      return
    }

    try {
      const response = await apiService.bookshelf.searchChapters(studyBook.value.id, searchKeyword.value)
      searchResults.value = response.data
      showSearchResults.value = true
    } catch (error) {
      console.error('搜索章节失败:', error)
    }
  }

  // 清空搜索
  const clearSearch = () => {
    searchKeyword.value = ''
    showSearchResults.value = false
    searchResults.value = []
  }

  // 关闭学习模态框
  const closeStudyModal = async () => {
    // 计算学习时间
    if (studyStartTime.value) {
      studyEndTime.value = new Date()
      const studyDuration = Math.floor((studyEndTime.value - studyStartTime.value) / 1000 / 60) // 转换为分钟

      if (studyDuration > 0 && studyBook.value) {
        try {
          // 更新学习进度
          const response = await apiService.bookshelf.updateProgress(studyBook.value.id, {
            progress: studyBook.value.progress || 0,
            lastReadPage: studyBook.value.lastReadPage || 0
          })

          if (response.code === 200) {
            // 保存学习进度到学习历史
            await apiService.learning.saveProgress({
              bookId: studyBook.value.id,
              progress: studyBook.value.progress || 0,
              studyTimeMinutes: studyDuration,
              pagesRead: studyBook.value.lastReadPage || 0
            })

            showToast(`学习时间：${studyDuration} 分钟，学习进度已更新`)
            // 更新本地书籍数据
            const bookIndex = books.value.findIndex(b => b.id === studyBook.value.id)
            if (bookIndex !== -1) {
              books.value[bookIndex].progress = response.data.progress || books.value[bookIndex].progress
            }
          }
        } catch (error) {
          console.error('更新学习进度失败:', error)
          showToast('更新学习进度失败', false)
        }
      }
    }

    // 重置学习记录
    studyStartTime.value = null
    studyEndTime.value = null
    showStudyModal.value = false
  }

  // 开始学习
  const startStudy = async (book) => {
    studyBook.value = book
    await chapters.fetchChapterTree(book.id)
    // 加载笔记数据
    await loadNotes()
    // 记录学习开始时间
    studyStartTime.value = new Date()

    // 从本地存储读取学习进度
    const localProgress = localStorage.getItem(`study_progress_${book.id}`)
    let lastChapterId = book.lastReadChapterId

    // 如果本地存储有进度，优先使用本地存储的数据
    if (localProgress) {
      try {
        const parsedProgress = JSON.parse(localProgress)
        lastChapterId = parsedProgress.chapterId
      } catch (error) {
        console.error('解析本地存储数据失败:', error)
      }
    }

    // 根据是否有学习记录决定显示的章节
    if (lastChapterId && chapters.chapterTree.value.length > 0) {
      // 找到上次学习的章节
      const lastChapter = chapters.findChapterById(lastChapterId, chapters.chapterTree.value)
      if (lastChapter) {
        selectedChapter.value = lastChapter
        currentContent.value = lastChapter.content || ''
      } else {
        // 如果找不到章节，默认显示第一章
        selectedChapter.value = chapters.chapterTree.value[0]
        currentContent.value = chapters.chapterTree.value[0].content || ''
      }
    } else if (chapters.chapterTree.value.length > 0) {
      // 没有学习记录，显示第一章
      selectedChapter.value = chapters.chapterTree.value[0]
      currentContent.value = chapters.chapterTree.value[0].content || ''
    }

    showStudyModal.value = true
  }

  // 结束学习并保存进度（页面卸载时由编排层调用）
  const endStudy = async () => {
    if (!studyBook.value || !studyStartTime.value) return

    try {
      // 计算学习时间（分钟）
      const endTime = new Date()
      const studyTimeMinutes = Math.round((endTime - studyStartTime.value) / (1000 * 60))

      // 确保学习时间至少为1分钟
      if (studyTimeMinutes < 1) return

      // 构建学习进度数据
      const progressData = {
        bookId: studyBook.value.id,
        bookTitle: studyBook.value.title || '未知书籍',
        bookAuthor: studyBook.value.author || '未知作者',
        progress: studyBook.value.progress || 0,
        studyTimeMinutes: studyTimeMinutes,
        pagesRead: studyBook.value.lastReadPage || 0,
        date: new Date().toISOString().split('T')[0]
      }

      // 构建书籍进度更新数据
      const bookProgressData = {
        progress: studyBook.value.progress || 0,
        lastReadPage: studyBook.value.lastReadPage || 0,
        lastReadChapterId: selectedChapter.value?.id || null
      }

      try {
        // 保存学习进度
        await apiService.learning.saveProgress(progressData)
        console.log('学习进度保存成功:', progressData)
      } catch (error) {
        console.error('保存学习进度失败，使用本地存储作为回退:', error)
        // 使用本地存储作为回退
        const learningHistory = JSON.parse(localStorage.getItem('learningHistory') || '[]')
        learningHistory.push({
          ...progressData,
          id: Date.now(),
          userId: 4 // 假设用户ID为4
        })
        localStorage.setItem('learningHistory', JSON.stringify(learningHistory))
      }

      try {
        // 更新书籍进度，保存最后阅读章节
        await apiService.bookshelf.updateProgress(studyBook.value.id, bookProgressData)
        console.log('书籍进度更新成功:', bookProgressData)
      } catch (error) {
        console.error('更新书籍进度失败，使用本地存储作为回退:', error)
        // 使用本地存储作为回退
        const bookProgresses = JSON.parse(localStorage.getItem('bookProgresses') || '{}')
        bookProgresses[studyBook.value.id] = bookProgressData
        localStorage.setItem('bookProgresses', JSON.stringify(bookProgresses))
      }
    } catch (error) {
      console.error('结束学习时发生错误:', error)
    } finally {
      // 重置学习状态
      studyStartTime.value = null
      showStudyModal.value = false
    }
  }

  // 处理文本选择
  const handleTextSelection = (event) => {
    const selection = window.getSelection()
    const text = selection.toString().trim()

    if (text) {
      selectedText.value = text

      // 计算弹出菜单的位置
      const range = selection.getRangeAt(0)
      const rect = range.getBoundingClientRect()
      const modalRect = document.querySelector('.study-modal').getBoundingClientRect()

      selectionMenuTop.value = rect.bottom - modalRect.top + 10
      selectionMenuLeft.value = rect.left - modalRect.left + rect.width / 2 - 50

      showSelectionMenu.value = true
    } else {
      showSelectionMenu.value = false
    }
  }

  // 提问函数
  const askQuestion = () => {
    if (selectedText.value) {
      // 关闭弹出菜单
      showSelectionMenu.value = false

      // 显示提问侧边栏
      showQuestionSidebar.value = true
      showAIAnswer.value = false
      userQuestion.value = ''
    }
  }

  // 提交问题
  const submitQuestion = async () => {
    if (userQuestion.value) {
      try {
        const response = await apiService.qa.askQuestion({
          question: userQuestion.value,
          selectedText: selectedText.value,
          bookId: studyBook.value?.id
        })
        aiAnswer.value = response.data
        showAIAnswer.value = true
      } catch (error) {
        console.error('提交问题失败:', error)
        showToast('提交问题失败', false)
      }
    }
  }

  // 跳转到个人问答中心
  const goToQACenter = () => {
    // 保存学习模态框的状态到 localStorage
    const studyState = {
      showStudyModal: true,
      studyBook: studyBook.value,
      showQuestionSidebar: showQuestionSidebar.value,
      userQuestion: userQuestion.value,
      showAIAnswer: showAIAnswer.value
    }
    localStorage.setItem('studyModalState', JSON.stringify(studyState))

    // 关闭提问侧边栏
    showQuestionSidebar.value = false

    // 跳转到个人问答中心，并传递问题、选中的文本和来源信息
    router.push({
      path: '/qa',
      query: {
        question: userQuestion.value,
        selectedText: selectedText.value,
        bookId: studyBook.value?.id,
        from: 'study' // 标记来源为学习页面
      }
    })
  }

  // 从 localStorage 恢复学习状态（跳转 QA 后返回时）
  const restoreStudyStateFromStorage = () => {
    const savedStudyState = localStorage.getItem('studyModalState')
    if (savedStudyState) {
      const state = JSON.parse(savedStudyState)
      if (state.showStudyModal && state.studyBook) {
        studyBook.value = state.studyBook
        showStudyModal.value = true
        showQuestionSidebar.value = state.showQuestionSidebar || false
        userQuestion.value = state.userQuestion || ''
        showAIAnswer.value = state.showAIAnswer || false
        // 清除保存的状态，避免重复打开
        localStorage.removeItem('studyModalState')
      }
    }
  }

  return {
    showStudyModal, studyBook, currentChapter, selectedChapter, currentContent,
    searchKeyword, searchResults, showSearchResults, searchChapters, clearSearch,
    studyStartTime, studyEndTime, contentLoading,
    showSelectionMenu, selectionMenuTop, selectionMenuLeft, selectedText,
    showQuestionSidebar, userQuestion, showAIAnswer, aiAnswer,
    selectChapter, closeStudyModal, startStudy, endStudy,
    handleTextSelection, askQuestion, submitQuestion, goToQACenter,
    restoreStudyStateFromStorage
  }
}
