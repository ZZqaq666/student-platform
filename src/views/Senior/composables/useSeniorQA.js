import { computed, ref, shallowRef, watch } from 'vue'

export const UI_STATES = Object.freeze({
  IDLE: 'idle',
  LOADING: 'loading',
  SUCCESS: 'success',
  ERROR: 'error',
  EMPTY: 'empty'
})

const TEXT = Object.freeze({
  untitledQuestion: '未命名问题',
  emptyContent: '内容暂未完善',
  unknownUser: '匿名同学',
  unknownSenior: '匿名学长学姐',
  defaultTag: '待补充',
  noAnswer: '暂无人工回复，欢迎继续追问',
  noQuestion: '当前条件下暂无问答内容'
})

const CATEGORY_LABEL_MAP = Object.freeze({
  math: '数学',
  physics: '物理',
  cs: '计算机',
  english: '英语',
  other: '其他'
})

function safeText(value, fallback) {
  if (typeof value !== 'string') {
    return fallback
  }

  const normalized = value.trim()
  return normalized || fallback
}

function safeArray(value, fallback = []) {
  return Array.isArray(value) ? value : fallback
}

function safeNumber(value, fallback = 0) {
  return Number.isFinite(value) ? value : fallback
}

function normalizeImages(value) {
  return safeArray(value, [])
    .map((item, index) => {
      if (typeof item === 'string') {
        const normalized = item.trim()
        if (!normalized) {
          return null
        }

        const isUrl = /^(data:image\/|blob:|https?:\/\/|\/)/i.test(normalized)
        return {
          id: `img-${index}`,
          name: isUrl ? `图片${index + 1}` : normalized,
          url: isUrl ? normalized : ''
        }
      }

      if (!item || typeof item !== 'object') {
        return null
      }

      const url = typeof item.url === 'string' ? item.url.trim() : ''
      if (!url) {
        return null
      }

      return {
        id: safeText(item.id, `img-${index}`),
        name: safeText(item.name, `图片${index + 1}`),
        url
      }
    })
    .filter((item) => Boolean(item && item.url))
}

