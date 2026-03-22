import { ref, computed } from 'vue'

export const UI_STATES = {
  IDLE: 'idle',
  LOADING: 'loading',
  ERROR: 'error',
  SUCCESS: 'success'
}

export function useSeniorQA() {
  // State
  const currentUser = ref({ id: 1, name: 'User' })
  const stats = ref({ totalQuestions: 0, totalAnswers: 0, totalLikes: 0 })
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
  
  // Computed
  const filteredSeniors = computed(() => [])
  const selectedSenior = computed(() => null)
  const listState = ref(UI_STATES.IDLE)
  const paginatedQuestions = computed(() => [])
  const filteredAndSortedQuestions = computed(() => [])
  const activeQuestion = ref(null)
  
  // Methods
  const setSeniorFilter = (filter) => {
    seniorFilter.value = filter
  }
  
  const selectSenior = (seniorId) => {
    selectedSeniorId.value = seniorId
  }
  
  const setListFilter = (filter) => {
    listFilter.value = filter
  }
  
  const setListSort = (sort) => {
    listSort.value = sort
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
  
  const publishQuestion = async (payload) => {
    askState.value = UI_STATES.LOADING
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      askState.value = UI_STATES.SUCCESS
      return { ok: true, message: '问题发布成功' }
    } catch (error) {
      askState.value = UI_STATES.ERROR
      return { ok: false, message: '发布失败，请重试' }
    }
  }
  
  const resetAskState = () => {
    askState.value = UI_STATES.IDLE
  }
  
  const openDetail = async (questionId) => {
    detailState.value = UI_STATES.LOADING
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      activeQuestion.value = { id: questionId, title: '示例问题', content: '示例内容', answers: [] }
      detailState.value = UI_STATES.SUCCESS
      detailDialogVisible.value = true
    } catch (error) {
      detailState.value = UI_STATES.ERROR
      detailError.value = '加载失败，请重试'
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
  
  const toggleAnswerLike = (answerId) => {
    // Simulate like toggle
    return true
  }
  
  const acceptAnswer = (answerId) => {
    // Simulate accept answer
    return { ok: true, message: '已采纳回答' }
  }
  
  const submitFollowUp = (content) => {
    // Simulate follow up submission
    return { ok: true, message: '追问已提交' }
  }
  
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