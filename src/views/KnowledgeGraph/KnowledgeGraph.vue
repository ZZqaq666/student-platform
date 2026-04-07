<template>
  <div class="knowledge-graph-page">
    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 知识图谱可视化区域 -->
      <div class="graph-container">
        <!-- 顶部工具栏 -->
        <div class="graph-toolbar">
          <button class="toolbar-btn home-btn" title="返回首页" @click="goToHome">
            <span class="toolbar-icon">←</span>
            <span class="toolbar-text">返回首页</span>
          </button>
          
          <!-- 书籍选择 -->
          <div class="book-selector">
            <div class="book-selector-header">
              <button class="toolbar-btn" :class="{ active: showBookSelector }" @click="toggleBookSelector">
                <span class="toolbar-icon">📖</span>
                <span class="toolbar-text">选择书籍</span>
              </button>
              
              <!-- 书籍选择界面 -->
              <div class="book-selector-container" v-if="showBookSelector">
                <div class="book-selector-header">
                  <h4>选择书籍</h4>
                  <button class="book-selector-close" @click="toggleBookSelector">✕</button>
                </div>
                <div class="book-selector-content">
                  <!-- 书架书籍 -->
                  <div class="book-section">
                    <h5>我的书架</h5>
                    <div class="bookshelf-list">
                      <!-- 加载状态 -->
                      <div v-if="isLoadingBooks" class="loading-books">
                        <span>加载书籍中...</span>
                      </div>
                      <!-- 空状态 -->
                      <div v-else-if="bookshelfBooks.length === 0" class="no-books">
                        <span>书架中暂无书籍</span>
                      </div>
                      <!-- 书籍列表 -->
                      <div 
                        v-for="book in bookshelfBooks" 
                        :key="book.id"
                        class="bookshelf-item"
                        @click="selectBook(book)"
                      >
                        <span class="book-title">{{ book.title }}</span>
                        <span class="book-author">{{ book.author }}</span>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 搜索书籍 -->
                  <div class="book-section">
                    <h5>搜索书籍</h5>
                    <div class="book-search">
                      <input 
                        type="text" 
                        v-model="bookSearchQuery"
                        placeholder="搜索书籍名称、作者"
                        @input="handleBookSearch"
                      />
                      <button class="search-btn">🔍</button>
                    </div>
                    <div class="search-results" v-if="bookSearchResults.length > 0">
                      <div 
                        v-for="(book, index) in bookSearchResults" 
                        :key="book.id || `search_book_${index}`"
                        class="search-book-item"
                        @click="selectBook(book)"
                      >
                        <span class="book-title">{{ book.title }}</span>
                        <span class="book-author">{{ book.author }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="toolbar-buttons">
            <button class="toolbar-btn" :class="{ active: viewMode !== 'standard' }" title="简化视图" @click="handleToggleViewMode(viewMode, chart, currentGraphData)">
              <span class="toolbar-icon">🌐</span>
              <span class="toolbar-text">{{ viewModeText }}</span>
            </button>
            <button class="toolbar-btn" title="搜索节点" @click="toggleSearch">
              <span class="toolbar-icon">🔍</span>
              <span class="toolbar-text">搜索节点</span>
            </button>
            
            <!-- 搜索界面 -->
            <div class="search-container" v-if="showSearch">
              <div class="search-header">
                <div class="search-input-wrapper">
                  <input 
                    type="text" 
                    v-model="searchQuery" 
                    placeholder="搜索知识点、公式、章节名称，比如 '牛顿-莱布尼茨公式'"
                    @input="handleSearchInput"
                    @blur="handleSearchBlur"
                    ref="searchInput"
                  />
                  <button class="search-clear" @click="clearSearch" v-if="searchQuery">✕</button>
                </div>
                <button class="search-close" @click="closeSearch">✕</button>
              </div>
              <div class="search-results" v-if="showSearchResults">
                <!-- 历史搜索记录 -->
                <div class="search-section" v-if="historySearch.length > 0">
                  <h4>历史搜索</h4>
                  <div class="history-list">
                    <div 
                      v-for="(item, index) in historySearch" 
                      :key="`history_${index}_${item}`"
                      class="history-item"
                      @click="searchByHistory(item)"
                    >
                      {{ item }}
                    </div>
                  </div>
                </div>
                
                <!-- 联想匹配结果 -->
                <div class="search-section" v-if="searchSuggestions.length > 0">
                  <h4>联想匹配</h4>
                  <div class="suggestion-list">
                    <div 
                      v-for="(item, index) in searchSuggestions" 
                      :key="item.id || `suggestion_${index}_${item.name}`"
                      class="suggestion-item"
                      @click="selectSuggestion(item)"
                    >
                      <span class="suggestion-name">{{ item.name }}</span>
                      <span class="suggestion-module">{{ item.module }} - {{ item.level }}</span>
                    </div>
                  </div>
                </div>
                
                <!-- 搜索结果 -->
                <div class="search-section" v-if="searchResults.length > 0">
                  <h4>搜索结果</h4>
                  <div class="result-list">
                    <div 
                      v-for="(item, index) in searchResults" 
                      :key="item.id || `result_${index}_${item.name}`"
                      class="result-item"
                      @click="selectSearchResult(item)"
                    >
                      <span class="result-name">{{ item.name }}</span>
                      <span class="result-info">{{ item.module }} - {{ item.level }} | {{ item.priority }}</span>
                    </div>
                  </div>
                  <div class="result-actions">
                    <button class="action-btn" @click="highlightAllResults">全部高亮</button>
                    <button class="action-btn" @click="clearHighlights">取消高亮</button>
                  </div>
                </div>
                
                <!-- 无匹配结果 -->
                <div class="search-section no-results" v-if="showNoResults">
                  <h4>未找到匹配的知识点</h4>
                  <p>你可以：① 检查关键词是否正确；② 切换到对应课程；③ 查看本课程全部知识点列表</p>
                  <div class="no-results-actions">
                    <button class="action-btn" @click="showAllNodes">查看全部知识点</button>
                  </div>
                  <div class="related-suggestions" v-if="relatedSuggestions.length > 0">
                    <h5>推荐相关知识点</h5>
                    <div class="related-list">
                      <div 
                        v-for="(item, index) in relatedSuggestions" 
                        :key="item.id || `related_${index}_${item.name}`"
                        class="related-item"
                        @click="selectSuggestion(item)"
                      >
                        {{ item.name }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <button class="toolbar-btn" title="导出PDF" @click="exportToPDF">
              <span class="toolbar-icon">📄</span>
              <span class="toolbar-text">导出PDF</span>
            </button>
          </div>
        </div>

        <!-- 图谱展示区 - ECharts -->
        <div class="graph-canvas" ref="chartContainer" style="width: 100%; height: 500px;">
          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading-overlay">
            <div class="loading-spinner"></div>
            <div class="loading-text">正在加载知识图谱...</div>
          </div>
        </div>
      </div>

      <!-- 右侧侧边栏 -->
      <div class="sidebar">
        <!-- 节点标题 -->
        <div class="node-header">
          <h2 class="node-title" v-if="selectedNode">{{ selectedNode }}</h2>
          <h2 class="node-title error" v-else>错误：未选择节点</h2>
          <div class="node-subtitle" v-if="selectedNode">{{ nodeSubtitle }}</div>
          <div class="node-subtitle error" v-else>请选择知识图谱中的节点</div>
        </div>

        <!-- 知识密度条 -->
        <div class="knowledge-density" v-if="selectedNode">
          <div class="density-header">
            <span class="density-label">知识密度</span>
            <span class="density-value">{{ knowledgeDensity }}%</span>
          </div>
          <div class="density-bar">
            <div class="density-fill" :style="{ width: knowledgeDensity + '%' }"></div>
          </div>
          <div class="density-desc">覆盖全部章节内容</div>
        </div>

        <!-- 关联知识点 -->
        <div class="related-nodes" v-if="selectedNode">
          <h3 class="section-title">关联知识点</h3>
          <div class="related-list">
            <div 
              v-for="(node, index) in relatedNodes" 
              :key="`node_${index}_${node}`"
              class="related-tag"
              @click="focusOnNode(node)"
            >
              {{ node }}
            </div>
          </div>
        </div>

        <!-- 高频知识点 -->
        <div class="original-text" v-if="selectedNode">
          <h3 class="section-title">高频知识点</h3>
          <div class="text-preview">
            <ul class="knowledge-list">
              <li v-for="(knowledge, index) in highFrequencyKnowledge" :key="`knowledge_${index}_${knowledge}`">{{ knowledge }}</li>
            </ul>
          </div>
        </div>

        <!-- 学习行动区 -->
        <div class="learning-actions" v-if="selectedNode">
          <h3 class="section-title">学习工具</h3>
          <div class="action-buttons">
            <button class="action-btn" @click="openExerciseModal">
              <span class="action-icon">📚</span>
              <span class="action-text">推荐关联习题</span>
            </button>
          </div>
        </div>

        <!-- 错误提示 -->
        <div class="error-message" v-else>
          <div class="error-icon">⚠️</div>
          <div class="error-text">
            <p>请在知识图谱中选择一个节点以查看详细信息</p>
            <p>点击知识图谱中的节点即可显示相关内容</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 习题弹窗 -->
    <div v-if="showExerciseModal" class="exercise-modal-overlay" @click="closeExerciseModal">
      <div class="exercise-modal" @click.stop>
        <!-- 弹窗头部 -->
        <div class="exercise-modal-header">
          <div class="header-left">
            <span class="header-icon">📚</span>
            <span class="header-title">{{ selectedNode || '知识节点' }} · 关联习题</span>
          </div>
          <div class="header-right">
            <button class="header-btn close-btn" @click="closeExerciseModal">✕</button>
          </div>
        </div>

        <!-- 难度筛选标签 -->
        <div class="difficulty-tabs">
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'all' }"
            @click="changeExerciseFilter('all')"
          >全部</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'basic' }"
            @click="changeExerciseFilter('basic')"
          >基础巩固</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'advanced' }"
            @click="changeExerciseFilter('advanced')"
          >进阶提升</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'exam' }"
            @click="changeExerciseFilter('exam')"
          >真题冲刺</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'competition' }"
            @click="changeExerciseFilter('competition')"
          >竞赛拓展</button>
        </div>

        <!-- 习题类型筛选 -->
        <div class="type-tabs">
          <button 
            class="tab-btn" 
            :class="{ active: exerciseTypeFilter === 'all' }"
            @click="changeExerciseTypeFilter('all')"
          >全部类型</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseTypeFilter === '概念题' }"
            @click="changeExerciseTypeFilter('概念题')"
          >概念题</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseTypeFilter === '应用题' }"
            @click="changeExerciseTypeFilter('应用题')"
          >应用题</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseTypeFilter === '分析题' }"
            @click="changeExerciseTypeFilter('分析题')"
          >分析题</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseTypeFilter === '编程题' }"
            @click="changeExerciseTypeFilter('编程题')"
          >编程题</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseTypeFilter === '设计题' }"
            @click="changeExerciseTypeFilter('设计题')"
          >设计题</button>
        </div>

        <!-- 筛选工具栏 -->
        <div class="filter-toolbar">
          <div class="filter-left">
            <span class="filter-info">共 {{ filteredExercises.length }} 道习题</span>
          </div>
          <div class="filter-right">
            <button class="toolbar-btn" @click="addToWrongBook">批量加入错题本</button>
            <button class="toolbar-btn primary" @click="selectAllExercises">全选</button>
          </div>
        </div>

        <!-- 习题列表 -->
        <div class="exercise-list">
          <div 
            v-for="exercise in filteredExercises" 
            :key="exercise.id"
            class="exercise-card"
            :class="{ selected: exercise.selected }"
            @click="toggleExerciseSelection(exercise)"
          >
            <div class="card-header">
              <span class="exercise-title">{{ exercise.title }}</span>
              <div class="tag-container">
                <span class="difficulty-tag" :class="exercise.adjustedDifficulty || exercise.difficulty" :style="{ backgroundColor: getDifficultyColor(exercise.adjustedDifficulty || exercise.difficulty) + '20', color: getDifficultyColor(exercise.adjustedDifficulty || exercise.difficulty) }">{{ exercise.adjustedDifficulty || exercise.difficulty }}</span>
                <span class="type-tag">{{ exercise.type }}</span>
              </div>
            </div>
            <div class="card-body">
              <p class="exercise-desc">{{ exercise.description }}</p>
              
              <!-- 推荐理由 -->
              <div class="recommendation-reason" v-if="exercise.recommendationReason">
                <span class="reason-label">推荐理由:</span>
                <span class="reason-text">{{ exercise.recommendationReason }}</span>
              </div>
              
              <!-- 知识点标签 -->
              <div class="knowledge-tags">
                <span 
                  v-for="(point, index) in exercise.knowledgePoints" 
                  :key="`point_${index}_${point}`"
                  class="knowledge-tag"
                >
                  {{ point }}
                </span>
              </div>
            </div>
            <div class="card-footer">
              <button class="card-btn primary" @click.stop="startSingleExercise(exercise)">开始练习</button>
              <button class="card-btn" @click.stop>加入错题</button>
            </div>
          </div>
        </div>

        <!-- 底部统计栏 -->
        <div class="modal-footer">
          <div class="footer-stats">
            <span>共{{ filteredExercises.length }}道习题</span>
            <span class="divider">|</span>
            <span>已选{{ selectedCount }}道</span>
          </div>
          <div class="footer-actions">
            <button class="footer-btn start-btn" @click="startExercise">开始刷题</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 学习进度弹窗 -->
    <div class="progress-modal" v-if="showProgressModal">
      <div class="progress-modal-content">
        <div class="progress-modal-header">
          <h3>{{ selectedKnowledgePoint }} 学习进度</h3>
          <button class="close-btn" @click="showProgressModal = false">✕</button>
        </div>
        <div class="progress-modal-body">
          <!-- 掌握程度 -->
          <div class="mastery-section">
            <h4>掌握程度</h4>
            <div class="mastery-bar">
              <div class="mastery-fill" :style="{ width: `${knowledgeMastery * 100}%` }"></div>
            </div>
            <div class="mastery-text">{{ Math.round(knowledgeMastery * 100) }}%</div>
          </div>
          
          <!-- 练习历史 -->
          <div class="history-section">
            <h4>练习历史</h4>
            <div class="history-list">
              <div class="history-item" v-for="(item, index) in practiceHistory" :key="`practice_${index}_${item.date}`">
                <div class="history-date">{{ item.date }}</div>
                <div class="history-stats">
                  <span>{{ item.correct }}/{{ item.total }}</span>
                  <span class="history-mastery">{{ Math.round(item.mastery * 100) }}%</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 推荐建议 -->
          <div class="recommendation-section">
            <h4>推荐建议</h4>
            <div class="recommendation-list">
              <div class="recommendation-item" v-if="knowledgeMastery < 0.6">
                建议加强基础练习，巩固{{ selectedKnowledgePoint }}的基本概念。
              </div>
              <div class="recommendation-item" v-else-if="knowledgeMastery < 0.8">
                建议进行进阶练习，提高{{ selectedKnowledgePoint }}的应用能力。
              </div>
              <div class="recommendation-item" v-else>
                掌握程度良好，建议挑战更高难度的习题，拓展{{ selectedKnowledgePoint }}的应用场景。
              </div>
            </div>
          </div>
        </div>
        <div class="progress-modal-footer">
          <button class="footer-btn" @click="showProgressModal = false">关闭</button>
          <button class="footer-btn primary" @click="openExerciseModal">开始练习</button>
        </div>
      </div>
    </div>

    <!-- 提示消息 -->
    <div class="toast" v-if="showToast">
      <div class="toast-content">{{ toastMessage }}</div>
    </div>
  </div>
