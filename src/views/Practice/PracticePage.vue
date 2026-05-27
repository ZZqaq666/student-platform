<template>
  <div class="pp-page">
    <!-- ===== 顶部学习状态栏 ===== -->
    <header class="pp-header">
      <button class="pph-back" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5m7-7-7 7 7 7"/></svg>
        <span>返回</span>
      </button>

      <div class="pph-mode">
        <span class="pphm-icon">{{ modeIcon }}</span>
        <div class="pphm-text">
          <span class="pphm-title">{{ modeTitle }}</span>
          <span class="pphm-sub">AI 强化训练</span>
        </div>
      </div>

      <div class="pph-progress">
        <div class="pphp-text">{{ currentIndex + 1 }} / {{ exercises.length }}</div>
        <div class="pphp-bar"><div class="pphp-fill" :style="{width:progressPercent+'%'}"></div></div>
      </div>

      <div class="pph-timer">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        <span>{{ formatTime(practiceTime) }}</span>
      </div>

      <button class="pph-exit" @click="exitPractice">退出</button>
    </header>

    <!-- ===== 主体双栏 ===== -->
    <div class="pp-body">
      <!-- 左侧：题目区 -->
      <main class="pp-main">
        <div class="pp-question" v-if="currentExercise">
          <!-- 题目头部 -->
          <div class="ppq-head">
            <div class="ppqh-tags">
              <span class="ppqht-type">{{ currentExercise.type || '选择题' }}</span>
              <span class="ppqht-diff" :class="currentExercise.difficulty">{{ currentExercise.difficultyText || '中等' }}</span>
              <span class="ppqht-score">{{ currentExercise.score || 10 }} 分</span>
            </div>
            <div class="ppqh-kp" v-if="currentExercise.knowledgePoint">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
              <span>{{ currentExercise.knowledgePoint }}</span>
            </div>
            <div class="ppqh-ai" v-if="aiTip">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
              <span>{{ aiTip }}</span>
            </div>
          </div>

          <!-- 题目内容卡片 -->
          <div class="ppq-body">
            <div class="ppqb-question">
              <span class="ppqbq-num">{{ currentIndex + 1 }}.</span>
              <span>{{ currentExercise.question }}</span>
            </div>

            <!-- 选择题选项 -->
            <div v-if="currentExercise.type === '选择题'" class="ppqb-options">
              <div
                v-for="(option, idx) in currentExercise.options || []"
                :key="idx"
                class="ppqbo-item"
                :class="{
                  selected: selectedOption === idx,
                  correct: showAnswer && idx === currentExercise.correctAnswer,
                  wrong: showAnswer && selectedOption === idx && idx !== currentExercise.correctAnswer
                }"
                @click="selectOption(idx)"
              >
                <span class="ppqboi-letter">{{ ['A','B','C','D'][idx] }}</span>
                <span class="ppqboi-text">{{ option }}</span>
                <span v-if="showAnswer && idx === currentExercise.correctAnswer" class="ppqboi-icon check">✓</span>
                <span v-if="showAnswer && selectedOption === idx && idx !== currentExercise.correctAnswer" class="ppqboi-icon cross">✗</span>
              </div>
            </div>

            <!-- 填空题 -->
            <div v-else-if="currentExercise.type === '填空题'" class="ppqb-fill">
              <input v-model="fillAnswer" placeholder="请输入答案..." :disabled="showAnswer" />
            </div>

            <!-- 解答题 -->
            <div v-else-if="currentExercise.type === '解答题'" class="ppqb-essay">
              <textarea v-model="essayAnswer" placeholder="请在此输入解答过程..." rows="6" :disabled="showAnswer"></textarea>
            </div>
          </div>

          <!-- 答案解析 -->
          <div v-if="showAnswer" class="ppq-analysis">
            <div class="ppqa-head">
              <span class="ppqah-title">答案解析</span>
              <span class="ppqah-result" :class="isCorrect ? 'ok' : 'fail'">{{ isCorrect ? '✓ 回答正确' : '✗ 回答错误' }}</span>
            </div>
            <div class="ppqa-my" :class="isCorrect ? 'ok' : 'fail'">
              <span class="ppqam-label">我的答案</span>
              <span class="ppqam-val">{{ getUserAnswerText() }}</span>
            </div>
            <div class="ppqa-right">
              <span class="ppqar-label">正确答案</span>
              <span class="ppqar-val">{{ currentExercise.correctAnswerText }}</span>
            </div>
            <div v-if="currentExercise.analysis" class="ppqa-detail">
              <span class="ppqad-label">解析</span>
              <p>{{ currentExercise.analysis }}</p>
            </div>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="pp-actions">
          <button class="ppa-btn prev" :disabled="currentIndex === 0" @click="prevQuestion">← 上一题</button>

          <template v-if="!showAnswer">
            <button v-if="currentIndex < exercises.length - 1" class="ppa-btn next" @click="saveAndNext">下一题 →</button>
            <button v-else class="ppa-btn submit" @click="submitAllAnswers">提交练习</button>
          </template>
          <template v-else>
            <button v-if="currentIndex < exercises.length - 1" class="ppa-btn next" @click="nextQuestion">下一题 →</button>
          </template>
        </div>
      </main>

      <!-- 右侧：AI 学习助手 -->
      <aside class="pp-side">
        <!-- 答题卡 -->
        <div class="pps-card">
          <div class="ppsc-head">答题卡</div>
          <div class="ppsc-grid">
            <div
              v-for="(ex, idx) in exercises"
              :key="idx"
              v-if="!showResults || viewMode === 'all' || (viewMode === 'wrong' && showResults && !results[idx]?.isCorrect)"
              class="ppscg-dot"
              :class="{
                current: currentIndex === idx,
                answered: answers[idx] !== undefined,
                correct: showResults && results[idx]?.isCorrect,
                wrong: showResults && !results[idx]?.isCorrect
              }"
              @click="jumpToQuestion(idx)"
            >{{ idx + 1 }}</div>
          </div>
          <div class="ppsc-legend">
            <span><i class="l-dot cur"></i>当前</span>
            <span><i class="l-dot done"></i>已答</span>
            <span><i class="l-dot ok"></i>正确</span>
            <span><i class="l-dot fail"></i>错误</span>
          </div>
        </div>

        <!-- 统计 -->
        <div class="pps-card">
          <div class="ppsc-head">练习统计</div>
          <div class="pps-stats">
            <div class="ppss-item"><span class="ppssi-val">{{ answeredCount }}</span><span class="ppssi-lbl">已答</span></div>
            <div class="ppss-item"><span class="ppssi-val">{{ correctCount }}</span><span class="ppssi-lbl">正确</span></div>
            <div class="ppss-item"><span class="ppssi-val">{{ accuracyRate }}%</span><span class="ppssi-lbl">正确率</span></div>
            <div class="ppss-item"><span class="ppssi-val">{{ formatTime(practiceTime) }}</span><span class="ppssi-lbl">用时</span></div>
          </div>
        </div>

        <!-- AI 助手 -->
        <div class="pps-card ai-card">
          <div class="ppsc-head">🤖 AI 学习助手</div>
          <div class="ppai-tips">
            <div class="ppait-row" v-if="accuracyRate > 0">
              <span class="ppait-icon">{{ accuracyRate >= 70 ? '👍' : '💪' }}</span>
              <span>当前正确率 <strong>{{ accuracyRate }}%</strong>{{ accuracyRate >= 70 ? '，继续保持！' : '，加油突破！' }}</span>
            </div>
            <div class="ppait-row">
              <span class="ppait-icon">⚠️</span>
              <span>注意审题，留意关键条件</span>
            </div>
            <div class="ppait-row">
              <span class="ppait-icon">💡</span>
              <span>先判断定义域，再代入计算</span>
            </div>
            <div class="ppait-row">
              <span class="ppait-icon">🔥</span>
              <span>连续答对 {{ streakCount }} 题</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 完成弹窗 -->
    <div v-if="showCompleteModal" class="pp-overlay" @click.self="showCompleteModal = false">
      <div class="pp-complete">
        <div class="ppc-icon">🎉</div>
        <h2>练习完成！</h2>
        <div class="ppc-stats">
          <div class="ppcs-item"><span>{{ exercises.length }}</span>总题数</div>
          <div class="ppcs-item ok"><span>{{ correctCount }}</span>正确</div>
          <div class="ppcs-item fail"><span>{{ exercises.length - correctCount }}</span>错误</div>
          <div class="ppcs-item"><span>{{ accuracyRate }}%</span>正确率</div>
        </div>
        <div class="ppc-btns">
          <button class="ppcb-btn sec" @click="reviewWrong">查看错题</button>
          <button class="ppcb-btn sec" @click="reviewAll">查看全部</button>
          <button class="ppcb-btn pri" @click="goBack">返回</button>
        </div>
      </div>
    </div>

    <!-- 退出确认弹窗 -->
    <div v-if="showExitConfirm" class="pp-overlay" @click.self="showExitConfirm = false">
      <div class="pp-confirm">
        <h3>确认退出？</h3>
        <p>退出后练习进度将保存</p>
        <div class="ppcf-btns">
          <button class="ppcfb-cancel" @click="showExitConfirm = false">继续练习</button>
          <button class="ppcfb-ok" @click="confirmExit">确认退出</button>
        </div>
      </div>
    </div>

    <!-- 提交确认 -->
    <div v-if="showSubmitConfirm" class="pp-overlay" @click.self="showSubmitConfirm = false">
      <div class="pp-confirm">
        <h3>{{ submitConfirmTitle }}</h3>
        <p>{{ submitConfirmMessage }}</p>
        <div class="ppcf-btns">
          <button class="ppcfb-cancel" @click="handleSubmitCancel">{{ submitConfirmCancelText }}</button>
          <button class="ppcfb-ok" @click="handleSubmitConfirm">{{ submitConfirmConfirmText }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter(); const route = useRoute()

/* ========== 数据 ========== */
const exercises = ref([])
const currentIndex = ref(0); const selectedOption = ref(null); const fillAnswer = ref(''); const essayAnswer = ref('')
const showAnswer = ref(false); const answers = ref({}); const results = ref({}); const showResults = ref(false)
const practiceTime = ref(0); const showExitConfirm = ref(false); const showCompleteModal = ref(false)
const viewMode = ref('all'); const showSubmitConfirm = ref(false)
const submitConfirmTitle = ref(''); const submitConfirmMessage = ref('')
const submitConfirmCancelText = ref('取消'); const submitConfirmConfirmText = ref('确定')
let timer = null; let submitConfirmCallback = null; let submitCancelCallback = null

/* ========== 展示 ========== */
const modeIcon = computed(() => route.query.from === 'graph' ? '🧠' : '📚')
const modeTitle = computed(() => route.query.from === 'graph' ? '知识图谱 · 强化练习' : '自由练习')
const aiTip = computed(() => {
  const tips = ['该知识点在考试中高频出现，建议熟练掌握', '注意审题，仔细读条件再作答', '这类题考研中经常出现变体', '先理解概念再动笔，事半功倍']
  return showAnswer.value ? '' : (exercises.value.length > 0 ? tips[currentIndex.value % tips.length] : '')
})
const streakCount = computed(() => {
  let s = 0
  for (let i = currentIndex.value; i >= 0; i--) { if (results.value[i]?.isCorrect) s++; else break }
  return s
})

const currentExercise = computed(() => exercises.value[currentIndex.value])
const progressPercent = computed(() => exercises.value.length ? ((currentIndex.value + 1) / exercises.value.length) * 100 : 0)
const isCorrect = computed(() => {
  if (!showAnswer.value) return false
  const ex = currentExercise.value; if (!ex) return false
  const a = answers.value[currentIndex.value]
  return a !== undefined ? checkAnswer(a, ex.correctAnswer) : false
})
const answeredCount = computed(() => Object.keys(answers.value).length)
const correctCount = computed(() => Object.values(results.value).filter(r => r.isCorrect).length)
const accuracyRate = computed(() => answeredCount.value === 0 ? 0 : Math.round((correctCount.value / answeredCount.value) * 100))

/* ========== 作答 ========== */
const selectOption = (idx) => { if (!showAnswer.value) selectedOption.value = idx }

const submitAnswer = () => {
  const ex = currentExercise.value; let ans = null
  if (ex?.type === '选择题') { if (selectedOption.value === null) return; ans = selectedOption.value }
  else if (ex?.type === '填空题') { if (!fillAnswer.value.trim()) return; ans = fillAnswer.value }
  else if (ex?.type === '解答题') { if (!essayAnswer.value.trim()) return; ans = essayAnswer.value }
  if (ans === null) return
  answers.value[currentIndex.value] = ans
  results.value[currentIndex.value] = { isCorrect: checkAnswer(ans, ex.correctAnswer), userAnswer: ans }
  showAnswer.value = true
  if (currentIndex.value === exercises.value.length - 1) setTimeout(() => showCompleteModal.value = true, 1500)
}
const saveAndNext = () => {
  const ex = currentExercise.value; let ans = null
  if (ex?.type === '选择题') { if (selectedOption.value === null) return; ans = selectedOption.value }
  else if (ex?.type === '填空题') { if (!fillAnswer.value.trim()) return; ans = fillAnswer.value }
  else if (ex?.type === '解答题') { if (!essayAnswer.value.trim()) return; ans = essayAnswer.value }
  if (ans === null) return
  answers.value[currentIndex.value] = ans
  if (currentIndex.value < exercises.value.length - 1) { currentIndex.value++; resetQuestion() }
}
const submitAllAnswers = () => {
  // 先保存当前题目的答案（防止已选但未点"下一题"的情况）
  saveCurrentAnswer()

  const unanswered = []; for (let i = 0; i < exercises.value.length; i++) { if (answers.value[i] === undefined) unanswered.push(i + 1) }
  if (unanswered.length > 0) {
    submitConfirmTitle.value = '确认提交'; submitConfirmMessage.value = `第 ${unanswered.join('、')} 题未作答，确定提交？`; submitConfirmCancelText.value = '去作答'; submitConfirmConfirmText.value = '确认提交'
    showSubmitConfirm.value = true; submitConfirmCallback = () => doSubmitAll(); submitCancelCallback = () => { currentIndex.value = unanswered[0] - 1; resetQuestion(); showSubmitConfirm.value = false }
  } else {
    submitConfirmTitle.value = '确认提交'; submitConfirmMessage.value = '确定要提交练习吗？'; submitConfirmCancelText.value = '再检查'; submitConfirmConfirmText.value = '确认提交'
    showSubmitConfirm.value = true; submitConfirmCallback = () => doSubmitAll(); submitCancelCallback = () => { showSubmitConfirm.value = false }
  }
}
// 保存当前题答案（不跳题，仅保存）
const saveCurrentAnswer = () => {
  const ex = currentExercise.value; if (!ex || answers.value[currentIndex.value] !== undefined) return
  if (ex.type === '选择题' && selectedOption.value !== null) { answers.value[currentIndex.value] = selectedOption.value }
  else if (ex.type === '填空题' && fillAnswer.value.trim()) { answers.value[currentIndex.value] = fillAnswer.value }
  else if (ex.type === '解答题' && essayAnswer.value.trim()) { answers.value[currentIndex.value] = essayAnswer.value }
}
// 判断答案正确性（用 == 避免 API 返回字符串 vs 数字的严格比较问题）
const checkAnswer = (userAnswer, correctAnswer) => userAnswer != null && String(userAnswer).trim() === String(correctAnswer).trim()

const doSubmitAll = () => { showSubmitConfirm.value = false; clearInterval(timer); timer = null; exercises.value.forEach((ex, i) => { const a = answers.value[i]; const ok = a !== undefined ? checkAnswer(a, ex.correctAnswer) : false; results.value[i] = { isCorrect: ok, userAnswer: a } }); currentIndex.value = 0; showAnswer.value = true; showResults.value = true; const a0 = answers.value[0]; if (a0 !== undefined) { const e0 = exercises.value[0]; if (e0 && e0.type === '选择题') selectedOption.value = a0; else if (e0 && e0.type === '填空题') fillAnswer.value = a0; else if (e0) essayAnswer.value = a0 }; showCompleteModal.value = true }
const handleSubmitConfirm = () => { if (submitConfirmCallback) submitConfirmCallback() }
const handleSubmitCancel = () => { if (submitCancelCallback) submitCancelCallback() }
const nextQuestion = () => { if (currentIndex.value < exercises.value.length - 1) { currentIndex.value++; showAnswer.value = true; selectedOption.value = answers.value[currentIndex.value] } }
const prevQuestion = () => { if (currentIndex.value > 0) { currentIndex.value--; showAnswer.value = true; selectedOption.value = answers.value[currentIndex.value] } }
const jumpToQuestion = (i) => { currentIndex.value = i; if (showResults.value) { showAnswer.value = true; const a = answers.value[i]; if (a !== undefined) selectedOption.value = a } else resetQuestion() }
const resetQuestion = () => { showAnswer.value = false; selectedOption.value = null; fillAnswer.value = ''; essayAnswer.value = ''; const a = answers.value[currentIndex.value]; if (a !== undefined) { if (currentExercise.value?.type === '选择题') selectedOption.value = a; else if (currentExercise.value?.type === '填空题') fillAnswer.value = a; else essayAnswer.value = a; showAnswer.value = true } }
const getUserAnswerText = () => { const a = answers.value[currentIndex.value]; if (a === undefined) return '未作答'; const ex = currentExercise.value; if (ex?.type === '选择题') return `${['A','B','C','D'][a]}. ${ex.options?.[a] || ''}`; return a }

/* ========== 导航 ========== */
const exitPractice = () => { showExitConfirm.value = true }
const confirmExit = () => { clearInterval(timer); router.back() }
const goBack = () => { clearInterval(timer); router.back() }
const reviewAll = () => { showCompleteModal.value = false; showResults.value = true; viewMode.value = 'all'; currentIndex.value = 0; showAnswer.value = true; selectedOption.value = answers.value[0] }
const reviewWrong = () => { showCompleteModal.value = false; showResults.value = true; viewMode.value = 'wrong'; const w = Object.keys(results.value).map(Number).filter(i => !results.value[i].isCorrect).sort((a,b)=>a-b); if (w.length) { currentIndex.value = w[0]; showAnswer.value = true; selectedOption.value = answers.value[w[0]] } }
const formatTime = (s) => { const m = Math.floor(s/60); const sec = s%60; return `${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}` }

/* ========== 数据获取 ========== */
const fetchExercises = async () => {
  try {
    if (route.query.single === 'true' && route.query.exerciseId) { const r = await axios.get(`/api/exercises/${route.query.exerciseId}`); if (r.data.code===200) exercises.value=[r.data.data] }
    else if (route.query.multiple === 'true' && route.query.exerciseIds) { const r = await axios.get('/api/exercises/batch',{params:{ids:route.query.exerciseIds}}); if (r.data.code===200) exercises.value=r.data.data }
    else if (route.query.knowledgePoint) { const r = await axios.get('/api/exercises',{params:{knowledgePoint:route.query.knowledgePoint}}); if (r.data.code===200) exercises.value=r.data.data }
    else { const r = await axios.get('/api/exercises'); if (r.data.code===200) exercises.value=r.data.data }
  } catch(e) { console.error(e) }
  if (!exercises.value.length) exercises.value = generateMock()
}

const generateMock = () => {
  const qs = [
    { type:'选择题',question:'函数 f(x) = x² 在 x=1 处的导数值为？',options:['0','1','2','3'],correctAnswer:2,correctAnswerText:'C. 2',analysis:'由导数定义 f\'(1)=lim(h→0)[(1+h)²-1]/h=lim(h→0)(2h+h²)/h=2',knowledgePoint:'导数与微分',difficulty:'basic',difficultyText:'基础',score:10 },
    { type:'选择题',question:'极限 lim(x→0) sin(x)/x 的值为？',options:['0','1','∞','不存在'],correctAnswer:1,correctAnswerText:'B. 1',analysis:'这是重要极限公式，lim(x→0)sin(x)/x=1',knowledgePoint:'极限与连续',difficulty:'basic',difficultyText:'基础',score:10 },
    { type:'选择题',question:'以下哪个是复合函数的链式求导法则？',options:['(f+g)\'=f\'+g\'','(fg)\'=f\'g+fg\'','f(g(x))\'=f\'(g(x))g\'(x)','(f/g)\'=(f\'g-fg\')/g²'],correctAnswer:2,correctAnswerText:'C',analysis:'链式法则：外层函数导数乘以内层函数导数',knowledgePoint:'导数与微分',difficulty:'advanced',difficultyText:'进阶',score:15 },
    { type:'选择题',question:'定积分 ∫₀¹ x dx 的值为？',options:['0','1','0.5','2'],correctAnswer:2,correctAnswerText:'C. 0.5',analysis:'∫₀¹ x dx = [x²/2]₀¹ = 1/2',knowledgePoint:'定积分',difficulty:'basic',difficultyText:'基础',score:10 },
    { type:'填空题',question:'若 f(x) 在 x₀ 处可导，则 f(x) 在 x₀ 处必定____。',correctAnswer:'连续',correctAnswerText:'连续',analysis:'可导必连续，连续不一定可导',knowledgePoint:'导数与微分',difficulty:'basic',difficultyText:'基础',score:5 },
  ]
  return qs
}

onMounted(async () => {
  try { if (route.query.exercises) { const p = JSON.parse(route.query.exercises); if (p.length && p[0].question) exercises.value = p; else await fetchExercises() } else await fetchExercises() } catch(e) { await fetchExercises() }
  timer = setInterval(() => practiceTime.value++, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.pp-page {
  --c: #7c5cfc; --c2: #a78bfa; --c3: #c4b5fd; --cl: #f5f0ff; --cb: #ece6fa;
  --t: #2d2438; --m: #9088a0;
  height: calc(100vh - 60px); display: flex; flex-direction: column; overflow: hidden;
  background: linear-gradient(175deg, #fdfbff 0%, #f8f4fd 40%, #f5f0fa 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; color: var(--t);
}

/* ===== 顶部状态栏 ===== */
.pp-header { display: flex; align-items: center; gap: 16px; padding: 0 24px; height: 52px; flex-shrink: 0; background: rgba(255,255,255,.82); backdrop-filter: blur(14px); border-bottom: 1px solid var(--cb); z-index: 10; }
.pph-back { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border-radius: 8px; border: 1.5px solid var(--cb); background: rgba(255,255,255,.5); color: var(--c); font-size: 13px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; }
.pph-back:hover { background: var(--cl); border-color: var(--c2); }
.pph-mode { display: flex; align-items: center; gap: 8px; flex: 1; }
.pphm-icon { font-size: 20px; }
.pphm-text { display: flex; flex-direction: column; gap: 1px; }
.pphm-title { font-size: 14px; font-weight: 700; color: var(--t); }
.pphm-sub { font-size: 11px; color: var(--c); font-weight: 600; }
.pph-progress { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.pphp-text { font-size: 12px; font-weight: 700; color: var(--c); }
.pphp-bar { width: 120px; height: 5px; background: #f0ecf8; border-radius: 10px; overflow: hidden; }
.pphp-fill { height: 100%; border-radius: 10px; background: linear-gradient(90deg, var(--c2), var(--c)); transition: width .4s; }
.pph-timer { display: flex; align-items: center; gap: 4px; font-size: 13px; color: var(--m); font-weight: 600; }
.pph-timer svg { color: var(--c2); }
.pph-exit { padding: 6px 16px; border-radius: 8px; border: 1.5px solid rgba(239,68,68,.25); background: rgba(254,226,226,.3); color: #b91c1c; font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; }
.pph-exit:hover { background: #fee2e2; }

/* ===== 主体 ===== */
.pp-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* ===== 左侧题目区 ===== */
.pp-main { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; padding: 20px 24px 0; }

.pp-question { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 16px; overflow-y: auto; padding-right: 4px; }

/* 题目头部 */
.ppq-head { display: flex; flex-direction: column; gap: 8px; padding: 14px 18px; border-radius: 16px; background: rgba(255,255,255,.6); border: 1px solid var(--cb); }
.ppqh-tags { display: flex; align-items: center; gap: 8px; }
.ppqht-type { padding: 3px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; background: var(--cl); color: var(--c); }
.ppqht-diff { padding: 3px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; }
.ppqht-diff.basic, .ppqht-diff.easy { background: #ecfdf5; color: #047857; }
.ppqht-diff.advanced, .ppqht-diff.mid { background: #fffbeb; color: #b45309; }
.ppqht-diff.exam, .ppqht-diff.hard { background: #fef2f2; color: #b91c1c; }
.ppqht-score { margin-left: auto; font-size: 12px; color: var(--m); font-weight: 600; }
.ppqh-kp { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--c); }
.ppqh-kp svg { color: var(--c2); }
.ppqh-ai { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--m); }
.ppqh-ai svg { color: var(--c2); flex-shrink: 0; }

/* 题目卡片 */
.ppq-body { background: #fff; border-radius: 20px; padding: 28px 28px 32px; border: 1px solid rgba(220,210,245,.35); box-shadow: 0 8px 32px rgba(120,100,180,.05); }
.ppqb-question { font-size: 16px; line-height: 1.8; color: var(--t); }
.ppqbq-num { font-weight: 800; color: var(--c); margin-right: 6px; }

/* 选项 */
.ppqb-options { display: flex; flex-direction: column; gap: 10px; margin-top: 24px; }
.ppqbo-item { display: flex; align-items: center; gap: 14px; padding: 14px 18px; border-radius: 14px; border: 2px solid rgba(220,210,245,.35); cursor: pointer; transition: all .25s cubic-bezier(.4,0,.2,1); background: #fdfbff; }
.ppqbo-item:hover:not(.correct):not(.wrong) { border-color: var(--c3); background: #faf7ff; transform: translateY(-1px); }
.ppqbo-item.selected { border-color: var(--c); background: #f3edfc; box-shadow: 0 0 0 3px rgba(124,92,252,.08); }
.ppqbo-item.correct { border-color: #10b981; background: #ecfdf5; }
.ppqbo-item.wrong { border-color: #ef4444; background: #fef2f2; animation: shake .4s ease-in-out; }
@keyframes shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-6px)} 75%{transform:translateX(6px)} }
.ppqboi-letter { width: 28px; height: 28px; border-radius: 8px; background: var(--cl); color: var(--c); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; flex-shrink: 0; }
.ppqbo-item.selected .ppqboi-letter { background: var(--c); color: #fff; }
.ppqboi-text { flex: 1; font-size: 15px; color: var(--t); }
.ppqboi-icon { font-size: 18px; font-weight: 800; }
.ppqboi-icon.check { color: #10b981; }
.ppqboi-icon.cross { color: #ef4444; }

/* 填空/解答 */
.ppqb-fill input { width: 100%; max-width: 360px; padding: 12px 16px; border: 2px solid rgba(200,185,240,.5); border-radius: 12px; font-size: 15px; font-family: inherit; color: var(--t); outline: none; transition: all .2s; }
.ppqb-fill input:focus { border-color: var(--c); }
.ppqb-fill input:disabled { background: #f5f0ff; }
.ppqb-essay textarea { width: 100%; padding: 16px; border: 2px solid rgba(200,185,240,.5); border-radius: 14px; font-size: 15px; line-height: 1.7; font-family: inherit; color: var(--t); resize: vertical; outline: none; }
.ppqb-essay textarea:focus { border-color: var(--c); }
.ppqb-essay textarea:disabled { background: #f5f0ff; }

/* 解析 */
.ppq-analysis { padding: 20px 22px; border-radius: 16px; background: #fff; border: 1px solid rgba(220,210,245,.35); display: flex; flex-direction: column; gap: 12px; }
.ppqa-head { display: flex; justify-content: space-between; align-items: center; }
.ppqah-title { font-size: 15px; font-weight: 700; color: var(--t); }
.ppqah-result { padding: 4px 12px; border-radius: 8px; font-size: 12px; font-weight: 700; }
.ppqah-result.ok { background: #ecfdf5; color: #047857; }
.ppqah-result.fail { background: #fef2f2; color: #b91c1c; }
.ppqa-my, .ppqa-right { padding: 10px 14px; border-radius: 10px; font-size: 13px; }
.ppqa-my { border: 1.5px solid; }
.ppqa-my.ok { border-color: #10b981; background: #ecfdf5; }
.ppqa-my.fail { border-color: #ef4444; background: #fef2f2; }
.ppqa-right { background: #f5f0ff; }
.ppqam-label, .ppqar-label, .ppqad-label { font-weight: 600; color: var(--m); margin-right: 8px; font-size: 12px; }
.ppqam-val, .ppqar-val { font-weight: 600; color: var(--t); }
.ppqa-detail p { margin: 6px 0 0; font-size: 13px; line-height: 1.6; color: var(--t); }

/* 底部操作栏 */
.pp-actions { display: flex; justify-content: center; gap: 12px; padding: 14px 0 18px; flex-shrink: 0; }
.ppa-btn { padding: 11px 28px; border-radius: 12px; font-size: 14px; font-weight: 700; cursor: pointer; font-family: inherit; transition: all .25s; border: none; }
.ppa-btn.prev { background: #f5f0ff; color: var(--c); border: 1.5px solid rgba(200,185,240,.4); }
.ppa-btn.prev:hover:not(:disabled) { background: #ede4fe; }
.ppa-btn.next { background: linear-gradient(135deg, #7c5cfc, #9b7cff); color: #fff; box-shadow: 0 4px 12px rgba(124,92,252,.2); }
.ppa-btn.next:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(124,92,252,.35); }
.ppa-btn.submit { background: linear-gradient(135deg, #10b981, #059669); color: #fff; box-shadow: 0 4px 12px rgba(16,185,129,.2); }
.ppa-btn.submit:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(16,185,129,.35); }
.ppa-btn:disabled { opacity: .4; cursor: not-allowed; }

/* ===== 右侧栏 ===== */
.pp-side { width: 260px; flex-shrink: 0; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }

.pps-card { background: rgba(255,255,255,.75); backdrop-filter: blur(12px); border-radius: 16px; padding: 16px; border: 1px solid rgba(220,210,245,.3); }
.ppsc-head { font-size: 12px; font-weight: 700; color: var(--m); margin-bottom: 12px; text-transform: uppercase; letter-spacing: .4px; }

/* 答题卡 */
.ppsc-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 7px; }
.ppscg-dot { width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; cursor: pointer; transition: all .2s; border: 2px solid #e8e2f4; color: var(--m); background: #faf8fd; }
.ppscg-dot:hover { border-color: var(--c2); }
.ppscg-dot.current { border-color: var(--c); background: #ede4fe; color: var(--c); box-shadow: 0 0 0 4px rgba(124,92,252,.1); }
.ppscg-dot.answered { background: #e0d6f8; border-color: var(--c2); color: var(--c); }
.ppscg-dot.correct { background: #d1fae5; border-color: #10b981; color: #047857; }
.ppscg-dot.wrong { background: #fee2e2; border-color: #ef4444; color: #b91c1c; }

.ppsc-legend { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; font-size: 10px; color: var(--m); }
.ppsc-legend span { display: flex; align-items: center; gap: 4px; }
.l-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.l-dot.cur { background: var(--c); box-shadow: 0 0 0 2px rgba(124,92,252,.2); }
.l-dot.done { background: var(--c2); }
.l-dot.ok { background: #10b981; }
.l-dot.fail { background: #ef4444; }

/* 统计 */
.pps-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.ppss-item { text-align: center; padding: 12px 8px; border-radius: 12px; background: rgba(248,246,255,.6); }
.ppssi-val { font-size: 20px; font-weight: 800; color: var(--t); display: block; }
.ppssi-lbl { font-size: 10px; color: var(--m); margin-top: 2px; display: block; }

/* AI 助手 */
.ai-card { background: linear-gradient(135deg, #faf7ff, #fdfbff); border-color: rgba(167,139,250,.25); }
.ppai-tips { display: flex; flex-direction: column; gap: 8px; }
.ppait-row { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--t); line-height: 1.5; }
.ppait-row strong { color: var(--c); }
.ppait-icon { font-size: 14px; flex-shrink: 0; }

/* ===== 弹窗 ===== */
.pp-overlay { position: fixed; inset: 0; background: rgba(45,36,56,.25); z-index: 2000; display: flex; align-items: center; justify-content: center; }
.pp-complete { background: rgba(255,255,255,.95); backdrop-filter: blur(16px); border-radius: 24px; padding: 36px 32px; text-align: center; max-width: 440px; width: 90%; box-shadow: 0 24px 64px rgba(124,92,252,.15); border: 1px solid rgba(200,185,240,.3); }
.ppc-icon { font-size: 56px; margin-bottom: 12px; }
.pp-complete h2 { font-size: 22px; font-weight: 800; margin: 0 0 20px; color: var(--t); }
.ppc-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 24px; }
.ppcs-item { padding: 12px 6px; border-radius: 12px; background: rgba(248,246,255,.6); font-size: 11px; color: var(--m); }
.ppcs-item span { font-size: 18px; font-weight: 800; color: var(--t); display: block; }
.ppcs-item.ok { background: #ecfdf5; } .ppcs-item.ok span { color: #047857; }
.ppcs-item.fail { background: #fef2f2; } .ppcs-item.fail span { color: #b91c1c; }
.ppc-btns { display: flex; gap: 8px; justify-content: center; }
.ppcb-btn { padding: 10px 22px; border-radius: 12px; font-size: 14px; font-weight: 700; cursor: pointer; font-family: inherit; border: none; transition: all .2s; }
.ppcb-btn.sec { background: #f5f0ff; color: var(--c); border: 1.5px solid rgba(200,185,240,.4); }
.ppcb-btn.sec:hover { background: #ede4fe; }
.ppcb-btn.pri { background: linear-gradient(135deg, #7c5cfc, #9b7cff); color: #fff; }
.ppcb-btn.pri:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124,92,252,.3); }

.pp-confirm { background: rgba(255,255,255,.95); backdrop-filter: blur(16px); border-radius: 20px; padding: 28px 32px; text-align: center; max-width: 380px; width: 90%; box-shadow: 0 24px 64px rgba(124,92,252,.1); border: 1px solid rgba(200,185,240,.3); }
.pp-confirm h3 { font-size: 18px; font-weight: 800; margin: 0 0 8px; color: var(--t); }
.pp-confirm p { font-size: 13px; color: var(--m); margin: 0 0 20px; }
.ppcf-btns { display: flex; gap: 10px; justify-content: center; }
.ppcfb-cancel { padding: 10px 22px; border-radius: 12px; border: 1.5px solid rgba(200,185,240,.4); background: #f5f0ff; color: var(--t); font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit; }
.ppcfb-cancel:hover { background: #ede4fe; }
.ppcfb-ok { padding: 10px 22px; border-radius: 12px; border: none; background: linear-gradient(135deg, #7c5cfc, #9b7cff); color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; font-family: inherit; }
.ppcfb-ok:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124,92,252,.3); }

.pp-main::-webkit-scrollbar, .pp-side::-webkit-scrollbar { width: 4px; }
.pp-main::-webkit-scrollbar-thumb, .pp-side::-webkit-scrollbar-thumb { background: rgba(200,185,240,.4); border-radius: 4px; }

@media (max-width: 900px) {
  .pp-side { display: none; }
  .pp-main { padding: 14px 16px 0; }
}
@media (max-width: 640px) {
  .pph-timer, .pph-mode .pphm-sub { display: none; }
}
</style>
