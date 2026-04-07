// 错题本 Mock 数据

// 生成随机日期
const randomDate = (start, end) => {
  return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()))
}

// 格式化日期
const formatDate = (date) => {
  return date.toISOString().slice(0, 19).replace('T', ' ')
}

// 生成模拟习题数据
const generateMockExercise = (id, subject) => {
  const exercises = {
    '计算机网络': [
      {
        id: 1001,
        title: 'TCP三次握手',
        content: 'TCP建立连接时，三次握手的目的是什么？',
        type: '选择题',
        options: ['防止失效的连接请求报文段突然又传送到服务器', '确保双方都能确认对方的接收和发送能力', '协商双方的初始序列号', '以上都是'],
        correctAnswer: 3,
        answer: 'D. 以上都是',
        analysis: 'TCP三次握手的主要目的包括：1. 防止失效的连接请求报文段突然又传送到服务器；2. 确保双方都能确认对方的接收和发送能力；3. 协商双方的初始序列号。',
        knowledgePoint: '计算机网络/TCP协议'
      },
      {
        id: 1002,
        title: 'HTTP状态码',
        content: 'HTTP状态码404表示什么？',
        type: '选择题',
        options: ['服务器内部错误', '请求的资源不存在', '请求被拒绝', '临时重定向'],
        correctAnswer: 1,
        answer: 'B. 请求的资源不存在',
        analysis: 'HTTP 404状态码表示客户端请求的资源在服务器上不存在。',
        knowledgePoint: '计算机网络/HTTP协议'
      },
      {
        id: 1003,
        title: 'IP地址分类',
        content: '以下哪个IP地址属于C类地址？',
        type: '选择题',
        options: ['10.0.0.1', '172.16.0.1', '192.168.1.1', '127.0.0.1'],
        correctAnswer: 2,
        answer: 'C. 192.168.1.1',
        analysis: 'C类IP地址范围是192.0.0.0到223.255.255.255，192.168.1.1属于C类私有地址。',
        knowledgePoint: '计算机网络/IP协议'
      }
    ],
    '操作系统': [
      {
        id: 2001,
        title: '进程调度',
        content: '在进程调度中，时间片轮转调度算法的主要优点是什么？',
        type: '选择题',
        options: ['响应时间快', '吞吐量大', '公平性好', '实现简单'],
        correctAnswer: 2,
        answer: 'C. 公平性好',
        analysis: '时间片轮转调度算法为每个进程分配相同的时间片，保证了公平性。',
        knowledgePoint: '操作系统/进程调度'
      },
      {
        id: 2002,
        title: '死锁条件',
        content: '产生死锁的四个必要条件不包括以下哪项？',
        type: '选择题',
        options: ['互斥条件', '请求与保持条件', '抢占条件', '循环等待条件'],
        correctAnswer: 2,
        answer: 'C. 抢占条件',
        analysis: '死锁的四个必要条件是：互斥、请求与保持、不可剥夺（非抢占）、循环等待。',
        knowledgePoint: '操作系统/死锁'
      },
      {
        id: 2003,
        title: '内存管理',
        content: '分页存储管理中，页面大小对系统性能的影响是？',
        type: '选择题',
        options: ['页面越大越好', '页面越小越好', '页面大小需要权衡', '页面大小不影响性能'],
        correctAnswer: 2,
        answer: 'C. 页面大小需要权衡',
        analysis: '页面大小需要权衡：页面大可减少页表大小，但会增加内部碎片；页面小可减少内部碎片，但会增加页表大小和缺页次数。',
        knowledgePoint: '操作系统/内存管理'
      }
    ],
    '数据库': [
      {
        id: 3001,
        title: 'SQL查询',
        content: 'SQL语句中，用于删除表中所有数据但保留表结构的是？',
        type: '选择题',
        options: ['DELETE', 'DROP', 'TRUNCATE', 'REMOVE'],
        correctAnswer: 2,
        answer: 'C. TRUNCATE',
        analysis: 'TRUNCATE删除表中所有数据但保留表结构，比DELETE更快，但不能回滚。',
        knowledgePoint: '数据库/SQL'
      },
      {
        id: 3002,
        title: '事务隔离级别',
        content: '以下哪个隔离级别可以防止幻读？',
        type: '选择题',
        options: ['读未提交', '读已提交', '可重复读', '串行化'],
        correctAnswer: 3,
        answer: 'D. 串行化',
        analysis: '串行化隔离级别可以防止脏读、不可重复读和幻读，但并发性能最低。',
        knowledgePoint: '数据库/事务'
      },
      {
        id: 3003,
        title: '索引类型',
        content: 'B+树索引相比B树索引的主要优势是？',
        type: '选择题',
        options: ['查询速度更快', '更适合范围查询', '占用空间更小', '插入速度更快'],
        correctAnswer: 1,
        answer: 'B. 更适合范围查询',
        analysis: 'B+树的所有数据都在叶子节点，且叶子节点通过指针连接，非常适合范围查询。',
        knowledgePoint: '数据库/索引'
      }
    ],
    '数据结构': [
      {
        id: 4001,
        title: '栈的应用',
        content: '以下哪种数据结构最适合实现函数调用栈？',
        type: '选择题',
        options: ['队列', '栈', '链表', '数组'],
        correctAnswer: 1,
        answer: 'B. 栈',
        analysis: '栈的后进先出(LIFO)特性非常适合实现函数调用栈，函数调用时入栈，返回时出栈。',
        knowledgePoint: '数据结构/栈'
      },
      {
        id: 4002,
        title: '二叉树遍历',
        content: '对二叉树进行中序遍历的结果是：左子树 -> 根节点 -> 右子树，那么前序遍历的顺序是？',
        type: '选择题',
        options: ['根节点 -> 左子树 -> 右子树', '左子树 -> 右子树 -> 根节点', '根节点 -> 右子树 -> 左子树', '右子树 -> 根节点 -> 左子树'],
        correctAnswer: 0,
        answer: 'A. 根节点 -> 左子树 -> 右子树',
        analysis: '前序遍历的顺序是：根节点 -> 左子树 -> 右子树。',
        knowledgePoint: '数据结构/二叉树'
      },
      {
        id: 4003,
        title: '排序算法',
        content: '快速排序的平均时间复杂度是？',
        type: '选择题',
        options: ['O(n)', 'O(n log n)', 'O(n²)', 'O(log n)'],
        correctAnswer: 1,
        answer: 'B. O(n log n)',
        analysis: '快速排序的平均时间复杂度为O(n log n)，最坏情况下为O(n²)。',
        knowledgePoint: '数据结构/排序'
      }
    ],
    '高等数学': [
      {
        id: 5001,
        title: '导数计算',
        content: '函数 f(x) = x³ 的导数是？',
        type: '选择题',
        options: ['3x²', 'x²', '3x', 'x³'],
        correctAnswer: 0,
        answer: 'A. 3x²',
        analysis: '根据幂函数求导法则，(xⁿ)\' = n·xⁿ⁻¹，所以 (x³)\' = 3x²。',
        knowledgePoint: '高等数学/导数'
      },
      {
        id: 5002,
        title: '定积分',
        content: '∫₀¹ x dx 的值是？',
        type: '选择题',
        options: ['0', '0.5', '1', '2'],
        correctAnswer: 1,
        answer: 'B. 0.5',
        analysis: '∫₀¹ x dx = [x²/2]₀¹ = 1/2 - 0 = 0.5',
        knowledgePoint: '高等数学/积分'
      },
      {
        id: 5003,
        title: '极限计算',
        content: 'lim(x→0) sin(x)/x 的值是？',
        type: '选择题',
        options: ['0', '1', '∞', '不存在'],
        correctAnswer: 1,
        answer: 'B. 1',
        analysis: '这是一个重要极限，lim(x→0) sin(x)/x = 1。',
        knowledgePoint: '高等数学/极限'
      }
    ]
  }

  const subjectExercises = exercises[subject] || exercises['计算机网络']
  const exercise = subjectExercises.find(e => e.id === id) || subjectExercises[0]
  return exercise
}