</template>

<script setup>
console.log('KnowledgeGraph.vue script started')
import { ref, computed, onMounted, onUnmounted } from 'vue'

import { useRoute, useRouter } from 'vue-router'
import api, { apiService } from '@/api/api.js'

// 为组件指定名称
defineOptions({
  name: 'KnowledgeGraph'
})

console.log('KnowledgeGraph.vue script imports completed')

// 导入工具文件
import { initChart as initECharts, toggleViewMode, highlightRelatedNodes, clearHighlights, destroyChart } from './utils/chartConfig.js'
import { generateDefaultGraphData, fetchExercisesUtil, updateRecommendationStrategyUtil } from './utils/dataProcessor.js'
import { showToastMessage, goToHome as goToHomeUtil, toggleBookSelector as toggleBookSelectorUtil, handleBookSearch as handleBookSearchUtil, selectBook as selectBookUtil, toggleSearch as toggleSearchUtil, closeSearch as closeSearchUtil, handleSearchInput as handleSearchInputUtil, handleSearchBlur as handleSearchBlurUtil, clearSearch as clearSearchUtil, searchByHistory as searchByHistoryUtil, selectSuggestion as selectSuggestionUtil, selectSearchResult as selectSearchResultUtil, highlightAllResults as highlightAllResultsUtil, showAllNodes as showAllNodesUtil, focusOnNode as focusOnNodeUtil, exportToPDF as exportToPDFUtil, openExerciseModal as openExerciseModalUtil, changeExerciseFilter as changeExerciseFilterUtil, changeExerciseTypeFilter as changeExerciseTypeFilterUtil, startSingleExercise as startSingleExerciseUtil, startExercise as startExerciseUtil, addToWrongBook as addToWrongBookUtil, handleToggleViewMode } from './utils/eventHandlers.js'
import { getModuleName, getLevelName, getDifficultyColor } from './utils/index.js'
import { loadLearningProgress } from './utils/learningProgress.js'

