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
                      <div 
                        v-for="(book, index) in bookshelfBooks" 
                        :key="book.id || `book_${index}`"
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
            <button class="toolbar-btn" :class="{ active: viewMode !== 'standard' }" title="简化视图" @click="toggleViewMode">
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
        <div class="graph-canvas" ref="chartContainer">
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
          <h2 class="node-title">{{ selectedNode || '高等数学' }}</h2>
          <div class="node-subtitle">{{ nodeSubtitle }}</div>
        </div>

        <!-- 知识密度条 -->
        <div class="knowledge-density">
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
        <div class="related-nodes">
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
        <div class="original-text">
          <h3 class="section-title">高频知识点</h3>
          <div class="text-preview">
            <ul class="knowledge-list">
              <li v-for="(knowledge, index) in highFrequencyKnowledge" :key="`knowledge_${index}_${knowledge}`">{{ knowledge }}</li>
            </ul>
          </div>
        </div>

        <!-- 学习行动区 -->
        <div class="learning-actions">
          <h3 class="section-title">学习工具</h3>
          <div class="action-buttons">

            <button class="action-btn" @click="openExerciseModal">
              <span class="action-icon">📚</span>
              <span class="action-text">推荐关联习题</span>
            </button>
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
import { ref, computed, onMounted, onUnmounted, watch, defineOptions, nextTick } from 'vue'

// 为组件指定名称
defineOptions({
  name: 'KnowledgeGraph'
})
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import api, { apiService } from '@/api/api.js'
import { ElMessage } from 'element-plus'

// 添加中文字体支持
import 'jspdf/dist/jspdf.umd.min.js'

const router = useRouter()
const route = useRoute()

// 图表实例
const chartContainer = ref(null)
let chart = null

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

// 安全执行函数
const safeExecute = (fn, ...args) => {
  if (!componentState.isMounted.value) {
    console.warn('Component unmounted, skipping execution')
    return null
  }
  return fn(...args)
}

// 安全更新响应式数据
const safeUpdate = (refValue, newValue) => {
  // 双重检查：1.组件挂载状态 2.DOM元素实际存在
  if (!componentState.isMounted.value) {
    return // 静默返回，不执行任何操作
  }

  // 额外检查：如果refValue是图表容器相关，检查DOM是否存在
  if (refValue === chartContainer && chartContainer.value && !document.body.contains(chartContainer.value)) {
    return
  }

  // 只有通过所有检查才更新
  refValue.value = newValue
}

// 安全异步操作
const safeAsync = async (promiseFn) => {
  if (!componentState.isMounted.value) {
    throw new Error('Component unmounted')
  }

  const promise = promiseFn()
  componentState.pendingPromises.add(promise)

  try {
    const result = await promise
    if (!componentState.isMounted.value) {
      throw new Error('Component unmounted during async operation')
    }
    return result
  } finally {
    componentState.pendingPromises.delete(promise)
  }
}

// 书籍选择相关
const showBookSelector = ref(false)
const bookSearchQuery = ref('')
const bookSearchResults = ref([])
const currentBook = ref(null)

// 习题弹窗相关
const showExerciseModal = ref(false)
const exerciseFilter = ref('all')
const exerciseTypeFilter = ref('all')
const selectedExercises = ref([])
const exercises = ref([])
const recommendedExercises = ref([])
const exerciseDifficulty = ref('balanced')
const exerciseFocus = ref('weakness')
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
const recommendParams = ref({
  difficulty: 'balanced', // balanced, easy, hard
  focus: 'weakness', // weakness, strength, all
  quantity: 5
})

// 习题与知识点映射
const exerciseKnowledgeMap = ref({
  // 计算机网络相关习题
  101: ['网络协议', 'TCP/IP'],
  102: ['网络协议', 'HTTP/HTTPS'],
  103: ['网络拓扑', '路由算法'],
  104: ['网络安全', '防火墙'],
  105: ['网络拓扑', '网络结构'],
  
  // 操作系统原理相关习题
  201: ['进程管理', '进程调度'],
  202: ['内存管理', '分页存储'],
  203: ['文件系统', '文件结构'],
  204: ['设备管理', '设备驱动'],
  205: ['死锁', '死锁预防'],
  
  // 数据库系统概论相关习题
  301: ['数据库模型', '关系模型'],
  302: ['SQL语言', '查询语句'],
  303: ['事务管理', 'ACID特性'],
  304: ['数据库设计', '范式理论'],
  305: ['事务管理', '并发控制'],
  
  // 数据结构与算法相关习题
  401: ['线性结构', '链表'],
  402: ['树形结构', '二叉树'],
  403: ['排序算法', '快速排序', '归并排序'],
  404: ['图结构', '最短路径'],
  405: ['动态规划', '算法'],
  
  // 高等数学相关习题
  501: ['微积分', '导数'],
  502: ['微积分', '定积分'],
  503: ['线性代数', '矩阵'],
  504: ['常微分方程', '一阶微分方程'],
  505: ['概率论', '概率分布'],
  
  // 体育与健康相关习题
  601: ['运动生理学', '心肺功能'],
  602: ['运动技能', '球类运动'],
  603: ['健康知识', '营养学'],
  604: ['体育赛事', '奥运会'],
  605: ['运动损伤', '损伤预防'],
  
  // 英语相关习题
  701: ['词汇', '核心词汇'],
  702: ['语法', '时态', '语态'],
  703: ['阅读', '精读'],
  704: ['写作', '议论文'],
  705: ['口语', '话题讨论']
})

// 获取习题列表
const fetchExercises = async (nodeName) => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping fetchExercises')
    return
  }
  try {
    const response = await api.get('/exercise/exercises', {
      knowledgePoint: nodeName, 
      difficulty: exerciseFilter.value
    })
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    if (response && response.data && componentState.isMounted.value) {
      exercises.value = response.data.map(exercise => ({
        ...exercise,
        selected: false
      }))
    } else {
      // 模拟计算机网络相关习题
      if (componentState.isMounted.value) {
        exercises.value = generateMockExercises(nodeName)
      }
    }
  } catch (error) {
    console.error('获取习题失败:', error)
    // 生成模拟习题
    if (componentState.isMounted.value) {
      exercises.value = generateMockExercises(nodeName)
    }
  }
}

