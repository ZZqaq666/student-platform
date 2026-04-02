<template>
  <div class="qa-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <button v-if="from === 'study'" class="back-home-btn" @click="goBackToStudy" style="margin-right: 10px;">
          <span class="back-icon">←</span>
          返回学习
        </button>
        <button class="back-home-btn" @click="goHome">
          <span class="back-icon">←</span>
          返回首页
        </button>
      </div>
      <h1 class="page-title">问答中心</h1>
      <div class="subtitle">核心功能模块</div>
    </div>

    <!-- 导航标签 -->
    <div class="tab-navigation">
      <div class="tab" :class="{ active: activeTab === '智能答疑' }" @click="activeTab = '智能答疑'">
        智能答疑
      </div>
      <div class="tab" :class="{ active: activeTab === '考研/考证问答' }" @click="activeTab = '考研/考证问答'">
        考研/考证问答
      </div>
    </div>

    <!-- 智能答疑内容（合并了作业答疑和专业课问答） -->
    <div v-if="activeTab === '智能答疑'" class="professional-qa">
      <div class="qa-container">
        <!-- 左侧：教材选择器 -->
        <div class="left-panel">
          <div class="panel-title">教材选择器</div>
          <!-- 搜索框 -->
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchQuery"
              placeholder="搜索教材..."
              class="search-input"
            >
            <span class="search-icon">🔍</span>
          </div>
          <div class="book-selector">
            <div v-if="loading.books" class="loading-message">加载书籍中...</div>
            <div v-else-if="error.books" class="error-message">{{ error.books }}</div>
            <div 
              v-for="(book, index) in books" 
              :key="book.id || index"
              class="book-item"
              :class="{ active: selectedBook?.id === book.id }"
              @click="selectBook(book)"
            >
              <div class="book-icon">
                <div class="book-cover">
                  <img :src="book.cover_image || book.coverImage || getDefaultCover(book.subject || 'DEFAULT')" :alt="book.title || book.name" @error="handleBookCoverImageError">
                </div>
              </div>
              <div class="book-info">
                <div class="book-name">{{ book.title || book.name }}</div>
                <div class="book-list">{{ book.author }}</div>
                <div class="book-publisher" v-if="book.publisher">{{ book.publisher }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间：章节导航 -->
        <div class="middle-panel">
          <div class="panel-title">章节导航</div>
          <div class="chapter-nav">
            <!-- 加载状态 -->
            <div v-if="loading.chapters" class="loading-state">
              <div class="loading-spinner"></div>
              <span class="loading-text">加载章节中...</span>
            </div>
            
            <!-- 错误状态 -->
            <div v-else-if="error.chapters" class="error-state">
              <span class="error-icon">⚠️</span>
              <span class="error-text">{{ error.chapters }}</span>
            </div>
            
            <!-- 空状态 -->
            <div v-else-if="chapters.length === 0" class="empty-state">
              <span class="empty-icon">📚</span>
              <span class="empty-text">暂无章节数据</span>
            </div>
            
            <!-- 章节树 -->
            <template v-else>
              <template v-for="chapter in chapters" :key="chapter?.id || Math.random()">
                <div 
                  v-if="chapter"
                  class="nav-item"
                  :class="{ active: selectedChapter?.id === chapter.id, expanded: expandedChapters.includes(chapter.id) }"
                >
                  <span class="nav-title" @click="selectChapter(chapter)">{{ chapter.title }}</span>
                  <span 
                    v-if="chapter.children && chapter.children.length > 0"
                    class="nav-arrow" 
                    @click.stop="toggleChapter(chapter.id)"
                  >
                    {{ expandedChapters.includes(chapter.id) ? '▼' : '>>' }}
                  </span>
                  <span v-else class="nav-arrow" @click="selectChapter(chapter)">></span>
                </div>
                <!-- 递归渲染子章节 -->
                <div 
                  v-if="chapter && expandedChapters.includes(chapter.id) && chapter.children && chapter.children.length > 0"
                  class="nav-children"
                >
                  <div 
                    v-for="child in chapter.children" 
                    :key="child.id"
                    class="nav-item sub-nav-item"
                    :class="{ active: selectedChapter?.id === child.id, expanded: expandedChapters.includes(child.id) }"
                  >
                    <span class="nav-title" @click="selectChapter(child)">{{ child.title }}</span>
                    <span 
                      v-if="child.children && child.children.length > 0"
                      class="nav-arrow" 
                      @click.stop="toggleChapter(child.id)"
                    >
                      {{ expandedChapters.includes(child.id) ? '▼' : '>>' }}
                    </span>
                    <span v-else class="nav-arrow" @click="selectChapter(child)">></span>
                  </div>
                  <!-- 递归渲染更深层次的子章节 -->
                  <div 
                    v-if="chapter && chapter.children"
                  >
                    <div 
                      v-for="child in chapter.children" 
                      :key="`sub-${child.id}`"
                      v-if="child && child.children && child.children.length > 0 && expandedChapters.includes(child.id)"
                    >
                      <div 
                        v-for="grandchild in child.children" 
                        :key="grandchild.id"
                        class="nav-item sub-sub-nav-item"
                        :class="{ active: selectedChapter?.id === grandchild.id }"
                        @click="selectChapter(grandchild)"
                      >
                        <span class="nav-title">{{ grandchild.title }}</span>
                        <span class="nav-arrow" @click="selectChapter(grandchild)">></span>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </template>
          </div>
          
          <!-- 章节内容显示区域 -->
          <div v-if="selectedChapter" class="chapter-content">
            <div class="content-header">
              <h3>{{ selectedChapter.title }}</h3>
            </div>
            <div class="content-body">
              <p>{{ selectedChapter.content || '暂无内容' }}</p>
            </div>
          </div>
        </div>

        <!-- 右侧：答疑区域（支持文字和图片） -->
        <div class="right-panel">
          <div class="panel-title">答疑区域</div>
          
          <!-- 问题类型切换 -->
          <div class="question-type-tabs">
            <div 
              class="type-tab" 
              :class="{ active: questionType === 'text' }" 
              @click="questionType = 'text'"
            >
              文字提问
            </div>
            <div 
              class="type-tab" 
              :class="{ active: questionType === 'image' }" 
              @click="questionType = 'image'"
            >
              图片上传
            </div>
          </div>

          <!-- 文字提问区域 -->
          <div v-if="questionType === 'text'" class="question-area">
            <textarea 
              v-model="question"
              placeholder="请输入您的问题..."
              class="question-input"
              rows="3"
            ></textarea>
            <button class="submit-btn" @click="submitQuestion">
              提交问题
            </button>
          </div>

          <!-- 图片上传区域 -->
          <div v-else class="image-upload-area">
            <div class="upload-box" @click="triggerFileUpload">
              <input 
                type="file" 
                ref="fileInput" 
                style="display: none;" 
                accept="image/*"
                @change="handleFileChange"
              >
              <div class="upload-placeholder">
                <span class="upload-icon">📷</span>
                <span class="upload-text">点击上传作业/教材图片</span>
              </div>
            </div>
            <div v-if="uploadedImage" class="uploaded-preview">
              <div class="preview-container">
                <img :src="uploadedImage" alt="上传的图片" class="preview-image">
                <button class="delete-btn" @click="deleteImage">
                  ×
                </button>
              </div>
              <div class="image-question-input">
                <textarea 
                  v-model="imageQuestion"
                  placeholder="请描述您的问题..."
                  class="question-input"
                  rows="2"
                ></textarea>
              </div>
              <button class="submit-btn" @click="submitRegularImageQuestion">
                  提交问题
                </button>
            </div>
          </div>

          <!-- AI回答区域 -->
          <div v-if="showAnswer" class="answer-area">

            <div class="answer-content">
              <div class="ai-avatar">
                <div class="avatar-placeholder">
                  <span class="avatar-text">AI</span>
                </div>
              </div>
              <div class="answer-text">
                <div v-if="isLoading" class="loading-indicator">
                  <div class="loading-spinner"></div>
                  <span class="loading-text">AI正在思考</span>
                </div>
                <div v-else-if="errorMessage" class="error-message">
                  {{ errorMessage }}
                  <button class="retry-btn" @click="submitQuestion">重试</button>
                </div>
                <div v-else>
                  <div v-if="isTyping" class="typing-content">
                    <div v-html="processMathText(typedAnswer)" class="processed-content"></div>
                    <span class="typing-cursor">|</span>
                  </div>
                  <div v-else class="typing-content">
                    <div v-html="processedAnswer" class="processed-content"></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="answer-controls" v-if="showAnswer">
              <button class="control-btn" @click="cancelGeneration">
                取消
              </button>
              <button class="control-btn" @click="submitQuestion">
                重新生成
              </button>
            </div>
          </div>

          <!-- 历史回答 -->
          <div class="history-section">
            <div class="history-header">
              <div class="history-title">历史答疑记录</div>
              <div class="history-actions">
                <input 
                  type="text" 
                  v-model="historySearchQuery"
                  placeholder="搜索历史记录..."
                  class="history-search-input"
                  @input="handleHistorySearch"
                >
                <div class="history-filters">
                  <select class="filter-select" @change="filterHistoryByTime($event.target.value)">
                    <option value="all">全部时间</option>
                    <option value="today">今天</option>
                    <option value="week">本周</option>
                    <option value="month">本月</option>
                    <option value="year">本年</option>
                  </select>
                  <select class="filter-select" @change="filterHistoryBySubject($event.target.value)">
                    <option value="all">全部学科</option>
                    <option value="math">数学</option>
                    <option value="physics">物理</option>
                    <option value="chemistry">化学</option>
                    <option value="biology">生物</option>
                    <option value="english">英语</option>
                  </select>
                </div>
              </div>
            </div>
            <div v-if="loading.history" class="loading-message">加载历史记录中...</div>
            <div v-else-if="error.history" class="error-message">{{ error.history }}</div>
            <div v-else-if="filteredHistory.length === 0" class="empty-history">
              暂无历史答疑记录
            </div>
            <div 
              v-for="(item, index) in paginatedHistory" 
              :key="item.id || index"
              class="history-item"
            >
              <div class="history-header">
                <div class="history-question">{{ item.question }}</div>
                <div class="history-time">{{ formatDateTime(item.createdAt) }}</div>
                <div class="history-actions">
                  <button 
                    class="more-btn" 
                    @click.stop="toggleDropdown(item.id)"
                    :class="{ active: activeDropdown === item.id }"
                  >
                    <span class="more-icon">⋮</span>
                  </button>
                  <div 
                    v-if="activeDropdown === item.id" 
                    class="dropdown-menu"
                  >
                    <button class="dropdown-item" @click.stop>
                      <span class="dropdown-icon">📌</span>
                      置顶
                    </button>
                    <button class="dropdown-item" @click.stop>
                      <span class="dropdown-icon">🔗</span>
                      分享
                    </button>
                    <button class="dropdown-item" @click.stop>
                      <span class="dropdown-icon">✏️</span>
                      重命名
                    </button>
                    <button class="dropdown-item" @click.stop>
                      <span class="dropdown-icon">⚠️</span>
                      举报
                    </button>
                    <button 
                      class="dropdown-item delete" 
                      @click.stop="confirmDeleteHistory(item.id)"
                    >
                      <span class="dropdown-icon">🗑️</span>
                      删除
                    </button>
                  </div>
                </div>
              </div>
              <div class="history-content" @click="selectHistory(item)">
                <div class="history-answer">{{ item.answer ? item.answer.replace(/\n/g, ' ').substring(0, 150) : '' }}...</div>
                <div class="history-subject" v-if="item.subject">
                  <span class="subject-tag">{{ item.subject }}</span>
                </div>
              </div>
            </div>
            <div v-if="filteredHistory.length > 0" class="pagination">
              <button 
                class="page-btn" 
                :disabled="currentPage === 1"
                @click="currentPage--"
              >
                上一页
              </button>
              <span class="page-info">
                第 {{ currentPage }} 页，共 {{ totalPages }} 页
              </span>
              <button 
                class="page-btn" 
                :disabled="currentPage === totalPages"
                @click="currentPage++"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：网课推荐 -->
      <div class="course-recommendation">
        <div class="recommendation-title">网课推荐</div>
        <div class="course-grid">
          <div 
            v-for="course in courses" 
            :key="course.id"
            class="course-item" 
            @click="openCourseLink(course.videoLink)"
          >
            <div class="course-cover">
              <div class="cover-wrapper">
                <img 
                  :src="course.coverImage || course['封面图片地址']" 
                  :alt="course.name" 
                  class="cover-image"
                  loading="lazy"
                  @error="handleImageError"
                  @load="handleImageLoad"
                >
                <div v-if="!course.coverImage && !course['封面图片地址']" class="cover-placeholder">
                  <span class="placeholder-text">🎓</span>
                </div>
                <div v-if="course.duration" class="video-duration">{{ formatDuration(course.duration) }}</div>
              </div>
            </div>
            <div class="course-info">
              <div class="course-name">{{ course.name }}</div>
              <div class="course-up">{{ course.platform || '未知' }}</div>
              <div class="course-desc">{{ course.desc }}</div>
              <div class="course-stats" v-if="course.viewCount">
                <span class="view-count">{{ formatNumber(course.viewCount) }} 播放</span>
              </div>
            </div>
            <div class="course-link-icon">🔗</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 考研/考证问答内容 -->
    <div v-else-if="activeTab === '考研/考证问答'" class="exam-qa-page">
      <div class="exam-qa-container">
        <!-- 左侧栏 -->
        <div class="exam-left-column">
          <!-- 考试科目选择 -->
          <div class="exam-panel">
            <div class="exam-panel-title">考试科目选择</div>
            <div class="exam-subject-select">
              <div class="searchable-select">
                <input 
                  type="text" 
                  v-model="subjectSearchQuery"
                  placeholder="搜索考试科目..."
                  class="select-search-input"
                >
                <div class="select-dropdown" v-show="showSubjectDropdown">
                  <div 
                    v-for="subject in filteredSubjects" 
                    :key="subject.id"
                    class="select-option"
                    :class="{ active: selectedExamSubject === subject.id }"
                    @click="selectSubject(subject)"
                  >
                    {{ subject.name }}
                  </div>
                </div>
                <div class="select-display" @click="toggleSubjectDropdown">
                  {{ getSelectedSubjectName() }}
                  <span class="select-arrow">▼</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 高频考点总结区 -->
          <div class="exam-panel">
            <div class="exam-panel-title">
              <span>高频考点总结区</span>
              <div class="keypoint-actions">
                <div class="progress-info">
                  <span class="progress-text">学习进度: {{ studyProgress }}%</span>
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: studyProgress + '%' }"></div>
                  </div>
                </div>
                <div class="sort-actions">
                  <button class="action-btn" @click="sortByImportance">重要性</button>
                  <button class="action-btn" @click="sortByDifficulty">难度</button>
                  <button class="action-btn" @click="sortByStudyTime">学习时间</button>
                  <button class="action-btn" @click="toggleViewMode">
                    {{ viewMode === 'list' ? '卡片视图' : '列表视图' }}
                  </button>
                </div>
              </div>
            </div>
            
            <!-- 考点分类筛选 -->
            <div class="keypoint-filter">
              <div class="filter-tabs">
                <div 
                  v-for="category in categories" 
                  :key="category.id"
                  class="filter-tab"
                  :class="{ active: selectedCategory === category.id }"
                  @click="selectCategory(category.id)"
                >
                  {{ category.name }}
                  <span class="category-count">{{ category.count }}</span>
                </div>
              </div>
            </div>
            
            <!-- 考点列表/卡片 -->
            <div :class="['keypoints-container', { 'card-view': viewMode === 'card' }]">
              <div 
                v-for="(point, index) in filteredKeyPoints" 
                :key="index"
                class="keypoint-item"
                :class="{ highlight: selectedKeyPoint === point, expanded: expandedKeyPoints.includes(point.id) }"
                @click="selectKeyPoint(point)"
              >
                <div class="keypoint-header">
                  <div class="keypoint-info">
                    <span class="keypoint-label">考点{{ index + 1 }}:</span>
                    <span class="keypoint-name">{{ point.name }}</span>
                    <span class="keypoint-difficulty" :class="point.difficulty">
                      {{ point.difficulty === 'easy' ? '简单' : point.difficulty === 'medium' ? '中等' : '困难' }}
                    </span>
                  </div>
                  <div class="keypoint-controls">
                    <button class="control-btn star-btn" @click.stop="toggleFavorite(point)" :class="{ active: point.isFavorite }">
                      ⭐
                    </button>
                    <button class="control-btn expand-btn" @click.stop="toggleExpand(point.id)">
                      {{ expandedKeyPoints.includes(point.id) ? '收起' : '展开' }}
                    </button>
                  </div>
                </div>
                
                <!-- 展开内容 -->
                <div v-if="expandedKeyPoints.includes(point.id)" class="keypoint-content">
                  <div class="content-section">
                    <h4>核心内容</h4>
                    <p>{{ point.content || '暂无详细内容' }}</p>
                  </div>
                  <div class="content-section">
                    <h4>考察频率</h4>
                    <div class="frequency-bar">
                      <div class="frequency-fill" :style="{ width: point.frequency + '%' }"></div>
                      <span class="frequency-text">{{ point.frequency }}%</span>
                    </div>
                  </div>
                  <div class="content-section" v-if="point.example">
                    <h4>典型例题</h4>
                    <p>{{ point.example }}</p>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 空状态 -->
            <div v-if="filteredKeyPoints.length === 0" class="empty-state">
              <span class="empty-icon">📚</span>
              <p>暂无考点数据</p>
            </div>
          </div>

          <!-- 真题推荐 -->
          <div class="exam-panel">
            <div class="exam-panel-header">
              <div class="exam-panel-title">真题推荐</div>
              <div class="exam-panel-actions">
                <button class="refresh-btn" @click="refreshRecommendations" :disabled="loading.examPapers">
                  <span v-if="!loading.examPapers">换一批</span>
                  <span v-else>加载中...</span>
                </button>
                <button class="ai-recommend-btn" @click="generateAiRecommendations" :disabled="loading.examPapers">
                  <span v-if="!loading.examPapers">AI推荐</span>
                  <span v-else>生成中...</span>
                </button>
              </div>
            </div>
            <div class="exam-papers-container">
              <div 
                v-for="(paper, index) in selectedPapers" 
                :key="paper.paperId || index"
                class="exam-paper-item"
              >
                <div class="paper-header">
                  <div class="paper-title">{{ paper.year }}年{{ paper.paperName || '真题' }}</div>
                  <div class="paper-actions">
                    <button class="action-btn favorite-btn" @click="togglePaperFavorite(paper)" :class="{ active: paper.isFavorite }">
                      {{ paper.isFavorite ? '★' : '☆' }}
                    </button>
                  </div>
                </div>
                <div class="paper-content">
                  <div class="paper-image">
                    <img src="https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=100&h=100&fit=crop" alt="真题">
                  </div>
                  <div class="paper-info">
                    <div class="paper-name">{{ paper.year }}年{{ paper.examType || '考试' }}真题</div>
                    <div class="paper-meta">
                      <span class="paper-meta-item">
                        <i class="meta-icon">📚</i>
                        {{ paper.subjectName || paper.subject }}
                      </span>
                      <span class="paper-meta-item">
                        <i class="meta-icon">⭐</i>
                        难度: {{ paper.difficulty }}
                      </span>
                      <span class="paper-meta-item">
                        <i class="meta-icon">📝</i>
                        {{ paper.totalQuestions || 0 }}题
                      </span>
                    </div>
                    <div class="paper-source">
                      <i class="source-icon">📍</i>
                      来源: {{ paper.source || '未知' }}
                    </div>
                    <div v-if="paper.recommendReason" class="paper-reason">
                      <i class="reason-icon">💡</i>
                      推荐理由: {{ paper.recommendReason }}
                    </div>
                  </div>
                </div>
                <div class="paper-footer">
                  <button class="paper-btn primary-btn" @click="viewPaperDetails(paper)">
                    查看详情
                  </button>
                  <button class="paper-btn secondary-btn" @click="startPractice(paper)">
                    开始练习
                  </button>
                </div>
              </div>
              <div v-if="selectedPapers.length === 0 && !loading.examPapers" class="empty-state">
                <div class="empty-icon">📄</div>
                <div class="empty-text">暂无推荐真题</div>
                <button class="refresh-btn" @click="refreshRecommendations">
                  重新推荐
                </button>
              </div>
              <div v-if="loading.examPapers" class="loading-state">
                <div class="loading-spinner"></div>
                <div class="loading-text">正在为您推荐真题...</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间栏 -->
        <div class="exam-middle-column">
          <!-- 考点提问区 -->
          <div class="exam-panel">
            <div class="exam-panel-title">考点提问区</div>
            <div class="exam-question-area">
              <input 
                v-model="examQuestion"
                type="text"
                placeholder="问题问传"
                class="exam-question-input"
              >
              <button class="exam-submit-btn" @click="submitExamQuestion">
                提交问题
              </button>
            </div>
            <!-- 回答区域 -->
            <div class="exam-question-display">
              <div class="exam-question-label">回答区域</div>
              <div class="answer-area">
                <div class="answer-content">
                  <div class="ai-avatar">
                    <div class="avatar-placeholder">
                      <span class="avatar-text">AI</span>
                    </div>
                  </div>
                  <div class="answer-text">
                    <div v-if="isLoading" class="loading-indicator">
                      <div class="loading-spinner"></div>
                      <span class="loading-text">AI正在思考</span>
                    </div>
                    <div v-else-if="errorMessage" class="error-message">
                      {{ errorMessage }}
                      <button class="retry-btn" @click="submitExamQuestion">重试</button>
                    </div>
                    <div v-else>
                      <div v-if="isTyping" class="typing-content">
                        <div v-html="processMathText(typedAnswer)" class="processed-content"></div>
                        <span class="typing-cursor">|</span>
                      </div>
                      <div v-else class="typing-content">
                        <div v-html="processedAnswer" class="processed-content"></div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="answer-controls" v-if="showAnswer">
                  <button class="control-btn" @click="cancelGeneration">
                    取消
                  </button>
                  <button class="control-btn" @click="submitExamQuestion">
                    重新生成
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 网课推荐 -->
          <div class="exam-panel">
            <div class="exam-panel-title">网课推荐</div>
            <div class="exam-courses-list">
              <div 
                v-for="(course, index) in examCourses" 
                :key="index"
                class="exam-course-item"
                @click="openCourseLink(course.videoLink)"
              >
                <div class="course-avatar">
                  <div class="avatar-placeholder">
                    <span class="avatar-text">讲</span>
                  </div>
                </div>
                <div class="course-info">
                  <div class="course-name">{{ course.name }}</div>
                  <div class="course-desc">{{ course.desc }}</div>
                  <div class="course-code">{{ course.code }}</div>
                </div>
                <div class="course-link-icon">🔗</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧栏 -->
        <div class="exam-right-column">
          <!-- 图片上传区域 -->
          <div class="exam-panel">
            <div class="exam-panel-title">图片上传区域</div>
            <div class="exam-upload-area" @click="triggerExamFileUpload">
              <input 
                type="file" 
                ref="examFileInput" 
                style="display: none;" 
                accept="image/*"
                @change="handleExamFileChange"
              >
              <div class="exam-upload-placeholder">
                <span class="exam-upload-text">拖拽或点击上传图片</span>
              </div>
            </div>
            <div v-if="examUploadedImage" class="exam-uploaded-preview">
              <div class="preview-container">
                <img :src="examUploadedImage" alt="上传的图片" class="exam-preview-image">
                <button class="delete-btn" @click="deleteExamImage">
                  ×
                </button>
              </div>
              <div class="exam-image-question-input">
                <textarea 
                  v-model="examImageQuestion"
                  placeholder="请描述您的问题..."
                  class="exam-question-input"
                  rows="2"
                ></textarea>
              </div>
              <button class="exam-upload-btn" @click="submitExamImageQuestion">
                提交问题
              </button>
            </div>
            <button v-else class="exam-upload-btn" @click="triggerExamFileUpload">
              图片上传
            </button>
          </div>

          <!-- 备考答疑历史记录 -->
          <div class="exam-panel">
            <div class="exam-panel-title">备考答疑历史记录</div>
            <div class="exam-history-list">
              <div 
                v-for="(record, index) in examHistory" 
                :key="index"
                class="exam-history-item"
              >
                <div class="exam-history-header">
                  <div class="exam-history-question">Q: {{ record.question }}</div>
                  <div class="exam-history-actions">
                    <button 
                      class="more-btn" 
                      @click.stop="toggleExamDropdown(index)"
                      :class="{ active: activeExamDropdown === index }"
                    >
                      <span class="more-icon">⋮</span>
                    </button>
                    <div 
                      v-if="activeExamDropdown === index" 
                      class="dropdown-menu"
                    >
                      <button class="dropdown-item" @click.stop>
                        <span class="dropdown-icon">📌</span>
                        置顶
                      </button>
                      <button class="dropdown-item" @click.stop>
                        <span class="dropdown-icon">🔗</span>
                        分享
                      </button>
                      <button class="dropdown-item" @click.stop>
                        <span class="dropdown-icon">✏️</span>
                        重命名
                      </button>
                      <button class="dropdown-item" @click.stop>
                        <span class="dropdown-icon">⚠️</span>
                        举报
                      </button>
                      <button 
                        class="dropdown-item delete" 
                        @click.stop="confirmDeleteExamHistory(index)"
                      >
                        <span class="dropdown-icon">🗑️</span>
                        删除
                      </button>
                    </div>
                  </div>
                </div>
                <div class="exam-history-content" @click="selectExamHistory(record)">
                  <div class="exam-history-answer">A: {{ record.answer }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'
import { streamingApiService } from '../../api/streaming.js'
import { apiService } from '../../api/api.js'
import { processAiResponse } from '../../utils/markdownUtils'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 成功消息提示
const showSuccessMessage = (message) => {
  ElMessage({
    message: message,
    type: 'success',
    duration: 3000
  })
}

// 错误消息提示
const showErrorMessage = (message) => {
  ElMessage({
    message: message,
    type: 'error',
    duration: 5000,
    showClose: true
  })
}

// 警告消息提示
const showWarningMessage = (message) => {
  ElMessage({
    message: message,
    type: 'warning',
    duration: 3000,
    showClose: true
  })
}

// 信息消息提示
const showInfoMessage = (message) => {
  ElMessage({
    message: message,
    type: 'info',
    duration: 2000
  })
}

// 来源信息
const from = ref('')

// 加载状态
const loading = ref({
  books: false,
  chapters: false,
  courses: false,
  history: false,
  examSubjects: false,
  keyPoints: false,
  examPapers: false,
  examCourses: false,
  examHistory: false
})

// 错误信息
const error = ref({
  books: '',
  chapters: '',
  courses: '',
  history: '',
  examSubjects: '',
  keyPoints: '',
  examPapers: '',
  examCourses: '',
  examHistory: ''
})

// 从URL参数中获取来源信息
onMounted(async () => {
  from.value = route.query.from || ''
  
  // 如果是从学习页面跳转过来的，自动填充问题和选中的文本
  if (route.query.question) {
    question.value = route.query.question
  }
  if (route.query.bookId) {
    // 这里可以根据bookId设置选中的书籍
    // 示例：selectedBook.value = books.value.find(book => book.id === route.query.bookId)
  }
  
  // 初始化数据
  await fetchBooks()
  await fetchHistoryQuestions()
  await fetchExamSubjects()
  await fetchExamHistory()
  // 最后获取课程推荐，避免阻塞其他请求
  await fetchCourses()
})

// 页面卸载时清理资源
onUnmounted(() => {
  // 关闭SSE连接
  if (sseConnection.value) {
    sseConnection.value.close()
  }
  // 清除打字定时器
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value)
  }
})

