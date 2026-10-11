import { ref, computed } from 'vue'
import { apiService } from '@/api/api'

// 跨书笔记总览域：拉全量笔记 → 按书籍分组 → 点击后进入对应书籍的学习模态并打开该笔记
// 依赖：books（bookId 映射书名）、startStudy（打开学习模态）、openNoteDetail（选中笔记）
export function useAllNotes({ showToast, books, startStudy, openNoteDetail }) {
  // 总览弹窗状态
  const showMyNotesModal = ref(false)
  const allNotes = ref([])
  const allNotesLoading = ref(false)
  const allNotesSearchQuery = ref('')
  const openingNoteId = ref(null)

  // 按标题/内容过滤（与书内笔记搜索交互一致）
  const filteredAllNotes = computed(() => {
    const query = allNotesSearchQuery.value.trim().toLowerCase()
    if (!query) {
      return allNotes.value
    }
    return allNotes.value.filter(note =>
      (note.title || '').toLowerCase().includes(query) ||
      (note.content || '').toLowerCase().includes(query)
    )
  })

  // bookId → 书名映射，取不到时用「未知教材」
  const resolveBookTitle = (bookId) => {
    const target = books.value.find(book => String(book.id) === String(bookId))
    return target?.title || '未知教材'
  }

  // 按书籍分组（保持后端返回顺序内的首次出现顺序）
  const groupedAllNotes = computed(() => {
    const groups = []
    const indexMap = new Map()

    filteredAllNotes.value.forEach(note => {
      const key = String(note.bookId ?? 'unknown')
      if (!indexMap.has(key)) {
        indexMap.set(key, groups.length)
        groups.push({
          key,
          bookId: note.bookId ?? '',
          bookTitle: resolveBookTitle(note.bookId),
          notes: []
        })
      }
      groups[indexMap.get(key)].notes.push({
        ...note,
        bookTitle: groups[indexMap.get(key)].bookTitle
      })
    })

    return groups
  })

  // 加载全部笔记
  const loadAllNotes = async () => {
    allNotesLoading.value = true
    try {
      const response = await apiService.note.getAllNotes()
      if (response.code === 200) {
        allNotes.value = response.data || []
      } else {
        showToast('加载笔记失败', false)
      }
    } catch (error) {
      console.error('加载全部笔记失败:', error)
      showToast('加载笔记失败', false)
      allNotes.value = []
    } finally {
      allNotesLoading.value = false
    }
  }

  // 打开总览弹窗
  const openMyNotesModal = () => {
    showMyNotesModal.value = true
    loadAllNotes()
  }

  // 关闭总览弹窗
  const closeMyNotesModal = () => {
    showMyNotesModal.value = false
    allNotesSearchQuery.value = ''
  }

  // 点击某条笔记：打开所属书籍的学习模态并加载该笔记
  const openNoteFromOverview = async (note) => {
    if (!note) return

    const book = books.value.find(item => String(item.id) === String(note.bookId))
    if (!book) {
      showToast('未找到该笔记所属教材', false)
      return
    }

    closeMyNotesModal()
    openingNoteId.value = note.id
    try {
      // startStudy 内部会加载章节树与该书笔记，并打开学习模态
      await startStudy(book)
      // 学习模态就绪后选中该笔记（总览里的对象来自跨书接口，字段完整）
      openNoteDetail({ ...note, bookTitle: book.title })
    } catch (error) {
      console.error('打开笔记所在教材失败:', error)
      showToast('打开笔记失败', false)
    } finally {
      openingNoteId.value = null
    }
  }

  return {
    showMyNotesModal, allNotes, allNotesLoading, allNotesSearchQuery,
    filteredAllNotes, groupedAllNotes, openingNoteId,
    loadAllNotes, openMyNotesModal, closeMyNotesModal, openNoteFromOverview
  }
}