// 生成模拟错题数据
const generateMockWrongBookItem = (index) => {
  const subjects = ['计算机网络', '操作系统', '数据库', '数据结构', '高等数学']
  const subject = subjects[index % subjects.length]
  
  const errorTypes = ['CONCEPT', 'CALCULATION', 'CARELESS', 'OTHER']
  const difficulties = ['EASY', 'MEDIUM', 'HARD']
  const masterStatuses = ['NOT_MASTERED', 'MASTERING', 'MASTERED']
  const priorities = ['HIGH', 'MEDIUM', 'LOW']
  
  const exerciseId = 1001 + (index % 15)
  const exercise = generateMockExercise(exerciseId, subject)
  
  const createdAt = randomDate(new Date(2024, 0, 1), new Date())
  const lastAttemptAt = randomDate(createdAt, new Date())
  const nextReviewAt = randomDate(new Date(), new Date(Date.now() + 7 * 24 * 60 * 60 * 1000))
  
  const wrongCount = Math.floor(Math.random() * 5) + 1
  const correctCount = Math.floor(Math.random() * wrongCount)
  
  return {
    id: index + 1,
    userId: 1,
    exerciseId: exerciseId,
    exercise: exercise,
    wrongCount: wrongCount,
    correctCount: correctCount,
    lastAttemptAt: formatDate(lastAttemptAt),
    nextReviewAt: formatDate(nextReviewAt),
    masterStatus: masterStatuses[Math.floor(Math.random() * masterStatuses.length)],
    errorType: errorTypes[Math.floor(Math.random() * errorTypes.length)],
    difficulty: difficulties[Math.floor(Math.random() * difficulties.length)],
    priority: priorities[Math.floor(Math.random() * priorities.length)],
    subject: subject,
    tags: ['重点', '易错'].slice(0, Math.floor(Math.random() * 2) + 1).join(','),
    notes: index % 3 === 0 ? '这道题需要重点复习，容易出错' : '',
    userAnswer: index % 2 === 0 ? 'A' : 'B',
    createdAt: formatDate(createdAt),
    updatedAt: formatDate(lastAttemptAt)
  }
}