// 生成模拟习题
const generateMockExercises = (nodeName) => {
  const exercises = []
  
  // 知识点映射关系
  const knowledgePointMappings = {
    '计算机网络': {
      '网络协议': ['TCP/IP', 'HTTP/HTTPS'],
      '网络拓扑': ['路由算法', '网络结构'],
      '网络安全': ['防火墙', '加密技术'],
      '网络层': ['IP协议', '路由'],
      '传输层': ['TCP', 'UDP']
    },
    '操作系统原理': {
      '进程管理': ['进程调度', '进程同步'],
      '内存管理': ['分页存储', '分段存储'],
      '文件系统': ['文件结构', '文件操作'],
      '设备管理': ['设备驱动', 'I/O操作'],
      '死锁': ['死锁预防', '死锁检测']
    },
    '数据库系统概论': {
      '关系模型': ['SQL', '关系代数'],
      '数据库设计': ['ER图', '范式'],
      '事务管理': ['ACID特性', '并发控制'],
      '查询优化': ['索引', '执行计划'],
      '数据库安全': ['权限管理', '数据加密']
    },
    '数据结构': {
      '线性表': ['链表', '栈', '队列'],
      '树': ['二叉树', '平衡树', '堆'],
      '图': ['图遍历', '最短路径', '最小生成树'],
      '排序': ['快速排序', '归并排序', '堆排序'],
      '查找': ['二分查找', '哈希表', '二叉搜索树']
    },
    '算法': {
      '贪心算法': ['最优子结构', '贪心选择性质'],
      '动态规划': ['状态转移', '最优子结构'],
      '分治算法': ['递归', '子问题'],
      '图算法': ['BFS', 'DFS', 'Dijkstra'],
      '字符串算法': ['KMP', '正则表达式', '编辑距离']
    }
  }
  
  // 计算机网络相关习题
  if (nodeName.includes('计算机网络') || nodeName.includes('网络')) {
    exercises.push(
      {
        id: 101,
        title: 'TCP/IP协议栈的层次结构',
        description: '请描述TCP/IP协议栈的层次结构，并说明各层的主要功能。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['网络协议', 'TCP/IP', '网络层次']
      },
      {
        id: 102,
        title: 'HTTP与HTTPS的区别',
        description: '请详细说明HTTP与HTTPS的区别，以及HTTPS的工作原理。',
        difficulty: '进阶提升',
        type: '比较题',
        knowledgePoints: ['网络协议', 'HTTP/HTTPS', '网络安全']
      },
      {
        id: 103,
        title: '路由算法的选择',
        description: '请比较不同路由算法的优缺点，并说明在不同网络环境下如何选择合适的路由算法。',
        difficulty: '进阶提升',
        type: '分析题',
        knowledgePoints: ['网络拓扑', '路由算法', '网络层']
      },
      {
        id: 104,
        title: '网络安全防护措施',
        description: '请列举常见的网络安全威胁，并说明相应的防护措施。',
        difficulty: '真题冲刺',
        type: '应用题',
        knowledgePoints: ['网络安全', '防火墙', '加密技术']
      },
      {
        id: 105,
        title: '网络拓扑设计',
        description: '请设计一个企业网络拓扑结构，并说明设计理由。',
        difficulty: '竞赛拓展',
        type: '设计题',
        knowledgePoints: ['网络拓扑', '网络结构', '网络规划']
      }
    )
  } else if (nodeName.includes('操作系统原理') || nodeName.includes('操作系统')) {
    // 操作系统原理相关习题
    exercises.push(
      {
        id: 201,
        title: '进程管理与调度',
        description: '请解释进程的概念和进程调度的基本原理。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['进程管理', '进程调度', 'CPU调度']
      },
      {
        id: 202,
        title: '内存管理',
        description: '请说明分页存储管理和分段存储管理的区别。',
        difficulty: '进阶提升',
        type: '比较题',
        knowledgePoints: ['内存管理', '分页存储', '分段存储']
      },
      {
        id: 203,
        title: '文件系统',
        description: '请描述文件系统的基本功能和常见的文件系统类型。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['文件系统', '文件结构', '存储管理']
      },
      {
        id: 204,
        title: '设备管理',
        description: '请解释设备驱动程序的作用和工作原理。',
        difficulty: '真题冲刺',
        type: '应用题',
        knowledgePoints: ['设备管理', '设备驱动', 'I/O操作']
      },
      {
        id: 205,
        title: '死锁',
        description: '请分析死锁产生的必要条件和预防死锁的方法。',
        difficulty: '竞赛拓展',
        type: '分析题',
        knowledgePoints: ['死锁', '死锁预防', '进程同步']
      }
    )
  } else if (nodeName.includes('数据库系统概论') || nodeName.includes('数据库')) {
    // 数据库系统概论相关习题
    exercises.push(
      {
        id: 301,
        title: '数据库模型',
        description: '请解释关系模型的基本概念和特点。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['数据库模型', '关系模型', 'SQL']
      },
      {
        id: 302,
        title: 'SQL查询',
        description: '请编写SQL语句查询员工表中工资大于5000的员工信息。',
        difficulty: '进阶提升',
        type: '编程题',
        knowledgePoints: ['SQL语言', '查询语句', '关系模型']
      },
      {
        id: 303,
        title: '事务管理',
        description: '请解释事务的ACID特性及其重要性。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['事务管理', 'ACID特性', '并发控制']
      },
      {
        id: 304,
        title: '数据库设计',
        description: '请设计一个图书馆管理系统的数据库 schema。',
        difficulty: '真题冲刺',
        type: '设计题',
        knowledgePoints: ['数据库设计', '范式理论', 'ER图']
      },
      {
        id: 305,
        title: '并发控制',
        description: '请分析并发操作可能导致的数据不一致问题及解决方法。',
        difficulty: '竞赛拓展',
        type: '分析题',
        knowledgePoints: ['事务管理', '并发控制', '锁机制']
      }
    )
  } else if (nodeName.includes('数据结构与算法') || nodeName.includes('数据结构') || nodeName.includes('算法')) {
    // 数据结构与算法相关习题
    exercises.push(
      {
        id: 401,
        title: '线性表',
        description: '请实现一个单链表的反转操作。',
        difficulty: '基础巩固',
        type: '编程题',
        knowledgePoints: ['线性结构', '链表', '数据结构']
      },
      {
        id: 402,
        title: '树结构',
        description: '请解释二叉树的遍历方法及其时间复杂度。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['树形结构', '二叉树', '图遍历']
      },
      {
        id: 403,
        title: '排序算法',
        description: '请比较快速排序和归并排序的优缺点。',
        difficulty: '进阶提升',
        type: '比较题',
        knowledgePoints: ['排序算法', '快速排序', '归并排序', '时间复杂度']
      },
      {
        id: 404,
        title: '图算法',
        description: '请实现Dijkstra算法求解最短路径问题。',
        difficulty: '真题冲刺',
        type: '编程题',
        knowledgePoints: ['图结构', '最短路径', '贪心算法']
      },
      {
        id: 405,
        title: '动态规划',
        description: '请使用动态规划解决最长公共子序列问题。',
        difficulty: '竞赛拓展',
        type: '编程题',
        knowledgePoints: ['动态规划', '算法', '状态转移']
      }
    )
  } else if (nodeName.includes('高等数学') || nodeName.includes('数学')) {
    // 高等数学相关习题
    exercises.push(
      {
        id: 501,
        title: '导数计算',
        description: '请计算函数f(x) = x^3 + 2x^2 - 3x + 1的导数。',
        difficulty: '基础巩固',
        type: '计算题',
        knowledgePoints: ['微积分', '导数', '函数']
      },
      {
        id: 502,
        title: '定积分',
        description: '请计算从0到1的x^2的定积分。',
        difficulty: '进阶提升',
        type: '计算题',
        knowledgePoints: ['微积分', '定积分', '反导数']
      },
      {
        id: 503,
        title: '矩阵运算',
        description: '请计算两个3x3矩阵的乘积。',
        difficulty: '进阶提升',
        type: '计算题',
        knowledgePoints: ['线性代数', '矩阵', '矩阵运算']
      },
      {
        id: 504,
        title: '微分方程',
        description: '请求解一阶线性微分方程y + y = e^x。',
        difficulty: '真题冲刺',
        type: '计算题',
        knowledgePoints: ['常微分方程', '一阶微分方程', '积分因子']
      },
      {
        id: 505,
        title: '概率论',
        description: '请计算正态分布的概率密度函数在特定区间的积分。',
        difficulty: '竞赛拓展',
        type: '计算题',
        knowledgePoints: ['概率论', '概率分布', '正态分布']
      }
    )
  } else if (nodeName.includes('体育与健康')) {
    // 体育与健康相关习题
    exercises.push(
      {
        id: 601,
        title: '运动生理学',
        description: '请解释运动对心血管系统的影响。',
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: ['运动生理学', '心肺功能', '心血管系统']
      },
      {
        id: 602,
        title: '运动技能',
        description: '请描述篮球投篮的基本动作要领。',
        difficulty: '进阶提升',
        type: '应用题',
        knowledgePoints: ['运动技能', '球类运动', '篮球']
      },
      {
        id: 603,
        title: '健康知识',
        description: '请说明合理饮食对健康的重要性。',
        difficulty: '进阶提升',
        type: '概念题',
        knowledgePoints: ['健康知识', '营养学', '合理饮食']
      },
      {
        id: 604,
        title: '体育赛事',
        description: '请分析奥运会对城市发展的影响。',
        difficulty: '真题冲刺',
        type: '分析题',
        knowledgePoints: ['体育赛事', '奥运会', '城市发展']
      },
      {
        id: 605,
        title: '运动损伤',
        description: '请说明常见运动损伤的预防和处理方法。',
        difficulty: '竞赛拓展',
        type: '应用题',
        knowledgePoints: ['运动损伤', '损伤预防', '急救处理']
      }
    )
  } else if (nodeName.includes('英语') || nodeName.includes('新视野大学英语')) {
    // 英语相关习题
    exercises.push(
      {
        id: 701,
        title: '词汇',
        description: '请解释下列单词的含义并举例说明：ambitious, significant, implement。',
        difficulty: '基础巩固',
        type: '词汇题',
        knowledgePoints: ['词汇', '核心词汇', '英语学习']
      },
      {
        id: 702,
        title: '语法',
        description: '请分析并纠正下列句子的语法错误：He have been working here for five years.。',
        difficulty: '进阶提升',
        type: '语法题',
        knowledgePoints: ['语法', '时态', '语态', '英语学习']
      },
      {
        id: 703,
        title: '阅读理解',
        description: '请阅读以下短文并回答问题：...（短文内容）。',
        difficulty: '进阶提升',
        type: '阅读题',
        knowledgePoints: ['阅读', '精读', '英语学习']
      },
      {
        id: 704,
        title: '写作',
        description: '请写一篇关于环境保护的短文，不少于150词。',
        difficulty: '真题冲刺',
        type: '写作题',
        knowledgePoints: ['写作', '议论文', '英语学习']
      },
      {
        id: 705,
        title: '口语',
        description: '请准备一段关于大学生活的口语表达，时间2分钟。',
        difficulty: '竞赛拓展',
        type: '口语题',
        knowledgePoints: ['口语', '话题讨论', '英语学习']
      }
    )
  } else {
    // 其他知识点的默认习题
    exercises.push(
      {
        id: 1,
        title: '知识点理解',
        description: `请解释${nodeName}的基本概念和应用场景。`,
        difficulty: '基础巩固',
        type: '概念题',
        knowledgePoints: [nodeName]
      },
      {
        id: 2,
        title: '知识点应用',
        description: `请举例说明${nodeName}在实际问题中的应用。`,
        difficulty: '进阶提升',
        type: '应用题',
        knowledgePoints: [nodeName]
      },
      {
        id: 3,
        title: '知识点综合',
        description: `请结合其他相关知识点，分析${nodeName}的重要性。`,
        difficulty: '真题冲刺',
        type: '分析题',
        knowledgePoints: [nodeName]
      },
      {
        id: 4,
        title: '知识点拓展',
        description: `请探讨${nodeName}的最新发展趋势和未来应用。`,
        difficulty: '竞赛拓展',
        type: '拓展题',
        knowledgePoints: [nodeName]
      }
    )
  }
  
  // 根据难度和类型筛选
  let filteredExercises = exercises
  
  if (exerciseFilter.value !== 'all') {
    const filterMap = {
      'basic': '基础巩固',
      'advanced': '进阶提升',
      'exam': '真题冲刺',
      'competition': '竞赛拓展'
    }
    filteredExercises = filteredExercises.filter(e => e.difficulty === filterMap[exerciseFilter.value])
  }
  
  if (exerciseTypeFilter.value !== 'all') {
    filteredExercises = filteredExercises.filter(e => e.type === exerciseTypeFilter.value)
  }
  
  return filteredExercises
}

// 计算知识点掌握程度
const calculateKnowledgeMastery = (knowledgePoint, bookId) => {
  const bookProgress = userLearningProgress.value.books[bookId] || {}
  const knowledgeProgress = bookProgress[knowledgePoint] || {
    correct: 0,
    total: 0,
    lastPracticed: null
  }
  
  if (knowledgeProgress.total === 0) {
    return 0
  }
  
  // 计算掌握程度 = 正确率 * 时间衰减因子
  const correctness = knowledgeProgress.correct / knowledgeProgress.total
  const timeFactor = knowledgeProgress.lastPracticed ? 
    Math.max(0.5, 1 - (Date.now() - new Date(knowledgeProgress.lastPracticed).getTime()) / (7 * 24 * 60 * 60 * 1000)) : 
    1
  
  return correctness * timeFactor
}

// 获取相关知识点
const getRelatedKnowledgePoints = (knowledgePoint) => {
  const relatedPoints = new Set()
  
  // 直接相关的知识点
  if (nodeRelations[knowledgePoint]) {
    nodeRelations[knowledgePoint].forEach(point => {
      relatedPoints.add(point)
    })
  }
  
  // 反向相关的知识点
  for (const [key, value] of Object.entries(nodeRelations)) {
    if (value.includes(knowledgePoint)) {
      relatedPoints.add(key)
    }
  }
  
  return Array.from(relatedPoints)
}

// 根据难度筛选习题
const filterExercisesByDifficulty = (exercises, difficultyLevel) => {
  const difficultyMap = {
    'easy': ['基础巩固'],
    'balanced': ['基础巩固', '进阶提升'],
    'hard': ['真题冲刺', '竞赛拓展']
  }
  
  return exercises.filter(exercise => 
    difficultyMap[difficultyLevel].includes(exercise.difficulty)
  )
}

// 根据相关性排序习题
const sortExercisesByRelevance = (exercises, targetKnowledgePoint, bookId) => {
  return exercises.sort((a, b) => {
    // 计算习题与目标知识点的相关性
    const relevanceA = calculateExerciseRelevance(a, targetKnowledgePoint, bookId)
    const relevanceB = calculateExerciseRelevance(b, targetKnowledgePoint, bookId)
    
    return relevanceB - relevanceA
  })
}

// 计算习题与知识点的相关性
const calculateExerciseRelevance = (exercise, targetKnowledgePoint, bookId) => {
  let relevance = 0
  
  // 直接匹配知识点
  if (exercise.knowledgePoints.includes(targetKnowledgePoint)) {
    relevance += 1.0
  }
  
  // 相关知识点匹配
  const relatedPoints = getRelatedKnowledgePoints(targetKnowledgePoint)
  exercise.knowledgePoints.forEach(point => {
    if (relatedPoints.includes(point)) {
      relevance += 0.5
    }
  })
  
  // 考虑知识点掌握程度（优先推荐掌握度低的）
  exercise.knowledgePoints.forEach(point => {
    const mastery = calculateKnowledgeMastery(point, bookId)
    relevance += (1 - mastery) * 0.3
  })
  
  return relevance
}

