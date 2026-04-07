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
      <!-- 无练习题时的提示 -->
      <div v-if="exercises.length === 0" class="empty-state">
        <div class="empty-icon">📚</div>
        <h3>暂无练习题</h3>
        <p>当前没有可用的练习题，请稍后再试</p>
        <button class="back-btn" @click="goBack">返回</button>
      </div>
      
      <!-- 有练习题时的内容 -->
      <template v-else>
        <!-- 左侧题目区 -->
        <div class="question-panel">
          <div class="question-card">
            <!-- 题目头部 -->
            <div class="question-header">
              <span class="question-type">{{ currentExercise?.type || currentExercise?.questionType || '选择题' }}</span>
              <span class="question-difficulty" :class="currentExercise?.difficulty">
                {{ currentExercise?.difficultyText }}
              </span>
              <span class="question-score">{{ currentExercise?.score || 10 }}分</span>
            </div>

            <!-- 题目内容 -->
            <div class="question-body">
              <div class="question-text">
                <span class="question-number">{{ currentIndex + 1 }}.</span>
                <span class="math-text" style="white-space: pre-wrap;">{{ formatQuestionText(currentExercise?.content || currentExercise?.title || currentExercise?.question) }}</span>
              </div>

              <!-- 选择题选项 -->
              <div v-if="currentExercise?.type === '选择题' || currentExercise?.questionType === '选择题'" class="options-list">
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
                  <span class="option-text"><span class="math-text">{{ formatOptionText(option) }}</span></span>
                  <span v-if="showAnswer && index === currentExercise?.correctAnswer" class="option-icon correct">✓</span>
                  <span v-if="showAnswer && selectedOption === index && index !== currentExercise?.correctAnswer" class="option-icon wrong">✗</span>
                </div>
              </div>

              <!-- 填空题输入 -->
              <div v-else-if="currentExercise?.type === '填空题' || currentExercise?.questionType === '填空题'" class="fill-blank-area">
                <input
                  v-model="fillAnswer"
                  type="text"
                  placeholder="请输入答案"
                  class="fill-input"
                  :disabled="showAnswer"
                />
              </div>

              <!-- 解答题输入 -->
              <div v-else-if="currentExercise?.type === '解答题' || currentExercise?.questionType === '解答题'" class="essay-area">
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
                    <span class="value"><span class="math-text">{{ getUserAnswerText() }}</span></span>
                  </div>
                  <div class="correct-answer">
                    <span class="label">正确答案：</span>
                    <span class="value"><span class="math-text">{{ getCorrectAnswerText() }}</span></span>
                  </div>
                  <div class="analysis-text">
                    <span class="label">解析：</span>
                    <p><span class="math-text">{{ currentExercise?.analysis }}</span></p>
                  </div>
                  <div class="knowledge-point">
                    <span class="label">知识点：</span>
                    <span class="tag"><span class="math-text">{{ currentExercise?.knowledgePoint }}</span></span>
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
              <template v-for="(exercise, index) in exercises" :key="index">
                <div
                  v-if="!showResults || viewMode === 'all' || (viewMode === 'wrong' && showResults && (!results[index] || !results[index].isCorrect))"
                  class="question-number-btn"
                  :class="{
                    current: currentIndex === index,
                    answered: answers[index] !== undefined,
                    correct: showResults && results[index] && results[index].isCorrect,
                    wrong: showResults && results[index] && !results[index].isCorrect
                  }"
                  @click="jumpToQuestion(index)"
                >
                  {{ index + 1 }}
                </div>
              </template>
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
              <button class="quick-btn wrong-book-btn" @click="openWrongBookDialog">
                <span class="btn-icon">📚</span>
                <span class="btn-text">加入错题本</span>
              </button>
            </div>
          </div>
        </div>
      </template>
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

  <!-- 添加错题本对话框 -->
  <el-dialog
    v-model="showWrongBookDialog"
    title="添加到错题本"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form :model="wrongBookForm" label-width="100px">
      <el-form-item label="错误类型">
        <el-select v-model="wrongBookForm.errorType" placeholder="请选择错误类型" style="width: 100%">
          <el-option
            v-for="item in errorTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="优先级">
        <el-radio-group v-model="wrongBookForm.priority">
          <el-radio-button label="HIGH">高</el-radio-button>
          <el-radio-button label="MEDIUM">中</el-radio-button>
          <el-radio-button label="LOW">低</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="标签">
        <el-input
          v-model="wrongBookForm.tags"
          placeholder="多个标签用逗号分隔，如：数学,函数,易错"
        />
      </el-form-item>

      <el-form-item label="笔记">
        <el-input
          v-model="wrongBookForm.notes"
          type="textarea"
          :rows="4"
          placeholder="记录错误原因、解题思路等..."
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showWrongBookDialog = false">取消</el-button>
        <el-button type="primary" @click="addToWrongBook">确定添加</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api/api.js'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { simulateButtonClick, updateButtonState, getButtonData } from '@/mock/buttonMock'

