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
                        :key="index"
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
                        :key="index"
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
                      :key="index"
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
                      :key="index"
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
                      :key="index"
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
                    <button class="action-btn">反馈补充知识点</button>
                  </div>
                  <div class="related-suggestions" v-if="relatedSuggestions.length > 0">
                    <h5>推荐相关知识点</h5>
                    <div class="related-list">
                      <div 
                        v-for="(item, index) in relatedSuggestions" 
                        :key="index"
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
        <div class="graph-canvas" ref="chartContainer"></div>
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
              :key="index"
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
              <li v-for="(knowledge, index) in highFrequencyKnowledge" :key="index">{{ knowledge }}</li>
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
            <button class="header-btn close-btn" @click="closeExerciseModal">关闭</button>
          </div>
        </div>

        <!-- 难度筛选标签 -->
        <div class="difficulty-tabs">
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'all' }"
            @click="exerciseFilter = 'all'"
          >全部</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'basic' }"
            @click="exerciseFilter = 'basic'"
          >基础巩固</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'advanced' }"
            @click="exerciseFilter = 'advanced'"
          >进阶提升</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'exam' }"
            @click="exerciseFilter = 'exam'"
          >真题冲刺</button>
          <button 
            class="tab-btn" 
            :class="{ active: exerciseFilter === 'competition' }"
            @click="exerciseFilter = 'competition'"
          >竞赛拓展</button>
        </div>

        <!-- 筛选工具栏 -->
        <div class="filter-toolbar">
          <div class="filter-left">
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
              <span class="difficulty-tag" :class="exercise.difficulty">{{ exercise.difficulty }}</span>
            </div>
            <div class="card-body">
              <p class="exercise-desc">{{ exercise.description }}</p>
            </div>
            <div class="card-footer">
              <button class="card-btn" @click.stop="startSingleExercise(exercise)">开始练习</button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, defineOptions } from 'vue'

// 为组件指定名称
defineOptions({
  name: 'KnowledgeGraph'
})
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

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
const exercises = ref([
  { id: 1, title: '线性代数', description: '矩阵的特征值与特征向量计算', difficulty: '基础巩固', selected: false },
  { id: 2, title: '概率论', description: '条件概率与贝叶斯公式应用', difficulty: '基础巩固', selected: false },
  { id: 3, title: '微积分', description: '定积分的换元法与分部积分', difficulty: '进阶提升', selected: false },
  { id: 4, title: '矩阵运算', description: '矩阵乘法与逆矩阵求解', difficulty: '基础巩固', selected: false },
  { id: 5, title: '随机变量', description: '离散型随机变量的分布列', difficulty: '进阶提升', selected: false },
  { id: 6, title: '极限计算', description: '各种未定式的极限求解方法', difficulty: '真题冲刺', selected: false },
  { id: 7, title: '行列式', description: '行列式的性质与计算方法', difficulty: '基础巩固', selected: false },
  { id: 8, title: '期望方差', description: '随机变量的数字特征计算', difficulty: '进阶提升', selected: false },
  { id: 9, title: '导数应用', description: '函数的单调性与极值问题', difficulty: '真题冲刺', selected: false },
  { id: 10, title: '向量空间', description: '线性相关性与基的概念', difficulty: '竞赛拓展', selected: false },
  { id: 11, title: '假设检验', description: '正态总体参数的假设检验', difficulty: '竞赛拓展', selected: false },
  { id: 12, title: '微分方程', description: '一阶线性微分方程求解', difficulty: '进阶提升', selected: false }
])

// 模拟书架数据
const bookshelfBooks = ref([
  { id: 1, title: '高等数学', author: '同济大学数学系' },
  { id: 2, title: '线性代数', author: '同济大学数学系' },
  { id: 3, title: '概率论与数理统计', author: '浙江大学' },
  { id: 4, title: '大学物理', author: '张三' },
  { id: 5, title: '计算机基础', author: '李四' }
])

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
  '高等数学': ['微积分', '线性代数', '概率论', '常微分方程'],
  '微积分': ['导数', '积分', '极限', '定积分'],
  '线性代数': ['矩阵', '行列式', '特征值', '特征向量'],
  '概率论': ['随机变量', '概率分布', '期望', '方差'],
  '常微分方程': ['一阶微分方程', '二阶微分方程', '齐次方程', '非齐次方程'],
  '导数': ['极限', '微积分'],
  '积分': ['定积分', '微积分'],
  '矩阵': ['特征值', '特征向量', '线性代数'],
  '行列式': ['线性代数'],
  '随机变量': ['期望', '方差', '概率论'],
  '概率分布': ['概率论'],
  '一阶微分方程': ['齐次方程', '常微分方程'],
  '二阶微分方程': ['非齐次方程', '常微分方程']
}