// 计算习题难度
const calculateExerciseDifficulty = (exercise, bookId) => {
  // 基础难度映射
  const baseDifficultyMap = {
    '基础巩固': 1,
    '进阶提升': 2,
    '真题冲刺': 3,
    '竞赛拓展': 4
  }
  
  let baseDifficulty = baseDifficultyMap[exercise.difficulty] || 2
  
  // 基于用户答题情况调整难度
  let adjustedDifficulty = baseDifficulty
  
  // 计算知识点平均掌握程度
  let totalMastery = 0
  let pointCount = 0
  
  exercise.knowledgePoints.forEach(point => {
    const mastery = calculateKnowledgeMastery(point, bookId)
    totalMastery += mastery
    pointCount++
  })
  
  if (pointCount > 0) {
    const avgMastery = totalMastery / pointCount
    
    // 根据掌握程度调整难度
    if (avgMastery < 0.3) {
      // 掌握程度低，降低难度
      adjustedDifficulty = Math.max(1, baseDifficulty - 1)
    } else if (avgMastery > 0.8) {
      // 掌握程度高，提高难度
      adjustedDifficulty = Math.min(4, baseDifficulty + 1)
    }
  }
  
  // 映射回难度等级
  const difficultyLevels = ['基础巩固', '进阶提升', '真题冲刺', '竞赛拓展']
  return {
    level: adjustedDifficulty,
    label: difficultyLevels[adjustedDifficulty - 1],
    baseLevel: baseDifficulty,
    baseLabel: exercise.difficulty
  }
}

// 获取难度颜色
const getDifficultyColor = (difficulty) => {
  const colorMap = {
    '基础巩固': '#10b981',
    '进阶提升': '#f59e0b',
    '真题冲刺': '#ef4444',
    '竞赛拓展': '#8b5cf6'
  }
  return colorMap[difficulty] || '#6b7280'
}

// 动态调整习题难度
const adjustExerciseDifficulty = (exerciseId, isCorrect, bookId) => {
  // 查找习题
  const exercise = exercises.value.find(e => e.id === exerciseId)
  if (!exercise) return
  
  // 更新用户学习进度
  updateLearningProgress(exerciseId, exercise.knowledgePoints, isCorrect, bookId)
  
  // 重新计算难度
  const difficultyInfo = calculateExerciseDifficulty(exercise, bookId)
  
  // 更新习题难度
  exercise.adjustedDifficulty = difficultyInfo.label
  exercise.difficultyLevel = difficultyInfo.level
  
  return difficultyInfo
}

// 推荐习题
const recommendExercises = (nodeName, bookId, params = {}) => {
  const { difficulty = 'balanced', focus = 'weakness', quantity = 5 } = params
  
  // 获取所有相关习题
  let allExercises = generateMockExercises(nodeName)
  
  // 根据难度筛选
  allExercises = filterExercisesByDifficulty(allExercises, difficulty)
  
  // 根据相关性排序
  allExercises = sortExercisesByRelevance(allExercises, nodeName, bookId)
  
  // 根据聚焦策略调整排序
  if (focus === 'weakness') {
    allExercises.sort((a, b) => {
      const weakScoreA = a.knowledgePoints.reduce((sum, point) => {
        return sum + (1 - calculateKnowledgeMastery(point, bookId))
      }, 0)
      const weakScoreB = b.knowledgePoints.reduce((sum, point) => {
        return sum + (1 - calculateKnowledgeMastery(point, bookId))
      }, 0)
      return weakScoreB - weakScoreA
    })
  } else if (focus === 'strength') {
    allExercises.sort((a, b) => {
      const strongScoreA = a.knowledgePoints.reduce((sum, point) => {
        return sum + calculateKnowledgeMastery(point, bookId)
      }, 0)
      const strongScoreB = b.knowledgePoints.reduce((sum, point) => {
        return sum + calculateKnowledgeMastery(point, bookId)
      }, 0)
      return strongScoreB - strongScoreA
    })
  }
  
  // 为每个推荐习题计算调整后的难度和推荐理由
  allExercises.forEach(exercise => {
    const difficultyInfo = calculateExerciseDifficulty(exercise, bookId)
    exercise.adjustedDifficulty = difficultyInfo.label
    exercise.difficultyLevel = difficultyInfo.level
    
    // 生成推荐理由
    exercise.recommendationReason = generateRecommendationReason(exercise, nodeName, bookId)
  })
  
  // 限制数量
  return allExercises.slice(0, quantity)
}

// 生成推荐理由
const generateRecommendationReason = (exercise, targetKnowledgePoint, bookId) => {
  // 计算知识点掌握程度
  const mastery = exercise.knowledgePoints.reduce((sum, point) => {
    return sum + calculateKnowledgeMastery(point, bookId)
  }, 0) / exercise.knowledgePoints.length
  
  // 根据掌握程度生成推荐理由
  if (mastery < 0.3) {
    return '该知识点掌握程度较低，建议加强练习'
  } else if (mastery < 0.6) {
    return '该知识点需要进一步巩固，建议重点练习'
  } else if (mastery < 0.8) {
    return '该知识点掌握较好，建议提升练习难度'
  } else {
    return '该知识点掌握优秀，建议挑战更高难度'
  }
}

// 监听用户答题情况，实时更新推荐
const handleExerciseCompleted = (exercise, isCorrect) => {
  const bookId = currentBook.value?.id || 'default'
  
  // 调整习题难度
  adjustExerciseDifficulty(exercise.id, isCorrect, bookId)
  
  // 重新生成推荐
  if (selectedNode.value) {
    const updatedRecommendations = recommendExercises(selectedNode.value, bookId, {
      difficulty: exerciseDifficulty.value,
      focus: exerciseFocus.value,
      quantity: 10
    })
    
    // 更新推荐结果
    recommendedExercises.value = updatedRecommendations
    
    // 显示反馈
    showToastMessage(isCorrect ? '回答正确！推荐结果已更新' : '回答错误，推荐结果已调整')
  }
}

// 实时更新推荐策略
const updateRecommendationStrategy = () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping updateRecommendationStrategy')
    return
  }
  
  console.log('updateRecommendationStrategy called, exerciseDifficulty:', exerciseDifficulty)
  if (!selectedNode.value) return
  
  const bookId = currentBook.value?.id || 'default'
  
  // 分析用户学习行为
  const learningBehavior = analyzeLearningBehavior(bookId)
  
  // 根据学习行为调整推荐参数
  let difficulty = 'balanced'
  let focus = 'weakness'
  
  if (learningBehavior.averageMastery > 0.8) {
    // 整体掌握程度高，提高难度
    difficulty = 'hard'
    focus = 'strength'
  } else if (learningBehavior.averageMastery < 0.4) {
    // 整体掌握程度低，降低难度
    difficulty = 'easy'
    focus = 'weakness'
  }
  
  // 更新推荐结果
  const updatedRecommendations = recommendExercises(selectedNode.value, bookId, {
    difficulty,
    focus,
    quantity: 10
  })
  
  recommendedExercises.value = updatedRecommendations
  
  // 更新筛选器
  exerciseDifficulty.value = difficulty
  exerciseFocus.value = focus
}

// 分析学习行为
const analyzeLearningBehavior = (bookId) => {
  const bookProgress = userLearningProgress.value.books[bookId] || {}
  
  let totalMastery = 0
  let pointCount = 0
  let totalCorrect = 0
  let totalAttempts = 0
  
  // 计算整体掌握程度
  Object.entries(bookProgress).forEach(([knowledgePoint, progress]) => {
    if (progress.total > 0) {
      const mastery = progress.correct / progress.total
      totalMastery += mastery
      pointCount++
      totalCorrect += progress.correct
      totalAttempts += progress.total
    }
  })
  
  const averageMastery = pointCount > 0 ? totalMastery / pointCount : 0
  const overallAccuracy = totalAttempts > 0 ? totalCorrect / totalAttempts : 0
  
  return {
    averageMastery,
    overallAccuracy,
    totalAttempts,
    pointCount,
    totalCorrect
  }
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
const updateLearningProgress = (exerciseId, knowledgePoints, isCorrect, bookId) => {
  if (!userLearningProgress.value.books[bookId]) {
    userLearningProgress.value.books[bookId] = {}
  }
  
  const bookProgress = userLearningProgress.value.books[bookId]
  
  knowledgePoints.forEach(point => {
    if (!bookProgress[point]) {
      bookProgress[point] = {
        correct: 0,
        total: 0,
        lastPracticed: null
      }
    }
    
    bookProgress[point].total += 1
    if (isCorrect) {
      bookProgress[point].correct += 1
    }
    bookProgress[point].lastPracticed = new Date().toISOString()
  })
  
  userLearningProgress.value.lastUpdated = new Date().toISOString()
  
  // 保存到本地存储
  localStorage.setItem('userLearningProgress', JSON.stringify(userLearningProgress.value))
}

// 加载学习进度
const loadLearningProgress = () => {
  const savedProgress = localStorage.getItem('userLearningProgress')
  if (savedProgress) {
    try {
      userLearningProgress.value = JSON.parse(savedProgress)
    } catch (error) {
      console.error('加载学习进度失败:', error)
    }
  }
}

// 初始化时加载学习进度
loadLearningProgress()

// 书架数据
const bookshelfBooks = ref([])

// 获取书架书籍
const fetchBookshelfBooks = async () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping fetchBookshelfBooks')
    return
  }
  try {
    const response = await api.get('/books')
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    if (componentState.isMounted.value) {
      bookshelfBooks.value = response.data
    }
  } catch (error) {
    console.error('获取书架书籍失败:', error)
  }
}

// 视图模式文本
const viewModeText = computed(() => {
  return viewMode.value === 'standard' ? '简化视图' : '标准视图'
})

// 为节点添加初始位置，避免重叠
const initNodePositions = () => {
  initNodePositionsForBook(currentGraphData)
}

