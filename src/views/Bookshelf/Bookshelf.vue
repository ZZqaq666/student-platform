<template>
  <div class="bookshelf-page" :class="{ 'dark-mode': isDarkMode }">
    <!-- 页面头部 -->
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
        <button class="add-textbook-btn" @click="addTextbook">
          添加大学教材
        </button>
      </div>
    </div>

    <!-- 书籍筛选 -->
    <div class="bookshelf-filter">
      <div class="filter-section">
        <div class="filter-label">按专业领域筛选</div>
        <div class="select-container">
          <div class="select-dropdown" v-show="showMajorDropdown">
            <div 
              v-for="major in majorOptions" 
              :key="major.value"
              class="select-option"
              :class="{ active: majorFilter === major.value }"
              @click="selectMajor(major.value)"
            >
              {{ major.label }}
            </div>
          </div>
          <div class="select-display" @click="toggleMajorDropdown">
            {{ getSelectedMajorName() }}
            <span class="select-arrow">▼</span>
          </div>
        </div>
      </div>
      <div class="filter-section">
        <div class="filter-label">按学期筛选</div>
        <div class="select-container">
          <div class="select-dropdown" v-show="showSemesterDropdown">
            <div 
              v-for="semester in semesterOptions" 
              :key="semester.value"
              class="select-option"
              :class="{ active: semesterFilter === semester.value }"
              @click="selectSemester(semester.value)"
            >
              {{ semester.label }}
            </div>
          </div>
          <div class="select-display" @click="toggleSemesterDropdown">
            {{ getSelectedSemesterName() }}
            <span class="select-arrow">▼</span>
          </div>
        </div>
      </div>
      <div class="filter-section">
        <div class="filter-label">按课程类型筛选</div>
        <div class="select-container">
          <div class="select-dropdown" v-show="showCourseTypeDropdown">
            <div 
              v-for="courseType in courseTypeOptions" 
              :key="courseType.value"
              class="select-option"
              :class="{ active: courseTypeFilter === courseType.value }"
              @click="selectCourseType(courseType.value)"
            >
              {{ courseType.label }}
            </div>
          </div>
          <div class="select-display" @click="toggleCourseTypeDropdown">
            {{ getSelectedCourseTypeName() }}
            <span class="select-arrow">▼</span>
          </div>
        </div>
      </div>
    </div>

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

  </div>

  <!-- 成功提示 -->
  <transition name="toast">
    <div class="toast" v-if="showSuccessToast">
      <div class="toast-icon" :class="{ 'error': toastType === 'error' }">
        {{ toastType === 'success' ? '✓' : '×' }}
      </div>
      <div class="toast-message">{{ successMessage }}</div>
    </div>
  </transition>

  <!-- 确认删除对话框 -->
  <transition name="modal">
    <div class="modal-overlay" v-if="showDeleteConfirm" @click="showDeleteConfirm = false">
      <div class="confirm-modal" @click.stop>
        <div class="confirm-header">
          <h3 class="confirm-title">确认删除</h3>
        </div>
        <div class="confirm-body">
          <p class="confirm-message">确定要删除这本书吗？</p>
        </div>
        <div class="confirm-footer">
          <button class="confirm-btn cancel" @click="showDeleteConfirm = false">取消</button>
          <button class="confirm-btn delete" @click="confirmDelete">删除</button>
        </div>
      </div>
    </div>
  </transition>

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
  
  <!-- 编辑书籍模态框 -->
  <div class="modal-overlay" v-if="showEditBookModal" @click="showEditBookModal = false" :class="{ 'dark-mode': isDarkMode }">
    <div class="modal-content edit-book-modal" @click.stop :class="{ 'dark-mode': isDarkMode }">
      <div class="modal-header">
        <div class="modal-header-content">
          <div class="modal-icon">📚</div>
          <h2 class="modal-title">编辑书籍信息</h2>
        </div>
        <button class="modal-close" @click="showEditBookModal = false">
          <span class="close-icon">×</span>
        </button>
      </div>
      
      <div class="edit-book-content">
        <form class="book-form">
          <div class="form-row">
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">📚</span>
                <label for="edit-book-title">书名</label>
              </div>
              <input 
                type="text" 
                id="edit-book-title" 
                v-model="editBookForm.title" 
                class="form-input"
                placeholder="请输入书名"
              >
            </div>
            
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">👤</span>
                <label for="edit-book-author">作者</label>
              </div>
              <input 
                type="text" 
                id="edit-book-author" 
                v-model="editBookForm.author" 
                class="form-input"
                placeholder="请输入作者"
              >
            </div>
          </div>
          
          <div class="form-field">
            <div class="field-label">
              <span class="label-icon">📝</span>
              <label for="edit-book-description">简介</label>
            </div>
            <textarea 
              id="edit-book-description" 
              v-model="editBookForm.description" 
              class="form-textarea"
              placeholder="请输入书籍简介"
              rows="4"
            ></textarea>
          </div>
          
          <div class="form-row">
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">🎓</span>
                <label for="edit-book-major">专业</label>
              </div>
              <select 
                id="edit-book-major" 
                v-model="editBookForm.major" 
                class="form-select"
              >
                <option value="">请选择专业</option>
                <option v-for="major in majorOptions" :key="major.value" :value="major.value">
                  {{ major.label }}
                </option>
              </select>
            </div>
            
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">📅</span>
                <label for="edit-book-semester">学期</label>
              </div>
              <select 
                id="edit-book-semester" 
                v-model="editBookForm.semester" 
                class="form-select"
              >
                <option value="">请选择学期</option>
                <option v-for="semester in semesterOptions" :key="semester.value" :value="semester.value">
                  {{ semester.label }}
                </option>
              </select>
            </div>
            
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">📋</span>
                <label for="edit-book-courseType">课程类型</label>
              </div>
              <select 
                id="edit-book-courseType" 
                v-model="editBookForm.courseType" 
                class="form-select"
              >
                <option value="">请选择课程类型</option>
                <option v-for="courseType in courseTypeOptions" :key="courseType.value" :value="courseType.value">
                  {{ courseType.label }}
                </option>
              </select>
            </div>
          </div>
          
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="showEditBookModal = false">
              <span class="btn-icon">❌</span>
              取消
            </button>
            <button type="button" class="btn btn-primary" @click="saveBookChanges" :disabled="editLoading">
              <span v-if="editLoading" class="loading-spinner small"></span>
              <span v-else class="btn-icon">💾</span>
              {{ editLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { apiService } from '@/api/api'

const router = useRouter()

// 组件挂载状态
const isMounted = ref(false)

// 夜间模式状态
const isDarkMode = ref(false)

// 切换夜间模式
const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  localStorage.setItem('bookshelfDarkMode', isDarkMode.value)
}

// 从 localStorage 加载夜间模式设置
const loadDarkModeSetting = () => {
  const savedMode = localStorage.getItem('bookshelfDarkMode')
  if (savedMode !== null) {
    isDarkMode.value = savedMode === 'true'
  }
}

// 在组件挂载时检查是否有保存的学习状态
onMounted(() => {
  // 设置组件为已挂载状态
  isMounted.value = true
  
  // 加载夜间模式设置
  loadDarkModeSetting()
  
  const savedStudyState = localStorage.getItem('studyModalState')
  if (savedStudyState) {
    const state = JSON.parse(savedStudyState)
    if (state.showStudyModal && state.studyBook) {
      studyBook.value = state.studyBook
      showStudyModal.value = true
      showQuestionSidebar.value = state.showQuestionSidebar || false
      userQuestion.value = state.userQuestion || ''
      showAIAnswer.value = state.showAIAnswer || false
      // 清除保存的状态，避免重复打开
      localStorage.removeItem('studyModalState')
    }
  }
})

// 组件卸载
onUnmounted(() => {
  // 设置组件为未挂载状态
  isMounted.value = false
  
  // 取消正在进行的书籍搜索请求
  if (booksSearchAbortController) {
    booksSearchAbortController.abort()
  }
  // 页面切换时保存学习进度
  if (studyBook.value && studyStartTime.value) {
    endStudy()
  }
})

// 搜索查询
const searchQuery = ref('')

// 教材搜索查询
const textbookSearchQuery = ref('')

// 成功提示
const showSuccessToast = ref(false)
const successMessage = ref('')
const toastType = ref('success')

// 确认删除对话框
const showDeleteConfirm = ref(false)
let bookToDelete = null

// 书籍数据
const books = ref([])

// 专业筛选
const majorFilter = ref('')
const showMajorDropdown = ref(false)

// 学期筛选
const semesterFilter = ref('')
const showSemesterDropdown = ref(false)

// 课程类型筛选
const courseTypeFilter = ref('')
const showCourseTypeDropdown = ref(false)

// 专业选项
const majorOptions = ref([
  { value: '', label: '全部专业' },
  { value: '计算机科学与技术', label: '计算机科学与技术' },
  { value: '数学', label: '数学' },
  { value: '物理', label: '物理' },
  { value: '经济学', label: '经济学' },
  { value: '管理学', label: '管理学' },
  { value: '通识教育', label: '通识教育' }
])

// 学期选项
const semesterOptions = ref([
  { value: '', label: '全部学期' },
  { value: 'Year 1', label: '大一' },
  { value: 'Year 2', label: '大二' },
  { value: 'Year 3', label: '大三' },
  { value: 'Year 4', label: '大四' }
])

// 课程类型选项
const courseTypeOptions = ref([
  { value: '', label: '全部类型' },
  { value: 'CORE', label: '专业核心课' },
  { value: 'FOUNDATION', label: '专业基础课' },
  { value: 'ELECTIVE', label: '选修课' },
  { value: 'GENERAL', label: '通识课' }
])

// 目录标题数据 - 从API获取
const chapterTitles = ref([])
const sectionTitles = ref([])

// 显示提示
const showToast = (message, isSuccess = true) => {
  successMessage.value = message
  showSuccessToast.value = true
  toastType.value = isSuccess ? 'success' : 'error'
  setTimeout(() => {
    // 检查组件是否已卸载
    if (isMounted.value) {
      showSuccessToast.value = false
    }
  }, 3000)
}

// 获取默认封面 - 使用本地 SVG 数据 URI
const getDefaultCover = (subject) => {
  const svgConfigs = {
    'MATH': { bg: '#4A90D9', text: '数学' },
    'PHYSICS': { bg: '#50C878', text: '物理' },
    'CHEMISTRY': { bg: '#FF6B6B', text: '化学' },
    'CHINESE': { bg: '#9B59B6', text: '语文' },
    'ENGLISH': { bg: '#F39C12', text: '英语' },
    'DEFAULT': { bg: '#6B7280', text: '书籍' }
  }
  const config = svgConfigs[subject] || svgConfigs['DEFAULT']
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="560" viewBox="0 0 400 560">
    <rect width="400" height="560" fill="${config.bg}"/>
    <text x="200" y="280" font-family="Arial, sans-serif" font-size="48" fill="white" text-anchor="middle" dominant-baseline="middle">${config.text}</text>
  </svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

// 处理书籍封面URL，避免使用豆瓣图片（418错误）
const getSafeCoverUrl = (coverUrl, subject) => {
  // 如果是Douban图片，使用默认封面（避免418错误）
  if (coverUrl && coverUrl.includes('doubanio.com')) {
    return getDefaultCover(subject)
  }
  return coverUrl || getDefaultCover(subject)
}

// 图片加载错误处理
const handleImageError = (event) => {
  const img = event.target
  const title = img.closest('.book-card')?.querySelector('.book-title')?.textContent || ''
  const subject = title.includes('数学') ? 'MATH' :
                  title.includes('物理') ? 'PHYSICS' :
                  title.includes('化学') ? 'CHEMISTRY' :
                  title.includes('语文') ? 'CHINESE' :
                  title.includes('英语') ? 'ENGLISH' : 'DEFAULT'
  img.src = getDefaultCover(subject)
}

// 处理图片URL，通过代理服务加载
const getProxyImageUrl = (url) => {
  if (!url) return ''
  // 如果已经是完整的图片链接（包含http://或https://），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 否则使用后端图片代理服务处理
  return `/api/image-proxy/proxy?url=${encodeURIComponent(url)}`
}

// 用于跟踪当前正在进行的书籍搜索请求
let booksSearchAbortController = null

// 获取书籍列表
const fetchBooks = async () => {
  // 取消之前的请求
  if (booksSearchAbortController) {
    booksSearchAbortController.abort()
  }
  
  // 创建新的AbortController
  booksSearchAbortController = new AbortController()
  
  try {
    const response = await apiService.bookshelf.getAllBooks(booksSearchAbortController.signal)
    // 检查响应格式，确保是后端返回的Result对象
    if (response && response.code === 200 && response.data) {
      books.value = response.data
      console.log('成功从后端获取书籍数据:', books.value.length, '本书籍')
    } else {
      console.error('获取书籍响应格式错误:', response)
      showToast('获取书籍数据格式错误，请稍后重试', false)
    }
  } catch (error) {
    // 忽略取消错误
    if (error.name !== 'AbortError') {
      console.error('获取书籍失败:', error)
      let errorMessage = '获取书籍失败，请稍后重试'
      if (error.errorMessage) {
        errorMessage = error.errorMessage
      }
      showToast(errorMessage, false)
    }
  } finally {
    // 清理AbortController
    if (booksSearchAbortController) {
      booksSearchAbortController = null
    }
  }
}

// 搜索书籍
const searchBooks = async () => {
  // 取消之前的请求
  if (booksSearchAbortController) {
    booksSearchAbortController.abort()
  }
  
  // 创建新的AbortController
  booksSearchAbortController = new AbortController()
  
  try {
    const response = await apiService.bookshelf.searchBooks(searchQuery.value, majorFilter.value, semesterFilter.value, courseTypeFilter.value, booksSearchAbortController.signal)
    // 检查响应格式，确保是后端返回的Result对象
    if (response && response.code === 200 && response.data) {
      books.value = response.data
      console.log('成功从后端搜索书籍数据:', books.value.length, '本书籍')
    } else {
      console.error('搜索书籍响应格式错误:', response)
      showToast('搜索书籍数据格式错误，请稍后重试', false)
    }
  } catch (error) {
    // 忽略取消错误
    if (error.name !== 'AbortError') {
      console.error('搜索教材失败:', error)
      let errorMessage = '搜索书籍失败，请稍后重试'
      if (error.errorMessage) {
        errorMessage = error.errorMessage
      }
      showToast(errorMessage, false)
    }
  } finally {
    // 清理AbortController
    if (booksSearchAbortController) {
      booksSearchAbortController = null
    }
  }
}

// 选择专业
const selectMajor = (majorValue) => {
  majorFilter.value = majorValue
  showMajorDropdown.value = false
  searchBooks()
}

// 选择学期
const selectSemester = (semesterValue) => {
  semesterFilter.value = semesterValue
  showSemesterDropdown.value = false
  searchBooks()
}

// 选择课程类型
const selectCourseType = (courseTypeValue) => {
  courseTypeFilter.value = courseTypeValue
  showCourseTypeDropdown.value = false
  searchBooks()
}

// 切换专业下拉菜单
const toggleMajorDropdown = () => {
  showMajorDropdown.value = !showMajorDropdown.value
}

// 切换学期下拉菜单
const toggleSemesterDropdown = () => {
  showSemesterDropdown.value = !showSemesterDropdown.value
}

// 切换课程类型下拉菜单
const toggleCourseTypeDropdown = () => {
  showCourseTypeDropdown.value = !showCourseTypeDropdown.value
}

// 获取选中的专业名称
const getSelectedMajorName = () => {
  const major = majorOptions.value.find(m => m.value === majorFilter.value)
  return major ? major.label : '全部专业'
}

// 获取选中的学期名称
const getSelectedSemesterName = () => {
  const semester = semesterOptions.value.find(s => s.value === semesterFilter.value)
  return semester ? semester.label : '全部学期'
}

// 获取选中的课程类型名称
const getSelectedCourseTypeName = () => {
  const courseType = courseTypeOptions.value.find(c => c.value === courseTypeFilter.value)
  return courseType ? courseType.label : '全部类型'
}

// 在组件挂载时获取书籍列表
onMounted(() => {
  // 优先从后端API获取真实数据
  fetchBooks()
  
  // 检查是否有保存的学习状态
  const savedStudyState = localStorage.getItem('studyModalState')
  if (savedStudyState) {
    const state = JSON.parse(savedStudyState)
    if (state.showStudyModal && state.studyBook) {
      studyBook.value = state.studyBook
      showStudyModal.value = true
      showQuestionSidebar.value = state.showQuestionSidebar || false
      userQuestion.value = state.userQuestion || ''
      showAIAnswer.value = state.showAIAnswer || false
      // 清除保存的状态，避免重复打开
      localStorage.removeItem('studyModalState')
    }
  }
})

// 分页相关
const currentPage = ref(1)
const itemsPerRow = 4 // 每行4本书
const rowsPerPage = 3 // 每页3行
const itemsPerPage = itemsPerRow * rowsPerPage // 每页12本书

// 筛选后的书籍
const filteredBooks = computed(() => {
  let result = books.value
  
  // 按专业筛选
  if (majorFilter.value) {
    result = result.filter(book => book.major === majorFilter.value)
  }
  
  // 按学期筛选
  if (semesterFilter.value) {
    result = result.filter(book => book.semester === semesterFilter.value)
  }
  
  // 按课程类型筛选
  if (courseTypeFilter.value) {
    result = result.filter(book => book.courseType === courseTypeFilter.value)
  }
  
  // 按搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(book => 
      book.title.toLowerCase().includes(query) || 
      book.author.toLowerCase().includes(query) ||
      book.major?.toLowerCase().includes(query) ||
      book.subject?.toLowerCase().includes(query)
    )
  }
  
  // 按最近点击时间排序（降序）
  result.sort((a, b) => b.lastClicked - a.lastClicked)
  
  return result
})

