import { ref, computed } from 'vue'

export const UI_STATES = {
  IDLE: 'idle',
  LOADING: 'loading',
  ERROR: 'error',
  SUCCESS: 'success'
}

/* ===== 分类文本映射 ===== */
const CATEGORY_MAP = {
  math: '数学',
  physics: '物理',
  cs: '计算机',
  english: '英语',
  other: '其他'
}

/* ===== Mock 学长数据 ===== */
function createMockSeniors() {
  return [
    { id: 's1', name: '张明远', grade: '研二', major: '应用数学', tags: ['高等数学', '线性代数'], answerCount: 86, rating: 4.9, certified: true, online: true, subjectFilter: ['math'] },
    { id: 's2', name: '李晓华', grade: '大四', major: '计算机科学', tags: ['Python', '数据结构'], answerCount: 64, rating: 4.8, certified: true, online: true, subjectFilter: ['cs'] },
    { id: 's3', name: '王思涵', grade: '研一', major: '物理学', tags: ['力学', '电磁学'], answerCount: 52, rating: 4.7, certified: true, online: false, subjectFilter: ['physics'] },
    { id: 's4', name: '陈雨桐', grade: '大三', major: '英语语言文学', tags: ['写作', '翻译'], answerCount: 41, rating: 4.6, certified: false, online: true, subjectFilter: ['english'] },
    { id: 's5', name: '赵博文', grade: '研二', major: '软件工程', tags: ['算法', '系统设计'], answerCount: 73, rating: 4.9, certified: true, online: true, subjectFilter: ['cs'] },
    { id: 's6', name: '刘思琪', grade: '大四', major: '统计学', tags: ['概率论', '数理统计'], answerCount: 38, rating: 4.5, certified: false, online: false, subjectFilter: ['math'] },
  ]
}

/* ===== Mock 初始问题数据 ===== */
function createMockQuestions() {
  return [
    {
      id: 'q1',
      authorId: 1,
      author: '小明同学',
      category: 'math',
      categoryText: '数学',
      title: '如何理解拉格朗日中值定理的几何意义？',
      preview: '最近在学习中值定理，对拉格朗日中值定理的几何解释不太理解，希望能得到帮助...',
      content: '最近在学习中值定理，对拉格朗日中值定理的几何解释不太理解。我知道它说的是函数在闭区间上连续、开区间可导，那么存在一点使得该点切线平行于连接端点的弦。但具体怎么应用到实际题目中呢？',
      tags: ['高等数学', '微分中值定理'],
      images: [],
      views: 128,
      answerCount: 3,
      likes: 24,
      solved: true,
      acceptedAnswerId: 'a1',
      seniorId: 's1',
      createdAt: '2 小时前',
      answers: [
        { id: 'a1', authorName: '张明远', content: '几何意义很直观：想象一条从A到B的曲线，拉格朗日定理保证在中间某点，切线与AB连线平行。做题时关键是验证条件——闭区间连续、开区间可导，然后设出等式求解。', time: '1 小时前', likes: 18, likedByCurrentUser: false, certified: true, badge: '最佳答案' },
        { id: 'a2', authorName: '刘思琪', content: '补充一点：中值定理的本质是建立函数增量与导数的关系，这在证明不等式时特别有用。', time: '30 分钟前', likes: 5, likedByCurrentUser: false, certified: false, badge: '' }
      ],
      followUps: []
    },
    {
      id: 'q2',
      authorId: 1,
      author: '小明同学',
      category: 'cs',
      categoryText: '计算机',
      title: 'Python 装饰器的执行顺序是什么？',
      preview: '在使用多个装饰器时，不太清楚它们的嵌套执行顺序，以及参数传递的细节...',
      content: '在使用多个装饰器时，不太清楚它们的嵌套执行顺序，以及参数传递的细节。比如 @decorator1 和 @decorator2 同时使用时，哪个先执行？',
      tags: ['Python', '装饰器'],
      images: [],
      views: 95,
      answerCount: 2,
      likes: 16,
      solved: false,
      acceptedAnswerId: null,
      seniorId: 's2',
      createdAt: '5 小时前',
      answers: [
        { id: 'a3', authorName: '李晓华', content: '多个装饰器的执行顺序是从下往上的（离函数最近的先执行）。可以把装饰器看作函数嵌套：decorator1(decorator2(original_func))。', time: '4 小时前', likes: 12, likedByCurrentUser: false, certified: true, badge: '' }
      ],
      followUps: []
    },
    {
      id: 'q3',
      authorId: 2,
      author: '小红学姐',
      category: 'physics',
      categoryText: '物理',
      title: '量子力学中波函数坍缩的物理本质是什么？',
      preview: '对哥本哈根诠释中的波函数坍缩概念感到困惑，想了解不同的诠释观点...',
      content: '对哥本哈根诠释中的波函数坍缩概念感到困惑。测量导致波函数从叠加态"坍缩"到本征态，但这个过程是瞬时的吗？不同的诠释（如多世界诠释）如何看待这个问题？',
      tags: ['量子力学', '波函数'],
      images: [],
      views: 203,
      answerCount: 2,
      likes: 42,
      solved: false,
      acceptedAnswerId: null,
      seniorId: 's3',
      createdAt: '1 天前',
      answers: [
        { id: 'a4', authorName: '王思涵', content: '波函数坍缩是量子力学诠释中最有争议的概念之一。哥本哈根诠释认为测量导致坍缩，但并未解释坍缩的机制。多世界诠释则否认坍缩，认为所有可能的结果都在不同的分支宇宙中实现。', time: '20 小时前', likes: 28, likedByCurrentUser: false, certified: true, badge: '热门回答' }
      ],
      followUps: []
    },
    {
      id: 'q4',
      authorId: 1,
      author: '小明同学',
      category: 'english',
      categoryText: '英语',
      title: '英语学术写作中如何避免中式表达？',
      preview: '写论文时总是会不自觉地用中式思维组织句子，想知道有什么好的改进方法...',
      content: '写论文时总是会不自觉地用中式思维组织句子，导致表达不够地道。想知道有什么系统性的改进方法，或者在写作过程中有哪些常见的陷阱需要避免？',
      tags: ['学术写作', '表达技巧'],
      images: [],
      views: 67,
      answerCount: 1,
      likes: 9,
      solved: false,
      acceptedAnswerId: null,
      seniorId: 's4',
      createdAt: '3 天前',
      answers: [
        { id: 'a5', authorName: '陈雨桐', content: '建议多读英文原版论文，注意作者的句式结构。写作时可以先用简单句表达，再逐步丰富。另外推荐使用Grammarly等工具辅助检查。常见陷阱包括：过度使用被动语态、主语缺失、连接词误用等。', time: '2 天前', likes: 7, likedByCurrentUser: false, certified: false, badge: '' }
      ],
      followUps: []
    }
  ]
}