// 返回学习页面
const goBackToStudy = () => {
  router.back()
}

// 激活的标签页
const activeTab = ref('智能答疑')

// 问题类型（text: 文字提问, image: 图片上传）
const questionType = ref('text')

// 文件输入引用
const fileInput = ref(null)

// 上传的图片
const uploadedImage = ref(null)
// 图片问题描述
const imageQuestion = ref('')
// 搜索查询
const searchQuery = ref('')
// 当前选中的书籍
const selectedBook = ref(null)
// 书籍数据
const books = ref([])
// 章节数据
const chapters = ref([])

// 展开的章节
const expandedChapters = ref([])

// 选中的章节
const selectedChapter = ref(null)

// 选中的子章节
const selectedSubChapter = ref(null)

// 获取书籍列表
const fetchBooks = async () => {
  loading.value.books = true
  error.value.books = ''
  try {
    const response = await apiService.qa.getBooks()
    console.log('Books API response:', response)
    if (response && response.data) {
      books.value = response.data || []
      console.log('Books data:', books.value)
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      books.value = response || []
      console.log('Books data (non-standard format):', books.value)
    }
  } catch (err) {
    console.error('获取书籍列表失败:', err)
    const errorMsg = err.errorMessage || '获取书籍列表失败'
    error.value.books = errorMsg
    showErrorMessage(errorMsg)
  } finally {
    loading.value.books = false
  }
}

