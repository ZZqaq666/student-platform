<template>
  <!-- 书籍详情弹窗 -->
  <div class="modal-overlay" v-if="showBookDetailModal" @click="showBookDetailModal = false">
    <div class="modal-content book-detail-modal" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">{{ selectedBook?.title }}</h2>
        <button class="modal-close" @click="showBookDetailModal = false">
          ×
        </button>
      </div>

      <div class="book-detail-content">
        <div class="book-detail-main">
          <!-- 书籍封面和基本信息 -->
          <div class="book-detail-header">
            <div class="book-detail-cover">
              <img :src="getProxyImageUrl(selectedBook?.coverImage) || getDefaultCover(selectedBook?.subject)" :alt="selectedBook?.title" @error="handleImageError">
            </div>
            <div class="book-detail-info">
              <h3 class="book-detail-title">{{ selectedBook?.title }}</h3>
              <div class="book-detail-meta">
                <span class="meta-item">作者：{{ selectedBook?.author || '未知作者' }}</span>
                <span class="meta-item">出版社：{{ selectedBook?.publisher || '未知出版社' }}</span>
                <span class="meta-item">出版年份：{{ selectedBook?.publish_date ? new Date(selectedBook.publish_date).getFullYear() : '2024' }}</span>
              </div>
              <div class="book-detail-description">
                <h4>书籍简介</h4>
                <p>{{ selectedBook?.description || '暂无简介' }}</p>
              </div>
              <div class="book-detail-catalog">
                <h4>目录</h4>
                <div class="catalog-link" @click="toggleFullCatalog">
                  {{ showFullCatalog ? '收起目录' : '查看完整目录' }} →
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="book-detail-actions">
            <button class="action-btn primary" @click="startStudy(selectedBook)">
              <span class="btn-icon">📚</span>
              {{ selectedBook.lastReadChapterId ? '继续学习' : '开始学习' }}
            </button>
            <button class="action-btn edit" @click="openEditBookModal(selectedBook)">
              <span class="btn-icon">✏️</span>
              编辑
            </button>
            <button class="action-btn delete" @click="deleteBook(selectedBook.id)">
              <span class="btn-icon">🗑️</span>
              删除书籍
            </button>
          </div>
        </div>

        <!-- 右侧侧边栏 -->
        <div class="book-detail-sidebar">
          <!-- 完整目录展示区 -->
          <div class="sidebar-section" v-if="showFullCatalog">
            <h4>完整目录</h4>
            <div class="full-catalog">
              <!-- 加载状态 -->
              <div v-if="chapterLoading" class="loading-state">
                <div class="loading-spinner"></div>
                <span class="loading-text">加载章节中...</span>
              </div>

              <!-- 错误状态 -->
              <div v-else-if="chapterError" class="error-state">
                <span class="error-icon">⚠️</span>
                <span class="error-text">{{ chapterError }}</span>
              </div>

              <!-- 空状态 -->
              <div v-else-if="chapterTree.length === 0" class="empty-state">
                <span class="empty-icon">📚</span>
                <span class="empty-text">暂无章节数据</span>
              </div>

              <!-- 章节树 -->
              <template v-else>
                <div v-for="chapter in chapterTree" :key="chapter.id" class="catalog-chapter">
                  <div class="chapter-title">{{ chapter.title }}</div>
                  <div class="chapter-sections" v-if="chapter.children && chapter.children.length > 0">
                    <div v-for="child in chapter.children" :key="child.id" class="section-item">
                      {{ child.title }}
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- 推荐书籍和用户评价 -->
          <template v-else>
            <!-- 推荐书籍 -->
            <div class="sidebar-section">
              <h4>推荐的类似书籍</h4>
              <div class="recommended-books" v-if="!recommendationsLoading">
                <div v-if="bookRecommendations?.recommendedBooks && bookRecommendations.recommendedBooks.length > 0">
                  <div class="recommended-book" v-for="book in bookRecommendations.recommendedBooks" :key="book.id">
                    <div class="recommended-book-cover">
                      <img :src="getProxyImageUrl(book.coverImage) || getDefaultCover(book.subject)" :alt="book.title" @error="handleImageError">
                    </div>
                    <div class="recommended-book-info">
                      <h5 class="recommended-book-title">{{ book.title }}</h5>
                      <p class="recommended-book-author">{{ book.author }}</p>
                      <p class="recommendation-reason">{{ book.recommendationReason }}</p>
                    </div>
                  </div>
                </div>
                <div v-else class="no-recommendations">
                  暂无推荐书籍
                </div>
              </div>
              <div v-else class="loading">
                加载中...
              </div>
            </div>

            <!-- 用户评价 -->
            <div class="sidebar-section">
              <h4>用户评价</h4>
              <div class="user-reviews" v-if="!recommendationsLoading">
                <div v-if="bookRecommendations?.userReviews && bookRecommendations.userReviews.length > 0">
                  <div class="review-item" v-for="review in bookRecommendations.userReviews" :key="review.id">
                    <div class="review-header">
                      <span class="reviewer-name">{{ review.username }}</span>
                      <span class="review-rating">
                        <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= review.rating }">★</span>
                      </span>
                    </div>
                    <div class="review-content">
                      {{ review.content }}
                    </div>
                    <div class="review-footer">
                      <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                      <span class="review-likes">👍 {{ review.likes }}</span>
                    </div>
                  </div>
                </div>
                <div v-else class="no-reviews">
                  暂无用户评价
                </div>
              </div>
              <div v-else class="loading">
                加载中...
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import { getProxyImageUrl, getDefaultCover, handleImageError } from '../utils/bookCovers'
import { formatDate } from '../utils/formatters'

const {
  showBookDetailModal, selectedBook, showFullCatalog, toggleFullCatalog,
  bookRecommendations, recommendationsLoading,
  chapterTree, chapterLoading, chapterError,
  startStudy, openEditBookModal, deleteBook
} = inject('bookshelfCtx')
</script>
