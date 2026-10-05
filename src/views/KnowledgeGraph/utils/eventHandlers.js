// 事件处理模块

import { useRouter } from 'vue-router'
import api, { apiService } from '@/api/api'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { generateDefaultGraphData, getRelatedKnowledgePoints } from './dataProcessor'
import { loadLearningProgress, calculateKnowledgeMastery, analyzeLearningBehavior, recommendExercises } from './learningProgress'
import { updateChart, toggleViewMode, highlightRelatedNodes, clearHighlights, highlightSearchResults } from './chartConfig'

const router = useRouter()

/**
 * 显示提示消息
 * @param {string} message - 消息内容
 * @param {string} type - 消息类型
 * @param {number} duration - 持续时间
 */
export const showToastMessage = (message, type = 'info', duration = 3000) => {
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
}

/**
 * 返回首页
 * @param {Object} router - Vue Router 实例
 */
export const goToHome = (router) => {
  if (router) {
    router.push('/')
  } else {
    console.error('Router instance is not provided')
  }
}

/**
 * 处理节点点击
 * @param {string} nodeName - 节点名称
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Function} updateSidebarContent - 更新侧边栏内容的函数
 * @param {Object} chart - 图表实例
 * @param {Object} currentGraphData - 当前图谱数据
 */
export const handleNodeClick = (nodeName, selectedNode, updateSidebarContent, chart, currentGraphData) => {
  selectedNode.value = nodeName
  
  // 更新侧边栏
  updateSidebarContent(nodeName)
  
  // 高亮关联节点
  highlightRelatedNodes(chart, nodeName, currentGraphData)
}

/**
 * 切换书籍选择器
 * @param {Object} showBookSelector - 书籍选择器显示状态的ref
 */
export const toggleBookSelector = (showBookSelector) => {
  showBookSelector.value = !showBookSelector.value
}

/**
 * 处理书籍搜索
 * @param {Object} bookSearchQuery - 书籍搜索查询的ref
 * @param {Object} bookSearchResults - 书籍搜索结果的ref
 * @param {Object} componentState - 组件状态
 */
export const handleBookSearch = async (bookSearchQuery, bookSearchResults, componentState) => {
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

/**
 * 选择书籍
 * @param {Object} book - 书籍对象
 * @param {Object} currentBook - 当前书籍的ref
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Object} showBookSelector - 书籍选择器显示状态的ref
 * @param {Object} isLoading - 加载状态的ref
 * @param {Object} currentGraphData - 当前图谱数据的ref
 * @param {Object} chart - 图表实例
 * @param {Function} updateSidebarForBook - 更新书籍侧边栏的函数
 * @param {Object} componentState - 组件状态
 * @param {Function} handleNodeClick - 节点点击处理函数
 */
export const selectBook = async (book, currentBook, selectedNode, showBookSelector, isLoading, currentGraphData, chart, updateSidebarForBook, componentState, handleNodeClick) => {
  if (!componentState.isMounted.value || isLoading.value) {
    return
  }

  isLoading.value = true
  
  let controller = null
  
  try {
    // 使用AbortController
    controller = new AbortController()
    componentState.abortControllers.add(controller)
    
    currentBook.value = book
    selectedNode.value = book.title
    showBookSelector.value = false
    
    try {
      // 传递signal到API调用
      const response = await apiService.knowledgeGraph.getKnowledgeGraphByBook(book.id, controller.signal)
      
      // 再次检查组件状态
      if (!componentState.isMounted.value) return
      
      if (response && response.code === 200 && response.data && response.data.nodes && response.data.relations && response.data.nodes.length > 0) {
          // 直接使用返回的数据，不检查是否是基础结构
          // 使用返回的数据
          console.log('后端返回的节点数据:', response.data.nodes)
          console.log('后端返回的关系数据:', response.data.relations)
          
          currentGraphData.value = {
            nodes: response.data.nodes.map(node => ({
              id: node.id,
              name: node.name,
              category: node.category || 0,
              value: node.value || 50,
              symbolSize: node.symbolSize || 40
            })),
            links: response.data.relations.map(relation => ({
              source: relation.sourceNodeId,
              target: relation.targetNodeId
            }))
          }
          
          console.log('转换后的links数据:', currentGraphData.value.links)
          
          // 更新图表和侧边栏
          updateChart(chart, currentGraphData.value, handleNodeClick)
          updateSidebarForBook(book)
          showToastMessage(`成功获取《${book.title}》知识图谱数据`, 'success')
      } else {
        // 处理空数据情况，生成默认知识图谱数据
        console.warn('获取知识图谱数据不完整，使用默认数据:', response)
        currentGraphData.value = generateDefaultGraphData(book.title)
        showToastMessage('获取知识图谱数据不完整，显示默认图谱', 'warning')
        
        // 更新图表和侧边栏
        updateChart(chart, currentGraphData.value, handleNodeClick)
        updateSidebarForBook(book)
      }
    } catch (error) {
      // 忽略"abort"错误
      if (error.name !== 'AbortError') {
        console.error('获取知识图谱失败:', error)
        // 使用默认数据继续，避免页面崩溃
        currentGraphData.value = generateDefaultGraphData(book.title)
        showToastMessage(`获取知识图谱失败: ${error.message || '服务器内部错误'}，显示默认图谱`, 'error')
        
        // 更新图表和侧边栏
        updateChart(chart, currentGraphData.value, handleNodeClick)
        updateSidebarForBook(book)
      }
    }
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
      isLoading.value = false
    }
  }
}

