import { ref, computed, nextTick } from 'vue'
import { apiService } from '@/api/api'
import { MOCK_FALLBACK_ENABLED } from '@/utils/mockFallback'
import { getNotePreview, renderMarkdownPreview } from '../utils/markdown'
import { formatNoteDate } from '../utils/formatters'

// 笔记数据域：列表/编辑/预览/导出
// 依赖学习域的 studyBook/currentChapter/selectedText（编排层传入）
export function useNotes({ showToast, study, chapters }) {
  // 笔记相关
  const showNoteModal = ref(false)
  const notes = ref([])
  const currentNote = ref(null)
  const noteContent = ref('')
  const noteTitle = ref('')
  const noteLoading = ref(false)
  const noteSearchQuery = ref('')
  const selectedNoteChapterId = ref('')
  const showNotePreview = ref(false)
  const showExportOptions = ref(false)
  const noteTextarea = ref(null)

  // 过滤后的笔记列表
  const filteredNotes = computed(() => {
    if (!noteSearchQuery.value.trim()) {
      return notes.value
    }
    const query = noteSearchQuery.value.toLowerCase()
    return notes.value.filter(note =>
      note.title.toLowerCase().includes(query) ||
      note.content.toLowerCase().includes(query)
    )
  })

  // 加载笔记
  const loadNotes = async () => {
    if (!study.studyBook.value) return

    noteLoading.value = true
    try {
      const response = await apiService.note.getNotesByBook(study.studyBook.value.id)
      if (response.code === 200) {
        notes.value = response.data || []
        // 为笔记添加章节标题
        notes.value.forEach(note => {
          if (note.chapterId) {
            note.chapterTitle = chapters.getChapterTitleById(note.chapterId)
          }
        })
      }
    } catch (error) {
      if (!MOCK_FALLBACK_ENABLED) {
        console.error('加载笔记失败:', error)
        notes.value = []
        showToast('笔记加载失败，请稍后重试', false)
        return
      }
      console.error('加载笔记失败，使用mock数据:', error)
      // 仅开发演示允许回退到 mock 笔记
      notes.value = [
        {
          id: 'note_1',
          bookId: study.studyBook.value.id,
          title: '导数的定义',
          content: '导数是函数在某一点的变化率，定义为极限值：f\'(x) = lim(h→0) [f(x+h) - f(x)]/h',
          chapterId: 'section_1_2',
          chapterTitle: '1.2 导数与微分',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        {
          id: 'note_2',
          bookId: study.studyBook.value.id,
          title: '不定积分的性质',
          content: '1. ∫[f(x) + g(x)]dx = ∫f(x)dx + ∫g(x)dx\n2. ∫kf(x)dx = k∫f(x)dx (k为常数)\n3. d/dx[∫f(x)dx] = f(x)',
          chapterId: 'section_2_1',
          chapterTitle: '2.1 不定积分',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ]
    } finally {
      noteLoading.value = false
    }
  }

  // 删除确认弹窗状态（页面内二次确认，替代原生 confirm）
  const showDeleteNoteConfirm = ref(false)
  const pendingDeleteNote = ref(null)

  // 将左侧列表中当前选中的笔记滚动到可视区域
  const focusActiveNoteItem = () => {
    const activeItem = document.querySelector('.note-item.active')
    if (activeItem && typeof activeItem.scrollIntoView === 'function') {
      activeItem.scrollIntoView({ block: 'nearest' })
    }
  }

  // 保存成功后联动：刷新列表 → 选中该笔记 → 滚动定位（不关闭模态）
  const applySavedNote = async (savedNote) => {
    await loadNotes()

    const savedId = String(savedNote?.id ?? '')
    const matched = notes.value.find(note => String(note.id) === savedId)
    const target = matched || savedNote

    if (target) {
      openNoteDetail(target)
      await nextTick()
      focusActiveNoteItem()
    }
  }

  // 创建笔记
  const createNote = async () => {
    if (!study.studyBook.value || !noteTitle.value.trim()) {
      showToast('请输入笔记标题', false)
      return
    }

    noteLoading.value = true
    try {
      const response = await apiService.note.createNote({
        bookId: study.studyBook.value.id,
        title: noteTitle.value,
        content: noteContent.value,
        chapterId: selectedNoteChapterId.value || study.currentChapter.value?.id
      })

      if (response.code === 200 && response.data) {
        showToast('笔记创建成功')
        // 行为变更：保存后不再关闭模态，而是刷新列表并选中刚创建的笔记
        await applySavedNote(response.data)
      } else {
        showToast('创建笔记失败', false)
      }
    } catch (error) {
      console.error('创建笔记失败:', error)
      showToast('创建笔记失败', false)
    } finally {
      noteLoading.value = false
    }
  }

  // 更新笔记
  const updateNote = async () => {
    if (!currentNote.value || !noteTitle.value.trim()) {
      showToast('请输入笔记标题', false)
      return
    }

    noteLoading.value = true
    try {
      const response = await apiService.note.updateNote(currentNote.value.id, {
        title: noteTitle.value,
        content: noteContent.value,
        chapterId: selectedNoteChapterId.value || currentNote.value.chapterId
      })

      if (response.code === 200 && response.data) {
        showToast('笔记更新成功')
        // 行为变更：保存后不再关闭模态，而是刷新列表并保持选中该笔记
        await applySavedNote(response.data)
      } else {
        showToast('更新笔记失败', false)
      }
    } catch (error) {
      console.error('更新笔记失败:', error)
      showToast('更新笔记失败', false)
    } finally {
      noteLoading.value = false
    }
  }

  // 请求删除笔记：先弹出页面内确认框，确认后才真正删除
  const requestDeleteNote = (noteId) => {
    const target = notes.value.find(note => String(note.id) === String(noteId))
    pendingDeleteNote.value = target || { id: noteId, title: noteTitle.value || '未命名笔记' }
    showDeleteNoteConfirm.value = true
  }

  // 取消删除（仅关闭确认层，不影响笔记模态）
  const cancelDeleteNote = () => {
    showDeleteNoteConfirm.value = false
    pendingDeleteNote.value = null
  }

  // 确认删除
  const confirmDeleteNote = async () => {
    const noteId = pendingDeleteNote.value?.id
    if (!noteId) {
      cancelDeleteNote()
      return
    }
    await deleteNote(noteId)
    cancelDeleteNote()
  }

  // 删除笔记
  const deleteNote = async (noteId) => {
    noteLoading.value = true
    try {
      const response = await apiService.note.deleteNote(noteId)
      if (response.code === 200) {
        showToast('笔记删除成功')
        await loadNotes()
        // 如果删除的是当前编辑的笔记，清空编辑区
        if (currentNote.value && currentNote.value.id === noteId) {
          createNewNote()
        }
      }
    } catch (error) {
      console.error('删除笔记失败:', error)
      showToast('删除笔记失败', false)
    } finally {
      noteLoading.value = false
    }
  }

  // 打开笔记详情
  const openNoteDetail = (note) => {
    currentNote.value = note
    noteTitle.value = note.title
    noteContent.value = note.content
    selectedNoteChapterId.value = note.chapterId || ''
    showNoteModal.value = true
  }

  // 新建笔记
  const createNewNote = () => {
    currentNote.value = null
    noteTitle.value = ''
    noteContent.value = ''
    selectedNoteChapterId.value = study.currentChapter.value?.id || ''
    showNotePreview.value = false
  }

  // 关闭笔记模态框
  const closeNoteModal = () => {
    showNoteModal.value = false
    // 关闭笔记模态时一并收起删除确认层，避免残留悬浮层
    showDeleteNoteConfirm.value = false
    pendingDeleteNote.value = null
    currentNote.value = null
    noteTitle.value = ''
    noteContent.value = ''
    selectedNoteChapterId.value = ''
    noteSearchQuery.value = ''
    showNotePreview.value = false
    showExportOptions.value = false
  }

  // 搜索笔记
  const searchNotes = () => {
    // 搜索逻辑由 filteredNotes computed 属性处理
    console.log('搜索笔记:', noteSearchQuery.value)
  }

  // 清空笔记搜索
  const clearNoteSearch = () => {
    noteSearchQuery.value = ''
  }

  // 插入 Markdown 标记
  const insertMarkdown = (prefix, suffix) => {
    const textarea = noteTextarea.value
    if (!textarea) return

    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = noteContent.value
    const selected = text.substring(start, end)

    const newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
    noteContent.value = newText

    // 恢复焦点并设置光标位置
    nextTick(() => {
      textarea.focus()
      const newCursorPos = start + prefix.length + selected.length
      textarea.setSelectionRange(newCursorPos, newCursorPos)
    })
  }

  // 插入选中的文本
  const insertSelectedText = () => {
    if (study.selectedText.value) {
      insertMarkdown('> ' + study.selectedText.value + '\n\n', '')
    }
  }

  // 导出笔记
  const exportNotes = (format) => {
    if (notes.value.length === 0) {
      showToast('没有可导出的笔记', false)
      showExportOptions.value = false
      return
    }

    const bookTitle = study.studyBook.value?.title || '笔记'
    let content = ''
    let filename = ''
    let mimeType = ''

    if (format === 'markdown') {
      // 导出为 Markdown
      content = `# ${bookTitle} - 学习笔记\n\n`
      content += `导出时间：${new Date().toLocaleString('zh-CN')}\n\n`
      content += `---\n\n`

      notes.value.forEach((note, index) => {
        content += `## ${index + 1}. ${note.title}\n\n`
        if (note.chapterTitle) {
          content += `**关联章节：** ${note.chapterTitle}\n\n`
        }
        content += `${note.content}\n\n`
        content += `*创建于：${new Date(note.createdAt).toLocaleString('zh-CN')}*\n\n`
        content += `---\n\n`
      })

      filename = `${bookTitle}_笔记_${new Date().toISOString().split('T')[0]}.md`
      mimeType = 'text/markdown'
    } else {
      // 导出为纯文本
      content = `${bookTitle} - 学习笔记\n\n`
      content += `导出时间：${new Date().toLocaleString('zh-CN')}\n\n`
      content += `${'='.repeat(50)}\n\n`

      notes.value.forEach((note, index) => {
        content += `${index + 1}. ${note.title}\n\n`
        if (note.chapterTitle) {
          content += `关联章节：${note.chapterTitle}\n\n`
        }
        content += `${note.content}\n\n`
        content += `创建于：${new Date(note.createdAt).toLocaleString('zh-CN')}\n\n`
        content += `${'-'.repeat(50)}\n\n`
      })

      filename = `${bookTitle}_笔记_${new Date().toISOString().split('T')[0]}.txt`
      mimeType = 'text/plain'
    }

    // 创建下载链接
    const blob = new Blob([content], { type: mimeType })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    showToast(`笔记已导出为 ${format === 'markdown' ? 'Markdown' : '文本'} 格式`)
    showExportOptions.value = false
  }

  return {
    showNoteModal, notes, currentNote, noteContent, noteTitle, noteLoading,
    noteSearchQuery, selectedNoteChapterId, showNotePreview, showExportOptions, noteTextarea,
    filteredNotes,
    showDeleteNoteConfirm, pendingDeleteNote,
    loadNotes, createNote, updateNote,
    requestDeleteNote, confirmDeleteNote, cancelDeleteNote, deleteNote,
    openNoteDetail, createNewNote, closeNoteModal,
    searchNotes, clearNoteSearch,
    insertMarkdown, insertSelectedText, exportNotes,
    getNotePreview, renderMarkdownPreview, formatNoteDate
  }
}