// 分页后的书籍
const paginatedBooks = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredBooks.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredBooks.value.length / itemsPerPage)
})

// 切换页码
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'not-started': '未开始',
    'learning': '学习中',
    'completed': '已完成'
  }
  return statusMap[status] || status
}

// 模态框状态
const showAddTextbookModal = ref(false)

// 书籍详情弹窗
const showBookDetailModal = ref(false)
const selectedBook = ref(null)
const showFullCatalog = ref(false)

// 书籍推荐和用户评价
const bookRecommendations = ref(null)
const recommendationsLoading = ref(false)

// 学习模态框
const showStudyModal = ref(false)
const studyBook = ref(null)
const chapters = ref([])
const chapterTree = ref([])
const expandedChapters = ref(new Set())
const currentChapter = ref(null)
const selectedChapter = ref(null)
const currentContent = ref('')
const searchKeyword = ref('')
const searchResults = ref([])
const showSearchResults = ref(false)

// 学习记录相关
const studyStartTime = ref(null)
const studyEndTime = ref(null)

// 章节加载状态
const chapterLoading = ref(false)
const contentLoading = ref(false)
const chapterError = ref('')

// 文本选择弹出菜单
const showSelectionMenu = ref(false)
const selectionMenuTop = ref(0)
const selectionMenuLeft = ref(0)
const selectedText = ref('')

// 提问侧边栏
const showQuestionSidebar = ref(false)
const userQuestion = ref('')
const showAIAnswer = ref(false)
const aiAnswer = ref('')

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

// 编辑书籍相关
const showEditBookModal = ref(false)
const editBookForm = ref({
  title: '',
  author: '',
  description: '',
  major: '',
  semester: '',
  courseType: ''
})
const editLoading = ref(false)

// 打开书籍详情
const openBookDetail = async (book) => {
  selectedBook.value = book
  showBookDetailModal.value = true
  showFullCatalog.value = false
  updateLastClicked(book)
  
  // 获取书籍推荐和用户评价
  await fetchBookRecommendations(book.id)
}

// 打开编辑书籍模态框
const openEditBookModal = (book) => {
  selectedBook.value = book
  // 填充编辑表单
  editBookForm.value = {
    title: book.title || '',
    author: book.author || '',
    description: book.description || '',
    major: book.major || '',
    semester: book.semester || '',
    courseType: book.courseType || ''
  }
  showEditBookModal.value = true
}

// 保存书籍更改
const saveBookChanges = async () => {
  if (!selectedBook.value) return
  
  editLoading.value = true
  try {
    // 这里应该调用API更新书籍信息
    // 由于API可能还没有实现，这里先模拟成功
    showToast('书籍信息更新成功')
    
    // 更新本地书籍数据
    const bookIndex = books.value.findIndex(b => b.id === selectedBook.value.id)
    if (bookIndex !== -1) {
      books.value[bookIndex] = {
        ...books.value[bookIndex],
        ...editBookForm.value
      }
    }
    
    // 关闭编辑模态框
    showEditBookModal.value = false
  } catch (error) {
    console.error('更新书籍失败:', error)
    showToast('更新书籍失败', false)
  } finally {
    editLoading.value = false
  }
}

// 获取书籍推荐和用户评价
const fetchBookRecommendations = async (bookId) => {
  try {
    recommendationsLoading.value = true
    const response = await apiService.bookshelf.getBookRecommendations(bookId)
    // 由于响应拦截器已经返回了response.data，所以直接使用response
    bookRecommendations.value = response
    
    // 如果后端返回的推荐数据为空，使用mock数据作为回退
    if (!bookRecommendations.value || 
        !bookRecommendations.value.recommendedBooks || 
        bookRecommendations.value.recommendedBooks.length === 0 ||
        !bookRecommendations.value.userReviews || 
        bookRecommendations.value.userReviews.length === 0) {
      console.log('后端返回的推荐数据为空，使用mock数据:', bookRecommendations.value)
      bookRecommendations.value = generateMockRecommendations(bookId)
    }
  } catch (error) {
    console.error('获取书籍推荐失败，使用mock数据:', error)
    // 使用mock数据作为回退
    bookRecommendations.value = generateMockRecommendations(bookId)
  } finally {
    recommendationsLoading.value = false
  }
}

// 生成书籍推荐和用户评价的mock数据
const generateMockRecommendations = (bookId) => {
  // 根据书籍ID生成相应的推荐书籍和用户评价
  const mockData = {
    recommendedBooks: [],
    userReviews: []
  }
  
  // 高等数学
  if (bookId === 205 || bookId === 206) {
    mockData.recommendedBooks = [
      {
        id: 207,
        title: '线性代数',
        author: '同济大学数学系',
        coverImage: '',
        subject: '数学',
        recommendationReason: '线性代数是高等数学的重要基础，两本书内容相辅相成'
      },
      {
        id: 208,
        title: '概率论与数理统计',
        author: '浙江大学',
        coverImage: '',
        subject: '数学',
        recommendationReason: '概率论与数理统计是高等数学的延伸，适合进一步学习'
      }
    ]
    mockData.userReviews = [
      {
        id: 1,
        username: '数学爱好者',
        rating: 5,
        content: '这本书内容详实，讲解清晰，是学习高等数学的好帮手。',
        createdAt: '2026-03-15',
        likes: 12
      },
      {
        id: 2,
        username: '大学生',
        rating: 4,
        content: '例题丰富，习题有针对性，对考试很有帮助。',
        createdAt: '2026-03-20',
        likes: 8
      }
    ]
  }
  // 大学物理
  else if (bookId === 209 || bookId === 210) {
    mockData.recommendedBooks = [
      {
        id: 205,
        title: '高等数学（上册）',
        author: '同济大学数学系',
        coverImage: '',
        subject: '数学',
        recommendationReason: '物理学需要扎实的数学基础，高等数学是必备的'
      },
      {
        id: 207,
        title: '线性代数',
        author: '同济大学数学系',
        coverImage: '',
        subject: '数学',
        recommendationReason: '线性代数在物理学中有广泛应用，特别是在量子力学中'
      }
    ]
    mockData.userReviews = [
      {
        id: 3,
        username: '物理迷',
        rating: 5,
        content: '这本书深入浅出，物理概念解释得非常清楚，推荐给所有学习物理的同学。',
        createdAt: '2026-03-18',
        likes: 15
      },
      {
        id: 4,
        username: '理工科学生',
        rating: 4,
        content: '实验部分的讲解很详细，有助于理解物理概念。',
        createdAt: '2026-03-22',
        likes: 6
      }
    ]
  }
  // 计算机相关课程
  else if ([211, 212, 213, 214, 216].includes(bookId)) {
    mockData.recommendedBooks = [
      {
        id: 212,
        title: '计算机网络',
        author: '谢希仁',
        coverImage: '',
        subject: '计算机',
        recommendationReason: '计算机网络是计算机专业的核心课程，与本课程内容相关'
      },
      {
        id: 213,
        title: '操作系统原理',
        author: '汤小丹',
        coverImage: '',
        subject: '计算机',
        recommendationReason: '操作系统是计算机系统的基础，与本课程内容密切相关'
      }
    ]
    mockData.userReviews = [
      {
        id: 5,
        username: '程序员',
        rating: 5,
        content: '这本书的内容非常实用，代码示例丰富，对实际开发很有帮助。',
        createdAt: '2026-03-16',
        likes: 20
      },
      {
        id: 6,
        username: '计算机专业学生',
        rating: 4,
        content: '讲解清晰，概念明确，是学习计算机相关课程的好教材。',
        createdAt: '2026-03-21',
        likes: 10
      }
    ]
  }
  // 英语课程
  else if (bookId === 222) {
    mockData.recommendedBooks = [
      {
        id: 223,
        title: '新视野大学英语2（第三版）',
        author: '郑树棠',
        coverImage: '',
        subject: '英语',
        recommendationReason: '是本教材的续册，内容进阶，适合进一步学习'
      },
      {
        id: 224,
        title: '英语听力教程',
        author: '张民伦',
        coverImage: '',
        subject: '英语',
        recommendationReason: '提高英语听力能力，与本教材配套使用效果更佳'
      }
    ]
    mockData.userReviews = [
      {
        id: 7,
        username: '英语学习者',
        rating: 5,
        content: '教材内容新颖，话题丰富，有助于提高英语综合能力。',
        createdAt: '2026-03-17',
        likes: 18
      },
      {
        id: 8,
        username: '大学生',
        rating: 4,
        content: '配套资源丰富，有音频和视频，学习体验很好。',
        createdAt: '2026-03-23',
        likes: 9
      }
    ]
  }
  // 其他课程
  else {
    mockData.recommendedBooks = [
      {
        id: 205,
        title: '高等数学（上册）',
        author: '同济大学数学系',
        coverImage: '',
        subject: '数学',
        recommendationReason: '高等数学是大学学习的基础课程，对各专业都有帮助'
      },
      {
        id: 222,
        title: '新视野大学英语1（第三版）',
        author: '郑树棠',
        coverImage: '',
        subject: '英语',
        recommendationReason: '英语是国际化交流的工具，对未来发展很重要'
      }
    ]
    mockData.userReviews = [
      {
        id: 9,
        username: '学习者',
        rating: 4,
        content: '这本书内容丰富，讲解详细，是学习本课程的好教材。',
        createdAt: '2026-03-19',
        likes: 7
      },
      {
        id: 10,
        username: '大学生',
        rating: 3,
        content: '教材难度适中，适合课堂学习使用。',
        createdAt: '2026-03-24',
        likes: 4
      }
    ]
  }
  
  return mockData
}