// 生成错题列表
export const generateMockWrongBookList = (count = 20) => {
  return Array.from({ length: count }, (_, i) => generateMockWrongBookItem(i))
}

// 生成统计数据
export const generateMockStats = () => {
  return {
    totalWrongCount: 45,
    dueTodayCount: 8,
    masteringCount: 15,
    masteredCount: 12,
    subjectStats: [
      { subject: '计算机网络', count: 12, masteredCount: 4 },
      { subject: '操作系统', count: 10, masteredCount: 3 },
      { subject: '数据库', count: 8, masteredCount: 2 },
      { subject: '数据结构', count: 9, masteredCount: 2 },
      { subject: '高等数学', count: 6, masteredCount: 1 }
    ],
    errorTypeStats: [
      { type: 'CONCEPT', count: 18, label: '概念错误' },
      { type: 'CALCULATION', count: 12, label: '计算错误' },
      { type: 'CARELESS', count: 10, label: '粗心大意' },
      { type: 'OTHER', count: 5, label: '其他' }
    ],
    difficultyStats: [
      { difficulty: 'EASY', count: 10, label: '简单' },
      { difficulty: 'MEDIUM', count: 25, label: '中等' },
      { difficulty: 'HARD', count: 10, label: '困难' }
    ],
    weeklyTrend: [
      { date: '2024-01-01', added: 5, reviewed: 3, mastered: 1 },
      { date: '2024-01-02', added: 3, reviewed: 5, mastered: 2 },
      { date: '2024-01-03', added: 2, reviewed: 4, mastered: 1 },
      { date: '2024-01-04', added: 4, reviewed: 6, mastered: 2 },
      { date: '2024-01-05', added: 1, reviewed: 3, mastered: 1 },
      { date: '2024-01-06', added: 3, reviewed: 4, mastered: 0 },
      { date: '2024-01-07', added: 2, reviewed: 5, mastered: 2 }
    ]
  }
}

// 存储模拟数据
let mockWrongBookList = generateMockWrongBookList(30)
let mockStats = generateMockStats()

// 筛选错题列表
export const filterMockWrongBookList = (filter) => {
  let result = [...mockWrongBookList]

  if (filter.keyword) {
    const keyword = filter.keyword.toLowerCase()
    result = result.filter(item => 
      item.exercise?.title?.toLowerCase().includes(keyword) ||
      item.exercise?.content?.toLowerCase().includes(keyword) ||
      item.notes?.toLowerCase().includes(keyword)
    )
  }

  if (filter.subject) {
    result = result.filter(item => item.subject === filter.subject)
  }

  if (filter.difficulty) {
    result = result.filter(item => item.difficulty === filter.difficulty)
  }

  if (filter.errorType) {
    result = result.filter(item => item.errorType === filter.errorType)
  }

  if (filter.masterStatus) {
    result = result.filter(item => item.masterStatus === filter.masterStatus)
  }

  if (filter.priority) {
    result = result.filter(item => item.priority === filter.priority)
  }

  if (filter.showDueReview) {
    const today = new Date().toISOString().slice(0, 10)
    result = result.filter(item => {
      if (!item.nextReviewAt) return false
      return item.nextReviewAt.slice(0, 10) <= today
    })
  }

  // 分页
  const pageNum = filter.pageNum || 1
  const pageSize = filter.pageSize || 20
  const start = (pageNum - 1) * pageSize
  const end = start + pageSize

  return {
    list: result.slice(start, end),
    total: result.length
  }
}