// 节点关系映射
const nodeRelations = {
  // 高等数学
  '高等数学': ['微积分', '线性代数', '概率论', '常微分方程'],
  '微积分': ['导数', '积分', '极限', '定积分', '不定积分', '微积分基本定理'],
  '线性代数': ['矩阵', '行列式', '特征值', '特征向量', '线性方程组', '向量空间'],
  '概率论': ['随机变量', '概率分布', '期望', '方差', '大数定律', '中心极限定理'],
  '常微分方程': ['一阶微分方程', '二阶微分方程', '齐次方程', '非齐次方程', '微分方程应用'],
  '导数': ['极限', '微积分', '微分中值定理', '洛必达法则'],
  '积分': ['定积分', '不定积分', '微积分', '反常积分'],
  '极限': ['数列极限', '函数极限', '连续函数'],
  '矩阵': ['特征值', '特征向量', '线性代数', '矩阵运算', '矩阵的秩'],
  '行列式': ['线性代数', '行列式计算', '克莱姆法则'],
  '随机变量': ['期望', '方差', '概率论', '概率分布'],
  '概率分布': ['概率论', '离散型分布', '连续型分布'],
  '一阶微分方程': ['齐次方程', '非齐次方程', '常微分方程', '变量分离法'],
  '二阶微分方程': ['非齐次方程', '常微分方程', '线性微分方程'],
  
  // 计算机网络
  '计算机网络': ['网络协议', '网络拓扑', '网络安全', '网络管理', '网络层', '传输层'],
  '网络协议': ['TCP/IP', 'HTTP/HTTPS', 'DNS', 'FTP', 'SMTP'],
  '网络拓扑': ['路由算法', '网络结构', '拓扑设计'],
  '网络安全': ['防火墙', '加密技术', '安全协议', '入侵检测'],
  '网络管理': ['SNMP', '网络监控', '故障诊断', '性能优化'],
  '网络层': ['IP协议', '路由协议', '子网划分'],
  '传输层': ['TCP', 'UDP', '流量控制', '拥塞控制'],
  'TCP/IP': ['网络协议', '网络层', '传输层'],
  'HTTP/HTTPS': ['网络协议', '应用层'],
  '路由算法': ['网络拓扑', '最短路径算法', '路由选择'],
  '防火墙': ['网络安全', '访问控制', '数据包过滤'],
  
  // 体育与健康
  '体育与健康': ['体育理论', '运动技能', '健康知识', '体育赛事', '运动损伤', '体育锻炼'],
  '体育理论': ['运动生理学', '运动心理学', '运动解剖学', '运动生物化学'],
  '运动技能': ['球类运动', '田径运动', '体操', '武术'],
  '健康知识': ['营养学', '健康评估', '心理健康', '疾病预防'],
  '体育赛事': ['奥运会', '世锦赛', '亚运会', '全运会'],
  '运动损伤': ['损伤预防', '损伤处理', '康复训练', '运动防护'],
  '体育锻炼': ['锻炼原则', '锻炼方法', '锻炼计划', '运动负荷'],
  '运动生理学': ['心肺功能', '肌肉系统', '能量代谢', '运动疲劳'],
  '球类运动': ['篮球', '足球', '排球', '乒乓球'],
  
  // 大学物理
  '大学物理（上册）': ['力学', '热学', '电磁学', '光学', '振动与波动'],
  '大学物理（下册）': ['量子力学', '相对论', '固体物理', '近代物理', '原子核物理'],
  '力学': ['牛顿运动定律', '动量守恒', '能量守恒', '角动量守恒', '刚体力学'],
  '热学': ['热力学定律', '气体动理论', '热传导', '热膨胀'],
  '电磁学': ['电场', '磁场', '电磁感应', '电磁场', '电磁波'],
  '光学': ['几何光学', '波动光学', '光的干涉', '光的衍射', '光的偏振'],
  '量子力学': ['波粒二象性', '薛定谔方程', '量子态', '量子跃迁'],
  '相对论': ['狭义相对论', '广义相对论', '时空观', '质能关系'],
  
  // 操作系统原理
  '操作系统原理': ['进程管理', '内存管理', '文件系统', '设备管理', '死锁', '虚拟内存'],
  '进程管理': ['进程调度', '进程同步', '进程通信', '线程管理'],
  '内存管理': ['分页存储', '分段存储', '虚拟内存', '内存分配', '内存回收'],
  '文件系统': ['文件结构', '文件操作', '文件保护', '文件系统实现'],
  '设备管理': ['设备驱动', 'I/O控制', '设备分配', '缓冲区管理'],
  '死锁': ['死锁预防', '死锁避免', '死锁检测', '死锁恢复'],
  '虚拟内存': ['分页置换算法', '请求分页', '页面调度'],
  '进程调度': ['调度算法', '优先级调度', '时间片轮转'],
  
  // 数据库系统概论
  '数据库系统概论': ['数据库模型', 'SQL语言', '事务管理', '数据库设计', '数据库安全', '数据库优化'],
  '数据库模型': ['关系模型', 'ER模型', '层次模型', '网状模型', '面向对象模型'],
  'SQL语言': ['查询语句', '数据定义', '数据操纵', '数据控制', '存储过程'],
  '事务管理': ['ACID特性', '并发控制', '事务隔离级别', '锁机制'],
  '数据库设计': ['范式理论', '数据库规划', '概念设计', '逻辑设计', '物理设计'],
  '数据库安全': ['访问控制', '数据加密', '审计', '备份与恢复'],
  '数据库优化': ['查询优化', '索引优化', '存储优化', '性能调优'],
  '关系模型': ['关系代数', '关系演算', '规范化理论'],
  
  // 数据结构与算法
  '数据结构与算法': ['线性结构', '树形结构', '图结构', '排序算法', '查找算法', '哈希表'],
  '线性结构': ['数组', '链表', '栈', '队列', '字符串'],
  '树形结构': ['二叉树', '平衡树', '红黑树', 'B树', '堆'],
  '图结构': ['图的遍历', '最短路径', '最小生成树', '拓扑排序', '网络流'],
  '排序算法': ['快速排序', '归并排序', '堆排序', '插入排序', '冒泡排序', '选择排序'],
  '查找算法': ['二分查找', '顺序查找', '哈希查找', '二叉搜索树查找'],
  '哈希表': ['哈希函数', '冲突处理', '负载因子', '哈希表实现'],
  '链表': ['单链表', '双链表', '循环链表', '链表操作'],
  
  // 新视野大学英语1
  '新视野大学英语1（第三版）': ['听力', '口语', '阅读', '写作', '词汇', '语法'],
  '听力': ['对话理解', '短文听力', '新闻听力', '讲座听力'],
  '口语': ['日常对话', '话题讨论', '演讲', '辩论'],
  '阅读': ['精读', '泛读', '快速阅读', '批判性阅读'],
  '写作': ['应用文', '议论文', '叙述文', '说明文'],
  '词汇': ['核心词汇', '短语搭配', '词汇记忆', '词汇应用'],
  '语法': ['时态', '语态', '从句', '虚拟语气', '非谓语动词'],
  
  // 概率论与数理统计
  '概率论与数理统计': ['概率基础', '随机变量', '统计推断', '假设检验', '方差分析', '回归分析'],
  '概率基础': ['概率公理', '条件概率', '独立性', '贝叶斯定理'],
  '随机变量': ['离散型', '连续型', '分布函数', '数字特征'],
  '统计推断': ['参数估计', '区间估计', '点估计', '估计量评价'],
  '假设检验': ['t检验', '卡方检验', 'F检验', '假设检验步骤'],
  '方差分析': ['单因素方差分析', '多因素方差分析', '方差齐性检验'],
  '回归分析': ['线性回归', '多元回归', '回归模型诊断', '非线性回归'],
  
  // 离散数学
  '离散数学': ['集合论', '图论', '代数系统', '数理逻辑', '组合数学', '数论'],
  '集合论': ['集合运算', '关系', '函数', '基数', '序数'],
  '图论': ['图的基本概念', '树', '路径', '回路', '图的着色'],
  '代数系统': ['群', '环', '域', '格', '布尔代数'],
  '数理逻辑': ['命题逻辑', '谓词逻辑', '推理规则', '范式'],
  '组合数学': ['排列组合', '容斥原理', '生成函数', '递推关系'],
  '数论': ['整除性', '同余', '素数', '最大公约数'],
  
  // 线性代数
  '线性代数': ['矩阵', '行列式', '线性方程组', '特征值', '向量空间', '线性变换'],
  '矩阵': ['矩阵运算', '矩阵的秩', '逆矩阵', '转置矩阵', '正定矩阵'],
  '行列式': ['行列式计算', '克莱姆法则', '行列式性质'],
  '线性方程组': ['高斯消元', '解的结构', '解的存在性', '解空间'],
  '特征值': ['特征向量', '相似对角化', '特征值性质'],
  '向量空间': ['向量运算', '基与维数', '子空间', '正交性'],
  '线性变换': ['变换矩阵', '特征变换', '正交变换'],
  
  // 经济学原理
  '经济学原理': ['微观经济学', '宏观经济学', '国际经济学', '发展经济学', '计量经济学', '博弈论'],
  '微观经济学': ['供给与需求', '市场结构', '消费者行为', '生产者行为', '市场失灵'],
  '宏观经济学': ['GDP', '通货膨胀', '失业', '经济增长', '财政政策', '货币政策'],
  '国际经济学': ['国际贸易', '汇率', '国际收支', '经济全球化'],
  '发展经济学': ['经济增长', '发展战略', '贫困与不平等', '可持续发展'],
  '计量经济学': ['回归分析', '时间序列分析', '因果推断', '经济预测'],
  '博弈论': ['纳什均衡', '囚徒困境', '重复博弈', '合作博弈'],
  
  // 马克思主义基本原理
  '马克思主义基本原理': ['哲学', '政治经济学', '科学社会主义', '马克思主义发展史', '马克思主义中国化'],
  '哲学': ['唯物辩证法', '认识论', '历史唯物主义', '辩证唯物主义'],
  '政治经济学': ['剩余价值理论', '资本主义经济', '劳动价值论', '资本积累'],
  '科学社会主义': ['社会主义理论', '共产主义', '无产阶级革命', '社会主义建设'],
  '马克思主义发展史': ['马克思恩格斯', '列宁主义', '毛泽东思想', '中国特色社会主义理论体系'],
  '马克思主义中国化': ['毛泽东思想', '邓小平理论', '三个代表重要思想', '科学发展观', '习近平新时代中国特色社会主义思想'],
  
  // 横向关联关系
  '微积分': ['力学', '电磁学', '经济学原理'],
  '线性代数': ['计算机网络', '数据结构与算法', '量子力学'],
  '概率论': ['统计学', '机器学习', '经济学原理'],
  '数据结构与算法': ['操作系统原理', '计算机网络', '数据库系统概论'],
  '计算机网络': ['操作系统原理', '数据库系统概论'],
  '操作系统原理': ['计算机网络', '数据库系统概论'],
  '数据库系统概论': ['计算机网络', '操作系统原理'],
  '大学物理': ['高等数学'],
  '经济学原理': ['高等数学', '概率论'],
  '离散数学': ['数据结构与算法', '计算机网络'],
  '马克思主义基本原理': ['哲学', '经济学原理']
}

