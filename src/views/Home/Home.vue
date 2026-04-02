<template>
  <div class="home-page">
    <!-- 快速入口 -->
    <div class="section-title">快速入口</div>
    <el-row :gutter="16" class="quick-entry">
      <el-col :span="6">
        <div class="entry-card blue" @click="toPage('/qa')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">问答中心</div>
              <div class="card-subtitle">ai问答</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">💬</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="entry-card cyan" @click="toPage('/graph')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">知识图谱</div>
              <div class="card-subtitle">辅助</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">🌐</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="entry-card blue" @click="toPage('/bookshelf')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">大学教材库</div>
              <div class="card-subtitle">教材资源</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">📚</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="entry-card cyan" @click="toPage('/senior')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">学长学姐答疑专区</div>
              <div class="card-subtitle">进步方式</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">👨‍🎓</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近一本书的学习进度 -->
    <div class="book-progress-section">
      <div class="section-title">最近学习</div>
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>
      <div class="book-progress-card" v-else-if="recentBook">
        <div class="book-info">
          <div class="book-image-placeholder" v-if="!getBookCover()">
            <span class="placeholder-text">书籍封面</span>
          </div>
          <img :src="getBookCover()" :alt="getBookTitle()" class="book-cover" v-else>
          <div class="book-details">
            <h3 class="book-title">{{ getBookTitle() }}</h3>
            <p class="book-author">{{ getBookAuthor() }}</p>
            <div class="progress-info">
              <span class="progress-text">已读 {{ getBookProgress() }}%</span>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: getBookProgress() + '%' }"></div>
              </div>
              <div class="last-read-info">
                <span class="last-read-text">最后阅读: {{ formatLastReadTime() }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 学习曲线图（包含日期和进度） -->
        <div class="chart-section">
          <h4 class="chart-title">学习趋势</h4>
          <div class="chart-container">
            <svg viewBox="0 0 800 250" class="learning-chart">
              <defs>
                <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" style="stop-color:#3b82f6;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#06b6d4;stop-opacity:1" />
                </linearGradient>
                <linearGradient id="areaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" style="stop-color:#3b82f6;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#3b82f6;stop-opacity:0" />
                </linearGradient>
              </defs>
              
              <!-- 网格线 -->
              <line x1="60" y1="40" x2="750" y2="40" stroke="#e5e7eb" stroke-width="1"/>
              <line x1="60" y1="90" x2="750" y2="90" stroke="#e5e7eb" stroke-width="1"/>
              <line x1="60" y1="140" x2="750" y2="140" stroke="#e5e7eb" stroke-width="1"/>
              <line x1="60" y1="190" x2="750" y2="190" stroke="#e5e7eb" stroke-width="1"/>
              
              <!-- Y轴标签 -->
              <text x="30" y="45" text-anchor="middle" fill="#9ca3af" font-size="12">100%</text>
              <text x="30" y="95" text-anchor="middle" fill="#9ca3af" font-size="12">75%</text>
              <text x="30" y="145" text-anchor="middle" fill="#9ca3af" font-size="12">50%</text>
              <text x="30" y="195" text-anchor="middle" fill="#9ca3af" font-size="12">25%</text>
              
              <!-- X轴日期标签 -->
              <text v-for="(point, index) in processedLearningData" :key="index" 
                    :x="100 + index * 150" y="230" text-anchor="middle" fill="#9ca3af" font-size="12">
                {{ formatDate(point.date) }}
              </text>
              
              <!-- 学习曲线区域 -->
              <path :d="areaPath" fill="url(#areaGradient)"/>
              
              <!-- 学习曲线 -->
              <path :d="linePath" fill="none" stroke="url(#lineGradient)" stroke-width="3"/>
              
              <!-- 数据点 -->
              <circle v-for="(point, index) in processedLearningData" :key="index" 
                      :cx="100 + index * 150" 
                      :cy="200 - (point.progress / 100) * 160" 
                      r="5" fill="#3b82f6"/>
            </svg>
          </div>
          
          <!-- 学习统计信息 -->
          <div class="learning-stats">
            <div class="stat-item">
              <div class="stat-value">{{ getTotalStudyTime() }}</div>
              <div class="stat-label">总学习时间</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ getTotalPagesRead() }}</div>
              <div class="stat-label">总阅读页数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ processedLearningData.length }}</div>
              <div class="stat-label">学习天数</div>
            </div>
          </div>
        </div>
      </div>
      <div class="no-data" v-else>
        <div class="no-data-content">
          <div class="no-data-icon">📚</div>
          <p class="no-data-text">暂无学习数据</p>
          <button class="no-data-btn" @click="toPage('/bookshelf')">去添加书籍</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiService } from '@/api/api.js'

