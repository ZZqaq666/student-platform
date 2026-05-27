<template>
  <div class="page" :class="{ show: ready }">
    <div class="bg-decor">
      <span v-for="s in 12" :key="'star' + s" class="deco-star" :style="decoStyle(s, 'star')">✦</span>
      <span v-for="c in 5" :key="'cloud' + c" class="deco-cloud" :style="decoStyle(c, 'cloud')" />
      <span v-for="d in 8" :key="'dot' + d" class="deco-dot" :style="decoStyle(d, 'dot')" />
      <span v-for="b in 4" :key="'book' + b" class="deco-book" :style="decoStyle(b, 'book')">📖</span>
    </div>

    <div class="main">
      <section class="hero">
        <div class="hero-inner">
          <!-- 左：信息 -->
          <div class="hero-left">
            <h1 class="hero-hi">{{ greeting }}</h1>
            <p class="hero-msg">{{ encourageMsg }}</p>

            <div class="hero-goal">
              <div class="goal-head">
                <span>🎯 今日目标：阅读 30 分钟</span>
                <span class="goal-pct">60%</span>
              </div>
              <div class="goal-bar"><div class="goal-fill" style="width:60%"></div></div>
            </div>

            <div class="hero-pills">
              <span class="pill">Lv.{{ level }}</span>
              <span class="pill pill-fire">🔥 {{ streakDays }} 天</span>
              <span class="pill pill-star">⭐ {{ unlockedBadgeCount }} 徽章</span>
              <span class="pill pill-xp">EXP {{ xpCurrent }}/{{ xpTarget }}</span>
            </div>
          </div>

          <!-- 右：AI 伙伴 -->
          <div class="hero-right">
            <div class="buddy-scene">
              <div class="buddy-card">
                <span class="buddy-chip chip-a">AI</span>
                <span class="buddy-chip chip-b">+{{ xpCurrent }}</span>
                <div class="book-buddy">
                  <div class="book-sprout">
                    <span class="sprout-stem"></span>
                    <span class="sprout-leaf leaf-l"></span>
                    <span class="sprout-leaf leaf-r"></span>
                  </div>
                  <div class="buddy-book">
                    <div class="book-cover">
                      <div class="book-title-line line-1"></div>
                      <div class="book-title-line line-2"></div>
                      <div class="book-smile"></div>
                      <div class="book-heart">✦</div>
                    </div>
                    <div class="book-pages"></div>
                    <div class="book-side"></div>
                  </div>
                  <div class="buddy-arm arm-left"></div>
                  <div class="buddy-arm arm-right"></div>
                  <div class="buddy-stars">
                    <span>✦</span><span>✧</span><span>✦</span>
                  </div>
                </div>
                <div class="buddy-shadow"></div>
              </div>
              <div class="buddy-bubble">今天也要加油哦 🌱</div>
            </div>
          </div>
        </div>

        <!-- 底部任务卡片 -->
        <div class="hero-tasks">
          <div class="ht-head">
            <span class="ht-title">今日任务</span>
            <div class="ht-head-right">
              <button class="ht-add-btn" @click.stop="showTaskInput = !showTaskInput">+ 添加任务</button>
              <span class="ht-count">{{ completedTasks }}/{{ tasks.length }} 完成</span>
            </div>
          </div>

          <!-- 添加任务输入行 -->
          <div v-if="showTaskInput" class="ht-add-row">
            <input
              v-model="newTaskText" placeholder="输入新任务..."
              class="ht-add-input" @keydown.enter="addTask"
              ref="taskInputRef"
            />
            <button class="ht-add-confirm" @click="addTask">添加</button>
            <button class="ht-add-cancel" @click="cancelAddTask">取消</button>
          </div>

          <div class="ht-grid">
            <div v-for="(t, i) in tasks" :key="i"
              class="ht-card" :class="{ done: t.done }" @click="toggleTask(i)">
              <span class="ht-card-icon">{{ t.done ? '✓' : '○' }}</span>
              <span class="ht-card-text">{{ t.text }}</span>
              <span class="ht-del" @click.stop="removeTask(i)" title="删除">×</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 功能卡片 Bento 2x2 -->
      <section class="bento">
        <!-- AI 问答：主卡片 -->
        <div class="b-card b-card-main" @click="toPage('/qa')">
          <div class="bc-illus">
            <span class="bc-emoji">💬</span>
            <span class="bc-spark s1">✨</span>
            <span class="bc-spark s2">💡</span>
          </div>
          <div class="bc-body">
            <span class="bc-name">AI 问答中心</span>
            <span class="bc-desc">智能对话，秒级解答</span>
          </div>
          <span class="bc-go">→</span>
        </div>

        <!-- 知识图谱 -->
        <div class="b-card" @click="toPage('/graph')">
          <div class="bc-illus bc-illus-graph">
            <span class="bc-node n1"></span><span class="bc-node n2"></span><span class="bc-node n3"></span>
          </div>
          <div class="bc-body">
            <span class="bc-name">知识图谱</span>
            <span class="bc-desc">探索学科知识关联</span>
          </div>
          <span class="bc-go">→</span>
        </div>

        <!-- 个人书架 -->
        <div class="b-card" @click="toPage('/bookshelf')">
          <div class="bc-illus bc-illus-books">
            <span class="bc-book">📕</span><span class="bc-book b2">📗</span><span class="bc-book b3">📘</span>
          </div>
          <div class="bc-body">
            <span class="bc-name">个人书架</span>
            <span class="bc-desc">管理你的藏书阁</span>
          </div>
          <span class="bc-go">→</span>
        </div>

        <!-- 学长答疑 -->
        <div class="b-card" @click="toPage('/senior')">
          <div class="bc-illus bc-illus-senior">
            <span>👨‍🎓</span><span class="bc-sp">👩‍🎓</span>
          </div>
          <div class="bc-body">
            <span class="bc-name">学长答疑</span>
            <span class="bc-desc">与前辈交流学习</span>
          </div>
          <span class="bc-go">→</span>
        </div>
      </section>

      <div class="bottom-row">
        <!-- 左侧：继续学习 -->
        <div class="desk-card" v-if="recentBook">
          <div class="desk-head">
            <span class="desk-head-title">继续学习</span>
            <span class="desk-head-chapter">{{ recentBook.chapter || '第3章 · 导数应用' }}</span>
          </div>
          <div class="desk-body">
            <div class="desk-book-3d">
              <div class="db3-spine"></div>
              <div class="db3-pages"></div>
              <div class="db3-cover">
                <img v-if="recentBook.cover" :src="recentBook.cover" :alt="recentBook.title" />
                <span v-else class="db3-placeholder">📘</span>
                <div class="db3-shine"></div>
              </div>
            </div>
            <div class="desk-info">
              <h3 class="di-name">{{ recentBook.title }}</h3>
              <p class="di-author">{{ recentBook.author }}</p>
              <div class="di-meta">
                <span>📄 {{ recentBook.chapters || 12 }} 章</span>
                <span>⏱️ 今日 {{ todayReadingMin }} 分钟</span>
                <span>🕐 {{ recentBook.lastRead || '今天 14:30' }}</span>
              </div>
              <div class="di-progress">
                <div class="di-bar"><div class="di-fill" :style="{ width: recentBook.progress + '%' }"></div></div>
                <span class="di-pct">{{ recentBook.progress }}%</span>
              </div>
              <button class="di-btn di-btn-main" @click="toPage('/bookshelf')">📖 继续阅读</button>
            </div>
          </div>
        </div>

        <div class="desk-card desk-empty" v-else>
          <div class="de-illustration">📚</div>
          <p class="de-title">还没有正在阅读的书</p>
          <p class="de-sub">选择一本书，开始你的学习之旅</p>
          <button class="di-btn di-btn-main" @click="toPage('/bookshelf')">去书架选书 →</button>
        </div>

        <!-- 右侧：学习轨迹时间轴 -->
        <div class="tl-card">
          <div class="tl-head">
            <span>学习轨迹</span>
            <span class="tl-week">近 7 天</span>
          </div>
          <div class="tl-track">
            <!-- 时间轴线 -->
            <div class="tl-line"></div>
            <div class="tl-line-done" :style="{ width: tlDoneWidth + '%' }"></div>
            <!-- 节点 -->
            <div class="tl-nodes">
              <div v-for="(n, i) in tlNodes" :key="i" class="tl-node-col">
                <div class="tl-dot" :class="{ done: n.done, current: n.current }">
                  <span v-if="n.done">✓</span>
                  <span v-else-if="n.current" class="tl-dot-curr"></span>
                  <span v-else></span>
                </div>
                <span class="tl-date">{{ n.date }}</span>
                <span class="tl-topic">{{ n.topic }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/api.js'

const router = useRouter()
const toPage = (path) => router.push(path)

const ready = ref(false)
onMounted(async () => {
  await nextTick()
  setTimeout(() => {
    ready.value = true
  }, 60)
})

const hour = new Date().getHours()
const greeting = computed(() => {
  if (hour < 6) return '夜深了，也要记得休息哦'
  if (hour < 9) return '早上好！新的一天开始啦'
  if (hour < 12) return '上午好！精力充沛～'
  if (hour < 14) return '中午好！别忘了午休'
  if (hour < 18) return '下午好！继续加油'
  return '晚上好！今天辛苦了'
})

const encourageMsg = computed(() => {
  const msgs = [
    '每一次学习，都是成长的足迹 🌱',
    '今天的努力，是明天的底气 ✨',
    '学习如登山，每一步都算数 ⛰️',
    '小积累，大改变，一起加油吧 🚀',
    '知识是最好的装备，武装自己吧 🛡️',
  ]
  return msgs[new Date().getDate() % msgs.length]
})

const decoStyle = (i, type) => {
  const seed = i * 7 + type.length * 13
  const pseudo = ((seed * 31) % 100) / 100

  if (type === 'star') {
    return {
      left: `${(i * 37 + 11) % 100}%`,
      top: `${(i * 23 + 7) % 100}%`,
      fontSize: `${pseudo * 10 + 8}px`,
      opacity: pseudo * 0.15 + 0.04,
      animationDelay: `${pseudo * 4}s`,
      animationDuration: `${pseudo * 3 + 3}s`,
    }
  }

  if (type === 'cloud') {
    return {
      left: `${(i * 41 + 19) % 100}%`,
      top: `${(i * 29 + 13) % 100}%`,
      width: `${pseudo * 60 + 40}px`,
      height: `${pseudo * 20 + 16}px`,
      opacity: pseudo * 0.1 + 0.03,
      animationDelay: `${pseudo * 6}s`,
      animationDuration: `${pseudo * 8 + 12}s`,
    }
  }

  if (type === 'dot') {
    return {
      left: `${(i * 53 + 3) % 100}%`,
      top: `${(i * 47 + 5) % 100}%`,
      width: `${pseudo * 6 + 3}px`,
      height: `${pseudo * 6 + 3}px`,
      opacity: pseudo * 0.12 + 0.03,
      animationDelay: `${pseudo * 3}s`,
    }
  }

  return {
    left: `${(i * 59 + 17) % 100}%`,
    top: `${(i * 31 + 23) % 100}%`,
    fontSize: `${pseudo * 10 + 12}px`,
    opacity: pseudo * 0.08 + 0.03,
    animationDelay: `${pseudo * 5}s`,
  }
}

const level = ref(7)
const xpCurrent = ref(420)
const xpTarget = ref(600)
const xpPercent = computed(() => Math.round((xpCurrent.value / xpTarget.value) * 100))
const streakDays = ref(7)
const badges = ref(5)

const tasks = ref([
  { text: '完成一次 AI 问答', done: true },
  { text: '浏览知识图谱', done: true },
  { text: '阅读书籍 30 分钟', done: false },
  { text: '整理学习笔记', done: false },
])

const showTaskInput = ref(false)
const newTaskText = ref('')
const taskInputRef = ref(null)

const toggleTask = (i) => {
  tasks.value[i].done = !tasks.value[i].done
}

const addTask = () => {
  const text = newTaskText.value.trim()
  if (!text) return
  tasks.value.push({ text, done: false })
  newTaskText.value = ''
  showTaskInput.value = false
}

const cancelAddTask = () => {
  newTaskText.value = ''
  showTaskInput.value = false
}

const removeTask = (i) => {
  tasks.value.splice(i, 1)
}

const achievements = ref([
  { icon: '🌟', name: '初次登录', locked: false },
  { icon: '🔥', name: '连续学习3天', locked: false },
  { icon: '📚', name: '读完一本书', locked: false },
  { icon: '💬', name: '提问10次', locked: false },
  { icon: '🏆', name: '学习达人', locked: true },
  { icon: '🎯', name: '满勤一周', locked: false },
])

const streakMsg = computed(() => {
  if (streakDays.value >= 7) return '太厉害了！本周全勤！🏆'
  if (streakDays.value >= 5) return '势头不错，继续坚持！💪'
  if (streakDays.value >= 3) return '渐入佳境，保持住～'
  return '好的开始，加油！'
})

const unlockedBadgeCount = computed(() => achievements.value.filter(a => !a.locked).length)
const completedTasks = computed(() => tasks.value.filter(t => t.done).length)

const recentBook = ref(null)
const learningData = ref([])

const fetchRecentBook = async () => {
  try {
    const res = await api.get('/books/recent')
    if (res.code === 200) recentBook.value = res.data
  } catch (e) {}
}

const fetchLearningData = async () => {
  try {
    const res = await api.get('/learning/history')
    if (res.code === 200) learningData.value = res.data
  } catch (e) {}
}

onMounted(async () => {
  await fetchRecentBook()
  await fetchLearningData()
})

const dayLabels = ['一', '二', '三', '四', '五', '六', '日']

/* ===== 学习轨迹时间轴 ===== */
const todayReadingMin = ref(28)

const tlNodes = computed(() => {
  const base = [
    { date: '05-18', topic: 'AI问答',   done: true,  current: false },
    { date: '05-19', topic: '知识图谱', done: true,  current: false },
    { date: '05-20', topic: '阅读练习', done: true,  current: false },
    { date: '05-21', topic: '整理笔记', done: true,  current: false },
    { date: '05-22', topic: '复习总结', done: false, current: false },
    { date: '05-23', topic: '练习题',   done: false, current: false },
    { date: '05-24', topic: '今日',     done: false, current: true  },
  ]
  if (learningData.value.length > 0) {
    return learningData.value.map((pt, i) => ({
      date: pt.date || base[i]?.date || '',
      topic: pt.topic || base[i]?.topic || '',
      done: pt.progress > 0,
      current: i === learningData.value.length - 1,
    })).slice(0, 7)
  }
  return base
})

const tlDoneWidth = computed(() => {
  const nodes = tlNodes.value
  let lastDone = -1
  nodes.forEach((n, i) => { if (n.done) lastDone = i })
  if (lastDone < 0) return 0
  return Math.round((lastDone / (nodes.length - 1)) * 100)
})

const pathNodes = computed(() => {
  if (learningData.value.length > 0) {
    return learningData.value.map((pt, i) => ({
      day: pt.date || dayLabels[i],
      done: pt.progress > 0,
      current: i === learningData.value.length - 1 && pt.progress > 0,
    }))
  }

  return [
    { day: '一', done: true, current: false },
    { day: '二', done: true, current: false },
    { day: '三', done: true, current: false },
    { day: '四', done: true, current: false },
    { day: '五', done: false, current: true },
    { day: '六', done: false, current: false },
    { day: '日', done: false, current: false },
  ]
})

const pathLine = computed(() => {
  const n = pathNodes.value.length
  if (!n) return ''
  const stepX = 300 / (n - 1)

  return pathNodes.value.map((_, i) => {
    const x = i * stepX
    return i === 0 ? `M${x},30` : `L${x},30`
  }).join(' ')
})

const pathDoneLine = computed(() => {
  const n = pathNodes.value
  if (!n.length) return ''

  const stepX = 300 / (n.length - 1)
  let lastDone = -1

  n.forEach((nd, i) => {
    if (nd.done) lastDone = i
  })

  if (lastDone < 0) return ''

  return n.slice(0, lastDone + 1).map((_, i) => {
    const x = i * stepX
    return i === 0 ? `M${x},30` : `L${x},30`
  }).join(' ')
})
</script>

<style scoped>
.page {
  --c1: #7c5cfc;
  --c2: #a78bfa;
  --c3: #5b9dfc;
  --c4: #5cc9a0;
  --c5: #ffb84d;
  --c6: #fc7bab;
  --radius: 24px;

  position: relative;
  min-height: calc(100vh - 60px);
  padding: 28px 32px 52px;
  background:
    radial-gradient(circle at 8% 12%, rgba(167, 139, 250, .16), transparent 26%),
    radial-gradient(circle at 92% 20%, rgba(91, 157, 252, .12), transparent 28%),
    radial-gradient(circle at 70% 88%, rgba(252, 123, 171, .10), transparent 30%),
    linear-gradient(175deg, #fffdfd 0%, #f9f6ff 34%, #f8fafd 64%, #fff8fb 100%);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
}

.main > * {
  opacity: 0;
  transform: translateY(24px);
}

.page.show .main > * {
  animation: sectionIn .7s ease-out forwards;
}

.page.show .main > *:nth-child(1) { animation-delay: .05s; }
.page.show .main > *:nth-child(2) { animation-delay: .15s; }
.page.show .main > *:nth-child(3) { animation-delay: .25s; }
.page.show .main > *:nth-child(4) { animation-delay: .35s; }

@keyframes sectionIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.bg-decor {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.deco-star {
  position: absolute;
  color: #fbbf24;
  animation: starTwinkle 3s ease-in-out infinite;
}

@keyframes starTwinkle {
  0%, 100% {
    opacity: .04;
    transform: scale(1);
  }
  50% {
    opacity: .18;
    transform: scale(1.3);
  }
}

.deco-cloud {
  position: absolute;
  border-radius: 50%;
  background: rgba(199, 180, 255, .25);
  filter: blur(30px);
  animation: cloudDrift 16s ease-in-out infinite;
}

@keyframes cloudDrift {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(30px);
  }
}

.deco-dot {
  position: absolute;
  border-radius: 50%;
  background: #c4b5fd;
  animation: dotPulse 4s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% {
    opacity: .05;
    transform: scale(1);
  }
  50% {
    opacity: .18;
    transform: scale(1.8);
  }
}

.deco-book {
  position: absolute;
  animation: bookFloat 6s ease-in-out infinite;
}

@keyframes bookFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-12px) rotate(5deg);
  }
}