// 节点高频知识点映射
const nodeHighFrequencyKnowledge = {
  // 高等数学
  '高等数学': [
    '高等数学是大学数学的基础课程',
    '包括微积分、线性代数、概率论等核心内容',
    '是理工科学生的必修课程',
    '是许多专业课程的基础'
  ],
  '微积分': [
    '微积分是研究函数的微分、积分以及相关概念和应用的数学分支',
    '包括微分学和积分学两大部分',
    '是现代数学的基础',
    '在物理学、工程学等领域有广泛应用'
  ],
  '线性代数': [
    '线性代数是研究向量空间和线性变换的数学分支',
    '包括矩阵、行列式、特征值等核心概念',
    '是计算机科学、物理学等领域的重要工具',
    '在数据科学和机器学习中有广泛应用'
  ],
  '概率论': [
    '概率论是研究随机现象数量规律的数学分支',
    '包括随机变量、概率分布、期望、方差等概念',
    '是统计学的基础',
    '在金融、保险、人工智能等领域有广泛应用'
  ],
  '常微分方程': [
    '常微分方程是研究含有一个自变量的微分方程的数学分支',
    '包括一阶、二阶微分方程等',
    '是描述自然现象的重要工具',
    '在物理学、工程学、生物学等领域有广泛应用'
  ],
  
  // 计算机网络
  '计算机网络': [
    '计算机网络是计算机科学与技术专业的核心课程',
    'TCP/IP协议栈是互联网的基础',
    '网络分层结构：物理层、数据链路层、网络层、传输层、应用层',
    '网络安全是网络设计的重要考虑因素'
  ],
  '网络协议': [
    '网络协议是计算机网络的核心组成部分',
    'TCP/IP协议栈是互联网的基础',
    'HTTP/HTTPS是Web应用的基础',
    '协议的分层设计原则'
  ],
  '网络拓扑': [
    '网络拓扑是网络的物理或逻辑结构',
    '常见拓扑结构：星型、总线型、环型、网状',
    '路由算法是网络拓扑的核心',
    '网络拓扑影响网络性能和可靠性'
  ],
  '网络安全': [
    '网络安全是网络设计的重要考虑因素',
    '防火墙是网络安全的第一道防线',
    '加密技术保护数据传输安全',
    '安全协议确保通信安全'
  ],
  
  // 体育与健康
  '体育与健康': [
    '体育与健康是促进身心健康的重要课程',
    '包括体育理论、运动技能、健康知识等内容',
    '体育锻炼可以增强体质，提高免疫力',
    '健康的生活方式是幸福人生的基础'
  ],
  '运动生理学': [
    '运动生理学研究运动对人体生理机能的影响',
    '心肺功能是衡量体质的重要指标',
    '能量代谢是运动的基础',
    '运动疲劳是运动训练中的重要问题'
  ],
  
  // 大学物理
  '大学物理（上册）': [
    '大学物理是理工科学生的必修基础课程',
    '包括力学、热学、电磁学、光学等内容',
    '是理解自然现象的重要工具',
    '与高等数学密切相关'
  ],
  '大学物理（下册）': [
    '包括量子力学、相对论、固体物理等内容',
    '是现代物理学的基础',
    '对理解现代科技发展至关重要',
    '需要较强的数学基础'
  ],
  
  // 操作系统原理
  '操作系统原理': [
    '操作系统是计算机系统的核心软件',
    '管理计算机的硬件和软件资源',
    '提供用户与计算机之间的接口',
    '包括进程管理、内存管理、文件系统等核心功能'
  ],
  '进程管理': [
    '进程是程序的执行实例',
    '进程调度决定CPU的分配',
    '进程同步解决进程间的协作问题',
    '进程通信实现进程间的数据交换'
  ],
  
  // 数据库系统概论
  '数据库系统概论': [
    '数据库是现代信息系统的核心',
    '关系模型是主流的数据库模型',
    'SQL是数据库操作的标准语言',
    '事务管理确保数据的一致性和可靠性'
  ],
  'SQL语言': [
    'SQL是结构化查询语言',
    '用于数据库的查询、插入、更新和删除操作',
    '包括数据定义语言(DDL)和数据操纵语言(DML)',
    '是数据库管理员和开发人员的必备技能'
  ],
  
  // 数据结构与算法
  '数据结构与算法': [
    '数据结构是组织和存储数据的方式',
    '算法是解决问题的步骤和方法',
    '是计算机科学的核心内容',
    '对程序的效率和性能有重要影响'
  ],
  '排序算法': [
    '排序是最基本的算法问题之一',
    '快速排序、归并排序、堆排序是常见的高效排序算法',
    '排序算法的时间复杂度是衡量其性能的重要指标',
    '排序算法在实际应用中广泛使用'
  ],
  
  // 新视野大学英语1
  '新视野大学英语1（第三版）': [
    '新视野大学英语是大学英语的主流教材',
    '注重听说读写全面发展',
    '培养学生的英语综合应用能力',
    '是大学英语四级考试的重要参考资料'
  ],
  '听力': [
    '听力是英语学习的重要组成部分',
    '提高听力能力需要多听多练',
    '掌握听力技巧可以提高听力水平',
    '听力能力对英语考试成绩有重要影响'
  ],
  
  // 概率论与数理统计
  '概率论与数理统计': [
    '概率论与数理统计是研究随机现象的数学分支',
    '概率论研究随机现象的规律',
    '数理统计研究数据的收集、整理和分析',
    '在科学研究、工程技术、经济管理等领域有广泛应用'
  ],
  '统计推断': [
    '统计推断是根据样本数据推断总体特征',
    '参数估计和假设检验是统计推断的主要内容',
    '统计推断在科学研究中起着重要作用',
    '需要掌握抽样分布和统计量的性质'
  ],
  
  // 离散数学
  '离散数学': [
    '离散数学是计算机科学的数学基础',
    '包括集合论、图论、代数系统、数理逻辑等内容',
    '是算法设计和分析的基础',
    '对理解计算机科学的理论问题至关重要'
  ],
  '图论': [
    '图论是研究图的性质和应用的数学分支',
    '图是由顶点和边组成的数学结构',
    '在计算机网络、社交网络、交通规划等领域有广泛应用',
    '图的遍历、最短路径等算法是图论的核心内容'
  ],
  
  // 线性代数
  '线性代数': [
    '线性代数是研究向量空间和线性变换的数学分支',
    '矩阵是线性代数的重要工具',
    '在计算机图形学、机器学习、信号处理等领域有广泛应用',
    '是理解现代科技的重要数学基础'
  ],
  '矩阵': [
    '矩阵是线性代数的核心概念',
    '矩阵运算包括加法、乘法、转置等',
    '矩阵的秩、特征值、特征向量等是重要的矩阵性质',
    '在计算机科学和工程中广泛应用'
  ],
  
  // 经济学原理
  '经济学原理': [
    '经济学是研究资源配置的社会科学',
    '微观经济学研究个体经济行为',
    '宏观经济学研究整体经济运行',
    '经济学原理对理解社会经济现象至关重要'
  ],
  '微观经济学': [
    '微观经济学研究个体经济单位的行为',
    '供给与需求是微观经济学的核心概念',
    '市场结构包括完全竞争、垄断、寡头等',
    '消费者行为和生产者行为是微观经济学的重要内容'
  ],
  
  // 马克思主义基本原理
  '马克思主义基本原理': [
    '马克思主义是科学的世界观和方法论',
    '包括哲学、政治经济学、科学社会主义三个组成部分',
    '是指导我们认识世界和改造世界的强大思想武器',
    '马克思主义中国化是马克思主义的重要发展'
  ],
  '哲学': [
    '哲学是关于世界观和方法论的学问',
    '唯物辩证法是马克思主义哲学的核心',
    '认识论研究认识的本质和规律',
    '历史唯物主义是研究社会发展规律的科学'
  ]
}

// 初始化 ECharts
const initChart = () => {
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
  
  // 初始化节点位置
  initNodePositionsForBook(currentGraphData)
  
  chart = echarts.init(chartContainer.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        // 只显示节点的提示，不显示连接线的提示
        if (params.dataType === 'node') {
          const nodeName = params.name
          const highFrequencyKnowledge = nodeHighFrequencyKnowledge[nodeName] || []
          let tooltipContent = `<div style="font-weight: bold; margin-bottom: 8px;">${nodeName}</div>`
          
          if (highFrequencyKnowledge.length > 0) {
            tooltipContent += '<div style="font-size: 12px; color: #666;">高频知识点：</div>'
            highFrequencyKnowledge.forEach(item => {
              tooltipContent += `<div style="font-size: 12px; margin-left: 10px; margin-top: 4px;">• ${item}</div>`
            })
          }
          
          return tooltipContent
        }
        return ''
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#3b82f6',
      borderWidth: 1,
      padding: 12,
      textStyle: {
        fontSize: 14
      }
    },
    animationDuration: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: currentGraphData.nodes.map(node => ({
          ...node,
          itemStyle: {
            color: getNodeColor(node.category),
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          },
          label: {
            show: true,
            position: node.category === 0 ? 'inside' : 'right',
            fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
            fontWeight: node.category <= 1 ? 'bold' : 'normal',
            color: node.category === 0 ? '#fff' : '#333',
            distance: node.category > 0 ? 10 : 0
          },
          symbolSize: node.symbolSize || (node.category === 0 ? 80 : node.category === 1 ? 60 : 45),
          emphasis: {
            itemStyle: {
              shadowBlur: 20,
              shadowColor: 'rgba(59, 130, 246, 0.6)',
              borderWidth: 3
            },
            label: {
              fontSize: node.category === 0 ? 18 : node.category === 1 ? 16 : 14,
              fontWeight: 'bold'
            }
          }
        })),
        links: currentGraphData.links.map(link => ({
          ...link,
          lineStyle: {
            color: getLinkColor(link.source),
            width: getLinkWidth(link.source),
            curveness: 0.2,
            type: getLinkType(link.source),
            opacity: 0.7
          },
          emphasis: {
            lineStyle: {
              width: 4,
              opacity: 1
            }
          }
        })),
        categories: [
          { name: '核心课程', itemStyle: { color: '#1565c0' } },
          { name: '章节模块', itemStyle: { color: '#1976d2' } },
          { name: '核心知识点', itemStyle: { color: '#1e88e5' } },
          { name: '细分知识点', itemStyle: { color: '#42a5f5' } }
        ],
        roam: true,
        draggable: true,
        dragMode: ['pan', 'node'],
        focusNodeAdjacency: true,
        cursor: 'grab',
        emphasis: {
          cursor: 'grabbing',
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        },
        scaleLimit: {
          min: 0.1,
          max: 10
        },
        force: {
          initLayout: 'circular',
          repulsion: 12000,
          edgeLength: [150, 300],
          gravity: 0.02,
          friction: 0.9,
          layoutAnimation: true,
          edgeLengthMin: 100,
          edgeLengthMax: 400
        }
      }
    ]
  }
  
  chart.setOption(option)
  
  // 点击事件
  chart.on('click', (params) => {
    if (params.dataType === 'node') {
      handleNodeClick(params.data.name)
    }
  })
  
  // 窗口大小变化时重新调整
  window.addEventListener('resize', handleResize)
  
  // 初始化侧边栏内容
  updateSidebarContent('高等数学')
}