// 节点高频知识点映射
const nodeHighFrequencyKnowledge = {
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
  ]
}

// 初始化 ECharts
const initChart = () => {
  if (!chartContainer.value) return
  
  // 确保容器有正确的大小
  chartContainer.value.style.width = '100%'
  chartContainer.value.style.height = '100%'
  
  // 初始化节点位置
  initNodePositions()
  
  chart = echarts.init(chartContainer.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        // 只显示节点的提示，不显示连接线的提示
        if (params.dataType === 'node') {
          return params.name
        }
        return ''
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
            color: getNodeColor(node.category)
          },
          label: {
            show: true,
            position: 'inside',
            fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
            fontWeight: node.category <= 1 ? 'bold' : 'normal',
            color: '#fff'
          }
        })),
        links: currentGraphData.links.map(link => ({
          ...link,
          lineStyle: {
            color: getLinkColor(link.source),
            width: getLinkWidth(link.source),
            curveness: 0.2,
            type: getLinkType(link.source)
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
        dragMode: ['pan'],
        focusNodeAdjacency: false,
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
          repulsion: 8000,
          edgeLength: [200, 400],
          gravity: 0.01,
          friction: 0.8,
          layoutAnimation: true
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
}

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
  showToast(`已定位到节点：${nodeName}`)
}

// 显示所有节点
const showAllNodes = () => {
  clearHighlights()
  closeSearch()
  showToast('已显示所有知识点')
}

// 切换书籍选择器
const toggleBookSelector = () => {
  showBookSelector.value = !showBookSelector.value
}

// 处理书籍搜索
const handleBookSearch = () => {
  if (!bookSearchQuery.value.trim()) {
    bookSearchResults.value = []
    return
  }
  
  // 模拟搜索结果
  bookSearchResults.value = [
    { id: 6, title: '高等数学（上册）', author: '同济大学数学系' },
    { id: 7, title: '高等数学（下册）', author: '同济大学数学系' },
    { id: 8, title: '线性代数辅导', author: '张三' },
    { id: 9, title: '概率论与数理统计辅导', author: '李四' }
  ].filter(book => 
    book.title.includes(bookSearchQuery.value) || 
    book.author.includes(bookSearchQuery.value)
  )
}

// 选择书籍
const selectBook = (book) => {
  currentBook.value = book
  selectedNode.value = book.title
  showBookSelector.value = false
  
  // 这里可以添加调用后端API获取书籍章节数据的逻辑
  // 例如：fetchBookChapters(book.id).then(data => updateGraph(data))
  
  // 模拟更新图谱数据
  updateGraphForBook(book)
}

// 不同书籍的图谱数据
const bookGraphData = {
  '高等数学': {
    nodes: [
      { id: '0', name: '高等数学', category: 0, symbolSize: 80, value: 100 },
      { id: '1', name: '微积分', category: 1, symbolSize: 60, value: 80 },
      { id: '2', name: '线性代数', category: 1, symbolSize: 60, value: 80 },
      { id: '3', name: '概率论', category: 1, symbolSize: 60, value: 80 },
      { id: '4', name: '常微分方程', category: 1, symbolSize: 60, value: 80 },
      { id: '5', name: '导数', category: 2, symbolSize: 45, value: 60 },
      { id: '6', name: '积分', category: 2, symbolSize: 45, value: 60 },
      { id: '7', name: '矩阵', category: 2, symbolSize: 45, value: 60 },
      { id: '8', name: '行列式', category: 2, symbolSize: 45, value: 60 },
      { id: '9', name: '随机变量', category: 2, symbolSize: 45, value: 60 },
      { id: '10', name: '概率分布', category: 2, symbolSize: 45, value: 60 },
      { id: '11', name: '一阶微分方程', category: 2, symbolSize: 45, value: 60 },
      { id: '12', name: '二阶微分方程', category: 2, symbolSize: 45, value: 60 },
      { id: '13', name: '极限', category: 3, symbolSize: 35, value: 40 },
      { id: '14', name: '定积分', category: 3, symbolSize: 35, value: 40 },
      { id: '15', name: '特征值', category: 3, symbolSize: 35, value: 40 },
      { id: '16', name: '特征向量', category: 3, symbolSize: 35, value: 40 },
      { id: '17', name: '期望', category: 3, symbolSize: 35, value: 40 },
      { id: '18', name: '方差', category: 3, symbolSize: 35, value: 40 },
      { id: '19', name: '齐次方程', category: 3, symbolSize: 35, value: 40 },
      { id: '20', name: '非齐次方程', category: 3, symbolSize: 35, value: 40 }
    ],
    links: [
      { source: '0', target: '1' },
      { source: '0', target: '2' },
      { source: '0', target: '3' },
      { source: '0', target: '4' },
      { source: '1', target: '5' },
      { source: '1', target: '6' },
      { source: '2', target: '7' },
      { source: '2', target: '8' },
      { source: '3', target: '9' },
      { source: '3', target: '10' },
      { source: '4', target: '11' },
      { source: '4', target: '12' },
      { source: '5', target: '13' },
      { source: '6', target: '14' },
      { source: '7', target: '15' },
      { source: '7', target: '16' },
      { source: '9', target: '17' },
      { source: '9', target: '18' },
      { source: '11', target: '19' },
      { source: '12', target: '20' }
    ]
  },
  '线性代数': {
    nodes: [
      { id: '0', name: '线性代数', category: 0, symbolSize: 80, value: 100 },
      { id: '1', name: '矩阵运算', category: 1, symbolSize: 60, value: 80 },
      { id: '2', name: '向量空间', category: 1, symbolSize: 60, value: 80 },
      { id: '3', name: '线性变换', category: 1, symbolSize: 60, value: 80 },
      { id: '4', name: '特征系统', category: 1, symbolSize: 60, value: 80 },
      { id: '5', name: '矩阵加法', category: 2, symbolSize: 45, value: 60 },
      { id: '6', name: '矩阵乘法', category: 2, symbolSize: 45, value: 60 },
      { id: '7', name: '逆矩阵', category: 2, symbolSize: 45, value: 60 },
      { id: '8', name: '行列式', category: 2, symbolSize: 45, value: 60 },
      { id: '9', name: '基与维数', category: 2, symbolSize: 45, value: 60 },
      { id: '10', name: '子空间', category: 2, symbolSize: 45, value: 60 },
      { id: '11', name: '特征值', category: 2, symbolSize: 45, value: 60 },
      { id: '12', name: '特征向量', category: 2, symbolSize: 45, value: 60 },
      { id: '13', name: '对角化', category: 3, symbolSize: 35, value: 40 },
      { id: '14', name: '正交化', category: 3, symbolSize: 35, value: 40 },
      { id: '15', name: '秩', category: 3, symbolSize: 35, value: 40 },
      { id: '16', name: '零空间', category: 3, symbolSize: 35, value: 40 }
    ],
    links: [
      { source: '0', target: '1' },
      { source: '0', target: '2' },
      { source: '0', target: '3' },
      { source: '0', target: '4' },
      { source: '1', target: '5' },
      { source: '1', target: '6' },
      { source: '1', target: '7' },
      { source: '1', target: '8' },
      { source: '2', target: '9' },
      { source: '2', target: '10' },
      { source: '4', target: '11' },
      { source: '4', target: '12' },
      { source: '4', target: '13' },
      { source: '2', target: '14' },
      { source: '1', target: '15' },
      { source: '2', target: '16' }
    ]
  },
  '概率论与数理统计': {
    nodes: [
      { id: '0', name: '概率论与数理统计', category: 0, symbolSize: 80, value: 100 },
      { id: '1', name: '概率基础', category: 1, symbolSize: 60, value: 80 },
      { id: '2', name: '随机变量', category: 1, symbolSize: 60, value: 80 },
      { id: '3', name: '分布理论', category: 1, symbolSize: 60, value: 80 },
      { id: '4', name: '统计推断', category: 1, symbolSize: 60, value: 80 },
      { id: '5', name: '条件概率', category: 2, symbolSize: 45, value: 60 },
      { id: '6', name: '贝叶斯定理', category: 2, symbolSize: 45, value: 60 },
      { id: '7', name: '离散变量', category: 2, symbolSize: 45, value: 60 },
      { id: '8', name: '连续变量', category: 2, symbolSize: 45, value: 60 },
      { id: '9', name: '正态分布', category: 2, symbolSize: 45, value: 60 },
      { id: '10', name: '泊松分布', category: 2, symbolSize: 45, value: 60 },
      { id: '11', name: '参数估计', category: 2, symbolSize: 45, value: 60 },
      { id: '12', name: '假设检验', category: 2, symbolSize: 45, value: 60 },
      { id: '13', name: '期望', category: 3, symbolSize: 35, value: 40 },
      { id: '14', name: '方差', category: 3, symbolSize: 35, value: 40 },
      { id: '15', name: '协方差', category: 3, symbolSize: 35, value: 40 },
      { id: '16', name: '相关系数', category: 3, symbolSize: 35, value: 40 }
    ],
    links: [
      { source: '0', target: '1' },
      { source: '0', target: '2' },
      { source: '0', target: '3' },
      { source: '0', target: '4' },
      { source: '1', target: '5' },
      { source: '1', target: '6' },
      { source: '2', target: '7' },
      { source: '2', target: '8' },
      { source: '3', target: '9' },
      { source: '3', target: '10' },
      { source: '4', target: '11' },
      { source: '4', target: '12' },
      { source: '2', target: '13' },
      { source: '2', target: '14' },
      { source: '2', target: '15' },
      { source: '2', target: '16' }
    ]
  },
  '大学物理': {
    nodes: [
      { id: '0', name: '大学物理', category: 0, symbolSize: 80, value: 100 },
      { id: '1', name: '力学', category: 1, symbolSize: 60, value: 80 },
      { id: '2', name: '电磁学', category: 1, symbolSize: 60, value: 80 },
      { id: '3', name: '热学', category: 1, symbolSize: 60, value: 80 },
      { id: '4', name: '光学', category: 1, symbolSize: 60, value: 80 },
      { id: '5', name: '牛顿定律', category: 2, symbolSize: 45, value: 60 },
      { id: '6', name: '动量守恒', category: 2, symbolSize: 45, value: 60 },
      { id: '7', name: '库仑定律', category: 2, symbolSize: 45, value: 60 },
      { id: '8', name: '电磁感应', category: 2, symbolSize: 45, value: 60 },
      { id: '9', name: '热力学定律', category: 2, symbolSize: 45, value: 60 },
      { id: '10', name: '分子运动', category: 2, symbolSize: 45, value: 60 },
      { id: '11', name: '光的干涉', category: 2, symbolSize: 45, value: 60 },
      { id: '12', name: '光的衍射', category: 2, symbolSize: 45, value: 60 },
      { id: '13', name: '功和能', category: 3, symbolSize: 35, value: 40 },
      { id: '14', name: '圆周运动', category: 3, symbolSize: 35, value: 40 },
      { id: '15', name: '电场', category: 3, symbolSize: 35, value: 40 },
      { id: '16', name: '磁场', category: 3, symbolSize: 35, value: 40 }
    ],
    links: [
      { source: '0', target: '1' },
      { source: '0', target: '2' },
      { source: '0', target: '3' },
      { source: '0', target: '4' },
      { source: '1', target: '5' },
      { source: '1', target: '6' },
      { source: '2', target: '7' },
      { source: '2', target: '8' },
      { source: '3', target: '9' },
      { source: '3', target: '10' },
      { source: '4', target: '11' },
      { source: '4', target: '12' },
      { source: '1', target: '13' },
      { source: '1', target: '14' },
      { source: '2', target: '15' },
      { source: '2', target: '16' }
    ]
  },
  '计算机基础': {
    nodes: [
      { id: '0', name: '计算机基础', category: 0, symbolSize: 80, value: 100 },
      { id: '1', name: '计算机组成', category: 1, symbolSize: 60, value: 80 },
      { id: '2', name: '操作系统', category: 1, symbolSize: 60, value: 80 },
      { id: '3', name: '数据结构', category: 1, symbolSize: 60, value: 80 },
      { id: '4', name: '计算机网络', category: 1, symbolSize: 60, value: 80 },
      { id: '5', name: 'CPU', category: 2, symbolSize: 45, value: 60 },
      { id: '6', name: '内存', category: 2, symbolSize: 45, value: 60 },
      { id: '7', name: '进程管理', category: 2, symbolSize: 45, value: 60 },
      { id: '8', name: '文件系统', category: 2, symbolSize: 45, value: 60 },
      { id: '9', name: '数组', category: 2, symbolSize: 45, value: 60 },
      { id: '10', name: '链表', category: 2, symbolSize: 45, value: 60 },
      { id: '11', name: 'TCP/IP', category: 2, symbolSize: 45, value: 60 },
      { id: '12', name: 'HTTP', category: 2, symbolSize: 45, value: 60 },
      { id: '13', name: '算法', category: 3, symbolSize: 35, value: 40 },
      { id: '14', name: '排序', category: 3, symbolSize: 35, value: 40 },
      { id: '15', name: '数据库', category: 3, symbolSize: 35, value: 40 },
      { id: '16', name: '编程语言', category: 3, symbolSize: 35, value: 40 }
    ],
    links: [
      { source: '0', target: '1' },
      { source: '0', target: '2' },
      { source: '0', target: '3' },
      { source: '0', target: '4' },
      { source: '1', target: '5' },
      { source: '1', target: '6' },
      { source: '2', target: '7' },
      { source: '2', target: '8' },
      { source: '3', target: '9' },
      { source: '3', target: '10' },
      { source: '4', target: '11' },
      { source: '4', target: '12' },
      { source: '3', target: '13' },
      { source: '3', target: '14' },
      { source: '4', target: '15' },
      { source: '3', target: '16' }
    ]
  }
}

// 当前图谱数据
let currentGraphData = JSON.parse(JSON.stringify(bookGraphData['高等数学']))

// 根据书籍更新图谱
const updateGraphForBook = (book) => {
  if (!chart) return
  
  // 获取对应书籍的图谱数据
  const bookData = bookGraphData[book.title]
  if (!bookData) {
    console.warn('未找到书籍的图谱数据:', book.title)
    return
  }
  
  // 更新当前图谱数据
  currentGraphData = JSON.parse(JSON.stringify(bookData))
  
  // 初始化节点位置
  initNodePositionsForBook(currentGraphData)
  
  // 更新右侧边栏信息
  updateSidebarForBook(book)
  
  // 更新图表
  const option = chart.getOption()
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
  showToast(`已切换到《${book.title}》知识图谱`)
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
    const baseAngle = Math.atan2(parentNode.y - centerY, parentNode.x - centerX)
    
    nodes.forEach((node, index) => {
      const offset = (index - (nodes.length - 1) / 2) * 0.5
      const angle = baseAngle + offset
      node.x = centerX + level2Radius * Math.cos(angle)
      node.y = centerY + level2Radius * Math.sin(angle)
    })
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
    const baseAngle = Math.atan2(parentNode.y - centerY, parentNode.x - centerX)
    
    nodes.forEach((node, index) => {
      const offset = (index - (nodes.length - 1) / 2) * 0.4
      const angle = baseAngle + offset
      node.x = centerX + level3Radius * Math.cos(angle)
      node.y = centerY + level3Radius * Math.sin(angle)
    })
  })
}

