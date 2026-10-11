<template>
  <div class="bookshelf-header">
    <div class="header-content">
      <button class="back-home-btn" @click="backToHome">
        <span class="back-icon">←</span>
        返回首页
      </button>
      <h1 class="page-title">大学教材库</h1>
      <button class="dark-mode-toggle" @click="toggleDarkMode" :title="isDarkMode ? '切换到日间模式' : '切换到夜间模式'">
        <span class="toggle-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
      </button>
    </div>
    <div class="search-wrapper">
      <div class="search-container">
        <div class="search-bar">
          <input
            type="text"
            v-model="searchQuery"
            placeholder="搜索大学教材、课程名称、专业领域"
            class="search-input"
            @keyup.enter="searchBooks"
          >
        </div>
      </div>
      <div class="header-actions">
        <button class="my-notes-btn" @click="openMyNotesModal">
          <span class="btn-icon">📝</span>
          我的笔记
        </button>
        <button class="add-textbook-btn" @click="addTextbook">
          添加大学教材
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const {
  isDarkMode, toggleDarkMode, searchQuery, searchBooks, addTextbook, backToHome, openMyNotesModal
} = inject('bookshelfCtx')
</script>