// 组件挂载
onMounted(async () => {
  componentState.isMounted.value = true
  componentState.isDestroyed.value = false
  componentState.isComponentDestroyed = false
  
  try {
    // 初始化图表
    initChart()
    
    // 获取书架书籍
    await fetchBookshelfBooks()
    
    // 启动推荐更新定时器
    startRecommendationUpdates()
    
    // 检查路由参数，是否需要打开习题弹窗
    if (route.query.showExerciseModal === 'true') {
      showExerciseModal.value = true
    }
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', handleResize)
    
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
    chart.dispose()
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
const getNodeColor = (category) => {
  const colors = ['#1565c0', '#1976d2', '#1e88e5', '#42a5f5']
  return colors[category] || '#64b5f6'
}

// 获取连接线颜色
const getLinkColor = (source) => {
  const node = currentGraphData.nodes.find(n => n.id === source)
  if (!node) return '#ccc'
  if (node.category === 0) return '#1e88e5'
  if (node.category === 1) return '#42a5f5'
  return '#90caf9'
}

// 获取连接线宽度
const getLinkWidth = (source) => {
  const node = currentGraphData.nodes.find(n => n.id === source)
  if (!node) return 1
  if (node.category === 0) return 3
  if (node.category === 1) return 2
  return 1
}

// 获取连接线类型
const getLinkType = (source) => {
  const node = currentGraphData.nodes.find(n => n.id === source)
  if (!node) return 'solid'
  if (node.category === 0) return 'solid'
  if (node.category === 1) return 'dashed'
  return 'dotted'
}

// 处理节点点击
const handleNodeClick = (nodeName) => {
  if (isDestroyed.value) return
  selectedNode.value = nodeName
  
  // 更新侧边栏
  updateSidebarContent(nodeName)
  
  // 高亮关联节点
  highlightRelatedNodes(nodeName)
}

// 更新侧边栏内容
const updateSidebarContent = (nodeName) => {
  const node = currentGraphData.nodes.find(n => n.name === nodeName)
  if (!node) return
  
  // 更新副标题
  const subtitles = ['核心课程', '章节模块', '核心知识点', '细分知识点']
  nodeSubtitle.value = subtitles[node.category] || '知识点'
  
  // 更新知识密度
  knowledgeDensity.value = node.value || 50
  
  // 更新高频知识点
  if (nodeHighFrequencyKnowledge[nodeName]) {
    highFrequencyKnowledge.value = nodeHighFrequencyKnowledge[nodeName]
  } else {
    // 根据当前书籍生成相关的高频知识点
    const currentBookTitle = currentBook.value?.title || '高等数学'
    highFrequencyKnowledge.value = [
      `${nodeName}是${currentBookTitle}的重要组成部分`,
      `掌握${nodeName}对理解后续内容至关重要`,
      `${nodeName}在考试中出现频率较高`,
      `建议通过练习巩固${nodeName}的相关知识`
    ]
  }
  
  // 更新关联节点
  // 从当前图谱数据中动态获取关联节点
  const relatedNodeIds = new Set()
  currentGraphData.links.forEach(link => {
    if (link.source === node.id) {
      relatedNodeIds.add(link.target)
    } else if (link.target === node.id) {
      relatedNodeIds.add(link.source)
    }
  })
  
  // 获取关联节点的名称
  const relatedNames = currentGraphData.nodes
    .filter(n => relatedNodeIds.has(n.id))
    .map(n => n.name)
  
  relatedNodes.value = relatedNames.length > 0 ? relatedNames : ['无关联节点']
}

// 显示提示消息
const showToastMessage = (message, type = 'info', duration = 3000) => {
  // 使用安全执行包装
  safeExecute(() => {
    if (!ElMessage) {
      console.log(message) // 降级到console
      return
    }
  
    const options = {
      message,
      duration,
      showClose: true
    }
  
    switch (type) {
      case 'success': ElMessage.success(options); break
      case 'error': ElMessage.error(options); break
      case 'warning': ElMessage.warning(options); break
      default: ElMessage.info(options)
    }
  })
}

// 学习进度弹窗
const showProgressModal = ref(false)
const selectedKnowledgePoint = ref('')
const knowledgeMastery = ref(0)
const practiceHistory = ref([])

// 显示学习进度
const showLearningProgress = () => {
  if (!selectedNode.value) return
  
  selectedKnowledgePoint.value = selectedNode.value
  const bookId = currentBook.value?.id || 'default'
  knowledgeMastery.value = calculateKnowledgeMastery(selectedNode.value, bookId)
  
  // 生成练习历史模拟数据
  practiceHistory.value = [
    {
      date: '2024-03-20',
      correct: 3,
      total: 5,
      mastery: 0.6
    },
    {
      date: '2024-03-25',
      correct: 4,
      total: 5,
      mastery: 0.8
    },
    {
      date: '2024-03-30',
      correct: 5,
      total: 5,
      mastery: 1.0
    }
  ]
  
  showProgressModal.value = true
}

// 开始单个习题练习
const startSingleExercise = (exercise) => {
  // 跳转到练习页面，传递单个习题数据
  router.push({
    path: '/practice',
    query: {
      from: 'graph',
      showExerciseModal: 'true',
      multiple: 'false',
      exerciseId: exercise.id
    }
  })
}

// 高亮关联节点
const highlightRelatedNodes = (nodeName) => {
  if (!chart) return
  
  // 从当前图谱数据中动态获取关联节点
  const targetNode = currentGraphData.nodes.find(n => n.name === nodeName)
  if (!targetNode) return
  
  // 找到所有直接关联的节点
  const relatedNodeIds = new Set()
  currentGraphData.links.forEach(link => {
    if (link.source === targetNode.id) {
      relatedNodeIds.add(link.target)
    } else if (link.target === targetNode.id) {
      relatedNodeIds.add(link.source)
    }
  })
  
  // 获取关联节点的名称
  const relatedNames = currentGraphData.nodes
    .filter(n => relatedNodeIds.has(n.id))
    .map(n => n.name)
  
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    return
  }
  
  const series = option.series[0]
  
  // 更新节点样式
  series.data = series.data.map(node => {
    const isRelated = relatedNames.includes(node.name) || node.name === nodeName
    const isSelected = node.name === nodeName
    return {
      ...node,
      itemStyle: {
        color: isRelated ? getNodeColor(node.category) : getNodeColor(node.category),
        opacity: isRelated ? 1 : 0.85,
        borderColor: isSelected ? '#ff6b6b' : 'transparent',
        borderWidth: isSelected ? 3 : 0
      },
      label: {
        ...node.label,
        opacity: 1,
        color: isRelated ? '#fff' : '#333'
      }
    }
  })
  
  // 更新连接线样式
  series.links = series.links.map(link => {
    const sourceNode = series.data.find(n => n.id === link.source)
    const targetNode = series.data.find(n => n.id === link.target)
    const isRelated = sourceNode?.itemStyle?.opacity === 1 && targetNode?.itemStyle?.opacity === 1
    
    return {
      ...link,
      lineStyle: {
        ...link.lineStyle,
        opacity: isRelated ? 1 : 0.3
      }
    }
  })
  
  chart.setOption(option)
}

// 清除高亮
const clearHighlights = () => {
  if (!chart) return
  
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    return
  }
  
  const series = option.series[0]
  
  // 恢复所有节点
  series.data = series.data.map(node => ({
    ...node,
    itemStyle: {
      color: getNodeColor(node.category),
      opacity: 1
    },
    label: {
      ...node.label,
      opacity: 1
    }
  }))
  
  // 恢复所有连接线
  series.links = series.links.map(link => ({
    ...link,
    lineStyle: {
      ...link.lineStyle,
      opacity: 1
    }
  }))
  
  chart.setOption(option)
}

// 聚焦到节点
const focusOnNode = (nodeName) => {
  if (!chart) return
  
  const node = currentGraphData.nodes.find(n => n.name === nodeName)
  if (!node) return
  
  // 触发点击事件
  handleNodeClick(nodeName)
  
  // 显示提示
  showToastMessage(`已定位到节点：${nodeName}`)
}

// 显示所有节点
const showAllNodes = () => {
  clearHighlights()
  closeSearch()
  showToastMessage('已显示所有知识点')
}

// 切换书籍选择器
const toggleBookSelector = () => {
  showBookSelector.value = !showBookSelector.value
}

// 处理书籍搜索
const handleBookSearch = async () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping handleBookSearch')
    return
  }
  if (!bookSearchQuery.value.trim()) {
    bookSearchResults.value = []
    return
  }
  
  try {
    const response = await api.get('/books/search', {
      params: { keyword: bookSearchQuery.value }
    })
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    bookSearchResults.value = response.data
  } catch (error) {
    console.error('搜索书籍失败:', error)
  }
}

// 选择书籍
const selectBook = async (book) => {
  // 立即检查
  if (!componentState.isMounted.value || isLoading.value) {
    return
  }

  safeUpdate(isLoading, true)
  
  let controller = null
  
  try {
    // 使用AbortController
    controller = new AbortController()
    componentState.abortControllers.add(controller)
    
    await safeAsync(async () => {
      safeUpdate(currentBook, book)
      safeUpdate(selectedNode, book.title)
      safeUpdate(showBookSelector, false)
      
      try {
        // 传递signal到API调用
        const response = await apiService.knowledgeGraph.getKnowledgeGraphByBook(book.id)
        
        // 再次检查组件状态
        if (!componentState.isMounted.value) return
        
        if (response && response.nodes && response.relations) {
          currentGraphData = {
            nodes: response.nodes.map(node => ({
              id: node.id,
              name: node.name,
              category: node.category || 0,
              value: node.value || 50,
              symbolSize: node.symbolSize || 40
            })),
            links: response.relations.map(relation => ({
            source: relation.sourceNodeId,
            target: relation.targetNodeId
          }))
          }
          
          // 使用nextTick确保DOM就绪
          nextTick(() => {
            if (componentState.isMounted.value) {
              updateGraphForBook(book)
            }
          })
          
          // 如果返回的是基础知识图谱，启动轮询检查完整图谱是否生成
          if (response.nodes && response.nodes.length === 1 && response.nodes[0].description === '正在生成完整知识图谱...') {
            startGraphPolling(book.id)
          }
        } else {
          // 处理空数据情况
          currentGraphData = {
            nodes: [],
            links: []
          }
          showToastMessage('获取知识图谱数据失败，显示空图谱', 'error')
        }
      } catch (error) {
        // 忽略"abort"错误
        if (error.name !== 'AbortError') {
          console.error('获取知识图谱失败:', error)
          // 使用空数据继续，避免页面崩溃
          currentGraphData = {
            nodes: [],
            links: []
          }
          showToastMessage('服务器内部错误，请稍后重试', 'error')
        }
      }
    })
  } catch (error) {
    if (error.message.includes('未挂载') || error.message.includes('卸载') || error.name === 'AbortError') {
      // 静默处理组件卸载错误和中止错误
      return
    }
    
    console.error('选择书籍失败:', error)
    showToastMessage(`选择书籍失败: ${error.message}`, 'error')
  } finally {
    // 清理AbortController
    if (controller) {
      componentState.abortControllers.delete(controller)
    }
    
    if (componentState.isMounted.value) {
      safeUpdate(isLoading, false)
    }
  }
}


// 轮询检查知识图谱是否生成完成
let pollingTimer = null