function delay(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

export function useSeniorQA() {
  const currentUser = shallowRef({ id: 'u-current', name: '我' })

  const seniors = ref([
    {
      id: 's-1',
      name: '陈雨桐',
      grade: '研一',
      major: '计算机科学与技术',
      subject: 'cs',
      tags: ['算法', '数据结构', '项目经验'],
      answerCount: 248,
      rating: 4.9,
      certified: true,
      online: true
    },
    {
      id: 's-2',
      name: '赵文清',
      grade: '大四',
      major: '数学与应用数学',
      subject: 'math',
      tags: ['高数', '线代', '概率论'],
      answerCount: 196,
      rating: 4.8,
      certified: true,
      online: false
    },
    {
      id: 's-3',
      name: '',
      grade: '大三',
      major: '物理学',
      subject: 'physics',
      tags: ['力学', '电磁学'],
      answerCount: 134,
      rating: 4.7,
      certified: false,
      online: true
    },
    {
      id: 's-4',
      name: '宋佳宁',
      grade: '大四',
      major: '英语',
      subject: 'english',
      tags: ['四六级', '写作', '口语'],
      answerCount: 102,
      rating: 4.6,
      certified: true,
      online: true
    }
  ])

  const questions = ref([
    {
      id: 'q-101',
      title: '高数期末冲刺应该先刷题还是先回顾理论？',
      content:
        '最近要准备期末，感觉高数内容比较散。请问应该先做章节回顾，还是直接做综合题再查漏补缺？',
      category: 'math',
      tags: ['期末复习', '高数'],
      authorId: 'u-a1',
      author: '李同学',
      mentorIds: ['s-2'],
      createdAt: '2026-03-18 20:15',
      views: 220,
      likes: 38,
      solved: false,
      acceptedAnswerId: '',
      images: ['复习提纲截图'],
      answers: [
        {
          id: 'a-201',
          authorId: 's-2',
          authorName: '赵文清',
          certified: true,
          badge: '高分学长',
          content:
            '建议先用一天时间梳理章节框架，再按题型分块训练。每天最后留 30 分钟做错题复盘，效率会更高。',
          time: '今天 09:10',
          likes: 26,
          likedByCurrentUser: false
        }
      ],
      followUps: []
    },
    {
      id: 'q-102',
      title: 'Vue 组件通信总是混乱，有没有清晰的拆分思路？',
      content:
        '我现在写页面时经常在一个组件里塞很多状态，后面维护很难。想知道组件划分和通信有什么实操建议。',
      category: 'cs',
      tags: ['Vue', '前端工程'],
      authorId: 'u-current',
      author: '我',
      mentorIds: ['s-1'],
      createdAt: '2026-03-19 08:05',
      views: 176,
      likes: 44,
      solved: false,
      acceptedAnswerId: '',
      images: [],
      answers: [
        {
          id: 'a-202',
          authorId: 's-1',
          authorName: '陈雨桐',
          certified: true,
          badge: '项目导师',
          content:
            '优先把“页面容器”和“功能模块”分开：容器只管状态和数据流，模块只管展示和事件。你可以先拆成筛选栏、列表、详情三个组件。',
          time: '今天 09:48',
          likes: 31,
          likedByCurrentUser: true
        },
        {
          id: 'a-203',
          authorId: 's-4',
          authorName: '宋佳宁',
          certified: true,
          badge: '表达优化',
          content: '把复杂逻辑提成 composable，并给每个事件命名。这样阅读成本会显著下降。',
          time: '今天 10:02',
          likes: 16,
          likedByCurrentUser: false
        }
      ],
      followUps: [
        {
          id: 'f-301',
          authorId: 'u-current',
          author: '我',
          time: '今天 10:30',
          content: '如果后续接入接口，容器层要怎么抽象会更稳？'
        }
      ]
    },
    {
      id: 'q-103',
      title: '电磁学大题总拿不到步骤分怎么办？',
      content: '老师说我思路对但步骤不规范，想问怎么提升答题表达。',
      category: 'physics',
      tags: ['电磁学', '答题规范'],
      authorId: 'u-a2',
      author: '周同学',
      mentorIds: ['s-3'],
      createdAt: '2026-03-17 18:20',
      views: 98,
      likes: 12,
      solved: true,
      acceptedAnswerId: 'a-204',
      images: [],
      answers: [
        {
          id: 'a-204',
          authorId: 's-3',
          authorName: '',
          certified: false,
          badge: '答疑志愿者',
          content: '先写已知条件和目标量，再写公式来源，最后代入计算。每一步标注单位，能显著提升步骤分。',
          time: '昨天 14:13',
          likes: 19,
          likedByCurrentUser: false
        }
      ],
      followUps: []
    },
    {
      id: 'q-104',
      title: '',
      content: '',
      category: 'other',
      tags: [],
      authorId: 'u-a3',
      author: '',
      mentorIds: ['s-1', 's-2'],
      createdAt: '2026-03-16 11:00',
      views: 60,
      likes: 2,
      solved: false,
      acceptedAnswerId: '',
      images: [],
      answers: [],
      followUps: []
    }
  ])

  const seniorFilter = shallowRef('all')
  const selectedSeniorId = shallowRef('')
  const listFilter = shallowRef('all')
  const listSort = shallowRef('time')
  const currentPage = shallowRef(1)
  const pageSize = 6

  const askDialogVisible = shallowRef(false)
  const askState = shallowRef(UI_STATES.IDLE)

  const detailDialogVisible = shallowRef(false)
  const activeQuestionId = shallowRef('')
  const detailState = shallowRef(UI_STATES.IDLE)
  const detailError = shallowRef('')

  const normalizedSeniors = computed(() => {
    return seniors.value.map((senior) => {
      const safeName = safeText(senior.name, TEXT.unknownSenior)
      return {
        ...senior,
        name: safeName,
        grade: safeText(senior.grade, '年级待补充'),
        major: safeText(senior.major, '专业待补充'),
        tags: safeArray(senior.tags, [TEXT.defaultTag]),
        answerCount: safeNumber(senior.answerCount, 0),
        rating: Number(safeNumber(senior.rating, 0).toFixed(1))
      }
    })
  })

  const filteredSeniors = computed(() => {
    return normalizedSeniors.value.filter((senior) => {
      if (seniorFilter.value === 'all') {
        return true
      }
      return senior.subject === seniorFilter.value
    })
  })

  const selectedSenior = computed(() => {
    if (!selectedSeniorId.value) {
      return null
    }

    return normalizedSeniors.value.find((senior) => senior.id === selectedSeniorId.value) || null
  })

  const normalizedQuestions = computed(() => {
    return questions.value.map((question) => {
      const safeTitle = safeText(question.title, TEXT.untitledQuestion)
      const rawContent = typeof question.content === 'string' ? question.content.trim() : ''
      const safeContent = safeText(question.content, TEXT.emptyContent)
      const safeAuthor = safeText(question.author, TEXT.unknownUser)
      const safeTags = safeArray(question.tags, [])
      const safeImages = normalizeImages(question.images)
      const safeAnswers = safeArray(question.answers, []).map((answer) => ({
        ...answer,
        authorName: safeText(answer.authorName, TEXT.unknownSenior),
        content: safeText(answer.content, TEXT.emptyContent),
        likes: safeNumber(answer.likes, 0),
        likedByCurrentUser: Boolean(answer.likedByCurrentUser)
      }))
      const safeFollowUps = safeArray(question.followUps, [])
      const category = safeText(question.category, 'other')

      return {
        ...question,
        title: safeTitle,
        content: safeContent,
        preview: safeContent.length > 60 ? `${safeContent.slice(0, 60)}...` : safeContent,
        author: safeAuthor,
        category,
        categoryText: CATEGORY_LABEL_MAP[category] || CATEGORY_LABEL_MAP.other,
        tags: safeTags,
        images: safeImages,
        answers: safeAnswers,
        followUps: safeFollowUps,
        likes: safeNumber(question.likes, 0),
        views: safeNumber(question.views, 0),
        mentorIds: safeArray(question.mentorIds, []),
        isContentMissing: !rawContent,
        createdAtMs: Date.parse(question.createdAt) || 0,
        answerCount: safeAnswers.length,
        hotScore: safeNumber(question.views, 0) + safeAnswers.length * 20 + safeNumber(question.likes, 0) * 2
      }
    })
  })

  const filteredAndSortedQuestions = computed(() => {
    let pool = normalizedQuestions.value

    if (selectedSeniorId.value) {
      pool = pool.filter((question) => question.mentorIds.includes(selectedSeniorId.value))
    }

    if (listFilter.value === 'unsolved') {
      pool = pool.filter((question) => !question.solved)
    }

    if (listFilter.value === 'solved') {
      pool = pool.filter((question) => question.solved)
    }

    if (listFilter.value === 'hot') {
      pool = pool.filter((question) => question.hotScore >= 100)
    }

    const sorted = [...pool].sort((left, right) => {
      if (listSort.value === 'likes') {
        return right.likes - left.likes
      }

      return right.createdAtMs - left.createdAtMs
    })

    return sorted
  })

  const paginatedQuestions = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    const end = start + pageSize
    return filteredAndSortedQuestions.value.slice(start, end)
  })

  const listState = computed(() => {
    return paginatedQuestions.value.length > 0 ? UI_STATES.SUCCESS : UI_STATES.EMPTY
  })

  const totalQuestions = computed(() => normalizedQuestions.value.length)

  const solvedRate = computed(() => {
    if (!normalizedQuestions.value.length) {
      return 0
    }

    const solvedCount = normalizedQuestions.value.filter((question) => question.solved).length
    return Math.round((solvedCount / normalizedQuestions.value.length) * 100)
  })

  const avgRating = computed(() => {
    if (!normalizedSeniors.value.length) {
      return '0.0'
    }

    const total = normalizedSeniors.value.reduce((sum, senior) => sum + senior.rating, 0)
    return (total / normalizedSeniors.value.length).toFixed(1)
  })

  const stats = computed(() => ({
    seniorCount: normalizedSeniors.value.length,
    totalQuestions: totalQuestions.value,
    solvedRate: solvedRate.value,
    avgRating: avgRating.value
  }))

  const activeQuestion = computed(() => {
    if (!activeQuestionId.value) {
      return null
    }

    return normalizedQuestions.value.find((question) => question.id === activeQuestionId.value) || null
  })

  watch([seniorFilter, selectedSeniorId, listFilter, listSort], () => {
    currentPage.value = 1
  })

  watch(filteredAndSortedQuestions, (items) => {
    const totalPages = Math.max(Math.ceil(items.length / pageSize), 1)
    if (currentPage.value > totalPages) {
      currentPage.value = totalPages
    }
  })

  function setSeniorFilter(filter) {
    seniorFilter.value = filter
  }

  function selectSenior(seniorId) {
    selectedSeniorId.value = selectedSeniorId.value === seniorId ? '' : seniorId
  }

  function setListFilter(filter) {
    listFilter.value = filter
  }

  function setListSort(sort) {
    listSort.value = sort
  }

  function setCurrentPage(page) {
    currentPage.value = page
  }

  function openAskDialog() {
    askDialogVisible.value = true
  }

  function closeAskDialog() {
    askDialogVisible.value = false
  }

  async function publishQuestion(formValue) {
    askState.value = UI_STATES.LOADING

    const payload = {
      title: safeText(formValue.title, ''),
      category: safeText(formValue.category, ''),
      content: safeText(formValue.content, ''),
      tags: safeArray(formValue.tags, []),
      images: normalizeImages(formValue.images)
    }

    if (!payload.title || !payload.category || !payload.content) {
      askState.value = UI_STATES.ERROR
      return { ok: false, message: '请先完整填写标题、分类和问题详情。' }
    }

    await delay(200)

    const id = `q-${Date.now()}`
    const nextQuestion = {
      id,
      title: payload.title,
      content: payload.content,
      category: payload.category,
      tags: payload.tags,
      authorId: currentUser.value.id,
      author: currentUser.value.name,
      mentorIds: selectedSeniorId.value ? [selectedSeniorId.value] : [],
      createdAt: '刚刚',
      views: 0,
      likes: 0,
      solved: false,
      acceptedAnswerId: '',
      images: payload.images,
      answers: [],
      followUps: []
    }

    questions.value = [nextQuestion, ...questions.value]
    currentPage.value = 1
    askState.value = UI_STATES.SUCCESS

    return { ok: true, message: '发布成功，已推送到答疑列表。' }
  }

  function resetAskState() {
    askState.value = UI_STATES.IDLE
  }

  async function openDetail(questionId) {
    activeQuestionId.value = questionId
    detailDialogVisible.value = true
    await loadDetail(questionId)
  }

  function closeDetailDialog() {
    detailDialogVisible.value = false
    detailState.value = UI_STATES.IDLE
    detailError.value = ''
  }

  async function loadDetail(questionId) {
    detailState.value = UI_STATES.LOADING
    detailError.value = ''

    await delay(180)

    const question = normalizedQuestions.value.find((item) => item.id === questionId)
    if (!question) {
      detailState.value = UI_STATES.ERROR
      detailError.value = '问题不存在或已被移除，请刷新列表后重试。'
      return
    }

    if (question.isContentMissing && !question.answers.length) {
      detailState.value = UI_STATES.EMPTY
      return
    }

    detailState.value = UI_STATES.SUCCESS
  }

  async function retryLoadDetail() {
    if (!activeQuestionId.value) {
      return
    }

    await loadDetail(activeQuestionId.value)
  }

  function toggleAnswerLike(answerId) {
    const question = questions.value.find((item) => item.id === activeQuestionId.value)
    if (!question) {
      return false
    }

    const answer = safeArray(question.answers).find((item) => item.id === answerId)
    if (!answer) {
      return false
    }

    if (answer.likedByCurrentUser) {
      answer.likedByCurrentUser = false
      answer.likes = Math.max(safeNumber(answer.likes, 0) - 1, 0)
      return true
    }

    answer.likedByCurrentUser = true
    answer.likes = safeNumber(answer.likes, 0) + 1
    return true
  }

  function acceptAnswer(answerId) {
    const question = questions.value.find((item) => item.id === activeQuestionId.value)
    if (!question) {
      return { ok: false, message: '当前问题不存在。' }
    }

    if (question.authorId !== currentUser.value.id) {
      return { ok: false, message: '只有提问者可以执行采纳操作。' }
    }

    if (question.acceptedAnswerId) {
      return { ok: false, message: '该问题已采纳，不能重复采纳。' }
    }

    const answer = safeArray(question.answers).find((item) => item.id === answerId)
    if (!answer) {
      return { ok: false, message: '目标回答不存在。' }
    }

    question.acceptedAnswerId = answerId
    question.solved = true

    return { ok: true, message: '已采纳该回答，问题状态已更新为已解决。' }
  }

  function submitFollowUp(rawContent) {
    const question = questions.value.find((item) => item.id === activeQuestionId.value)
    if (!question) {
      return { ok: false, message: '未找到当前问题。' }
    }

    const content = safeText(rawContent, '')
    if (!content) {
      return { ok: false, message: '追问内容不能为空。' }
    }

    question.followUps = [
      ...safeArray(question.followUps),
      {
        id: `f-${Date.now()}`,
        authorId: currentUser.value.id,
        author: currentUser.value.name,
        time: '刚刚',
        content
      }
    ]

    return { ok: true, message: '追问已提交，等待学长学姐补充回复。' }
  }

  return {
    TEXT,
    UI_STATES,
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
