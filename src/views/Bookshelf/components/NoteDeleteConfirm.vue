<template>
  <!-- 笔记删除二次确认（浮在笔记模态之上，取消只关闭本层） -->
  <transition name="modal">
    <div
      class="modal-overlay note-confirm-overlay"
      v-if="showDeleteNoteConfirm"
      @click.stop="cancelDeleteNote"
      :class="{ 'dark-mode': isDarkMode }"
    >
      <div class="confirm-modal" @click.stop :class="{ 'dark-mode': isDarkMode }">
        <div class="confirm-header">
          <h3 class="confirm-title">删除笔记</h3>
        </div>
        <div class="confirm-body">
          <p class="confirm-message">确定要删除这条笔记吗？删除后不可恢复。</p>
          <p class="confirm-note-title" v-if="pendingDeleteNote?.title">
            {{ pendingDeleteNote.title }}
          </p>
        </div>
        <div class="confirm-footer">
          <button class="confirm-btn cancel" @click="cancelDeleteNote">取消</button>
          <button class="confirm-btn delete" @click="confirmDeleteNote" :disabled="noteLoading">
            {{ noteLoading ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { inject } from 'vue'

const {
  isDarkMode,
  showDeleteNoteConfirm, pendingDeleteNote,
  confirmDeleteNote, cancelDeleteNote,
  noteLoading
} = inject('bookshelfCtx')
</script>