// 添加中文字体支持
import 'jspdf/dist/jspdf.umd.min.js'

const route = useRoute()
const router = useRouter()

// 图表实例
const chartContainer = ref(null)
let chart = null

// 当前图谱数据
const currentGraphData = ref({
  nodes: [],
  links: []
})

// 状态
const selectedNode = ref('')
const viewMode = ref('standard')
const showSearch = ref(false)
const searchQuery = ref('')
const searchResults = ref([])
const searchSuggestions = ref([])
const historySearch = ref([])
const showSearchResults = ref(false)
const showNoResults = ref(false)
const relatedSuggestions = ref([])
const relatedNodes = ref(['微积分', '线性代数', '概率论', '常微分方程'])
const highFrequencyKnowledge = ref([
  '高等数学是大学数学的基础课程',
  '包括微积分、线性代数、概率论等核心内容',
  '是理工科学生的必修课程',
  '是许多专业课程的基础'
])
const knowledgeDensity = ref(100)
const nodeSubtitle = ref('核心课程')
// 组件状态管理
const componentState = {
  isMounted: ref(false),
  isDestroyed: ref(false),
  isComponentDestroyed: false,
  abortControllers: new Set(),
  timers: new Set(),
  pendingPromises: new Set()
}


// 从后端获取书架书籍
const fetchBookshelfBooks = async () => {
  if (!componentState.isMounted.value) return
  
  isLoadingBooks.value = true
  try {
    const response = await apiService.bookshelf.getAllBooks()
    if (response && response.code === 200 && response.data) {
      bookshelfBooks.value = response.data
      console.log('成功从后端获取书架书籍:', bookshelfBooks.value.length, '本书籍')
    } else {
      console.error('获取书架书籍响应格式错误:', response)
      bookshelfBooks.value = []
    }
  } catch (error) {
    console.error('获取书架书籍失败:', error)
    bookshelfBooks.value = []
  } finally {
    isLoadingBooks.value = false
  }
}