const startGraphPolling = (bookId) => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping startGraphPolling')
    return
  }
  
  // 清除之前的轮询
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  
  // 每3秒检查一次
  pollingTimer = setInterval(async () => {
    // 检查组件是否已卸载、销毁
    if (!componentState.isMounted.value) {
      clearInterval(pollingTimer)
      pollingTimer = null
      return
    }
    
    try {
      const response = await apiService.knowledgeGraph.getKnowledgeGraphByBook(bookId)
      
      // 检查组件是否已卸载、销毁
      if (!componentState.isMounted.value) {
        clearInterval(pollingTimer)
        pollingTimer = null
        return
      }
      
      if (response && response.nodes && response.relations && response.nodes.length > 1) {
        // 完整知识图谱已生成
        currentGraphData = {
          nodes: response.nodes.map(node => ({
            id: node.id,
            name: node.name,
            category: node.category || 0,
            value: node.value || 50,
            symbolSize: node.symbolSize || 40
          })),
          links: response.relations.map(relation => ({
            source: relation.sourceNodeId,
            target: relation.targetNodeId
          }))
        }
        
        // 检查组件是否已卸载、销毁
        if (!componentState.isMounted.value) {
          clearInterval(pollingTimer)
          pollingTimer = null
          return
        }
        
        updateGraphForBook(currentBook.value)
        clearInterval(pollingTimer)
        pollingTimer = null
        showToastMessage('知识图谱生成完成', 'success')
      }
    } catch (error) {
      console.error('轮询知识图谱失败:', error)
      // 检查组件是否已卸载、销毁
      if (!componentState.isMounted.value) {
        clearInterval(pollingTimer)
        pollingTimer = null
        return
      }
      // 停止轮询并显示错误信息
      clearInterval(pollingTimer)
      pollingTimer = null
      showToastMessage('轮询获取知识图谱失败，请稍后手动刷新', 'error')
    }
  }, 3000)
}



// 生成默认知识图谱数据
const generateDefaultGraphData = (bookTitle) => {
  const nodes = []
  const links = []
  let nodeId = 1
  
  // 添加中心节点
  nodes.push({
    id: nodeId,
    name: bookTitle,
    category: 0,
    value: 100,
    symbolSize: 80
  })
  nodeId++
  
  // 添加一级节点
  const level1Nodes = nodeRelations[bookTitle] || []
  level1Nodes.forEach(level1Name => {
    nodes.push({
      id: nodeId,
      name: level1Name,
      category: 1,
      value: 80,
      symbolSize: 60
    })
    links.push({
      source: 1, // 中心节点
      target: nodeId
    })
    
    // 添加二级节点
    const level2Nodes = nodeRelations[level1Name] || []
    const currentLevel1Id = nodeId
    nodeId++
    
    level2Nodes.forEach(level2Name => {
      nodes.push({
        id: nodeId,
        name: level2Name,
        category: 2,
        value: 60,
        symbolSize: 45
      })
      links.push({
        source: currentLevel1Id,
        target: nodeId
      })
      nodeId++
    })
  })
  
  return { nodes, links }
}

// 打开习题弹窗
const openExerciseModal = async () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping openExerciseModal')
    return
  }
  showExerciseModal.value = true
  await fetchExercises(selectedNode.value)
}

// 切换习题难度筛选
const changeExerciseFilter = async (filter) => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping changeExerciseFilter')
    return
  }
  exerciseFilter.value = filter
  await fetchExercises(selectedNode.value)
}

// 切换习题类型筛选
const changeExerciseTypeFilter = async (filter) => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping changeExerciseTypeFilter')
    return
  }
  exerciseTypeFilter.value = filter
  await fetchExercises(selectedNode.value)
}

// 当前图谱数据
let currentGraphData = {
  nodes: [
    { id: 1, name: '高等数学', category: 0, value: 100, symbolSize: 80 },
    { id: 2, name: '微积分', category: 1, value: 80, symbolSize: 60 },
    { id: 3, name: '线性代数', category: 1, value: 75, symbolSize: 60 },
    { id: 4, name: '概率论', category: 1, value: 70, symbolSize: 60 },
    { id: 5, name: '常微分方程', category: 1, value: 65, symbolSize: 60 },
    { id: 6, name: '导数', category: 2, value: 60, symbolSize: 45 },
    { id: 7, name: '积分', category: 2, value: 55, symbolSize: 45 },
    { id: 8, name: '矩阵', category: 2, value: 50, symbolSize: 45 },
    { id: 9, name: '行列式', category: 2, value: 45, symbolSize: 45 },
    { id: 10, name: '随机变量', category: 2, value: 40, symbolSize: 45 },
    { id: 11, name: '一阶微分方程', category: 2, value: 35, symbolSize: 45 },
    // 计算机网络
    { id: 12, name: '计算机网络', category: 0, value: 100, symbolSize: 80 },
    { id: 13, name: '网络协议', category: 1, value: 85, symbolSize: 60 },
    { id: 14, name: '网络拓扑', category: 1, value: 75, symbolSize: 60 },
    { id: 15, name: '网络安全', category: 1, value: 80, symbolSize: 60 },
    { id: 16, name: '网络管理', category: 1, value: 70, symbolSize: 60 },
    { id: 17, name: 'TCP/IP', category: 2, value: 65, symbolSize: 45 },
    { id: 18, name: 'HTTP/HTTPS', category: 2, value: 60, symbolSize: 45 },
    { id: 19, name: '路由算法', category: 2, value: 55, symbolSize: 45 },
    { id: 20, name: '防火墙', category: 2, value: 50, symbolSize: 45 }
  ],
  links: [
    { source: 1, target: 2 },
    { source: 1, target: 3 },
    { source: 1, target: 4 },
    { source: 1, target: 5 },
    { source: 2, target: 6 },
    { source: 2, target: 7 },
    { source: 3, target: 8 },
    { source: 3, target: 9 },
    { source: 4, target: 10 },
    { source: 5, target: 11 },
    // 计算机网络连接
    { source: 12, target: 13 },
    { source: 12, target: 14 },
    { source: 12, target: 15 },
    { source: 12, target: 16 },
    { source: 13, target: 17 },
    { source: 13, target: 18 },
    { source: 14, target: 19 },
    { source: 15, target: 20 }
  ]
}

// 根据书籍更新图谱
const updateGraphForBook = (book) => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping updateGraphForBook')
    return
  }
  if (!chart) return
  
  // 初始化节点位置
  initNodePositionsForBook(currentGraphData)
  
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) return
  
  // 更新右侧边栏信息
  updateSidebarForBook(book)
  
  // 更新图表
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    // 如果option或series不存在，重新初始化图表
    initChart()
    // 检查组件是否已销毁
    if (componentState.isMounted.value) {
      showToastMessage(`已切换到《${book.title}》知识图谱`, 'success')
    }
    return
  }
  
  option.series[0].data = currentGraphData.nodes.map(node => ({
    ...node,
    itemStyle: {
      color: getNodeColor(node.category)
    },
    label: {
      show: true,
      position: 'inside',
      fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
      fontWeight: node.category <= 1 ? 'bold' : 'normal',
      color: '#fff'
    }
  }))
  option.series[0].links = currentGraphData.links.map(link => ({
    ...link,
    lineStyle: {
      color: getLinkColor(link.source),
      width: getLinkWidth(link.source),
      curveness: 0.2,
      type: getLinkType(link.source)
    }
  }))
  chart.setOption(option, true)
  
  // 显示提示
  if (componentState.isMounted.value) {
    showToastMessage(`已切换到《${book.title}》知识图谱`, 'success')
  }
}

// 为书籍初始化节点位置
const initNodePositionsForBook = (data) => {
  const centerX = 0
  const centerY = 0
  const level1Radius = 200
  const level2Radius = 350
  const level3Radius = 500
  
  // 第一层：中心节点
  const centerNode = data.nodes.find(n => n.category === 0)
  if (centerNode) {
    centerNode.x = centerX
    centerNode.y = centerY
    centerNode.fixed = true
  }
  
  // 第二层：主要模块
  const level1Nodes = data.nodes.filter(n => n.category === 1)
  level1Nodes.forEach((node, index) => {
    const angle = (index * 360 / level1Nodes.length - 90) * Math.PI / 180
    node.x = centerX + level1Radius * Math.cos(angle)
    node.y = centerY + level1Radius * Math.sin(angle)
  })
  
  // 第三层和第四层根据连接关系分布
  const level2Nodes = data.nodes.filter(n => n.category === 2)
  const level2Groups = {}
  level2Nodes.forEach(node => {
    const parentLink = data.links.find(l => l.target === node.id)
    if (parentLink) {
      if (!level2Groups[parentLink.source]) {
        level2Groups[parentLink.source] = []
      }
      level2Groups[parentLink.source].push(node)
    }
  })
  
  Object.keys(level2Groups).forEach((parentId) => {
    const parentNode = data.nodes.find(n => n.id === parentId)
    const nodes = level2Groups[parentId]
    if (parentNode) {
      const baseAngle = Math.atan2(parentNode.y - centerY, parentNode.x - centerX)
      
      nodes.forEach((node, index) => {
        const offset = (index - (nodes.length - 1) / 2) * 0.5
        const angle = baseAngle + offset
        node.x = centerX + level2Radius * Math.cos(angle)
        node.y = centerY + level2Radius * Math.sin(angle)
      })
    }
  })
  
  // 第四层：细分知识点
  const level3Nodes = data.nodes.filter(n => n.category === 3)
  const level3Groups = {}
  level3Nodes.forEach(node => {
    const parentLink = data.links.find(l => l.target === node.id)
    if (parentLink) {
      if (!level3Groups[parentLink.source]) {
        level3Groups[parentLink.source] = []
      }
      level3Groups[parentLink.source].push(node)
    }
  })
  
  Object.keys(level3Groups).forEach((parentId) => {
    const parentNode = data.nodes.find(n => n.id === parentId)
    const nodes = level3Groups[parentId]
    if (parentNode) {
      const baseAngle = Math.atan2(parentNode.y - centerY, parentNode.x - centerX)
      
      nodes.forEach((node, index) => {
        const offset = (index - (nodes.length - 1) / 2) * 0.4
        const angle = baseAngle + offset
        node.x = centerX + level3Radius * Math.cos(angle)
        node.y = centerY + level3Radius * Math.sin(angle)
      })
    }
  })
}

// 用于跟踪当前正在进行的侧边栏更新请求
let sidebarUpdateAbortController = null
// 用于跟踪所有AbortController
const abortControllers = []