// 根据书籍更新侧边栏信息
const updateSidebarForBook = (book) => {
  // 更新节点标题
  selectedNode.value = book.title
  
  // 根据书籍更新高频知识点
  const knowledgeMap = {
    '高等数学': ['微积分基本定理', '牛顿-莱布尼茨公式', '定积分的几何意义', '换元积分法', '分部积分法'],
    '线性代数': ['矩阵运算规则', '行列式计算', '特征值求解', '向量空间', '线性变换'],
    '概率论与数理统计': ['全概率公式', '贝叶斯公式', '正态分布', '中心极限定理', '假设检验'],
    '大学物理': ['牛顿运动定律', '能量守恒', '电磁感应定律', '热力学定律', '光的波动性'],
    '计算机基础': ['冯诺依曼结构', '进程与线程', '时间复杂度', '网络协议', '数据存储']
  }
  
  highFrequencyKnowledge.value = knowledgeMap[book.title] || knowledgeMap['高等数学']
  
  // 更新关联节点
  const relatedMap = {
    '高等数学': ['微积分', '线性代数', '概率论', '常微分方程'],
    '线性代数': ['矩阵', '向量', '行列式', '特征值'],
    '概率论与数理统计': ['随机变量', '概率分布', '统计推断', '回归分析'],
    '大学物理': ['力学', '电磁学', '热学', '光学'],
    '计算机基础': ['算法', '数据结构', '操作系统', '网络']
  }
  
  relatedNodes.value = relatedMap[book.title] || relatedMap['高等数学']
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
  const series = option.series[0]
  
  if (viewMode.value === 'simplified') {
    // 简化视图：隐藏三级节点
    series.data = series.data.map(node => ({
      ...node,
      symbolSize: node.category >= 3 ? 0 : node.symbolSize,
      label: {
        ...node.label,
        show: node.category < 3
      }
    }))
  } else {
    // 标准视图：显示所有节点
    series.data = series.data.map(node => ({
      ...node,
      symbolSize: node.category === 0 ? 80 : node.category === 1 ? 60 : node.category === 2 ? 45 : 35,
      label: {
        ...node.label,
        show: true
      }
    }))
  }
  
  chart.setOption(option)
}