const router = useRouter()
const route = useRoute()

// 组件挂载状态
const isMounted = ref(false)

// 练习数据
const exercises = ref([])

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
  return exercise || {} // 确保返回一个空对象，避免undefined
})

// 获取正确答案文本
const getCorrectAnswerText = () => {
  const exercise = currentExercise.value
  if (!exercise) return ''

  if (exercise.type === '选择题') {
    const optionLabels = ['A', 'B', 'C', 'D']
    const correctIndex = exercise.correctAnswer
    const optionText = exercise.options?.[correctIndex]
    if (optionText === undefined || optionText === null) {
      return ''
    }
    // 去掉选项前缀，避免重复显示
    const cleanOptionText = formatOptionText(optionText)
    return `${optionLabels[correctIndex] || ''}. ${cleanOptionText}`
  }

  return exercise.correctAnswer || ''
}

// KaTeX 已通过 npm 安装并导入，无需动态加载
const loadKaTeX = () => {
  return Promise.resolve()
}

// 渲染数学公式
const renderMathInElement = (element) => {
  if (!element) return

  // 查找所有包含数学公式的元素
  const mathElements = element.querySelectorAll('.math-text')
  mathElements.forEach(el => {
    // 如果已经渲染过，跳过
    if (el.getAttribute('data-rendered') === 'true') return

    const text = el.textContent
    if (text) {
      try {
        // 检查是否包含数学公式符号，只有包含数学公式的内容才使用KaTeX渲染
        // 更严格的数学公式符号检查
        const mathPattern = /[\$\\\^_\(\)\[\]\{\}\frac\sqrt\sum\int\pi\times\div\pm\mp\leq\geq\neq]/
        if (mathPattern.test(text)) {
          katex.render(text, el, {
            throwOnError: false,
            displayMode: true,
            strict: false // 禁用严格模式，避免中文警告
          })
          el.setAttribute('data-rendered', 'true')
        }
      } catch (e) {
        console.error('KaTeX render error:', e)
      }
    }
  })
}

// 监听题目变化，重新渲染公式
watch(() => currentExercise.value, async () => {
  if (currentExercise.value) {
    await loadKaTeX()
    setTimeout(() => {
      renderMathInElement(document.querySelector('.question-body'))
      renderMathInElement(document.querySelector('.answer-analysis'))
    }, 0)
  }
}, { deep: true, immediate: false })

// 监听显示答案变化，重新渲染公式
watch(showAnswer, async () => {
  if (showAnswer.value && currentExercise.value) {
    await loadKaTeX()
    setTimeout(() => {
      renderMathInElement(document.querySelector('.answer-analysis'))
    }, 0)
  }
}, { immediate: false })

const progressPercent = computed(() => {
  if (exercises.value.length === 0) return 0
  return ((currentIndex.value + 1) / exercises.value.length) * 100
})