.card-soft {
  background: rgba(255, 255, 255, .82);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border-radius: var(--radius);
  border: 1px solid rgba(230, 224, 244, .7);
  box-shadow:
    0 20px 60px rgba(124, 92, 252, .06),
    0 2px 10px rgba(120, 100, 180, .04),
    inset 0 1px 0 rgba(255, 255, 255, .9);
}

.main {
  position: relative;
  z-index: 1;
  max-width: 1140px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* ===== Hero 主卡 ===== */
.hero {
  background: rgba(255,255,255,.75); backdrop-filter: blur(16px);
  border-radius: 22px; padding: 0;
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 2px 20px rgba(120,100,180,.05);
  overflow: hidden;
}

.hero-inner {
  display: grid; grid-template-columns: 6fr 4fr; gap: 30px;
  padding: 32px 40px 22px; align-items: center;
}

.hero-left {
  display: flex; flex-direction: column; gap: 10px;
  min-width: 0;
}

.hero-hi {
  margin: 0; font-size: 28px; font-weight: 900; color: #1a1528; letter-spacing: -.5px;
  line-height: 1.2;
}

.hero-msg {
  margin: 0; font-size: 14px; color: #807a90;
}

.hero-goal {
  margin-top: 4px; background: rgba(248,246,255,.9); border-radius: 14px;
  padding: 12px 16px; max-width: 320px; border: 1px solid #ece6fa;
}

.goal-head { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; }
.goal-head span:first-child { color: #5b4a8a; font-weight: 700; }
.goal-pct { color: #7c5cfc; font-weight: 800; }

.goal-bar { height: 7px; background: #e8e2f8; border-radius: 99px; overflow: hidden; }

.goal-fill {
  height: 100%; border-radius: 99px;
  background: linear-gradient(90deg, #a78bfa, #7c5cfc);
  box-shadow: 0 0 12px rgba(124,92,252,.28);
}

.hero-right {
  display: flex; align-items: center; justify-content: center;
}

.buddy-scene {
  position: relative;
  width: 300px;
  height: 210px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.buddy-card {
  position: relative;
  width: 230px;
  height: 178px;
  border-radius: 34px;
  background:
    radial-gradient(circle at 24% 22%, rgba(255, 255, 255, .96), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, .86), rgba(245, 239, 255, .72));
  border: 1px solid rgba(223, 212, 255, .72);
  box-shadow:
    0 24px 60px rgba(124, 92, 252, .12),
    inset 0 1px 0 rgba(255, 255, 255, .95);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.buddy-chip {
  position: absolute;
  z-index: 4;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .92);
  border: 1px solid rgba(226, 216, 255, .85);
  box-shadow: 0 8px 18px rgba(124, 92, 252, .08);
  color: #6d55d8;
  font-size: 12px;
  font-weight: 900;
  display: flex;
  align-items: center;
}

.chip-a {
  top: 18px;
  left: 20px;
  transform: rotate(-8deg);
}

.chip-b {
  right: 18px;
  bottom: 24px;
  color: #16a34a;
  transform: rotate(7deg);
}

.book-buddy {
  position: relative;
  width: 118px;
  height: 138px;
  animation: buddyFloat 3.8s ease-in-out infinite;
}

@keyframes buddyFloat {
  0%, 100% {
    transform: translateY(0) rotate(-1deg);
  }
  50% {
    transform: translateY(-10px) rotate(1.5deg);
  }
}

.buddy-book {
  position: absolute;
  left: 50%;
  top: 34px;
  transform: translateX(-50%);
  width: 98px;
  height: 104px;
}

.book-cover {
  position: absolute;
  inset: 0 8px 0 0;
  z-index: 2;
  border-radius: 18px 24px 24px 18px;
  background:
    linear-gradient(145deg, #9f8cff 0%, #7c5cfc 52%, #6d55e9 100%);
  box-shadow:
    0 18px 32px rgba(124, 92, 252, .26),
    inset 8px 0 0 rgba(255, 255, 255, .16),
    inset 0 1px 0 rgba(255, 255, 255, .4);
  overflow: hidden;
}

.book-cover::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 68% 28%, rgba(255, 255, 255, .28), transparent 18%),
    linear-gradient(120deg, transparent 0%, rgba(255,255,255,.18) 42%, transparent 62%);
}

.book-title-line {
  position: absolute;
  left: 24px;
  height: 6px;
  border-radius: 99px;
  background: rgba(255, 255, 255, .72);
}

.line-1 {
  top: 28px;
  width: 42px;
}

.line-2 {
  top: 40px;
  width: 30px;
  opacity: .66;
}

.book-smile {
  position: absolute;
  left: 50%;
  top: 62px;
  width: 22px;
  height: 12px;
  transform: translateX(-50%);
  border-bottom: 3px solid rgba(255, 255, 255, .86);
  border-radius: 0 0 20px 20px;
}

.book-heart {
  position: absolute;
  right: 15px;
  bottom: 16px;
  color: #fff7b8;
  font-size: 16px;
  animation: heartPop 2.4s ease-in-out infinite;
}

@keyframes heartPop {
  0%, 100% {
    transform: scale(.9) rotate(0deg);
    opacity: .72;
  }
  50% {
    transform: scale(1.15) rotate(10deg);
    opacity: 1;
  }
}

.book-pages {
  position: absolute;
  right: 0;
  top: 8px;
  width: 18px;
  height: 90px;
  border-radius: 0 18px 18px 0;
  background:
    repeating-linear-gradient(
      0deg,
      #fffdf7 0,
      #fffdf7 5px,
      #efe7d8 6px
    );
  box-shadow: 6px 10px 18px rgba(70, 50, 130, .1);
}

.book-side {
  position: absolute;
  left: -6px;
  top: 8px;
  width: 12px;
  height: 92px;
  z-index: 1;
  border-radius: 10px 0 0 10px;
  background: linear-gradient(180deg, #6d55e9, #5b45d2);
}

.book-sprout {
  position: absolute;
  left: 50%;
  top: 3px;
  width: 46px;
  height: 44px;
  transform: translateX(-50%);
  z-index: 5;
  animation: sproutWave 3s ease-in-out infinite;
  transform-origin: bottom center;
}

@keyframes sproutWave {
  0%, 100% {
    transform: translateX(-50%) rotate(0deg);
  }
  50% {
    transform: translateX(-50%) rotate(5deg);
  }
}

.sprout-stem {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 5px;
  height: 28px;
  border-radius: 999px;
  background: linear-gradient(180deg, #8bdc9d, #39b874);
  transform: translateX(-50%);
}

.sprout-leaf {
  position: absolute;
  bottom: 16px;
  width: 24px;
  height: 17px;
  background: linear-gradient(135deg, #9ff0b6, #43c77d);
  border-radius: 100% 0 100% 0;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.5);
}

.leaf-l {
  left: 0;
  transform: rotate(-28deg);
}

.leaf-r {
  right: 0;
  transform: scaleX(-1) rotate(-28deg);
}

.buddy-arm {
  position: absolute;
  top: 82px;
  width: 28px;
  height: 12px;
  border-radius: 999px;
  background: #8d79ff;
  box-shadow: 0 6px 12px rgba(124, 92, 252, .16);
  z-index: 1;
  animation: armWave 2.8s ease-in-out infinite;
}

.arm-left {
  left: 0;
  transform: rotate(-22deg);
}

.arm-right {
  right: 0;
  transform: rotate(22deg);
  animation-delay: .3s;
}

@keyframes armWave {
  0%, 100% {
    transform: rotate(var(--r, 22deg));
  }
  50% {
    transform: rotate(36deg);
  }
}

.arm-left {
  --r: -22deg;
}

.arm-right {
  --r: 22deg;
}

.buddy-stars span {
  position: absolute;
  z-index: 6;
  color: #f8c84f;
  font-size: 15px;
  animation: buddyStar 2.8s ease-in-out infinite;
}

.buddy-stars span:nth-child(1) {
  left: -2px;
  top: 34px;
}

.buddy-stars span:nth-child(2) {
  right: 4px;
  top: 24px;
  animation-delay: .8s;
}

.buddy-stars span:nth-child(3) {
  right: 10px;
  bottom: 14px;
  animation-delay: 1.4s;
}

@keyframes buddyStar {
  0%, 100% {
    transform: scale(.7) rotate(0deg);
    opacity: .35;
  }
  50% {
    transform: scale(1.2) rotate(18deg);
    opacity: 1;
  }
}

.buddy-shadow {
  position: absolute;
  left: 50%;
  bottom: 18px;
  width: 112px;
  height: 18px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(124, 92, 252, .16), transparent 70%);
  animation: buddyShadow 3.8s ease-in-out infinite;
}

@keyframes buddyShadow {
  0%, 100% {
    transform: translateX(-50%) scale(1);
    opacity: .6;
  }
  50% {
    transform: translateX(-50%) scale(.78);
    opacity: .34;
  }
}

.buddy-bubble {
  position: absolute;
  top: 16px;
  right: -6px;
  z-index: 8;
  padding: 10px 16px;
  border-radius: 18px 18px 6px 18px;
  background: rgba(255, 255, 255, .96);
  border: 1px solid rgba(230, 224, 244, .72);
  box-shadow: 0 12px 26px rgba(96, 76, 150, .08);
  color: #5b4a8a;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  animation: bubbleIn .55s cubic-bezier(.34, 1.56, .64, 1) both;
}

@keyframes bubbleIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(.7);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ===== 功能卡片 Bento 2x2 ===== */
.bento {
  display: grid; grid-template-columns: 1fr 1fr; gap: 14px;
}

.b-card {
  background: rgba(255,255,255,.75); backdrop-filter: blur(14px);
  border-radius: 18px; padding: 24px;
  border: 1px solid rgba(230,224,244,.6);
  cursor: pointer; transition: all .35s cubic-bezier(.4,0,.2,1);
  display: flex; flex-direction: column; gap: 14px;
  position: relative; overflow: hidden;
}

.b-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(120,100,180,.08), 0 0 0 1.5px rgba(160,140,220,.2);
}

.b-card-main { background: linear-gradient(160deg, rgba(255,255,255,.85), rgba(248,244,255,.8)); }

/* 卡片内部 */
.bc-illus { position: relative; display: flex; align-items: center; gap: 4px; }
.b-card-main .bc-illus { justify-content: center; padding: 16px 0 8px; }

.bc-emoji { font-size: 50px; animation: chatBob 3s ease-in-out infinite; }
@keyframes chatBob { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-8px)} }

.bc-spark { position: absolute; font-size: 16px; animation: sparklePop 2.2s ease-in-out infinite; }
.bc-spark.s1 { top: 0; left: 20%; animation-delay: 0s; }
.bc-spark.s2 { bottom: 0; right: 22%; animation-delay: .8s; }
@keyframes sparklePop { 0%,100%{opacity:.15;transform:scale(.6)} 50%{opacity:1;transform:scale(1.1)} }

/* 知识图谱节点 */
.bc-illus-graph { width: 52px; height: 52px; flex-shrink: 0; position: relative; }
.bc-node { position: absolute; width: 12px; height: 12px; border-radius: 50%; }
.bc-node.n1 { top: 4px; left: 20px; background: #7c5cfc; }
.bc-node.n2 { top: 20px; left: 4px; background: #a78bfa; }
.bc-node.n3 { top: 20px; right: 4px; background: #c4b5fd; }

/* 书架 */
.bc-illus-books { display: flex; gap: 2px; flex-shrink: 0; }
.bc-book { font-size: 22px; transition: transform .3s cubic-bezier(.34,1.56,.64,1); }
.b-card:hover .bc-book.b2 { transform: translateY(-8px); }
.b-card:hover .bc-book.b3 { transform: translateY(-4px); }

/* 学长 */
.bc-illus-senior { display: flex; gap: 3px; flex-shrink: 0; font-size: 28px; }
.bc-sp { animation: peopleBob 4s ease-in-out infinite; animation-delay: .5s; }
@keyframes peopleBob { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-5px)} }

.bc-body { display: flex; flex-direction: column; gap: 2px; }
.bc-name { font-size: 15px; font-weight: 800; color: #2d2438; }
.bc-desc { font-size: 12px; color: #9088a0; }

.bc-go {
  position: absolute; bottom: 18px; right: 20px;
  width: 30px; height: 30px; border-radius: 50%;
  background: #f5f0ff; display: flex; align-items: center; justify-content: center;
  font-size: 15px; color: #7c5cfc; transition: all .3s;
}
.b-card:hover .bc-go { background: #7c5cfc; color: #fff; transform: translateX(3px); }

/* ===== Hero pills ===== */
.hero-pills {
  display: flex; gap: 6px; margin-top: 10px; flex-wrap: wrap;
}

.pill {
  display: inline-flex; align-items: center; gap: 3px;
  padding: 4px 12px; border-radius: 999px;
  font-size: 11px; font-weight: 700;
  color: #5b4a8a; background: #f0ecfc;
}

.pill-fire { background: #fff7ed; color: #c2410c; }
.pill-star { background: #fffbeb; color: #a16207; }
.pill-xp  { background: #ecfdf3; color: #166534; }

/* ===== 今日任务卡片 ===== */
.hero-tasks {
  margin: 0 40px 0; padding: 18px 0 22px;
  border-top: 1px solid rgba(200,185,240,.22);
}

.ht-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px;
}

.ht-title { font-size: 13px; font-weight: 700; color: #5b4a8a; }

.ht-head-right { display: flex; align-items: center; gap: 12px; }

.ht-add-btn {
  padding: 4px 12px; border-radius: 999px; border: 1.5px solid rgba(180,165,225,.4);
  background: rgba(248,246,255,.7); color: #7c5cfc;
  font-size: 11px; font-weight: 700; cursor: pointer;
  transition: all .2s cubic-bezier(.4,0,.2,1);
}

.ht-add-btn:hover { transform: translateY(-1px); background: #ece2fc; border-color: #c4b5fd; }

.ht-count { font-size: 11px; font-weight: 600; color: #9088a0; }

/* 添加输入行 */
.ht-add-row {
  display: flex; gap: 8px; align-items: center;
  margin-bottom: 10px; padding: 10px 14px;
  background: rgba(248,246,255,.7); border-radius: 12px;
  border: 1px solid rgba(200,185,240,.25);
}

.ht-add-input {
  flex: 1; padding: 8px 12px; border: 1.5px solid rgba(200,185,240,.4);
  border-radius: 10px; font-size: 12px; outline: none;
  background: rgba(255,255,255,.7); color: #2d2438;
  transition: all .2s;
}

.ht-add-input:focus { border-color: #7c5cfc; box-shadow: 0 0 0 3px rgba(124,92,252,.08); }
.ht-add-input::placeholder { color: #b0a8c0; }

.ht-add-confirm {
  padding: 7px 16px; border-radius: 10px; border: none;
  background: #7c5cfc; color: #fff; font-size: 12px; font-weight: 700;
  cursor: pointer; white-space: nowrap; transition: all .2s;
}

.ht-add-confirm:hover { background: #6a4aee; }

.ht-add-cancel {
  padding: 7px 14px; border-radius: 10px; border: 1.5px solid rgba(200,185,240,.4);
  background: #fff; color: #9088a0; font-size: 12px; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: all .2s;
}

.ht-add-cancel:hover { background: #f5f0ff; }

.ht-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
}

.ht-card {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px; border-radius: 12px;
  cursor: pointer; transition: all .25s cubic-bezier(.4,0,.2,1);
  background: rgba(248,246,255,.7); border: 1.5px solid rgba(220,210,245,.35);
  font-size: 13px; font-weight: 500; color: #5b4a8a;
}

.ht-card:hover { transform: translateY(-1px); border-color: #c4b5fd; box-shadow: 0 4px 14px rgba(124,92,252,.06); }

.ht-card.done {
  background: linear-gradient(135deg, #f0fdf4, #ecfdf3);
  border-color: #bbf7d0; color: #166534;
  box-shadow: 0 0 0 1px rgba(34,197,94,.08);
}

.ht-card-icon { font-size: 16px; flex-shrink: 0; transition: transform .3s; }
.ht-card.done .ht-card-icon { transform: scale(1.2); color: #22c55e; }
.ht-card-text { flex: 1; }
.ht-card.done .ht-card-text { text-decoration: line-through; opacity: .7; }

.ht-del {
  width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; color: #c0b0d8;
  opacity: 0; transition: all .2s; cursor: pointer;
}

.ht-card:hover .ht-del { opacity: 1; }
.ht-del:hover { background: #fee2e2; color: #ef4444; }

.bottom-row {
  display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 18px;
}

/* ---- 书桌卡片 ---- */
.desk-card {
  background: linear-gradient(170deg, #fefdfb 0%, #faf7f2 40%, #f6f0f8 100%);
  border-radius: 20px; padding: 24px 28px;
  border: 1px solid rgba(210,195,180,.3);
  box-shadow: 0 2px 20px rgba(120,100,80,.04);
}

.desk-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.desk-head-title { font-size: 15px; font-weight: 800; color: #3d2e1a; }
.desk-head-chapter { font-size: 11px; font-weight: 600; color: #a09080; background: #f5f0eb; padding: 4px 12px; border-radius: 8px; }

.desk-body { display: flex; gap: 24px; align-items: center; }

/* 3D 书 */
.desk-book-3d { position: relative; flex-shrink: 0; transition: transform .5s cubic-bezier(.34,1.56,.64,1); }
.desk-book-3d:hover { transform: perspective(600px) rotateY(-10deg) translateX(-4px); }

.db3-cover {
  width: 100px; height: 138px; border-radius: 3px 12px 12px 3px;
  overflow: hidden; position: relative; z-index: 2;
  box-shadow: 5px 6px 18px rgba(0,0,0,.1), 0 0 0 1px rgba(0,0,0,.03);
}

.db3-cover img { width: 100%; height: 100%; object-fit: cover; }

.db3-placeholder {
  width: 100%; height: 100%; background: linear-gradient(140deg, #eef0ff, #ddd6fe);
  display: flex; align-items: center; justify-content: center; font-size: 40px;
}

.db3-shine {
  position: absolute; top: 0; left: 0; right: 35%; bottom: 0;
  background: linear-gradient(90deg, rgba(255,255,255,.18), transparent); pointer-events: none;
}

.db3-spine {
  position: absolute; left: -7px; top: 0; bottom: 0; width: 7px; z-index: 1;
  background: linear-gradient(90deg, rgba(0,0,0,.1), rgba(0,0,0,.02));
  border-radius: 2px 0 0 2px;
}

.db3-pages {
  position: absolute; right: -5px; top: 3px; bottom: 3px; width: 10px; z-index: 0;
  background: repeating-linear-gradient(0deg, #f5f0e8 0px, #e8e0d0 1px, #f5f0e8 2px, #e8e0d0 3px);
  border-radius: 0 5px 5px 0; opacity: .55;
}

/* 书籍信息 */
.desk-info { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }

.di-name { font-size: 17px; font-weight: 800; color: #2d1f0e; margin: 0; }
.di-author { font-size: 12px; color: #908070; margin: 0; }

.di-meta { display: flex; gap: 14px; font-size: 11px; color: #a09080; }

.di-progress { display: flex; align-items: center; gap: 10px; margin-top: 4px; }

.di-bar { flex: 1; height: 7px; background: #efe8e0; border-radius: 10px; overflow: hidden; }

.di-fill {
  height: 100%; border-radius: 10px;
  background: linear-gradient(90deg, #a78bfa, #7c5cfc);
  transition: width 1s cubic-bezier(.4,0,.2,1);
}

.di-pct { font-size: 13px; font-weight: 800; color: #7c5cfc; }

.di-btn-main {
  padding: 9px 18px; border-radius: 12px; font-size: 13px; font-weight: 700;
  cursor: pointer; transition: all .3s cubic-bezier(.4,0,.2,1); border: none;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa); color: #fff;
  box-shadow: 0 4px 14px rgba(124,92,252,.22);
  align-self: flex-start; margin-top: 6px;
}

.di-btn-main:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(124,92,252,.35); }


/* 空状态 */
.desk-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 44px 28px; gap: 8px; }

.de-illustration { font-size: 48px; opacity: .7; }
.de-title { font-size: 15px; font-weight: 700; color: #5b4a6a; margin: 0; }
.de-sub { font-size: 12px; color: #9088a0; margin: 0; }

/* ---- 学习轨迹时间轴 ---- */
.tl-card {
  background: linear-gradient(160deg, #faf7ff 0%, #f3f0fc 40%, #eef5ff 100%);
  border-radius: 20px; padding: 22px 24px;
  border: 1px solid rgba(200,190,230,.3);
  box-shadow: 0 2px 20px rgba(120,100,180,.04);
  display: flex; flex-direction: column;
}

.tl-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; font-size: 14px; font-weight: 700; color: #5b4a8a;
}

.tl-week { font-size: 11px; color: #9088a0; background: rgba(255,255,255,.5); padding: 3px 10px; border-radius: 8px; font-weight: 500; }

.tl-track { position: relative; flex: 1; display: flex; align-items: center; padding: 0 4px; }

/* 时间轴横线 */
.tl-line {
  position: absolute; top: 16px; left: 0; right: 0; height: 2px;
  background: #e8e2f4; border-radius: 2px;
}

.tl-line-done {
  position: absolute; top: 16px; left: 0; height: 2px;
  background: linear-gradient(90deg, #a78bfa, #7c5cfc); border-radius: 2px;
  transition: width .6s ease;
}

/* 节点列 */
.tl-nodes {
  display: flex; justify-content: space-between; width: 100%; position: relative; z-index: 1;
}

.tl-node-col {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  flex: 1; min-width: 0;
}

.tl-dot {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff;
  background: #f0ecf8; border: 2px solid #e0d8f0;
  transition: all .3s;
  flex-shrink: 0;
}

.tl-dot.done { background: linear-gradient(135deg, #a78bfa, #7c5cfc); border-color: #7c5cfc; box-shadow: 0 2px 10px rgba(124,92,252,.18); }
.tl-dot.current { border-color: #7c5cfc; background: #fff; box-shadow: 0 0 0 5px rgba(124,92,252,.1); }

.tl-dot-curr {
  width: 12px; height: 12px; border-radius: 50%; background: #7c5cfc;
  animation: tlBreath 1.6s ease-in-out infinite;
}

@keyframes tlBreath {
  0%,100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: .5; }
}

.tl-date { font-size: 11px; color: #9088a0; font-weight: 500; }
.tl-topic { font-size: 10px; color: #b0a8c0; white-space: nowrap; }

@media (max-width: 1000px) {
  .bento { grid-template-columns: 1fr 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
}

@media (max-width: 700px) {
  .page {
    padding: 16px 12px 40px;
  }

  .hero-inner {
    grid-template-columns: 1fr; text-align: center; padding: 24px 20px 18px;
  }
  .hero-goal { max-width: 100%; }
  .hero-pills { justify-content: center; }
  .ht-grid { grid-template-columns: 1fr; }
  .hero-tasks { margin: 0 20px 0; }
  .buddy-scene { width: 240px; }
  .bento { grid-template-columns: 1fr; }
}
</style>