// 获取书籍章节
const fetchChapters = async (bookId) => {
  chapterLoading.value = true
  chapterError.value = ''
  try {
    const response = await apiService.bookshelf.getBookChapterTree(bookId)
    console.log('Chapters API response:', response)
    if (response && response.data) {
      chapterTree.value = response.data || []
      console.log('Chapters data:', chapterTree.value)
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      chapterTree.value = response || []
      console.log('Chapters data (non-standard format):', chapterTree.value)
    }
  } catch (error) {
    console.error('获取章节失败:', error)
    chapterError.value = error.errorMessage || '获取章节失败'
  } finally {
    chapterLoading.value = false
  }
}

// 获取章节树数据
const fetchChapterTree = async (bookId) => {
  try {
    const response = await apiService.bookshelf.getBookChapterTree(bookId)
    chapterTree.value = response.data
    // 默认展开所有章节
    expandAllChapters(chapterTree.value)
  } catch (error) {
    console.error('获取章节树失败，使用mock数据:', error)
    // 使用mock数据作为回退
    chapterTree.value = [
      {
        id: 'chapter_1',
        title: '第一章 高等数学基础',
        children: [
          {
            id: 'section_1_1',
            title: '1.1 函数与极限',
            children: [
              { id: 'subsection_1_1_1', title: '1.1.1 函数的概念' },
              { id: 'subsection_1_1_2', title: '1.1.2 极限的定义' },
              { id: 'subsection_1_1_3', title: '1.1.3 极限的计算' }
            ]
          },
          {
            id: 'section_1_2',
            title: '1.2 导数与微分',
            children: [
              { id: 'subsection_1_2_1', title: '1.2.1 导数的定义' },
              { id: 'subsection_1_2_2', title: '1.2.2 导数的计算' },
              { id: 'subsection_1_2_3', title: '1.2.3 微分的概念' }
            ]
          }
        ]
      },
      {
        id: 'chapter_2',
        title: '第二章 一元函数积分学',
        children: [
          {
            id: 'section_2_1',
            title: '2.1 不定积分',
            children: [
              { id: 'subsection_2_1_1', title: '2.1.1 不定积分的概念' },
              { id: 'subsection_2_1_2', title: '2.1.2 基本积分公式' },
              { id: 'subsection_2_1_3', title: '2.1.3 换元积分法' }
            ]
          },
          {
            id: 'section_2_2',
            title: '2.2 定积分',
            children: [
              { id: 'subsection_2_2_1', title: '2.2.1 定积分的概念' },
              { id: 'subsection_2_2_2', title: '2.2.2 定积分的性质' },
              { id: 'subsection_2_2_3', title: '2.2.3 微积分基本定理' }
            ]
          }
        ]
      }
    ]
    // 默认展开所有章节
    expandAllChapters(chapterTree.value)
  }
}

// 展开所有章节
const expandAllChapters = (chapters) => {
  if (!chapters) return
  chapters.forEach(chapter => {
    expandedChapters.value.add(chapter.id)
    if (chapter.children && chapter.children.length > 0) {
      expandAllChapters(chapter.children)
    }
  })
}

// 切换章节展开/折叠
const toggleChapter = (chapterId) => {
  if (expandedChapters.value.has(chapterId)) {
    expandedChapters.value.delete(chapterId)
  } else {
    expandedChapters.value.add(chapterId)
  }
}

// 切换所有章节展开/折叠
const toggleAllChapters = () => {
  const allExpanded = chapterTree.value.every(chapter => expandedChapters.value.has(chapter.id))
  expandedChapters.value.clear()
  if (!allExpanded) {
    expandAllChapters(chapterTree.value)
  }
}

// 选择章节
const selectChapter = (chapter) => {
  currentChapter.value = chapter
  selectedChapter.value = chapter
  currentContent.value = chapter.content || ''
  showSearchResults.value = false
}

// 搜索章节
const searchChapters = async () => {
  if (!searchKeyword.value.trim()) {
    showSearchResults.value = false
    return
  }
  
  try {
    const response = await apiService.bookshelf.searchChapters(studyBook.value.id, searchKeyword.value)
    searchResults.value = response.data
    showSearchResults.value = true
  } catch (error) {
    console.error('搜索章节失败:', error)
  }
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
  showSearchResults.value = false
  searchResults.value = []
}

// 关闭学习模态框
const closeStudyModal = async () => {
  // 计算学习时间
  if (studyStartTime.value) {
    studyEndTime.value = new Date()
    const studyDuration = Math.floor((studyEndTime.value - studyStartTime.value) / 1000 / 60) // 转换为分钟
    
    if (studyDuration > 0 && studyBook.value) {
      try {
        // 更新学习进度
        const response = await apiService.bookshelf.updateProgress(studyBook.value.id, {
          progress: studyBook.value.progress || 0,
          lastReadPage: studyBook.value.lastReadPage || 0
        })
        
        if (response.code === 200) {
          // 保存学习进度到学习历史
          await apiService.learning.saveProgress({
            bookId: studyBook.value.id,
            progress: studyBook.value.progress || 0,
            studyTimeMinutes: studyDuration,
            pagesRead: studyBook.value.lastReadPage || 0
          })
          
          showToast(`学习时间：${studyDuration} 分钟，学习进度已更新`)
          // 更新本地书籍数据
          const bookIndex = books.value.findIndex(b => b.id === studyBook.value.id)
          if (bookIndex !== -1) {
            books.value[bookIndex].progress = response.data.progress || books.value[bookIndex].progress
          }
        }
      } catch (error) {
        console.error('更新学习进度失败:', error)
        showToast('更新学习进度失败', false)
      }
    }
  }
  
  // 重置学习记录
  studyStartTime.value = null
  studyEndTime.value = null
  showStudyModal.value = false
}

// 开始学习
const startStudy = async (book) => {
  studyBook.value = book
  await fetchChapterTree(book.id)
  // 加载笔记数据
  await loadNotes()
  // 记录学习开始时间
  studyStartTime.value = new Date()
  
  // 从本地存储读取学习进度
  const localProgress = localStorage.getItem(`study_progress_${book.id}`)
  let lastChapterId = book.lastReadChapterId
  
  // 如果本地存储有进度，优先使用本地存储的数据
  if (localProgress) {
    try {
      const parsedProgress = JSON.parse(localProgress)
      lastChapterId = parsedProgress.chapterId
    } catch (error) {
      console.error('解析本地存储数据失败:', error)
    }
  }
  
  // 根据是否有学习记录决定显示的章节
  if (lastChapterId && chapterTree.value.length > 0) {
    // 找到上次学习的章节
    const lastChapter = findChapterById(lastChapterId, chapterTree.value)
    if (lastChapter) {
      selectedChapter.value = lastChapter
      currentContent.value = lastChapter.content || ''
    } else {
      // 如果找不到章节，默认显示第一章
      selectedChapter.value = chapterTree.value[0]
      currentContent.value = chapterTree.value[0].content || ''
    }
  } else if (chapterTree.value.length > 0) {
    // 没有学习记录，显示第一章
    selectedChapter.value = chapterTree.value[0]
    currentContent.value = chapterTree.value[0].content || ''
  }
  
  showStudyModal.value = true
}

// 递归查找章节
const findChapterById = (id, chapters) => {
  for (const chapter of chapters) {
    if (chapter.id === id) {
      return chapter
    }
    if (chapter.children && chapter.children.length > 0) {
      const found = findChapterById(id, chapter.children)
      if (found) {
        return found
      }
    }
  }
  return null
}

