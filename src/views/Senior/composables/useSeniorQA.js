import { computed, ref } from 'vue'
import { apiService } from '@/api/api'
import { useAuthStore } from '@/stores/auth'

export const UI_STATES = {
  IDLE: 'idle',
  LOADING: 'loading',
  ERROR: 'error',
  EMPTY: 'empty',
  SUCCESS: 'success'
}

/** AI 生成状态（与后端 SeniorAiService 常量保持一致） */
export const AI_STATUS = {
  NONE: 'none',
  GENERATING: 'generating',
  DONE: 'done',
  FAILED: 'failed'
}

/** 轮询配置：大模型生成通常需要 10~40 秒 */
const POLL_INTERVAL_MS = 3000
const POLL_MAX_ATTEMPTS = 40

const CATEGORY_TEXT_MAP = {
  computer: '计算机科学',
  electronics: '电子信息',
  software: '软件工程',
  data: '数据科学',
  math: '数学',
  physics: '物理',
  english: '英语',
  cs: '计算机科学',
  other: '其他'
}

const DEFAULT_PAGE_SIZE = 10

function getCategoryText(category) {
  return CATEGORY_TEXT_MAP[category] || category || '其他'
}

function buildPreview(content, max = 110) {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  return text.length <= max ? text : `${text.slice(0, max)}…`
}

function normalizeQuestion(raw) {
  if (!raw) {
    return null
  }
  return {
    ...raw,
    categoryText: getCategoryText(raw.category),
    preview: buildPreview(raw.content),
    answerCount: raw.answerCount ?? raw.answers?.length ?? 0,
    aiStatus: raw.aiStatus || AI_STATUS.NONE,
    answers: raw.answers || [],
    followUps: raw.followUps || []
  }
}

