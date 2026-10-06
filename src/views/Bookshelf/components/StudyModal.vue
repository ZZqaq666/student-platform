<template>
  <!-- 学习模态框 -->
  <div class="modal-overlay" v-if="showStudyModal" @click="closeStudyModal" :class="{ 'dark-mode': isDarkMode }">
    <div class="modal-content study-modal" @click.stop :class="{ 'dark-mode': isDarkMode }">
      <!-- 顶部导航栏 -->
      <div class="study-header">
        <div class="header-left">
          <button class="back-btn" @click="closeStudyModal">返回</button>
        </div>
      </div>

      <!-- 主体内容区 -->
      <div class="study-content" :class="{ 'with-question-sidebar': showQuestionSidebar }">
        <!-- 左侧目录区 -->
        <div class="sidebar">
          <div class="sidebar-header">
            <h3>目录</h3>
            <button class="collapse-btn" @click="toggleAllChapters">▼</button>
          </div>
          <!-- 搜索输入框 -->
          <div class="search-section">
            <div class="search-input-container">
              <input
                type="text"
                v-model="searchKeyword"
                placeholder="搜索章节..."
                class="search-input"
                @keyup.enter="searchChapters"
              >
              <button class="search-btn" @click="searchChapters">🔍</button>
              <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">×</button>
            </div>
          </div>
          <div class="toc-tree">
            <!-- 搜索结果 -->
            <template v-if="showSearchResults && searchResults.length > 0">
              <div class="search-results">
                <div class="search-results-header">
                  <span>搜索结果 ({{ searchResults.length }})</span>
                  <button class="close-btn" @click="clearSearch">×</button>
                </div>
                <div
                  v-for="chapter in searchResults"
                  :key="chapter.id"
                  class="search-result-item"
                  @click="selectChapter(chapter)"
                >
                  <span class="search-result-title">{{ chapter.title }}</span>
                </div>
              </div>
            </template>
            <!-- 章节树 -->
            <template v-else>
              <template v-if="chapterTree.length > 0">
                <div
                  v-for="chapter in chapterTree"
                  :key="chapter.id"
                  class="toc-item"
                >
                  <div class="toc-header" @click="toggleChapter(chapter.id)">
                    <span class="toggle-icon">{{ expandedChapters.has(chapter.id) ? '▼' : '▶' }}</span>
                    <span
                      class="toc-title"
                      :class="{ active: currentChapter && currentChapter.id === chapter.id }"
                      @click.stop="selectChapter(chapter)"
                    >
                      {{ chapter.title }}
                    </span>
                  </div>
                  <div
                    v-if="chapter.children && chapter.children.length > 0 && expandedChapters.has(chapter.id)"
                    class="toc-children"
                  >
                    <div
                      v-for="child in chapter.children"
                      :key="child.id"
                      class="toc-item"
                    >
                      <div class="toc-header" @click="toggleChapter(child.id)">
                        <span class="toggle-icon">{{ expandedChapters.has(child.id) ? '▼' : '▶' }}</span>
                        <span
                          class="toc-title"
                          :class="{ active: currentChapter && currentChapter.id === child.id }"
                          @click.stop="selectChapter(child)"
                        >
                          {{ child.title }}
                        </span>
                      </div>
                      <div
                        v-if="child.children && child.children.length > 0 && expandedChapters.has(child.id)"
                        class="toc-children"
                      >
                        <div
                          v-for="grandchild in child.children"
                          :key="grandchild.id"
                          class="toc-item"
                        >
                          <span
                            class="toc-title"
                            :class="{ active: currentChapter && currentChapter.id === grandchild.id }"
                            @click="selectChapter(grandchild)"
                          >
                            {{ grandchild.title }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="empty-chapters">
                暂无章节数据
              </div>
            </template>
          </div>
        </div>

        <!-- 中间阅读区 -->
        <div class="reading-area">
          <div class="reading-header">
            <h2>{{ selectedChapter?.title || '开始学习' }}</h2>
            <button class="expand-btn">▼</button>
          </div>
          <div class="reading-content" @mouseup="handleTextSelection">
            <!-- 动态章节内容 -->
            <div v-if="contentLoading" class="loading-state">
              <div class="loading-spinner"></div>
              <p>内容加载中...</p>
            </div>
            <div v-else-if="currentContent" class="chapter-content" v-html="currentContent"></div>
            <div v-else class="empty-state">
              <div class="empty-icon">📖</div>
              <p>请从左侧目录选择章节开始阅读</p>
            </div>

            <button class="create-btn" @click="createNewNote">创建学习笔记</button>
          </div>
          <div class="pagination">
            <span>1</span>
            <span>2</span>
            <span>3</span>
            <span>›</span>
          </div>
        </div>

        <!-- 右侧提问侧边栏 -->
        <div class="question-sidebar" v-if="showQuestionSidebar">
          <div class="question-sidebar-header">
            <h3>提问</h3>
            <button class="close-btn" @click="showQuestionSidebar = false">×</button>
          </div>
          <div class="question-content">
            <div class="selected-text">
              <h4>选中的内容：</h4>
              <p>{{ selectedText }}</p>
            </div>
            <div class="question-input" :class="{ 'small': showAIAnswer }">
              <h4>问题：</h4>
              <textarea v-model="userQuestion" placeholder="请输入您的问题..."></textarea>
            </div>
            <button class="submit-question-btn" v-if="!showAIAnswer" @click="submitQuestion">提交问题</button>

            <!-- AI回答区域 -->
            <div class="ai-answer" v-if="showAIAnswer">
              <h4>AI回答：</h4>
              <div class="answer-content">
                <p>{{ aiAnswer }}</p>
              </div>
              <button class="detailed-question-btn" @click="goToQACenter">详细提问到个人问答中心</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 文本选择弹出菜单 -->
      <div class="text-selection-menu" v-if="showSelectionMenu" :style="{ top: selectionMenuTop + 'px', left: selectionMenuLeft + 'px' }">
        <button class="menu-item" @click="askQuestion">
          <span class="menu-icon">❓</span>
          <span>提问</span>
        </button>
      </div>

      <!-- 底部工具栏 -->
      <div class="study-footer">
        <div class="footer-left">
          <button class="footer-btn" @click="showNoteModal = true">
            <span class="btn-icon">📝</span>
            <span class="btn-text">笔记</span>
          </button>
          <button class="footer-btn" @click="toggleDarkMode">
            <span class="btn-icon">{{ isDarkMode ? '☀️' : '🌙' }}</span>
            <span class="btn-text">{{ isDarkMode ? '日间模式' : '夜间模式' }}</span>
            <span class="btn-arrow">›</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const {
  isDarkMode, toggleDarkMode,
  showStudyModal, closeStudyModal,
  showQuestionSidebar, userQuestion, showAIAnswer, aiAnswer, submitQuestion, goToQACenter,
  selectedText, showSelectionMenu, selectionMenuTop, selectionMenuLeft, askQuestion, handleTextSelection,
  searchKeyword, searchResults, showSearchResults, searchChapters, clearSearch,
  chapterTree, expandedChapters, toggleChapter, toggleAllChapters,
  currentChapter, selectedChapter, selectChapter, currentContent, contentLoading,
  createNewNote, showNoteModal
} = inject('bookshelfCtx')
</script>