// 获取章节列表
const fetchChapters = async (bookId) => {
  loading.value.chapters = true
  error.value.chapters = ''
  try {
    const response = await apiService.qa.getChapters(bookId)
    console.log('Chapters API response:', response)
    if (response && response.data) {
      chapters.value = response.data || []
      console.log('Chapters data:', chapters.value)
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      chapters.value = response || []
      console.log('Chapters data (non-standard format):', chapters.value)
    }
  } catch (err) {
    console.error('获取章节列表失败:', err)
    const errorMsg = err.errorMessage || '获取章节列表失败'
    error.value.chapters = errorMsg
    showErrorMessage(errorMsg)
  } finally {
    loading.value.chapters = false
  }
}

// 返回首页
const goHome = () => {
  router.push('/')
}

// 选择书籍
const selectBook = (book) => {
  selectedBook.value = book
  // 重置展开状态
  expandedChapters.value = []
  // 根据选中的书籍更新章节数据
  if (book.id) {
    fetchChapters(book.id)
  } else {
    chapters.value = []
  }
}

// 切换章节展开/折叠
const toggleChapter = (chapterId) => {
  const index = expandedChapters.value.indexOf(chapterId)
  if (index > -1) {
    // 折叠章节
    expandedChapters.value.splice(index, 1)
  } else {
    // 展开章节
    expandedChapters.value.push(chapterId)
  }
}

// 选择章节
const selectChapter = (chapter) => {
  selectedChapter.value = chapter
  showInfoMessage(`已选择章节：${chapter.title}`)
  // 这里可以添加章节相关的逻辑，比如加载章节内容等
}