export function useSeniorQA() {
  const authStore = useAuthStore()

  // 真实登录用户：不再硬编码 id=1 / name='User'
  const currentUser = computed(() => {
    const id = authStore.userId || localStorage.getItem('userId') || ''
    const name =
      localStorage.getItem('userName') ||
      authStore.user?.nickname ||
      authStore.user?.username ||
      ''
    return { id: id ? String(id) : '', name: name || '我' }
  })

  const stats = ref({ seniorCount: 0, totalQuestions: 0, totalAnswers: 0, solvedRate: 0, avgRating: 0 })
  const seniorFilter = ref('all')
  const selectedSeniorId = ref(null)
  const listFilter = ref('all')
  const listSort = ref('latest')
  const currentPage = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)

  const askDialogVisible = ref(false)
  const askState = ref(UI_STATES.IDLE)
  const detailDialogVisible = ref(false)
  const detailState = ref(UI_STATES.IDLE)
  const detailError = ref(null)

  const questions = ref([])
  const seniors = ref([])
  const listState = ref(UI_STATES.IDLE)
  const activeQuestion = ref(null)

  // ---------------------------------------------------------------- 派生数据

  // 侧边栏方向筛选：按学长人设的 subject 真正过滤（旧实现直接返回全量）
  const filteredSeniors = computed(() => {
    if (!seniorFilter.value || seniorFilter.value === 'all') {
      return seniors.value
    }
    return seniors.value.filter((senior) => senior.subject === seniorFilter.value)
  })

  const selectedSenior = computed(() => {
    if (!selectedSeniorId.value) return null
    return seniors.value.find((s) => s.id === selectedSeniorId.value) || null
  })

  const paginatedQuestions = computed(() => questions.value)

  // ---------------------------------------------------------------- 数据加载

  const loadStats = async () => {
    try {
      const response = await apiService.senior.getStats()
      const data = response?.data ?? response
      if (data) {
        stats.value = data
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
    }
  }

  const loadSeniors = async () => {
    try {
      const response = await apiService.senior.getSeniors()
      const data = response?.data ?? response
      seniors.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('加载学长学姐列表失败:', error)
      seniors.value = []
    }
  }

  const loadQuestions = async () => {
    listState.value = UI_STATES.LOADING
    try {
      const params = {
        seniorId: selectedSeniorId.value || undefined,
        category: seniorFilter.value && seniorFilter.value !== 'all' ? seniorFilter.value : undefined,
        filter: listFilter.value || 'all',
        sort: listSort.value || 'latest',
        page: currentPage.value,
        pageSize: pageSize.value
      }

      const response = await apiService.senior.getQuestions(params)
      const data = response?.data ?? response
      const list = Array.isArray(data) ? data : data?.list ?? []

      questions.value = list.map(normalizeQuestion)
      total.value = Number(data?.total ?? questions.value.length)
      listState.value = questions.value.length ? UI_STATES.SUCCESS : UI_STATES.EMPTY
    } catch (error) {
      console.error('加载问题列表失败:', error)
      listState.value = UI_STATES.ERROR
      questions.value = []
      total.value = 0
    }
  }

  // ---------------------------------------------------------------- 交互动作

  const setSeniorFilter = (filter) => {
    seniorFilter.value = filter
    // 切换方向后取消具体学长的选中，避免出现「方向与人选不一致」的空列表
    selectedSeniorId.value = null
    currentPage.value = 1
    loadQuestions()
  }

  const selectSenior = (seniorId) => {
    selectedSeniorId.value = selectedSeniorId.value === seniorId ? null : seniorId
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

  const resetAskState = () => {
    askState.value = UI_STATES.IDLE
  }

  // ---------------------------------------------------------------- AI 状态轮询

  let pollTimer = null

  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  const syncQuestionIntoList = (question) => {
    const normalized = normalizeQuestion(question)
    const index = questions.value.findIndex((item) => item.id === normalized.id)
    if (index >= 0) {
      questions.value.splice(index, 1, normalized)
    }
    if (activeQuestion.value?.id === normalized.id) {
      activeQuestion.value = normalized
    }
    return normalized
  }

  const fetchQuestionById = async (questionId) => {
    const response = await apiService.senior.getQuestionById(questionId)
    const data = response?.data ?? response
    return normalizeQuestion(data)
  }

  /**
   * 轮询某个问题的 AI 生成状态，直到 done / failed。
   * 这样「学长正在解答」不会永远转圈，也不会让用户手动刷新才看到回答。
   */
  const startPolling = (questionId) => {
    if (!questionId) return
    stopPolling()

    let attempts = 0
    pollTimer = setInterval(async () => {
      attempts += 1
      try {
        const question = await fetchQuestionById(questionId)
        syncQuestionIntoList(question)
        if (question.aiStatus === AI_STATUS.DONE || question.aiStatus === AI_STATUS.FAILED) {
          stopPolling()
          await loadQuestions()
        }
      } catch (error) {
        console.error('轮询问题状态失败:', error)
      }

      if (attempts >= POLL_MAX_ATTEMPTS) {
        stopPolling()
      }
    }, POLL_INTERVAL_MS)
  }

  // ---------------------------------------------------------------- 写操作

  const publishQuestion = async (payload) => {
    askState.value = UI_STATES.LOADING
    try {
      const response = await apiService.senior.publishQuestion(payload)
      const data = response?.data ?? response
      askState.value = UI_STATES.SUCCESS
      await loadQuestions()
      startPolling(data?.id)
      return { ok: true, message: '问题发布成功，学长正在解答…', questionId: data?.id }
    } catch (error) {
      console.error('发布问题失败:', error)
      askState.value = UI_STATES.ERROR
      return { ok: false, message: '发布失败：' + (error.errorMessage || error.message) }
    }
  }

  const regenerateAnswer = async (questionId) => {
    try {
      await apiService.senior.regenerateAnswer(questionId)
      startPolling(questionId)
      return { ok: true, message: '已重新请求学长解答' }
    } catch (error) {
      console.error('重新生成回答失败:', error)
      return { ok: false, message: '重新生成失败：' + (error.errorMessage || error.message) }
    }
  }

  const openDetail = async (questionId) => {
    detailState.value = UI_STATES.LOADING
    detailError.value = null
    try {
      const question = await fetchQuestionById(questionId)
      activeQuestion.value = question
      detailState.value = UI_STATES.SUCCESS
      detailDialogVisible.value = true
      // 打开详情时若仍在生成中，顺带接上轮询
      if (question.aiStatus === AI_STATUS.GENERATING) {
        startPolling(questionId)
      }
    } catch (error) {
      console.error('加载问题详情失败:', error)
      detailState.value = UI_STATES.ERROR
      detailError.value = '加载失败：' + (error.errorMessage || error.message)
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
      const response = await apiService.senior.likeAnswer(answerId)
      const data = response?.data ?? response
      if (activeQuestion.value) {
        activeQuestion.value = {
          ...activeQuestion.value,
          answers: activeQuestion.value.answers.map((answer) =>
            answer.id === answerId
              ? { ...answer, likedByMe: !!data?.liked, likes: data?.likes ?? answer.likes }
              : answer
          )
        }
      }
      return true
    } catch (error) {
      console.error('点赞失败:', error)
      return false
    }
  }

  const acceptAnswer = async (answerId) => {
    try {
      if (!activeQuestion.value?.id) {
        return { ok: false, message: '采纳失败：问题信息不存在' }
      }
      await apiService.senior.acceptAnswer(activeQuestion.value.id, answerId)
      await openDetail(activeQuestion.value.id)
      await loadQuestions()
      return { ok: true, message: '已采纳回答' }
    } catch (error) {
      console.error('采纳回答失败:', error)
      return { ok: false, message: '采纳失败：' + (error.errorMessage || error.message) }
    }
  }

  const submitFollowUp = async (content) => {
    try {
      if (!activeQuestion.value?.id) {
        return { ok: false, message: '提交失败：问题信息不存在，请重新打开问题详情' }
      }
      const questionId = activeQuestion.value.id
      await apiService.senior.submitFollowUp(questionId, content)
      startPolling(questionId)
      return { ok: true, message: '追问已提交，学长正在回复…' }
    } catch (error) {
      console.error('提交追问失败:', error)
      return { ok: false, message: '追问提交失败：' + (error.errorMessage || error.message) }
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
    questions,
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
    regenerateAnswer,
    openDetail,
    closeDetailDialog,
    retryLoadDetail,
    toggleAnswerLike,
    acceptAnswer,
    submitFollowUp,
    stopPolling
  }
}