export function useSeniorQA() {
  /* ===== 响应式状态 ===== */
  const currentUser = ref({ id: 1, name: '小明同学' })
  const seniors = ref(createMockSeniors())
  const questions = ref(createMockQuestions())
  const seniorFilter = ref('all')
  const selectedSeniorId = ref(null)
  const listFilter = ref('all')
  const listSort = ref('time')
  const currentPage = ref(1)
  const pageSize = ref(6)
  const askDialogVisible = ref(false)
  const askState = ref(UI_STATES.IDLE)
  const detailDialogVisible = ref(false)
  const detailState = ref(UI_STATES.IDLE)
  const detailError = ref(null)
  const listLoaded = ref(false)
  const activeQuestion = ref(null)

  /* ===== 统计数据 ===== */
  const stats = computed(() => {
    const allAnswered = questions.value.filter(q => q.answerCount > 0)
    const solved = questions.value.filter(q => q.solved)
    const allLikes = questions.value.reduce((s, q) => s + (q.likes || 0), 0)
    return {
      seniorCount: seniors.value.filter(s => s.online).length,
      totalQuestions: questions.value.length,
      solvedRate: questions.value.length ? Math.round((solved.length / questions.value.length) * 100) : 0,
      avgRating: '4.8',
      totalAnswers: questions.value.reduce((s, q) => s + (q.answerCount || 0), 0),
      totalLikes: allLikes
    }
  })

  /* ===== 学长筛选 ===== */
  const filteredSeniors = computed(() => {
    if (seniorFilter.value === 'all') return seniors.value
    return seniors.value.filter(s => s.subjectFilter.includes(seniorFilter.value))
  })

  const selectedSenior = computed(() => {
    if (!selectedSeniorId.value) return null
    return seniors.value.find(s => s.id === selectedSeniorId.value) || null
  })

  /* ===== 问题筛选、排序、分页 ===== */
  const filteredAndSortedQuestions = computed(() => {
    let result = [...questions.value]

    // 学长筛选
    if (selectedSeniorId.value) {
      result = result.filter(q => q.seniorId === selectedSeniorId.value)
    }

    // 状态筛选
    if (listFilter.value === 'hot') {
      result.sort((a, b) => b.views - a.views)
    } else if (listFilter.value === 'unsolved') {
      result = result.filter(q => !q.solved)
      result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    } else if (listFilter.value === 'solved') {
      result = result.filter(q => q.solved)
      result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    } else {
      result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    }

    // 排序
    if (listSort.value === 'likes') {
      result.sort((a, b) => b.likes - a.likes)
    }

    return result
  })

  const totalPages = computed(() => Math.max(1, Math.ceil(filteredAndSortedQuestions.value.length / pageSize.value)))

  const paginatedQuestions = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filteredAndSortedQuestions.value.slice(start, start + pageSize.value)
  })

  const listState = computed(() => {
    if (!listLoaded.value) return UI_STATES.IDLE
    if (filteredAndSortedQuestions.value.length === 0) return 'empty'
    return 'success'
  })

  /* ===== 初始化加载 ===== */
  setTimeout(() => {
    listLoaded.value = true
  }, 300)

  /* ===== 方法 ===== */
  const setSeniorFilter = (filter) => {
    seniorFilter.value = filter
    currentPage.value = 1
  }

  const selectSenior = (seniorId) => {
    if (selectedSeniorId.value === seniorId) {
      selectedSeniorId.value = null
    } else {
      selectedSeniorId.value = seniorId
    }
  }

  const setListFilter = (filter) => {
    listFilter.value = filter
    currentPage.value = 1
  }

  const setListSort = (sort) => {
    listSort.value = sort
    currentPage.value = 1
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
      // 模拟 API 延迟
      await new Promise(resolve => setTimeout(resolve, 600))

      const newQuestion = {
        id: `q-${Date.now()}`,
        authorId: currentUser.value.id,
        author: currentUser.value.name,
        category: payload.category,
        categoryText: CATEGORY_MAP[payload.category] || '其他',
        title: payload.title,
        preview: payload.content.slice(0, 80) + (payload.content.length > 80 ? '...' : ''),
        content: payload.content,
        tags: payload.tags || [],
        images: payload.images || [],
        views: 0,
        answerCount: 0,
        likes: 0,
        solved: false,
        acceptedAnswerId: null,
        seniorId: null,
        createdAt: '刚刚',
        answers: [],
        followUps: []
      }

      // 添加到问题列表最前面
      questions.value.unshift(newQuestion)
      askState.value = UI_STATES.SUCCESS
      currentPage.value = 1
      return { ok: true, message: '问题发布成功！' }
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
    detailError.value = null
    try {
      await new Promise(resolve => setTimeout(resolve, 400))
      const found = questions.value.find(q => q.id === questionId)
      if (!found) {
        detailState.value = 'empty'
        detailDialogVisible.value = true
        return
      }
      activeQuestion.value = found
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
    } else {
      detailState.value = UI_STATES.IDLE
    }
  }

  const toggleAnswerLike = (answerId) => {
    if (!activeQuestion.value) return false
    const answer = activeQuestion.value.answers.find(a => a.id === answerId)
    if (!answer) return false
    answer.likedByCurrentUser = !answer.likedByCurrentUser
    answer.likes += answer.likedByCurrentUser ? 1 : -1
    return true
  }

  const acceptAnswer = (answerId) => {
    if (!activeQuestion.value) return { ok: false, message: '问题数据异常' }
    if (activeQuestion.value.acceptedAnswerId) return { ok: false, message: '已有采纳回答' }
    if (String(activeQuestion.value.authorId) !== String(currentUser.value.id)) return { ok: false, message: '只有提问者可以采纳回答' }
    activeQuestion.value.acceptedAnswerId = answerId
    activeQuestion.value.solved = true
    // 同步更新问题列表中的数据
    const idx = questions.value.findIndex(q => q.id === activeQuestion.value.id)
    if (idx >= 0) {
      questions.value[idx].solved = true
      questions.value[idx].acceptedAnswerId = answerId
    }
    return { ok: true, message: '已采纳该回答' }
  }

  const submitFollowUp = (content) => {
    if (!activeQuestion.value) return { ok: false, message: '问题数据异常' }
    const followUp = {
      id: `fu-${Date.now()}`,
      author: currentUser.value.name,
      time: '刚刚',
      content
    }
    activeQuestion.value.followUps.push(followUp)
    // 同步更新问题列表
    const idx = questions.value.findIndex(q => q.id === activeQuestion.value.id)
    if (idx >= 0) {
      questions.value[idx].followUps = [...activeQuestion.value.followUps]
    }
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