const router = useRouter()
// 跳转到对应页面
const toPage = (path) => router.push(path)

// 最近学习的书籍
const recentBook = ref(null)
// 学习数据
const learningData = ref([])
// 加载状态
const loading = ref(true)

// 获取最近学习的书籍
const fetchRecentBook = async () => {
  try {
    // 使用apiService的learning.getHistory方法获取学习历史，然后找到最近的书籍
    const response = await apiService.learning.getHistory()
    if (response && response.data) {
      // 首先尝试直接获取recentBook字段
      if (response.data.recentBook) {
        recentBook.value = response.data.recentBook
      } else if (response.data.learningHistory && response.data.learningHistory.length > 0) {
        // 如果没有recentBook字段，从learningHistory中找到最近的学习记录
        const sortedHistory = [...response.data.learningHistory].sort((a, b) => {
          const dateA = new Date(a.date || a.Date || 0)
          const dateB = new Date(b.date || b.Date || 0)
          return dateB - dateA
        })
        
        if (sortedHistory.length > 0) {
          // 获取最近的学习记录
          const latestRecord = sortedHistory[0]
          // 构建recentBook对象
          recentBook.value = {
            id: latestRecord.bookId || latestRecord.book_id,
            title: latestRecord.bookTitle || latestRecord.book_title || '未知书籍',
            author: latestRecord.bookAuthor || latestRecord.book_author || '未知作者',
            progress: latestRecord.progress || 0,
            lastReadAt: latestRecord.date || latestRecord.Date
          }
        }
      }
    }
  } catch (error) {
    console.error('获取最近学习书籍失败:', error)
  }
}

// 获取学习数据
const fetchLearningData = async () => {
  try {
    const response = await apiService.learning.getHistory()
    if (response && response.data) {
      learningData.value = response.data.learningHistory || []
    }
  } catch (error) {
    console.error('获取学习数据失败:', error)
  }
}

// 处理学习数据，确保数据结构正确
const processedLearningData = computed(() => {
  if (!learningData.value || learningData.value.length === 0) {
    return []
  }
  
  return learningData.value.map(item => {
    // 确保每个数据项都有必要的字段
    return {
      date: item.date || item.Date || new Date().toISOString().split('T')[0],
      progress: item.progress || item.Progress || 0,
      pagesRead: item.pagesRead || item.PagesRead || 0,
      studyTimeMinutes: item.studyTimeMinutes || item.StudyTimeMinutes || 0
    }
  }).sort((a, b) => new Date(a.date) - new Date(b.date))
})

// 获取书籍封面
const getBookCover = () => {
  if (!recentBook.value) return ''
  // 处理不同的数据结构
  let coverUrl = ''
  if (recentBook.value.cover) coverUrl = recentBook.value.cover
  if (recentBook.value.book && recentBook.value.book.coverImage) coverUrl = recentBook.value.book.coverImage
  if (recentBook.value.book && recentBook.value.book.cover) coverUrl = recentBook.value.book.cover
  
  // 如果已经是完整的图片链接（包含http://或https://），直接返回
  if (coverUrl && (coverUrl.startsWith('http://') || coverUrl.startsWith('https://'))) {
    return coverUrl
  }
  // 否则使用后端图片代理服务处理
  if (coverUrl) {
    return `/api/image-proxy/proxy?url=${encodeURIComponent(coverUrl)}`
  }
  return ''
}

// 获取书籍标题
const getBookTitle = () => {
  if (!recentBook.value) return '未知书籍'
  // 处理不同的数据结构
  if (recentBook.value.title) return recentBook.value.title
  if (recentBook.value.book && recentBook.value.book.title) return recentBook.value.book.title
  return '未知书籍'
}

// 获取书籍作者
const getBookAuthor = () => {
  if (!recentBook.value) return '未知作者'
  // 处理不同的数据结构
  if (recentBook.value.author) return recentBook.value.author
  if (recentBook.value.book && recentBook.value.book.author) return recentBook.value.book.author
  return '未知作者'
}

// 获取书籍进度
const getBookProgress = () => {
  if (!recentBook.value) return 0
  // 处理不同的数据结构
  if (recentBook.value.progress) return recentBook.value.progress
  if (recentBook.value.book && recentBook.value.book.progress) return recentBook.value.book.progress
  return 0
}

