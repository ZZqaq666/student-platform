<template>
  <div class="bookshelf-page">
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
          <img :src="book.cover" :alt="book.title">
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
            <img :src="book.cover" :alt="book.title" class="textbook-cover">
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
      <div class="toast-icon">✓</div>
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
              <img :src="selectedBook?.cover" :alt="selectedBook?.title">
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
            <button class="action-btn primary">开始学习/继续学习</button>
            <button class="action-btn">编辑</button>
            <button class="action-btn">删除书籍</button>
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
                    <img :src="selectedBook?.cover" :alt="selectedBook?.title">
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
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 搜索查询
const searchQuery = ref('')

// 教材搜索查询
const textbookSearchQuery = ref('')

// 成功提示
const showSuccessToast = ref(false)
const successMessage = ref('')

// 确认删除对话框
const showDeleteConfirm = ref(false)
let bookToDelete = null

// 显示成功提示
const showToast = (message) => {
  successMessage.value = message
  showSuccessToast.value = true
  setTimeout(() => {
    showSuccessToast.value = false
  }, 3000)
}

// 搜索书籍
const searchBooks = () => {
  // 搜索功能已经通过计算属性实现，这里只需要触发重新计算
  // 可以添加额外的搜索逻辑如果需要
  console.log('搜索书籍:', searchQuery.value)
}

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

