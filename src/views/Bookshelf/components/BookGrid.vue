<template>
  <div>
    <!-- 书籍列表 -->
    <div class="books-container">
      <div
        v-for="book in paginatedBooks"
        :key="book.id"
        class="book-card"
        :class="book.status"
        @click="openBookDetail(book)"
      >
        <div class="book-status" :class="book.status">
          {{ getStatusText(book.status) }}
        </div>
        <button
          class="delete-btn"
          @click.stop="deleteBook(book.id)"
          title="删除书籍"
        >
          ×
        </button>
        <div class="book-cover">
          <img :src="getProxyImageUrl(book.coverImage) || getDefaultCover(book.subject)" :alt="book.title" @error="handleImageError">
        </div>
        <div class="book-info">
          <h3 class="book-title">{{ book.title }}</h3>
          <p class="book-author">{{ book.author }}</p>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: book.progress + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页组件 -->
    <div class="pagination" v-if="totalPages > 1">
      <button
        class="pagination-btn"
        :disabled="currentPage === 1"
        @click="prevPage"
      >
        上一页
      </button>
      <div class="pagination-numbers">
        <button
          v-for="page in totalPages"
          :key="page"
          class="pagination-number"
          :class="{ active: currentPage === page }"
          @click="goToPage(page)"
        >
          {{ page }}
        </button>
      </div>
      <button
        class="pagination-btn"
        :disabled="currentPage === totalPages"
        @click="nextPage"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { getProxyImageUrl, getDefaultCover, handleImageError } from '../utils/bookCovers'
import { getStatusText } from '../utils/formatters'

const {
  paginatedBooks, totalPages, currentPage, goToPage, prevPage, nextPage,
  openBookDetail, deleteBook
} = inject('bookshelfCtx')
</script>
