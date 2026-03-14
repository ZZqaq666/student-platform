<template>
  <div class="practice-page">
    <!-- 顶部导航栏 -->
    <div class="practice-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span>
          返回
        </button>
        <span class="header-title">{{ currentExercise?.title || '练习模式' }}</span>
      </div>
      <div class="header-right">
        <div class="progress-info">
          <span class="progress-text">进度 {{ currentIndex + 1 }} / {{ exercises.length }}</span>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>
        <button class="exit-btn" @click="exitPractice">退出练习</button>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="practice-content">
      <!-- 左侧题目区 -->
      <div class="question-panel">
        <div class="question-card">
          <!-- 题目头部 -->
          <div class="question-header">
            <span class="question-type">{{ currentExercise?.type || '选择题' }}</span>
            <span class="question-difficulty" :class="currentExercise?.difficulty">
              {{ currentExercise?.difficultyText }}
            </span>
            <span class="question-score">{{ currentExercise?.score || 10 }}分</span>
          </div>

          <!-- 题目内容 -->
          <div class="question-body">
            <div class="question-text">
              <span class="question-number">{{ currentIndex + 1 }}.</span>
              {{ currentExercise?.question }}
            </div>

            <!-- 选择题选项 -->
            <div v-if="currentExercise?.type === '选择题'" class="options-list">
              <div
                v-for="(option, index) in currentExercise?.options || []"
                :key="index"
                class="option-item"
                :class="{
                  selected: selectedOption === index,
                  correct: showAnswer && index === currentExercise?.correctAnswer,
                  wrong: showAnswer && selectedOption === index && index !== currentExercise?.correctAnswer
                }"
                @click="selectOption(index)"
              >
                <span class="option-label">{{ ['A', 'B', 'C', 'D'][index] }}.</span>
                <span class="option-text">{{ option }}</span>
                <span v-if="showAnswer && index === currentExercise?.correctAnswer" class="option-icon correct">✓</span>
                <span v-if="showAnswer && selectedOption === index && index !== currentExercise?.correctAnswer" class="option-icon wrong">✗</span>
              </div>
            </div>

            <!-- 填空题输入 -->
            <div v-else-if="currentExercise?.type === '填空题'" class="fill-blank-area">
              <input
                v-model="fillAnswer"
                type="text"
                placeholder="请输入答案"
                class="fill-input"
                :disabled="showAnswer"
              />
            </div>

            <!-- 解答题输入 -->
            <div v-else-if="currentExercise?.type === '解答题'" class="essay-area">
              <textarea
                v-model="essayAnswer"
                placeholder="请在此输入解答过程..."
                class="essay-input"
                rows="8"
                :disabled="showAnswer"
              ></textarea>
            </div>
          </div>

          <!-- 答题后的解析 -->
          <div v-if="showAnswer" class="answer-analysis">
            <div class="analysis-header">
              <span class="analysis-title">答案解析</span>
              <span class="answer-result" :class="isCorrect ? 'correct' : 'wrong'">
                {{ isCorrect ? '回答正确' : '回答错误' }}
              </span>
            </div>
            <div class="analysis-content">
              <div class="user-answer" :class="isCorrect ? 'correct' : 'wrong'">
                <span class="label">我的答案：</span>
                <span class="value">{{ getUserAnswerText() }}</span>
              </div>
              <div class="correct-answer">
                <span class="label">正确答案：</span>
                <span class="value">{{ currentExercise?.correctAnswerText }}</span>
              </div>
              <div class="analysis-text">
                <span class="label">解析：</span>
                <p>{{ currentExercise?.analysis }}</p>
              </div>
              <div class="knowledge-point">
                <span class="label">知识点：</span>
                <span class="tag">{{ currentExercise?.knowledgePoint }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="question-actions">
          <button
            class="action-btn prev-btn"
            :disabled="currentIndex === 0"
            @click="prevQuestion"
          >
            上一题
          </button>
          <button
            v-if="!showAnswer && currentIndex < exercises.length - 1"
            class="action-btn next-btn"
            @click="saveAndNext"
          >
            下一题
          </button>
          <button
            v-else-if="!showAnswer"
            class="action-btn submit-btn"
            @click="submitAllAnswers"
          >
            提交练习
          </button>
          <button
            v-else-if="showAnswer && currentIndex < exercises.length - 1"
            class="action-btn next-btn"
            @click="nextQuestion"
          >
            下一题
          </button>
        </div>
      </div>

      <!-- 右侧信息面板 -->
      <div class="info-panel">
        <!-- 答题卡 -->
        <div class="answer-card">
          <h3 class="panel-title">答题卡</h3>
          <div class="question-grid">
            <div
              v-for="(exercise, index) in exercises"
              :key="index"
              v-if="!showResults || viewMode === 'all' || (viewMode === 'wrong' && showResults && !results[index]?.isCorrect)"
              class="question-number-btn"
              :class="{
                current: currentIndex === index,
                answered: answers[index] !== undefined,
                correct: showResults && results[index]?.isCorrect,
                wrong: showResults && !results[index]?.isCorrect
              }"
              @click="jumpToQuestion(index)"
            >
              {{ index + 1 }}
            </div>
          </div>
        </div>

        <!-- 练习统计 -->
        <div class="practice-stats">
          <h3 class="panel-title">练习统计</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ answeredCount }}</span>
              <span class="stat-label">已答</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ correctCount }}</span>
              <span class="stat-label">正确</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ accuracyRate }}%</span>
              <span class="stat-label">正确率</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ formatTime(practiceTime) }}</span>
              <span class="stat-label">用时</span>
            </div>
          </div>
        </div>

        <!-- 快捷操作 - 只在提交答案后显示 -->
        <div v-if="showAnswer" class="quick-actions">
          <h3 class="panel-title">快捷操作</h3>
          <div class="action-list">
            <button class="quick-btn wrong-book-btn" @click="addToWrongBook">
              <span class="btn-icon">📚</span>
              <span class="btn-text">加入错题本</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 退出确认弹窗 -->
    <div v-if="showExitConfirm" class="confirm-modal">
      <div class="confirm-content">
        <h3>确认退出?</h3>
        <p>退出后练习进度将保存，您可以随时继续练习</p>
        <div class="confirm-actions">
          <button class="confirm-btn cancel" @click="showExitConfirm = false">继续练习</button>
          <button class="confirm-btn confirm" @click="confirmExit">确认退出</button>
        </div>
      </div>
    </div>

    <!-- 提交确认弹窗 -->
    <div v-if="showSubmitConfirm" class="confirm-modal">
      <div class="confirm-content">
        <div class="confirm-icon">📝</div>
        <h3>{{ submitConfirmTitle }}</h3>
        <p>{{ submitConfirmMessage }}</p>
        <div class="confirm-actions">
          <button class="confirm-btn cancel" @click="handleSubmitCancel">
            {{ submitConfirmCancelText }}
          </button>
          <button class="confirm-btn confirm" @click="handleSubmitConfirm">
            {{ submitConfirmConfirmText }}
          </button>
        </div>
      </div>
    </div>

    <!-- 练习完成弹窗 -->
    <div v-if="showCompleteModal" class="complete-modal">
      <div class="complete-content">
        <div class="complete-icon">🎉</div>
        <h3>练习完成!</h3>
        <div class="complete-stats">
          <div class="stat-row">
            <span class="stat-label">总题数</span>
            <span class="stat-value">{{ exercises.length }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">正确数</span>
            <span class="stat-value correct">{{ correctCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">错误数</span>
            <span class="stat-value wrong">{{ exercises.length - correctCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">正确率</span>
            <span class="stat-value">{{ accuracyRate }}%</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">总用时</span>
            <span class="stat-value">{{ formatTime(practiceTime) }}</span>
          </div>
        </div>
        <div class="complete-actions">
          <button class="complete-btn secondary" @click="reviewWrong">查看错题</button>
          <button class="complete-btn secondary" @click="reviewAll">查看全部题</button>
          <button class="complete-btn primary" @click="goBack">返回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 练习数据
const exercises = ref([
  {
    id: 1,
    title: '矩阵的特征值计算',
    type: '选择题',
    difficulty: 'basic',
    difficultyText: '基础巩固',
    score: 10,
    question: '设A为3阶矩阵，已知A的特征值为1, 2, 3，则|A|的值为（  ）',
    options: ['6', '5', '4', '3'],
    correctAnswer: 0,
    correctAnswerText: 'A. 6',
    analysis: '矩阵的行列式等于其所有特征值的乘积，所以|A| = 1 × 2 × 3 = 6。',
    knowledgePoint: '特征值与行列式'
  },
  {
    id: 2,
    title: '概率分布计算',
    type: '选择题',
    difficulty: 'advanced',
    difficultyText: '进阶提升',
    score: 15,
    question: '设随机变量X服从参数为λ的泊松分布，已知P(X=1) = P(X=2)，则λ =（  ）',
    options: ['1', '2', '3', '4'],
    correctAnswer: 1,
    correctAnswerText: 'B. 2',
    analysis: '泊松分布的概率质量函数为P(X=k) = (λ^k/k!)e^(-λ)。由P(X=1)=P(X=2)可得λe^(-λ) = (λ^2/2)e^(-λ)，解得λ=2。',
    knowledgePoint: '泊松分布'
  },
  {
    id: 3,
    title: '定积分计算',
    type: '填空题',
    difficulty: 'basic',
    difficultyText: '基础巩固',
    score: 10,
    question: '计算定积分 ∫₀¹ x² dx = ______',
    correctAnswer: '1/3',
    correctAnswerText: '1/3',
    analysis: '∫₀¹ x² dx = [x³/3]₀¹ = 1/3 - 0 = 1/3',
    knowledgePoint: '定积分计算'
  }
])

// 确保数据在组件加载时就有值
console.log('Exercises initialized:', exercises.value)
console.log('Exercises length:', exercises.value.length)

// 状态管理
const currentIndex = ref(0)
const selectedOption = ref(null)
const fillAnswer = ref('')
const essayAnswer = ref('')
const showAnswer = ref(false)
const answers = ref({})
const results = ref({})
const showResults = ref(false)
const practiceTime = ref(0)
const showExitConfirm = ref(false)
const showCompleteModal = ref(false)
const viewMode = ref('all') // 'all' 或 'wrong'
const showSubmitConfirm = ref(false)
const submitConfirmTitle = ref('')
const submitConfirmMessage = ref('')
const submitConfirmCancelText = ref('取消')
const submitConfirmConfirmText = ref('确定')
let timer = null
let submitConfirmCallback = null
let submitCancelCallback = null

// 计算属性
const currentExercise = computed(() => {
  console.log('Current index:', currentIndex.value)
  console.log('Exercises length:', exercises.value.length)
  console.log('Exercises data:', exercises.value)
  const exercise = exercises.value[currentIndex.value]
  console.log('Current exercise:', exercise)
  return exercise
})

const progressPercent = computed(() => {
  return ((currentIndex.value + 1) / exercises.value.length) * 100
})

const isCorrect = computed(() => {
  if (!showAnswer.value) return false
  const exercise = currentExercise
  if (exercise?.type === '选择题') {
    return selectedOption.value === exercise.correctAnswer
  }
  return false
})

const answeredCount = computed(() => {
  return Object.keys(answers.value).length
})

const correctCount = computed(() => {
  return Object.values(results.value).filter(r => r.isCorrect).length
})

const accuracyRate = computed(() => {
  if (answeredCount.value === 0) return 0
  return Math.round((correctCount.value / answeredCount.value) * 100)
})

// 方法
const selectOption = (index) => {
  if (showAnswer.value) return
  selectedOption.value = index
}

const submitAnswer = () => {
  const exercise = currentExercise
  let isCorrect = false
  let userAnswer = null

  if (exercise?.type === '选择题') {
    if (selectedOption.value === null) {
      alert('请选择答案')
      return
    }
    userAnswer = selectedOption.value
    isCorrect = selectedOption.value === exercise.correctAnswer
  } else if (exercise?.type === '填空题') {
    if (!fillAnswer.value.trim()) {
      alert('请输入答案')
      return
    }
    userAnswer = fillAnswer.value
    isCorrect = fillAnswer.value.trim() === exercise.correctAnswer
  } else if (exercise?.type === '解答题') {
    if (!essayAnswer.value.trim()) {
      alert('请输入解答')
      return
    }
    userAnswer = essayAnswer.value
    isCorrect = true // 解答题默认正确，需要人工批改
  }

  answers.value[currentIndex.value] = userAnswer
  results.value[currentIndex.value] = { isCorrect, userAnswer }
  showAnswer.value = true

  // 检查是否完成所有题目
  if (currentIndex.value === exercises.value.length - 1) {
    setTimeout(() => {
      showCompleteModal.value = true
    }, 1500)
  }
}

// 保存答案并进入下一题
const saveAndNext = () => {
  const exercise = currentExercise
  let userAnswer = null

  // 检查是否已作答
  if (exercise?.type === '选择题') {
    if (selectedOption.value === null) {
      alert('请选择答案')
      return
    }
    userAnswer = selectedOption.value
  } else if (exercise?.type === '填空题') {
    if (!fillAnswer.value.trim()) {
      alert('请输入答案')
      return
    }
    userAnswer = fillAnswer.value
  } else if (exercise?.type === '解答题') {
    if (!essayAnswer.value.trim()) {
      alert('请输入解答')
      return
    }
    userAnswer = essayAnswer.value
  }

  // 保存答案（但不显示解析）
  answers.value[currentIndex.value] = userAnswer
  
  // 进入下一题
  if (currentIndex.value < exercises.value.length - 1) {
    currentIndex.value++
    resetQuestion()
  }
}

// 提交所有答案
const submitAllAnswers = () => {
  // 先保存当前题目的答案（如果已作答但未保存）
  const exercise = currentExercise.value
  if (exercise && answers.value[currentIndex.value] === undefined) {
    let userAnswer = null
    let hasAnswer = false
    
    if (exercise.type === '选择题' && selectedOption.value !== null) {
      userAnswer = selectedOption.value
      hasAnswer = true
    } else if (exercise.type === '填空题' && fillAnswer.value.trim()) {
      userAnswer = fillAnswer.value
      hasAnswer = true
    } else if (exercise.type === '解答题' && essayAnswer.value.trim()) {
      userAnswer = essayAnswer.value
      hasAnswer = true
    }
    
    if (hasAnswer) {
      answers.value[currentIndex.value] = userAnswer
    }
  }
  
  // 检查是否有未答题目
  const unansweredIndices = []
  for (let i = 0; i < exercises.value.length; i++) {
    if (answers.value[i] === undefined) {
      unansweredIndices.push(i + 1)
    }
  }
  
  if (unansweredIndices.length > 0) {
    // 显示自定义确认弹框 - 有未答题目
    submitConfirmTitle.value = '确认提交'
    submitConfirmMessage.value = `您还有第 ${unansweredIndices.join('、')} 题未作答，确定要提交吗？`
    submitConfirmCancelText.value = '去作答'
    submitConfirmConfirmText.value = '确认提交'
    showSubmitConfirm.value = true
    
    submitConfirmCallback = () => {
      // 确认提交
      doSubmitAll()
    }
    
    submitCancelCallback = () => {
      // 跳转到第一道未答题目
      currentIndex.value = unansweredIndices[0] - 1
      resetQuestion()
      showSubmitConfirm.value = false
    }
  } else {
    // 显示自定义确认弹框 - 全部答完
    submitConfirmTitle.value = '确认提交'
    submitConfirmMessage.value = '确定要提交练习吗？提交后将显示答案解析。'
    submitConfirmCancelText.value = '再检查一下'
    submitConfirmConfirmText.value = '确认提交'
    showSubmitConfirm.value = true
    
    submitConfirmCallback = () => {
      doSubmitAll()
    }
    
    submitCancelCallback = () => {
      showSubmitConfirm.value = false
    }
  }
}

// 执行提交
const doSubmitAll = () => {
  showSubmitConfirm.value = false
  
  // 停止计时器
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  // 计算所有答案的正确性
  exercises.value.forEach((exercise, index) => {
    const userAnswer = answers.value[index]
    let isCorrect = false
    
    if (userAnswer !== undefined) {
      if (exercise.type === '选择题') {
        isCorrect = userAnswer === exercise.correctAnswer
      } else if (exercise.type === '填空题') {
        isCorrect = userAnswer.trim() === exercise.correctAnswer
      } else if (exercise.type === '解答题') {
        isCorrect = true
      }
    }
    
    results.value[index] = { isCorrect, userAnswer }
  })
  
  // 显示第一题的解析
  currentIndex.value = 0
  showAnswer.value = true
  const firstAnswer = answers.value[0]
  if (firstAnswer !== undefined) {
    const firstExercise = exercises.value[0]
    if (firstExercise.type === '选择题') {
      selectedOption.value = firstAnswer
    } else if (firstExercise.type === '填空题') {
      fillAnswer.value = firstAnswer
    } else if (firstExercise.type === '解答题') {
      essayAnswer.value = firstAnswer
    }
  }
  
  // 显示完成弹窗
  showCompleteModal.value = true
}

// 处理提交确认
const handleSubmitConfirm = () => {
  if (submitConfirmCallback) {
    submitConfirmCallback()
  }
}

// 处理提交取消
const handleSubmitCancel = () => {
  if (submitCancelCallback) {
    submitCancelCallback()
  }
}

const nextQuestion = () => {
  if (viewMode.value === 'wrong') {
    // 只在错题之间导航
    const wrongIndices = Object.keys(results.value)
      .map(Number)
      .filter(index => !results.value[index].isCorrect)
      .sort((a, b) => a - b)
    
    const currentIndexInWrong = wrongIndices.indexOf(currentIndex.value)
    if (currentIndexInWrong < wrongIndices.length - 1) {
      currentIndex.value = wrongIndices[currentIndexInWrong + 1]
      showAnswer.value = true
      selectedOption.value = answers.value[currentIndex.value]
    }
  } else {
    // 查看全部题目
    if (currentIndex.value < exercises.value.length - 1) {
      currentIndex.value++
      showAnswer.value = true
      selectedOption.value = answers.value[currentIndex.value]
    }
  }
}

const prevQuestion = () => {
  if (viewMode.value === 'wrong') {
    // 只在错题之间导航
    const wrongIndices = Object.keys(results.value)
      .map(Number)
      .filter(index => !results.value[index].isCorrect)
      .sort((a, b) => a - b)
    
    const currentIndexInWrong = wrongIndices.indexOf(currentIndex.value)
    if (currentIndexInWrong > 0) {
      currentIndex.value = wrongIndices[currentIndexInWrong - 1]
      showAnswer.value = true
      selectedOption.value = answers.value[currentIndex.value]
    }
  } else {
    // 查看全部题目
    if (currentIndex.value > 0) {
      currentIndex.value--
      showAnswer.value = true
      selectedOption.value = answers.value[currentIndex.value]
    }
  }
}

const jumpToQuestion = (index) => {
  if (viewMode.value === 'wrong') {
    // 在查看错题模式下，只允许跳转到错题
    if (!results.value[index] || results.value[index].isCorrect) {
      return // 不允许跳转到正确的题目
    }
  }
  
  currentIndex.value = index
  
  // 在查看模式下，保持显示答案
  if (showResults.value) {
    showAnswer.value = true
    const savedAnswer = answers.value[currentIndex.value]
    if (savedAnswer !== undefined) {
      const exercise = currentExercise.value
      if (exercise.type === '选择题') {
        selectedOption.value = savedAnswer
      } else if (exercise.type === '填空题') {
        fillAnswer.value = savedAnswer
      } else if (exercise.type === '解答题') {
        essayAnswer.value = savedAnswer
      }
    }
  } else {
    resetQuestion()
  }
}

const resetQuestion = () => {
  showAnswer.value = false
  selectedOption.value = null
  fillAnswer.value = ''
  essayAnswer.value = ''

  // 恢复已保存的答案
  const savedAnswer = answers.value[currentIndex.value]
  if (savedAnswer !== undefined) {
    const exercise = currentExercise.value
    if (exercise.type === '选择题') {
      selectedOption.value = savedAnswer
    } else if (exercise.type === '填空题') {
      fillAnswer.value = savedAnswer
    } else if (exercise.type === '解答题') {
      essayAnswer.value = savedAnswer
    }
    showAnswer.value = true
  }
}

const exitPractice = () => {
  showExitConfirm.value = true
}

const confirmExit = () => {
  clearInterval(timer)
  // 确保返回上一页，即关联习题的选择界面
  router.back()
}

const goBack = () => {
  clearInterval(timer)
  // 确保返回上一页，即关联习题的选择界面
  router.back()
}

const finishPractice = () => {
  clearInterval(timer)
  // 确保返回上一页，即关联习题的选择界面
  router.back()
}

const reviewAll = () => {
  // 查看全部题目
  showCompleteModal.value = false
  showResults.value = true
  viewMode.value = 'all'
  
  // 跳转到第一题
  currentIndex.value = 0
  // 重置当前题目的答题状态以便查看
  showAnswer.value = true
  selectedOption.value = answers.value[0]
}

const reviewWrong = () => {
  // 筛选错题并显示
  showCompleteModal.value = false
  showResults.value = true
  viewMode.value = 'wrong'
  
  // 找到第一道错题的索引
  const wrongIndices = Object.keys(results.value)
    .map(Number)
    .filter(index => !results.value[index].isCorrect)
    .sort((a, b) => a - b)
  
  if (wrongIndices.length > 0) {
    // 跳转到第一道错题
    currentIndex.value = wrongIndices[0]
    // 重置当前题目的答题状态以便查看
    showAnswer.value = true
    selectedOption.value = answers.value[wrongIndices[0]]
  }
}

const markQuestion = () => {
  alert('已标记该题目')
}

const addToWrongBook = () => {
  alert('已加入错题本')
}

const showHint = () => {
  alert('提示：仔细审题，注意题目中的关键条件')
}

const askAI = () => {
  alert('AI答疑功能开发中')
}

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 获取用户答案文本
const getUserAnswerText = () => {
  const userAnswer = answers.value[currentIndex.value]
  if (userAnswer === undefined) {
    return '未作答'
  }
  
  const exercise = currentExercise.value
  if (exercise?.type === '选择题') {
    const optionLabels = ['A', 'B', 'C', 'D']
    const optionText = exercise.options?.[userAnswer]
    return `${optionLabels[userAnswer]}. ${optionText || ''}`
  }
  
  return userAnswer
}

// 生命周期
onMounted(() => {
  // 从路由参数获取题目数据
  if (route.query.exercises) {
    try {
      const parsedExercises = JSON.parse(route.query.exercises)
      // 检查数据结构是否正确
      if (parsedExercises.length > 0 && parsedExercises[0].question) {
        exercises.value = parsedExercises
      } else {
        console.log('使用默认练习数据')
      }
    } catch (e) {
      console.error('解析题目数据失败', e)
    }
  } else if (route.query.single === 'true' && route.query.exerciseId) {
    // 单个习题模式
    const exerciseId = parseInt(route.query.exerciseId)
    // 模拟从习题列表中查找对应习题
    const exercise = exercises.value.find(e => e.id === exerciseId)
    if (exercise) {
      exercises.value = [exercise]
    }
  } else if (route.query.multiple === 'true' && route.query.exerciseIds) {
    // 多个习题模式
    const exerciseIds = route.query.exerciseIds.split(',').map(id => parseInt(id))
    // 模拟从习题列表中查找对应习题
    const selectedExercises = exercises.value.filter(e => exerciseIds.includes(e.id))
    if (selectedExercises.length > 0) {
      exercises.value = selectedExercises
    }
  }

  // 开始计时
  timer = setInterval(() => {
    practiceTime.value++
  }, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.practice-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.practice-header {
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #e5e7eb;
}

.back-icon {
  font-size: 16px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.progress-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.progress-text {
  font-size: 14px;
  color: #6b7280;
}

.progress-bar {
  width: 150px;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.exit-btn {
  padding: 8px 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.exit-btn:hover {
  background: #fee2e2;
}

/* 主体内容区 */
.practice-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* 左侧题目区 */
.question-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.question-type {
  padding: 4px 12px;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.question-difficulty {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.question-difficulty.basic {
  background: #d1fae5;
  color: #047857;
}

.question-difficulty.advanced {
  background: #fef3c7;
  color: #b45309;
}

.question-difficulty.exam {
  background: #fee2e2;
  color: #dc2626;
}

.question-score {
  margin-left: auto;
  font-size: 14px;
  color: #6b7280;
}

.question-body {
  margin-bottom: 24px;
}

.question-text {
  font-size: 16px;
  line-height: 1.8;
  color: #1f2937;
  margin-bottom: 24px;
}

.question-number {
  font-weight: 600;
  margin-right: 8px;
}

/* 选项列表 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f9fafb;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover:not(.selected):not(.correct):not(.wrong) {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.option-item.selected {
  background: #eff6ff;
  border-color: #3b82f6;
}

.option-item.correct {
  background: #d1fae5;
  border-color: #10b981;
}

.option-item.wrong {
  background: #fee2e2;
  border-color: #ef4444;
}

.option-label {
  font-weight: 600;
  color: #6b7280;
  min-width: 24px;
}

.option-text {
  flex: 1;
  color: #374151;
}

.option-icon {
  font-size: 18px;
  font-weight: bold;
}

.option-icon.correct {
  color: #10b981;
}

.option-icon.wrong {
  color: #ef4444;
}

/* 填空题输入 */
.fill-blank-area {
  margin-top: 16px;
}

.fill-input {
  width: 100%;
  max-width: 300px;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
}

.fill-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.fill-input:disabled {
  background: #f3f4f6;
  cursor: not-allowed;
}

/* 解答题输入 */
.essay-area {
  margin-top: 16px;
}

.essay-input {
  width: 100%;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  resize: vertical;
  transition: all 0.2s;
}

.essay-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.essay-input:disabled {
  background: #f3f4f6;
  cursor: not-allowed;
}

/* 答案解析 */
.answer-analysis {
  margin-top: 24px;
  padding: 20px;
  background: #f9fafb;
  border-radius: 12px;
  border-left: 4px solid #3b82f6;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.analysis-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.answer-result {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}

.answer-result.correct {
  background: #d1fae5;
  color: #047857;
}

.answer-result.wrong {
  background: #fee2e2;
  color: #dc2626;
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.analysis-content .label {
  font-weight: 500;
  color: #6b7280;
}

.analysis-content .value {
  color: #111827;
}

.correct-answer {
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.user-answer {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 2px solid #e5e7eb;
}

.user-answer.correct {
  border-color: #10b981;
  background: #ecfdf5;
}

.user-answer.wrong {
  border-color: #ef4444;
  background: #fef2f2;
}

.user-answer .value {
  font-weight: 600;
}

.user-answer.correct .value {
  color: #047857;
}

.user-answer.wrong .value {
  color: #dc2626;
}

.analysis-text p {
  margin: 8px 0 0 0;
  line-height: 1.6;
  color: #374151;
}

.knowledge-point .tag {
  display: inline-block;
  padding: 4px 12px;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 6px;
  font-size: 13px;
}

/* 底部操作栏 */
.question-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.action-btn {
  padding: 12px 32px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.prev-btn {
  background: #f3f4f6;
  color: #374151;
}

.prev-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.prev-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.next-btn {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.next-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.next-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 右侧信息面板 */
.info-panel {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

/* 答题卡 */
.answer-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.question-number-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.question-number-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.question-number-btn.current {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #3b82f6;
}

.question-number-btn.answered {
  background: #dbeafe;
  border-color: #3b82f6;
  color: #1d4ed8;
}

.question-number-btn.correct {
  background: #d1fae5;
  border-color: #10b981;
  color: #047857;
}

.question-number-btn.wrong {
  background: #fee2e2;
  border-color: #ef4444;
  color: #dc2626;
}

/* 练习统计 */
.practice-stats {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

/* 快捷操作 */
.quick-actions {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 20px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}

.quick-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.quick-btn.wrong-book-btn {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-color: #f59e0b;
}

.quick-btn.wrong-book-btn:hover {
  background: linear-gradient(135deg, #fde68a 0%, #fcd34d 100%);
  border-color: #d97706;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.btn-icon {
  font-size: 20px;
}

.btn-text {
  font-size: 15px;
  font-weight: 500;
  color: #374151;
}

.quick-btn.wrong-book-btn .btn-text {
  color: #92400e;
}

/* 确认弹窗 */
.confirm-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-content {
  background: white;
  padding: 32px;
  border-radius: 16px;
  text-align: center;
  max-width: 400px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.confirm-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.confirm-content h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #111827;
}

.confirm-content p {
  color: #6b7280;
  margin-bottom: 24px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.confirm-btn.cancel {
  background: #f3f4f6;
  color: #374151;
}

.confirm-btn.cancel:hover {
  background: #e5e7eb;
}

.confirm-btn.confirm {
  background: #dc2626;
  color: white;
}

.confirm-btn.confirm:hover {
  background: #b91c1c;
}

/* 完成弹窗 */
.complete-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.complete-content {
  background: white;
  padding: 40px;
  border-radius: 20px;
  text-align: center;
  max-width: 550px;
  width: 90%;
}

.complete-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.complete-content h3 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
}

.complete-stats {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #e5e7eb;
}

.stat-row:last-child {
  border-bottom: none;
}

.stat-row .stat-label {
  color: #6b7280;
}

.stat-row .stat-value {
  font-weight: 600;
  color: #111827;
}

.stat-row .stat-value.correct {
  color: #10b981;
}

.stat-row .stat-value.wrong {
  color: #ef4444;
}

.complete-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.complete-btn {
  padding: 12px 32px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.complete-btn.secondary {
  background: #f3f4f6;
  color: #374151;
}

.complete-btn.secondary:hover {
  background: #e5e7eb;
}

.complete-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

.complete-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}
</style>