// 从后端获取节点详情，包括高频考点（使用模拟数据）
const fetchNodeDetails = async (nodeId) => {
  if (!componentState.isMounted.value || componentState.isDestroyed.value) return
  
  try {
    // 模拟API请求延迟
    await new Promise(resolve => setTimeout(resolve, 300))
    
    // 生成模拟节点详情数据
    const nodeName = selectedNode.value || '未知节点'
    const mockHighFrequencyKnowledge = [
      `${nodeName}是重要的知识点`,
      `掌握${nodeName}对理解相关内容至关重要`,
      `${nodeName}在实际应用中经常出现`,
      `建议通过练习巩固${nodeName}的相关知识`
    ]
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value || componentState.isDestroyed.value) return
    
    // 更新高频知识点
    highFrequencyKnowledge.value = mockHighFrequencyKnowledge
    console.log('使用模拟数据更新节点详情:', nodeName)
    console.log('模拟高频知识点:', mockHighFrequencyKnowledge)
  } catch (error) {
    console.error('获取节点详情失败:', error)
  }
}

// 书籍选择相关
const showBookSelector = ref(false)
const bookSearchQuery = ref('')
const bookSearchResults = ref([])
const currentBook = ref(null)
const isLoadingBooks = ref(false)

// 习题弹窗相关
const showExerciseModal = ref(false)
const exerciseFilter = ref('all')
const exerciseTypeFilter = ref('all')
const exercises = ref([])
const recommendedExercises = ref([])
const exerciseDifficulty = ref('balanced')
console.log('exerciseDifficulty defined', exerciseDifficulty)
const isLoading = ref(false)
const toastMessage = ref('')
const showToast = ref(false)

// 用户学习进度数据
const userLearningProgress = ref({
  books: {},
  lastUpdated: new Date().toISOString()
})

// 习题推荐参数

// 习题与知识点映射

// 获取习题列表
const fetchExercises = async (nodeName) => {
  await fetchExercisesUtil(nodeName, exerciseFilter, exercises, componentState)
}





// 监听用户答题情况，实时更新推荐

// 实时更新推荐策略
const updateRecommendationStrategy = () => {
  updateRecommendationStrategyUtil(selectedNode, currentBook, recommendedExercises, componentState)
}




// 定期更新推荐
let recommendationUpdateInterval

// 启动推荐更新定时器
const startRecommendationUpdates = () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping startRecommendationUpdates')
    return
  }
  
  // 每30秒更新一次推荐
  recommendationUpdateInterval = setInterval(() => {
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) {
      clearInterval(recommendationUpdateInterval)
      recommendationUpdateInterval = null
      return
    }
    updateRecommendationStrategy()
  }, 30000)
}

// 停止推荐更新定时器
const stopRecommendationUpdates = () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping stopRecommendationUpdates')
    return
  }
  
  if (recommendationUpdateInterval) {
    clearInterval(recommendationUpdateInterval)
    recommendationUpdateInterval = null
  }
}

// 更新学习进度


// 初始化时加载学习进度
loadLearningProgress(userLearningProgress)

// 书架数据
const bookshelfBooks = ref([
  { id: 1, title: '集合与函数', author: '高中数学' },
  { id: 205, title: '高等数学（上册）', author: '同济大学' },
  { id: 206, title: '高等数学（下册）', author: '同济大学' },
  { id: 216, title: '离散数学', author: '耿素云' }
])
console.log('Bookshelf books initialized:', bookshelfBooks.value)

// 视图模式文本
const viewModeText = computed(() => {
  return viewMode.value === 'standard' ? '简化视图' : '标准视图'
})

// 为节点添加初始位置，避免重叠


// 初始化 ECharts
const initChart = () => {
  console.log('initChart called')
  // 检查组件是否已挂载
  if (!componentState.isMounted.value) {
    console.warn('组件未挂载，跳过图表初始化')
    return
  }
  
  if (!chartContainer.value) {
    console.error('图表容器未找到')
    return
  }
  
  if (chart) {
    chart.dispose()
    chart = null
  }
  
  chartContainer.value.style.width = '100%'
  chartContainer.value.style.height = '100%'
  
  // 检查容器高度
  const graphContainer = chartContainer.value.parentElement
  console.log('graphContainer:', graphContainer)
  console.log('graphContainer clientHeight:', graphContainer.clientHeight)
  console.log('chartContainer clientHeight:', chartContainer.value.clientHeight)
  
  // 强制设置高度
  chartContainer.value.style.height = `${graphContainer.clientHeight - 60}px` // 减去工具栏高度
  console.log('chartContainer style height:', chartContainer.value.style.height)
  
  // 生成默认数据
  if (currentGraphData.value.nodes.length === 0) {
    currentGraphData.value = generateDefaultGraphData('高等数学')
  }
  
  // 初始化图表
  chart = initECharts(chartContainer.value, currentGraphData.value, handleNodeClick)
  console.log('chart initialized:', chart)
  console.log('chart container size:', chart.getWidth(), 'x', chart.getHeight())
  
  

}

