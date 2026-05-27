<template>
  <div class="bookshelf-page" :class="{ ready: pageReady }">
    <!-- 背景装饰层 -->
    <div class="bg-decor">
      <span v-for="s in 8" :key="'star' + s" class="deco-star" :style="decoStyle(s, 'star')">✦</span>
      <span v-for="d in 6" :key="'dot' + d" class="deco-dot" :style="decoStyle(d, 'dot')" />
      <span v-for="b in 3" :key="'book' + b" class="deco-book" :style="decoStyle(b, 'book')">📖</span>
    </div>

    <div class="bookshelf-content">
    <!-- 页面头部 -->
    <div class="bookshelf-header">
      <div class="header-content">
        <button class="back-home-btn" @click="backToHome">
          <span class="back-icon">←</span>
          返回首页
        </button>
        <h1 class="page-title">个人书架</h1>
      </div>
      <div class="search-wrapper">
        <div class="search-container">
          <div class="search-bar">
            <input 
              type="text" 
              v-model="searchQuery"
              placeholder="搜索书架书籍"
              class="search-input"
              @keyup.enter="searchBooks"
            >
          </div>
        </div>
        <button class="add-textbook-btn" @click="addTextbook">
          添加教材
        </button>
      </div>
    </div>

    <!-- 书籍筛选 -->
    <div class="bookshelf-filter">
      <div class="filter-section">
        <div class="filter-label">按学习状态筛选</div>
        <div class="select-container">
          <div class="select-dropdown" v-show="showStatusDropdown">
            <div 
              v-for="status in statusOptions" 
              :key="status.value"
              class="select-option"
              :class="{ active: statusFilter === status.value }"
              @click="selectStatus(status.value)"
            >
              {{ status.label }}
            </div>
          </div>
          <div class="select-display" @click="toggleStatusDropdown">
            {{ getSelectedStatusName() }}
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
          <img :src="book.coverImage || getDefaultCover(book.subject)" :alt="book.title" @error="handleImageError">
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
            <img :src="book.coverImage || getDefaultCover(book.subject)" :alt="book.title" class="textbook-cover" @error="handleImageError">
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
    <div class="modal-overlay confirm-overlay" v-if="showDeleteConfirm" @click="showDeleteConfirm = false">
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
              <img :src="selectedBook?.coverImage || getDefaultCover(selectedBook?.subject)" :alt="selectedBook?.title" @error="handleImageError">
            </div>
            <div class="book-detail-info">
              <h3 class="book-detail-title">{{ selectedBook?.title }}</h3>
              <div class="book-detail-meta">
                <span class="meta-item">作者：{{ selectedBook?.author }}</span>
                <span class="meta-item">出版社：未知出版社</span>
                <span class="meta-item">出版年份：2024</span>
              </div>
              <div class="book-detail-description">
                <h4>书籍简介</h4>
                <p>书籍简介内容，详细描述书籍的主要内容、作者背景、适用人群等信息。书籍简介内容，详细描述书籍的主要内容、作者背景、适用人群等信息。</p>
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
            <button class="action-btn primary" @click="startStudy(selectedBook)">开始学习/继续学习</button>
            <button class="action-btn">编辑</button>
            <button class="action-btn" @click="deleteBook(selectedBook.id)">删除书籍</button>
          </div>
        </div>
        
        <!-- 右侧侧边栏 -->
        <div class="book-detail-sidebar">
          <!-- 完整目录展示区 -->
          <div class="sidebar-section" v-if="showFullCatalog">
            <h4>完整目录</h4>
            <div class="full-catalog">
              <div class="catalog-chapter" v-for="(chapter, index) in 5" :key="index">
                <div class="chapter-title">第{{ index + 1 }}章 {{ chapterTitles[index] }}</div>
                <div class="chapter-sections">
                  <div class="section-item" v-for="(section, secIndex) in 3" :key="secIndex">
                    {{ index + 1 }}.{{ secIndex + 1 }} {{ sectionTitles[secIndex] }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 推荐书籍和用户评价 -->
          <template v-else>
            <!-- 推荐书籍 -->
            <div class="sidebar-section">
              <h4>推荐的类似书籍</h4>
              <div class="recommended-books">
                <div class="recommended-book" v-for="i in 3" :key="i">
                  <div class="recommended-book-cover">
                    <img :src="selectedBook?.coverImage || getDefaultCover(selectedBook?.subject)" :alt="selectedBook?.title" @error="handleImageError">
                  </div>
                  <div class="recommended-book-info">
                    <h5 class="recommended-book-title">书籍标题</h5>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 用户评价 -->
            <div class="sidebar-section">
              <h4>用户评价</h4>
              <div class="user-reviews">
                <div class="review-item">
                  <div class="review-header">
                    <span class="reviewer-name">用户评价</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
  
  <!-- 学习模态框 -->
  <div class="modal-overlay" v-if="showStudyModal" @click="showStudyModal = false">
    <div class="modal-content study-modal" @click.stop>
      <!-- 顶部导航栏 -->
      <div class="study-header">
        <div class="header-left">
          <button class="back-btn" @click="showStudyModal = false">返回</button>
        </div>
      </div>

      <!-- 主体内容区 -->
      <div class="study-content" :class="{ 'with-question-sidebar': showQuestionSidebar }">
        <!-- 左侧目录区 -->
        <div class="sidebar">
          <div class="sidebar-header">
            <h3>目录</h3>
            <button class="collapse-btn">▼</button>
          </div>
          <div class="toc-tree">
            <div class="toc-item">
              <span class="toc-title">第一章</span>
              <div class="toc-children">
                <div class="toc-item active">
                  <span class="toc-title">1.1 知识卡片</span>
                  <div class="toc-children">
                    <div class="toc-item">
                      <span class="toc-title">1.1.1 高亮章节</span>
                    </div>
                    <div class="toc-item">
                      <span class="toc-title">1.1.2 笔记</span>
                    </div>
                  </div>
                </div>
                <div class="toc-item">
                  <span class="toc-title">1.2 树形分析</span>
                  <div class="toc-children">
                    <div class="toc-item">
                      <span class="toc-title">1.2.1 高亮章节</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="toc-item">
              <span class="toc-title">第二章</span>
              <div class="toc-children">
                <div class="toc-item">
                  <span class="toc-title">2.1 高亮</span>
                </div>
                <div class="toc-item">
                  <span class="toc-title">2.2 专业管理</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间阅读区 -->
        <div class="reading-area">
          <div class="reading-header">
            <h2>沉浸式</h2>
            <button class="expand-btn">▼</button>
          </div>
          <div class="reading-content" @mouseup="handleTextSelection">
            <p>知识卡片卡词语的造语句理词中，可以重型正文阅读申述中公务场景。可以将构成正式话的情景正式项，似以方相可使分连句文更传确表达。</p>
            <p>通俗周易行后是，实场中的文字啊确结，可正实文字对比应写入自己在边的工具应用的内容的类结。苏扬性盘文化社中，备通文</p>
            
            <div class="section-title">分清单</div>
            <div class="checklist">
              <div class="check-item">
                <input type="checkbox" checked>
                <span>选择文中在文中处词语理理理服务</span>
              </div>
              <div class="check-item">
                <input type="checkbox">
                <span>准备需要位位置标标标注词文</span>
              </div>
            </div>
            
            <p>一拉训，文选组中的在文字在文的边的理服务，卡片，正文信信正在二分法实用啊应用的可进定义。取后技巧</p>
            <p>人了似切割，复动的实理可，可始解啊</p>
            
            <button class="create-btn">建立新工具</button>
            
            <p>需哈，试选择一种检验工具，可检验确，预学中等内容</p>
            <p>过可还可通过分重组集可应用于中级内容和复习项，流法，</p>
            <p>能在中公务易场景的可性选择场世界，智与智能服人员交交的最在至理为学习的词，可对照实物过一遍后对复人员的实写的进正正文信写后</p>
          </div>
          <div class="pagination">
            <span>1</span>
            <span>2</span>
            <span>3</span>
            <span>6</span>
            <span>14</span>
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
          <button class="footer-btn">
            <span class="btn-icon">📝</span>
            <span class="btn-text">笔记</span>
          </button>
          <button class="footer-btn">
            <span class="btn-icon">🌙</span>
            <span class="btn-text">夜间模式</span>
            <span class="btn-arrow">›</span>
          </button>
        </div>
      </div>
    </div>
    </div><!-- .bookshelf-content -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/api.js'

const router = useRouter()

/* ===== 页面入场动画 & 背景装饰 ===== */
const pageReady = ref(false)
onMounted(async () => {
  await nextTick()
  setTimeout(() => { pageReady.value = true }, 60)
})

const decoStyle = (i, type) => {
  const seed = i * 7 + type.length * 13
  const pseudo = ((seed * 31) % 100) / 100
  if (type === 'star') {
    return {
      left: `${(i * 37 + 11) % 100}%`,
      top: `${(i * 23 + 7) % 100}%`,
      fontSize: `${pseudo * 10 + 8}px`,
      opacity: pseudo * 0.12 + 0.03,
      animationDelay: `${pseudo * 4}s`,
      animationDuration: `${pseudo * 3 + 3}s`,
    }
  }
  if (type === 'dot') {
    return {
      left: `${(i * 53 + 3) % 100}%`,
      top: `${(i * 47 + 5) % 100}%`,
      width: `${pseudo * 6 + 3}px`,
      height: `${pseudo * 6 + 3}px`,
      opacity: pseudo * 0.10 + 0.03,
      animationDelay: `${pseudo * 3}s`,
    }
  }
  return {
    left: `${(i * 59 + 17) % 100}%`,
    top: `${(i * 31 + 23) % 100}%`,
    fontSize: `${pseudo * 10 + 12}px`,
    opacity: pseudo * 0.07 + 0.03,
    animationDelay: `${pseudo * 5}s`,
  }
}

// 在组件挂载时检查是否有保存的学习状态
onMounted(() => {
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

// 状态筛选
const statusFilter = ref('')

// 显示状态下拉菜单
const showStatusDropdown = ref(false)

// 状态选项
const statusOptions = ref([
  { value: '', label: '全部' },
  { value: 'not-started', label: '未开始' },
  { value: 'learning', label: '学习中' },
  { value: 'completed', label: '已完成' }
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
    showSuccessToast.value = false
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

// 获取书籍列表
const fetchBooks = async () => {
  try {
    const response = await api.get('/books')
    books.value = response.data
  } catch (error) {
    console.error('获取书籍失败:', error)
    showToast('获取书籍失败', false)
  }
}

// 搜索书籍
const searchBooks = async () => {
  try {
    const response = await api.get('/books/search', {
      params: { keyword: searchQuery.value, status: statusFilter.value }
    })
    books.value = response.data
  } catch (error) {
    console.error('搜索书籍失败:', error)
  }
}

// 选择状态
const selectStatus = (statusValue) => {
  statusFilter.value = statusValue
  showStatusDropdown.value = false
  searchBooks()
}

// 切换状态下拉菜单
const toggleStatusDropdown = () => {
  showStatusDropdown.value = !showStatusDropdown.value
}

// 获取选中的状态名称
const getSelectedStatusName = () => {
  const status = statusOptions.value.find(s => s.value === statusFilter.value)
  return status ? status.label : '请选择状态'
}

// 在组件挂载时获取书籍列表
onMounted(() => {
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
  
  // 按状态筛选
  if (statusFilter.value) {
    result = result.filter(book => book.status === statusFilter.value)
  }
  
  // 按搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(book => 
      book.title.toLowerCase().includes(query) || 
      book.author.toLowerCase().includes(query)
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

// 学习模态框
const showStudyModal = ref(false)
const studyBook = ref(null)

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

// 打开书籍详情
const openBookDetail = (book) => {
  selectedBook.value = book
  showBookDetailModal.value = true
  showFullCatalog.value = false
  updateLastClicked(book)
}

// 开始学习
const startStudy = (book) => {
  studyBook.value = book
  showStudyModal.value = true
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
      const response = await api.post('/qa/ask', {
        question: userQuestion.value,
        selectedText: selectedText.value,
        bookId: studyBook.value?.id
      })
      aiAnswer.value = response.data.answer
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

// 切换完整目录显示
const toggleFullCatalog = () => {
  showFullCatalog.value = !showFullCatalog.value
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
    const response = await api.get('/books/categories')
    categories.value = response.data
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
    const response = await api.get('/books/textbooks', {
      params: { category: selectedCategory.value, keyword: textbookSearchQuery.value }
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
    const response = await api.post('/books/add', { books: booksToAdd })
    
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
      const response = await api.delete(`/books/${bookToDelete}`)
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
  --c1: #7c5cfc;
  --c2: #a78bfa;
  --c3: #5b9dfc;
  --c4: #5cc9a0;
  --c5: #ffb84d;
  --c6: #fc7bab;

  position: relative;
  padding: 28px 32px 52px;
  min-height: calc(100vh - 60px);
  background:
    radial-gradient(circle at 8% 12%, rgba(167, 139, 250, .12), transparent 26%),
    radial-gradient(circle at 92% 20%, rgba(91, 157, 252, .10), transparent 28%),
    radial-gradient(circle at 70% 88%, rgba(252, 123, 171, .08), transparent 30%),
    linear-gradient(175deg, #fffdfd 0%, #f9f6ff 34%, #f8fafd 64%, #fff8fb 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
  overflow: hidden;
}

/* ---- 背景装饰 ---- */
.bg-decor { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.deco-star { position: absolute; color: #fbbf24; animation: starTwinkle 3s ease-in-out infinite; }
@keyframes starTwinkle { 0%,100%{opacity:.03;transform:scale(1)} 50%{opacity:.15;transform:scale(1.3)} }
.deco-dot { position: absolute; border-radius: 50%; background: #c4b5fd; animation: dotPulse 4s ease-in-out infinite; }
@keyframes dotPulse { 0%,100%{opacity:.04;transform:scale(1)} 50%{opacity:.16;transform:scale(1.8)} }
.deco-book { position: absolute; animation: bookFloat 6s ease-in-out infinite; }
@keyframes bookFloat { 0%,100%{transform:translateY(0) rotate(0deg)} 50%{transform:translateY(-10px) rotate(4deg)} }

/* ---- 内容区域入场动画 ---- */
.bookshelf-content { position: relative; z-index: 1; max-width: 1140px; margin: 0 auto; }
.bookshelf-content > * { opacity: 0; transform: translateY(24px); }
.bookshelf-page.ready .bookshelf-content > * { animation: sectionIn .7s ease-out forwards; }
.bookshelf-page.ready .bookshelf-content > *:nth-child(1) { animation-delay: .05s; }
.bookshelf-page.ready .bookshelf-content > *:nth-child(2) { animation-delay: .15s; }
.bookshelf-page.ready .bookshelf-content > *:nth-child(3) { animation-delay: .25s; }
@keyframes sectionIn { to { opacity: 1; transform: translateY(0); } }

/* 页面头部 */
.bookshelf-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding: 28px 32px 24px;
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(14px);
  border-radius: 20px;
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 2px 20px rgba(120,100,180,.05);
  width: 100%;
  position: relative;
  overflow: hidden;
}

.bookshelf-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #7c5cfc, #a78bfa, #fbbf24, #5cc9a0, #7c5cfc);
  background-size: 200% 100%;
  animation: gradientFlow 6s ease infinite;
}

@keyframes gradientFlow {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  width: 100%;
  max-width: 1200px;
  position: relative;
}

.back-home-btn {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255,255,255,.85);
  backdrop-filter: blur(10px);
  color: #5b4a8a;
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all .25s cubic-bezier(.4,0,.2,1);
  font-family: inherit;
  z-index: 10;
}

.back-home-btn:hover {
  transform: translateY(-50%) translateY(-1px);
  background: #f5f0ff;
  border-color: #c4b5fd;
  box-shadow: 0 4px 14px rgba(124,92,252,.08);
}

.page-title {
  font-size: 28px;
  font-weight: 900;
  color: #1a1528;
  margin: 0;
  letter-spacing: -.5px;
  background: linear-gradient(135deg, #1a1528 20%, #7c5cfc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.back-icon {
  font-size: 15px;
  font-weight: 700;
}

.search-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.search-container {
  flex: 1;
  display: flex;
  justify-content: center;
}

.search-bar {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
}

.add-textbook-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all .3s cubic-bezier(.4,0,.2,1);
  font-family: inherit;
  white-space: nowrap;
  box-shadow: 0 4px 14px rgba(124,92,252,.22);
}

.add-textbook-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(124,92,252,.35);
}

.search-input {
  width: 100%;
  padding: 10px 18px;
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 12px;
  font-size: 14px;
  transition: all .25s;
  font-family: inherit;
  box-sizing: border-box;
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(10px);
  color: #2d2438;
}

.search-input::placeholder {
  color: #b0a8c0;
}

.search-input:focus {
  outline: none;
  border-color: #7c5cfc;
  box-shadow: 0 0 0 3px rgba(124,92,252,.08);
}


/* 筛选区域 */
.bookshelf-filter {
  display: flex;
  align-items: center;
  margin-bottom: 22px;
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(14px);
  padding: 14px 20px;
  border-radius: 16px;
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 2px 16px rgba(120,100,180,.04);
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 13px;
  font-weight: 700;
  color: #5b4a8a;
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
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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
  color: #3b82f6;
}

.filter-select option:checked {
  background: #3b82f6;
  color: white;
  font-weight: 500;
}

.select-arrow {
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
  color: #3b82f6;
}

.custom-select select:focus + .select-arrow {
  transform: translateY(-50%) rotate(180deg);
  color: #3b82f6;
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
  border-color: #93c5fd;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
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
  background: rgba(255,255,255,.96);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(230,224,244,.7);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(120,100,180,.1);
  z-index: 1000;
  margin-top: 6px;
}

.select-option {
  padding: 10px 16px;
  cursor: pointer;
  transition: all .2s;
  font-size: 13px;
  color: #5b4a8a;
}

.select-option:hover {
  background: #f5f0ff;
  color: #7c5cfc;
}

.select-option.active {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: white;
  font-weight: 700;
}

.select-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 10px;
  background: rgba(255,255,255,.7);
  cursor: pointer;
  transition: all .25s;
  font-size: 13px;
  color: #5b4a8a;
}

.select-display:hover {
  border-color: #c4b5fd;
  box-shadow: 0 2px 8px rgba(124,92,252,.08);
}

.select-display .select-arrow {
  font-size: 12px;
  color: #6b7280;
  transition: all 0.3s ease;
}

.select-display:hover .select-arrow {
  color: #3b82f6;
  transform: rotate(180deg);
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
  background: rgba(30, 20, 50, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  overflow: hidden;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: rgba(255,255,255,.96);
  backdrop-filter: blur(18px);
  border-radius: 20px;
  padding: 32px;
  width: 1000px;
  max-width: 90%;
  height: 700px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 24px 60px rgba(120,100,180,.12);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(200,185,240,.2);
}

.modal-title {
  font-size: 22px;
  font-weight: 800;
  color: #1a1528;
  margin: 0;
  font-family: inherit;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #9088a0;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all .25s;
}

.modal-close:hover {
  background: #f5f0ff;
  color: #7c5cfc;
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
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 分类标签 */
.category-section {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.category-btn {
  padding: 7px 16px;
  background: rgba(255,255,255,.75);
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 999px;
  cursor: pointer;
  transition: all .25s cubic-bezier(.4,0,.2,1);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: #5b4a8a;
}

.category-btn:hover {
  border-color: #c4b5fd;
  color: #7c5cfc;
  transform: translateY(-1px);
}

.category-btn.active {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff;
  border-color: transparent;
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
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(10px);
  border: 1.5px solid rgba(230,224,244,.6);
  border-radius: 14px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all .35s cubic-bezier(.4,0,.2,1);
  position: relative;
  overflow: hidden;
}

.textbook-card:hover {
  border-color: #c4b5fd;
  box-shadow: 0 8px 24px rgba(124,92,252,.1);
  transform: translateY(-4px);
}

.textbook-card.selected {
  border-color: #7c5cfc;
  background: rgba(124,92,252,.06);
}

.textbook-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 10px;
  margin-bottom: 12px;
}

.textbook-title {
  font-size: 13px;
  font-weight: 600;
  color: #5b4a8a;
  margin-bottom: 8px;
  font-family: inherit;
}

.select-checkbox {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 26px;
  height: 26px;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  box-shadow: 0 2px 10px rgba(124,92,252,.35);
}

/* 批量添加按钮 */
.batch-add-btn {
  padding: 16px 48px;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all .3s cubic-bezier(.4,0,.2,1);
  font-family: inherit;
  box-shadow: 0 4px 14px rgba(124,92,252,.22);
}

.batch-add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(124,92,252,.35);
}

.batch-add-text {
  font-size: 14px;
  color: #9088a0;
  font-family: inherit;
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
  gap: 18px;
  margin-bottom: 32px;
}

/* 书籍卡片 */
.book-card {
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(14px);
  border-radius: 16px;
  overflow: hidden;
  transition: all .4s cubic-bezier(.34,1.56,.64,1);
  position: relative;
  cursor: pointer;
  border: 1px solid rgba(230,224,244,.6);
}

.book-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(180deg, #7c5cfc, #a78bfa);
  border-radius: 0 3px 3px 0;
  z-index: 5;
  transition: height .4s cubic-bezier(.34,1.56,.64,1);
}

.book-card::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: linear-gradient(135deg, rgba(255,255,255,.4) 0%, transparent 50%, rgba(255,255,255,.1) 100%);
  opacity: 0;
  transition: opacity .35s;
  border-radius: 16px;
}

.book-card:hover {
  transform: translateY(-6px) perspective(600px) rotateX(1deg);
  box-shadow: 0 20px 44px rgba(120,100,180,.12), 0 0 0 1.5px rgba(160,140,220,.25);
}

.book-card:hover::before {
  height: 60%;
}

.book-card:hover::after {
  opacity: 1;
}

.book-card:active {
  transform: translateY(-2px) scale(.99);
}

/* 状态标签 */
.book-status {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  z-index: 10;
}

/* 删除按钮 */
.delete-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 26px;
  height: 26px;
  background: rgba(239, 68, 68, 0.85);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .25s;
  opacity: 0;
  transform: scale(.8);
}

.book-card:hover .delete-btn {
  opacity: 1;
  transform: scale(1);
}

.delete-btn:hover {
  background: rgba(220, 38, 38, 1);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4);
  transform: scale(1.1);
}

.book-status.not-started {
  background: #f0ecfc;
  color: #5b4a8a;
}

.book-status.learning {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: white;
}

.book-status.completed {
  background: linear-gradient(135deg, #5cc9a0, #3db88b);
  color: white;
}

/* 书籍封面 */
.book-cover {
  width: 100%;
  height: 220px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #faf7ff, #f3f0fc);
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .4s cubic-bezier(.4,0,.2,1);
}

.book-card:hover .book-cover img {
  transform: scale(1.05);
}

/* 书籍信息 */
.book-info {
  padding: 16px;
}

.book-title {
  font-size: 15px;
  font-weight: 700;
  color: #2d2438;
  margin: 0 0 6px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-author {
  font-size: 13px;
  color: #9088a0;
  margin: 0 0 12px 0;
}

/* 进度条 */
.progress-bar {
  width: 100%;
  height: 6px;
  background: #f0ecf8;
  border-radius: 99px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #a78bfa, #7c5cfc);
  border-radius: 99px;
  transition: width .6s cubic-bezier(.4,0,.2,1);
  box-shadow: 0 0 8px rgba(124,92,252,.18);
  position: relative;
  overflow: hidden;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,.35), transparent);
  animation: shimmer 2.5s ease-in-out infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  100% { left: 100%; }
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
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.5);
}

/* 书籍详情弹窗 */
.book-detail-modal {
  max-width: 1100px;
  max-height: 85vh;
  overflow-y: hidden;
  background: rgba(255,255,255,.96);
  backdrop-filter: blur(18px);
  border-radius: 20px;
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 24px 60px rgba(120,100,180,.12);
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
  border: 1.5px solid rgba(200,185,240,.25);
  border-radius: 10px;
  padding: 12px;
  transition: all .25s cubic-bezier(.4,0,.2,1);
}

.catalog-chapter:hover {
  border-color: #c4b5fd;
  box-shadow: 0 4px 14px rgba(124,92,252,.06);
}

.chapter-title {
  font-size: 14px;
  font-weight: 700;
  color: #2d2438;
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
  color: #807a90;
  padding: 4px 0;
  transition: color .2s;
}

.section-item:hover {
  color: #7c5cfc;
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
  border-left: 1px solid rgba(200,185,240,.2);
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
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(120,100,180,.08);
}

.book-detail-info {
  flex: 1;
}

.book-detail-title {
  font-size: 22px;
  font-weight: 800;
  color: #1a1528;
  margin-bottom: 14px;
}

.book-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  color: #9088a0;
  font-size: 13px;
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
  font-size: 15px;
  font-weight: 700;
  color: #1a1528;
  margin-bottom: 8px;
}

.book-detail-description p {
  color: #807a90;
  line-height: 1.6;
  font-size: 13px;
}

.catalog-link {
  color: #7c5cfc;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: inline-block;
  padding: 4px 0;
  transition: color .2s;
}

.catalog-link:hover {
  color: #a78bfa;
}

.book-detail-progress {
  margin-bottom: 16px;
}

.book-detail-progress h4 {
  font-size: 15px;
  font-weight: 700;
  color: #1a1528;
  margin-bottom: 12px;
}

.progress-container {
  background: rgba(248,246,255,.7);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(200,185,240,.2);
}

.progress-container.small {
  padding: 10px;
}

.progress-track {
  position: relative;
  height: 8px;
  background: #f0ecf8;
  border-radius: 99px;
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
  background: #3b82f6;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
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
  padding: 9px 18px;
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 10px;
  background: rgba(255,255,255,.75);
  color: #5b4a8a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .25s cubic-bezier(.4,0,.2,1);
  font-family: inherit;
}

.action-btn:hover {
  background: #f5f0ff;
  border-color: #c4b5fd;
  transform: translateY(-1px);
}

.action-btn.primary {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 14px rgba(124,92,252,.22);
}

.action-btn.primary:hover {
  box-shadow: 0 8px 24px rgba(124,92,252,.35);
  transform: translateY(-2px);
}

/* 侧边栏样式 */
.sidebar-section {
  margin-bottom: 28px;
}

.sidebar-section h4 {
  font-size: 14px;
  font-weight: 700;
  color: #1a1528;
  margin-bottom: 14px;
}

.recommended-books {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommended-book {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: rgba(248,246,255,.7);
  border-radius: 10px;
  border: 1px solid rgba(230,224,244,.3);
  transition: all .25s cubic-bezier(.4,0,.2,1);
}

.recommended-book:hover {
  border-color: #c4b5fd;
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
}

.user-reviews {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  padding: 14px;
  background: rgba(248,246,255,.5);
  border-radius: 10px;
  border: 1.5px solid rgba(200,185,240,.2);
}

.review-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
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
    border-top: 1px solid rgba(200,185,240,.2);
    padding-left: 0;
    padding-top: 24px;
  }
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 32px;
  margin-bottom: 20px;
}

.pagination-btn {
  padding: 9px 18px;
  border: 1.5px solid rgba(200,185,240,.35);
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #5b4a8a;
  transition: all .25s;
  font-family: inherit;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #c4b5fd;
  color: #7c5cfc;
  transform: translateY(-1px);
}

.pagination-btn:disabled {
  opacity: .4;
  cursor: not-allowed;
}

.pagination-numbers {
  display: flex;
  gap: 6px;
}

.pagination-number {
  width: 38px;
  height: 38px;
  border: 1.5px solid rgba(200,185,240,.35);
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #5b4a8a;
  transition: all .25s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}

.pagination-number:hover {
  border-color: #c4b5fd;
  color: #7c5cfc;
  transform: translateY(-1px);
}

.pagination-number.active {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 10px rgba(124,92,252,.22);
}

/* 成功提示样式 */
.toast {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(30, 20, 50, .88);
  backdrop-filter: blur(18px);
  color: white;
  padding: 22px 36px;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,.1);
  box-shadow: 0 12px 40px rgba(30, 20, 50, .3);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  z-index: 2000;
  animation: fadeIn 0.3s ease, fadeOut 0.3s ease 2.7s;
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
.confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(30, 20, 50, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
}

.confirm-modal {
  background: rgba(255,255,255,.96);
  backdrop-filter: blur(18px);
  border-radius: 18px;
  border: 1px solid rgba(230,224,244,.7);
  box-shadow: 0 24px 60px rgba(120,100,180,.15);
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  animation: confirmIn .35s cubic-bezier(.34,1.56,.64,1);
}

@keyframes confirmIn {
  from { opacity: 0; transform: scale(.9) translateY(12px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.confirm-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(200,185,240,.2);
  display: flex;
  align-items: center;
  gap: 10px;
}

.confirm-title {
  font-size: 17px;
  font-weight: 800;
  color: #1a1528;
  margin: 0;
}

.confirm-body {
  padding: 24px;
}

.confirm-message {
  font-size: 14px;
  color: #807a90;
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
  padding: 9px 20px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all .25s cubic-bezier(.4,0,.2,1);
  flex: 1;
  max-width: 120px;
  font-family: inherit;
}

.confirm-btn.cancel {
  background: #f0ecfc;
  color: #5b4a8a;
  border: 1.5px solid rgba(200,185,240,.35);
}

.confirm-btn.cancel:hover {
  background: #e8e0fa;
}

.confirm-btn.delete {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  box-shadow: 0 4px 12px rgba(239, 68, 68, .25);
}

.confirm-btn.delete:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(239, 68, 68, .35);
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
  background: rgba(255,255,255,.96);
  backdrop-filter: blur(18px);
  border-radius: 20px;
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 24px 60px rgba(120,100,180,.12);
  display: flex;
  flex-direction: column;
}

/* 学习模态框头部 */
.study-header {
  background: rgba(255,255,255,.85);
  backdrop-filter: blur(14px);
  padding: 14px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(200,185,240,.2);
}

.study-header .header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.study-header .back-btn, .study-header .menu-btn, .study-header .search-btn {
  padding: 8px 16px;
  background: rgba(248,246,255,.7);
  border: 1.5px solid rgba(200,185,240,.25);
  border-radius: 10px;
  color: #5b4a8a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .25s;
  font-family: inherit;
}

.study-header .back-btn:hover, .study-header .menu-btn:hover, .study-header .search-btn:hover {
  background: #f5f0ff;
  border-color: #c4b5fd;
}

.study-header .menu-btn.active {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff;
  border-color: transparent;
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
  background: rgba(248,246,255,.7);
  border: 1.5px solid rgba(200,185,240,.25);
  border-radius: 10px;
  cursor: pointer;
  transition: all .25s;
}

.study-header .icon-btn:hover {
  background: #f5f0ff;
  border-color: #c4b5fd;
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
  background: linear-gradient(175deg, #fefdfc 0%, #f9f6ff 20%, #f8fafd 50%, #fff8fb 100%);
}

/* 显示提问侧边栏时的布局 */
.study-content.with-question-sidebar .reading-area {
  flex: 1;
}

/* 提问侧边栏 */
.question-sidebar {
  width: 300px;
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(14px);
  border-radius: 14px;
  border: 1px solid rgba(230,224,244,.6);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.question-sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(200,185,240,.2);
  background: rgba(255,255,255,.5);
}

.question-sidebar-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #2d2438;
}

.question-sidebar-header .close-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  font-size: 20px;
  color: #9088a0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all .2s;
}

.question-sidebar-header .close-btn:hover {
  background: #f5f0ff;
  color: #7c5cfc;
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
  background: rgba(255,255,255,.7);
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(230,224,244,.6);
}

.selected-text h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 700;
  color: #5b4a8a;
}

.selected-text p {
  margin: 0;
  font-size: 13px;
  color: #807a90;
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
  font-size: 13px;
  font-weight: 700;
  color: #5b4a8a;
}

.question-input textarea {
  flex: 1;
  padding: 12px;
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 10px;
  font-size: 13px;
  color: #2d2438;
  resize: none;
  min-height: 120px;
  font-family: inherit;
  transition: all .25s;
  background: rgba(255,255,255,.7);
}

.question-input textarea:focus {
  outline: none;
  border-color: #7c5cfc;
  box-shadow: 0 0 0 3px rgba(124,92,252,.08);
}

.question-input.small textarea {
  min-height: 60px;
  max-height: 80px;
}

.question-input textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.submit-question-btn {
  padding: 12px 16px;
  background: #3b82f6;
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
  background: #2563eb;
}

.submit-question-btn:active {
  transform: scale(0.98);
}

/* AI回答区域 */
.ai-answer {
  margin-top: 16px;
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #dbeafe;
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
  color: #3b82f6;
  border: 1px solid #3b82f6;
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
  background: rgba(255,255,255,.7);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(230,224,244,.6);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.study-content .sidebar-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(200,185,240,.2);
  background: rgba(255,255,255,.5);
}

.study-content .sidebar-header h3 {
  font-size: 14px;
  font-weight: 800;
  color: #1a1528;
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
  color: #9088a0;
  font-size: 12px;
}

/* 目录树 */
.toc-tree {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.toc-item {
  position: relative;
  padding: 6px 16px;
  cursor: pointer;
  transition: all .2s;
  border-radius: 6px;
  margin: 2px 8px;
  color: #5b4a8a;
  font-size: 13px;
}

.toc-item:hover {
  background: #f5f0ff;
}

.toc-item.active {
  background: #f0ecfc;
  color: #7c5cfc;
  font-weight: 600;
}

.toc-item.active {
  background: #eff6ff;
  color: #3b82f6;
}

.toc-title {
  font-size: 13px;
  display: block;
  line-height: 1.5;
  font-weight: 500;
}

.toc-children {
  margin-left: 12px;
  margin-top: 2px;
  border-left: 1.5px solid rgba(200,185,240,.3);
  padding-left: 8px;
}

.toc-children .toc-item {
  padding: 4px 12px;
  margin: 1px 4px;
}

.toc-children .toc-title {
  font-size: 12px;
  font-weight: 400;
}

/* 阅读区 */
.reading-area {
  flex: 1;
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(230,224,244,.6);
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
  accent-color: #3b82f6;
}

.create-btn {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
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
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
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
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
  border: 1px solid #bfdbfe;
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
  background: #3b82f6;
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
</style>