/**
 * 切换视图模式
 * @param {Object} viewMode - 视图模式的ref
 * @param {Object} chart - 图表实例
 * @param {Object} currentGraphData - 当前图谱数据
 */
export const handleToggleViewMode = (viewMode, chart, currentGraphData) => {
  viewMode.value = viewMode.value === 'standard' ? 'simplified' : 'standard'
  
  if (!chart) return
  
  toggleViewMode(chart, viewMode.value, currentGraphData.value)
  showToastMessage(viewMode.value === 'simplified' ? '已切换到简化视图' : '已切换到标准视图')
}

/**
 * 切换搜索
 * @param {Object} showSearch - 搜索显示状态的ref
 * @param {Object} componentState - 组件状态
 */
export const toggleSearch = (showSearch, componentState) => {
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

/**
 * 关闭搜索
 * @param {Object} showSearch - 搜索显示状态的ref
 * @param {Object} searchQuery - 搜索查询的ref
 * @param {Object} searchResults - 搜索结果的ref
 * @param {Object} searchSuggestions - 搜索建议的ref
 * @param {Object} showSearchResults - 搜索结果显示状态的ref
 * @param {Object} showNoResults - 无结果显示状态的ref
 */
export const closeSearch = (showSearch, searchQuery, searchResults, searchSuggestions, showSearchResults, showNoResults) => {
  showSearch.value = false
  searchQuery.value = ''
  searchResults.value = []
  searchSuggestions.value = []
  showSearchResults.value = false
  showNoResults.value = false
}

/**
 * 处理搜索输入
 * @param {Object} searchQuery - 搜索查询的ref
 * @param {Object} searchSuggestions - 搜索建议的ref
 * @param {Object} searchResults - 搜索结果的ref
 * @param {Object} showSearchResults - 搜索结果显示状态的ref
 * @param {Object} showNoResults - 无结果显示状态的ref
 * @param {Object} currentGraphData - 当前图谱数据
 * @param {Function} getModuleName - 获取模块名称的函数
 * @param {Function} getLevelName - 获取层级名称的函数
 */
export const handleSearchInput = (searchQuery, searchSuggestions, searchResults, showSearchResults, showNoResults, currentGraphData, getModuleName, getLevelName) => {
  if (!searchQuery.value.trim()) {
    searchSuggestions.value = []
    searchResults.value = []
    showSearchResults.value = false
    showNoResults.value = false
    return
  }
  
  const query = searchQuery.value.toLowerCase()
  
  // 防御性检查
  if (!currentGraphData || !currentGraphData.value || !currentGraphData.value.nodes) {
    searchSuggestions.value = []
    searchResults.value = []
    showSearchResults.value = true
    showNoResults.value = true
    return
  }
  
  // 搜索建议
  searchSuggestions.value = currentGraphData.value.nodes
    .filter(node => node.name.toLowerCase().includes(query))
    .slice(0, 5)
    .map(node => ({
      name: node.name,
      module: getModuleName(node.category),
      level: getLevelName(node.category)
    }))
  
  // 搜索结果
  searchResults.value = currentGraphData.value.nodes
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

/**
 * 处理搜索失焦
 * @param {Object} componentState - 组件状态
 * @param {Object} searchQuery - 搜索查询的ref
 * @param {Function} closeSearch - 关闭搜索的函数
 */
export const handleSearchBlur = (componentState, searchQuery, closeSearch) => {
  setTimeout(() => {
    // 检查组件是否已卸载或销毁
    if (componentState.isMounted.value && !componentState.isDestroyed.value && !searchQuery.value.trim()) {
      closeSearch()
    }
  }, 200)
}

/**
 * 清除搜索
 * @param {Object} searchQuery - 搜索查询的ref
 * @param {Object} searchSuggestions - 搜索建议的ref
 * @param {Object} searchResults - 搜索结果的ref
 * @param {Object} showSearchResults - 搜索结果显示状态的ref
 * @param {Object} showNoResults - 无结果显示状态的ref
 */
export const clearSearch = (searchQuery, searchSuggestions, searchResults, showSearchResults, showNoResults) => {
  searchQuery.value = ''
  searchSuggestions.value = []
  searchResults.value = []
  showSearchResults.value = false
  showNoResults.value = false
  document.querySelector('.search-input-wrapper input')?.focus()
}

/**
 * 搜索历史
 * @param {string} item - 历史搜索项
 * @param {Object} searchQuery - 搜索查询的ref
 * @param {Function} handleSearchInput - 处理搜索输入的函数
 */
export const searchByHistory = (item, searchQuery, handleSearchInput) => {
  searchQuery.value = item
  handleSearchInput()
}

/**
 * 选择建议
 * @param {Object} item - 建议项
 * @param {Object} searchQuery - 搜索查询的ref
 * @param {Function} handleSearchInput - 处理搜索输入的函数
 */
export const selectSuggestion = (item, searchQuery, handleSearchInput) => {
  searchQuery.value = item.name
  handleSearchInput()
}

/**
 * 选择搜索结果
 * @param {Object} item - 搜索结果项
 * @param {Function} focusOnNode - 聚焦到节点的函数
 * @param {Function} closeSearch - 关闭搜索的函数
 */
export const selectSearchResult = (item, focusOnNode, closeSearch) => {
  focusOnNode(item.name)
  closeSearch()
}

/**
 * 高亮所有结果
 * @param {Object} chart - 图表实例
 * @param {Object} searchResults - 搜索结果的ref
 */
export const highlightAllResults = (chart, searchResults) => {
  if (!chart || searchResults.value.length === 0) return
  
  highlightSearchResults(chart, searchResults.value)
}

/**
 * 显示所有节点
 * @param {Function} clearHighlights - 清除高亮的函数
 * @param {Function} closeSearch - 关闭搜索的函数
 */
export const showAllNodes = (clearHighlights, closeSearch) => {
  clearHighlights()
  closeSearch()
  showToastMessage('已显示所有知识点')
}

/**
 * 聚焦到节点
 * @param {string} nodeName - 节点名称
 * @param {Function} handleNodeClick - 处理节点点击的函数
 */
export const focusOnNode = (nodeName, handleNodeClick) => {
  handleNodeClick(nodeName)
  showToastMessage(`已定位到节点：${nodeName}`)
}

/**
 * 导出PDF
 * @param {Object} chartContainer - 图表容器的ref
 * @param {Object} chart - 图表实例
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Object} componentState - 组件状态
 */
export const exportToPDF = async (chartContainer, chart, selectedNode, componentState) => {
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping exportToPDF')
    return
  }
  if (!chartContainer.value || !chart) return
  
  try {
    // 检查组件是否已销毁
    if (!componentState.isMounted.value) return
    
    showToastMessage('正在生成PDF，请稍候...')
    const [{ default: html2canvas }, jspdfModule] = await Promise.all([
      import('html2canvas'),
      import('jspdf')
    ])
    const jsPDF = jspdfModule.jsPDF || jspdfModule.default
    
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
    if (componentState.isDestroyed.value) {
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

/**
 * 打开习题弹窗
 * @param {Object} showExerciseModal - 习题弹窗显示状态的ref
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Function} fetchExercises - 获取习题的函数
 * @param {Object} componentState - 组件状态
 */
export const openExerciseModal = async (showExerciseModal, selectedNode, fetchExercises, componentState) => {
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping openExerciseModal')
    return
  }
  
  if (!selectedNode.value) {
    console.warn('No node selected, cannot open exercise modal')
    showToastMessage('请先选择一个知识节点', 'warning')
    return
  }
  
  showExerciseModal.value = true
  await fetchExercises(selectedNode.value)
}

/**
 * 切换习题难度筛选
 * @param {string} filter - 筛选条件
 * @param {Object} exerciseFilter - 习题难度筛选的ref
 * @param {Function} fetchExercises - 获取习题的函数
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Object} componentState - 组件状态
 */
export const changeExerciseFilter = async (filter, exerciseFilter, fetchExercises, selectedNode, componentState) => {
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping changeExerciseFilter')
    return
  }
  exerciseFilter.value = filter
  await fetchExercises(selectedNode.value)
}

/**
 * 切换习题类型筛选
 * @param {string} filter - 筛选条件
 * @param {Object} exerciseTypeFilter - 习题类型筛选的ref
 * @param {Function} fetchExercises - 获取习题的函数
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Object} componentState - 组件状态
 */
export const changeExerciseTypeFilter = async (filter, exerciseTypeFilter, fetchExercises, selectedNode, componentState) => {
  if (!componentState.isMounted.value) {
    console.warn('Component destroyed, skipping changeExerciseTypeFilter')
    return
  }
  exerciseTypeFilter.value = filter
  await fetchExercises(selectedNode.value)
}

/**
 * 开始单个习题练习
 * @param {Object} exercise - 习题对象
 * @param {Object} router - Vue Router 实例
 */
export const startSingleExercise = (exercise, router) => {
  // 跳转到练习页面，传递单个习题数据
  if (router) {
    router.push({
      path: '/practice',
      query: {
        from: 'graph',
        showExerciseModal: 'true',
        multiple: 'false',
        exerciseId: exercise.id
      }
    })
  } else {
    console.error('Router instance is not provided')
    showToastMessage('跳转失败，请重试', 'error')
  }
}

/**
 * 开始练习
 * @param {Object} exercises - 习题列表的ref
 * @param {Function} closeExerciseModal - 关闭习题弹窗的函数
 * @param {Object} router - Vue Router 实例
 */
export const startExercise = (exercises, closeExerciseModal, router) => {
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToastMessage('请先选择习题')
    return
  }

  // 先关闭习题弹窗
  closeExerciseModal()

  // 跳转到练习页面，传递选中的习题数据
  if (router) {
    // 使用 sessionStorage 存储习题数据，避免 URL 过长导致 431 错误
    const exercisesKey = 'knowledgeGraphExercises_' + Date.now()
    sessionStorage.setItem(exercisesKey, JSON.stringify(selected))
    
    router.push({
      path: '/practice',
      query: {
        from: 'graph',
        showExerciseModal: 'true',
        multiple: selected.length > 1 ? 'true' : 'false',
        exercisesKey: exercisesKey
      }
    })
  } else {
    console.error('Router instance is not provided')
    showToastMessage('跳转失败，请重试', 'error')
  }
}

/**
 * 批量加入错题本
 * @param {Object} exercises - 习题列表的ref
 */
export const addToWrongBook = (exercises) => {
  const selected = exercises.value.filter(e => e.selected)
  if (selected.length === 0) {
    showToastMessage('请先选择习题')
    return
  }
  showToastMessage(`已添加 ${selected.length} 道题目到错题本`)
}

/**
 * 显示学习进度
 * @param {Object} showProgressModal - 学习进度弹窗显示状态的ref
 * @param {Object} selectedNode - 选中节点的ref
 * @param {Object} selectedKnowledgePoint - 选中知识点的ref
 * @param {Object} knowledgeMastery - 知识掌握程度的ref
 * @param {Object} practiceHistory - 练习历史的ref
 * @param {Object} currentBook - 当前书籍的ref
 * @param {Object} userLearningProgress - 用户学习进度数据
 */
export const showLearningProgress = (showProgressModal, selectedNode, selectedKnowledgePoint, knowledgeMastery, practiceHistory, currentBook, userLearningProgress) => {
  if (!selectedNode.value) return
  
  selectedKnowledgePoint.value = selectedNode.value
  const bookId = currentBook.value?.id || 'default'
  knowledgeMastery.value = calculateKnowledgeMastery(selectedNode.value, bookId, userLearningProgress)
  
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