// 组件挂载
onMounted(async () => {
  console.log('onMounted called')
  componentState.isMounted.value = true
  componentState.isDestroyed.value = false
  componentState.isComponentDestroyed = false
  
  try {
    // 检查 chartContainer 元素
    console.log('chartContainer.value:', chartContainer.value)
    if (chartContainer.value) {
      console.log('chartContainer parent:', chartContainer.value.parentElement)
    }
    
    // 初始化图表
    console.log('Calling initChart')
    initChart()
    
    // 获取书架书籍
    console.log('Calling fetchBookshelfBooks')
    await fetchBookshelfBooks()
    
    // 启动推荐更新定时器
    console.log('Calling startRecommendationUpdates')
    startRecommendationUpdates()
    
    // 检查路由参数，是否需要打开习题弹窗
    if (route.query.showExerciseModal === 'true') {
      showExerciseModal.value = true
    }
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', handleResize)
    
    // 调试：默认显示书籍选择器
    showBookSelector.value = true
    console.log('Bookshelf books:', bookshelfBooks.value)
    

    
    console.log('KnowledgeGraph组件初始化完成')
  } catch (error) {
    console.error('组件初始化失败:', error)
    showToastMessage('组件初始化失败，请刷新页面', 'error')
  }
})

// 组件卸载
onUnmounted(() => {
  // 1. 立即标记为卸载
  componentState.isMounted.value = false
  componentState.isDestroyed.value = true
  componentState.isComponentDestroyed = true

  // 2. 取消所有请求
  componentState.abortControllers.forEach(controller => {
    try {
      controller.abort()
    } catch (error) {
      console.warn('取消请求失败:', error)
    }
  })
  componentState.abortControllers.clear()

  // 3. 清理图表实例
  if (chart) {
    destroyChart(chart)
    chart = null
  }

  // 4. 清除所有定时器
  componentState.timers.forEach(timer => {
    clearTimeout(timer)
  })
  componentState.timers.clear()

  // 5. 清除轮询定时器
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }

  // 6. 清理pending promises
  componentState.pendingPromises.clear()

  // 7. 停止推荐更新定时器
  stopRecommendationUpdates()

  // 8. 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize)

  console.log('KnowledgeGraph组件已完全清理')
})

// 获取节点颜色


// 处理节点点击
const handleNodeClick = (nodeName) => {
  // 检查组件是否已销毁
  if (componentState.isDestroyed.value || !componentState.isMounted.value) {
    console.warn('Component destroyed, skipping handleNodeClick')
    return
  }
  
  // 检查 nodeName 是否存在
  if (!nodeName) {
    console.warn('Node name is empty, skipping handleNodeClick')
    return
  }
  
  console.log('Node clicked:', nodeName)
  selectedNode.value = nodeName
  
  // 更新侧边栏
  updateSidebarContent(nodeName)
  
  // 高亮关联节点
  if (chart && currentGraphData.value && componentState.isMounted.value && !componentState.isDestroyed.value) {
    highlightRelatedNodes(chart, nodeName, currentGraphData.value)
  }
}

// 更新侧边栏内容
const updateSidebarContent = (nodeName) => {
  // 检查组件是否已销毁
  if (componentState.isDestroyed.value || !componentState.isMounted.value) {
    console.warn('Component destroyed, skipping updateSidebarContent')
    return
  }
  
  // 检查 nodeName 是否存在
  if (!nodeName) {
    console.warn('Node name is empty, skipping updateSidebarContent')
    return
  }
  
  // 检查 currentGraphData 是否存在
  if (!currentGraphData.value || !currentGraphData.value.nodes) {
    console.warn('Graph data not available, skipping updateSidebarContent')
    return
  }
  
  const node = currentGraphData.value.nodes.find(n => n.name === nodeName)
  if (!node) {
    console.warn('Node not found:', nodeName)
    return
  }
  
  console.log('Updating sidebar for node:', nodeName)
  
  // 检查组件是否已销毁（再次检查，因为可能在异步操作期间组件被销毁）
  if (componentState.isDestroyed.value || !componentState.isMounted.value) {
    console.warn('Component destroyed during updateSidebarContent')
    return
  }
  
  // 更新副标题
  const subtitles = ['核心课程', '章节模块', '核心知识点', '细分知识点']
  nodeSubtitle.value = subtitles[node.category] || '知识点'
  
  // 更新知识密度
  knowledgeDensity.value = node.value || 50
  
  // 从后端获取节点详情，包括高频考点
  if (componentState.isMounted.value && !componentState.isDestroyed.value) {
    fetchNodeDetails(node.id)
  }
  
  // 临时使用默认高频知识点，等待后端数据
  const currentBookTitle = currentBook.value?.title || '高等数学'
  highFrequencyKnowledge.value = [
    `${nodeName}是${currentBookTitle}的重要组成部分`,
    `掌握${nodeName}对理解后续内容至关重要`,
    `${nodeName}在考试中出现频率较高`,
    `建议通过练习巩固${nodeName}的相关知识`
  ]
  
  // 更新关联节点
  // 从当前图谱数据中动态获取关联节点
  const relatedNodeIds = new Set()
  if (currentGraphData.value.links) {
    currentGraphData.value.links.forEach(link => {
      if (link.source == node.id) {
        relatedNodeIds.add(link.target)
      } else if (link.target == node.id) {
        relatedNodeIds.add(link.source)
      }
    })
  }
  
  // 获取关联节点的名称
  const relatedNames = currentGraphData.value.nodes
    .filter(n => relatedNodeIds.has(n.id))
    .map(n => n.name)
  
  relatedNodes.value = relatedNames.length > 0 ? relatedNames : ['无关联节点']
}