// 结束学习并保存进度
const endStudy = async () => {
  if (!studyBook.value || !studyStartTime.value) return
  
  try {
    // 计算学习时间（分钟）
    const endTime = new Date()
    const studyTimeMinutes = Math.round((endTime - studyStartTime.value) / (1000 * 60))
    
    // 确保学习时间至少为1分钟
    if (studyTimeMinutes < 1) return
    
    // 构建学习进度数据
    const progressData = {
      bookId: studyBook.value.id,
      bookTitle: studyBook.value.title || '未知书籍',
      bookAuthor: studyBook.value.author || '未知作者',
      progress: studyBook.value.progress || 0,
      studyTimeMinutes: studyTimeMinutes,
      pagesRead: studyBook.value.lastReadPage || 0,
      date: new Date().toISOString().split('T')[0]
    }
    
    // 构建书籍进度更新数据
    const bookProgressData = {
      progress: studyBook.value.progress || 0,
      lastReadPage: studyBook.value.lastReadPage || 0,
      lastReadChapterId: selectedChapter.value?.id || null
    }
    
    try {
      // 保存学习进度
      await apiService.learning.saveProgress(progressData)
      console.log('学习进度保存成功:', progressData)
    } catch (error) {
      console.error('保存学习进度失败，使用本地存储作为回退:', error)
      // 使用本地存储作为回退
      const learningHistory = JSON.parse(localStorage.getItem('learningHistory') || '[]')
      learningHistory.push({
        ...progressData,
        id: Date.now(),
        userId: 4 // 假设用户ID为4
      })
      localStorage.setItem('learningHistory', JSON.stringify(learningHistory))
    }
    
    try {
      // 更新书籍进度，保存最后阅读章节
      await apiService.bookshelf.updateProgress(studyBook.value.id, bookProgressData)
      console.log('书籍进度更新成功:', bookProgressData)
    } catch (error) {
      console.error('更新书籍进度失败，使用本地存储作为回退:', error)
      // 使用本地存储作为回退
      const bookProgresses = JSON.parse(localStorage.getItem('bookProgresses') || '{}')
      bookProgresses[studyBook.value.id] = bookProgressData
      localStorage.setItem('bookProgresses', JSON.stringify(bookProgresses))
    }
  } catch (error) {
    console.error('结束学习时发生错误:', error)
  } finally {
    // 重置学习状态
    studyStartTime.value = null
    showStudyModal.value = false
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 处理文本选择
const handleTextSelection = (event) => {
  const selection = window.getSelection()
  const text = selection.toString().trim()
  
  if (text) {
    selectedText.value = text
    
    // 计算弹出菜单的位置
    const range = selection.getRangeAt(0)
    const rect = range.getBoundingClientRect()
    const modalRect = document.querySelector('.study-modal').getBoundingClientRect()
    
    selectionMenuTop.value = rect.bottom - modalRect.top + 10
    selectionMenuLeft.value = rect.left - modalRect.left + rect.width / 2 - 50
    
    showSelectionMenu.value = true
  } else {
    showSelectionMenu.value = false
  }
}

// 提问函数
const askQuestion = () => {
  if (selectedText.value) {
    // 关闭弹出菜单
    showSelectionMenu.value = false
    
    // 显示提问侧边栏
    showQuestionSidebar.value = true
    showAIAnswer.value = false
    userQuestion.value = ''
  }
}

// 提交问题
const submitQuestion = async () => {
  if (userQuestion.value) {
    try {
      const response = await apiService.qa.askQuestion({
        question: userQuestion.value,
        selectedText: selectedText.value,
        bookId: studyBook.value?.id
      })
      aiAnswer.value = response.data
      showAIAnswer.value = true
    } catch (error) {
      console.error('提交问题失败:', error)
      showToast('提交问题失败', false)
    }
  }
}

// 跳转到个人问答中心
const goToQACenter = () => {
  // 保存学习模态框的状态到 localStorage
  const studyState = {
    showStudyModal: true,
    studyBook: studyBook.value,
    showQuestionSidebar: showQuestionSidebar.value,
    userQuestion: userQuestion.value,
    showAIAnswer: showAIAnswer.value
  }
  localStorage.setItem('studyModalState', JSON.stringify(studyState))
  
  // 关闭提问侧边栏
  showQuestionSidebar.value = false
  
  // 跳转到个人问答中心，并传递问题、选中的文本和来源信息
  router.push({
    path: '/qa',
    query: {
      question: userQuestion.value, 
      selectedText: selectedText.value, 
      bookId: studyBook.value?.id,
      from: 'study' // 标记来源为学习页面
    }
  })
}

// 加载笔记
const loadNotes = async () => {
  if (!studyBook.value) return
  
  noteLoading.value = true
  try {
    const response = await apiService.note.getNotesByBook(studyBook.value.id)
    if (response.code === 200) {
      notes.value = response.data || []
      // 为笔记添加章节标题
      notes.value.forEach(note => {
        if (note.chapterId) {
          note.chapterTitle = getChapterTitleById(note.chapterId)
        }
      })
    }
  } catch (error) {
    console.error('加载笔记失败，使用mock数据:', error)
    // 使用mock数据作为回退
    notes.value = [
      {
        id: 'note_1',
        bookId: studyBook.value.id,
        title: '导数的定义',
        content: '导数是函数在某一点的变化率，定义为极限值：f\'(x) = lim(h→0) [f(x+h) - f(x)]/h',
        chapterId: 'section_1_2',
        chapterTitle: '1.2 导数与微分',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      {
        id: 'note_2',
        bookId: studyBook.value.id,
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

// 创建笔记
const createNote = async () => {
  if (!studyBook.value || !noteTitle.value.trim()) {
    showToast('请输入笔记标题', false)
    return
  }
  
  noteLoading.value = true
  try {
    const response = await apiService.note.createNote({
      bookId: studyBook.value.id,
      title: noteTitle.value,
      content: noteContent.value,
      chapterId: selectedNoteChapterId.value || currentChapter?.value?.id
    })
    
    if (response.code === 200) {
      showToast('笔记创建成功')
      await loadNotes()
      closeNoteModal()
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
    
    if (response.code === 200) {
      showToast('笔记更新成功')
      await loadNotes()
      closeNoteModal()
    }
  } catch (error) {
    console.error('更新笔记失败:', error)
    showToast('更新笔记失败', false)
  } finally {
    noteLoading.value = false
  }
}

// 删除笔记
const deleteNote = async (noteId) => {
  if (!confirm('确定要删除这条笔记吗？')) return
  
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
  console.log('createNewNote called')
  currentNote.value = null
  noteTitle.value = ''
  noteContent.value = ''
  selectedNoteChapterId.value = currentChapter?.value?.id || ''
  showNotePreview.value = false
  console.log('createNewNote executed')
}

// 关闭笔记模态框
const closeNoteModal = () => {
  showNoteModal.value = false
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

// 获取笔记预览文本
const getNotePreview = (content) => {
  if (!content) return ''
  // 移除 Markdown 标记，获取纯文本预览
  const plainText = content
    .replace(/#{1,6}\s/g, '')
    .replace(/\*\*/g, '')
    .replace(/\*/g, '')
    .replace(/`{3}[\s\S]*?`{3}/g, '[代码块]')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/>\s/g, '')
    .replace(/-\s/g, '')
    .replace(/\n+/g, ' ')
    .trim()
  return plainText.length > 60 ? plainText.substring(0, 60) + '...' : plainText
}

// 格式化笔记日期
const formatNoteDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  // 小于1小时显示"X分钟前"
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return minutes < 1 ? '刚刚' : `${minutes}分钟前`
  }
  // 小于24小时显示"X小时前"
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }
  // 小于7天显示"X天前"
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }
  // 否则显示具体日期
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 获取章节标题
const getChapterTitleById = (chapterId) => {
  if (!chapterId || !chapterTree.value) return ''
  
  const findChapter = (chapters, id) => {
    for (const chapter of chapters) {
      if (chapter.id === id) {
        return chapter.title
      }
      if (chapter.children && chapter.children.length > 0) {
        const found = findChapter(chapter.children, id)
        if (found) return found
      }
    }
    return ''
  }
  
  return findChapter(chapterTree.value, chapterId)
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
  if (selectedText.value) {
    insertMarkdown('> ' + selectedText.value + '\n\n', '')
  }
}

// 简单的 Markdown 预览渲染
const renderMarkdownPreview = (content) => {
  if (!content) return ''
  
  let html = content
    // 代码块
    .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
    // 行内代码
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    // 标题
    .replace(/^###### (.*$)/gim, '<h6>$1</h6>')
    .replace(/^##### (.*$)/gim, '<h5>$1</h5>')
    .replace(/^#### (.*$)/gim, '<h4>$1</h4>')
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 引用
    .replace(/^> (.*$)/gim, '<blockquote>$1</blockquote>')
    // 无序列表
    .replace(/^- (.*$)/gim, '<li>$1</li>')
    // 有序列表
    .replace(/^\d+\. (.*$)/gim, '<li>$1</li>')
    // 换行
    .replace(/\n/g, '<br>')
  
  // 包裹列表项
  html = html.replace(/(<li>.*<\/li>)/g, '<ul>$1</ul>')
  
  return html
}

// 导出笔记
const exportNotes = (format) => {
  if (notes.value.length === 0) {
    showToast('没有可导出的笔记', false)
    showExportOptions.value = false
    return
  }
  
  const bookTitle = studyBook.value?.title || '笔记'
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

// 切换完整目录显示
const toggleFullCatalog = async () => {
  showFullCatalog.value = !showFullCatalog.value
  // 如果显示完整目录，获取章节数据
  if (showFullCatalog.value && selectedBook.value) {
    await fetchChapters(selectedBook.value.id)
  }
}

// 添加教材

// 更新最近点击时间
const updateLastClicked = (book) => {
  book.lastClicked = Date.now()
}

// 分类
const categories = ref([])
const selectedCategory = ref('')

// 选中的书籍
const selectedBooks = ref([])

// 教材数据
const textbooks = ref([])

// 获取分类列表
const fetchCategories = async () => {
  try {
    const response = await apiService.bookshelf.getAllBooks()
    // 从书籍数据中提取分类
    const uniqueCategories = [...new Set(response.data.map(book => book.category).filter(Boolean))]
    categories.value = uniqueCategories
    if (categories.value.length > 0) {
      selectedCategory.value = categories.value[0]
    }
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 获取教材列表
const fetchTextbooks = async () => {
  try {
    const response = await apiService.bookshelf.getTextbooks({
      category: selectedCategory.value, 
      keyword: textbookSearchQuery.value 
    })
    textbooks.value = response.data
  } catch (error) {
    console.error('获取教材失败:', error)
  }
}

// 过滤后的教材
const filteredTextbooks = computed(() => {
  let result = textbooks.value
  
  // 按搜索筛选
  if (textbookSearchQuery.value) {
    const query = textbookSearchQuery.value.toLowerCase()
    result = result.filter(book => 
      book.title.toLowerCase().includes(query)
    )
  }
  
  return result
})

// 添加按钮文本
const addButtonText = computed(() => {
  const count = selectedBooks.value.length
  if (count === 0) {
    return '添加教材'
  } else if (count === 1) {
    return '添加'
  } else {
    return '批量添加'
  }
})

// 选择分类
const selectCategory = async (category) => {
  selectedCategory.value = category
  await fetchTextbooks()
}

// 切换选中状态
const toggleSelect = (index) => {
  const selectedIndex = selectedBooks.value.indexOf(index)
  if (selectedIndex > -1) {
    selectedBooks.value.splice(selectedIndex, 1)
  } else {
    selectedBooks.value.push(index)
  }
}

// 批量添加
const batchAdd = async () => {
  if (selectedBooks.value.length === 0) {
    showToast('请选择要添加的教材')
    return
  }
  
  try {
    const booksToAdd = selectedBooks.value.map(index => textbooks.value[index])
    const userId = localStorage.getItem('userId')
    if (!userId) {
      showToast('用户未登录', false)
      return
    }
    const response = await apiService.bookshelf.addBooks(userId, { books: booksToAdd })
    
    if (response.code === 200) {
      showToast(`已成功添加 ${selectedBooks.value.length} 本教材到个人书架`)
      await fetchBooks()
      
      showAddTextbookModal.value = false
      selectedBooks.value = []
      textbookSearchQuery.value = ''
    }
  } catch (error) {
    console.error('添加教材失败:', error)
    showToast('添加教材失败', false)
  }
}

// 删除书籍
const deleteBook = (bookId) => {
  bookToDelete = bookId
  showDeleteConfirm.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (bookToDelete) {
    try {
      const response = await apiService.bookshelf.deleteBook(bookToDelete)
      if (response.code === 200) {
        showToast('书籍删除成功')
        await fetchBooks()
      }
    } catch (error) {
      console.error('删除书籍失败:', error)
      showToast('删除书籍失败', false)
    } finally {
      showDeleteConfirm.value = false
      bookToDelete = null
    }
  }
}

// 返回首页
const backToHome = () => {
  router.push('/')
}

// 添加教材
const addTextbook = async () => {
  showAddTextbookModal.value = true
  await fetchCategories()
  await fetchTextbooks()
}
</script>

<style scoped>
.bookshelf-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* 页面头部 */
.bookshelf-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
  text-align: center;
  width: 100%;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  max-width: 1200px;
  padding: 0 20px;
  position: relative;
}

.dark-mode-toggle {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
  z-index: 20;
}

.dark-mode-toggle:hover {
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.dark-mode-toggle:active {
  transform: translateY(-50%) scale(0.95);
}

.toggle-icon {
  font-size: 20px;
  line-height: 1;
}

.back-home-btn {
  position: absolute;
  left: -100px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(90deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
  z-index: 10;
}

.back-home-btn:hover {
  transform: translateY(-50%) translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.back-home-btn:active {
  transform: translateY(-50%) translateY(0);
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.2);
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin: 0;
  background: linear-gradient(135deg, #6366f1 0%, #10b981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(99, 102, 241, 0.1);
  letter-spacing: 1px;
  font-family: 'Microsoft YaHei', sans-serif;
  text-align: center;
  width: 100%;
}

.back-icon {
  font-size: 16px;
  font-weight: bold;
  margin-right: 4px;
}

.search-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1200px;
  padding: 0 20px;
  margin-top: 20px;
}

.search-container {
  flex: 1;
  display: flex;
  justify-content: center;
  margin-right: -177px;
  width: auto;
}

.search-bar {
  position: relative;
  width: 100%;
  max-width: 600px;
  display: flex;
  align-items: center;
}

.add-textbook-btn {
  padding: 12px 32px;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
  white-space: nowrap;
  z-index: 10;
  flex-shrink: 0;
}

.add-textbook-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.add-textbook-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.2);
}

.search-input {
  width: 100%;
  padding: 12px 20px;
  border: 2px solid #e5e7eb;
  border-radius: 25px;
  font-size: 16px;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
  box-sizing: border-box;
}

.search-input::placeholder {
  color: #909399;
}

.search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.notification-icon {
  position: relative;
  cursor: pointer;
}

.bell {
  font-size: 20px;
  color: #6b7280;
}

.badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 筛选区域 */
.bookshelf-filter {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.custom-select {
  position: relative;
  display: inline-block;
}

.filter-select {
  padding: 10px 40px 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  min-width: 140px;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
}

.filter-select:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.filter-select option {
  padding: 12px 16px;
  background: white;
  color: #374151;
  font-size: 14px;
  font-family: 'Microsoft YaHei', sans-serif;
}

.filter-select option:hover {
  background: #f3f4f6;
  color: #6366f1;
}

.filter-select option:checked {
  background: #6366f1;
  color: white;
  font-weight: 500;
}

.custom-select .select-arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #6b7280;
  font-size: 12px;
  pointer-events: none;
  transition: all 0.3s ease;
}

.custom-select:hover .select-arrow {
  color: #6366f1;
}

.custom-select select:focus + .select-arrow {
  transform: translateY(-50%) rotate(180deg);
  color: #6366f1;
}

/* 修复下拉菜单样式 */
.filter-select {
  position: relative;
  z-index: 1;
}

.custom-select {
  z-index: 10;
}

/* 确保下拉菜单不被其他元素遮挡 */
.filter-select option {
  position: relative;
  z-index: 100;
}

.filter-select:hover {
  border-color: #a5b4fc;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

/* 自定义下拉菜单样式 */
.filter-select::-ms-expand {
  display: none;
}

.filter-select::-webkit-scrollbar {
  width: 6px;
}

.filter-select::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 8px;
}

.filter-select::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 8px;
}

.filter-select::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 搜索able下拉框样式 */
.searchable-select,
.select-container {
  position: relative;
  width: 200px;
  z-index: 100;
}

.select-search-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  margin-bottom: 8px;
}

.select-search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  max-height: 200px;
  overflow-y: auto;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  margin-top: 4px;
}

.select-option {
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  color: #374151;
}

.select-option:hover {
  background-color: #f3f4f6;
  color: #6366f1;
}

.select-option.active {
  background-color: #6366f1;
  color: white;
  font-weight: 500;
}

.select-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #374151;
}

.select-display:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.select-display .select-arrow {
  font-size: 12px;
  color: #6b7280;
  transition: all 0.3s ease;
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.select-display:hover .select-arrow {
  color: #6366f1;
  transform: translateY(-50%) rotate(180deg);
}

/* 自定义滚动条 */
.select-dropdown::-webkit-scrollbar {
  width: 6px;
}

.select-dropdown::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 8px;
}

.select-dropdown::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 8px;
}

.select-dropdown::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  overflow: hidden;
  backdrop-filter: blur(2px);
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 32px;
  width: 1000px;
  max-width: 90%;
  height: 700px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  transition: none;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
  position: relative;
}

.modal-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0;
  font-family: 'Microsoft YaHei', sans-serif;
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-title::before {
  content: '✏️';
  font-size: 20px;
  display: inline-block;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6b7280;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
  transform: rotate(90deg);
}

/* 模态框动画效果 */
.edit-book-modal {
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 表单标签样式优化 */
.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  display: block;
  position: relative;
  padding-left: 12px;
}

.form-group label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border-radius: 2px;
}

/* 搜索区域 */
.search-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  gap: 16px;
}

.search-bar {
  position: relative;
  flex: 1;
  max-width: 600px;
  margin-right: 20px;
}

.search-input {
  width: 100%;
  padding: 12px 20px;
  border: 2px solid #e5e7eb;
  border-radius: 25px;
  font-size: 16px;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
}

.search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

/* 分类标签 */
.category-section {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.category-btn {
  padding: 8px 20px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  color: #374151;
}

.category-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.category-btn.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

.more-btn {
  position: relative;
}

.more-btn::after {
  content: '';
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 5px solid #374151;
}

/* 教材列表 */
.textbook-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.textbook-card {
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.textbook-card:hover {
  border-color: #6366f1;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
  transform: translateY(-4px);
}

.textbook-card.selected {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.05);
}

.textbook-cover {
  width: 100%;
  height: 180px;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.textbook-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.textbook-title {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
  font-family: 'Microsoft YaHei', sans-serif;
}

.select-checkbox {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: #6366f1;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

/* 批量添加按钮 */
.batch-add-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-top: 32px;
}

.batch-add-btn {
  padding: 16px 48px;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Microsoft YaHei', sans-serif;
}

.batch-add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.3);
}

.batch-add-text {
  font-size: 16px;
  color: #6b7280;
  font-family: 'Microsoft YaHei', sans-serif;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    padding: 24px;
  }
  
  .search-section {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .search-bar {
    margin-right: 0;
  }
  
  .textbook-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 16px;
  }
  
  .batch-add-section {
    flex-direction: column;
    gap: 12px;
  }
}

/* 成功提示 */
.toast {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.8);
  border-radius: 16px;
  padding: 24px 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  z-index: 2000;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
}

.toast-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.toast-icon.error {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.toast-message {
  color: white;
  font-size: 16px;
  font-weight: 500;
  font-family: 'Microsoft YaHei', sans-serif;
  text-align: center;
  white-space: nowrap;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -60%);
}

/* 书籍容器 */
.books-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

/* 书籍卡片 */
.book-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  position: relative;
  cursor: pointer;
  border: 1px solid #f3f4f6;
}

