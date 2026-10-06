<template>
  <div class="bookshelf-page" :class="{ 'dark-mode': isDarkMode }">
    <!-- 页面头部 -->
    <BookshelfHeader />

    <!-- 书籍筛选 -->
    <BookshelfFilters />

    <!-- 书籍列表 + 分页 -->
    <BookGrid />

    <!-- 添加教材模态框 -->
    <AddTextbookModal />
  </div>

  <!-- 成功提示 -->
  <ToastMessage />

  <!-- 确认删除对话框 -->
  <ConfirmDeleteModal />

  <!-- 书籍详情弹窗 -->
  <BookDetailModal />

  <!-- 学习模态框 -->
  <StudyModal />

  <!-- 笔记模态框 -->
  <NoteModal />

  <!-- 编辑书籍模态框 -->
  <EditBookModal />
</template>

<script setup>
// ============================================================
// Bookshelf 编排层：组装各功能域 composables，通过 provide 注入子组件
// 功能域拆分见 ./composables/，UI 组件见 ./components/
// ============================================================
import { provide, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

import { useDarkMode } from './composables/useDarkMode'
import { useToast } from './composables/useToast'
import { useBooks } from './composables/useBooks'
import { usePagination } from './composables/usePagination'
import { useChapters } from './composables/useChapters'
import { useBookDetail } from './composables/useBookDetail'
import { useStudy } from './composables/useStudy'
import { useNotes } from './composables/useNotes'
import { useAddTextbook } from './composables/useAddTextbook'
import { useEditBook } from './composables/useEditBook'

import BookshelfHeader from './components/BookshelfHeader.vue'
import BookshelfFilters from './components/BookshelfFilters.vue'
import BookGrid from './components/BookGrid.vue'
import AddTextbookModal from './components/AddTextbookModal.vue'
import ToastMessage from './components/ToastMessage.vue'
import ConfirmDeleteModal from './components/ConfirmDeleteModal.vue'
import BookDetailModal from './components/BookDetailModal.vue'
import StudyModal from './components/StudyModal.vue'
import NoteModal from './components/NoteModal.vue'
import EditBookModal from './components/EditBookModal.vue'

const router = useRouter()

// ---------- 功能域组装 ----------
const { isDarkMode, toggleDarkMode, loadDarkModeSetting } = useDarkMode()

const {
  isMounted, markMounted,
  showSuccessToast, successMessage, toastType, showToast
} = useToast()

const {
  books, searchQuery,
  majorFilter, showMajorDropdown, semesterFilter, showSemesterDropdown, courseTypeFilter, showCourseTypeDropdown,
  majorOptions, semesterOptions, courseTypeOptions,
  fetchBooks, searchBooks,
  selectMajor, selectSemester, selectCourseType,
  toggleMajorDropdown, toggleSemesterDropdown, toggleCourseTypeDropdown,
  getSelectedMajorName, getSelectedSemesterName, getSelectedCourseTypeName,
  filteredBooks, updateLastClicked,
  showDeleteConfirm, deleteBook, confirmDelete
} = useBooks({ showToast })

const { currentPage, paginatedBooks, totalPages, goToPage, prevPage, nextPage } = usePagination(filteredBooks)

const chapters = useChapters()

const {
  showBookDetailModal, selectedBook, showFullCatalog,
  bookRecommendations, recommendationsLoading,
  openBookDetail, toggleFullCatalog
} = useBookDetail({ chapters, updateLastClicked })

// study 与 notes 互相依赖（startStudy 需要 loadNotes；notes 需要 studyBook/currentChapter/selectedText），
// 通过延迟闭包解引用，避免创建顺序死锁。
const notesApi = {}
const {
  showStudyModal, studyBook, currentChapter, selectedChapter, currentContent,
  searchKeyword, searchResults, showSearchResults, searchChapters, clearSearch,
  studyStartTime, contentLoading,
  showSelectionMenu, selectionMenuTop, selectionMenuLeft, selectedText,
  showQuestionSidebar, userQuestion, showAIAnswer, aiAnswer,
  selectChapter, closeStudyModal, startStudy, endStudy,
  handleTextSelection, askQuestion, submitQuestion, goToQACenter,
  restoreStudyStateFromStorage
} = useStudy({
  router, showToast, books, chapters,
  loadNotes: () => notesApi.loadNotes()
})

const {
  showNoteModal, notes, currentNote, noteContent, noteTitle, noteLoading,
  noteSearchQuery, selectedNoteChapterId, showNotePreview, showExportOptions, noteTextarea,
  filteredNotes,
  loadNotes, createNote, updateNote, deleteNote, openNoteDetail, createNewNote, closeNoteModal,
  searchNotes, clearNoteSearch,
  insertMarkdown, insertSelectedText, exportNotes,
  getNotePreview, renderMarkdownPreview, formatNoteDate
} = useNotes({ showToast, study: { studyBook, currentChapter, selectedText }, chapters })
Object.assign(notesApi, { loadNotes })

const {
  showAddTextbookModal, textbookSearchQuery,
  categories, selectedCategory, selectedBooks, textbooks,
  filteredTextbooks, addButtonText,
  selectCategory, toggleSelect, batchAdd, addTextbook
} = useAddTextbook({ showToast, fetchBooks })

const { showEditBookModal, editBookForm, editLoading, openEditBookModal, saveBookChanges } = useEditBook({ showToast, books, selectedBook })

// ---------- 返回首页 ----------
const backToHome = () => {
  router.push('/')
}

// ---------- 生命周期 ----------
onMounted(() => {
  // 设置组件为已挂载状态
  markMounted()

  // 加载夜间模式设置
  loadDarkModeSetting()

  // 检查是否有保存的学习状态（跳转问答中心后返回）
  restoreStudyStateFromStorage()

  // 获取书籍列表
  fetchBooks()
})

onUnmounted(() => {
  // 页面切换时保存学习进度
  if (studyBook.value && studyStartTime.value) {
    endStudy()
  }
})

// ---------- 依赖注入 ----------
provide('bookshelfCtx', {
  // 夜间模式
  isDarkMode, toggleDarkMode,
  // Toast
  showSuccessToast, successMessage, toastType,
  // 头部
  searchQuery, searchBooks, addTextbook, backToHome,
  // 筛选
  majorFilter, showMajorDropdown, majorOptions, selectMajor, toggleMajorDropdown, getSelectedMajorName,
  semesterFilter, showSemesterDropdown, semesterOptions, selectSemester, toggleSemesterDropdown, getSelectedSemesterName,
  courseTypeFilter, showCourseTypeDropdown, courseTypeOptions, selectCourseType, toggleCourseTypeDropdown, getSelectedCourseTypeName,
  // 书籍网格 + 分页
  paginatedBooks, totalPages, currentPage, goToPage, prevPage, nextPage,
  openBookDetail, deleteBook,
  // 添加教材
  showAddTextbookModal, textbookSearchQuery, batchAdd, addButtonText,
  categories, selectedCategory, selectCategory, filteredTextbooks, selectedBooks, toggleSelect,
  // 删除确认
  showDeleteConfirm, confirmDelete,
  // 书籍详情
  showBookDetailModal, selectedBook, showFullCatalog, toggleFullCatalog,
  bookRecommendations, recommendationsLoading,
  chapterTree: chapters.chapterTree, chapterLoading: chapters.chapterLoading, chapterError: chapters.chapterError,
  // 学习
  showStudyModal, closeStudyModal, startStudy,
  showQuestionSidebar, userQuestion, showAIAnswer, aiAnswer, submitQuestion, goToQACenter,
  showSelectionMenu, selectionMenuTop, selectionMenuLeft, selectedText, askQuestion, handleTextSelection,
  searchKeyword, searchResults, showSearchResults, searchChapters, clearSearch,
  expandedChapters: chapters.expandedChapters,
  toggleChapter: chapters.toggleChapter, toggleAllChapters: chapters.toggleAllChapters,
  currentChapter, selectedChapter, selectChapter, currentContent, contentLoading,
  // 笔记
  showNoteModal, currentNote, noteTitle, noteContent, noteLoading,
  noteSearchQuery, selectedNoteChapterId, showNotePreview, showExportOptions, noteTextarea,
  filteredNotes, createNote, updateNote, deleteNote, openNoteDetail, createNewNote, closeNoteModal,
  searchNotes, clearNoteSearch, insertMarkdown, insertSelectedText, exportNotes,
  getNotePreview, renderMarkdownPreview, formatNoteDate,
  getChapterTitleById: chapters.getChapterTitleById,
  // 编辑书籍
  showEditBookModal, editBookForm, editLoading, saveBookChanges
})
</script>

<style src="./bookshelf.css"></style>