// 显示提示消息


// 学习进度弹窗
const showProgressModal = ref(false)
const selectedKnowledgePoint = ref('')
const knowledgeMastery = ref(0)
const practiceHistory = ref([])

// 显示学习进度

// 开始单个习题练习
const startSingleExercise = (exercise) => {
  startSingleExerciseUtil(exercise, router)
}
// 聚焦到节点
const focusOnNode = (nodeName) => {
  focusOnNodeUtil(nodeName, handleNodeClick)
}

// 显示所有节点
const showAllNodes = () => {
  showAllNodesUtil(clearHighlights, closeSearch)
}

// 切换书籍选择器
const toggleBookSelector = () => {
  toggleBookSelectorUtil(showBookSelector)
}

// 处理书籍搜索
const handleBookSearch = async () => {
  if (!componentState.isMounted.value || componentState.isDestroyed.value) return
  await handleBookSearchUtil(bookSearchQuery, bookSearchResults, componentState)
}

// 选择书籍
const selectBook = (book) => {
  if (!componentState.isMounted.value || componentState.isDestroyed.value) return
  selectBookUtil(book, currentBook, selectedNode, showBookSelector, isLoading, currentGraphData, chart, updateSidebarForBook, componentState, handleNodeClick)
}



// 轮询检查知识图谱是否生成完成
let pollingTimer = null


// 打开习题弹窗
const openExerciseModal = async () => {
  if (!componentState.isMounted.value || componentState.isDestroyed.value) return
  await openExerciseModalUtil(showExerciseModal, selectedNode, fetchExercises, componentState)
}

// 切换习题难度筛选
const changeExerciseFilter = async (filter) => {
  if (!componentState.isMounted.value || componentState.isDestroyed.value) return
  await changeExerciseFilterUtil(filter, exerciseFilter, fetchExercises, selectedNode, componentState)
}

// 切换习题类型筛选
const changeExerciseTypeFilter = async (filter) => {
  if (!componentState.isMounted.value || componentState.isDestroyed.value) return
  await changeExerciseTypeFilterUtil(filter, exerciseTypeFilter, fetchExercises, selectedNode, componentState)
}



// 根据书籍更新图谱
// 用于跟踪当前正在进行的侧边栏更新请求
let sidebarUpdateAbortController = null
// 用于跟踪所有AbortController

// 根据书籍更新侧边栏信息
const updateSidebarForBook = (book) => {
  console.log('=== updateSidebarForBook called ===')
  console.log('Book:', book)
  
  // 直接同步更新响应式变量
  console.log('=== Updating sidebar data ===')
  console.log('Updating selectedNode to:', book.title)
  
  // 立即更新基本信息
  selectedNode.value = book.title
  nodeSubtitle.value = '核心课程'
  knowledgeDensity.value = 100
  
  // 设置默认的高频知识点和关联知识点
  const defaultHighFrequency = [
    `${book.title}是重要的学习内容`,
    `掌握${book.title}对理解相关领域至关重要`,
    `${book.title}包含多个核心知识点`,
    `建议通过练习巩固${book.title}的相关知识`
  ]
  
  const defaultRelated = ['核心知识点', '重要概念', '实践应用', '相关领域']
  
  console.log('Using default highFrequencyKnowledge:', defaultHighFrequency)
  console.log('Using default relatedNodes:', defaultRelated)
  
  // 立即更新知识点数据
  highFrequencyKnowledge.value = defaultHighFrequency
  relatedNodes.value = defaultRelated
  
  console.log('After update - selectedNode:', selectedNode.value)
  console.log('After update - highFrequencyKnowledge:', highFrequencyKnowledge.value)
  console.log('After update - relatedNodes:', relatedNodes.value)
  
  // 后台异步获取真实数据（不阻塞UI更新）
  fetchSidebarData(book)
}

// 后台异步获取侧边栏数据（使用模拟数据）
const fetchSidebarData = async (book) => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value || componentState.isDestroyed.value) {
    console.warn('Component destroyed, skipping fetchSidebarData')
    return
  }
  
  try {
    // 模拟API请求延迟
    await new Promise(resolve => setTimeout(resolve, 300))
    
    // 生成模拟高频知识点数据
    const mockHighFrequencyKnowledge = [
      `${book.title}是重要的学习内容`,
      `掌握${book.title}对理解相关领域至关重要`,
      `${book.title}包含多个核心知识点`,
      `建议通过练习巩固${book.title}的相关知识`
    ]
    
    // 生成模拟关联知识点数据
    const mockRelatedNodes = ['核心知识点', '重要概念', '实践应用', '相关领域']
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value || componentState.isDestroyed.value) {
      return
    }
    
    // 更新数据
    highFrequencyKnowledge.value = mockHighFrequencyKnowledge
    relatedNodes.value = mockRelatedNodes
    
    console.log(`=== 使用模拟数据更新书籍 ${book.title} 的侧边栏信息 ===`)
    console.log('模拟高频知识点:', mockHighFrequencyKnowledge)
    console.log('模拟关联知识点:', mockRelatedNodes)
  } catch (error) {
    if (error.message.includes('destroyed')) {
      console.warn('Component destroyed during fetchSidebarData')
      return
    }
    console.error('获取书籍侧边栏信息失败:', error)
  }
}

// 切换搜索
const toggleSearch = () => {
  toggleSearchUtil(showSearch, componentState)
}

// 导出PDF
const exportToPDF = async () => {
  await exportToPDFUtil(chartContainer, chart, selectedNode, componentState)
}

// 返回首页
const goToHome = () => {
  goToHomeUtil(router)
}

// 关闭搜索
const closeSearch = () => {
  closeSearchUtil(showSearch, searchQuery, searchResults, searchSuggestions, showSearchResults, showNoResults)
}

// 处理搜索输入
const handleSearchInput = () => {
  handleSearchInputUtil(searchQuery, searchSuggestions, searchResults, showSearchResults, showNoResults, currentGraphData.value, getModuleName, getLevelName)
}