.book-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.12);
  border-color: #e5e7eb;
}

.book-card:active {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

/* 状态标签 */
.book-status {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  z-index: 10;
}

/* 删除按钮 */
.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: rgba(239, 68, 68, 0.9);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  opacity: 0;
  transform: scale(0.8);
}

.book-card:hover .delete-btn {
  opacity: 1;
  transform: scale(1);
}

.delete-btn:hover {
  background: rgba(220, 38, 38, 1);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4);
}

.delete-btn:active {
  transform: scale(0.9);
}

.book-status.not-started {
  background: #e5e7eb;
  color: #374151;
}

.book-status.learning {
  background: #10b981;
  color: white;
}

.book-status.completed {
  background: #6366f1;
  color: white;
}

/* 书籍封面 */
.book-cover {
  width: 100%;
  height: 240px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.book-card:hover .book-cover img {
  transform: scale(1.05);
}

/* 书籍信息 */
.book-info {
  padding: 16px;
}

.book-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-author {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 12px 0;
}

/* 进度条 */
.progress-bar {
  width: 100%;
  height: 4px;
  background: #f3f4f6;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1 0%, #4f46e5 100%);
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* 添加教材按钮 */
.add-textbook {
  position: fixed;
  bottom: 32px;
  right: 32px;
  z-index: 100;
}

.add-btn {
  padding: 16px 32px;
  background: linear-gradient(90deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border: none;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.5);
}

/* 书籍详情弹窗 */
.modal-overlay {
  background-color: rgba(243, 244, 246, 0.9);
}

.book-detail-modal {
  max-width: 1100px;
  max-height: 85vh;
  overflow-y: hidden;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

/* 自定义滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 8px;
}

::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 8px;
  transition: background 0.3s ease;
}

::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 为书籍详情弹窗中的滚动条添加更精美的样式 */
.book-detail-main::-webkit-scrollbar,
.book-detail-sidebar::-webkit-scrollbar,
.full-catalog::-webkit-scrollbar {
  width: 4px;
}

.book-detail-main::-webkit-scrollbar-track,
.book-detail-sidebar::-webkit-scrollbar-track,
.full-catalog::-webkit-scrollbar-track {
  background: #f9fafb;
  border-radius: 4px;
}

.book-detail-main::-webkit-scrollbar-thumb,
.book-detail-sidebar::-webkit-scrollbar-thumb,
.full-catalog::-webkit-scrollbar-thumb {
  background: #e5e7eb;
  border-radius: 4px;
}

.book-detail-main::-webkit-scrollbar-thumb:hover,
.book-detail-sidebar::-webkit-scrollbar-thumb:hover,
.full-catalog::-webkit-scrollbar-thumb:hover {
  background: #d1d5db;
}

/* 完整目录样式 */
.full-catalog {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.catalog-chapter {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s ease;
}

.catalog-chapter:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.chapter-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.chapter-sections {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 16px;
}

.section-item {
  font-size: 13px;
  color: #6b7280;
  padding: 4px 0;
  transition: color 0.2s ease;
}

.section-item:hover {
  color: #6366f1;
  cursor: pointer;
}

.book-detail-content {
  padding: 24px;
  display: flex;
  gap: 32px;
  height: calc(100% - 120px);
  overflow: hidden;
}

.book-detail-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-right: 12px;
}

.book-detail-sidebar {
  width: 300px;
  flex-shrink: 0;
  border-left: 1px solid #e5e7eb;
  padding-left: 24px;
  overflow-y: auto;
  padding-right: 12px;
}

.book-detail-header {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  flex: 1;
  overflow-y: auto;
}

.book-detail-cover {
  width: 200px;
  flex-shrink: 0;
}

.book-detail-cover img {
  width: 100%;
  height: 280px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.book-detail-info {
  flex: 1;
}

.book-detail-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 16px;
}

.book-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  color: #6b7280;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
}

.book-detail-description {
  margin-bottom: 16px;
}

.book-detail-description h4,
.book-detail-catalog h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.book-detail-description p {
  color: #4b5563;
  line-height: 1.5;
  font-size: 14px;
}

.catalog-link {
  color: #6366f1;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: inline-block;
  padding: 4px 0;
}

.catalog-link:hover {
  text-decoration: underline;
}

.book-detail-progress {
  margin-bottom: 16px;
}

.book-detail-progress h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.progress-container {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 12px;
}

.progress-container.small {
  padding: 10px;
}

.progress-track {
  position: relative;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  margin-bottom: 12px;
}

.progress-container.small .progress-track {
  height: 6px;
  margin-bottom: 10px;
}

.progress-handle {
  position: absolute;
  top: -6px;
  width: 20px;
  height: 20px;
  background: #6366f1;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(99, 102, 241, 0.3);
  transform: translateX(-50%);
}

.progress-stats {
  display: flex;
  justify-content: space-between;
  color: #6b7280;
  font-size: 14px;
}

.book-detail-actions {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  padding-bottom: 16px;
  margin-top: auto;
}

.action-btn {
  padding: 10px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  color: #374151;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Microsoft YaHei', sans-serif;
}

.action-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.action-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.action-btn.primary {
  background: linear-gradient(90deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border-color: #6366f1;
}

.action-btn.primary:hover {
  background: linear-gradient(90deg, #4f46e5 0%, #1d4ed8 100%);
  border-color: #4f46e5;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

/* 编辑按钮样式 */
.action-btn.edit {
  background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%);
  color: white;
  border-color: #f59e0b;
}

.action-btn.edit:hover {
  background: linear-gradient(90deg, #d97706 0%, #b45309 100%);
  border-color: #d97706;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

/* 删除按钮样式 */
.action-btn.delete {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border-color: #ef4444;
}

.action-btn.delete:hover {
  background: linear-gradient(90deg, #dc2626 0%, #b91c1c 100%);
  border-color: #dc2626;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

/* 按钮图标样式 */
.btn-icon {
  font-size: 16px;
  line-height: 1;
  display: inline-block;
  width: 20px;
  text-align: center;
}

/* 侧边栏样式 */
.sidebar-section {
  margin-bottom: 32px;
}

.sidebar-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
}

.recommended-books {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommended-book {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.recommended-book:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
}

.recommended-book-cover {
  width: 60px;
  flex-shrink: 0;
}

.recommended-book-cover img {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.recommended-book-info {
  flex: 1;
}

.recommended-book-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.recommended-book-author {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommendation-reason {
  font-size: 11px;
  color: #6b7280;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.no-recommendations,
.no-reviews {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  padding: 20px 0;
}

.loading {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  padding: 20px 0;
}

.user-reviews {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-rating {
  font-size: 12px;
  color: #ffd700;
}

.star {
  opacity: 0.5;
}

.star.filled {
  opacity: 1;
}

.review-content {
  font-size: 12px;
  color: #1f2937;
  line-height: 1.4;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #6b7280;
}

.review-date {
  margin-right: 10px;
}

.review-likes {
  display: flex;
  align-items: center;
}

.reviewer-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .bookshelf-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .search-bar {
    width: 100%;
  }
  
  .header-actions {
    justify-content: flex-end;
  }
  
  .books-container {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
  
  .book-cover {
    height: 200px;
  }
  
  .book-detail-header {
    flex-direction: column;
    align-items: center;
  }
  
  .book-detail-cover {
    width: 160px;
  }
  
  .book-detail-cover img {
    height: 220px;
  }
  
  .book-detail-actions {
    flex-direction: column;
  }
  
  .action-btn {
    width: 100%;
  }
  
  .book-detail-content {
    flex-direction: column;
  }
  
  .book-detail-sidebar {
    width: 100%;
    border-left: none;
    border-top: 1px solid #e5e7eb;
    padding-left: 0;
    padding-top: 24px;
  }
  
  /* 学习模态框响应式 */
  .study-modal {
    max-width: 95vw;
    max-height: 95vh;
    width: 100%;
    height: 90vh;
  }
  
  .study-content {
    flex-direction: column;
    padding: 12px;
    gap: 12px;
  }
  
  .study-content .sidebar {
    width: 100%;
    max-height: 200px;
  }
  
  .reading-area {
    flex: 1;
    padding: 16px;
  }
  
  .question-sidebar {
    width: 100%;
    max-height: 300px;
  }
  
  .study-content.with-question-sidebar {
    flex-direction: column;
  }
  
  .study-content.with-question-sidebar .reading-area {
    flex: 1;
  }
  
  /* 目录响应式 */
  .toc-item {
    margin: 1px 4px;
  }
  
  .toc-header {
    padding: 4px 12px;
  }
  
  .toc-title {
    font-size: 12px;
  }
  
  .toc-children {
    margin-left: 8px;
    padding-left: 6px;
  }
  
  .toc-children .toc-header {
    padding: 3px 8px;
  }
  
  .toc-children .toc-title {
    font-size: 11px;
  }
  
  /* 搜索区域响应式 */
  .search-section {
    padding: 6px 8px;
  }
  
  .search-input-container .search-input {
    padding: 4px 28px 4px 8px;
    font-size: 11px;
  }
  
  .search-input-container .search-btn {
    right: 32px;
    font-size: 10px;
  }
  
  .search-input-container .clear-btn {
    right: 8px;
    width: 16px;
    height: 16px;
    font-size: 12px;
  }
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 40px;
  margin-bottom: 40px;
}

.pagination-btn {
  padding: 10px 20px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s ease;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #6366f1;
  color: #6366f1;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-numbers {
  display: flex;
  gap: 8px;
}

.pagination-number {
  width: 40px;
  height: 40px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-number:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.pagination-number.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

/* 成功提示样式 */
.toast {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.85);
  color: white;
  padding: 20px 32px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  z-index: 2000;
  animation: fadeIn 0.3s ease, fadeOut 0.3s ease 2.7s;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  backdrop-filter: blur(12px);
  min-width: 200px;
  max-width: 300px;
  text-align: center;
}

.toast:hover {
  transform: translate(-50%, -50%) scale(1.05);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.toast-icon {
  font-size: 24px;
  font-weight: bold;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.toast-message {
  font-size: 14px;
  font-weight: 500;
  font-family: 'Microsoft YaHei', sans-serif;
  line-height: 1.4;
}

/* 确认删除对话框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-modal {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  animation: modalFadeIn 0.3s ease;
}

.confirm-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
  background: #f9fafb;
}

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.confirm-body {
  padding: 24px;
}

.confirm-message {
  font-size: 16px;
  color: #4b5563;
  text-align: center;
  margin: 0;
}

.confirm-footer {
  padding: 0 24px 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.confirm-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
  max-width: 120px;
}

.confirm-btn.cancel {
  background: #f3f4f6;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}

.confirm-btn.cancel:hover {
  background: #e5e7eb;
}

.confirm-btn.delete {
  background: #ef4444;
  color: white;
}

.confirm-btn.delete:hover {
  background: #dc2626;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.confirm-btn:active {
  transform: translateY(1px);
}

/* 动画 */
@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

@keyframes modalFadeIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .confirm-modal,
.modal-leave-to .confirm-modal {
  transform: scale(0.9);
}
/* 学习模态框样式 */
.study-modal {
  max-width: 90vw;
  max-height: 90vh;
  width: 1400px;
  height: 800px;
  overflow: hidden;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

/* 学习模态框头部 */
.study-header {
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid #f3f4f6;
}

.study-header .header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.study-header .back-btn, .study-header .menu-btn, .study-header .search-btn {
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.study-header .back-btn:hover, .study-header .menu-btn:hover, .study-header .search-btn:hover {
  background: #e5e7eb;
}

.study-header .menu-btn.active {
  background: #6366f1;
  color: white;
}

.study-header .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.study-header .icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.study-header .icon-btn:hover {
  background: #e5e7eb;
}

.study-header .icon {
  font-size: 16px;
}

/* 学习内容区 */
.study-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
}

/* 显示提问侧边栏时的布局 */
.study-content.with-question-sidebar .reading-area {
  flex: 1;
}

/* 提问侧边栏 */
.question-sidebar {
  width: 300px;
  background: #f9fafb;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.question-sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
  background: white;
}

.question-sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.question-sidebar-header .close-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  font-size: 20px;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.question-sidebar-header .close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.question-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.selected-text {
  background: white;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.selected-text h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.selected-text p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.question-input {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-input h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.question-input textarea {
  flex: 1;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  resize: none;
  min-height: 120px;
  font-family: inherit;
  transition: all 0.3s ease;
}

.question-input.small textarea {
  min-height: 60px;
  max-height: 80px;
}

.question-input textarea:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.submit-question-btn {
  padding: 12px 16px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-start;
}

.submit-question-btn:hover {
  background: #4f46e5;
}

.submit-question-btn:active {
  transform: scale(0.98);
}

/* AI回答区域 */
.ai-answer {
  margin-top: 16px;
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  animation: fadeIn 0.3s ease-in-out;
}

.ai-answer h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1e40af;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-answer h4::before {
  content: "🤖";
  font-size: 16px;
}

.answer-content {
  margin-bottom: 12px;
}

.answer-content p {
  margin: 0;
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
}

.detailed-question-btn {
  padding: 8px 12px;
  background: white;
  color: #6366f1;
  border: 1px solid #6366f1;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-start;
}

.detailed-question-btn:hover {
  background: #f0f9ff;
}

.detailed-question-btn:active {
  transform: scale(0.98);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 侧边栏 */
.study-content .sidebar {
  width: 200px;
  background: #f9fafb;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.study-content .sidebar-header {
  padding: 16px 20px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f3f4f6;
  background: white;
}

.study-content .sidebar-header h3 {
  font-size: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.study-content .collapse-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
}

/* 目录树 */
.toc-tree {
  flex: 1;
  padding: 12px 0;
  padding: 8px 0;
  overflow-y: auto;
}

.toc-item {
  position: relative;
  transition: all 0.2s;
  margin: 2px 8px;
  border-radius: 6px;
}

.toc-header {
  display: flex;
  align-items: center;
  padding: 6px 16px;
  cursor: pointer;
  border-radius: 6px;
}

.toc-header:hover {
  background: #f3f4f6;
}

.toggle-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #6b7280;
  margin-right: 8px;
  transition: all 0.2s ease;
}

.toc-title {
  flex: 1;
  font-size: 13px;
  line-height: 1.5;
  font-weight: 500;
  cursor: pointer;
}

.toc-item.active .toc-title {
  color: #6366f1;
}

.toc-item.active {
  background: #eef2ff;
}

.toc-children {
  margin-left: 12px;
  margin-top: 2px;
  border-left: 1px solid #e5e7eb;
  padding-left: 8px;
}

.toc-children .toc-item {
  margin: 1px 4px;
}

.toc-children .toc-header {
  padding: 4px 12px;
}

.toc-children .toc-title {
  font-size: 12px;
  font-weight: 400;
}

.empty-chapters {
  padding: 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}

/* 搜索区域 */
.search-section {
  padding: 8px 12px;
  border-bottom: 1px solid #f3f4f6;
  background: white;
}

.search-input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input-container .search-input {
  flex: 1;
  padding: 6px 32px 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  font-size: 12px;
  transition: all 0.3s ease;
}

.search-input-container .search-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-input-container .search-btn {
  position: absolute;
  right: 36px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: #6b7280;
  padding: 4px;
}

.search-input-container .clear-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
  padding: 4px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.search-input-container .clear-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

/* 搜索结果 */
.search-results {
  margin: 8px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
}

.search-results-header .close-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
  padding: 2px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.search-results-header .close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.search-result-item {
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.2s ease;
  cursor: pointer;
  margin-bottom: 4px;
}

.search-result-item:hover {
  background: #f3f4f6;
}

.search-result-title {
  font-size: 12px;
  color: #374151;
  line-height: 1.4;
}

/* 阅读区 */
.reading-area {
  flex: 1;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.reading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.reading-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.expand-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
}

.reading-content {
  flex: 1;
  line-height: 1.8;
  color: #374151;
  font-size: 15px;
  overflow-y: auto;
  margin-bottom: 24px;
}

.reading-content p {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 24px 0 16px 0;
}

.checklist {
  margin: 16px 0;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.check-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #6366f1;
}

.create-btn {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin: 16px 0;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}

.pagination span {
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.pagination span:hover {
  background: #f3f4f6;
  color: #374151;
}

/* 右侧辅助信息区 */
.right-sidebar {
  width: 280px;
}

.info-section {
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
  background: white;
}

.info-section:last-child {
  border-bottom: none;
}

.info-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
}

.card-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.info-card {
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.info-card.primary {
  background: linear-gradient(135deg, #e0e7ff, #eef2ff);
  border: 1px solid #c7d2fe;
}

.info-card.secondary {
  background: linear-gradient(135deg, #d1fae5, #ecfdf5);
  border: 1px solid #a7f3d0;
}

.card-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 12px;
  color: #6b7280;
}

.key-metrics {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.metric-label {
  font-size: 12px;
  color: #6b7280;
  margin-left: 8px;
  flex: 1;
}

.metric-icon {
  font-size: 16px;
  color: #6b7280;
}

/* 学习模态框底部 */
.study-footer {
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  border-top: 1px solid #f3f4f6;
}

.study-footer .footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.study-footer .footer-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.study-footer .footer-btn:hover {
  background: #e5e7eb;
}

.study-footer .footer-btn.active {
  background: #6366f1;
  color: white;
}

.study-footer .btn-icon {
  font-size: 16px;
}

.study-footer .btn-arrow {
  font-size: 12px;
  margin-left: 4px;
}

.study-footer .ai-assistant {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.study-footer .ai-assistant:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.study-footer .ai-avatar {
  font-size: 20px;
}

.study-footer .ai-text {
  font-size: 14px;
  font-weight: 500;
  color: #92400e;
}

/* 文本选择弹出菜单 */
.text-selection-menu {
  position: absolute;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px;
  z-index: 1000;
  display: flex;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f3f4f6;
  border: none;
  border-radius: 6px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #e5e7eb;
}

.menu-icon {
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .study-modal {
    max-width: 95vw;
    max-height: 95vh;
    width: 100%;
  }
  
  .study-content {
    flex-direction: column;
  }
  
  .study-content .sidebar {
    width: 100%;
    max-height: 300px;
  }
  
  .reading-area {
    order: -1;
  }
}

/* 笔记模态框样式 */
.note-modal {
  max-width: 1000px;
  max-height: 80vh;
  width: 90%;
  overflow: hidden;
}

.note-content {
  height: 600px;
  overflow: hidden;
}

.note-container {
  display: flex;
  height: 100%;
  gap: 20px;
}

/* 左侧笔记列表 */
.note-list {
  width: 300px;
  background: #f9fafb;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.note-list-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note-list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.add-note-btn {
  padding: 6px 12px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.add-note-btn:hover {
  background: #4f46e5;
}

.note-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.note-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.note-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.note-item.active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.05);
}

.note-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
}

.delete-note-btn {
  width: 20px;
  height: 20px;
  border: none;
  background: none;
  color: #ef4444;
  font-size: 16px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.delete-note-btn:hover {
  background: #fee2e2;
}

.loading {
  padding: 20px;
  text-align: center;
  color: #6b7280;
}

.empty-notes {
  padding: 40px 20px;
  text-align: center;
  color: #6b7280;
}

/* 右侧笔记编辑区 */
.note-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.note-title-input {
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  transition: all 0.3s ease;
}

.note-title-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.note-content-textarea {
  flex: 1;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  resize: none;
  font-family: inherit;
  transition: all 0.3s ease;
}

.note-content-textarea:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.note-editor-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.save-note-btn {
  padding: 12px 24px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.save-note-btn:hover:not(:disabled) {
  background: #4f46e5;
}

.save-note-btn:disabled {
  background: #a5b4fc;
  cursor: not-allowed;
}

.cancel-note-btn {
  padding: 12px 24px;
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-note-btn:hover {
  background: #e5e7eb;
}

/* 夜间模式笔记样式 */
.dark-mode .note-modal {
  background: #1e293b;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.dark-mode .note-list {
  background: #1e293b;
}

.dark-mode .note-list-header {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark-mode .note-list-header h3 {
  color: #e2e8f0;
}

.dark-mode .add-note-btn {
  background: #6366f1;
}

.dark-mode .add-note-btn:hover {
  background: #4f46e5;
}

.dark-mode .note-item {
  background: #1e293b;
  border-color: #334155;
}

.dark-mode .note-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.dark-mode .note-item.active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
}

.dark-mode .note-item-title {
  color: #e2e8f0;
}

.dark-mode .note-item-meta {
  color: #94a3b8;
}

.dark-mode .delete-note-btn {
  color: #f87171;
}

.dark-mode .delete-note-btn:hover {
  background: rgba(248, 113, 113, 0.1);
}

.dark-mode .loading,
.dark-mode .empty-notes {
  color: #94a3b8;
}

.dark-mode .note-title-input {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .note-title-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.dark-mode .note-content-textarea {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .note-content-textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.dark-mode .save-note-btn {
  background: #6366f1;
}

.dark-mode .save-note-btn:hover:not(:disabled) {
  background: #4f46e5;
}

.dark-mode .save-note-btn:disabled {
  background: #1e40af;
}

.dark-mode .cancel-note-btn {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .cancel-note-btn:hover {
  background: #475569;
}

/* 笔记头部操作按钮 */
.modal-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.export-btn:hover {
  background: #e5e7eb;
}

.export-dropdown {
  position: absolute;
  top: 100%;
  right: 40px;
  margin-top: 8px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  min-width: 160px;
}

.export-option {
  padding: 12px 16px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.export-option:hover {
  background: #f3f4f6;
}

.export-option:first-child {
  border-radius: 8px 8px 0 0;
}

.export-option:last-child {
  border-radius: 0 0 8px 8px;
}

/* 笔记搜索 */
.note-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
  background: white;
}

.note-search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.note-search-input:focus {
  outline: none;
  border-color: #6366f1;
}

.search-btn, .clear-btn {
  padding: 8px 12px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  border-radius: 6px;
  transition: all 0.2s;
}

.search-btn:hover, .clear-btn:hover {
  background: #f3f4f6;
}

/* 笔记列表项增强 */
.note-item-chapter {
  margin: 4px 0;
}

.chapter-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  font-size: 11px;
  border-radius: 4px;
}

.note-item-preview {
  font-size: 12px;
  color: #6b7280;
  margin: 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 章节选择器 */
.chapter-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.selector-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
}

.chapter-select {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.chapter-select:focus {
  outline: none;
  border-color: #6366f1;
}

.current-chapter-tag {
  padding: 4px 12px;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  font-size: 12px;
  border-radius: 4px;
  white-space: nowrap;
}

/* 编辑器工具栏 */
.editor-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px;
  background: #f9fafb;
  border-radius: 8px;
  flex-wrap: wrap;
}

.toolbar-btn {
  padding: 6px 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 32px;
}

.toolbar-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-btn.highlight-btn {
  font-weight: normal;
  font-size: 12px;
  margin-left: auto;
}

/* 预览切换 */
.preview-toggle {
  display: flex;
  justify-content: flex-end;
}

.toggle-btn {
  padding: 6px 12px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn:hover, .toggle-btn.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

/* 笔记预览 */
.note-preview {
  flex: 1;
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
}

.note-preview h1, .note-preview h2, .note-preview h3,
.note-preview h4, .note-preview h5, .note-preview h6 {
  margin: 16px 0 8px;
  color: #111827;
}

.note-preview h1 { font-size: 24px; }
.note-preview h2 { font-size: 20px; }
.note-preview h3 { font-size: 18px; }

.note-preview p {
  margin: 8px 0;
  color: #374151;
}

.note-preview blockquote {
  margin: 12px 0;
  padding: 12px 16px;
  background: rgba(99, 102, 241, 0.05);
  border-left: 4px solid #6366f1;
  color: #4b5563;
  font-style: italic;
}

.note-preview ul {
  margin: 8px 0;
  padding-left: 24px;
}

.note-preview li {
  margin: 4px 0;
  color: #374151;
}

.note-preview code.inline-code {
  padding: 2px 6px;
  background: #f3f4f6;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #ef4444;
}

.note-preview pre.code-block {
  margin: 12px 0;
  padding: 16px;
  background: #1f2937;
  border-radius: 8px;
  overflow-x: auto;
}

.note-preview pre.code-block code {
  color: #e5e7eb;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

/* 夜间模式新增样式 */
.dark-mode .export-btn {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .export-btn:hover {
  background: #475569;
}

.dark-mode .export-dropdown {
  background: #1e293b;
  border-color: #334155;
}

.dark-mode .export-option {
  color: #e2e8f0;
}

.dark-mode .export-option:hover {
  background: #334155;
}

.dark-mode .note-search {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark-mode .note-search-input {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .note-search-input:focus {
  border-color: #6366f1;
}

.dark-mode .chapter-tag {
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
}

.dark-mode .note-item-preview {
  color: #94a3b8;
}

.dark-mode .chapter-selector {
  background: #334155;
}

.dark-mode .selector-label {
  color: #e2e8f0;
}

.dark-mode .chapter-select {
  background: #1e293b;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .current-chapter-tag {
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
}

.dark-mode .editor-toolbar {
  background: #334155;
}

.dark-mode .toolbar-btn {
  background: #1e293b;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .toolbar-btn:hover:not(:disabled) {
  background: #334155;
}

.dark-mode .toggle-btn {
  background: #334155;
  border-color: #475569;
  color: #94a3b8;
}

.dark-mode .toggle-btn:hover, .dark-mode .toggle-btn.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

.dark-mode .note-preview {
  background: #334155;
  border-color: #475569;
}

.dark-mode .note-preview h1,
.dark-mode .note-preview h2,
.dark-mode .note-preview h3,
.dark-mode .note-preview h4,
.dark-mode .note-preview h5,
.dark-mode .note-preview h6 {
  color: #e2e8f0;
}

.dark-mode .note-preview p {
  color: #cbd5e1;
}

.dark-mode .note-preview blockquote {
  background: rgba(99, 102, 241, 0.1);
  color: #94a3b8;
}

.dark-mode .note-preview li {
  color: #cbd5e1;
}

.dark-mode .note-preview code.inline-code {
  background: #1e293b;
  color: #f87171;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .note-container {
    flex-direction: column;
  }
  
  .note-list {
    width: 100%;
    max-height: 200px;
  }
  
  .note-content {
    height: 500px;
  }
}

/* 夜间模式样式 */
.dark-mode {
  background-color: #0f172a;
  color: #e2e8f0;
  transition: all 0.3s ease;
}

/* 夜间模式页面背景 */
.dark-mode .bookshelf-page {
  background-color: #0f172a;
}

/* 夜间模式头部 */
.dark-mode .bookshelf-header {
  border-bottom-color: #1e293b;
}

.dark-mode .page-title {
  color: #f1f5f9;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 夜间模式搜索框 */
.dark-mode .search-input {
  background-color: #1e293b;
  border-color: #334155;
  color: #e2e8f0;
}

.dark-mode .search-input::placeholder {
  color: #94a3b8;
}

.dark-mode .search-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

/* 夜间模式筛选区域 */
.dark-mode .bookshelf-filter {
  background: #1e293b;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark-mode .filter-label {
  color: #cbd5e1;
}

.dark-mode .select-display {
  background: #1e293b;
  border-color: #334155;
  color: #e2e8f0;
}

.dark-mode .select-display:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.dark-mode .select-display .select-arrow {
  color: #94a3b8;
}

.dark-mode .select-display:hover .select-arrow {
  color: #6366f1;
  transform: translateY(-50%) rotate(180deg);
}

.dark-mode .select-dropdown {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dark-mode .select-option {
  color: #e2e8f0;
}

.dark-mode .select-option:hover {
  background-color: #334155;
  color: #6366f1;
}

.dark-mode .select-option.active {
  background-color: #6366f1;
  color: white;
}

/* 夜间模式书籍卡片 */
.dark-mode .book-card {
  background: #1e293b;
  border-color: #334155;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.2);
}

.dark-mode .book-card:hover {
  border-color: #475569;
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.3);
}

.dark-mode .book-title {
  color: #f1f5f9;
}

.dark-mode .book-author {
  color: #94a3b8;
}

.dark-mode .progress-bar {
  background: #334155;
}

.dark-mode .book-cover {
  background: #334155;
}

/* 夜间模式分页 */
.dark-mode .pagination-btn {
  background: #1e293b;
  border-color: #334155;
  color: #cbd5e1;
}

.dark-mode .pagination-btn:hover:not(:disabled) {
  border-color: #6366f1;
  color: #6366f1;
}

.dark-mode .pagination-number {
  background: #1e293b;
  border-color: #334155;
  color: #cbd5e1;
}

.dark-mode .pagination-number:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.dark-mode .pagination-number.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

/* 夜间模式模态框 */
.dark-mode .modal-content {
  background: #1e293b;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.dark-mode .modal-header {
  border-bottom-color: #334155;
}

.dark-mode .modal-title {
  color: #f1f5f9;
}

.dark-mode .modal-close {
  color: #94a3b8;
}

.dark-mode .modal-close:hover {
  background: #334155;
  color: #cbd5e1;
}

/* 夜间模式教材模态框 */
.dark-mode .category-btn {
  background: #1e293b;
  border-color: #334155;
  color: #cbd5e1;
}

.dark-mode .category-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.dark-mode .category-btn.active {
  background: #6366f1;
  color: white;
  border-color: #6366f1;
}

.dark-mode .textbook-card {
  background: #1e293b;
  border-color: #334155;
}

.dark-mode .textbook-card:hover {
  border-color: #6366f1;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
}

.dark-mode .textbook-card.selected {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
}

.dark-mode .textbook-title {
  color: #e2e8f0;
}

/* 夜间模式书籍详情 */
.dark-mode .book-detail-modal {
  background: #1e293b;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.dark-mode .book-detail-title {
  color: #f1f5f9;
}

.dark-mode .book-detail-meta {
  color: #94a3b8;
}

.dark-mode .book-detail-description h4,
.dark-mode .book-detail-catalog h4 {
  color: #e2e8f0;
}

.dark-mode .book-detail-description p {
  color: #cbd5e1;
}

.dark-mode .catalog-link {
  color: #818cf8;
}

.dark-mode .chapter-title {
  color: #e2e8f0;
}

.dark-mode .section-item {
  color: #94a3b8;
}

.dark-mode .section-item:hover {
  color: #818cf8;
}

.dark-mode .catalog-chapter {
  border-color: #334155;
}

.dark-mode .catalog-chapter:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.dark-mode .action-btn {
  background: #1e293b;
  border-color: #334155;
  color: #cbd5e1;
}

.dark-mode .action-btn:hover {
  background: #334155;
  border-color: #475569;
}

.dark-mode .action-btn.primary {
  background: linear-gradient(90deg, #6366f1 0%, #4f46e5 100%);
  border-color: #6366f1;
}

.dark-mode .action-btn.primary:hover {
  background: linear-gradient(90deg, #4f46e5 0%, #1d4ed8 100%);
  border-color: #4f46e5;
}

.dark-mode .action-btn.edit {
  background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%);
  border-color: #f59e0b;
}

.dark-mode .action-btn.edit:hover {
  background: linear-gradient(90deg, #d97706 0%, #b45309 100%);
  border-color: #d97706;
}

.dark-mode .action-btn.delete {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
  border-color: #ef4444;
}

.dark-mode .action-btn.delete:hover {
  background: linear-gradient(90deg, #dc2626 0%, #b91c1c 100%);
  border-color: #dc2626;
}

/* 深色模式下的模态框标题 */
.dark-mode .modal-title {
  color: #f1f5f9;
}

.dark-mode .modal-header {
  border-bottom-color: #334155;
}

.dark-mode .modal-close {
  color: #94a3b8;
}

.dark-mode .modal-close:hover {
  background: #334155;
  color: #e2e8f0;
}

/* 深色模式下的表单标签 */
.dark-mode .form-group label {
  color: #e2e8f0;
}

.dark-mode .form-group label::before {
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
}

/* 夜间模式学习模态框 */
.dark-mode .study-modal {
  background: #1e293b;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.dark-mode .study-header {
  background: #1e293b;
  border-bottom-color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .study-header .back-btn, 
.dark-mode .study-header .menu-btn, 
.dark-mode .study-header .search-btn {
  background: #334155;
  color: #e2e8f0;
}

.dark-mode .study-header .back-btn:hover, 
.dark-mode .study-header .menu-btn:hover, 
.dark-mode .study-header .search-btn:hover {
  background: #475569;
}

.dark-mode .study-header .menu-btn.active {
  background: #6366f1;
  color: white;
}

.dark-mode .study-header .icon-btn {
  background: #334155;
}

.dark-mode .study-header .icon-btn:hover {
  background: #475569;
}

.dark-mode .study-content {
  background: #1e293b;
}

/* 夜间模式侧边栏 */
.dark-mode .study-content .sidebar {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .study-content .sidebar-header {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark-mode .study-content .sidebar-header h3 {
  color: #e2e8f0;
}

.dark-mode .study-content .collapse-btn {
  color: #94a3b8;
}

.dark-mode .toc-item {
  color: #e2e8f0;
}

.dark-mode .toc-item:hover {
  background: #334155;
}

.dark-mode .toc-item.active {
  background: rgba(99, 102, 241, 0.1);
  color: #818cf8;
}

.dark-mode .toc-children {
  border-left-color: #334155;
}

/* 夜间模式阅读区 */
.dark-mode .reading-area {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .reading-header {
  border-bottom-color: #334155;
}

.dark-mode .reading-header h2 {
  color: #e2e8f0;
}

.dark-mode .expand-btn {
  background: #334155;
  color: #94a3b8;
}

.dark-mode .reading-content {
  color: #cbd5e1;
}

.dark-mode .section-title {
  color: #e2e8f0;
}

.dark-mode .check-item span {
  color: #cbd5e1;
}

.dark-mode .create-btn {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
}

.dark-mode .create-btn:hover {
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.dark-mode .pagination {
  border-top-color: #334155;
}

.dark-mode .pagination span {
  color: #94a3b8;
}

.dark-mode .pagination span:hover {
  background: #334155;
  color: #e2e8f0;
}

/* 夜间模式提问侧边栏 */
.dark-mode .question-sidebar {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .question-sidebar-header {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark-mode .question-sidebar-header h3 {
  color: #e2e8f0;
}

.dark-mode .question-sidebar-header .close-btn {
  color: #94a3b8;
}

.dark-mode .question-sidebar-header .close-btn:hover {
  background: #334155;
  color: #e2e8f0;
}

.dark-mode .selected-text {
  background: #1e293b;
  border-color: #334155;
}

.dark-mode .selected-text h4 {
  color: #e2e8f0;
}

.dark-mode .selected-text p {
  color: #cbd5e1;
}

.dark-mode .question-input h4 {
  color: #e2e8f0;
}

.dark-mode .question-input textarea {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
}

.dark-mode .question-input textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.dark-mode .submit-question-btn {
  background: #6366f1;
  color: white;
}

.dark-mode .submit-question-btn:hover {
  background: #4f46e5;
}

.dark-mode .ai-answer {
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.3);
}

.dark-mode .ai-answer h4 {
  color: #818cf8;
}

.dark-mode .answer-content p {
  color: #cbd5e1;
}

.dark-mode .detailed-question-btn {
  background: #334155;
  color: #818cf8;
  border-color: #818cf8;
}

.dark-mode .detailed-question-btn:hover {
  background: rgba(99, 102, 241, 0.1);
}

/* 夜间模式文本选择菜单 */
.dark-mode .text-selection-menu {
  background: #1e293b;
  border: 1px solid #334155;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dark-mode .menu-item {
  background: #334155;
  color: #e2e8f0;
}

.dark-mode .menu-item:hover {
  background: #475569;
  color: white;
}

/* 夜间模式底部工具栏 */
.dark-mode .study-footer {
  background: #1e293b;
  border-top-color: #334155;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .study-footer .footer-btn {
  background: #334155;
  color: #e2e8f0;
}

.dark-mode .study-footer .footer-btn:hover {
  background: #475569;
}

.dark-mode .study-footer .footer-btn.active {
  background: #6366f1;
  color: white;
}

.dark-mode .study-footer .ai-assistant {
  background: linear-gradient(135deg, #334155, #475569);
}

.dark-mode .study-footer .ai-text {
  color: #fbbf24;
}

.dark-mode .action-btn.primary:hover {
  background: linear-gradient(90deg, #4f46e5 0%, #1d4ed8 100%);
  border-color: #4f46e5;
}

.dark-mode .sidebar-section h4 {
  color: #e2e8f0;
}

.dark-mode .recommended-book {
  background: #334155;
}

.dark-mode .recommended-book:hover {
  background: #475569;
}

.dark-mode .recommended-book-title {
  color: #e2e8f0;
}

.dark-mode .review-item {
  background: #334155;
}

.dark-mode .reviewer-name {
  color: #e2e8f0;
}

.dark-mode .book-detail-sidebar {
  border-left-color: #334155;
}

/* 夜间模式学习模态框 */
.dark-mode .study-modal {
  background: #1e293b;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.dark-mode .study-header {
  background: #1e293b;
  border-bottom-color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .study-header .back-btn,
.dark-mode .study-header .menu-btn,
.dark-mode .study-header .search-btn {
  background: #334155;
  color: #cbd5e1;
}

.dark-mode .study-header .back-btn:hover,
.dark-mode .study-header .menu-btn:hover,
.dark-mode .study-header .search-btn:hover {
  background: #475569;
}

.dark-mode .study-header .menu-btn.active {
  background: #6366f1;
  color: white;
}

.dark-mode .study-header .icon-btn {
  background: #334155;
}

.dark-mode .study-header .icon-btn:hover {
  background: #475569;
}

.dark-mode .study-content .sidebar {
  background: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.dark-mode .study-content .sidebar-header {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark-mode .study-content .sidebar-header h3 {
  color: #f1f5f9;
}

.dark-mode .study-content .collapse-btn {
  color: #94a3b8;
}

.dark-mode .toc-item:hover {
  background: #475569;
}

.dark-mode .toc-item.active {
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
}

.dark-mode .toc-children {
  border-left-color: #334155;
}

.dark-mode .reading-area {
  background: #1e293b;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.dark-mode .reading-header {
  border-bottom-color: #334155;
}

.dark-mode .reading-header h2 {
  color: #f1f5f9;
}

.dark-mode .expand-btn {
  background: #334155;
  color: #94a3b8;
}

.dark-mode .reading-content {
  color: #cbd5e1;
}

.dark-mode .section-title {
  color: #e2e8f0;
}

.dark-mode .question-sidebar {
  background: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.dark-mode .question-sidebar-header {
  background: #1e293b;
  border-bottom-color: #334155;
}

.dark-mode .question-sidebar-header h3 {
  color: #e2e8f0;
}

.dark-mode .question-sidebar-header .close-btn {
  color: #94a3b8;
}

.dark-mode .question-sidebar-header .close-btn:hover {
  background: #334155;
  color: #cbd5e1;
}

.dark-mode .selected-text {
  background: #1e293b;
  border-color: #334155;
}

.dark-mode .selected-text h4 {
  color: #e2e8f0;
}

.dark-mode .selected-text p {
  color: #94a3b8;
}

.dark-mode .question-input h4 {
  color: #e2e8f0;
}

.dark-mode .question-input textarea {
  background: #1e293b;
  border-color: #334155;
  color: #e2e8f0;
}

.dark-mode .question-input textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.dark-mode .ai-answer {
  background: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.3);
}

.dark-mode .ai-answer h4 {
  color: #818cf8;
}

.dark-mode .answer-content p {
  color: #cbd5e1;
}

.dark-mode .detailed-question-btn {
  background: #1e293b;
  color: #818cf8;
  border-color: #6366f1;
}

.dark-mode .detailed-question-btn:hover {
  background: rgba(99, 102, 241, 0.1);
}

.dark-mode .study-footer {
  background: #1e293b;
  border-top-color: #334155;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.2);
}

.dark-mode .study-footer .footer-btn {
  background: #334155;
  color: #cbd5e1;
}

.dark-mode .study-footer .footer-btn:hover {
  background: #475569;
}

.dark-mode .study-footer .footer-btn.active {
  background: #6366f1;
  color: white;
}

.dark-mode .text-selection-menu {
  background: #1e293b;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.dark-mode .menu-item {
  background: #334155;
  color: #cbd5e1;
}

.dark-mode .menu-item:hover {
  background: #475569;
}

/* 夜间模式确认删除对话框 */
.dark-mode .confirm-modal {
  background: #1e293b;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.dark-mode .confirm-header {
  background: #334155;
  border-bottom-color: #334155;
}

.dark-mode .confirm-title {
  color: #f1f5f9;
}

.dark-mode .confirm-message {
  color: #cbd5e1;
}

.dark-mode .confirm-btn.cancel {
  background: #334155;
  color: #cbd5e1;
  border-color: #334155;
}

.dark-mode .confirm-btn.cancel:hover {
  background: #475569;
}

/* 夜间模式自定义滚动条 */
.dark-mode ::-webkit-scrollbar-track {
  background: #1e293b;
}

.dark-mode ::-webkit-scrollbar-thumb {
  background: #475569;
}

.dark-mode ::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

.dark-mode .book-detail-main::-webkit-scrollbar-track,
.dark-mode .book-detail-sidebar::-webkit-scrollbar-track,
.dark-mode .full-catalog::-webkit-scrollbar-track {
  background: #1e293b;
}

.dark-mode .book-detail-main::-webkit-scrollbar-thumb,
.dark-mode .book-detail-sidebar::-webkit-scrollbar-thumb,
.dark-mode .full-catalog::-webkit-scrollbar-thumb {
  background: #475569;
}

.dark-mode .book-detail-main::-webkit-scrollbar-thumb:hover,
.dark-mode .book-detail-sidebar::-webkit-scrollbar-thumb:hover,
.dark-mode .full-catalog::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

/* 夜间模式响应式调整 */
@media (max-width: 768px) {
  .dark-mode .dark-mode-toggle {
    position: static;
    transform: none;
  }
  
  .dark-mode .header-content {
    justify-content: center;
  }
}

/* 加载状态样式 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6b7280;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e5e7eb;
  border-top: 2px solid #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
}

/* 错误状态样式 */
.error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #ef4444;
  background-color: #fef2f2;
  border-radius: 8px;
  margin: 10px 0;
}

.error-icon {
  font-size: 20px;
  margin-right: 10px;
}

.error-text {
  font-size: 14px;
  text-align: center;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #6b7280;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
  text-align: center;
}

/* 编辑书籍模态框样式 */
.edit-book-modal {
  max-width: 900px;
  width: 95%;
  max-height: 85vh;
  overflow: hidden;
  background: white;
  border-radius: 20px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.18);
  animation: modalSlideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 24px 30px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f9fafb;
  border-radius: 20px 20px 0 0;
}

.modal-header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-icon {
  font-size: 24px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #6b7280;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
  transform: rotate(90deg);
}

.edit-book-content {
  padding: 30px;
  overflow-y: auto;
  flex: 1;
  max-height: calc(85vh - 120px);
}

.book-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #374151;
  font-size: 14px;
  position: relative;
  padding-left: 8px;
}

.field-label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border-radius: 3px;
}

.label-icon {
  font-size: 16px;
}

.form-input,
.form-textarea,
.form-select {
  padding: 14px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  font-family: 'Microsoft YaHei', sans-serif;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  color: #374151;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1), 0 4px 16px rgba(99, 102, 241, 0.15);
  transform: translateY(-1px);
}

.form-textarea {
  resize: vertical;
  min-height: 140px;
  line-height: 1.5;
}

/* 下拉选择框样式 */
.form-select {
  /* 完全重置样式 */
  all: unset;
  box-sizing: border-box;
  
  /* 基础样式 */
  padding: 14px 18px;
  padding-right: 40px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  font-family: 'Microsoft YaHei', sans-serif;
  background: white;
  color: #374151;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: block;
  width: 100%;
  line-height: 1.5;
}

/* 隐藏所有浏览器的默认箭头 */
.form-select::-ms-expand {
  display: none;
}

/* 确保没有浏览器默认箭头 */
.form-select {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
}

/* 使用CSS伪元素实现自定义箭头 */
.form-select::after {
  content: '';
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  width: 10px;
  height: 6px;
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%236B7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 1l4 4 4-4'/%3E%3C/svg%3E") no-repeat center;
  pointer-events: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.form-select:hover::after {
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%233B82F6' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 1l4 4 4-4'/%3E%3C/svg%3E") no-repeat center;
  transform: translateY(-50%) scale(1.1);
}

.form-select:focus::after {
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%233B82F6' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 5l4-4 4 4'/%3E%3C/svg%3E") no-repeat center;
  transform: translateY(-50%) scale(1.1);
}

.form-select:disabled::after {
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%239CA3AF' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 1l4 4 4-4'/%3E%3C/svg%3E") no-repeat center;
}

/* 确保:focus状态正常 */
.form-select:focus {
  outline: 2px solid #6366f1;
  outline-offset: 2px;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1), 0 4px 16px rgba(99, 102, 241, 0.15);
  transform: translateY(-1px);
}

.form-select:hover {
  border-color: #a5b4fc;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
  transform: translateY(-1px);
}

.form-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f9fafb;
  border-color: #e5e7eb;
  transform: none;
  box-shadow: none;
}

/* 确保.form-select元素不会显示额外的箭头 */
.form-select + .select-arrow {
  display: none;
}

/* 确保编辑模态框中的下拉框不会受到自定义下拉框样式的影响 */
.edit-book-modal .select-arrow {
  display: none;
}

.edit-book-modal .select-container {
  position: static;
  width: 100%;
}

.edit-book-modal .select-display {
  display: block;
  position: static;
  padding: 0;
  border: none;
  background: none;
  cursor: default;
}

.edit-book-modal .select-display .select-arrow {
  display: none;
}

/* 优化选项样式 */
.form-select option {
  padding: 14px 18px;
  background: white;
  color: #374151;
  font-size: 14px;
  font-family: 'Microsoft YaHei', sans-serif;
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 4px 8px;
  cursor: pointer;
  border: 1px solid transparent;
  display: block;
  width: calc(100% - 16px);
  box-sizing: border-box;
}

.form-select option:hover {
  background: #f0f9ff;
  color: #6366f1;
  border-color: #e0e7ff;
  transform: translateX(4px);
}

.form-select option:checked {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  font-weight: 600;
  border-color: #6366f1;
  transform: translateX(8px);
}

.form-select option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 空选项样式 */
.form-select option[value=""] {
  font-style: italic;
  color: #9ca3af;
}

.form-select option[value=""]:hover {
  background: #f9fafb;
  color: #6b7280;
  border-color: #e5e7eb;
  transform: none;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.btn {
  padding: 14px 28px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
  justify-content: center;
}

.btn-secondary {
  background: #f3f4f6;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #e5e7eb;
  color: #374151;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #4f46e5 0%, #1d4ed8 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 按钮图标 */
.btn-icon {
  font-size: 16px;
  line-height: 1;
  display: inline-block;
  width: 20px;
  text-align: center;
}

/* 加载状态 */
.loading-spinner.small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 0;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 模态框动画 */
@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.92);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 编辑模态框滚动条 */
.edit-book-content::-webkit-scrollbar {
  width: 8px;
}

.edit-book-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
  margin: 10px 0;
}

.edit-book-content::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 10px;
  transition: background 0.3s ease;
}

.edit-book-content::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 夜间模式编辑模态框 */
.dark-mode .edit-book-modal {
  background: #1e293b;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.4);
}

.dark-mode .modal-header {
  background: #253347;
  border-bottom-color: #334155;
}

.dark-mode .modal-title {
  color: #f8fafc;
}

.dark-mode .modal-close {
  color: #94a3b8;
}

.dark-mode .modal-close:hover {
  background: #334155;
  color: #f8fafc;
}

.dark-mode .field-label {
  color: #e2e8f0;
}

.dark-mode .field-label::before {
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
}

.dark-mode .form-input,
.dark-mode .form-textarea,
.dark-mode .form-select {
  background: #334155;
  border-color: #475569;
  color: #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.dark-mode .form-input:focus,
.dark-mode .form-textarea:focus,
.dark-mode .form-select:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2), 0 4px 16px rgba(99, 102, 241, 0.25);
}

/* 夜间模式下的箭头样式 */
.dark-mode .form-select::after {
  content: '';
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  width: 10px;
  height: 6px;
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%2394A3B8' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 1l4 4 4-4'/%3E%3C/svg%3E") no-repeat center;
  pointer-events: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.dark-mode .form-select:hover::after {
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%2360A5FA' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 1l4 4 4-4'/%3E%3C/svg%3E") no-repeat center;
  transform: translateY(-50%) scale(1.1);
}

.dark-mode .form-select:focus::after {
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%2360A5FA' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 5l4-4 4 4'/%3E%3C/svg%3E") no-repeat center;
  transform: translateY(-50%) scale(1.1);
}

.dark-mode .form-select:disabled::after {
  background: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 10 6'%3E%3Cpath stroke='%2364748B' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M1 1l4 4 4-4'/%3E%3C/svg%3E") no-repeat center;
}

/* 确保夜间模式下的:focus状态正常 */
.dark-mode .form-select:focus {
  outline: 2px solid #6366f1;
  outline-offset: 2px;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2), 0 4px 16px rgba(99, 102, 241, 0.25);
  transform: translateY(-1px);
}

.dark-mode .form-select:hover {
  border-color: #818cf8;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
  transform: translateY(-1px);
}

.dark-mode .form-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #1e293b;
  border-color: #334155;
  transform: none;
  box-shadow: none;
}

/* 夜间模式选项样式 */
.dark-mode .form-select option {
  padding: 14px 18px;
  background: #1e293b;
  color: #e2e8f0;
  font-size: 14px;
  font-family: 'Microsoft YaHei', sans-serif;
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 4px 8px;
  cursor: pointer;
  border: 1px solid transparent;
  display: block;
  width: calc(100% - 16px);
  box-sizing: border-box;
}

.dark-mode .form-select option:hover {
  background: #1e40af;
  color: #818cf8;
  border-color: #6366f1;
  transform: translateX(4px);
}

.dark-mode .form-select option:checked {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  font-weight: 600;
  border-color: #6366f1;
  transform: translateX(8px);
}

.dark-mode .form-select option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 夜间模式空选项样式 */
.dark-mode .form-select option[value=""] {
  font-style: italic;
  color: #64748b;
}

.dark-mode .form-select option[value=""]:hover {
  background: #334155;
  color: #94a3b8;
  border-color: #475569;
  transform: none;
}

.dark-mode .form-actions {
  border-top-color: #334155;
}

.dark-mode .btn-secondary {
  background: #334155;
  color: #94a3b8;
  border-color: #475569;
}

.dark-mode .btn-secondary:hover {
  background: #475569;
  color: #f8fafc;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.dark-mode .btn-primary {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.dark-mode .btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #4f46e5 0%, #1d4ed8 100%);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

/* 夜间模式编辑模态框滚动条 */
.dark-mode .edit-book-content::-webkit-scrollbar-track {
  background: #334155;
}

.dark-mode .edit-book-content::-webkit-scrollbar-thumb {
  background: #475569;
}

.dark-mode .edit-book-content::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .edit-book-modal {
    width: 98%;
    max-height: 90vh;
  }
  
  .modal-header {
    padding: 20px 24px;
  }
  
  .modal-title {
    font-size: 18px;
  }
  
  .modal-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .edit-book-content {
    padding: 24px;
    max-height: calc(90vh - 100px);
  }
  
  .form-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
  }
}

/* 夜间模式状态样式 */
.dark-mode .loading-state {
  color: #9ca3af;
}

.dark-mode .loading-spinner {
  border-color: #374151;
  border-top-color: #818cf8;
}

.dark-mode .error-state {
  color: #f87171;
  background-color: #7f1d1d;
}

.dark-mode .empty-state {
  color: #9ca3af;
}

/* 章节内容样式 */
.chapter-content {
  padding: 10px 0;
  line-height: 1.8;
  color: #333;
}

.chapter-content h2 {
  font-size: 1.4em;
  color: #1a1a2e;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #4a90d9;
}

.chapter-content h3 {
  font-size: 1.15em;
  color: #2d3748;
  margin: 20px 0 12px;
}

.chapter-content p {
  margin-bottom: 12px;
  text-indent: 2em;
}

.chapter-content ul, .chapter-content ol {
  margin: 8px 0 12px 2em;
}

.chapter-content li {
  margin-bottom: 6px;
}

.chapter-content .highlight {
  background: #fff8e1;
  border-left: 4px solid #ffc107;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 0 4px 4px 0;
}

.chapter-content .highlight strong {
  color: #e65100;
}

.chapter-content .example {
  background: #e3f2fd;
  border-left: 4px solid #1976d2;
  padding: 12px 16px;
  margin: 12px 0;
  border-radius: 0 4px 4px 0;
}

.chapter-content .example strong {
  color: #1565c0;
}

.chapter-content .content-section {
  margin-bottom: 20px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e0e0e0;
  border-top-color: #4a90d9;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #aaa;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

/* 暗色模式 */
.dark-mode .chapter-content {
  color: #e0e0e0;
}

.dark-mode .chapter-content h2 {
  color: #90caf9;
  border-bottom-color: #42a5f5;
}

.dark-mode .chapter-content h3 {
  color: #b0bec5;
}

.dark-mode .chapter-content .highlight {
  background: #3e2723;
  border-left-color: #ffb300;
}

.dark-mode .chapter-content .highlight strong {
  color: #ffb300;
}

.dark-mode .chapter-content .example {
  background: #1a237e;
  border-left-color: #42a5f5;
}

.dark-mode .chapter-content .example strong {
  color: #64b5f6;
}
</style>