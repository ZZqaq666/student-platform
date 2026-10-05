import { ref, computed } from 'vue'
import { apiService } from '@/api/api'
import { useAuthStore } from '@/stores/auth'

export const UI_STATES = {
  IDLE: 'idle',
  LOADING: 'loading',
  ERROR: 'error',
  SUCCESS: 'success'
}

export function useSeniorQA() {
  const authStore = useAuthStore()
  const currentUser = ref({ id: authStore.userId || 1, name: 'User' })
  const stats = ref({ seniorCount: 0, totalQuestions: 0, solvedRate: 0, avgRating: 0 })
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
  const seniors = ref([])
  const total = ref(0)
  
  const filteredSeniors = computed(() => {
    return seniors.value
  })
  
  const selectedSenior = computed(() => {
    if (!selectedSeniorId.value) return null
    return seniors.value.find(s => s.id === selectedSeniorId.value) || null
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
    
    if (seniorFilter.value && seniorFilter.value !== 'all') {
      result = result.filter(q => q.category === seniorFilter.value)
    }
    
    return result
  })
  
  const activeQuestion = ref(null)
  
  const setSeniorFilter = (filter) => {
    seniorFilter.value = filter
    currentPage.value = 1
    loadQuestions()
  }
  
  const selectSenior = (seniorId) => {
    selectedSeniorId.value = seniorId
    currentPage.value = 1
    loadQuestions()
  }
  
  const setListFilter = (filter) => {
    listFilter.value = filter
    currentPage.value = 1
    loadQuestions()
  }
  
  const setListSort = (sort) => {
    listSort.value = sort
    currentPage.value = 1
    loadQuestions()
  }
  
  const setCurrentPage = (page) => {
    currentPage.value = page
    loadQuestions()
  }
  
  const openAskDialog = () => {
    askDialogVisible.value = true
  }
  
  const closeAskDialog = () => {
    askDialogVisible.value = false
  }
  
  const loadStats = async () => {
    try {
      const response = await apiService.senior.getStats()
      if (response && response.data) {
        stats.value = response.data
      } else if (response && typeof response === 'object') {
        stats.value = response
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
    }
  }
  
  const loadSeniors = async () => {
    try {
      const response = await apiService.senior.getSeniors()
      if (response && response.data) {
        seniors.value = response.data
      } else if (Array.isArray(response)) {
        seniors.value = response
      }
    } catch (error) {
      console.error('加载学长学姐列表失败:', error)
      seniors.value = []
    }
  }
  
  const loadQuestions = async () => {
    listState.value = UI_STATES.LOADING
    try {
      const params = {
        seniorId: selectedSeniorId.value,
        filter: listFilter.value,
        sort: listSort.value,
        page: currentPage.value,
        pageSize: pageSize.value
      }
      
      if (seniorFilter.value && seniorFilter.value !== 'all') {
        params.category = seniorFilter.value
      }
      
      console.log('加载问题列表，参数:', params)
      const response = await apiService.senior.getQuestions(params)
      console.log('问题列表原始响应:', response)
      
      let rawQuestions = []
      if (Array.isArray(response)) {
        rawQuestions = response
      } else if (response?.data) {
        if (Array.isArray(response.data)) {
          rawQuestions = response.data
        } else if (Array.isArray(response.data.list)) {
          rawQuestions = response.data.list
          total.value = response.data.total || rawQuestions.length
        } else if (Array.isArray(response.data.records)) {
          rawQuestions = response.data.records
          total.value = response.data.total || rawQuestions.length
        }
      }
      
      questions.value = rawQuestions.map(q => ({
        ...q,
        categoryText: getCategoryText(q.category),
        status: q.solved ? 'solved' : 'unsolved'
      }))
      
      if (total.value === 0) {
        total.value = questions.value.length
      }
      
      listState.value = UI_STATES.SUCCESS
    } catch (error) {
      console.error('加载问题列表失败:', error)
      listState.value = UI_STATES.ERROR
      questions.value = []
    }
  }
  
  const getCategoryText = (category) => {
    const categoryMap = {
      'computer': '计算机科学',
      'electronics': '电子信息',
      'software': '软件工程',
      'data': '数据科学',
      'math': '数学',
      'physics': '物理',
      'other': '其他',
      'cs': '计算机科学',
      'math': '数学',
      'physics': '物理',
      'english': '英语'
    }
    return categoryMap[category] || category
  }
  
  const publishQuestion = async (payload) => {
    askState.value = UI_STATES.LOADING
    try {
      console.log('发布问题，参数:', payload)
      const response = await apiService.senior.publishQuestion(payload)
      console.log('问题发布成功，响应:', response)
      askState.value = UI_STATES.SUCCESS
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
      const response = await apiService.senior.getQuestionById(questionId)
      console.log('问题详情加载成功:', response)
      const questionData = response.data || response
      activeQuestion.value = {
        ...questionData,
        categoryText: getCategoryText(questionData.category),
        status: questionData.solved ? 'solved' : 'unsolved',
        answers: questionData.answers || []
      }
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
      await apiService.senior.likeAnswer(answerId)
      console.log('点赞成功')
      await openDetail(questionId)
      return true
    } catch (error) {
      console.error('点赞失败:', error)
      return false
    }
  }
  
  const acceptAnswer = async (answerId) => {
    try {
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
      await apiService.senior.acceptAnswer(questionId, answerId)
      console.log('采纳成功')
      await openDetail(questionId)
      return { ok: true, message: '已采纳回答' }
    } catch (error) {
      console.error('采纳回答失败:', error)
      return { ok: false, message: '采纳失败，请重试: ' + (error.errorMessage || error.message) }
    }
  }
  
  const submitFollowUp = async (content) => {
    try {
      if (!activeQuestion.value) {
        console.error('提交追问失败: 当前没有激活的问题')
        return { ok: false, message: '提交失败：问题信息不存在，请重新打开问题详情' }
      }

      const questionId = activeQuestion.value.id
      if (!questionId) {
        console.error('提交追问失败: 问题 ID 不存在', activeQuestion.value)
        return { ok: false, message: '提交失败：问题 ID 无效' }
      }

      console.log('提交追问，问题ID:', questionId, '内容:', content)
      await apiService.senior.submitFollowUp(questionId, content)
      console.log('追问提交成功')
      await openDetail(questionId)
      return { ok: true, message: '追问已提交' }
    } catch (error) {
      console.error('提交追问失败:', error)
      return { ok: false, message: '追问提交失败，请重试: ' + (error.errorMessage || error.message) }
    }
  }
  
  const initData = async () => {
    await loadStats()
    await loadSeniors()
    await loadQuestions()
  }
  
  initData()
  
  return {
    currentUser,
    stats,
    seniorFilter,
    selectedSeniorId,
    listFilter,
    listSort,
    currentPage,
    pageSize,
    total,
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