// 处理搜索失焦
const handleSearchBlur = () => {
  handleSearchBlurUtil(componentState, searchQuery, closeSearch)
}

// 清除搜索
const clearSearch = () => {
  clearSearchUtil(searchQuery, searchSuggestions, searchResults, showSearchResults, showNoResults)
}

// 搜索历史
const searchByHistory = (item) => {
  searchByHistoryUtil(item, searchQuery, handleSearchInput)
}

// 选择建议
const selectSuggestion = (item) => {
  selectSuggestionUtil(item, searchQuery, handleSearchInput)
}

// 选择搜索结果
const selectSearchResult = (item) => {
  selectSearchResultUtil(item, focusOnNode, closeSearch)
}

// 高亮所有结果
const highlightAllResults = () => {
  highlightAllResultsUtil(chart, searchResults)
}


// 处理窗口大小变化
const handleResize = () => {
  chart?.resize()
}

// 习题弹窗相关方法
const closeExerciseModal = () => {
  showExerciseModal.value = false
}

const filteredExercises = computed(() => {
  let result = exercises.value
  
  // 难度筛选
  if (exerciseFilter.value !== 'all') {
    const filterMap = {
      'basic': '基础巩固',
      'advanced': '进阶提升',
      'exam': '真题冲刺',
      'competition': '竞赛拓展'
    }
    result = result.filter(e => e.difficulty === filterMap[exerciseFilter.value])
  }
  
  return result
})

const selectedCount = computed(() => {
  return exercises.value.filter(e => e.selected).length
})

const toggleExerciseSelection = (exercise) => {
  exercise.selected = !exercise.selected
}

const selectAllExercises = () => {
  const allSelected = filteredExercises.value.every(e => e.selected)
  filteredExercises.value.forEach(e => {
    e.selected = !allSelected
  })
}



const startExercise = () => {
  startExerciseUtil(exercises, closeExerciseModal, router)
}

const addToWrongBook = () => {
  addToWrongBookUtil(exercises)
}

</script>

<style scoped>
.knowledge-graph-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
  font-family: 'Microsoft YaHei', sans-serif;
  overflow-x: hidden;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  position: relative;
}

/* 知识图谱可视化区域 */
.graph-container {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 60px;
  width: 70%;
  height: calc(100vh - 60px);
}

/* 顶部工具栏 */
.graph-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #1a365d;
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

/* 书籍选择器 */
.book-selector {
  position: relative;
  margin-right: 16px;
}

.book-selector-container {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  background: #1a365d;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  width: 320px;
  z-index: 100;
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.book-selector-container::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 40px;
  width: 12px;
  height: 12px;
  background: #1a365d;
  transform: rotate(45deg);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.book-selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;
  border-radius: 8px 8px 0 0;
}

.book-selector-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: white;
}

.book-selector-close {
  background: transparent;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.book-selector-close:hover {
  color: white;
}

.book-selector-content {
  padding: 16px 18px;
}

.book-section {
  margin-bottom: 20px;
}

.book-section h5 {
  margin: 0 0 10px 0;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  text-transform: none;
  letter-spacing: 0;
}

.bookshelf-list {
  max-height: 160px;
  overflow-y: auto;
}

/* 自定义滚动条 */
.bookshelf-list::-webkit-scrollbar {
  width: 6px;
}

.bookshelf-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.bookshelf-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  transition: background 0.2s ease;
}

.bookshelf-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.bookshelf-item {
  padding: 10px 14px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.bookshelf-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.book-title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: white;
  margin-bottom: 2px;
}

