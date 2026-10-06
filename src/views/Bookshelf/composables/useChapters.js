import { ref } from 'vue'
import { apiService } from '@/api/api'

// 章节树数据域：详情弹窗完整目录与学习模态共用
export function useChapters() {
  // 目录标题数据 - 从API获取
  const chapterTree = ref([])
  const expandedChapters = ref(new Set())

  // 章节加载状态
  const chapterLoading = ref(false)
  const chapterError = ref('')

  // 获取书籍章节（详情弹窗用：不展开、带加载/错误状态）
  const fetchChapters = async (bookId) => {
    chapterLoading.value = true
    chapterError.value = ''
    try {
      const response = await apiService.bookshelf.getBookChapterTree(bookId)
      console.log('Chapters API response:', response)
      if (response && response.data) {
        chapterTree.value = response.data || []
        console.log('Chapters data:', chapterTree.value)
      } else if (response) {
        // 如果响应不是标准Result格式，直接使用
        chapterTree.value = response || []
        console.log('Chapters data (non-standard format):', chapterTree.value)
      }
    } catch (error) {
      console.error('获取章节失败:', error)
      chapterError.value = error.errorMessage || '获取章节失败'
    } finally {
      chapterLoading.value = false
    }
  }

  // 获取章节树数据（学习模态用：默认展开全部，失败回退 mock）
  const fetchChapterTree = async (bookId) => {
    try {
      const response = await apiService.bookshelf.getBookChapterTree(bookId)
      chapterTree.value = response.data
      // 默认展开所有章节
      expandAllChapters(chapterTree.value)
    } catch (error) {
      console.error('获取章节树失败，使用mock数据:', error)
      // 使用mock数据作为回退
      chapterTree.value = [
        {
          id: 'chapter_1',
          title: '第一章 高等数学基础',
          children: [
            {
              id: 'section_1_1',
              title: '1.1 函数与极限',
              children: [
                { id: 'subsection_1_1_1', title: '1.1.1 函数的概念' },
                { id: 'subsection_1_1_2', title: '1.1.2 极限的定义' },
                { id: 'subsection_1_1_3', title: '1.1.3 极限的计算' }
              ]
            },
            {
              id: 'section_1_2',
              title: '1.2 导数与微分',
              children: [
                { id: 'subsection_1_2_1', title: '1.2.1 导数的定义' },
                { id: 'subsection_1_2_2', title: '1.2.2 导数的计算' },
                { id: 'subsection_1_2_3', title: '1.2.3 微分的概念' }
              ]
            }
          ]
        },
        {
          id: 'chapter_2',
          title: '第二章 一元函数积分学',
          children: [
            {
              id: 'section_2_1',
              title: '2.1 不定积分',
              children: [
                { id: 'subsection_2_1_1', title: '2.1.1 不定积分的概念' },
                { id: 'subsection_2_1_2', title: '2.1.2 基本积分公式' },
                { id: 'subsection_2_1_3', title: '2.1.3 换元积分法' }
              ]
            },
            {
              id: 'section_2_2',
              title: '2.2 定积分',
              children: [
                { id: 'subsection_2_2_1', title: '2.2.1 定积分的概念' },
                { id: 'subsection_2_2_2', title: '2.2.2 定积分的性质' },
                { id: 'subsection_2_2_3', title: '2.2.3 微积分基本定理' }
              ]
            }
          ]
        }
      ]
      // 默认展开所有章节
      expandAllChapters(chapterTree.value)
    }
  }

  // 展开所有章节
  const expandAllChapters = (chapters) => {
    if (!chapters) return
    chapters.forEach(chapter => {
      expandedChapters.value.add(chapter.id)
      if (chapter.children && chapter.children.length > 0) {
        expandAllChapters(chapter.children)
      }
    })
  }

  // 切换章节展开/折叠
  const toggleChapter = (chapterId) => {
    if (expandedChapters.value.has(chapterId)) {
      expandedChapters.value.delete(chapterId)
    } else {
      expandedChapters.value.add(chapterId)
    }
  }

  // 切换所有章节展开/折叠
  const toggleAllChapters = () => {
    const allExpanded = chapterTree.value.every(chapter => expandedChapters.value.has(chapter.id))
    expandedChapters.value.clear()
    if (!allExpanded) {
      expandAllChapters(chapterTree.value)
    }
  }

  // 递归查找章节
  const findChapterById = (id, chapters) => {
    for (const chapter of chapters) {
      if (chapter.id === id) {
        return chapter
      }
      if (chapter.children && chapter.children.length > 0) {
        const found = findChapterById(id, chapter.children)
        if (found) {
          return found
        }
      }
    }
    return null
  }

  // 获取章节标题
  const getChapterTitleById = (chapterId) => {
    if (!chapterId || !chapterTree.value) return ''

    const findChapter = (chapters, id) => {
      for (const chapter of chapters) {
        if (chapter.id === id) {
          return chapter.title
        }
        if (chapter.children && chapter.children.length > 0) {
          const found = findChapter(chapter.children, id)
          if (found) return found
        }
      }
      return ''
    }

    return findChapter(chapterTree.value, chapterId)
  }

  return {
    chapterTree, expandedChapters, chapterLoading, chapterError,
    fetchChapters, fetchChapterTree,
    toggleChapter, toggleAllChapters, findChapterById, getChapterTitleById
  }
}