// 格式化最后阅读时间
const formatLastReadTime = () => {
  if (!recentBook.value) return '未知'
  
  let lastReadAt = null
  // 处理不同的数据结构
  if (recentBook.value.lastReadAt) lastReadAt = recentBook.value.lastReadAt
  if (recentBook.value.book && recentBook.value.book.lastReadAt) lastReadAt = recentBook.value.book.lastReadAt
  
  if (!lastReadAt) return '未知'
  
  const date = new Date(lastReadAt)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  })
}

// 计算总学习时间
const getTotalStudyTime = () => {
  const totalMinutes = processedLearningData.value.reduce((total, item) => {
    return total + (item.studyTimeMinutes || 0)
  }, 0)
  
  if (totalMinutes < 60) {
    return `${totalMinutes}分钟`
  } else {
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60
    return `${hours}小时${minutes}分钟`
  }
}

// 计算总阅读页数
const getTotalPagesRead = () => {
  return processedLearningData.value.reduce((total, item) => {
    return total + (item.pagesRead || 0)
  }, 0)
}

// 计算区域路径
const areaPath = computed(() => {
  if (processedLearningData.value.length === 0) {
    return 'M 100,200 L 700,200 Z'
  }
  const points = processedLearningData.value.map((point, index) => {
    const x = 100 + index * 150
    const y = 200 - (point.progress / 100) * 160
    return `${x},${y}`
  })
  return `M ${points.join(' T ')} L 700,200 L 100,200 Z`
})

// 计算线路径
const linePath = computed(() => {
  if (processedLearningData.value.length === 0) {
    return 'M 100,200 L 700,200'
  }
  const points = processedLearningData.value.map((point, index) => {
    const x = 100 + index * 150
    const y = 200 - (point.progress / 100) * 160
    return `${x},${y}`
  })
  return `M ${points.join(' T ')}`
})

// 组件挂载时获取数据
onMounted(async () => {
  loading.value = true
  try {
    await fetchRecentBook()
    await fetchLearningData()
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* 页面标题 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 区块标题 */
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

/* 快速入口卡片 */
.quick-entry {
  margin-bottom: 32px;
}

.entry-card {
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100px;
  display: flex;
  align-items: center;
}

.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.entry-card.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.entry-card.cyan {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.card-text {
  color: white;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 14px;
  opacity: 0.9;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-bg {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  font-size: 24px;
}

/* 书籍学习进度部分 */
.book-progress-section {
  margin-top: 24px;
}

.loading-container {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.book-progress-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.book-progress-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.book-info {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
  align-items: center;
}

.book-image-placeholder {
  width: 120px;
  height: 160px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.book-cover {
  width: 120px;
  height: 160px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

.book-cover:hover {
  transform: scale(1.02);
}

.placeholder-text {
  color: #6366f1;
  font-size: 14px;
  font-weight: 500;
}

.book-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.book-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.book-author {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 20px 0;
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-text {
  font-size: 16px;
  font-weight: 600;
  color: #3b82f6;
}

.progress-bar {
  height: 10px;
  background: #e5e7eb;
  border-radius: 5px;
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 5px;
  transition: width 0.3s ease;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.4);
}

.last-read-info {
  margin-top: 4px;
}

.last-read-text {
  font-size: 12px;
  color: #9ca3af;
}

/* 无数据状态 */
.no-data {
  background: white;
  border-radius: 16px;
  padding: 48px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  text-align: center;
  transition: all 0.3s ease;
}

.no-data:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.no-data-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.no-data-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.no-data-text {
  color: #6b7280;
  font-size: 16px;
  margin: 0;
}

.no-data-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.no-data-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* 图表部分 */
.chart-section {
  border-top: 1px solid #e5e7eb;
  padding-top: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px 0;
}

.chart-container {
  width: 100%;
  height: 260px;
  margin-bottom: 24px;
}

.learning-chart {
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.learning-chart:hover {
  transform: scale(1.01);
}

/* 学习统计信息 */
.learning-stats {
  display: flex;
  gap: 24px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.stat-item {
  flex: 1;
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: #f3f4f6;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }
  
  .quick-entry {
    margin-bottom: 24px;
  }
  
  .entry-card {
    height: 90px;
    padding: 16px;
  }
  
  .card-title {
    font-size: 16px;
  }
  
  .book-info {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .book-details {
    align-items: center;
  }
  
  .book-title {
    font-size: 20px;
  }
  
  .chart-container {
    height: 200px;
  }
  
  .learning-stats {
    flex-direction: column;
    gap: 12px;
  }
  
  .stat-item {
    padding: 12px;
  }
}
</style>