// 获取默认封面 - 使用本地 SVG 数据 URI
const getDefaultCover = (subject) => {
  try {
    const svgConfigs = {
      'MATH': { bg: '#4A90D9', text: '数学' },
      'PHYSICS': { bg: '#50C878', text: '物理' },
      'CHEMISTRY': { bg: '#FF6B6B', text: '化学' },
      'CHINESE': { bg: '#9B59B6', text: '语文' },
      'ENGLISH': { bg: '#F39C12', text: '英语' },
      'DEFAULT': { bg: '#6B7280', text: '书籍' }
    }
    const config = svgConfigs[subject] || svgConfigs['DEFAULT']
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="40" height="56" viewBox="0 0 40 56">
      <rect width="40" height="56" fill="${config.bg}"/>
      <text x="20" y="30" font-family="Arial, sans-serif" font-size="12" fill="white" text-anchor="middle" dominant-baseline="middle">${config.text}</text>
    </svg>`
    return `data:image/svg+xml,${encodeURIComponent(svg)}`
  } catch (error) {
    console.error('生成默认封面失败:', error)
    // 返回一个简单的默认封面
    return `data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="40" height="56" viewBox="0 0 40 56"%3E%3Crect width="40" height="56" fill="%236B7280"/%3E%3Ctext x="20" y="30" font-family="Arial, sans-serif" font-size="12" fill="white" text-anchor="middle" dominant-baseline="middle"%3E书籍%3C/text%3E%3C/svg%3E`
  }
}

// 图片加载错误处理 - 用于教材封面
const handleBookCoverImageError = (event) => {
  try {
    const img = event.target
    if (!img) return
    
    // 直接使用DEFAULT作为默认主题，确保总是能生成默认封面
    img.src = getDefaultCover('DEFAULT')
    
    // 确保图片显示
    img.style.display = 'block'
  } catch (error) {
    console.error('处理图片错误失败:', error)
  }
}

// 问答相关
const question = ref('')
const aiAnswer = ref('')
const showAnswer = ref(false)
const historyQuestions = ref([])
const historySearchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(6)
const activeDropdown = ref(null)

// 获取历史问题
const fetchHistoryQuestions = async () => {
  loading.value.history = true
  error.value.history = ''
  try {
    // 从localStorage获取userId
    let userId = localStorage.getItem('userId')
    
    // 如果localStorage中没有userId，从authStore获取
    if (!userId) {
      const authStore = useAuthStore()
      userId = authStore.user?.id || authStore.userId
    }
    
    // 确保userId是数字类型
    userId = Number(userId || '')
    
    if (!userId || isNaN(userId)) {
      // 用户未登录，返回空数组
      historyQuestions.value = []
      loading.value.history = false
      return
    }
    
    console.log('获取历史记录，userId:', userId)
    const response = await apiService.qa.getHistory({ params: { userId } })
    console.log('历史记录响应:', response)
    if (response && response.data && Array.isArray(response.data)) {
      historyQuestions.value = response.data
      console.log('历史记录数据:', historyQuestions.value)
    } else if (response && Array.isArray(response)) {
      // 如果响应不是标准Result格式，直接使用
      historyQuestions.value = response
      console.log('历史记录数据（非标准格式）:', historyQuestions.value)
    } else {
      // 如果响应不是数组，设置空数组
      historyQuestions.value = []
    }
  } catch (err) {
    console.error('获取历史问题失败:', err)
    const errorMsg = err.errorMessage || '获取历史问题失败'
    error.value.history = errorMsg
    showErrorMessage('获取历史记录失败: ' + (err.errorMessage || err.message || '请稍后重试'))
    // 设置空数组以避免界面错误
    historyQuestions.value = []
  } finally {
    loading.value.history = false
  }
}

// SSE 连接管理
const sseConnection = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')
const connectionStatus = ref('idle') // idle, connecting, connected, error
const currentQuestion = ref('') // 保存当前问题，用于重新生成
const reconnectAttempts = ref(0)
const maxReconnectAttempts = 3
const reconnectDelay = 1000 // 1秒

// 打字机效果
const typedAnswer = ref('')
const typingSpeed = ref(3) // 打字速度（毫秒/字符），3ms实现超快打字效果
const typingTimeout = ref(null)
const fullAnswer = ref('')
const processedAnswer = ref('')
const isTyping = ref(false)
const isPaused = ref(false)

// 打字机效果函数
const startTyping = (text) => {
  // 如果已经在打字，更新fullAnswer并继续打字
  if (isTyping.value) {
    // 只更新fullAnswer，不重新开始打字
    fullAnswer.value = text
    return
  }
  
  // 只在第一次调用时重置typedAnswer
  if (!typedAnswer.value) {
    typedAnswer.value = ''
  }
  
  fullAnswer.value = text
  isTyping.value = true
  // 从当前typedAnswer的长度开始打字
  let index = typedAnswer.value.length
  
  const typeNext = () => {
    if (index < fullAnswer.value.length) {
      // 逐字添加文本
      typedAnswer.value += fullAnswer.value.charAt(index)
      index++
      // 自动滚动到最新内容
      scrollToBottom()
      typingTimeout.value = setTimeout(typeNext, typingSpeed.value)
    } else {
      // 检查是否还有新的内容（流式数据可能会继续到达）
      // 等待一小段时间，看看是否有新数据
      typingTimeout.value = setTimeout(() => {
        // 如果fullAnswer.value长度增加了，继续打字
        if (index < fullAnswer.value.length) {
          typeNext()
        } else {
          // 确实没有更多内容了，结束打字
          isTyping.value = false
          // 处理完整回答，包括Markdown和数学公式，使用processMathText处理数学文本
          processedAnswer.value = processMathText(fullAnswer.value)
        }
      }, 100) // 100ms的等待时间
    }
  }
  
  typeNext()
}

// 取消生成
const cancelGeneration = () => {
  // 停止打字机效果
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value)
    typingTimeout.value = null
  }
  isTyping.value = false
  
  // 关闭SSE连接
  if (sseConnection.value) {
    try {
      if (typeof sseConnection.value.close === 'function') {
        sseConnection.value.close()
      } else if (sseConnection.value.eventSource) {
        sseConnection.value.eventSource.close()
      }
    } catch (error) {
      console.error('Error closing SSE connection:', error)
    }
    sseConnection.value = null
  }
  
  // 重置重连计数器
  reconnectAttempts.value = 0
  
  isLoading.value = false
  connectionStatus.value = 'disconnected'
  
  // 如果有内容，处理已生成的部分
  if (fullAnswer.value) {
    processedAnswer.value = processMathText(fullAnswer.value)
  }
}

// 自动滚动到最新内容
const scrollToBottom = () => {
  const answerElement = document.querySelector('.answer-text')
  if (answerElement) {
    answerElement.scrollTop = answerElement.scrollHeight
  }
}

// 提交问题
const submitQuestion = async () => {
  try {
    // 使用当前问题（如果有）或输入框中的问题
    let questionText = currentQuestion.value || question.value.trim()
    if (!questionText) {
      showErrorMessage('请输入问题')
      return
    }
    
    // 对输入进行sanitize处理，防止XSS攻击
    questionText = sanitizeInput(questionText)
    
    // 保存当前问题用于重新生成
    currentQuestion.value = questionText
    
    // 重置状态
    showAnswer.value = true
    isLoading.value = true
    errorMessage.value = ''
    connectionStatus.value = 'connecting'
    typedAnswer.value = ''
    fullAnswer.value = ''
    processedAnswer.value = ''
    
    // 显示加载提示
    showInfoMessage('正在提交问题，请稍候...')
    
    // 取消之前的连接
    if (sseConnection.value) {
      try {
        if (typeof sseConnection.value.close === 'function') {
          sseConnection.value.close()
        } else if (sseConnection.value.eventSource) {
          sseConnection.value.eventSource.close()
        }
      } catch (error) {
        console.error('Error closing SSE connection:', error)
      }
    }
    
    // 构建请求数据
    const requestData = {
      question: questionText,
      subject: selectedBook.value?.name || selectedBook.value?.title || ''
    }
    
    // 使用流式POST请求
    const attemptConnection = () => {
      streamingApiService.stream.askQuestionStream(
        requestData,
        (message) => {
          if (message.error) {
            errorMessage.value = message.error
            connectionStatus.value = 'error'
            isLoading.value = false
            showErrorMessage('AI回答生成失败: ' + message.error)
          } else {
            // 先处理content，再处理completed
            if (message.content) {
              // 累积数据
              fullAnswer.value += message.content
              // 开始打字效果
              if (!isTyping.value) {
                isLoading.value = false
                startTyping(fullAnswer.value)
              }
            }
            if (message.completed) {
                connectionStatus.value = 'connected'
                isLoading.value = false
                // 重置重连计数器
                reconnectAttempts.value = 0
              }
            }
          },
          () => {
            // 完成回调
            console.log('Stream completed')
            isLoading.value = false
            connectionStatus.value = 'connected'
            // 重置重连计数器
            reconnectAttempts.value = 0
            // 确保有内容时处理完整回答，使用processMathText处理数学文本
            if (fullAnswer.value) {
              processedAnswer.value = processMathText(fullAnswer.value)
              
              // 保存处理后的文本到后端
              const questionText = currentQuestion.value || question.value.trim()
              const requestData = {
                question: questionText,
                processedText: fullAnswer.value,
                subject: selectedBook.value?.name || ''
              }
              
              // 调用API保存处理后的文本
              console.log('开始保存处理后的文本:', requestData);
              
              // 检查用户是否登录
              const authStore = useAuthStore();
              if (!authStore.isLoggedIn) {
                showErrorMessage('用户未登录，无法保存历史记录');
                return;
              }
              
              apiService.qa.saveProcessedText(requestData)
                .then((response) => {
                  console.log('处理后的文本保存成功，响应:', response)
                  
                  // 显示成功提示
                  showSuccessMessage('历史记录保存成功')
                  
                  // 重新获取历史记录，确保数据与后端一致
                  fetchHistoryQuestions()
                })
                .catch(error => {
                  console.error('保存处理后的文本失败:', error)
                  console.error('错误详情:', error.response || error)
                  showErrorMessage('保存历史记录失败: ' + (error.errorMessage || error.message || '未知错误'))
                })
            }
          },
      (error) => {
        // 错误回调
        console.error('Stream error:', error)
        
        // 尝试重连
        if (reconnectAttempts.value < maxReconnectAttempts) {
          reconnectAttempts.value++
          console.log(`尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`)
          showInfoMessage(`连接失败，正在尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`)
          
          // 延迟后重连
          setTimeout(attemptConnection, reconnectDelay * reconnectAttempts.value)
        } else {
          // 重连失败
          let errorMsg = error.message || '连接失败，请重试'
          
          // 针对不同错误类型提供更友好的提示
          if (errorMsg.includes('500')) {
            errorMsg = '服务器内部错误，请稍后再试'
          } else if (errorMsg.includes('401')) {
            errorMsg = '请先登录'
            // 跳转到登录页面
            setTimeout(() => {
              window.location.href = '/login'
            }, 2000)
          } else if (errorMsg.includes('404')) {
            errorMsg = '请求的接口不存在'
          } else if (errorMsg.includes('Network')) {
            errorMsg = '网络连接失败，请检查网络设置'
          }
          
          errorMessage.value = errorMsg
          connectionStatus.value = 'error'
          isLoading.value = false
          
          // 显示错误提示
          showErrorMessage('连接失败: ' + errorMsg)
          // 重置重连计数器
          reconnectAttempts.value = 0
        }
      }
    )
    }
    
    // 开始连接
    attemptConnection()
  

} catch (error) {
  console.error('提交问题失败:', error)
  showErrorMessage('提交问题失败: ' + (error.errorMessage || error.message || '请稍后重试'))
  isLoading.value = false
  connectionStatus.value = 'error'
}
}

// 过滤后的历史记录
const filteredHistory = computed(() => {
  if (!Array.isArray(historyQuestions.value)) {
    return []
  }
  if (!historySearchQuery.value) {
    return historyQuestions.value
  }
  const query = historySearchQuery.value.toLowerCase()
  return historyQuestions.value.filter(item => 
    item.question.toLowerCase().includes(query) || 
    (item.answer && item.answer.toLowerCase().includes(query))
  )
})

// 分页后的历史记录
const paginatedHistory = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredHistory.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredHistory.value.length / pageSize.value)
})

// 时间格式化方法
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 处理历史记录搜索
const handleHistorySearch = () => {
  currentPage.value = 1 // 搜索时重置到第一页
}

// 按时间范围筛选历史记录
const filterHistoryByTime = (timeRange) => {
  currentPage.value = 1
  // 这里可以添加时间范围筛选逻辑
  showInfoMessage(`已筛选 ${timeRange} 的历史记录`)
}

// 按学科筛选历史记录
const filterHistoryBySubject = (subject) => {
  currentPage.value = 1
  // 这里可以添加学科筛选逻辑
  showInfoMessage(`已筛选 ${subject} 的历史记录`)
}

// 选择历史问题
const selectHistory = (item) => {
  question.value = item.question
  fullAnswer.value = item.answer
  showAnswer.value = true
  // 直接渲染HTML内容，不再触发打字机效果
  processedAnswer.value = processMathText(item.answer)
  isTyping.value = false
}

// 切换下拉菜单
const toggleDropdown = (id) => {
  if (activeDropdown.value === id) {
    activeDropdown.value = null
  } else {
    activeDropdown.value = id
  }
}

// 确认删除历史记录
const confirmDeleteHistory = (id) => {
  ElMessageBox.confirm('确定要删除这条历史记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteHistory(id)
  }).catch(() => {
    // 取消删除
  })
  activeDropdown.value = null
}

// 删除历史记录
const deleteHistory = async (id) => {
  try {
    // 显示加载状态
    loading.value.history = true
    
    // 确保id是数字类型
    const numericId = typeof id === 'string' ? parseInt(id) : id
    
    // 调用API删除记录
    await apiService.qa.deleteHistory(numericId)
    
    // 从本地数组中删除记录
    const index = historyQuestions.value.findIndex(item => item.id === id)
    if (index > -1) {
      historyQuestions.value.splice(index, 1)
    }
    
    // 显示成功消息
    showSuccessMessage('历史记录已删除')
  } catch (error) {
    console.error('删除历史记录失败:', error)
    // 显示错误消息
    showErrorMessage('删除历史记录失败: ' + (error.errorMessage || error.message || '请稍后重试'))
  } finally {
    // 隐藏加载状态
    loading.value.history = false
  }
}

// 课程数据
const courses = ref([])

// 获取推荐课程
const fetchCourses = async () => {
  loading.value.courses = true
  error.value.courses = ''
  try {
    console.log('开始获取推荐课程')
    const response = await apiService.qa.getRecommendedCourses(5)
    console.log('课程响应:', response)
    if (response && response.data) {
      courses.value = response.data || []
      console.log('课程数据:', courses.value)
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      courses.value = response || []
      console.log('课程数据（非标准格式）:', courses.value)
    }
  } catch (err) {
    console.error('获取推荐课程失败:', err)
    error.value.courses = err.errorMessage || '获取推荐课程失败'
  } finally {
    loading.value.courses = false
  }
}

// 打开课程链接
const openCourseLink = (link) => {
  if (link) {
    window.open(link, '_blank')
  }
}

// 处理图片加载错误
const handleImageError = (event) => {
  if (event && event.target) {
    event.target.style.display = 'none';
    const placeholder = event.target.nextElementSibling;
    if (placeholder) {
      placeholder.style.display = 'flex';
    }
  }
}

// 处理图片加载成功
const handleImageLoad = (event) => {
  if (event && event.target) {
    event.target.style.display = 'block';
    const placeholder = event.target.nextElementSibling;
    if (placeholder) {
      placeholder.style.display = 'none';
    }
  }
}

// 格式化视频时长
const formatDuration = (seconds) => {
  if (!seconds || seconds < 0) return '';
  
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
}

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万';
  }
  return num.toString();
}

// 触发文件上传
const triggerFileUpload = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 压缩图片
    compressImage(file, 0.7, 1024, 768).then(compressedImage => {
      uploadedImage.value = compressedImage
    }).catch(error => {
      console.error('图片压缩失败:', error)
      // 压缩失败时使用原图
      const reader = new FileReader()
      reader.onload = (e) => {
        uploadedImage.value = e.target.result
      }
      reader.readAsDataURL(file)
    })
  }
}

// 图片压缩函数
const compressImage = (file, quality, maxWidth, maxHeight) => {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const img = new Image()
    
    img.onload = () => {
      // 计算压缩后的尺寸
      let width = img.width
      let height = img.height
      
      if (width > maxWidth) {
        height = (height * maxWidth) / width
        width = maxWidth
      }
      
      if (height > maxHeight) {
        width = (width * maxHeight) / height
        height = maxHeight
      }
      
      canvas.width = width
      canvas.height = height
      
      // 绘制压缩后的图片
      ctx.drawImage(img, 0, 0, width, height)
      
      // 将canvas转换为base64
      canvas.toBlob((blob) => {
        if (blob) {
          const reader = new FileReader()
          reader.onload = (e) => {
            resolve(e.target.result)
          }
          reader.readAsDataURL(blob)
        } else {
          reject(new Error('图片压缩失败'))
        }
      }, 'image/jpeg', quality)
    }
    
    img.onerror = () => {
      reject(new Error('图片加载失败'))
    }
    
    img.src = URL.createObjectURL(file)
  })
}

// 通用图片提交函数（流式）
const submitImageQuestion = async (image, question, isExam = false, subjectId = null) => {
  if (!image) {
    showErrorMessage('请先上传图片')
    return
  }
  
  isLoading.value = true
  errorMessage.value = ''
  fullAnswer.value = ''
  processedAnswer.value = ''
  
  try {
    // 显示加载提示
    showInfoMessage('正在处理图片，请稍候...')
    
    // 准备表单数据
    const formData = new FormData()
    const sanitizedQuestion = sanitizeInput(question || '基于上传的图片')
    formData.append('question', sanitizedQuestion)
    
    // 如果是考试模式，添加科目ID
    if (isExam && subjectId) {
      formData.append('subjectId', subjectId)
    }
    
    // 假设 image 是 base64 格式，需要转换为 Blob
    if (image) {
      try {
        const blob = await fetch(image).then(r => r.blob())
        formData.append('image', blob, isExam ? 'exam-question.jpg' : 'question.jpg')
        console.log('图片转换为Blob成功，大小:', blob.size)
        console.log('FormData内容:', [...formData.entries()])
      } catch (error) {
        console.error('图片转换为Blob失败:', error)
        showErrorMessage('图片处理失败，请重试')
        isLoading.value = false
        return
      }
    }
    
    // 使用流式接口
    streamingApiService.stream.askImageQuestionStream(
      formData,
      (message) => {
        if (message.error) {
          errorMessage.value = message.error
          isLoading.value = false
          showErrorMessage('图片问答生成失败: ' + message.error)
        } else {
          // 处理流式数据
          if (message.content) {
            // 累积数据
            fullAnswer.value += message.content
            // 开始打字效果
            if (!isTyping.value) {
              isLoading.value = false
              startTyping(fullAnswer.value)
            }
          }
          if (message.completed) {
            isLoading.value = false
          }
        }
      },
      () => {
        // 完成回调
        console.log('图片问答流式响应完成')
        isLoading.value = false
        
        // 确保有内容时处理完整回答
        if (fullAnswer.value) {
          processedAnswer.value = processMathText(fullAnswer.value)
          
          // 根据模式更新相应的回答和历史记录
          if (isExam) {
            examAnswer.value = processedAnswer.value
            
            // 添加到考试历史记录
            examHistory.value.unshift({ 
              question: `[图片提问] ${question || '基于上传的图片'}`, 
              answer: fullAnswer.value 
            })
            if (examHistory.value.length > 5) {
              examHistory.value.pop()
            }
          } else {
            // 设置处理后的回答，用于显示
            processedAnswer.value = processedAnswer.value
            
            // 添加到历史记录
            historyQuestions.value.unshift({ 
              question: `[图片提问] ${question || '基于上传的图片'}`, 
              answer: fullAnswer.value 
            })
            if (historyQuestions.value.length > 5) {
              historyQuestions.value.pop()
            }
            
            // 显示回答
            showAnswer.value = true
            
            // 保存处理后的文本到后端
            const requestData = {
              question: `[图片提问] ${question || '基于上传的图片'}`,
              processedText: fullAnswer.value,
              subject: selectedBook.value?.name || ''
            }
            
            // 调用API保存处理后的文本
            apiService.qa.saveProcessedText(requestData)
              .then((response) => {
                console.log('图片问答处理后的文本保存成功:', response)
                fetchHistoryQuestions()
              })
              .catch(error => {
                console.error('保存图片问答历史记录失败:', error)
              })
          }
          
          showSuccessMessage('图片问题处理成功')
        }
      },
      (error) => {
        // 错误回调
        console.error('图片问答流式请求失败:', error)
        const errorMsg = error.message || '提交图片问题失败'
        errorMessage.value = errorMsg
        showErrorMessage(errorMsg)
        isLoading.value = false
      }
    )
  } catch (err) {
    const errorMsg = err.errorMessage || '提交图片问题失败'
    errorMessage.value = errorMsg
    showErrorMessage(errorMsg)
    isLoading.value = false
  }
}

// 提交普通图片问题
const submitRegularImageQuestion = async () => {
  await submitImageQuestion(uploadedImage.value, imageQuestion.value, false)
}

// 提交考试图片问题
const submitExamImageQuestion = async () => {
  await submitImageQuestion(examUploadedImage.value, examImageQuestion.value, true, selectedExamSubject.value)
}

// 删除上传的图片
const deleteImage = () => {
  uploadedImage.value = null
  imageQuestion.value = ''
}

// ==================== 考研/考证问答相关 ====================
// 考试科目
const examSubjects = ref([])

// 选中的考试科目
const selectedExamSubject = ref('builder')

// 科目搜索
const subjectSearchQuery = ref('')

// 显示科目下拉菜单
const showSubjectDropdown = ref(false)

// 过滤后的科目
const filteredSubjects = computed(() => {
  if (!subjectSearchQuery.value) {
    return examSubjects.value
  }
  const query = subjectSearchQuery.value.toLowerCase()
  return examSubjects.value.filter(subject => 
    subject.name.toLowerCase().includes(query)
  )
})

// 获取考试科目
const fetchExamSubjects = async () => {
  loading.value.examSubjects = true
  error.value.examSubjects = ''
  try {
    const response = await apiService.qa.getExamSubjects()
    console.log('考试科目响应:', response)
    if (response && response.data && Array.isArray(response.data)) {
      examSubjects.value = response.data
      console.log('考试科目数据:', examSubjects.value)
      // 如果有数据，设置默认选中的科目
      if (response.data.length > 0) {
        selectedExamSubject.value = response.data[0].id
        // 获取默认科目的高频考点
        await fetchKeyPoints(response.data[0].id)
        // 获取默认科目的真题和课程
        await fetchExamPapers(response.data[0].id)
        await fetchExamCourses(response.data[0].id)
      }
    } else if (response && Array.isArray(response)) {
      // 如果响应不是标准Result格式，直接使用
      examSubjects.value = response
      console.log('考试科目数据（非标准格式）:', examSubjects.value)
      // 如果有数据，设置默认选中的科目
      if (response.length > 0) {
        selectedExamSubject.value = response[0].id
        // 获取默认科目的高频考点
        await fetchKeyPoints(response[0].id)
        // 获取默认科目的真题和课程
        await fetchExamPapers(response[0].id)
        await fetchExamCourses(response[0].id)
      }
    }
  } catch (err) {
    console.error('获取考试科目失败:', err)
    error.value.examSubjects = err.errorMessage || '获取考试科目失败'
  } finally {
    loading.value.examSubjects = false
  }
}

// 选择科目
const selectSubject = async (subject) => {
  selectedExamSubject.value = subject.id
  subjectSearchQuery.value = ''
  showSubjectDropdown.value = false
  // 更新高频考点
  await fetchKeyPoints(subject.id)
  // 重置选中的考点
  selectedKeyPoint.value = null
  // 获取相关的真题和课程
  await fetchExamPapers(subject.id)
  await fetchExamCourses(subject.id)
}

// 切换科目下拉菜单
const toggleSubjectDropdown = () => {
  showSubjectDropdown.value = !showSubjectDropdown.value
}

// 获取选中的科目名称
const getSelectedSubjectName = () => {
  if (!Array.isArray(examSubjects.value)) {
    return '请选择科目'
  }
  const subject = examSubjects.value.find(s => s.id === selectedExamSubject.value)
  return subject ? subject.name : '请选择科目'
}

// 高频考点
const keyPoints = ref([])

// 选中的考点
const selectedKeyPoint = ref(null)

// 考点提问
const examQuestion = ref('')

// 考点分类
const categories = ref([
  { id: 'all', name: '全部', count: 0 },
  { id: 'easy', name: '简单', count: 0 },
  { id: 'medium', name: '中等', count: 0 },
  { id: 'hard', name: '困难', count: 0 }
])

// 选中的分类
const selectedCategory = ref('all')

// 视图模式
const viewMode = ref('list') // list or card

// 展开的考点
const expandedKeyPoints = ref([])

// 过滤后的考点
const filteredKeyPoints = computed(() => {
  let filtered = keyPoints.value
  if (selectedCategory.value !== 'all') {
    filtered = filtered.filter(point => point.difficulty === selectedCategory.value)
  }
  return filtered
})

// 切换视图模式
const toggleViewMode = () => {
  viewMode.value = viewMode.value === 'list' ? 'card' : 'list'
}

// 选择分类
const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
}

// 切换收藏状态
const toggleFavorite = (point) => {
  point.isFavorite = !point.isFavorite
}

// 切换展开状态
const toggleExpand = (pointId) => {
  const index = expandedKeyPoints.value.indexOf(pointId)
  if (index > -1) {
    expandedKeyPoints.value.splice(index, 1)
  } else {
    expandedKeyPoints.value.push(pointId)
    // 记录学习时间
    recordStudyTime(pointId)
  }
}

// 更新分类计数
const updateCategoryCounts = () => {
  const counts = {
    all: keyPoints.value.length,
    easy: 0,
    medium: 0,
    hard: 0
  }
  keyPoints.value.forEach(point => {
    if (point.difficulty) {
      counts[point.difficulty]++
    }
  })
  categories.value.forEach(category => {
    category.count = counts[category.id]
  })
}

// 学习进度
const studyProgress = ref(0)

// 记录学习时间
const recordStudyTime = (pointId) => {
  const point = keyPoints.value.find(p => p.id === pointId)
  if (point) {
    point.lastStudied = new Date().toISOString()
    point.studyCount = (point.studyCount || 0) + 1
    updateStudyProgress()
  }
}

// 更新学习进度
const updateStudyProgress = () => {
  const studiedCount = keyPoints.value.filter(point => point.studyCount > 0).length
  studyProgress.value = keyPoints.value.length > 0 ? Math.round((studiedCount / keyPoints.value.length) * 100) : 0
}

// 按重要性排序
const sortByImportance = () => {
  keyPoints.value.sort((a, b) => {
    // 优先排序收藏的考点
    if (a.isFavorite && !b.isFavorite) return -1
    if (!a.isFavorite && b.isFavorite) return 1
    // 然后按考察频率排序
    return (b.frequency || 0) - (a.frequency || 0)
  })
  updateCategoryCounts()
}

// 按难度排序
const sortByDifficulty = () => {
  const difficultyOrder = { hard: 0, medium: 1, easy: 2 }
  keyPoints.value.sort((a, b) => {
    return difficultyOrder[a.difficulty || 'medium'] - difficultyOrder[b.difficulty || 'medium']
  })
  updateCategoryCounts()
}

// 按学习时间排序
const sortByStudyTime = () => {
  keyPoints.value.sort((a, b) => {
    const timeA = a.lastStudied ? new Date(a.lastStudied).getTime() : 0
    const timeB = b.lastStudied ? new Date(b.lastStudied).getTime() : 0
    return timeB - timeA
  })
  updateCategoryCounts()
}

// AI回答
const examAnswer = ref('')

// 选中的真题
const selectedPapers = ref([])

// 网课推荐数据
const examCourses = ref([])

// 备考历史记录
const examHistory = ref([])
// 备考历史记录下拉菜单状态
const activeExamDropdown = ref(null)

// 切换备考历史记录下拉菜单
const toggleExamDropdown = (index) => {
  if (activeExamDropdown.value === index) {
    activeExamDropdown.value = null
  } else {
    activeExamDropdown.value = index
  }
}

// 确认删除备考历史记录
const confirmDeleteExamHistory = (index) => {
  ElMessageBox.confirm('确定要删除这条历史记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteExamHistory(index)
  }).catch(() => {
    // 取消删除
  })
  activeExamDropdown.value = null
}

// 删除备考历史记录
const deleteExamHistory = async (index) => {
  try {
    // 显示加载状态
    loading.value.examHistory = true
    
    // 获取要删除的记录ID
    const record = examHistory.value[index]
    if (!record || !record.id) {
      showErrorMessage('无效的历史记录')
      return
    }
    
    // 调用API删除记录
    await apiService.qa.deleteExamQaHistory(record.id)
    
    // 从本地数组中删除记录
    if (index >= 0 && index < examHistory.value.length) {
      examHistory.value.splice(index, 1)
    }
    
    // 显示成功消息
    showSuccessMessage('历史记录已删除')
  } catch (error) {
    console.error('删除备考历史记录失败:', error)
    // 显示错误消息
    showErrorMessage('删除历史记录失败: ' + (error.errorMessage || error.message || '请稍后重试'))
  } finally {
    // 隐藏加载状态
    loading.value.examHistory = false
  }
}

// 获取高频考点
const fetchKeyPoints = async (subjectId) => {
  loading.value.keyPoints = true
  error.value.keyPoints = ''
  try {
    console.log('开始获取高频考点，科目ID:', subjectId)
    const response = await apiService.qa.getKeyPoints(subjectId)
    console.log('高频考点响应:', response)
    if (response && response.data) {
      // 为每个考点添加必要的属性
      keyPoints.value = response.data.map(point => ({
        ...point,
        id: point.id || Math.random().toString(36).substr(2, 9),
        difficulty: point.difficulty || 'medium',
        frequency: point.frequency || 50,
        isFavorite: false,
        content: point.content,
        example: point.example
      }))
      console.log('高频考点数据:', keyPoints.value)
      // 更新分类计数
      updateCategoryCounts()
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      keyPoints.value = response.map(point => ({
        ...point,
        id: point.id || Math.random().toString(36).substr(2, 9),
        difficulty: point.difficulty || 'medium',
        frequency: point.frequency || 50,
        isFavorite: false,
        content: point.content,
        example: point.example
      }))
      console.log('高频考点数据（非标准格式）:', keyPoints.value)
      // 更新分类计数
      updateCategoryCounts()
    }
  } catch (err) {
    console.error('获取高频考点失败:', err)
    error.value.keyPoints = err.errorMessage || '获取高频考点失败'
  } finally {
    loading.value.keyPoints = false
  }
}

// 获取真题
const fetchExamPapers = async (subjectId) => {
  loading.value.examPapers = true
  error.value.examPapers = ''
  try {
    console.log('开始获取真题，科目ID:', subjectId)
    const response = await apiService.qa.getExamPapers(subjectId)
    console.log('真题响应:', response)
    if (response && response.data) {
      selectedPapers.value = response.data.map(paper => ({
        ...paper,
        isFavorite: false
      }))
      console.log('真题数据:', selectedPapers.value)
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      selectedPapers.value = response.map(paper => ({
        ...paper,
        isFavorite: false
      }))
      console.log('真题数据（非标准格式）:', selectedPapers.value)
    }
  } catch (err) {
    console.error('获取真题失败:', err)
    error.value.examPapers = err.errorMessage || '获取真题失败'
  } finally {
    loading.value.examPapers = false
  }
}

// 刷新推荐
const refreshRecommendations = async () => {
  if (selectedExamSubject.value) {
    await fetchExamPapers(selectedExamSubject.value)
  }
}

// AI生成真题推荐
const generateAiRecommendations = async () => {
  if (selectedExamSubject.value) {
    loading.value.examPapers = true
    try {
      showInfoMessage('AI正在为您生成真题推荐，请稍候...')
      const response = await apiService.qa.generateExamPapersByAI(selectedExamSubject.value)
      if (response && response.data) {
        selectedPapers.value = response.data.map(paper => ({
          ...paper,
          isFavorite: false
        }))
        showSuccessMessage('AI真题推荐生成成功')
      }
    } catch (error) {
      console.error('AI生成真题推荐失败:', error)
      showErrorMessage('AI生成真题推荐失败: ' + (error.errorMessage || error.message || '请稍后重试'))
    } finally {
      loading.value.examPapers = false
    }
  } else {
    showWarningMessage('请先选择考试科目')
  }
}

// 切换真题收藏状态
const togglePaperFavorite = (paper) => {
  paper.isFavorite = !paper.isFavorite
  // 这里可以添加收藏逻辑，比如保存到本地存储或调用API
  if (paper.isFavorite) {
    console.log('收藏真题:', paper.paperName)
  } else {
    console.log('取消收藏真题:', paper.paperName)
  }
}

// 查看真题详情
const viewPaperDetails = (paper) => {
  console.log('查看真题详情:', paper)
  // 显示详细的真题信息弹窗
  ElMessageBox.alert(
    `<div style="text-align: left;">
      <h3 style="margin-bottom: 16px;">${paper.year}年${paper.paperName}</h3>
      <div style="margin-bottom: 8px;"><strong>科目:</strong> ${paper.subjectName || paper.subject}</div>
      <div style="margin-bottom: 8px;"><strong>考试时长:</strong> ${paper.duration}分钟</div>
      <div style="margin-bottom: 8px;"><strong>总分:</strong> ${paper.totalScore}分</div>
      <div style="margin-bottom: 8px;"><strong>题目数量:</strong> ${paper.questionCount || 0}题</div>
      <div style="margin-bottom: 8px;"><strong>难度:</strong> ${paper.difficulty} (1-5，1最简单，5最难)</div>
      <div style="margin-bottom: 8px;"><strong>来源:</strong> ${paper.source || '未知'}</div>
      <div style="margin-top: 16px;"><strong>推荐理由:</strong> ${paper.recommendReason || '暂无推荐理由'}</div>
    </div>`,
    '真题详情',
    {
      confirmButtonText: '关闭',
      dangerouslyUseHTMLString: true,
      customClass: 'paper-details-dialog',
      size: 'large'
    }
  )
}

// 开始练习
const startPractice = async (paper) => {
  console.log('开始练习:', paper)
  // 显示开始练习确认弹窗
  ElMessageBox.confirm(
    `确定要开始练习 ${paper.year}年${paper.paperName} 吗？`,
    '开始练习',
    {
      confirmButtonText: '开始',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    showInfoMessage(`正在准备 ${paper.paperName} 的练习...`)
    
    try {
      // 调用API获取真实的练习题目
      const response = await apiService.qa.getExamPaperQuestions(paper.id)
      
      if (response && response.data && response.data.length > 0) {
        // 如果有真实的题目数据，跳转到练习页面
        const exercises = response.data.map((question, index) => ({
          id: question.id || `q-${index}`,
          question: question.content || question.question || question.title,
          type: question.type || '选择题',
          options: question.options || ['选项A', '选项B', '选项C', '选项D'],
          correctAnswer: question.correctAnswer || 0,
          analysis: question.analysis || question.explanation || '暂无解析',
          difficulty: question.difficulty || 'medium',
          difficultyText: getDifficultyText(question.difficulty),
          score: question.score || 10,
          knowledgePoint: question.knowledgePoint || paper.subjectName
        }))
        
        // 跳转到练习页面，传递题目数据
        router.push({
          path: '/practice',
          query: {
            exercises: JSON.stringify(exercises),
            paperId: paper.id,
            paperName: paper.paperName,
            subjectName: paper.subjectName
          }
        })
        
        showSuccessMessage(`已开始 ${paper.paperName} 的练习`)
      } else {
        // 如果没有获取到题目，使用默认题目
        const defaultExercises = generateDefaultExercises(paper)
        
        router.push({
          path: '/practice',
          query: {
            exercises: JSON.stringify(defaultExercises),
            paperId: paper.id,
            paperName: paper.paperName,
            subjectName: paper.subjectName
          }
        })
        
        showSuccessMessage(`已开始 ${paper.paperName} 的练习（使用默认题目）`)
      }
    } catch (error) {
      console.error('获取练习题目失败:', error)
      // 使用默认题目
      const defaultExercises = generateDefaultExercises(paper)
      
      router.push({
        path: '/practice',
        query: {
          exercises: JSON.stringify(defaultExercises),
          paperId: paper.id,
          paperName: paper.paperName,
          subjectName: paper.subjectName
        }
      })
      
      showSuccessMessage(`已开始 ${paper.paperName} 的练习`)
    }
  }).catch(() => {
    // 取消练习
  })
}

// 获取难度文本
const getDifficultyText = (difficulty) => {
  const difficultyMap = {
    'easy': '简单',
    'medium': '中等',
    'hard': '困难',
    1: '简单',
    2: '较易',
    3: '中等',
    4: '较难',
    5: '困难'
  }
  return difficultyMap[difficulty] || '中等'
}

// 生成默认练习题目
const generateDefaultExercises = (paper) => {
  const questionCount = paper.questionCount || 20
  const exercises = []
  
  for (let i = 0; i < questionCount; i++) {
    exercises.push({
      id: `default-${i}`,
      question: `第${i + 1}题：这是一道${paper.subjectName}相关的练习题，请认真作答。`,
      type: '选择题',
      options: [
        'A. 选项A',
        'B. 选项B', 
        'C. 选项C',
        'D. 选项D'
      ],
      correctAnswer: Math.floor(Math.random() * 4),
      analysis: `本题考查${paper.subjectName}的基础知识，正确答案为对应选项。建议结合教材和相关资料进行复习。`,
      difficulty: ['easy', 'medium', 'hard'][Math.floor(Math.random() * 3)],
      difficultyText: ['简单', '中等', '困难'][Math.floor(Math.random() * 3)],
      score: Math.floor(paper.totalScore / questionCount) || 5,
      knowledgePoint: paper.subjectName
    })
  }
  
  return exercises
}

// 获取考试课程
const fetchExamCourses = async (subjectId) => {
  loading.value.examCourses = true
  error.value.examCourses = ''
  try {
    console.log('开始获取考试课程，科目ID:', subjectId)
    const response = await apiService.qa.getExamCourses(subjectId)
    console.log('考试课程响应:', response)
    if (response && response.data) {
      examCourses.value = response.data || []
      console.log('考试课程数据:', examCourses.value)
    } else if (response) {
      // 如果响应不是标准Result格式，直接使用
      examCourses.value = response || []
      console.log('考试课程数据（非标准格式）:', examCourses.value)
    }
  } catch (err) {
    console.error('获取考试课程失败:', err)
    // 更详细的错误处理
    if (err.response) {
      // 服务器返回了错误状态码
      error.value.examCourses = `获取考试课程失败: ${err.response.status} ${err.response.statusText}`
    } else if (err.request) {
      // 请求已发出但没有收到响应
      error.value.examCourses = '网络错误，无法连接到服务器'
    } else {
      // 其他错误
      error.value.examCourses = err.message || '获取考试课程失败'
    }
  } finally {
    loading.value.examCourses = false
  }
}

// 获取考试历史记录
const fetchExamHistory = async () => {
  loading.value.examHistory = true
  error.value.examHistory = ''
  try {
    // 从localStorage获取userId
    let userId = localStorage.getItem('userId')
    
    // 如果localStorage中没有userId，从authStore获取
    if (!userId) {
      const authStore = useAuthStore()
      userId = authStore.user?.id || authStore.userId
    }
    
    // 确保userId是数字类型
    userId = Number(userId || '')
    
    if (!userId || isNaN(userId)) {
      // 用户未登录，返回空数组
      examHistory.value = []
      loading.value.examHistory = false
      return
    }
    
    console.log('获取考试准备Q&A历史记录，userId:', userId)
    const response = await apiService.qa.getExamQaHistory({ params: { userId } })
    console.log('考试准备Q&A历史记录响应:', response)
    if (response && response.data && Array.isArray(response.data)) {
      examHistory.value = response.data
      console.log('考试准备Q&A历史记录数据:', examHistory.value)
    } else if (response && Array.isArray(response)) {
      // 如果响应不是标准Result格式，直接使用
      examHistory.value = response
      console.log('考试准备Q&A历史记录数据（非标准格式）:', examHistory.value)
    } else {
      // 如果响应不是数组，设置空数组
      examHistory.value = []
    }
  } catch (err) {
    console.error('获取考试准备Q&A历史记录失败:', err)
    error.value.examHistory = err.errorMessage || '获取考试准备Q&A历史记录失败'
    // 设置空数组以避免界面错误
    examHistory.value = []
  } finally {
    loading.value.examHistory = false
  }
}

// 考试文件上传相关
const examFileInput = ref(null)
// 考试上传的图片
const examUploadedImage = ref(null)
// 考试图片问题描述
const examImageQuestion = ref('')

// 触发考试文件上传
const triggerExamFileUpload = () => {
  examFileInput.value?.click()
}

// 处理考试文件选择
const handleExamFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 压缩图片
    compressImage(file, 0.7, 1024, 768).then(compressedImage => {
      examUploadedImage.value = compressedImage
    }).catch(error => {
      console.error('图片压缩失败:', error)
      // 压缩失败时使用原图
      const reader = new FileReader()
      reader.onload = (e) => {
        examUploadedImage.value = e.target.result
      }
      reader.readAsDataURL(file)
    })
  }
}

// 删除考试上传的图片
const deleteExamImage = () => {
  examUploadedImage.value = null
  examImageQuestion.value = ''
}

// 提交考点问题
const submitExamQuestion = async () => {
  if (!examQuestion.value.trim()) {
    showErrorMessage('请输入问题')
    return
  }
  
  try {
    // 对输入进行sanitize处理，防止XSS攻击
    const sanitizedQuestion = sanitizeInput(examQuestion.value)
    
    // 重置状态
    showAnswer.value = true
    isLoading.value = true
    errorMessage.value = ''
    typedAnswer.value = ''
    fullAnswer.value = ''
    processedAnswer.value = ''
    
    // 显示加载提示
    showInfoMessage('正在提交问题，请稍候...')
    
    // 构建请求数据
    const requestData = {
      question: sanitizedQuestion,
      subject: selectedExamSubject.value
    }
    
    // 使用流式POST请求
    const attemptConnection = () => {
      streamingApiService.stream.askQuestionStream(
        requestData,
        (message) => {
          if (message.error) {
            errorMessage.value = message.error
            isLoading.value = false
            showErrorMessage('AI回答生成失败: ' + message.error)
          } else {
            // 处理流式数据
            if (message.content) {
              // 累积数据
              const newContent = message.content
              fullAnswer.value += newContent
              
              // 只在第一次接收到数据时开始打字效果
              if (!isTyping.value) {
                // 开始打字效果
                isLoading.value = false
                startTyping(fullAnswer.value)
              }
              // 注意：不要在这里直接更新typedAnswer，让startTyping函数处理打字效果
            }
            if (message.completed) {
              isLoading.value = false
            }
          }
        },
        () => {
          // 完成回调
            console.log('Stream completed')
            isLoading.value = false
            
            // 确保有内容时处理完整回答
            if (fullAnswer.value) {
              // 处理完整回答，使用processMathText处理数学文本
              processedAnswer.value = processMathText(fullAnswer.value)
              // 同时更新examAnswer，保持兼容性
              examAnswer.value = processedAnswer.value
              
              // 保存处理后的文本到后端
              const questionText = examQuestion.value.trim()
              const requestData = {
                question: questionText,
                answer: fullAnswer.value,
                subject: getSelectedSubjectName(),
                userId: localStorage.getItem('userId') || useAuthStore().user?.id || useAuthStore().userId
              }
              
              // 调用API保存处理后的文本
              console.log('开始保存考试准备Q&A历史记录:', requestData);
              
              // 检查用户是否登录
              const authStore = useAuthStore();
              if (!authStore.isLoggedIn) {
                showErrorMessage('用户未登录，无法保存历史记录');
                return;
              }
              
              apiService.qa.saveExamQaHistory(requestData)
                .then((response) => {
                  console.log('考试准备Q&A历史记录保存成功，响应:', response)
                  
                  // 显示成功提示
                  showSuccessMessage('历史记录保存成功')
                  
                  // 重新获取历史记录，确保数据与后端一致
                  fetchExamHistory()
                })
                .catch(error => {
                  console.error('保存考试准备Q&A历史记录失败:', error)
                  console.error('错误详情:', error.response || error)
                  showErrorMessage('保存历史记录失败: ' + (error.errorMessage || error.message || '未知错误'))
                })
            }
        },
        (error) => {
          // 错误回调
          console.error('Stream error:', error)
          
          // 尝试重连
          if (reconnectAttempts.value < maxReconnectAttempts) {
            reconnectAttempts.value++
            console.log(`尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`)
            showInfoMessage(`连接失败，正在尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`)
            
            // 延迟后重连
            setTimeout(attemptConnection, reconnectDelay * reconnectAttempts.value)
          } else {
            // 重连失败
            let errorMsg = error.message || '连接失败，请重试'
            
            // 针对不同错误类型提供更友好的提示
            if (errorMsg.includes('500')) {
              errorMsg = '服务器内部错误，请稍后再试'
            } else if (errorMsg.includes('401')) {
              errorMsg = '请先登录'
              // 跳转到登录页面
              setTimeout(() => {
                window.location.href = '/login'
              }, 2000)
            } else if (errorMsg.includes('404')) {
              errorMsg = '请求的接口不存在'
            } else if (errorMsg.includes('Network')) {
              errorMsg = '网络连接失败，请检查网络设置'
            }
            
            errorMessage.value = errorMsg
            isLoading.value = false
            
            // 显示错误提示
            showErrorMessage('连接失败: ' + errorMsg)
            // 重置重连计数器
            reconnectAttempts.value = 0
          }
        }
      )
    }
    
    // 开始连接
    attemptConnection()
  } catch (err) {
    const errorMsg = err.errorMessage || '提交问题失败'
    errorMessage.value = errorMsg
    showErrorMessage(errorMsg)
    isLoading.value = false
  }
}

// 选择考点
const selectKeyPoint = (point) => {
  selectedKeyPoint.value = point
  examQuestion.value = '请详细讲解：' + point.name
}

// 选择历史记录
const selectExamHistory = (record) => {
  examQuestion.value = record.question
}

// 处理数学文本的函数
const processMathText = (text) => {
  try {
    if (!text) return ''
    
    // 去除所有空行，只保留单个换行
    let processedText = text.replace(/\n{2,}/g, '\n')
    
    // 使用processAiResponse处理Markdown和数学公式
    return processAiResponse(processedText)
  } catch (error) {
    console.error('处理数学文本失败:', error)
    // 如果处理失败，返回原始文本
    return text || ''
  }
}

// 输入 sanitization 函数
const sanitizeInput = (input) => {
  if (!input) return ''
  // 转义HTML特殊字符，防止XSS攻击
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// 格式化回答内容
const formatAnswer = (answer) => {
  if (!answer) return ''
  
  // 使用processMathText处理数学文本
  return processMathText(answer)
}
</script>

<style scoped>
.qa-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* 代码块样式 */
:deep(pre) {
  background-color: #f6f8fa;
  border-radius: 8px;
  padding: 16px;
  margin: 16px 0;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  border: 1px solid #e1e4e8;
}

:deep(code) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
}

:deep(.hljs) {
  background: transparent !important;
  padding: 0 !important;
}

:deep(.code-with-lines) {
  position: relative;
}

:deep(.code-line) {
  display: flex;
  align-items: flex-start;
}

:deep(.line-number) {
  display: inline-block;
  width: 40px;
  padding-right: 16px;
  text-align: right;
  color: #999;
  user-select: none;
  border-right: 1px solid #e1e4e8;
  margin-right: 16px;
}

:deep(.line-content) {
  flex: 1;
}

/* 行内代码样式 */
:deep(code:not(.hljs)) {
  background-color: #f1f1f1;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  justify-content: center;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: #6b7280;
}

/* 打字效果 */
.typing-content {
  position: relative;
  min-height: 60px;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.typing-cursor {
  display: inline-block;
  width: 10px;
  height: 18px;
  background-color: #3b82f6;
  margin-left: 4px;
  animation: blink 1s infinite;
  vertical-align: middle;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 回答控制按钮 */
.answer-controls {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  justify-content: flex-end;
}

.control-btn {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
}

/* 错误消息 */
.error-message {
  padding: 16px;
  background-color: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #b91c1c;
  font-size: 14px;
  margin-bottom: 16px;
}

.retry-btn {
  margin-left: 12px;
  padding: 4px 12px;
  background-color: #b91c1c;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.retry-btn:hover {
  background-color: #991b1b;
}

/* 处理后的内容 */
.processed-content {
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
}

/* 考试问答相关样式 */
.exam-answer-content {
  margin-top: 16px;
}

.ai-answer-box {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.ai-answer-box:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.ai-answer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.ai-label {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.ai-answer-text {
  font-size: 15px;
  line-height: 1.7;
  color: #374151;
}

.ai-answer-text p {
  margin-bottom: 16px;
}

.exam-question-display {
  margin-top: 24px;
}

/* 高频考点总结区样式 */
.exam-panel {
  background-color: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.exam-panel:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.exam-panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #3b82f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #3b82f6;
}

.exam-panel-actions {
  display: flex;
  gap: 12px;
}

.ai-recommend-btn {
  padding: 8px 16px;
  border: 1px solid #10b981;
  border-radius: 6px;
  background-color: white;
  color: #10b981;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ai-recommend-btn:hover {
  background-color: #f0fdf4;
  border-color: #059669;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.ai-recommend-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.exam-panel-header .exam-panel-title {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

/* 学习进度样式 */
.progress-info {
  margin-bottom: 16px;
}

.progress-text {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
  display: block;
}

.progress-bar {
  width: 100%;
  height: 10px;
  background-color: #f3f4f6;
  border-radius: 5px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 5px;
  transition: width 0.3s ease;
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* 考点分类筛选样式 */
.keypoint-filter {
  margin-bottom: 24px;
}

.filter-tabs {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 20px;
  background-color: white;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background-color: #eff6ff;
}

.filter-tab.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.category-count {
  font-size: 12px;
  background-color: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 400;
}

.filter-tab.active .category-count {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 考点列表/卡片样式 */
.keypoints-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.keypoints-container.card-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.keypoint-item {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.keypoint-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.keypoint-item.highlight {
  border-color: #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
  background-color: #f8faff;
}

.keypoint-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.keypoint-info {
  flex: 1;
}

.keypoint-label {
  font-size: 14px;
  color: #6b7280;
  margin-right: 8px;
}

.keypoint-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-right: 12px;
}

.keypoint-difficulty {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  text-transform: capitalize;
}

.keypoint-difficulty.easy {
  background-color: #d1fae5;
  color: #065f46;
}

.keypoint-difficulty.medium {
  background-color: #fef3c7;
  color: #92400e;
}

.keypoint-difficulty.hard {
  background-color: #fee2e2;
  color: #991b1b;
}

.keypoint-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.control-btn:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
}

.star-btn.active {
  color: #f59e0b;
  border-color: #f59e0b;
  background-color: #fef3c7;
}

.star-btn:hover {
  color: #f59e0b;
  border-color: #f59e0b;
}

/* 考点内容样式 */
.keypoint-content {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.content-section {
  margin-bottom: 16px;
}

.content-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.content-section h4::before {
  content: '•';
  color: #3b82f6;
  font-weight: bold;
}

.content-section p {
  font-size: 14px;
  line-height: 1.6;
  color: #6b7280;
  margin: 0;
}

/* 考察频率样式 */
.frequency-bar {
  width: 100%;
  height: 8px;
  background-color: #f3f4f6;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  margin-bottom: 8px;
}

.frequency-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.frequency-text {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  text-align: right;
}

/* 排序和视图切换按钮样式 */
.sort-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.action-btn {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
}

.action-btn.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  color: white;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  margin-bottom: 16px;
}

/* 真题推荐样式 */
.exam-papers-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-paper-item {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.exam-paper-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.exam-paper-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.5s ease;
  border-radius: 12px;
  pointer-events: none;
}

.exam-paper-item:hover::before {
  left: 100%;
}

.paper-details-dialog {
  max-width: 600px !important;
}

.paper-details-dialog .el-message-box__content {
  padding: 20px !important;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6b7280;
}

.loading-state .loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.paper-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.paper-actions {
  display: flex;
  gap: 8px;
}

.favorite-btn {
  background: none;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.favorite-btn:hover {
  border-color: #f59e0b;
}

.favorite-btn.active {
  color: #f59e0b;
  border-color: #f59e0b;
  background-color: #fef3c7;
}

.paper-content {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.paper-image {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f3f4f6;
}

.paper-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.paper-info {
  flex: 1;
}

.paper-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.paper-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.paper-meta-item {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  font-size: 14px;
}

.paper-source {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.source-icon {
  font-size: 14px;
}

.paper-reason {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.reason-icon {
  font-size: 14px;
  margin-top: 2px;
}

.paper-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.paper-btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.primary-btn {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border: 1px solid #3b82f6;
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.secondary-btn {
  background-color: white;
  border: 1px solid #d1d5db;
  color: #374151;
}

.secondary-btn:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
}

/* 网课推荐样式 */
.exam-courses-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-course-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.exam-course-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.course-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.avatar-placeholder {
  color: white;
  font-size: 20px;
  font-weight: 600;
}

.course-info {
  flex: 1;
}

.course-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.course-desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
  line-height: 1.4;
}

.course-code {
  font-size: 12px;
  color: #9ca3af;
}

.course-link-icon {
  font-size: 16px;
  color: #3b82f6;
  flex-shrink: 0;
}

/* 备考答疑历史记录样式 */
.exam-history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.exam-history-item {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.exam-history-item:hover {
  background-color: #f9fafb;
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.exam-history-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.exam-history-question {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #374151;
  margin-right: 12px;
  line-height: 1.4;
}

.exam-history-actions {
  position: relative;
}

.more-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.more-btn:hover {
  background-color: #f3f4f6;
}

.more-btn.active {
  background-color: #e5e7eb;
}

.more-icon {
  font-size: 16px;
  color: #6b7280;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 160px;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: #374151;
  background: none;
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;
  transition: background-color 0.3s ease;
}

.dropdown-item:hover {
  background-color: #f3f4f6;
}

.dropdown-item.delete {
  color: #b91c1c;
}

.dropdown-item.delete:hover {
  background-color: #fee2e2;
}

.dropdown-icon {
  font-size: 14px;
}

.exam-history-content {
  cursor: pointer;
  padding: 4px 0;
}

.exam-history-answer {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 图片上传区域样式 */
.exam-upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #f9fafb;
}

.exam-upload-area:hover {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.exam-upload-placeholder {
  color: #6b7280;
}

.exam-upload-text {
  font-size: 14px;
  margin-top: 8px;
}

.exam-uploaded-preview {
  margin-top: 16px;
}

.preview-container {
  position: relative;
  width: 100%;
  max-width: 300px;
  margin: 0 auto 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.exam-preview-image {
  width: 100%;
  height: auto;
  display: block;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background-color: rgba(0, 0, 0, 0.8);
  transform: scale(1.1);
}

.exam-image-question-input {
  margin-bottom: 12px;
}

.exam-question-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  transition: all 0.3s ease;
}

.exam-question-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.exam-upload-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.exam-upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* 搜索框样式 */
.searchable-select {
  position: relative;
  margin-bottom: 16px;
}

.select-search-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.select-search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
}

.select-option {
  padding: 12px 16px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.select-option:hover {
  background-color: #f3f4f6;
}

.select-option.active {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 500;
}

.select-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.select-display:hover {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.select-arrow {
  font-size: 12px;
  color: #6b7280;
  transition: transform 0.3s ease;
}

.select-display:hover .select-arrow {
  transform: rotate(180deg);
}

/* 考点提问区样式 */
.exam-question-area {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.exam-question-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  transition: all 0.3s ease;
}

.exam-question-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.exam-submit-btn {
  padding: 0 24px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  white-space: nowrap;
}

.exam-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .exam-qa-container {
    flex-direction: column;
  }
  
  .exam-left-column,
  .exam-middle-column,
  .exam-right-column {
    width: 100%;
  }
  
  .keypoints-container.card-view {
    grid-template-columns: 1fr;
  }
  
  .paper-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .paper-image {
    width: 100%;
    max-width: 200px;
  }
  
  .sort-actions {
    flex-direction: column;
  }
  
  .action-btn {
    width: 100%;
    justify-content: center;
  }
  
  .filter-tabs {
    flex-direction: column;
  }
  
  .filter-tab {
    width: 100%;
    justify-content: center;
  }
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
  position: relative;
}

.header-left {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
}

.back-home-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.back-home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
  background: linear-gradient(90deg, #2563eb 0%, #1d4ed8 100%);
}

.back-icon {
  font-size: 16px;
  font-weight: 600;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  text-align: center;
}

/* 搜索框样式 */
.search-box {
  position: relative;
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
  padding: 12px 40px 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: #9ca3af;
  pointer-events: none;
}

/* 导航标签 */
.tab-navigation {
  display: flex;
  background: white;
  border-radius: 12px;
  padding: 8px;
  margin-bottom: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab {
  flex: 1;
  padding: 12px 0;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab:hover {
  color: #3b82f6;
  background-color: #eff6ff;
}

.tab.active {
  color: white;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 专业课问答容器 */
.qa-container {
  display: grid;
  grid-template-columns: 280px 320px 1fr;
  gap: 20px;
  margin-bottom: 40px;
}

/* 面板通用样式 */
.left-panel,
.middle-panel,
.right-panel {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #3b82f6;
}

/* 左侧：教材选择器 */
.book-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.book-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.book-item:hover {
  background-color: #f3f4f6;
}

.book-item.active {
  background-color: #dbeafe;
  border-left: 4px solid #3b82f6;
  font-weight: 500;
  color: #2563eb;
}

.book-icon {
  flex-shrink: 0;
}

.book-cover {
  width: 40px;
  height: 56px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.book-info {
  flex: 1;
}

.book-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-list {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 中间：章节导航 */
.chapter-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
  gap: 8px;
}

.nav-item:hover {
  background-color: #f3f4f6;
}

.nav-item.active {
  background-color: #dbeafe;
  color: #2563eb;
  font-weight: 500;
}

.sub-nav-item.active {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 500;
  border-left: 3px solid #3b82f6;
}

.nav-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 8px;
}

.nav-arrow {
  font-size: 12px;
  opacity: 0.7;
  flex-shrink: 0;
}

.nav-children {
  margin-left: 20px;
  margin-top: 4px;
  margin-bottom: 4px;
}

.sub-nav-item {
  margin-left: 10px;
  background-color: #f9fafb;
}

.sub-sub-nav-item {
  margin-left: 20px;
  background-color: #f3f4f6;
  font-size: 13px;
}

.sub-sub-nav-item.active {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 500;
  border-left: 3px solid #3b82f6;
}

.sub-nav-item:hover {
  background: #f3f4f6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .middle-panel {
    margin-top: 20px;
  }
  
  .nav-item {
    padding: 10px 12px;
  }
  
  .nav-title {
    font-size: 13px;
  }
  
  .nav-children {
    margin-left: 15px;
  }
  
  .sub-nav-item {
    margin-left: 5px;
  }
  
  .sub-sub-nav-item {
    margin-left: 15px;
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .nav-item {
    padding: 8px 10px;
  }
  
  .nav-title {
    font-size: 12px;
  }
  
  .nav-arrow {
    font-size: 10px;
  }
  
  .nav-children {
    margin-left: 10px;
  }
  
  .sub-sub-nav-item {
    font-size: 11px;
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
  border-top: 2px solid #3b82f6;
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

/* 章节内容显示区域样式 */
.chapter-content {
  margin-top: 20px;
  padding: 20px;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.content-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.content-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.content-body {
  line-height: 1.6;
  color: #374151;
}

.content-body p {
  margin: 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chapter-content {
    padding: 16px;
  }
  
  .content-header h3 {
    font-size: 14px;
  }
  
  .content-body p {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .chapter-content {
    padding: 12px;
  }
  
  .content-header h3 {
    font-size: 13px;
  }
  
  .content-body p {
    font-size: 12px;
  }
}

.sub-nav-title {
  font-size: 13px;
  color: #374151;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
}

.sub-nav-arrow {
  font-size: 12px;
  opacity: 0.7;
  flex-shrink: 0;
}

/* 右侧：答疑区域 */
/* 问题类型切换标签 */
.question-type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.type-tab {
  flex: 1;
  padding: 10px 16px;
  text-align: center;
  background: #f3f4f6;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-tab:hover {
  background: #e5e7eb;
}

.type-tab.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

/* 提问区域 */
.question-area {
  margin-bottom: 20px;
}

/* 图片上传区域 */
.image-upload-area {
  margin-bottom: 20px;
}

.upload-box {
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.upload-box:hover {
  border-color: #3b82f6;
  background: #f0f7ff;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
}

.upload-text {
  font-size: 14px;
  color: #6b7280;
}

.uploaded-preview {
  margin-top: 16px;
  text-align: center;
}

.preview-container {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  display: block;
}

.delete-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 0, 0, 0.8);
  color: white;
  border: none;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background: rgba(255, 0, 0, 1);
  transform: scale(1.1);
}

.exam-preview-image {
  max-width: 100%;
  max-height: 150px;
  border-radius: 6px;
  display: block;
  margin-bottom: 12px;
}

.image-question-input {
  margin-bottom: 16px;
  width: 100%;
}

.exam-uploaded-preview {
  margin-top: 16px;
  text-align: center;
}

.exam-preview-image {
  max-width: 100%;
  max-height: 150px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.exam-image-question-input {
  margin-bottom: 12px;
  width: 100%;
}

.question-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
  margin-bottom: 12px;
}

.question-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.submit-btn {
  width: 100%;
  padding: 10px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* AI回答区域 */
.answer-area {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  max-height: 300px;
  overflow-y: auto;
}

/* Markdown渲染样式 */
.processed-content {
  line-height: 1.15;
}

.processed-content h1,
.processed-content h2,
.processed-content h3,
.processed-content h4,
.processed-content h5,
.processed-content h6 {
  margin: 0.3em 0 0.1em 0;
  font-weight: 600;
  line-height: 1.1;
}

.processed-content h1 {
  font-size: 1.6em;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 0.2em;
}

.processed-content h2 {
  font-size: 1.4em;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.1em;
}

.processed-content h3 {
  font-size: 1.2em;
}

.processed-content h4 {
  font-size: 1.1em;
}

.processed-content h5,
.processed-content h6 {
  font-size: 1em;
}

.processed-content p {
  margin: 0.1em 0;
}

.processed-content ul,
.processed-content ol {
  margin: 0.1em 0;
  padding-left: 2em;
}

.processed-content li {
  margin: 0.05em 0;
}

.processed-content a {
  color: #3b82f6;
  text-decoration: none;
}

.processed-content a:hover {
  text-decoration: underline;
}

.processed-content blockquote {
  border-left: 4px solid #e5e7eb;
  padding-left: 1em;
  margin: 0.1em 0;
  color: #6b7280;
  font-style: italic;
}

/* 代码块样式 */
.processed-content pre {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 1em;
  overflow-x: auto;
  margin: 0.1em 0;
}

.processed-content code {
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9em;
}

.processed-content pre code {
  background: none;
  padding: 0;
}

/* 数学公式样式 */
.processed-content .katex {
  font-size: 1.1em;
}

.processed-content .katex-display {
  margin: 0.3em 0;
  text-align: center;
}

.answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  background: #f9fafb;
  z-index: 1;
}

.answer-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.connection-status {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.connection-status.connecting {
  background: #fef3c7;
  color: #d97706;
}

.connection-status.connected {
  background: #d1fae5;
  color: #065f46;
}

.connection-status.error {
  background: #fee2e2;
  color: #b91c1c;
}

.connection-status.idle {
  background: #e5e7eb;
  color: #6b7280;
}

.knowledge-graph {
  font-size: 14px;
  color: #3b82f6;
  font-weight: 500;
  cursor: pointer;
}

.answer-content {
  display: flex;
  gap: 12px;
}

.ai-avatar {
  flex-shrink: 0;
}

.answer-content .avatar-placeholder {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.answer-text {
  flex: 1;
  font-size: 14px;
  color: #374151;
  line-height: 1.15;
  white-space: pre-wrap;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: #6b7280;
  font-size: 16px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-radius: 50%;
  border-top-color: #3b82f6;
  animation: spin 1.5s linear infinite;
}

.loading-text {
  font-weight: 500;
  color: #4b5563;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 增强视觉设计 */
.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}

.answer-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.answer-content {
  display: flex;
  gap: 16px;
  padding: 24px;
  background-color: #f9fafb;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.answer-content:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.ai-avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 18px;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

.answer-text {
  flex: 1;
  min-height: 120px;
  font-size: 16px;
  line-height: 1.15;
  color: #374151;
}

.error-message {
  color: #b91c1c;
  background: #fee2e2;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-left: 4px solid #ef4444;
}

.retry-btn {
  padding: 8px 16px;
  background-color: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.3s ease;
}

.retry-btn:hover {
  background-color: #dc2626;
}

.typing-content {
  position: relative;
  min-height: 80px;
}

.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1.2em;
  background-color: #374151;
  animation: blink 1s infinite;
  margin-left: 2px;
  vertical-align: text-bottom;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.answer-controls {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.control-btn {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  color: #374151;
}

.control-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.control-btn:nth-child(2) {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.control-btn:nth-child(2):hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
}

/* 历史问题 */
.history-section {
  background: #f3f4f6;
  border-radius: 12px;
  padding: 16px;
  max-height: 800px;
  overflow-y: auto;
  height: 800px;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.history-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e5e7eb;
}

.history-item:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.history-question {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  width: 100%;
}

.history-answer {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.2;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 8px;
}

/* 历史记录增强样式 */
.history-section {
  margin-top: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 20px;
  flex-wrap: wrap;
}

.history-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0;
  white-space: nowrap;
}

.history-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.history-search-input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  transition: all 0.3s ease;
  width: 200px;
  margin-right: 12px;
}

.history-search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.history-filters {
  display: flex;
  gap: 8px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-select:hover {
  border-color: #3b82f6;
}

.filter-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.loading-message,
.error-message,
.empty-history {
  text-align: center;
  padding: 20px;
  color: #6b7280;
  font-size: 14px;
}

.error-message {
  color: #ef4444;
}

.history-item {
  padding: 16px;
  margin-bottom: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.3s ease;
}

.history-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
  transform: translateY(-1px);
}

.history-item .history-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.history-question {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.history-time {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.history-subject {
  margin-top: 8px;
}

.subject-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #dbeafe;
  color: #2563eb;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

/* 更多操作按钮样式 */
.history-item .history-actions {
  position: relative;
  display: inline-block;
}

.more-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  transition: all 0.2s ease;
  margin-left: 8px;
}

.more-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.more-btn.active {
  background: #e5e7eb;
  color: #1f2937;
}

.more-icon {
  line-height: 1;
}

/* 下拉菜单样式 */
.dropdown-menu {
  position: absolute;
  top: 32px;
  right: 0;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  z-index: 100;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  background: none;
  border: none;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
  gap: 10px;
}

.dropdown-item:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.dropdown-item:active {
  background: #e5e7eb;
}

.dropdown-item.delete {
  color: #ef4444;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
}

.dropdown-item.delete:hover {
  background: #fee2e2;
  color: #dc2626;
}

.dropdown-icon {
  font-size: 16px;
  flex-shrink: 0;
  width: 16px;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .qa-container {
    grid-template-columns: 1fr;
  }
  
  .history-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .history-search-input {
    width: 100%;
    margin-right: 0;
  }
  
  .history-filters {
    flex-wrap: wrap;
  }
  
  .filter-select {
    flex: 1;
    min-width: 120px;
  }
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  gap: 12px;
}

.page-btn {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #3b82f6;
  color: #3b82f6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}

/* 底部：网课推荐 */
.course-recommendation {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.recommendation-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20px;
  text-align: center;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.course-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f9fafb;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.course-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.course-link-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.course-item:hover .course-link-icon {
  opacity: 1;
}

.course-avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.course-info {
  flex: 1;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.course-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.course-code {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

/* 其他标签页占位 */
.tab-content {
  background: white;
  border-radius: 16px;
  padding: 100px 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.placeholder {
  font-size: 18px;
  color: #6b7280;
  font-weight: 500;
}

/* ==================== 考研/考证问答页面样式 ==================== */
.exam-qa-page {
  padding: 20px;
}

.exam-qa-container {
  display: grid;
  grid-template-columns: 300px 1fr 280px;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 面板通用样式 */
.exam-panel {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.exam-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

/* 左侧栏 - 考试科目选择 */
.exam-subject-select {
  margin-bottom: 12px;
}

.subject-dropdown {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236B7280' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

.subject-dropdown:focus {
  outline: none;
  border-color: #3b82f6;
}

.exam-subject-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.exam-subject-item {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.exam-subject-item:hover {
  background: #f3f4f6;
}

.exam-subject-item.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-color: #3b82f6;
}

/* 搜索able下拉框样式 */
.searchable-select {
  position: relative;
  width: 100%;
  z-index: 100;
}

.select-search-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  margin-bottom: 8px;
}

.select-search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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
  color: #3b82f6;
}

.select-option.active {
  background-color: #3b82f6;
  color: white;
  font-weight: 500;
}

.select-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #374151;
}

.select-display:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.select-arrow {
  font-size: 12px;
  color: #6b7280;
  transition: all 0.3s ease;
}

.select-display:hover .select-arrow {
  color: #3b82f6;
  transform: rotate(180deg);
}

/* 自定义滚动条 */
.select-dropdown::-webkit-scrollbar,
.history-section::-webkit-scrollbar,
.exam-history-list::-webkit-scrollbar,
.answer-area::-webkit-scrollbar,
.exam-answer-content::-webkit-scrollbar,
.exam-papers-container::-webkit-scrollbar,
.exam-courses-list::-webkit-scrollbar {
  width: 8px;
}

.select-dropdown::-webkit-scrollbar-track,
.history-section::-webkit-scrollbar-track,
.exam-history-list::-webkit-scrollbar-track,
.answer-area::-webkit-scrollbar-track,
.exam-answer-content::-webkit-scrollbar-track,
.exam-papers-container::-webkit-scrollbar-track,
.exam-courses-list::-webkit-scrollbar-track {
  background: #f8fafc;
  border-radius: 10px;
}

.select-dropdown::-webkit-scrollbar-thumb,
.history-section::-webkit-scrollbar-thumb,
.exam-history-list::-webkit-scrollbar-thumb,
.answer-area::-webkit-scrollbar-thumb,
.exam-answer-content::-webkit-scrollbar-thumb,
.exam-papers-container::-webkit-scrollbar-thumb,
.exam-courses-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.select-dropdown::-webkit-scrollbar-thumb:hover,
.history-section::-webkit-scrollbar-thumb:hover,
.exam-history-list::-webkit-scrollbar-thumb:hover,
.answer-area::-webkit-scrollbar-thumb:hover,
.exam-answer-content::-webkit-scrollbar-thumb:hover,
.exam-papers-container::-webkit-scrollbar-thumb:hover,
.exam-courses-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
  transform: scaleX(1.1);
}

/* 隐藏滚动条但保留滚动功能 */
.select-dropdown,
.history-section,
.exam-history-list,
.answer-area,
.exam-answer-content,
.exam-papers-container,
.exam-courses-list {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f8fafc;
}

/* 高频考点 */
.exam-panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 2px solid #3b82f6;
}

.keypoint-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: #e5e7eb;
  border-color: #d1d5db;
}

/* 学习进度 */
.progress-info {
  margin-right: 16px;
  min-width: 120px;
}

.progress-text {
  display: block;
  font-size: 11px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 2px;
  transition: width 0.5s ease;
}

/* 排序操作 */
.sort-actions {
  display: flex;
  gap: 6px;
}

.sort-actions .action-btn {
  font-size: 11px;
  padding: 5px 10px;
}

/* 考点分类筛选 */
.keypoint-filter {
  margin-bottom: 16px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.filter-tab {
  padding: 8px 16px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover {
  background: #e5e7eb;
  color: #374151;
}

.filter-tab.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-color: #3b82f6;
}

.category-count {
  font-size: 11px;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.filter-tab.active .category-count {
  background: rgba(255, 255, 255, 0.3);
}

/* 考点容器 */
.keypoints-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 500px;
  overflow-y: auto;
}

.keypoints-container.card-view {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 16px;
}

/* 考点项 */
.keypoint-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
}

.keypoints-container.card-view .keypoint-item {
  flex: 1 1 calc(50% - 8px);
  min-width: 200px;
}

.keypoint-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
  transform: translateY(-2px);
}

.keypoint-item.highlight {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

/* 考点头部 */
.keypoint-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.keypoint-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.keypoint-label {
  color: #6b7280;
  font-size: 12px;
  font-weight: 500;
}

.keypoint-name {
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
}

.keypoint-difficulty {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.keypoint-difficulty.easy {
  background: #d1fae5;
  color: #065f46;
}

.keypoint-difficulty.medium {
  background: #fef3c7;
  color: #d97706;
}

.keypoint-difficulty.hard {
  background: #fee2e2;
  color: #b91c1c;
}

/* 考点控制 */
.keypoint-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  padding: 6px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  color: #6b7280;
}

.control-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.star-btn.active {
  color: #f59e0b;
  background: #fef3c7;
  border-color: #f59e0b;
}

/* 考点内容 */
.keypoint-content {
  padding: 16px;
  background: #f9fafb;
  border-top: 1px solid #f3f4f6;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.content-section {
  margin-bottom: 16px;
}

.content-section:last-child {
  margin-bottom: 0;
}

.content-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.content-section p {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.4;
  margin: 0;
}

/* 频率条 */
.frequency-bar {
  position: relative;
  height: 12px;
  background: #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}

.frequency-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 6px;
  transition: width 0.5s ease;
}

.frequency-text {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  background: white;
  padding: 1px 6px;
  border-radius: 10px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .exam-qa-container {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 0 12px;
  }
  
  .keypoints-container.card-view {
    flex-direction: column;
  }
  
  .keypoints-container.card-view .keypoint-item {
    flex: 1 1 100%;
  }
  
  .filter-tabs {
    gap: 6px;
  }
  
  .filter-tab {
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .keypoint-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .keypoint-controls {
    align-self: flex-end;
  }
  
  .exam-panel {
    padding: 16px;
  }
  
  .keypoint-content {
    padding: 12px;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .exam-qa-container {
    grid-template-columns: 280px 1fr;
  }
  
  .exam-right-column {
    grid-column: 1 / -1;
  }
  
  .keypoints-container.card-view .keypoint-item {
    flex: 1 1 calc(50% - 8px);
  }
}

/* 中间栏 - 考点提问区 */
.exam-question-area {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.exam-question-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  min-width: 400px;
}

.exam-question-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.exam-submit-btn {
  padding: 12px 24px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.exam-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.exam-question-display {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  margin-top: 16px;
}

.exam-question-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.exam-answer-content {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  height: 300px;
  overflow-y: auto;
}

.ai-answer-box {
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.ai-answer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.ai-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.ai-answer-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.15;
}

.ai-answer-text p {
  margin-bottom: 4px;
}

.ai-answer-text ul {
  padding-left: 20px;
  margin: 0;
}

.ai-answer-text li {
  margin-bottom: 2px;
}

.ai-answer-text li:last-child {
  margin-bottom: 0;
}

/* 面板头部 */
.exam-panel-header .history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.history-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* 刷新按钮 */
.refresh-btn {
  padding: 6px 12px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
  transition: all 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: #e5e7eb;
  border-color: #d1d5db;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 试卷头部 */
.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.paper-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

/* 试卷操作 */
.paper-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
}

.favorite-btn {
  color: #9ca3af;
}

.favorite-btn.active {
  color: #f59e0b;
}

/* 试卷信息 */
.paper-info {
  flex: 1;
  min-width: 0;
}

.paper-name {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 试卷元数据 */
.paper-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.paper-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
  background: #f9fafb;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.meta-icon {
  font-size: 12px;
}

/* 试卷来源 */
.paper-source {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.source-icon {
  font-size: 12px;
}

/* 推荐理由 */
.paper-reason {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: #10b981;
  background: #d1fae5;
  padding: 6px 8px;
  border-radius: 6px;
  border: 1px solid #a7f3d0;
}

.reason-icon {
  font-size: 12px;
  margin-top: 1px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  background: #f9fafb;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 16px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f4f6;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: #6b7280;
}

/* 网课推荐 */
.exam-courses-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 350px;
  overflow-y: auto;
}

.exam-course-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f9fafb;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  border: 1px solid #e5e7eb;
}

.exam-course-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #d1d5db;
}

.exam-course-item .course-link-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.exam-course-item:hover .course-link-icon {
  opacity: 1;
}

.exam-course-item .course-avatar {
  flex-shrink: 0;
}

.exam-course-item .avatar-placeholder {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

.exam-course-item .course-info {
  flex: 1;
}

.exam-course-item .course-name {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exam-course-item .course-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exam-course-item .course-code {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

/* 右侧栏 - 图片上传 */
.exam-upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9fafb;
  margin-bottom: 12px;
}

.exam-upload-area:hover {
  border-color: #3b82f6;
  background: #f0f7ff;
}

.exam-upload-text {
  font-size: 14px;
  color: #6b7280;
}

.exam-upload-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.exam-upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 备考答疑历史记录 */
.exam-history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 600px;
  overflow-y: auto;
}

.exam-history-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.exam-history-item:hover {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.exam-history-question {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.exam-history-answer {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.2;
}

/* 响应式设计 - 考研考证页面 */
@media (max-width: 1200px) {
  .exam-qa-container {
    grid-template-columns: 260px 1fr;
  }
  
  .exam-right-column {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }
  
  .exam-right-column .exam-panel {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .exam-qa-container {
    grid-template-columns: 1fr;
  }
  
  .exam-right-column {
    grid-template-columns: 1fr;
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .qa-container {
    grid-template-columns: 1fr 1fr;
    grid-template-areas:
      "left middle"
      "right right";
  }
  
  .left-panel {
    grid-area: left;
  }
  
  .middle-panel {
    grid-area: middle;
  }
  
  .right-panel {
    grid-area: right;
  }
}

@media (max-width: 768px) {
  .qa-container {
    grid-template-columns: 1fr;
    grid-template-areas:
      "left"
      "middle"
      "right";
  }
  
  .tab-navigation {
    flex-direction: column;
    gap: 4px;
  }
  
  .tab {
    padding: 10px 0;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .header-left {
    position: relative;
    top: 0;
    left: 0;
    transform: none;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .subtitle {
    text-align: left;
  }
}

/* 课程推荐样式 */
.course-cover {
  flex-shrink: 0;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: relative;
}

.cover-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.course-item:hover .cover-image {
  transform: scale(1.05);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  position: absolute;
  top: 0;
  left: 0;
}

.video-duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  z-index: 1;
}

/* 课程信息样式 */
.course-up {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.course-stats {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}

.view-count {
  display: inline-block;
  padding: 2px 6px;
  background: #f3f4f6;
  border-radius: 10px;
}

/* 响应式设计 - 课程推荐 */
@media (max-width: 768px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
  
  .course-cover {
    width: 80px;
    height: 80px;
  }
  
  .cover-placeholder {
    font-size: 24px;
  }
  
  .video-duration {
    font-size: 9px;
    padding: 2px 6px;
  }
  
  .course-name {
    font-size: 14px;
  }
  
  .course-up {
    font-size: 11px;
  }
  
  .course-desc {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .course-cover {
    width: 60px;
    height: 60px;
  }
  
  .cover-placeholder {
    font-size: 18px;
  }
  
  .video-duration {
    font-size: 8px;
    padding: 1px 4px;
  }
  
  .course-item {
    padding: 12px;
  }
  
  .course-name {
    font-size: 13px;
  }
  
  .course-up {
    font-size: 10px;
  }
  
  .course-desc {
    font-size: 11px;
  }
}

/* 书籍出版社样式 */
.book-publisher {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
</style>
