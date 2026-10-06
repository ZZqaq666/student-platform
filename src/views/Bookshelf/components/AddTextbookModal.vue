<template>
  <!-- 添加教材模态框 -->
  <div class="modal-overlay" v-if="showAddTextbookModal" @click="showAddTextbookModal = false">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">添加教材</h2>
        <button class="modal-close" @click="showAddTextbookModal = false">
          ×
        </button>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-bar">
          <input
            type="text"
            v-model="textbookSearchQuery"
            placeholder="输入教材名称/ISBN搜索"
            class="search-input"
          >
        </div>
        <button class="batch-add-btn" @click="batchAdd">
          {{ addButtonText }}
        </button>
      </div>

      <!-- 分类标签 -->
      <div class="category-section">
        <button
          v-for="category in categories"
          :key="category"
          class="category-btn"
          :class="{ active: selectedCategory === category }"
          @click="selectCategory(category)"
        >
          {{ category }}
        </button>
        <button class="category-btn more-btn">
          ...
        </button>
      </div>

      <!-- 教材列表 -->
      <div class="textbook-grid">
        <div
          v-for="(book, index) in filteredTextbooks"
          :key="index"
          class="textbook-card"
          :class="{ selected: selectedBooks.includes(index) }"
          @click="toggleSelect(index)"
        >
          <div class="textbook-cover">
            <img :src="getProxyImageUrl(book.coverImage) || getDefaultCover(book.subject)" :alt="book.title" @error="handleImageError">
          </div>
          <div class="textbook-title">{{ book.title }}</div>
          <div class="select-checkbox" v-if="selectedBooks.includes(index)">
            ✓
          </div>
        </div>
      </div>


    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { getProxyImageUrl, getDefaultCover, handleImageError } from '../utils/bookCovers'

const {
  showAddTextbookModal, textbookSearchQuery, batchAdd, addButtonText,
  categories, selectedCategory, selectCategory,
  filteredTextbooks, selectedBooks, toggleSelect
} = inject('bookshelfCtx')
</script>
