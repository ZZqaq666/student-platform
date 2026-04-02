import { ref, computed } from 'vue'
import { apiService } from '../../../api/api.js'
import { useAuthStore } from '../../../store/auth.js'
import { seniorMockApi, stats as mockStats, seniors as mockSeniors } from '../../../mock/seniorMock.js'

export const UI_STATES = {
  IDLE: 'idle',
  LOADING: 'loading',
  ERROR: 'error',
  SUCCESS: 'success'
}

export function useSeniorQA() {
  // State
  const authStore = useAuthStore()
  const currentUser = ref({ id: authStore.userId || 1, name: 'User' })
  const stats = ref(mockStats)
  const seniorFilter = ref('all')
  const selectedSeniorId = ref(null)
  const listFilter = ref('all')
  const listSort = ref('latest')
  const currentPage = ref(1)
  const pageSize = ref(10)
  const askDialogVisible = ref(false)
  const askState = ref(UI_STATES.IDLE)
  const detailDialogVisible = ref(false)
  const detailState = ref(UI_STATES.IDLE)
  const detailError = ref(null)
  const questions = ref([])
  
  // Computed
  const filteredSeniors = computed(() => {
    return mockSeniors
  })
  const selectedSenior = computed(() => {
    if (!selectedSeniorId.value) return null
    return mockSeniors.find(s => s.id === selectedSeniorId.value) || null
  })
  const listState = ref(UI_STATES.IDLE)
  const paginatedQuestions = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredAndSortedQuestions.value.slice(start, end)
  })
  const filteredAndSortedQuestions = computed(() => {
    if (!Array.isArray(questions.value)) {
      return []
    }
    
    let result = [...questions.value]
    
    // 根据专业方向筛选（如果后端没有筛选）
    if (seniorFilter.value && seniorFilter.value !== 'all') {
      result = result.filter(q => q.category === seniorFilter.value)
    }
    
    return result
  })
  const activeQuestion = ref(null)
  
  // Methods
  const setSeniorFilter = (filter) => {
    seniorFilter.value = filter
    loadQuestions()
  }
  
  const selectSenior = (seniorId) => {
    selectedSeniorId.value = seniorId
    loadQuestions()
  }
  
  const setListFilter = (filter) => {
    listFilter.value = filter
    loadQuestions()
  }
  
  const setListSort = (sort) => {
    listSort.value = sort
    loadQuestions()
  }
  
  const setCurrentPage = (page) => {
    currentPage.value = page
  }
  
  const openAskDialog = () => {
    askDialogVisible.value = true
  }
  
  const closeAskDialog = () => {
    askDialogVisible.value = false
  }
  
  const loadQuestions = async () => {
    listState.value = UI_STATES.LOADING
    try {
      // 构建查询参数
      const params = {
        seniorId: selectedSeniorId.value,
        filter: listFilter.value,
        sort: listSort.value,
        page: currentPage.value,
        pageSize: pageSize.value
      }
      
      // 如果选择了专业方向筛选（不是"全部"），添加到参数中
      if (seniorFilter.value && seniorFilter.value !== 'all') {
        params.category = seniorFilter.value
      }
      
      console.log('加载问题列表，参数:', params)
      const response = await seniorMockApi.getQuestions(params)
      console.log('问题列表原始响应:', response)
      
      // 处理各种可能的后端返回结构
      let rawQuestions = []
      if (Array.isArray(response)) {
        rawQuestions = response
      } else if (response?.data) {
        // 可能是 {code, message, data: [...]} 或 {code, message, data: {list: [...]}}
        if (Array.isArray(response.data)) {
          rawQuestions = response.data
        } else if (Array.isArray(response.data.list)) {
          rawQuestions = response.data.list
        } else if (Array.isArray(response.data.records)) {
          rawQuestions = response.data.records
        }
      }
      
      // 打印每个问题的分类，用于调试
      console.log('问题分类统计:', rawQuestions.map(q => ({ id: q.id, category: q.category, categoryText: q.categoryText, title: q.title?.slice(0, 20) })))
      questions.value = rawQuestions
      listState.value = UI_STATES.SUCCESS
    } catch (error) {
      console.error('加载问题列表失败:', error)
      listState.value = UI_STATES.ERROR
      questions.value = []
    }
  }
  
  const publishQuestion = async (payload) => {
    askState.value = UI_STATES.LOADING
    try {
      console.log('发布问题，参数:', payload)
      const response = await seniorMockApi.publishQuestion(payload)
      console.log('问题发布成功，响应:', response)
      askState.value = UI_STATES.SUCCESS
      // 重新加载问题列表
      await loadQuestions()
      return { ok: true, message: '问题发布成功' }
    } catch (error) {
      console.error('发布问题失败:', error)
      askState.value = UI_STATES.ERROR
      return { ok: false, message: '发布失败，请重试: ' + (error.errorMessage || error.message) }
    }
  }
  
  const resetAskState = () => {
    askState.value = UI_STATES.IDLE
  }
  
  const openDetail = async (questionId) => {
    detailState.value = UI_STATES.LOADING
    try {
      console.log('加载问题详情，ID:', questionId)
      const response = await seniorMockApi.getQuestionById(questionId)
      console.log('问题详情加载成功:', response)
      // 处理后端返回的统一响应格式 {code, message, data}
      const questionData = response.data || response
      activeQuestion.value = questionData
      detailState.value = UI_STATES.SUCCESS
      detailDialogVisible.value = true
    } catch (error) {
      console.error('加载问题详情失败:', error)
      detailState.value = UI_STATES.ERROR
      detailError.value = '加载失败，请重试: ' + (error.errorMessage || error.message)
    }
  }
  
  const closeDetailDialog = () => {
    detailDialogVisible.value = false
  }
  
  const retryLoadDetail = () => {
    if (activeQuestion.value) {
      openDetail(activeQuestion.value.id)
    }
  }
  
  const toggleAnswerLike = async (answerId) => {
    try {
      // 检查 activeQuestion 是否存在
      if (!activeQuestion.value) {
        console.error('点赞失败: 当前没有激活的问题')
        return false
      }

      const questionId = activeQuestion.value.id
      if (!questionId) {
        console.error('点赞失败: 问题 ID 不存在')
        return false
      }

      console.log('点赞回答，ID:', answerId)
      await seniorMockApi.likeAnswer(answerId)
      console.log('点赞成功')
      // 重新加载问题详情
      await openDetail(questionId)
      return true
    } catch (error) {
      console.error('点赞失败:', error)
      return false
    }
  }
  
  const acceptAnswer = async (answerId) => {
    try {
      // 检查 activeQuestion 是否存在
      if (!activeQuestion.value) {
        console.error('采纳回答失败: 当前没有激活的问题')
        return { ok: false, message: '采纳失败：问题信息不存在' }
      }

      const questionId = activeQuestion.value.id
      if (!questionId) {
        console.error('采纳回答失败: 问题 ID 不存在')
        return { ok: false, message: '采纳失败：问题 ID 无效' }
      }

      console.log('采纳回答，问题ID:', questionId, '回答ID:', answerId)
      await seniorMockApi.acceptAnswer(questionId, answerId)
      console.log('采纳成功')
      // 重新加载问题详情
      await openDetail(questionId)
      return { ok: true, message: '已采纳回答' }
    } catch (error) {
      console.error('采纳回答失败:', error)
      return { ok: false, message: '采纳失败，请重试: ' + (error.errorMessage || error.message) }
    }
  }
  
  const submitFollowUp = async (content) => {
    try {
      // 检查 activeQuestion 是否存在
      if (!activeQuestion.value) {
        console.error('提交追问失败: 当前没有激活的问题')
        return { ok: false, message: '提交失败：问题信息不存在，请重新打开问题详情' }
      }

      // 检查问题 ID 是否存在
      const questionId = activeQuestion.value.id
      if (!questionId) {
        console.error('提交追问失败: 问题 ID 不存在', activeQuestion.value)
        return { ok: false, message: '提交失败：问题 ID 无效' }
      }

      console.log('提交追问，问题ID:', questionId, '内容:', content)
      await seniorMockApi.submitFollowUp(questionId, content)
      console.log('追问提交成功')
      // 重新加载问题详情
      await openDetail(questionId)
      return { ok: true, message: '追问已提交' }
    } catch (error) {
      console.error('提交追问失败:', error)
      return { ok: false, message: '追问提交失败，请重试: ' + (error.errorMessage || error.message) }
    }
  }
  
  // 初始化加载问题列表
  loadQuestions()
  
  return {
    currentUser,
    stats,
    seniorFilter,
    selectedSeniorId,
    listFilter,
    listSort,
    currentPage,
    pageSize,
    askDialogVisible,
    askState,
    detailDialogVisible,
    detailState,
    detailError,
    filteredSeniors,
    selectedSenior,
    listState,
    paginatedQuestions,
    filteredAndSortedQuestions,
    activeQuestion,
    setSeniorFilter,
    selectSenior,
    setListFilter,
    setListSort,
    setCurrentPage,
    openAskDialog,
    closeAskDialog,
    publishQuestion,
    resetAskState,
    openDetail,
    closeDetailDialog,
    retryLoadDetail,
    toggleAnswerLike,
    acceptAnswer,
    submitFollowUp
  }
}