// 选择状态
const selectStatus = (statusValue) => {
  statusFilter.value = statusValue
  showStatusDropdown.value = false
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

// 目录标题数据
const chapterTitles = ref([
  '基础知识',
  '核心概念',
  '进阶技巧',
  '实战应用',
  '总结与展望'
])

const sectionTitles = ref([
  '基本定义',
  '重要定理',
  '典型例题'
])

// 书籍数据
const books = ref([
  {
    id: 1,
    title: '禄学职讲名菜',
    author: '蓝人书语',
    cover: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=200&h=250&fit=crop',
    status: 'not-started',
    progress: 0,
    lastClicked: new Date('2024-01-15').getTime()
  },
  {
    id: 2,
    title: '文书法',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=200&h=250&fit=crop',
    status: 'not-started',
    progress: 0,
    lastClicked: new Date('2024-01-10').getTime()
  },
  {
    id: 3,
    title: '学习书',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=200&h=250&fit=crop',
    status: 'learning',
    progress: 50,
    lastClicked: new Date('2024-01-20').getTime()
  },
  {
    id: 4,
    title: '师情手册服务',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1589998059171-988d887df646?w=200&h=250&fit=crop',
    status: 'learning',
    progress: 60,
    lastClicked: new Date('2024-01-25').getTime()
  },
  {
    id: 5,
    title: '准论书语',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=200&h=250&fit=crop',
    status: 'completed',
    progress: 100,
    lastClicked: new Date('2024-01-05').getTime()
  },
  {
    id: 6,
    title: '学习教证',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1531901599431-363981c7e049?w=200&h=250&fit=crop',
    status: 'not-started',
    progress: 0,
    lastClicked: new Date('2024-01-12').getTime()
  },
  {
    id: 7,
    title: '老教文明谈成',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=200&h=250&fit=crop',
    status: 'learning',
    progress: 70,
    lastClicked: new Date('2024-01-18').getTime()
  },
  {
    id: 8,
    title: '学习问人才',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1542903660-eed4a463d952?w=200&h=250&fit=crop',
    status: 'learning',
    progress: 40,
    lastClicked: new Date('2024-01-22').getTime()
  },
  {
    id: 9,
    title: '与籍材',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1587825140041-b41b2a7c600e?w=200&h=250&fit=crop',
    status: 'learning',
    progress: 80,
    lastClicked: new Date('2024-01-16').getTime()
  },
  {
    id: 10,
    title: '借态教学学教育',
    author: '名人书语',
    cover: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=200&h=250&fit=crop',
    status: 'completed',
    progress: 100,
    lastClicked: new Date('2024-01-08').getTime()
  }
])

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

// 打开书籍详情
const openBookDetail = (book) => {
  selectedBook.value = book
  showBookDetailModal.value = true
  showFullCatalog.value = false
  updateLastClicked(book)
}

// 切换完整目录显示
const toggleFullCatalog = () => {
  showFullCatalog.value = !showFullCatalog.value
}

// 添加教材
const addTextbook = () => {
  showAddTextbookModal.value = true
}

// 更新最近点击时间
const updateLastClicked = (book) => {
  book.lastClicked = Date.now()
}

// 分类
const categories = ['计算机', '医学', '教育', '考研', '职业资格', '四六级']
const selectedCategory = ref('计算机')

// 选中的书籍
const selectedBooks = ref([])

// 教材数据
const textbooks = ref([
  { title: '计算机', cover: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=200&h=250&fit=crop' },
  { title: '职业资格', cover: 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=200&h=250&fit=crop' },
  { title: '名校英语语法', cover: 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=200&h=250&fit=crop' },
  { title: '教分里', cover: 'https://images.unsplash.com/photo-1589998059171-988d887df646?w=200&h=250&fit=crop' },
  { title: '考研十年', cover: 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=200&h=250&fit=crop' },
  { title: '扫码六级', cover: 'https://images.unsplash.com/photo-1531901599431-363981c7e049?w=200&h=250&fit=crop' },
  { title: '计算机基础', cover: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=200&h=250&fit=crop' },
  { title: '职业资格', cover: 'https://images.unsplash.com/photo-1542903660-eed4a463d952?w=200&h=250&fit=crop' },
  { title: '中考全面超越', cover: 'https://images.unsplash.com/photo-1587825140041-b41b2a7c600e?w=200&h=250&fit=crop' }
])

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
const selectCategory = (category) => {
  selectedCategory.value = category
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
const batchAdd = () => {
  if (selectedBooks.value.length === 0) {
    alert('请选择要添加的教材')
    return
  }
  
  // 将选择的书籍添加到个人书架
  selectedBooks.value.forEach(index => {
    const textbook = textbooks.value[index]
    if (textbook) {
      // 生成唯一ID
      const newId = books.value.length > 0 ? Math.max(...books.value.map(book => book.id)) + 1 : 1
      
      // 添加到书架，标记为未完成
      books.value.push({
        id: newId,
        title: textbook.title,
        author: '未知作者', // 这里可以根据实际情况修改
        cover: textbook.cover,
        status: 'not-started', // 标记为未完成
        progress: 0, // 初始进度为0
        lastClicked: new Date().getTime() // 当前时间
      })
    }
  })
  
  // 显示成功消息
  showToast(`已成功添加 ${selectedBooks.value.length} 本教材到个人书架`)
  
  // 关闭模态框并清空选中列表
  showAddTextbookModal.value = false
  selectedBooks.value = []
  textbookSearchQuery.value = '' // 清空搜索框
}

// 删除书籍
const deleteBook = (bookId) => {
  bookToDelete = bookId
  showDeleteConfirm.value = true
}

// 确认删除
const confirmDelete = () => {
  if (bookToDelete) {
    const index = books.value.findIndex(book => book.id === bookToDelete)
    if (index > -1) {
      books.value.splice(index, 1)
      showToast('书籍删除成功')
      
      // 检查当前页码是否需要调整
      if (paginatedBooks.value.length === 0 && currentPage.value > 1) {
        currentPage.value--
      }
    }
    showDeleteConfirm.value = false
    bookToDelete = null
  }
}

// 返回首页
const backToHome = () => {
  router.push('/')
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

.back-home-btn {
  position: absolute;
  left: -100px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
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
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.back-home-btn:active {
  transform: translateY(-50%) translateY(0);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin: 0;
  background: linear-gradient(135deg, #3b82f6 0%, #10b981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(59, 130, 246, 0.1);
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
  margin-right: 20px;
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
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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
  border-radius: 12px;
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
}

.modal-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0;
  font-family: 'Microsoft YaHei', sans-serif;
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
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
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
  border-color: #3b82f6;
  color: #3b82f6;
}

.category-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
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
  border-color: #3b82f6;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);
  transform: translateY(-4px);
}

.textbook-card.selected {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}

.textbook-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 12px;
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
  background: #3b82f6;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
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
  background: #3b82f6;
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
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
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
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
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
  color: #3b82f6;
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
  color: #3b82f6;
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
  padding: 10px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  color: #374151;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.action-btn.primary {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-color: #3b82f6;
}

.action-btn.primary:hover {
  background: linear-gradient(90deg, #2563eb 0%, #1d4ed8 100%);
  border-color: #2563eb;
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
    border-top: 1px solid #e5e7eb;
    padding-left: 0;
    padding-top: 24px;
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
  border-color: #3b82f6;
  color: #3b82f6;
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
  border-color: #3b82f6;
  color: #3b82f6;
}

.pagination-number.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

/* 成功提示样式 */
.toast {
  position: fixed;
  top: 60px;
  right: 20px;
  background: rgba(16, 185, 129, 0.95);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 2000;
  animation: slideInRight 0.3s ease, fadeOut 0.3s ease 2.7s;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(4px);
  min-width: auto;
  max-width: 200px;
}

.toast:hover {
  transform: translateX(-4px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.toast-icon {
  font-size: 14px;
  font-weight: bold;
}

.toast-message {
  font-size: 12px;
  font-weight: 500;
  font-family: 'Microsoft YaHei', sans-serif;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
</style>