.book-author {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.book-search {
  display: flex;
  margin-bottom: 12px;
}

.book-search input {
  flex: 1;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px 0 0 6px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  color: white;
}

.book-search input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.book-search input:focus {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
}

.search-btn {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-left: none;
  border-radius: 0 6px 6px 0;
  padding: 0 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.search-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.search-results {
  max-height: 160px;
  overflow-y: auto;
}

.search-book-item {
  padding: 10px 14px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.search-book-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.toolbar-buttons {
  display: flex;
  gap: 12px;
}

.home-btn {
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.home-btn:hover {
  background: linear-gradient(90deg, #2563eb 0%, #3b82f6 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  color: white;
}

.toolbar-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.toolbar-btn.active {
  background: #4F94FF;
  color: white;
  border-color: #4F94FF;
  box-shadow: 0 2px 4px rgba(79, 148, 255, 0.3);
}

.toolbar-icon {
  font-size: 16px;
}

.toolbar-text {
  font-size: 14px;
  font-weight: 500;
}

/* 搜索容器 */
.search-container {
  position: absolute;
  top: 80px;
  right: 20px;
  transform: scale(0.95);
  width: 400px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  overflow: hidden;
  opacity: 0;
  animation: searchPopup 0.25s ease-out forwards;
}

@keyframes searchPopup {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.search-input-wrapper {
  flex: 1;
  position: relative;
}

.search-input-wrapper input {
  width: 100%;
  padding: 8px 32px 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}

.search-input-wrapper input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  font-size: 14px;
}

.search-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-size: 18px;
  padding: 4px;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
}

.search-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.search-section h4 {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin: 0 0 8px 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.history-list,
.suggestion-list,
.result-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item,
.suggestion-item,
.result-item {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.history-item:hover,
.suggestion-item:hover,
.result-item:hover {
  background-color: #f3f4f6;
}

.suggestion-item,
.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.suggestion-name,
.result-name {
  font-weight: 500;
  color: #111827;
}

.suggestion-module,
.result-info {
  font-size: 12px;
  color: #6b7280;
}

.result-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.action-btn {
  padding: 6px 12px;
  background-color: #f3f4f6;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background-color: #e5e7eb;
}

.no-results {
  text-align: center;
}

.no-results h4 {
  color: #ef4444;
  margin-bottom: 8px;
}

.no-results p {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
}

.no-results-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.related-suggestions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.related-suggestions h5 {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin: 0 0 8px 0;
}

.related-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.related-item {
  padding: 4px 8px;
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.related-item:hover {
  background-color: #dbeafe;
}

/* 图谱展示区 */
.graph-canvas {
  flex: 1;
  width: 100%;
  height: 100%;
  background-color: #e3f2fd;
  cursor: grab;
}

.graph-canvas:active {
  cursor: grabbing;
}

/* 右侧侧边栏 */
.sidebar {
  width: 30%;
  min-width: 300px;
  max-width: 400px;
  background-color: white;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 24px;
  gap: 24px;
  min-height: 100vh;
  margin-left: 70%;
  position: relative;
  z-index: 5;
}

/* 错误提示样式 */
.error {
  color: #ff4d4f;
}

.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  background: #fff7f7;
  border: 1px solid #ffccc7;
  border-radius: 8px;
  margin-top: 40px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-text {
  color: #ff4d4f;
  line-height: 1.6;
}

.error-text p {
  margin: 8px 0;
}

/* 节点标题 */
.node-header {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.node-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.node-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 知识密度条 */
.knowledge-density {
  background-color: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.density-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.density-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.density-value {
  font-size: 14px;
  font-weight: 600;
  color: #059669;
}

.density-bar {
  height: 8px;
  background-color: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.density-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.density-desc {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

/* 高频知识点 */
.original-text {
  background-color: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 12px 0;
}

.text-preview {
  background-color: white;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
}

.knowledge-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.knowledge-list li {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.knowledge-list li:last-child {
  border-bottom: none;
}

.knowledge-list li::before {
  content: '•';
  color: #3b82f6;
  font-weight: bold;
  margin-right: 8px;
}

/* 学习行动区 */
.learning-actions {
  background-color: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.action-btn:hover {
  background-color: #f3f4f6;
  border-color: #d1d5db;
}

.action-icon {
  font-size: 20px;
}

.action-text {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

/* 关联知识点 */
.related-nodes {
  background-color: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.related-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.related-tag {
  padding: 6px 12px;
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 20px;
  font-size: 13px;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.related-tag:hover {
  background-color: #dbeafe;
  border-color: #93c5fd;
}

/* 习题弹窗样式 */
.exercise-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.exercise-modal {
  width: 900px;
  max-height: 85vh;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.exercise-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 24px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 10px;
}

.header-btn {
  padding: 6px 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.header-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.difficulty-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.type-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 8px 20px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.tab-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-info {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: #374151;
  background: white;
  cursor: pointer;
}

.filter-right {
  display: flex;
  gap: 10px;
}

.toolbar-btn {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.toolbar-btn.primary {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.toolbar-btn.primary:hover {
  background: #2563eb;
}

.exercise-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.exercise-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
}

.exercise-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.exercise-card.selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.exercise-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
  margin-right: 10px;
}

.tag-container {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  flex-direction: column;
}

.difficulty-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}

.type-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  background: #e2e8f0;
  color: #475569;
  text-align: center;
  border: 1px solid #cbd5e1;
}

.difficulty-tag.基础巩固 {
  background: #dcfce7;
  color: #166534;
}

.difficulty-tag.进阶提升 {
  background: #fef3c7;
  color: #92400e;
}

.difficulty-tag.真题冲刺 {
  background: #fee2e2;
  color: #991b1b;
}

.difficulty-tag.竞赛拓展 {
  background: #ede9fe;
  color: #5b21b6;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #f3f3f3;
  border-top: 5px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: #1e40af;
  font-weight: 500;
}

.card-body {
  margin-bottom: 12px;
}

.exercise-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 12px 0;
}

/* 推荐理由 */
.recommendation-reason {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-left: 3px solid #3b82f6;
  border-radius: 4px;
  font-size: 12px;
}

.reason-label {
  font-weight: 600;
  color: #1e40af;
  margin-right: 4px;
}

.reason-text {
  color: #3b82f6;
}

/* 知识点标签 */
.knowledge-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.knowledge-tag {
  padding: 4px 10px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
  transition: all 0.2s ease;
}

.knowledge-tag:hover {
  background: #e2e8f0;
  border-color: #cbd5e1;
  color: #475569;
}

.card-footer {
  display: flex;
  gap: 8px;
}

.card-btn {
  flex: 1;
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #64748b;
  transition: all 0.2s ease;
}

.card-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.footer-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #64748b;
}

.footer-stats .divider {
  color: #cbd5e1;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.footer-btn {
  padding: 10px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.start-btn {
  background: #3b82f6;
  color: white;
  border: none;
}

.start-btn:hover {
  background: #2563eb;
}

.ai-btn {
  background: white;
  color: #3b82f6;
  border: 1px solid #3b82f6;
}

.ai-btn:hover {
  background: #eff6ff;
}

/* 学习进度弹窗 */
.progress-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1500;
}

.progress-modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.progress-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
  border-radius: 12px 12px 0 0;
}

.progress-modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.progress-modal-body {
  padding: 24px;
}

.mastery-section {
  margin-bottom: 32px;
}

.mastery-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.mastery-bar {
  height: 12px;
  background: #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.mastery-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 6px;
  transition: width 0.3s ease;
}

.mastery-text {
  font-size: 14px;
  font-weight: 600;
  color: #10b981;
  text-align: right;
}

.history-section {
  margin-bottom: 32px;
}

.history-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.history-date {
  font-size: 14px;
  color: #64748b;
}

.history-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  font-weight: 500;
}

.history-mastery {
  color: #10b981;
}

.recommendation-section {
  margin-bottom: 24px;
}

.recommendation-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.recommendation-item {
  padding: 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #1e40af;
}

.progress-modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #f8fafc;
  border-radius: 0 0 12px 12px;
}

.progress-modal-footer .footer-btn {
  padding: 10px 20px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  transition: all 0.2s ease;
}

.progress-modal-footer .footer-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.progress-modal-footer .footer-btn.primary {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.progress-modal-footer .footer-btn.primary:hover {
  background: #2563eb;
  border-color: #2563eb;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: white;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 提示消息 */
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #3b82f6;
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  z-index: 2000;
  animation: slideInRight 0.3s ease-out;
}

.toast-content {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

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
</style>