<template>
  <!-- 笔记模态框 -->
  <div class="modal-overlay" v-if="showNoteModal" @click="closeNoteModal" :class="{ 'dark-mode': isDarkMode }">
    <div class="modal-content note-modal" @click.stop :class="{ 'dark-mode': isDarkMode }">
      <div class="modal-header">
        <h2 class="modal-title">{{ currentNote ? '编辑笔记' : '新建笔记' }}</h2>
        <div class="modal-header-actions">
          <button class="export-btn" @click="showExportOptions = !showExportOptions" title="导出笔记">
            <span class="btn-icon">📥</span>
            导出
          </button>
          <div class="export-dropdown" v-if="showExportOptions">
            <div class="export-option" @click="exportNotes('markdown')">导出为 Markdown</div>
            <div class="export-option" @click="exportNotes('txt')">导出为 文本</div>
          </div>
          <button class="modal-close" @click="closeNoteModal">×</button>
        </div>
      </div>

      <div class="note-content">
        <!-- 笔记列表和编辑区域 -->
        <div class="note-container">
          <!-- 左侧笔记列表 -->
          <div class="note-list">
            <div class="note-list-header">
              <h3>我的笔记</h3>
              <button class="add-note-btn" @click="createNewNote">+ 新建笔记</button>
            </div>
            <!-- 笔记搜索 -->
            <div class="note-search">
              <input
                type="text"
                v-model="noteSearchQuery"
                placeholder="搜索笔记..."
                class="note-search-input"
              >
              <button class="search-btn" @click="searchNotes">🔍</button>
              <button v-if="noteSearchQuery" class="clear-btn" @click="clearNoteSearch">×</button>
            </div>
            <div class="note-list-content">
              <div v-if="noteLoading" class="loading">加载中...</div>
              <div v-else-if="filteredNotes.length === 0" class="empty-notes">
                <p>{{ noteSearchQuery ? '未找到匹配的笔记' : '暂无笔记，点击新建笔记开始记录' }}</p>
              </div>
              <div v-else>
                <div
                  v-for="note in filteredNotes"
                  :key="note.id"
                  class="note-item"
                  :class="{ active: currentNote && currentNote.id === note.id }"
                  @click="openNoteDetail(note)"
                >
                  <div class="note-item-title">{{ note.title }}</div>
                  <div class="note-item-chapter" v-if="note.chapterTitle">
                    <span class="chapter-tag">📖 {{ note.chapterTitle }}</span>
                  </div>
                  <div class="note-item-preview">{{ getNotePreview(note.content) }}</div>
                  <div class="note-item-meta">
                    <span class="note-date">{{ formatNoteDate(note.updatedAt || note.createdAt) }}</span>
                    <button class="delete-note-btn" @click.stop="deleteNote(note.id)">×</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧笔记编辑区 -->
          <div class="note-editor">
            <!-- 章节选择 -->
            <div class="chapter-selector" v-if="chapterTree.length > 0">
              <label class="selector-label">关联章节：</label>
              <select v-model="selectedNoteChapterId" class="chapter-select">
                <option value="">选择章节（可选）</option>
                <optgroup v-for="chapter in chapterTree" :key="chapter.id" :label="chapter.title">
                  <option :value="chapter.id">{{ chapter.title }}</option>
                  <option v-for="child in chapter.children" :key="child.id" :value="child.id">
                    &nbsp;&nbsp;└ {{ child.title }}
                  </option>
                </optgroup>
              </select>
              <span v-if="selectedNoteChapterId" class="current-chapter-tag">
                {{ getChapterTitleById(selectedNoteChapterId) }}
              </span>
            </div>

            <input
              type="text"
              v-model="noteTitle"
              placeholder="请输入笔记标题"
              class="note-title-input"
            >

            <!-- 富文本工具栏 -->
            <div class="editor-toolbar">
              <button class="toolbar-btn" @click="insertMarkdown('**', '**')" title="粗体">B</button>
              <button class="toolbar-btn" @click="insertMarkdown('*', '*')" title="斜体">I</button>
              <button class="toolbar-btn" @click="insertMarkdown('# ', '')" title="标题">H</button>
              <button class="toolbar-btn" @click="insertMarkdown('- ', '')" title="列表">•</button>
              <button class="toolbar-btn" @click="insertMarkdown('> ', '')" title="引用">"</button>
              <button class="toolbar-btn" @click="insertMarkdown('```\n', '\n```')" title="代码块">{ }</button>
              <button class="toolbar-btn highlight-btn" @click="insertSelectedText" title="插入选中内容" :disabled="!selectedText">
                📄 插入选中内容
              </button>
            </div>

            <textarea
              v-model="noteContent"
              placeholder="请输入笔记内容，支持 Markdown 格式"
              class="note-content-textarea"
              rows="12"
              ref="noteTextarea"
            ></textarea>

            <!-- 预览切换 -->
            <div class="preview-toggle">
              <button class="toggle-btn" :class="{ active: showNotePreview }" @click="showNotePreview = !showNotePreview">
                {{ showNotePreview ? '隐藏预览' : '显示预览' }}
              </button>
            </div>

            <!-- 笔记预览 -->
            <div class="note-preview" v-if="showNotePreview && noteContent" v-html="renderMarkdownPreview(noteContent)"></div>

            <div class="note-editor-actions">
              <button
                class="save-note-btn"
                @click="currentNote ? updateNote() : createNote()"
                :disabled="noteLoading || !noteTitle.trim()"
              >
                {{ noteLoading ? '保存中...' : '保存' }}
              </button>
              <button class="cancel-note-btn" @click="closeNoteModal">取消</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const {
  isDarkMode,
  showNoteModal, closeNoteModal, currentNote,
  showExportOptions, exportNotes,
  noteSearchQuery, searchNotes, clearNoteSearch, filteredNotes, noteLoading,
  openNoteDetail, deleteNote, createNewNote,
  getNotePreview, formatNoteDate,
  chapterTree, getChapterTitleById, selectedNoteChapterId,
  noteTitle, insertMarkdown, insertSelectedText, selectedText,
  noteContent, showNotePreview, renderMarkdownPreview,
  noteTextarea, updateNote, createNote
} = inject('bookshelfCtx')
</script>
