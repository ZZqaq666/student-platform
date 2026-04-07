// 学习进度模块

/**
 * 计算知识点掌握程度
 * @param {string} knowledgePoint - 知识点名称
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @returns {number} 掌握程度（0-1）
 */
export const calculateKnowledgeMastery = (knowledgePoint, bookId, userLearningProgress) => {
  const bookProgress = userLearningProgress.books[bookId] || {}
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

/**
 * 计算习题与知识点的相关性
 * @param {Object} exercise - 习题对象
 * @param {string} targetKnowledgePoint - 目标知识点
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @param {Function} getRelatedKnowledgePoints - 获取相关知识点的函数
 * @returns {number} 相关性分数
 */
export const calculateExerciseRelevance = (exercise, targetKnowledgePoint, bookId, userLearningProgress, getRelatedKnowledgePoints) => {
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
    const mastery = calculateKnowledgeMastery(point, bookId, userLearningProgress)
    relevance += (1 - mastery) * 0.3
  })
  
  return relevance
}

/**
 * 计算习题难度
 * @param {Object} exercise - 习题对象
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @returns {Object} 难度信息
 */
export const calculateExerciseDifficulty = (exercise, bookId, userLearningProgress) => {
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
    const mastery = calculateKnowledgeMastery(point, bookId, userLearningProgress)
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

/**
 * 根据相关性排序习题
 * @param {Array} exercises - 习题列表
 * @param {string} targetKnowledgePoint - 目标知识点
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @param {Function} getRelatedKnowledgePoints - 获取相关知识点的函数
 * @returns {Array} 排序后的习题列表
 */
export const sortExercisesByRelevance = (exercises, targetKnowledgePoint, bookId, userLearningProgress, getRelatedKnowledgePoints) => {
  return exercises.sort((a, b) => {
    // 计算习题与目标知识点的相关性
    const relevanceA = calculateExerciseRelevance(a, targetKnowledgePoint, bookId, userLearningProgress, getRelatedKnowledgePoints)
    const relevanceB = calculateExerciseRelevance(b, targetKnowledgePoint, bookId, userLearningProgress, getRelatedKnowledgePoints)
    
    return relevanceB - relevanceA
  })
}

/**
 * 动态调整习题难度
 * @param {number} exerciseId - 习题ID
 * @param {boolean} isCorrect - 是否正确
 * @param {string} bookId - 书籍ID
 * @param {Array} exercises - 习题列表
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @returns {Object} 难度信息
 */
export const adjustExerciseDifficulty = (exerciseId, isCorrect, bookId, exercises, userLearningProgress) => {
  // 查找习题
  const exercise = exercises.find(e => e.id === exerciseId)
  if (!exercise) return
  
  // 更新用户学习进度
  updateLearningProgress(exerciseId, exercise.knowledgePoints, isCorrect, bookId, userLearningProgress)
  
  // 重新计算难度
  const difficultyInfo = calculateExerciseDifficulty(exercise, bookId, userLearningProgress)
  
  // 更新习题难度
  exercise.adjustedDifficulty = difficultyInfo.label
  exercise.difficultyLevel = difficultyInfo.level
  
  return difficultyInfo
}

/**
 * 推荐习题
 * @param {string} nodeName - 节点名称
 * @param {string} bookId - 书籍ID
 * @param {Object} params - 推荐参数
 * @param {Array} exercises - 习题列表
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @param {Function} filterExercisesByDifficulty - 根据难度筛选习题的函数
 * @param {Function} sortExercisesByRelevance - 根据相关性排序习题的函数
 * @param {Function} getRelatedKnowledgePoints - 获取相关知识点的函数
 * @returns {Array} 推荐习题列表
 */
export const recommendExercises = (nodeName, bookId, params = {}, exercises, userLearningProgress, filterExercisesByDifficulty, sortExercisesByRelevance, getRelatedKnowledgePoints) => {
  const { difficulty = 'balanced', focus = 'weakness', quantity = 5 } = params
  
  // 获取所有相关习题
  let allExercises = exercises
  
  // 根据难度筛选
  allExercises = filterExercisesByDifficulty(allExercises, difficulty)
  
  // 根据相关性排序
  allExercises = sortExercisesByRelevance(allExercises, nodeName, bookId, userLearningProgress, getRelatedKnowledgePoints)
  
  // 根据聚焦策略调整排序
  if (focus === 'weakness') {
    allExercises.sort((a, b) => {
      const weakScoreA = a.knowledgePoints.reduce((sum, point) => {
        return sum + (1 - calculateKnowledgeMastery(point, bookId, userLearningProgress))
      }, 0)
      const weakScoreB = b.knowledgePoints.reduce((sum, point) => {
        return sum + (1 - calculateKnowledgeMastery(point, bookId, userLearningProgress))
      }, 0)
      return weakScoreB - weakScoreA
    })
  } else if (focus === 'strength') {
    allExercises.sort((a, b) => {
      const strongScoreA = a.knowledgePoints.reduce((sum, point) => {
        return sum + calculateKnowledgeMastery(point, bookId, userLearningProgress)
      }, 0)
      const strongScoreB = b.knowledgePoints.reduce((sum, point) => {
        return sum + calculateKnowledgeMastery(point, bookId, userLearningProgress)
      }, 0)
      return strongScoreB - strongScoreA
    })
  }
  
  // 为每个推荐习题计算调整后的难度和推荐理由
  allExercises.forEach(exercise => {
    const difficultyInfo = calculateExerciseDifficulty(exercise, bookId, userLearningProgress)
    exercise.adjustedDifficulty = difficultyInfo.label
    exercise.difficultyLevel = difficultyInfo.level
    
    // 生成推荐理由
    exercise.recommendationReason = generateRecommendationReason(exercise, nodeName, bookId, userLearningProgress)
  })
  
  // 限制数量
  return allExercises.slice(0, quantity)
}

/**
 * 生成推荐理由
 * @param {Object} exercise - 习题对象
 * @param {string} targetKnowledgePoint - 目标知识点
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @returns {string} 推荐理由
 */
export const generateRecommendationReason = (exercise, targetKnowledgePoint, bookId, userLearningProgress) => {
  // 计算知识点掌握程度
  const mastery = exercise.knowledgePoints.reduce((sum, point) => {
    return sum + calculateKnowledgeMastery(point, bookId, userLearningProgress)
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

/**
 * 分析学习行为
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 * @returns {Object} 学习行为分析结果
 */
export const analyzeLearningBehavior = (bookId, userLearningProgress) => {
  const bookProgress = userLearningProgress.books[bookId] || {}
  
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

/**
 * 更新学习进度
 * @param {number} exerciseId - 习题ID
 * @param {Array} knowledgePoints - 知识点列表
 * @param {boolean} isCorrect - 是否正确
 * @param {string} bookId - 书籍ID
 * @param {Object} userLearningProgress - 用户学习进度数据
 */
export const updateLearningProgress = (exerciseId, knowledgePoints, isCorrect, bookId, userLearningProgress) => {
  if (!userLearningProgress.books[bookId]) {
    userLearningProgress.books[bookId] = {}
  }
  
  const bookProgress = userLearningProgress.books[bookId]
  
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
  
  userLearningProgress.lastUpdated = new Date().toISOString()
  
  // 保存到本地存储
  localStorage.setItem('userLearningProgress', JSON.stringify(userLearningProgress))
}

/**
 * 加载学习进度
 * @returns {Object} 用户学习进度数据
 */
export const loadLearningProgress = () => {
  const savedProgress = localStorage.getItem('userLearningProgress')
  if (savedProgress) {
    try {
      return JSON.parse(savedProgress)
    } catch (error) {
      console.error('加载学习进度失败:', error)
      return {
        books: {},
        lastUpdated: new Date().toISOString()
      }
    }
  }
  return {
    books: {},
    lastUpdated: new Date().toISOString()
  }
}