const isCorrect = computed(() => {
  if (!showAnswer.value) return false
  const exercise = currentExercise.value
  if (exercise?.type === '选择题') {
    return selectedOption.value === exercise.correctAnswer
  } else if (exercise?.type === '填空题') {
    const userAnswer = answers.value[currentIndex.value]
    return userAnswer !== undefined && userAnswer.trim() === exercise.correctAnswer
  } else if (exercise?.type === '解答题') {
    return true // 解答题默认正确，需要人工批改
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
  if (exercises.value.length === 0) return
  
  const exercise = currentExercise.value
  if (!exercise) return
  
  let isCorrect = false
  let userAnswer = null

  if (exercise?.type === '选择题') {
    if (selectedOption.value === null) {
      ElMessage.warning('请选择答案')
      return
    }
    userAnswer = selectedOption.value
    isCorrect = selectedOption.value === exercise.correctAnswer
  } else if (exercise?.type === '填空题') {
    if (!fillAnswer.value || !fillAnswer.value.trim()) {
      ElMessage.warning('请输入答案')
      return
    }
    userAnswer = fillAnswer.value
    isCorrect = fillAnswer.value.trim() === exercise.correctAnswer
  } else if (exercise?.type === '解答题') {
    if (!essayAnswer.value || !essayAnswer.value.trim()) {
      ElMessage.warning('请输入解答')
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
      if (isMounted.value) {
        showCompleteModal.value = true
      }
    }, 1500)
  }
}

// 保存答案并进入下一题
const saveAndNext = () => {
  if (exercises.value.length === 0) return
  
  const exercise = currentExercise.value
  if (!exercise) return
  
  let userAnswer = null

  // 检查是否已作答
  if (exercise?.type === '选择题') {
    if (selectedOption.value === null) {
      ElMessage.warning('请选择答案')
      return
    }
    userAnswer = selectedOption.value
  } else if (exercise?.type === '填空题') {
    if (!fillAnswer.value || !fillAnswer.value.trim()) {
      ElMessage.warning('请输入答案')
      return
    }
    userAnswer = fillAnswer.value
  } else if (exercise?.type === '解答题') {
    if (!essayAnswer.value || !essayAnswer.value.trim()) {
      ElMessage.warning('请输入解答')
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
  if (exercises.value.length === 0) return
  
  // 先保存当前题目的答案（如果已作答但未保存）
  const exercise = currentExercise.value
  if (exercise && answers.value[currentIndex.value] === undefined) {
    let userAnswer = null
    let hasAnswer = false
    
    if (exercise.type === '选择题' && selectedOption.value !== null) {
      userAnswer = selectedOption.value
      hasAnswer = true
    } else if (exercise.type === '填空题' && fillAnswer.value && fillAnswer.value.trim()) {
      userAnswer = fillAnswer.value
      hasAnswer = true
    } else if (exercise.type === '解答题' && essayAnswer.value && essayAnswer.value.trim()) {
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
  
  if (exercises.value.length === 0) {
    // 没有练习题时直接显示完成弹窗
    showCompleteModal.value = true
    return
  }
  
  // 计算所有答案的正确性
  exercises.value.forEach((exercise, index) => {
    const userAnswer = answers.value[index]
    let isCorrect = false
    
    if (userAnswer !== undefined && userAnswer !== null) {
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
  if (exercises.value.length === 0) return
  
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

const prevQuestion = async () => {
  if (exercises.value.length === 0) return
  
  const response = await simulateButtonClick('practice', 'prevBtn');
  if (response.success) {
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
    ElMessage.success(response.message);
  }
}

const jumpToQuestion = (index) => {
  if (exercises.value.length === 0) return
  
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
  if (exercises.value.length === 0) return
  
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

const reviewAll = async () => {
  if (exercises.value.length === 0) return
  
  const response = await simulateButtonClick('practice', 'reviewAllBtn');
  if (response.success) {
    // 查看全部题目
    showCompleteModal.value = false
    showResults.value = true
    viewMode.value = 'all'
    
    // 跳转到第一题
    currentIndex.value = 0
    // 重置当前题目的答题状态以便查看
    showAnswer.value = true
    selectedOption.value = answers.value[0]
    ElMessage.success(response.message);
  }
}

const reviewWrong = async () => {
  if (exercises.value.length === 0) return
  
  const response = await simulateButtonClick('practice', 'reviewWrongBtn');
  if (response.success) {
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
    ElMessage.success(response.message);
  }
}

const markQuestion = () => {
  alert('已标记该题目')
}

// 错题本相关状态
const showWrongBookDialog = ref(false)
const wrongBookForm = ref({
  errorType: 'OTHER',
  notes: '',
  tags: '',
  priority: 'MEDIUM'
})

// 错误类型选项
const errorTypeOptions = [
  { label: '概念错误', value: 'CONCEPT' },
  { label: '计算错误', value: 'CALCULATION' },
  { label: '粗心大意', value: 'CARELESS' },
  { label: '其他', value: 'OTHER' }
]

// 优先级选项
const priorityOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' }
]

// 打开添加错题对话框
const openWrongBookDialog = () => {
  const exercise = currentExercise.value
  if (!exercise) {
    ElMessage.warning('当前没有题目')
    return
  }

  // 如果已经答错，自动填充用户答案
  if (showAnswer.value && !isCorrect.value) {
    const userAnswer = answers.value[currentIndex.value]
    wrongBookForm.value.notes = `我的答案：${userAnswer}\n需要重点复习`
  }

  showWrongBookDialog.value = true
}

// 添加到错题本
const addToWrongBook = async () => {
  const exercise = currentExercise.value
  if (!exercise) {
    ElMessage.warning('当前没有题目')
    return
  }

  const response = await simulateButtonClick('practice', 'addToWrongBookBtn');
  if (response.success) {
    ElMessage.success(response.message)
    showWrongBookDialog.value = false

    // 重置表单
    wrongBookForm.value = {
      errorType: 'OTHER',
      notes: '',
      tags: '',
      priority: 'MEDIUM'
    }
  } else {
    ElMessage.error(response.message || '添加失败')
  }
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

// 格式化选项文本，去掉 A. B. C. D. 前缀或 选项A 前缀
const formatOptionText = (option) => {
  if (!option) return ''
  // 去掉开头的 A. B. C. D. 前缀（包括中英文冒号、点和空格）
  // 或 选项A 这样的前缀
  // 匹配模式：选项A 或 选项 A 后跟冒号或空格
  const cleaned = option.replace(/^选项\s*[A-Da-d]\s*[:：]?\s*/i, '').trim()
  // 再去掉 A. B. C. D. 前缀
  const cleanedAgain = cleaned.replace(/^[A-Da-d][\.．:：]?\s*/i, '').trim()
  return cleanedAgain
}

// 格式化题目文本，自动插入换行
const formatQuestionText = (text) => {
  if (!text) return ''
  // 在标点符号后添加换行符，使长文本更易读
  // 在逗号、句号、分号、问号、感叹号后添加换行
  // 但不要在 LaTeX 公式内部添加换行
  let result = text
    .replace(/([，。；？！])([^\n])/g, '$1\n$2')
    .replace(/([,;?!])([^\n])/g, '$1\n$2')
  
  // 在 LaTeX 块级公式前后添加换行，使其单独一行显示
  result = result.replace(/(\\\[|\\\])/g, '\n$1\n')
  
  // 移除连续的换行符
  result = result.replace(/\n{3,}/g, '\n\n')
  
  return result.trim()
}

// 获取用户答案文本
const getUserAnswerText = () => {
  const userAnswer = answers.value[currentIndex.value]
  if (userAnswer === undefined || userAnswer === null || userAnswer === '') {
    return '未作答'
  }

  const exercise = currentExercise.value
  if (exercise?.type === '选择题') {
    const optionLabels = ['A', 'B', 'C', 'D']
    const optionText = exercise.options?.[userAnswer]
    if (!optionText) {
      return '未作答'
    }
    // 去掉选项前缀，避免重复显示
    const cleanOptionText = formatOptionText(optionText)
    return `${optionLabels[userAnswer] || ''}. ${cleanOptionText}`
  }

  return userAnswer
}

// 生成 mock 习题数据
const generateMockExercise = (id) => {
  // 基础知识点和对应的题目
  const knowledgePoints = [
    {
      point: '高等数学/导数',
      title: '导数计算',
      content: '函数f(x) = x²的导数是？',
      options: ['f\'(x) = x', 'f\'(x) = 2x', 'f\'(x) = x²', 'f\'(x) = 2'],
      correctAnswer: 1,
      analysis: '根据幂函数求导法则，(x^n)\' = n*x^(n-1)，所以(x²)\' = 2x。'
    },
    {
      point: '计算机网络/TCP/IP',
      title: 'TCP/IP协议层次结构',
      content: 'TCP/IP协议栈分为几个层次？',
      options: ['4层', '5层', '6层', '7层'],
      correctAnswer: 0,
      analysis: 'TCP/IP协议栈分为应用层、传输层、网络层和网络接口层，共4层。'
    },
    {
      point: '操作系统/进程管理',
      title: '进程状态',
      content: '进程的三种基本状态是？',
      options: ['就绪、运行、阻塞', '创建、运行、终止', '挂起、运行、等待', '新建、就绪、运行'],
      correctAnswer: 0,
      analysis: '进程的三种基本状态是就绪（Ready）、运行（Running）和阻塞（Blocked）。'
    },
    {
      point: '数据库/SQL',
      title: 'SQL语句',
      content: '以下哪个SQL语句用于删除表中的所有数据但保留表结构？',
      options: ['DELETE FROM table', 'DROP TABLE table', 'TRUNCATE TABLE table', 'REMOVE FROM table'],
      correctAnswer: 2,
      analysis: 'TRUNCATE TABLE删除所有数据但保留表结构，比DELETE更快，但不能回滚。'
    },
    {
      point: '数据结构/排序算法',
      title: '时间复杂度',
      content: '快速排序的平均时间复杂度是？',
      options: ['O(n)', 'O(n log n)', 'O(n²)', 'O(log n)'],
      correctAnswer: 1,
      analysis: '快速排序的平均时间复杂度为O(n log n)，最坏情况下为O(n²)。'
    }
  ]
  
  // 根据ID选择知识点
  let index = 0
  if (id.includes('_')) {
    index = parseInt(id.split('_')[1]) % knowledgePoints.length || 0
  } else {
    // 对于纯数字ID，使用特定的映射
    const idNum = parseInt(id)
    if (idNum >= 500 && idNum < 600) {
      // 高等数学相关ID
      index = 0
    } else if (idNum >= 100 && idNum < 200) {
      // 计算机网络相关ID
      index = 1
    } else if (idNum >= 200 && idNum < 300) {
      // 操作系统相关ID
      index = 2
    } else if (idNum >= 300 && idNum < 400) {
      // 数据库相关ID
      index = 3
    } else if (idNum >= 400 && idNum < 500) {
      // 数据结构相关ID
      index = 4
    }
  }
  const knowledgePoint = knowledgePoints[index]
  
  return {
    id: id,
    title: knowledgePoint.title,
    content: knowledgePoint.content,
    type: '选择题',
    questionType: '选择题',
    options: knowledgePoint.options,
    correctAnswer: knowledgePoint.correctAnswer,
    analysis: knowledgePoint.analysis,
    knowledgePoint: knowledgePoint.point,
    difficulty: 'MEDIUM',
    score: 10
  }
}

// 从API获取习题数据
const fetchExercises = async () => {
  try {
    console.log('Route query:', route.query)
    console.log('exerciseIds:', route.query.exerciseIds)
    // 根据路由参数决定获取哪种类型的习题
    if (route.query.exercisesKey) {
      // 从知识图谱传递的完整习题数据（存储在sessionStorage中）
      try {
        const exercisesKey = route.query.exercisesKey
        const exercisesData = sessionStorage.getItem(exercisesKey)
        if (exercisesData) {
          const parsedExercises = JSON.parse(exercisesData)
          console.log('Parsed exercises from graph:', parsedExercises)
          exercises.value = parsedExercises
          // 读取后删除sessionStorage中的数据
          sessionStorage.removeItem(exercisesKey)
          return
        }
      } catch (error) {
        console.error('Failed to parse exercises:', error)
      }
    }
    if (route.query.exercises) {
      // 兼容旧的URL参数方式（为了向后兼容）
      try {
        const parsedExercises = JSON.parse(route.query.exercises)
        console.log('Parsed exercises from URL:', parsedExercises)
        exercises.value = parsedExercises
        return
      } catch (error) {
        console.error('Failed to parse exercises from URL:', error)
      }
    }
    if ((route.query.single === 'true' || route.query.multiple === 'false') && route.query.exerciseId) {
      // 单个习题模式
      const exerciseId = route.query.exerciseId
      console.log('Fetching exercise:', exerciseId)
      try {
        const response = await api.get(`/exercise/${exerciseId}`)
        console.log('API response:', response)
        if (response.code === 200) {
          exercises.value = [response.data]
          console.log('Exercises set:', exercises.value)
        } else {
          console.error('API error:', response.message)
          // 使用 mock 数据
          exercises.value = [generateMockExercise(exerciseId)]
        }
      } catch (error) {
        console.error('Fetch error:', error)
        // 使用 mock 数据
        exercises.value = [generateMockExercise(exerciseId)]
      }
    } else if (route.query.multiple === 'true' && route.query.exerciseIds) {
      // 多个习题模式
      const exerciseIds = route.query.exerciseIds
      // 由于后端没有批量获取习题的接口，我们逐个获取
      const exercisePromises = exerciseIds.split(',').map(id => api.get(`/exercise/${id}`))
      const responses = await Promise.all(exercisePromises)
      const exerciseData = responses
        .filter(response => response.code === 200)
        .map(response => response.data)
      exercises.value = exerciseData
    } else if (route.query.knowledgePoint) {
      // 根据知识点获取习题
      const knowledgePoint = route.query.knowledgePoint
      const response = await api.get('/exercise/exercises', {
        knowledgePoint: knowledgePoint, 
        difficulty: 'basic'
      })
      if (response && response.code === 200) {
        exercises.value = response.data
      }
    } else {
      // 获取默认习题
      const response = await api.get('/exercise/exercises', {
        knowledgePoint: 'Java基础', 
        difficulty: 'basic'
      })
      if (response && response.code === 200) {
        exercises.value = response.data
      }
    }
  } catch (error) {
    console.error('获取习题数据失败:', error)
  }
}

// 生命周期
onMounted(async () => {
  // 设置组件为已挂载状态
  isMounted.value = true
  
  // 优先检查 exercisesKey（sessionStorage 方式）
  if (route.query.exercisesKey) {
    try {
      const exercisesKey = route.query.exercisesKey
      const exercisesData = sessionStorage.getItem(exercisesKey)
      if (exercisesData) {
        const parsedExercises = JSON.parse(exercisesData)
        console.log('Parsed exercises from sessionStorage:', parsedExercises)
        exercises.value = parsedExercises
        // 读取后删除sessionStorage中的数据
        sessionStorage.removeItem(exercisesKey)
      } else {
        console.log('sessionStorage中没有找到习题数据，使用API获取')
        await fetchExercises()
      }
    } catch (e) {
      console.error('解析题目数据失败', e)
      await fetchExercises()
    }
  } else if (route.query.exercises) {
    // 兼容旧的URL参数方式（为了向后兼容）
    try {
      const parsedExercises = JSON.parse(route.query.exercises)
      // 检查数据结构是否正确
      if (parsedExercises.length > 0 && parsedExercises[0].question) {
        exercises.value = parsedExercises
      } else {
        console.log('使用API获取练习数据')
        await fetchExercises()
      }
    } catch (e) {
      console.error('解析题目数据失败', e)
      await fetchExercises()
    }
  } else {
    // 从API获取练习数据
    await fetchExercises()
  }

  // 开始计时
  timer = setInterval(() => {
    if (isMounted.value) {
      practiceTime.value++
    }
  }, 1000)
})

onUnmounted(() => {
  // 设置组件为未挂载状态
  isMounted.value = false
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

/* 空状态样式 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin: 0 auto;
  max-width: 500px;
  width: 100%;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

.empty-state p {
  color: #6b7280;
  margin-bottom: 32px;
  line-height: 1.6;
}

.empty-state .back-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.empty-state .back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
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
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
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
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
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
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

/* 数学公式文本换行 */
.math-text {
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  display: inline;
}

/* KaTeX 渲染后的公式换行 */
.math-text .katex {
  white-space: normal;
  word-wrap: break-word;
  word-break: break-word;
}

.math-text .katex-html {
  white-space: normal;
  word-wrap: break-word;
  word-break: break-word;
}

/* 块级公式换行 */
.math-text .katex-display {
  overflow-x: auto;
  overflow-y: hidden;
  max-width: 100%;
}

.math-text .katex-display .katex {
  white-space: normal;
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