// 更新错题
export const updateMockWrongBookItem = (id, data) => {
  const index = mockWrongBookList.findIndex(item => item.id === id)
  if (index !== -1) {
    mockWrongBookList[index] = {
      ...mockWrongBookList[index],
      ...data,
      updatedAt: formatDate(new Date())
    }
    return mockWrongBookList[index]
  }
  return null
}

// 删除错题
export const deleteMockWrongBookItem = (id) => {
  const index = mockWrongBookList.findIndex(item => item.id === id)
  if (index !== -1) {
    mockWrongBookList.splice(index, 1)
    // 更新统计
    mockStats = generateMockStats()
    mockStats.totalWrongCount = mockWrongBookList.length
    return true
  }
  return false
}

// 重做错题
export const redoMockWrongBookItem = (id, userAnswer, isCorrect) => {
  const index = mockWrongBookList.findIndex(item => item.id === id)
  if (index !== -1) {
    const item = mockWrongBookList[index]
    item.wrongCount = isCorrect ? item.wrongCount : item.wrongCount + 1
    item.correctCount = isCorrect ? item.correctCount + 1 : item.correctCount
    item.lastAttemptAt = formatDate(new Date())
    item.updatedAt = formatDate(new Date())
    
    // 更新掌握状态
    if (isCorrect && item.correctCount >= 3) {
      item.masterStatus = 'MASTERED'
    } else if (item.correctCount > 0) {
      item.masterStatus = 'MASTERING'
    }
    
    return item
  }
  return null
}

// 添加错题
export const addMockWrongBookItem = (data) => {
  const newId = Math.max(...mockWrongBookList.map(item => item.id)) + 1
  const exercise = generateMockExercise(data.exerciseId, data.subject || '计算机网络')
  
  const newItem = {
    id: newId,
    userId: data.userId || 1,
    exerciseId: data.exerciseId,
    exercise: exercise,
    wrongCount: 1,
    correctCount: 0,
    lastAttemptAt: formatDate(new Date()),
    nextReviewAt: formatDate(new Date(Date.now() + 24 * 60 * 60 * 1000)),
    masterStatus: 'NOT_MASTERED',
    errorType: data.errorType || 'OTHER',
    difficulty: data.difficulty || 'MEDIUM',
    priority: data.priority || 'MEDIUM',
    subject: data.subject || '计算机网络',
    tags: data.tags || '',
    notes: data.notes || '',
    userAnswer: data.userAnswer || '',
    createdAt: formatDate(new Date()),
    updatedAt: formatDate(new Date())
  }
  
  mockWrongBookList.unshift(newItem)
  mockStats.totalWrongCount = mockWrongBookList.length
  
  return newItem
}

// 导出错题
export const exportMockWrongBook = (format = 'json') => {
  if (format === 'json') {
    return JSON.stringify(mockWrongBookList, null, 2)
  } else {
    // CSV 格式
    const headers = ['ID', '科目', '题目', '错误类型', '难度', '掌握状态', '错误次数', '正确次数', '创建时间']
    const rows = mockWrongBookList.map(item => [
      item.id,
      item.subject,
      item.exercise?.title || '',
      item.errorType,
      item.difficulty,
      item.masterStatus,
      item.wrongCount,
      item.correctCount,
      item.createdAt
    ])
    
    return [headers.join(','), ...rows.map(row => row.join(','))].join('\n')
  }
}

// 获取科目选项
export const getMockSubjectOptions = () => {
  return ['计算机网络', '操作系统', '数据库', '数据结构', '高等数学']
}

// 获取单个错题详情
export const getMockWrongBookItem = (id) => {
  return mockWrongBookList.find(item => item.id === parseInt(id)) || null
}
