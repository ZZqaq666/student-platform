<template>
  <!-- 跨书「我的笔记」总览弹窗 -->
  <transition name="modal">
    <div
      class="modal-overlay my-notes-overlay"
      v-if="showMyNotesModal"
      @click="closeMyNotesModal"
      :class="{ 'dark-mode': isDarkMode }"
    >
      <div class="modal-content my-notes-modal" @click.stop :class="{ 'dark-mode': isDarkMode }">
        <div class="modal-header">
          <h2 class="modal-title">我的笔记</h2>
          <button class="modal-close" @click="closeMyNotesModal">×</button>
        </div>

        <!-- 搜索 -->
        <div class="my-notes-search">
          <input
            type="text"
            v-model="allNotesSearchQuery"
            placeholder="搜索笔记标题或内容..."
            class="my-notes-search-input"
          >
          <button v-if="allNotesSearchQuery" class="clear-btn" @click="allNotesSearchQuery = ''">×</button>
        </div>

        <!-- 列表主体 -->
        <div class="my-notes-body">
          <div v-if="allNotesLoading" class="loading">加载中...</div>
          <div v-else-if="filteredAllNotes.length === 0" class="empty-notes">
            <p>{{ allNotesSearchQuery ? '未找到匹配的笔记' : '暂无笔记' }}</p>
          </div>
          <div v-else class="my-notes-groups">
            <div class="my-notes-group" v-for="group in groupedAllNotes" :key="group.key">
              <div class="my-notes-group-title">
                <span class="group-icon">📚</span>
                <span class="group-book">{{ group.bookTitle }}</span>
                <span class="group-count">{{ group.notes.length }}</span>
              </div>
              <div
                class="my-notes-item"
                v-for="note in group.notes"
                :key="note.id"
                :class="{ opening: String(openingNoteId) === String(note.id) }"
                @click="openNoteFromOverview(note)"
              >
                <div class="my-notes-item-title">{{ note.title }}</div>
                <div class="my-notes-item-chapter" v-if="note.chapterTitle">
                  <span class="chapter-tag">📖 {{ note.chapterTitle }}</span>
                </div>
                <div class="my-notes-item-preview">{{ getNotePreview(note.content) }}</div>
                <div class="my-notes-item-meta">
                  <span class="my-notes-date">{{ formatNoteDate(note.updatedAt || note.createdAt) }}</span>
                  <span class="my-notes-open">打开 ›</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { inject } from 'vue'

const {
  isDarkMode,
  showMyNotesModal, closeMyNotesModal,
  allNotesSearchQuery, allNotesLoading,
  filteredAllNotes, groupedAllNotes, openingNoteId,
  openNoteFromOverview,
  getNotePreview, formatNoteDate
} = inject('bookshelfCtx')
</script>