// 根据书籍更新侧边栏信息
const updateSidebarForBook = async (book) => {
  return safeAsync(async () => {
    // 取消之前的请求
    if (sidebarUpdateAbortController) {
      sidebarUpdateAbortController.abort()
    }
    
    // 创建新的AbortController
    sidebarUpdateAbortController = new AbortController()
    componentState.abortControllers.add(sidebarUpdateAbortController)
    
    // 更新节点标题
    safeUpdate(selectedNode, book.title)
    
    try {
      // 重置为默认值，以防API调用失败
      safeUpdate(highFrequencyKnowledge, [])
      safeUpdate(relatedNodes, [])
      
      console.log(`Fetching high-frequency knowledge for book ${book.id}: ${book.title}`)
      const knowledgeResponse = await api.get(`/books/${book.id}/high-frequency`, {
        signal: sidebarUpdateAbortController.signal
      })
      
      if (knowledgeResponse && knowledgeResponse.code === 200 && Array.isArray(knowledgeResponse.data)) {
        safeUpdate(highFrequencyKnowledge, knowledgeResponse.data)
        console.log('High-frequency knowledge:', highFrequencyKnowledge.value)
      } else {
        console.warn('获取高频知识点数据不完整:', knowledgeResponse)
      }
      
      console.log(`Fetching related nodes for book ${book.id}: ${book.title}`)
      const relatedResponse = await api.get(`/books/${book.id}/related-nodes`, {
        signal: sidebarUpdateAbortController.signal
      })
      
      if (relatedResponse && relatedResponse.code === 200 && Array.isArray(relatedResponse.data)) {
        safeUpdate(relatedNodes, relatedResponse.data)
        console.log('Related nodes:', relatedNodes.value)
      } else {
        console.warn('获取相关知识点数据不完整:', relatedResponse)
      }
    } catch (error) {
      if (error.message.includes('destroyed')) {
        console.warn('Component destroyed during updateSidebarForBook')
        return // 静默失败
      }
      // 忽略取消错误
      if (error.name !== 'AbortError') {
        console.error('获取书籍侧边栏信息失败:', error)
      }
      // 保持默认值，不影响界面显示
    } finally {
      // 清理AbortController
      if (sidebarUpdateAbortController) {
        componentState.abortControllers.delete(sidebarUpdateAbortController)
        sidebarUpdateAbortController = null
      }
    }
  })
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 切换视图模式
const toggleViewMode = () => {
  viewMode.value = viewMode.value === 'standard' ? 'simplified' : 'standard'
  
  if (!chart) return
  
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    return
  }
  
  const series = option.series[0]
  
  if (viewMode.value === 'simplified') {
    // 简化视图：只显示核心课程和章节模块
    series.data = series.data.map(node => ({
      ...node,
      symbolSize: node.category <= 1 ? (node.category === 0 ? 90 : 70) : 0,
      label: {
        ...node.label,
        show: node.category <= 1,
        fontSize: node.category === 0 ? 18 : 16,
        fontWeight: 'bold'
      },
      itemStyle: {
        ...node.itemStyle,
        opacity: node.category <= 1 ? 1 : 0
      }
    }))
    
    // 隐藏与三级及以下节点相关的连接线
    series.links = series.links.filter(link => {
      const sourceNode = series.data.find(n => n.id === link.source)
      const targetNode = series.data.find(n => n.id === link.target)
      return sourceNode && targetNode && sourceNode.category <= 1 && targetNode.category <= 1
    })
    
    // 调整简化视图的力导向布局参数
    series.force = {
      ...series.force,
      repulsion: 15000,
      edgeLength: [200, 350],
      gravity: 0.01
    }
  } else {
    // 标准视图：显示所有节点
    series.data = series.data.map(node => ({
      ...node,
      symbolSize: node.category === 0 ? 80 : node.category === 1 ? 60 : node.category === 2 ? 45 : 35,
      label: {
        ...node.label,
        show: true,
        fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
        fontWeight: node.category <= 1 ? 'bold' : 'normal'
      },
      itemStyle: {
        ...node.itemStyle,
        opacity: 1
      }
    }))
    
    // 恢复所有连接线
    series.links = currentGraphData.links.map(link => ({
      ...link,
      lineStyle: {
        color: getLinkColor(link.source),
        width: getLinkWidth(link.source),
        curveness: 0.2,
        type: getLinkType(link.source),
        opacity: 0.7
      }
    }))
    
    // 恢复标准视图的力导向布局参数
    series.force = {
      initLayout: 'circular',
      repulsion: 12000,
      edgeLength: [150, 300],
      gravity: 0.02,
      friction: 0.9,
      layoutAnimation: true,
      edgeLengthMin: 100,
      edgeLengthMax: 400
    }
  }
  
  chart.setOption(option)
  showToastMessage(viewMode.value === 'simplified' ? '已切换到简化视图' : '已切换到标准视图')
}

// 切换搜索
const toggleSearch = () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping toggleSearch')
    return
  }
  
  showSearch.value = !showSearch.value
  if (showSearch.value) {
    setTimeout(() => {
      // 检查组件是否已销毁
      if (!componentState.isMounted.value) return
      document.querySelector('.search-input-wrapper input')?.focus()
    }, 100)
  }
}

// 导出PDF
const exportToPDF = async () => {
  // 检查组件是否已销毁
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping exportToPDF')
    return
  }
  if (!chartContainer.value || !chart) return
  
  try {
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    showToastMessage('正在生成PDF，请稍候...')
    
    // 创建一个临时容器来渲染标题和图表
    const tempDiv = document.createElement('div')
    tempDiv.style.cssText = `
      width: 1200px;
      height: 800px;
      background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
      padding: 30px;
      box-sizing: border-box;
      font-family: 'Microsoft YaHei', 'SimHei', sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
    `
    
    // 添加标题
    const titleDiv = document.createElement('div')
    titleDiv.style.cssText = `
      font-size: 32px;
      font-weight: bold;
      color: #1565c0;
      margin-bottom: 30px;
      text-align: center;
      text-shadow: 2px 2px 4px rgba(0,0,0,0.1);
    `
    titleDiv.textContent = '知识图谱 - ' + (selectedNode.value || '高等数学')
    tempDiv.appendChild(titleDiv)
    
    // 添加图表容器
    const chartDiv = document.createElement('div')
    chartDiv.style.cssText = `
      width: 1100px;
      height: 650px;
      background: #e3f2fd;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
    `
    tempDiv.appendChild(chartDiv)
    
    // 检查组件是否已销毁
    if (isDestroyed.value) {
      // 清理
      return
    }
    
    // 临时添加到页面
    tempDiv.style.position = 'fixed'
    tempDiv.style.left = '-9999px'
    tempDiv.style.top = '-9999px'
    document.body.appendChild(tempDiv)
    
    // 初始化临时图表
    const tempChart = echarts.init(chartDiv)
    const option = chart.getOption()
    tempChart.setOption(option)
    
    // 等待渲染完成
    await new Promise(resolve => setTimeout(resolve, 800))
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) {
      // 清理
      tempChart.dispose()
      try {
        document.body.removeChild(tempDiv)
      } catch (error) {
        console.warn('移除临时元素失败:', error)
      }
      return
    }
    
    // 使用 html2canvas 截图
    const canvas = await html2canvas(tempDiv, {
      backgroundColor: null,
      scale: 2,
      useCORS: true,
      allowTaint: true,
      width: 1200,
      height: 800
    })
    
    // 清理
    tempChart.dispose()
    try {
      document.body.removeChild(tempDiv)
    } catch (error) {
      console.warn('移除临时元素失败:', error)
    }
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    // 获取图片数据
    const imgData = canvas.toDataURL('image/png')
    
    // 创建 PDF
    const pdf = new jsPDF({
      orientation: 'landscape',
      unit: 'mm',
      format: 'a4'
    })
    
    const pdfWidth = pdf.internal.pageSize.getWidth()
    const pdfHeight = pdf.internal.pageSize.getHeight()
    
    // 计算图片尺寸以适应页面
    const imgWidth = canvas.width
    const imgHeight = canvas.height
    const ratio = Math.min((pdfWidth - 10) / imgWidth, (pdfHeight - 10) / imgHeight)
    const finalWidth = imgWidth * ratio
    const finalHeight = imgHeight * ratio
    const x = (pdfWidth - finalWidth) / 2
    const y = (pdfHeight - finalHeight) / 2
    
    // 添加图片到 PDF
    pdf.addImage(imgData, 'PNG', x, y, finalWidth, finalHeight)
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    // 下载 PDF
    const fileName = `知识图谱_${selectedNode.value || '高等数学'}_${new Date().toLocaleDateString()}.pdf`
    pdf.save(fileName)
    
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    showToastMessage('PDF导出成功！', 'success')
  } catch (error) {
    console.error('导出PDF失败:', error)
    // 检查组件是否已销毁
    if (componentState.isMounted.value) {
      showToastMessage('导出PDF失败，请重试', 'error')
    }
  }
}

// 关闭搜索
const closeSearch = () => {
  showSearch.value = false
  searchQuery.value = ''
  searchResults.value = []
  searchSuggestions.value = []
  showSearchResults.value = false
  showNoResults.value = false
}

// 处理搜索输入
const handleSearchInput = () => {
  if (!searchQuery.value.trim()) {
    searchSuggestions.value = []
    searchResults.value = []
    showSearchResults.value = false
    showNoResults.value = false
    return
  }
  
  const query = searchQuery.value.toLowerCase()
  
  // 搜索建议
  searchSuggestions.value = currentGraphData.nodes
    .filter(node => node.name.toLowerCase().includes(query))
    .slice(0, 5)
    .map(node => ({
      name: node.name,
      module: getModuleName(node.category),
      level: getLevelName(node.category)
    }))
  
  // 搜索结果
  searchResults.value = currentGraphData.nodes
    .filter(node => node.name.toLowerCase().includes(query))
    .map(node => ({
      name: node.name,
      module: getModuleName(node.category),
      level: getLevelName(node.category),
      priority: '高频'
    }))
  
  showSearchResults.value = true
  showNoResults.value = searchResults.value.length === 0
}

// 获取模块名称
const getModuleName = (category) => {
  const modules = ['核心课程', '章节模块', '核心知识点', '细分知识点']
  return modules[category] || '其他'
}

// 获取层级名称
const getLevelName = (category) => {
  const levels = ['核心', '一级', '二级', '三级']
  return levels[category] || '其他'
}

// 处理搜索失焦
const handleSearchBlur = () => {
  setTimeout(() => {
    // 检查组件是否已卸载或销毁
    if (isMounted.value && !isDestroyed.value && !searchQuery.value.trim()) {
      closeSearch()
    }
  }, 200)
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
  searchSuggestions.value = []
  searchResults.value = []
  showSearchResults.value = false
  showNoResults.value = false
  document.querySelector('.search-input-wrapper input')?.focus()
}

// 搜索历史
const searchByHistory = (item) => {
  searchQuery.value = item
  handleSearchInput()
}

// 选择建议
const selectSuggestion = (item) => {
  searchQuery.value = item.name
  handleSearchInput()
}

// 选择搜索结果
const selectSearchResult = (item) => {
  focusOnNode(item.name)
  closeSearch()
}

// 高亮所有结果
const highlightAllResults = () => {
  if (!chart || searchResults.value.length === 0) return
  
  const resultNames = searchResults.value.map(r => r.name)
  
  const option = chart.getOption()
  const series = option.series[0]
  
  series.data = series.data.map(node => {
    const isMatch = resultNames.includes(node.name)
    return {
      ...node,
      itemStyle: {
        color: isMatch ? '#ff6b6b' : getNodeColor(node.category),
        opacity: isMatch ? 1 : 0.3
      },
      label: {
        ...node.label,
        opacity: isMatch ? 1 : 0.3
      }
    }
  })
  
  chart.setOption(option)
}

// 显示提示


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
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToastMessage('请先选择习题')
    return
  }
  
  // 先关闭习题弹窗
  closeExerciseModal()
  
  // 跳转到练习页面，传递选中的习题数据
  router.push({
    path: '/practice',
    query: { 
      from: 'graph',
      showExerciseModal: 'true',
      multiple: 'true',
      exerciseIds: selected.map(e => e.id).join(',')
    }
  })
}

const addToWrongBook = () => {
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToastMessage('请先选择习题')
    return
  }
  showToastMessage(`已添加 ${selected.length} 道题目到错题本`)
}

const addToPractice = () => {
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToastMessage('请先选择习题')
    return
  }
  showToastMessage(`已添加 ${selected.length} 道题目到练习册`)
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