// 切换搜索
const toggleSearch = () => {
  showSearch.value = !showSearch.value
  if (showSearch.value) {
    setTimeout(() => {
      document.querySelector('.search-input-wrapper input')?.focus()
    }, 100)
  }
}

// 导出PDF
const exportToPDF = async () => {
  if (!chartContainer.value || !chart) return
  
  try {
    showToast('正在生成PDF，请稍候...')
    
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
    document.body.removeChild(tempDiv)
    
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
    
    // 下载 PDF
    const fileName = `知识图谱_${selectedNode.value || '高等数学'}_${new Date().toLocaleDateString()}.pdf`
    pdf.save(fileName)
    
    showToast('PDF导出成功！')
  } catch (error) {
    console.error('导出PDF失败:', error)
    showToast('导出PDF失败，请重试')
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
    if (!searchQuery.value.trim()) {
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
const showToast = (message) => {
  // 简单的提示实现
  console.log(message)
}

// 处理窗口大小变化
const handleResize = () => {
  chart?.resize()
}

// 初始化
onMounted(() => {
  initChart()
  
  // 检查路由参数，是否需要打开习题弹窗
  if (route.query.showExerciseModal === 'true') {
    showExerciseModal.value = true
  }
})

// 清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})

// 习题弹窗相关方法
const openExerciseModal = () => {
  showExerciseModal.value = true
}

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

const startSingleExercise = (exercise) => {
  // 先关闭习题弹窗
  closeExerciseModal()
  
  // 跳转到练习页面，传递单个习题数据
  router.push({
    path: '/practice',
    query: { 
      from: 'graph',
      showExerciseModal: 'true',
      single: 'true',
      exerciseId: exercise.id
    }
  })
}

const startExercise = () => {
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToast('请先选择习题')
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
    showToast('请先选择习题')
    return
  }
  showToast(`已添加 ${selected.length} 道题目到错题本`)
}

const addToPractice = () => {
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToast('请先选择习题')
    return
  }
  showToast(`已添加 ${selected.length} 道题目到练习册`)
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
  top: 60px;
  left: 50%;
  transform: translateX(-50%) scale(0.95);
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
    transform: translateX(-50%) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) scale(1);
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
  align-items: center;
  margin-bottom: 10px;
}

.exercise-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.difficulty-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
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

.card-body {
  margin-bottom: 12px;
}

.exercise-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
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
</style>
