<template>
  <div class="qa-page">
    <div class="qp-bg"></div>

    <!-- ===== 顶部栏 ===== -->
    <header class="qa-header">
      <button class="qah-back" @click="goHome">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5m7-7-7 7 7 7"/></svg>
      </button>
      <div class="qah-info">
        <span class="qahi-icon">{{ currentMode === 'exam' ? '🎓' : '📚' }}</span>
        <div class="qahi-text">
          <span class="qahi-title">{{ currentMode === 'exam' ? '考研考证' : '教材学习' }}</span>
          <span class="qahi-sub">{{ currentMode === 'exam' ? '按考点真题应试回答' : '按教材知识体系回答' }}</span>
        </div>
        <span class="qahi-badge" v-if="selectedResource">学习中</span>
      </div>
      <div class="qah-stats">
        <span>🔥 {{ streak }} 天</span>
        <span>📖 {{ todayMinutes }}min</span>
        <span>⭐ {{ masteredCount }} 知识点</span>
      </div>
    </header>

    <div class="qa-body">
      <!-- 左侧栏 -->
      <aside class="qa-sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="qas-scroll">
          <!-- 模式切换 -->
          <div class="qas-mode">
            <button class="qasm-btn" :class="{ active: currentMode === 'textbook' }" @click="switchMode('textbook')">
              <span class="qasm-icon">📚</span>
              <div class="qasm-text"><span class="qasm-label">教材学习</span><span class="qasm-desc">按课本知识体系</span></div>
            </button>
            <button class="qasm-btn" :class="{ active: currentMode === 'exam' }" @click="switchMode('exam')">
              <span class="qasm-icon">🎓</span>
              <div class="qasm-text"><span class="qasm-label">考研考证</span><span class="qasm-desc">按考点真题应试</span></div>
            </button>
          </div>

          <!-- 当前资源 -->
          <div class="qas-resource" v-if="selectedResource">
            <div class="qasr-head">当前学习</div>
            <div class="qasr-body">
              <span class="qasr-emoji">{{ currentMode === 'exam' ? '🎓' : '📚' }}</span>
              <div class="qasr-info">
                <div class="qasr-name">{{ selectedResource.name }}</div>
                <div class="qasr-extra" v-if="selectedResource.author">{{ selectedResource.author }}</div>
                <div class="qasr-extra" v-if="currentMode === 'exam'">备考中</div>
              </div>
            </div>
          </div>

          <!-- 教材模式 -->
          <template v-if="currentMode === 'textbook'">
            <!-- 教材上下文栏 -->
            <div v-if="selectedBook" class="qas-ctx">
              <div class="qas-ctx-left">
                <span class="qas-ctx-icon">📘</span>
                <div class="qas-ctx-info">
                  <span class="qas-ctx-name">{{ selectedBook.name }}</span>
                  <span class="qas-ctx-pub">{{ selectedBook.author || '未知作者' }}{{ selectedBook.publisher ? ' · ' + selectedBook.publisher : '' }}</span>
                </div>
              </div>
              <div class="qas-ctx-actions">
                <button class="qas-ctx-switch" @click="bookPanelOpen = true">切换</button>
                <button class="qas-ctx-clear" @click="clearBookContext">清除</button>
              </div>
            </div>

            <!-- 搜索框 -->
            <div class="qas-search-wrap">
              <div class="qas-search" @click="bookPanelOpen = true">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
                <span class="qas-search-placeholder">{{ selectedBook ? '切换教材...' : '搜索教材 / 专业 / 作者 / 考研方向...' }}</span>
              </div>
            </div>

            <!-- 章节列表 -->
            <div v-if="selectedBook && chapters.length" class="qas-chapters">
              <div class="qas-label">章节列表</div>
              <div v-for="ch in chapters" :key="ch.id">
                <div class="qasch-head" @click="toggleChapter(ch.id)">
                  <svg class="qasch-arrow" :class="{open:expandedChapters.includes(ch.id)}" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m9 18 6-6-6-6"/></svg>
                  <span>{{ ch.title }}</span>
                  <span class="qasch-num" v-if="ch.children?.length">{{ ch.children.length }} 节</span>
                </div>
                <div v-if="expandedChapters.includes(ch.id) && ch.children" class="qasch-sub">
                  <div v-for="sub in ch.children" :key="sub.id" class="qasch-sub-item" @click="askChapterQuestion(ch, sub)">{{ sub.title }}</div>
                </div>
              </div>
            </div>

            <!-- 相关教材推荐 -->
            <div v-if="selectedBook && relatedBooks.length" class="qas-related">
              <div class="qas-label">📖 相关教材</div>
              <div v-for="b in relatedBooks.slice(0, 4)" :key="'rel'+b.id" class="qasb-card" @click="selectBook(b)">
                <div class="qasb-cover">{{ getBookEmoji(b.name) }}</div>
                <div class="qasb-info">
                  <div class="qasb-name">{{ b.name }}</div>
                  <div class="qasb-author">{{ b.author || '未知作者' }}</div>
                </div>
              </div>
            </div>

            <!-- 浮层 -->
            <div v-if="bookPanelOpen" class="qas-panel-overlay" @click="bookPanelOpen = false">
              <div class="qas-panel" @click.stop>
                <div class="qas-panel-bar">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
                  <input v-model="bookSearchText" placeholder="搜索教材 / 专业 / 作者 / 考研方向..." class="qas-panel-input" @keydown.esc="bookPanelOpen = false" />
                  <span class="qas-panel-esc">ESC</span>
                </div>
                <div class="qas-panel-body">
                  <template v-if="!bookSearchText.trim()">
                    <div class="qas-panel-sec" v-if="recentBooks.length">
                      <div class="qas-panel-sec-head">📚 书架教材</div>
                      <div v-for="b in recentBooks.slice(0, 4)" :key="b.id" class="qas-panel-item" :class="{ on: selectedBook?.id === b.id }" @click="selectBook(b); bookPanelOpen = false">
                        <span class="qas-panel-item-cover">{{ getBookEmoji(b.name) }}</span>
                        <div class="qas-panel-item-info">
                          <span class="qas-panel-item-name">{{ b.name }}</span>
                          <span class="qas-panel-item-meta">{{ b.author || '未知作者' }}</span>
                        </div>
                        <span v-if="selectedBook?.id === b.id" class="qas-panel-item-ctx">上下文</span>
                      </div>
                    </div>
                    <div class="qas-panel-sec" v-if="hotDirections.length">
                      <div class="qas-panel-sec-head">🔥 热门方向</div>
                      <div class="qas-panel-chips">
                        <button v-for="d in hotDirections" :key="d" class="qas-panel-chip" @click="bookSearchText = d">{{ d }}</button>
                      </div>
                    </div>
                    <div class="qas-panel-sec" v-if="aiBooks.length">
                      <div class="qas-panel-sec-head">✨ AI 推荐</div>
                      <div v-for="b in aiBooks.slice(0, 3)" :key="'a'+b.id" class="qas-panel-item" @click="selectBook(b); bookPanelOpen = false">
                        <span class="qas-panel-item-cover">{{ getBookEmoji(b.name) }}</span>
                        <div class="qas-panel-item-info">
                          <span class="qas-panel-item-name">{{ b.name }}</span>
                          <span class="qas-panel-item-meta">{{ b.author || '未知作者' }} · {{ b.direction || '通用' }}</span>
                        </div>
                        <span class="qas-panel-item-pct">{{ b.matchRate || 90 }}%</span>
                      </div>
                    </div>
                  </template>
                  <template v-else-if="panelSearchResults.length">
                    <div class="qas-panel-sec-head">搜索结果 <span class="qas-panel-count">{{ panelSearchResults.length }}</span></div>
                    <div v-for="(b, i) in panelSearchResults" :key="'r'+i" class="qas-panel-item" :style="{ animationDelay: i * 0.04 + 's' }" :class="{ on: selectedBook?.id === b.id }" @click="selectBook(b); bookPanelOpen = false">
                      <span class="qas-panel-item-cover">{{ getBookEmoji(b.name) }}</span>
                      <div class="qas-panel-item-info">
                        <span class="qas-panel-item-name">{{ b.name }}</span>
                        <span class="qas-panel-item-meta">{{ b.author || '未知作者' }} · {{ b.publisher || '' }}</span>
                      </div>
                      <span v-if="selectedBook?.id === b.id" class="qas-panel-item-ctx">上下文</span>
                      <span v-else class="qas-panel-item-pct">{{ b.matchRate || 80 }}%</span>
                    </div>
                  </template>
                  <div v-else class="qas-panel-empty">📡 未找到相关教材</div>
                </div>
              </div>
            </div>
          </template>

          <!-- 考研模式 -->
          <template v-if="currentMode === 'exam'">
            <div class="qas-search">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
              <input v-model="subjectSearchQuery" placeholder="搜索科目..." @focus="showSubjectDropdown = true" @blur="showSubjectDropdown = false" />
            </div>
            <div class="qas-drop" v-if="showSubjectDropdown && filteredSubjects.length">
              <div v-for="s in filteredSubjects" :key="s.id" class="qasd-opt" :class="{active:selectedExamSubject===s.id}" @mousedown.prevent="selectSubject(s)">{{ s.name }}</div>
            </div>
            <div class="qas-label" v-if="keyPoints.length">🔥 高频考点</div>
            <div class="qas-kp" v-if="keyPoints.length">
              <div v-for="(kp,i) in keyPoints.slice(0,6)" :key="i" class="qask-item" :class="{active:selectedKeyPoint===kp}" @click="selectKeyPoint(kp)">
                <span class="qask-rank">{{ i+1 }}</span><span>{{ kp.name }}</span>
              </div>
            </div>
            <div class="qas-label" v-if="selectedPapers.length">📝 真题推荐</div>
            <div class="qas-papers" v-if="selectedPapers.length">
              <div v-for="(p,i) in selectedPapers.slice(0,4)" :key="i" class="qasp-item">
                <span class="qasp-year">{{ p.year }}</span><span>{{ p.subject }}</span>
              </div>
            </div>
            <div class="qas-predict" @click="askAiPredict">
              <span class="qaspd-icon">🎯</span>
              <div class="qaspd-info"><div>AI 智能押题</div><div>基于历年真题趋势预测</div></div>
              <span>→</span>
            </div>
          </template>

          <!-- ====== 历史对话 ====== -->
          <div class="qas-history" v-if="sessions.length > 0">
            <div class="qash-head">
              <span class="qas-label">📋 历史对话</span>
              <button class="qash-new-btn" @click="startNewSession" title="新对话">+ 新对话</button>
            </div>
            <div class="qash-list">
              <div
                v-for="s in sessions"
                :key="s.id"
                class="qash-item"
                :class="{ active: activeSessionId === s.id }"
                @click="loadSession(s)"
              >
                <div class="qashi-icon">{{ s.mode === 'exam' ? '🎓' : '📚' }}</div>
                <div class="qashi-info">
                  <div class="qashi-title">{{ s.title }}</div>
                  <div class="qashi-meta">{{ s.msgCount }} 条对话 · {{ formatSessionTime(s.createdAt) }}</div>
                </div>
                <button class="qashi-del" @click.stop="deleteSession(s.id)" title="删除">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 新对话按钮（无历史时显示） -->
          <button v-if="messages.length > 0 && sessions.length === 0" class="qas-new-session-btn" @click="startNewSession">
            + 新对话
          </button>
        </div>

        <button class="qas-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg :class="{ rotated: !sidebarCollapsed }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m9 18 6-6-6-6"/></svg>
        </button>
      </aside>

      <!-- ===== 主聊天区 ===== -->
      <main class="qa-main">
        <div class="qam-messages" ref="chatBody">
          <div v-if="messages.length === 0" class="qam-empty">
            <div class="qame-hero">
              <div class="qame-avatar"><span>🤖</span></div>
              <h2>{{ welcomeTitle }}</h2>
              <p>{{ welcomeSub }}</p>
            </div>
            <div class="qame-label">✨ AI 为你推荐</div>
            <div class="qame-cards">
              <div v-for="rec in modeRecommendations" :key="rec.text" class="qamec-card" @click="sendQuestion(rec.text)">
                <span class="qamec-icon">{{ rec.icon }}</span>
                <span class="qamec-text">{{ rec.text }}</span>
                <span class="qamec-tag">{{ rec.tag }}</span>
              </div>
            </div>
            <div class="qame-label">💡 试试这些问题</div>
            <div class="qame-chips">
              <span v-for="q in modeQuickQuestions" :key="q" class="qamec-chip" @click="sendQuestion(q)">{{ q }}</span>
            </div>
          </div>

          <div v-for="msg in messages" :key="msg.id" class="qam-row" :class="msg.role">
            <div v-if="msg.role === 'user'" class="qam-bubble user">{{ msg.question }}</div>
            <template v-else>
              <div class="qam-avatar">AI</div>
              <div class="qam-ai-wrap">
                <div class="qam-bubble ai">
                  <div v-if="msg.status === 'loading'" class="qam-loading">
                    <span></span><span></span><span></span>
                    <em>AI 正在思考...</em>
                  </div>
                  <div v-else-if="msg.status === 'error'" class="qam-err">
                    <div class="qame-head">
                      <span class="qameh-icon">⚠️</span>
                      <span class="qameh-title">服务暂时异常</span>
                    </div>
                    <p class="qame-desc">请稍后重试，或重新编辑问题再发送</p>
                    <div class="qame-btns">
                      <button class="qameb-retry" @click="retry(msg)" :disabled="isSending">重试</button>
                      <button class="qameb-reask" @click="reAsk(msg.question)">重新提问</button>
                    </div>
                  </div>
                  <div v-else-if="msg.status === 'typing'" class="qam-typing">
                    <span v-html="msg.displayContent"></span><span class="qam-cursor">|</span>
                  </div>
                  <div v-else-if="msg.status === 'success'">
                    <div v-if="msg.displayContent" v-html="msg.displayContent" class="qam-html"></div>
                    <div v-else class="qam-empty-content">暂无回答内容，请重试</div>
                  </div>
                </div>
                <div class="qam-actions" v-if="msg.status === 'success'">
                  <button @click="regenerate(msg)" :disabled="isSending">🔄 重新生成</button>
                  <button @click="copyContent(msg)">📋 复制</button>
                </div>
              </div>
            </template>
          </div>
        </div>

        <div v-if="lastSuccessMsg" class="qam-followup">
          <span>继续问：</span>
          <span v-for="c in currentQuickChips" :key="c" @click="sendQuestion(c)">{{ c }}</span>
        </div>

        <div class="qam-input">
          <div v-if="uploadedImage" class="qami-preview">
            <img :src="uploadedImage" />
            <button @click="deleteImage">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div class="qami-bar">
            <button class="qamib-img" :class="{active:questionType==='image'}" @click="toggleImageMode" title="图片提问">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
            </button>
            <span v-if="selectedBook && currentMode === 'textbook'" class="qami-ctx-tag" @click="bookPanelOpen = true" title="点击切换教材">📘 {{ selectedBook.name }}</span>
            <input ref="inputRef" v-model="inputText" :placeholder="inputPlaceholder" class="qamib-input" @keydown.enter.prevent="sendQuestion()" />
            <input type="file" ref="fileInput" style="display:none" accept="image/*" @change="handleFileChange" />
            <button class="qamib-send" :class="{ready:canSend, loading:isSending}" @click="sendQuestion()" :disabled="!canSend || isSending">
              <svg v-if="!isSending" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg>
              <span v-else class="qamib-spin"></span>
            </button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'
import { streamingApiService } from '../../api/streaming.js'
import { apiService } from '../../api/api.js'
import { processAiResponse } from '../../utils/markdownUtils'

const router = useRouter(); const route = useRoute()

/* ========== 核心状态 ========== */
const currentMode = ref('textbook')
const messages = ref([])
const inputText = ref('')
const isSending = ref(false)
const sidebarCollapsed = ref(false)
const selectedResource = ref(null)

/* ========== 会话历史 ========== */
const sessions = ref([])
const activeSessionId = ref(null)

const saveCurrentSession = () => {
  const msgs = messages.value
  if (msgs.length === 0) return
  // 找第一条用户消息作为标题
  const firstUser = msgs.find(m => m.role === 'user')
  const title = firstUser ? (firstUser.question.slice(0, 40) + (firstUser.question.length > 40 ? '...' : '')) : '对话记录'
  const session = {
    id: Date.now(),
    title,
    mode: currentMode.value,
    resource: selectedResource.value ? { ...selectedResource.value } : null,
    messages: msgs.map(m => ({ ...m })), // 深拷贝
    msgCount: msgs.length,
    createdAt: new Date().toISOString()
  }
  sessions.value.unshift(session)
  activeSessionId.value = session.id

  // 最多保留 20 条
  if (sessions.value.length > 20) sessions.value = sessions.value.slice(0, 20)
}

const loadSession = (s) => {
  // 保存当前对话
  if (messages.value.length > 0 && activeSessionId.value !== s.id) {
    saveCurrentSilent()
  }
  activeSessionId.value = s.id
  currentMode.value = s.mode
  messages.value = s.messages.map(m => ({ ...m }))
  inputText.value = ''
  isSending.value = false
  selectedResource.value = s.resource
  if (sidebarCollapsed.value) sidebarCollapsed.value = false
}

const saveCurrentSilent = () => {
  const msgs = messages.value
  if (msgs.length === 0) return
  // 更新已有 session 还是新建
  const existing = sessions.value.find(s => s.id === activeSessionId.value)
  if (existing) {
    existing.messages = msgs.map(m => ({ ...m }))
    existing.msgCount = msgs.length
    return
  }
  // 新建
  const firstUser = msgs.find(m => m.role === 'user')
  const title = firstUser ? (firstUser.question.slice(0, 40) + (firstUser.question.length > 40 ? '...' : '')) : '对话记录'
  const session = {
    id: Date.now(),
    title,
    mode: currentMode.value,
    resource: selectedResource.value ? { ...selectedResource.value } : null,
    messages: msgs.map(m => ({ ...m })),
    msgCount: msgs.length,
    createdAt: new Date().toISOString()
  }
  sessions.value.unshift(session)
  activeSessionId.value = session.id
  if (sessions.value.length > 20) sessions.value = sessions.value.slice(0, 20)
}

const startNewSession = () => {
  saveCurrentSilent()
  messages.value = []
  inputText.value = ''
  isSending.value = false
  activeSessionId.value = null
  uploadedImage.value = null
  questionType.value = 'text'
}

const deleteSession = (id) => {
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (activeSessionId.value === id) {
    activeSessionId.value = null
    messages.value = []
    inputText.value = ''
  }
}

const formatSessionTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return `${Math.floor(diff/60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff/3600000)} 小时前`
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2,'0')}`
}

/* ========== switchMode — 保存当前对话后切换 ========== */
const switchMode = (mode) => {
  if (currentMode.value === mode) return
  saveCurrentSilent()
  closeSSE(); stopTyping()
  currentMode.value = mode
  messages.value = []
  inputText.value = ''
  isSending.value = false
  uploadedImage.value = null
  questionType.value = 'text'
  selectedBook.value = null; chapters.value = []; expandedChapters.value = []
  selectedExamSubject.value = ''; subjectSearchQuery.value = ''
  keyPoints.value = []; selectedKeyPoint.value = null; selectedPapers.value = []
  selectedResource.value = null
  activeSessionId.value = null
  if (sidebarCollapsed.value) sidebarCollapsed.value = false
}

/* ========== 选择教材/科目 — 保存后切换 ========== */
const selectBook = (book) => {
  if (selectedBook.value?.id === book.id) return
  saveCurrentSilent()
  closeSSE(); stopTyping()
  selectedBook.value = book
  selectedResource.value = { name: book.name, author: book.author }
  expandedChapters.value = []
  messages.value = []
  inputText.value = ''
  isSending.value = false
  uploadedImage.value = null
  questionType.value = 'text'
  activeSessionId.value = null
  if (book.id) fetchChapters(book.id); else chapters.value = []
}

const selectSubject = async (s) => {
  if (selectedExamSubject.value === s.id) return
  saveCurrentSilent()
  closeSSE(); stopTyping()
  selectedExamSubject.value = s.id; subjectSearchQuery.value = ''; showSubjectDropdown.value = false
  selectedResource.value = { name: s.name }
  selectedKeyPoint.value = null
  messages.value = []
  inputText.value = ''
  isSending.value = false
  uploadedImage.value = null
  questionType.value = 'text'
  activeSessionId.value = null
  await fetchKeyPoints(s.id); await fetchExamPapers(s.id)
}

const lastSuccessMsg = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'assistant' && messages.value[i].status === 'success') return messages.value[i]
  }
  return null
})

/* ========== 展示数据 ========== */
const streak = ref(7); const todayMinutes = ref(30); const masteredCount = ref(5)
const welcomeTitle = computed(() => currentMode.value === 'exam' ? '今天想备考什么？' : '今天想学什么？')
const welcomeSub = computed(() => currentMode.value === 'exam' ? '选择科目后，我会按考点和真题思路回答' : '选择教材后，我会按教材知识体系回答')
const inputPlaceholder = computed(() => {
  if (currentMode.value === 'exam') return '输入问题，AI 按考点和真题思路回答...'
  if (selectedBook.value) return `输入问题，AI 将基于《${selectedBook.value.name}》知识体系回答...`
  return '输入问题，AI 将自由回答...'
})
const modeRecommendations = computed(() => {
  if (currentMode.value === 'exam') {
    return [{ icon:'🎯',text:'这个科目的高频考点有哪些？',tag:'考点分析'},{ icon:'📝',text:'给我一道典型真题',tag:'真题演练'},{ icon:'💡',text:'这类题怎么快速拿分？',tag:'应试技巧'},{ icon:'⚠️',text:'帮我整理易错点',tag:'易错归纳' }]
  }
  if (selectedBook.value) {
    return [{ icon:'📖',text:`《${selectedBook.value.name}》的核心知识点有哪些？`,tag:'知识梳理'},{ icon:'📝',text:`请按${selectedBook.value.name}体系总结本章重点`,tag:'章节总结'},{ icon:'🔍',text:'这个概念在书中如何应用？',tag:'概念应用'},{ icon:'✏️',text:'给我出几道练习题测试一下',tag:'练习巩固' }]
  }
  return [{ icon:'📖',text:'选择教材后，AI 将按教材体系回答',tag:'提示'},{ icon:'📝',text:'请先在左侧选择一本教材',tag:'开始学习'},{ icon:'🔍',text:'也可以直接提问，AI 自由回答',tag:'自由模式'},{ icon:'✏️',text:'试试问：导数是什么？',tag:'试试看' }]
})
const modeQuickQuestions = computed(() => {
  if (currentMode.value === 'exam') return ['高频考点有哪些？','讲解一道真题','推荐备考计划','这个知识点容易出错的地方']
  if (selectedBook.value) return [`${selectedBook.value.name}的核心知识点有哪些？`,'请帮我总结本章重点','这个概念如何应用到实际中？','给我出几道练习题']
  return ['导数是什么？','栈和队列有什么区别？','什么是微积分？','给我讲一下线性代数基础']
})
const quickChips = computed(() => selectedBook.value ? ['帮我总结本章要点','解释一下这个概念','出几道练习题'] : ['选择教材后提问','或直接自由提问'])
const examQuickChips = ['解析这道题的考点','推荐备考计划','对比这几个概念的异同']
const currentQuickChips = computed(() => currentMode.value === 'exam' ? examQuickChips : quickChips.value)

/* ========== 教材 ========== */
const searchQuery = ref(''); const selectedBook = ref(null); const books = ref([]); const chapters = ref([]); const expandedChapters = ref([])
const loading = ref({ books: false, chapters: false }); const error = ref({ books: '', chapters: '' })
const getBookEmoji = (n) => { const m={'高':'📐','数':'🔢','线':'📏','概':'🎲','物':'⚡','化':'🧪','英':'🌍','计':'💻','大':'📘'}; return m[n?.charAt(0)||'']||'📘' }
const filteredBooks = computed(() => { const q=searchQuery.value.trim().toLowerCase(); return q ? books.value.filter(b=>b.name?.toLowerCase().includes(q)) : books.value })
const MOCK_BOOKS = [
  { id: 1, name: '高等数学（同济第七版）', author: '同济大学数学系', publisher: '高等教育出版社', direction: '考研数学一' },
  { id: 2, name: '线性代数（同济第六版）', author: '同济大学数学系', publisher: '高等教育出版社', direction: '考研数学一' },
  { id: 3, name: '概率论与数理统计', author: '盛骤', publisher: '高等教育出版社', direction: '考研数学一' },
  { id: 4, name: '数据结构（C语言版）', author: '严蔚敏', publisher: '清华大学出版社', direction: '计算机基础' },
  { id: 5, name: '计算机网络', author: '谢希仁', publisher: '电子工业出版社', direction: '计算机基础' },
  { id: 6, name: '大学物理（上）', author: '张三慧', publisher: '清华大学出版社', direction: '理工科通识' },
  { id: 7, name: '新概念英语3', author: '亚历山大', publisher: '外语教学与研究出版社', direction: '考研英语' },
  { id: 8, name: '数学分析', author: '华东师范大学数学系', publisher: '高等教育出版社', direction: '大学必修' },
]

const fetchBooks = async () => {
  loading.value.books = true
  try {
    const t = useAuthStore().token
    const r = await fetch('/api/qa/books', { headers: { 'Content-Type': 'application/json', ...(t ? { Authorization: `Bearer ${t}` } : {}) } })
    if (r.ok) { const d = await r.json(); if (d && d.length) { books.value = d; loading.value.books = false; return } }
  } catch (e) {}
  books.value = MOCK_BOOKS
  loading.value.books = false
}
const fetchChapters = async (id) => {
  loading.value.chapters = true
  try { const r = await apiService.qa.getChapters(id); if (r && r.length) { chapters.value = r; loading.value.chapters = false; return } } catch (e) {}
  chapters.value = [
    { id: 1, title: '第一章', children: [{ id: 11, title: '基本概念' }, { id: 12, title: '核心定理' }, { id: 13, title: '典型例题' }] },
    { id: 2, title: '第二章', children: [{ id: 21, title: '基础知识' }, { id: 22, title: '进阶内容' }] },
    { id: 3, title: '第三章', children: [{ id: 31, title: '应用方法' }, { id: 32, title: '综合练习' }] },
  ]
  loading.value.chapters = false
}
const toggleChapter = (id) => { const i=expandedChapters.value.indexOf(id); i>-1?expandedChapters.value.splice(i,1):expandedChapters.value.push(id) }
const askChapterQuestion = (ch,sub) => sendQuestion(`请讲解「${ch.title} > ${sub.title}」的内容`)

/* ========== 教材选择浮层 ========== */
const bookPanelOpen = ref(false)
const bookSearchText = ref('')
const clearBookContext = () => { selectedBook.value = null; chapters.value = []; expandedChapters.value = []; selectedResource.value = null }

const recentBooks = computed(() => books.value.slice(0, 6))
const hotDirections = ['计算机考研', '人工智能', '数学分析', '英语四级', '高等数学', '数据科学']

const aiBooks = computed(() => {
  const pool = [...books.value]
  if (pool.length <= 3) return pool.map(b => ({ ...b, matchRate: 85 + Math.floor(Math.random() * 13), direction: '通用' }))
  return pool.sort(() => Math.random() - 0.5).slice(0, 5).map(b => ({ ...b, matchRate: 85 + Math.floor(Math.random() * 13), direction: ['考研数学一', '大学必修', '计算机基础', '理工科通识'][Math.floor(Math.random() * 4)] }))
})

const panelSearchResults = computed(() => {
  const q = bookSearchText.value.trim().toLowerCase()
  if (!q) return []
  return books.value.filter(b => b.name?.toLowerCase().includes(q) || (b.author && b.author.toLowerCase().includes(q))).slice(0, 8).map(b => ({ ...b, matchRate: 80 + Math.floor(Math.random() * 16) }))
})

const relatedBooks = computed(() => {
  if (!selectedBook.value) return []
  return books.value.filter(b => b.id !== selectedBook.value.id).slice(0, 5)
})

/* ========== sendQuestion ========== */
let sseConnection = null; let typingTimer = null
const inputRef = ref(null)
const canSend = computed(() => (questionType.value==='image'&&uploadedImage.value)?true:inputText.value.trim().length>0)

const buildContext = () => {
  if (currentMode.value==='textbook'&&selectedBook.value) return selectedBook.value.name||''
  if (currentMode.value==='exam') return getSelectedSubjectName()!=='选择科目'?getSelectedSubjectName():''
  return ''
}

const sendQuestion = async (text) => {
  const raw = typeof text === 'string' ? text : inputText.value
  const t = raw.trim()
  if (!t) return
  if (isSending.value) return
  if (questionType.value==='image'&&uploadedImage.value) { submitImageQuestion(); return }

  isSending.value = true
  inputText.value = ''

  const mode = currentMode.value; const ctx = buildContext()
  const aiMsg = { id: Date.now()+1, role: 'assistant', question: t, content: '', displayContent: '', status: 'loading', errorMessage: '', mode, context: ctx }
  messages.value.push({ id: Date.now(), role: 'user', question: t }, aiMsg)
  await scrollToBottom()

  closeSSE(); stopTyping()
  let fullContent = ''; let hasError = false

  streamingApiService.stream.askQuestionStream(
    { question: t, subject: ctx },
    (msg) => {
      if (msg.error) { hasError=true; aiMsg.status='error'; aiMsg.errorMessage=msg.error||'服务暂时异常，请稍后重试'; isSending.value=false; return }
      if (msg.content) { fullContent+=msg.content; if (aiMsg.status==='loading') { aiMsg.status='typing'; startTypingEffect(aiMsg, fullContent) } }
    },
    () => { if (!hasError) { stopTyping(); aiMsg.content=fullContent; aiMsg.displayContent=processMathText(fullContent); aiMsg.status='success' } isSending.value=false },
    (e) => { stopTyping(); aiMsg.status='error'; const em=e?.message||''; aiMsg.errorMessage=(em.includes('500')||em.includes('服务器'))?'服务暂时异常，请稍后重试':(em.includes('超时')||em.includes('timeout'))?'请求超时，请检查网络后重试':(em||'服务暂时异常，请稍后重试'); isSending.value=false }
  )
}

const regenerate = async (aiMsg) => {
  if (aiMsg.role!=='assistant'||isSending.value) return
  const idx=messages.value.indexOf(aiMsg); let um=null
  for(let i=idx-1;i>=0;i--){if(messages.value[i].role==='user'){um=messages.value[i];break}}
  if(!um)return
  isSending.value=true; closeSSE(); stopTyping()
  aiMsg.status='loading'; aiMsg.content=''; aiMsg.displayContent=''; aiMsg.errorMessage=''
  const t=um.question; const ctx=aiMsg.context||''; let full=''; let err=false
  streamingApiService.stream.askQuestionStream(
    {question:t,subject:ctx},
    (m)=>{if(m.error){err=true;aiMsg.status='error';aiMsg.errorMessage=m.error||'服务暂时异常，请稍后重试';isSending.value=false;return}if(m.content){full+=m.content;if(aiMsg.status==='loading'){aiMsg.status='typing';startTypingEffect(aiMsg,full)}}},
    ()=>{if(!err){stopTyping();aiMsg.content=full;aiMsg.displayContent=processMathText(full);aiMsg.status='success'}isSending.value=false},
    (e)=>{stopTyping();aiMsg.status='error';const em=e?.message||'';aiMsg.errorMessage=(em.includes('500')||em.includes('服务器'))?'服务暂时异常，请稍后重试':(em||'服务暂时异常，请稍后重试');isSending.value=false}
  )
}

const retry = async (aiMsg) => {
  if (aiMsg.role!=='assistant'||aiMsg.status!=='error'||isSending.value) return
  isSending.value=true; closeSSE(); stopTyping()
  aiMsg.status='loading'; aiMsg.errorMessage=''
  const t=aiMsg.question; const ctx=aiMsg.context||''; let full=''; let err=false
  streamingApiService.stream.askQuestionStream(
    {question:t,subject:ctx},
    (m)=>{if(m.error){err=true;aiMsg.status='error';aiMsg.errorMessage=m.error||'服务暂时异常，请稍后重试';isSending.value=false;return}if(m.content){full+=m.content;if(aiMsg.status==='loading'){aiMsg.status='typing';startTypingEffect(aiMsg,full)}}},
    ()=>{if(!err){stopTyping();aiMsg.content=full;aiMsg.displayContent=processMathText(full);aiMsg.status='success'}isSending.value=false},
    (e)=>{stopTyping();aiMsg.status='error';const em=e?.message||'';aiMsg.errorMessage=(em.includes('500')||em.includes('服务器'))?'服务暂时异常，请稍后重试':(em||'服务暂时异常，请稍后重试');isSending.value=false}
  )
}

const reAsk = (text) => { inputText.value = text || ''; nextTick(() => inputRef.value?.focus()) }

const startTypingEffect = (aiMsg, full) => {
  aiMsg.displayContent=''; let i=0
  const tick=()=>{if(i<full.length){aiMsg.displayContent+=full.charAt(i);i++;typingTimer=setTimeout(tick,28)}else{aiMsg.displayContent=processMathText(full);aiMsg.status='success'}}
  tick()
}
const stopTyping = () => { if(typingTimer){clearTimeout(typingTimer);typingTimer=null} }
const closeSSE = () => { if(sseConnection){try{sseConnection.close?.()}catch(e){};sseConnection=null} }
const copyContent = async (m) => { try{await navigator.clipboard.writeText(m.content||'')}catch(e){} }

const chatBody = ref(null)
const scrollToBottom = async () => { await nextTick(); if(chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight }

/* ========== 图片 ========== */
const questionType = ref('text'); const fileInput = ref(null); const uploadedImage = ref(null)
const toggleImageMode = () => { questionType.value=questionType.value==='text'?'image':'text'; if(questionType.value==='image')setTimeout(()=>fileInput.value?.click(),100) }
const handleFileChange = (e) => { const f=e.target.files[0]; if(f){const r=new FileReader();r.onload=(ev)=>uploadedImage.value=ev.target.result;r.readAsDataURL(f)} }
const deleteImage = () => { uploadedImage.value=null; questionType.value='text' }
const submitImageQuestion = async () => {
  if(!uploadedImage.value||isSending.value)return; isSending.value=true
  const t=inputText.value||'基于上传的图片'
  const aiMsg={id:Date.now()+1,role:'assistant',question:t,content:'',displayContent:'',status:'loading',errorMessage:'',mode:currentMode.value,context:buildContext()}
  messages.value.push({id:Date.now(),role:'user',question:'[图片提问]'},aiMsg); inputText.value=''
  try{const fd=new FormData();fd.append('question',t);const blob=await fetch(uploadedImage.value).then(r=>r.blob());fd.append('image',blob,'question.jpg');const r=await apiService.qa.askImageQuestion(fd);if(r.code===200&&r.data){aiMsg.content=r.data.answer;aiMsg.displayContent=processMathText(r.data.answer);aiMsg.status='success'}else{aiMsg.status='error';aiMsg.errorMessage='图片识别失败，请重试'}}catch(e){aiMsg.status='error';aiMsg.errorMessage=e?.errorMessage||'图片上传失败，请重试'}finally{isSending.value=false;uploadedImage.value=null;questionType.value='text'}
}

/* ========== 考研 ========== */
const examSubjects=ref([]);const selectedExamSubject=ref('');const subjectSearchQuery=ref('');const showSubjectDropdown=ref(false)
const keyPoints=ref([]);const selectedKeyPoint=ref(null);const selectedPapers=ref([])
const filteredSubjects=computed(()=>{const q=subjectSearchQuery.value.toLowerCase();return q?examSubjects.value.filter(s=>s.name.toLowerCase().includes(q)):examSubjects.value})
const fetchExamSubjects=async()=>{try{const r=await apiService.qa.getExamSubjects();if(r){examSubjects.value=r;if(r.length){selectedExamSubject.value=r[0].id;await fetchKeyPoints(r[0].id);await fetchExamPapers(r[0].id)}}}catch(e){}}
const getSelectedSubjectName=()=>{const s=examSubjects.value.find(x=>x.id===selectedExamSubject.value);return s?s.name:'选择科目'}
const fetchKeyPoints=async(sid)=>{try{const r=await apiService.qa.getKeyPoints(sid);if(r)keyPoints.value=r}catch(e){}}
const fetchExamPapers=async(sid)=>{try{const r=await apiService.qa.getExamPapers(sid);if(r)selectedPapers.value=r}catch(e){}}
const selectKeyPoint=(p)=>{selectedKeyPoint.value=p;sendQuestion('请详细讲解：'+p.name)}
const askAiPredict=()=>sendQuestion(`请基于${getSelectedSubjectName()}的历年真题趋势，分析今年的高频考点和预测方向`)

const goHome = () => router.push('/')

onMounted(async () => { if(route.query.question) inputText.value=route.query.question; await fetchBooks(); await fetchExamSubjects() })
onUnmounted(() => { closeSSE(); stopTyping() })

const processMathText = (t) => processAiResponse((t||'').replace(/\n{2,}/g,'\n'))
</script>

<style scoped>
.qa-page {
  --c: #7c5cfc; --c2: #a78bfa; --c3: #c4b5fd;
  --border: rgba(220,210,245,.5); --border2: rgba(200,185,240,.4);
  --text: #2d2438; --text2: #5b4a8a; --text3: #9088a0;
  height: calc(100vh - 60px);
  display: flex; flex-direction: column; overflow: hidden;
  background: linear-gradient(175deg, #fffdfd 0%, #f9f6ff 34%, #f8fafd 64%, #fdfaff 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
  color: var(--text); position: relative;
}
.qp-bg { position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image: radial-gradient(circle at 8% 12%, rgba(167,139,250,.10), transparent 26%), radial-gradient(circle at 92% 75%, rgba(124,92,252,.06), transparent 28%);
}

/* ===== 顶部栏 ===== */
.qa-header { display: flex; align-items: center; gap: 14px; padding: 0 20px; height: 52px; flex-shrink: 0; background: rgba(255,255,255,.82); backdrop-filter: blur(16px); border-bottom: 1px solid var(--border); position: relative; z-index: 10; }
.qah-back { width: 32px; height: 32px; border-radius: 8px; border: 1.5px solid var(--border2); background: rgba(255,255,255,.5); color: var(--text2); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .2s; flex-shrink: 0; }
.qah-back:hover { background: #f5f0ff; border-color: var(--c2); color: var(--c); }
.qah-info { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.qahi-icon { font-size: 18px; flex-shrink: 0; }
.qahi-text { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.qahi-title { font-size: 14px; font-weight: 700; color: var(--text); }
.qahi-sub { font-size: 11px; color: var(--text3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qahi-badge { font-size: 10px; padding: 2px 8px; border-radius: 8px; background: #ede4fe; color: var(--c); font-weight: 600; flex-shrink: 0; margin-left: auto; }
.qah-stats { display: flex; align-items: center; gap: 12px; flex-shrink: 0; font-size: 11px; color: var(--text3); }

.qa-body { flex: 1; display: flex; min-height: 0; overflow: hidden; position: relative; z-index: 1; }

/* ===== 左侧栏 ===== */
.qa-sidebar { width: 320px; min-width: 320px; max-width: 320px; flex: 0 0 320px; position: relative; border-right: 1px solid var(--border); transition: width .35s cubic-bezier(.4,0,.2,1), min-width .35s, max-width .35s; z-index: 2; overflow: visible; }
.qa-sidebar.collapsed { width: 0; min-width: 0; max-width: 0; flex: 0 0 0; border-right-color: transparent; }
.qas-scroll { width: 320px; min-width: 320px; height: 100%; overflow-y: auto; overflow-x: hidden; padding: 12px 14px 16px; background: rgba(255,255,255,.65); backdrop-filter: blur(14px); transition: transform .35s cubic-bezier(.4,0,.2,1), opacity .35s; }
.qa-sidebar.collapsed .qas-scroll { transform: translateX(-320px); opacity: 0; pointer-events: none; }

.qas-toggle { position: absolute; right: -18px; top: 50%; transform: translateY(-50%); width: 22px; height: 44px; border-radius: 7px; border: 1px solid var(--border); background: rgba(255,255,255,.95); color: var(--text3); cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 10; padding: 0; transition: background .2s, color .2s; box-shadow: 0 2px 8px rgba(120,100,180,.08); }
.qas-toggle:hover { color: var(--c); background: #fff; }
.qas-toggle svg { transition: transform .35s cubic-bezier(.4,0,.2,1); }
.qas-toggle svg.rotated { transform: rotate(180deg); }

.qas-mode { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.qasm-btn { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; border: 1.5px solid transparent; cursor: pointer; background: rgba(248,246,255,.6); font-family: inherit; transition: all .2s; text-align: left; }
.qasm-btn:hover { background: #f5f0ff; border-color: var(--c3); }
.qasm-btn.active { background: #ede4fe; border-color: var(--c); box-shadow: 0 0 0 2px rgba(124,92,252,.06); }
.qasm-icon { font-size: 20px; flex-shrink: 0; }
.qasm-text { display: flex; flex-direction: column; gap: 1px; }
.qasm-label { font-size: 13px; font-weight: 700; color: var(--text); }
.qasm-btn.active .qasm-label { color: var(--c); }
.qasm-desc { font-size: 10px; color: var(--text3); }

.qas-resource { background: linear-gradient(135deg,#f8f4ff,#fdfaff); border:1.5px solid var(--border); border-radius:10px; padding:12px 14px; margin-bottom:14px; }
.qasr-head { font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.6px;color:var(--text3);margin-bottom:8px; }
.qasr-body { display:flex;align-items:center;gap:10px; }
.qasr-emoji { font-size:28px;flex-shrink:0; }
.qasr-info { flex:1;min-width:0; }
.qasr-name { font-size:13px;font-weight:700;color:var(--text);white-space:nowrap;overflow:hidden;text-overflow:ellipsis; }
.qasr-extra { font-size:11px;color:var(--text3);margin-top:2px; }

.qas-search { display:flex;align-items:center;gap:7px;padding:8px 12px;border-radius:10px;margin-bottom:10px;background:rgba(248,246,255,.7);border:1.5px solid var(--border2);transition:all .2s; }
.qas-search:focus-within { border-color:var(--c);background:#fff;box-shadow:0 0 0 3px rgba(124,92,252,.05); }
.qas-search svg { color:var(--text3);flex-shrink:0; }
.qas-search input { flex:1;border:none;outline:none;background:transparent;font-size:12px;color:var(--text);font-family:inherit; }
.qas-search input::placeholder { color:#b0a8c0; }

/* 搜索入口 */
.qas-search-wrap { margin-bottom: 10px; }
.qas-search-wrap .qas-search { cursor: pointer; }
.qas-search-placeholder { font-size: 12px; color: #b0a8c0; flex: 1; }

/* 上下文栏 */
.qas-ctx { display:flex;align-items:center;gap:10px;padding:10px 12px;border-radius:12px;margin-bottom:8px;background:linear-gradient(135deg,rgba(124,92,252,.08),rgba(167,139,250,.04));border:1.5px solid rgba(167,139,250,.18);animation:ctxIn .3s ease-out; }
@keyframes ctxIn { from{opacity:0;transform:translateY(-4px)} to{opacity:1;transform:translateY(0)} }
.qas-ctx-left { display:flex;align-items:center;gap:9px;flex:1;min-width:0; }
.qas-ctx-icon { font-size:20px; }
.qas-ctx-info { display:flex;flex-direction:column;gap:0;min-width:0; }
.qas-ctx-name { font-size:12px;font-weight:700;color:var(--text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.qas-ctx-pub { font-size:10px;color:var(--text3);margin-top:1px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.qas-ctx-actions { display:flex;gap:6px;flex-shrink:0; }
.qas-ctx-switch { font-size:10px;padding:4px 10px;border-radius:6px;border:1px solid rgba(167,139,250,.2);background:rgba(255,255,255,.6);color:var(--c);cursor:pointer;transition:all .15s;white-space:nowrap;font-family:inherit; }
.qas-ctx-switch:hover { background:rgba(124,92,252,.08);border-color:var(--c2); }
.qas-ctx-clear { font-size:10px;padding:4px 10px;border-radius:6px;border:1px solid rgba(200,185,240,.25);background:rgba(255,255,255,.4);color:var(--text3);cursor:pointer;transition:all .15s;white-space:nowrap;font-family:inherit; }
.qas-ctx-clear:hover { background:rgba(255,200,200,.25);border-color:#f87171;color:#ef4444; }

/* 教材选择浮层 */
.qas-panel-overlay { position:fixed;inset:0;z-index:2000;background:rgba(20,16,40,.08);display:flex;align-items:flex-start;justify-content:flex-start;padding:172px 0 0 24px;animation:qaspoIn .15s ease-out; }
@keyframes qaspoIn { from{opacity:0} to{opacity:1} }

.qas-panel { width:420px;max-height:440px;border-radius:20px;background:rgba(255,255,255,.86);backdrop-filter:blur(22px);-webkit-backdrop-filter:blur(22px);border:1px solid rgba(255,255,255,.55);box-shadow:0 20px 60px rgba(90,60,180,.18),0 4px 12px rgba(0,0,0,.05),0 0 0 1px rgba(255,255,255,.4) inset;display:flex;flex-direction:column;overflow:hidden;animation:qaspIn .2s cubic-bezier(.16,1,.3,1); }
@keyframes qaspIn { from{opacity:0;transform:translateY(-8px) scale(.97)} to{opacity:1;transform:translateY(0) scale(1)} }

.qas-panel-bar { display:flex;align-items:center;gap:10px;padding:14px 16px;border-bottom:1px solid rgba(200,185,240,.12); }
.qas-panel-bar svg { color:var(--c2);flex-shrink:0; }
.qas-panel-input { flex:1;border:none;outline:none;background:none;font-size:14px;font-family:inherit;color:var(--text); }
.qas-panel-input::placeholder { color:#b0a8c0; }
.qas-panel-esc { font-size:9px;font-weight:600;color:#b0a8c0;padding:2px 7px;border-radius:4px;border:1px solid rgba(200,185,240,.2); }

.qas-panel-body { flex:1;overflow-y:auto;padding:6px 12px 14px; }
.qas-panel-body::-webkit-scrollbar { width:3px; }
.qas-panel-body::-webkit-scrollbar-thumb { background:rgba(180,165,220,.2);border-radius:3px; }

.qas-panel-sec { padding:4px 2px; }
.qas-panel-sec + .qas-panel-sec { border-top:1px solid rgba(200,190,230,.08);margin-top:4px;padding-top:10px; }

.qas-panel-sec-head { font-size:10px;font-weight:700;color:var(--text3);text-transform:uppercase;letter-spacing:.4px;padding:4px 6px 6px; }
.qas-panel-count { font-size:9px;padding:1px 6px;border-radius:5px;background:rgba(124,92,252,.07);color:var(--c); }

.qas-panel-chips { display:flex;gap:6px;flex-wrap:wrap;padding:0 4px; }
.qas-panel-chip { padding:5px 12px;border-radius:8px;border:1px solid rgba(200,185,240,.2);background:rgba(255,255,255,.5);font-size:11px;font-family:inherit;color:var(--text2);cursor:pointer;transition:all .15s; }
.qas-panel-chip:hover { border-color:var(--c2);background:rgba(255,255,255,.8);transform:translateY(-1px); }

.qas-panel-item { display:flex;align-items:center;gap:10px;width:100%;padding:8px 10px;border-radius:9px;border:none;background:transparent;cursor:pointer;font-family:inherit;transition:all .15s;text-align:left;animation:qaspiIn .25s ease-out both; }
@keyframes qaspiIn { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
.qas-panel-item:hover { background:rgba(245,240,255,.6); }
.qas-panel-item.on { background:rgba(124,92,252,.05);border:1px solid rgba(167,139,250,.2); }

.qas-panel-item-cover { width:30px;height:40px;border-radius:4px;flex-shrink:0;background:linear-gradient(140deg,#ede4fe,#ddd2f8);display:flex;align-items:center;justify-content:center;font-size:16px; }

.qas-panel-item-info { flex:1;min-width:0;display:flex;flex-direction:column;gap:1px; }
.qas-panel-item-name { font-size:12px;font-weight:600;color:var(--text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.qas-panel-item-meta { font-size:10px;color:var(--text3);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }

.qas-panel-item-ctx { font-size:9px;font-weight:700;color:var(--c);padding:2px 7px;border-radius:6px;background:rgba(124,92,252,.07);flex-shrink:0; }
.qas-panel-item-pct { font-size:10px;font-weight:700;color:#7c3aed;padding:2px 6px;border-radius:6px;background:rgba(167,139,250,.07);flex-shrink:0; }

.qas-panel-empty { text-align:center;padding:20px;font-size:12px;color:var(--text3); }

.qas-label { font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.6px;color:var(--text3);padding:6px 2px; }
.qas-skel { display:flex;flex-direction:column;gap:8px; }
.qass-item { height:60px;border-radius:10px;background:rgba(200,185,240,.12);animation:shimmer 1.6s ease-in-out infinite; }
@keyframes shimmer { 0%,100%{opacity:.4} 50%{opacity:.8} }
.qas-empty { font-size:12px;color:var(--text3);text-align:center;padding:20px; }
.qas-books { display:flex;flex-direction:column;gap:5px; }
.qasb-card { display:flex;gap:10px;padding:9px 10px;border-radius:10px;cursor:pointer;transition:all .2s;border:1.5px solid transparent; }
.qasb-card:hover { background:#faf8ff;border-color:var(--c3);transform:translateY(-1px);box-shadow:0 2px 16px rgba(120,100,180,.05); }
.qasb-card.active { background:#f3edfc;border-color:var(--c); }
.qasb-cover { width:36px;height:48px;border-radius:4px;flex-shrink:0;background:linear-gradient(140deg,#ede4fe,#ddd2f8);display:flex;align-items:center;justify-content:center;font-size:18px; }
.qasb-info { flex:1;min-width:0;display:flex;flex-direction:column;gap:2px; }
.qasb-name { font-size:12px;font-weight:600;color:var(--text);white-space:nowrap;overflow:hidden;text-overflow:ellipsis; }
.qasb-author { font-size:10px;color:var(--text3); }
.qasb-bar { height:2px;background:#f0ecf8;border-radius:10px;overflow:hidden;margin-top:2px; }
.qasb-fill { height:100%;border-radius:10px;background:linear-gradient(90deg,var(--c2),var(--c));transition:width .6s; }
.qas-chapters { margin-top:4px; }
.qasch-head { display:flex;align-items:center;gap:6px;padding:4px;border-radius:6px;cursor:pointer;font-size:11px;color:var(--text2);transition:all .15s; }
.qasch-head:hover { background:#f5f0ff; }
.qasch-arrow { color:var(--text3);transition:transform .2s;flex-shrink:0; }
.qasch-arrow.open { transform:rotate(90deg); }
.qasch-num { font-size:9px;color:var(--text3);margin-left:auto;background:#f5f0ff;padding:1px 6px;border-radius:6px; }
.qasch-sub { margin-left:16px; }
.qasch-sub-item { padding:3px 6px;font-size:11px;color:var(--text3);cursor:pointer;border-radius:5px;transition:all .12s; }
.qasch-sub-item:hover { background:#f5f0ff;color:var(--c); }

.qas-related { margin-top:8px; }
.qas-related .qasb-card { padding:6px 8px;border-radius:8px; }
.qas-drop { position:absolute;z-index:50;background:#fff;border:1.5px solid var(--border2);border-radius:10px;max-height:150px;overflow-y:auto;margin-top:-6px;box-shadow:0 8px 32px rgba(120,100,180,.08);left:14px;right:14px; }
.qasd-opt { padding:8px 12px;font-size:12px;cursor:pointer;transition:background .12s; }
.qasd-opt:hover { background:#f5f0ff; }
.qasd-opt.active { background:#ede4fe;color:var(--c);font-weight:600; }
.qas-kp { display:flex;flex-direction:column;gap:3px; }
.qask-item { display:flex;align-items:center;gap:8px;padding:7px 8px;border-radius:7px;cursor:pointer;transition:all .15s;font-size:11px;color:var(--text2); }
.qask-item:hover { background:#f5f0ff; }
.qask-item.active { background:#ede4fe;color:var(--c);font-weight:600; }
.qask-rank { width:18px;height:18px;border-radius:50%;background:#ede4fe;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;color:var(--c);flex-shrink:0; }
.qas-papers { display:flex;flex-direction:column;gap:3px; }
.qasp-item { display:flex;align-items:center;gap:8px;padding:6px 8px;border-radius:7px;font-size:11px;color:var(--text2); }
.qasp-year { padding:2px 7px;border-radius:4px;background:#ede4fe;font-size:10px;font-weight:700;color:var(--c); }
.qas-predict { display:flex;align-items:center;gap:10px;padding:11px 12px;margin-top:8px;border-radius:10px;cursor:pointer;transition:all .25s;background:linear-gradient(135deg,#f8f4ff,#fdfaff);border:1.5px solid var(--c3); }
.qas-predict:hover { transform:translateY(-1px);box-shadow:0 8px 32px rgba(120,100,180,.08);border-color:var(--c); }
.qaspd-icon { font-size:22px;flex-shrink:0; }
.qaspd-info { flex:1;font-size:12px;font-weight:600;color:var(--text); }
.qaspd-info div:last-child { font-size:10px;color:var(--text3);margin-top:1px;font-weight:400; }

/* ===== 历史对话 ===== */
.qas-history { margin-top: 16px; }
.qash-head { display: flex; align-items: center; justify-content: space-between; }
.qash-new-btn { padding: 4px 10px; border-radius: 14px; border: 1.5px solid var(--c3); background: #fff; color: var(--c); font-size: 11px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; }
.qash-new-btn:hover { background: #ede4fe; }
.qash-list { display: flex; flex-direction: column; gap: 4px; margin-top: 6px; }
.qash-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 8px; cursor: pointer; transition: all .15s; border: 1.5px solid transparent; }
.qash-item:hover { background: #f5f0ff; }
.qash-item.active { background: #ede4fe; border-color: var(--c); }
.qashi-icon { font-size: 16px; flex-shrink: 0; }
.qashi-info { flex: 1; min-width: 0; }
.qashi-title { font-size: 12px; font-weight: 600; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qashi-meta { font-size: 10px; color: var(--text3); margin-top: 2px; }
.qashi-del { width: 22px; height: 22px; border-radius: 50%; border: none; background: transparent; color: var(--text3); cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; opacity: 0; transition: all .15s; }
.qash-item:hover .qashi-del { opacity: 1; }
.qashi-del:hover { background: #fee2e2; color: #ef4444; }

.qas-new-session-btn { width: 100%; margin-top: 12px; padding: 8px; border-radius: 10px; border: 1.5px dashed var(--border2); background: transparent; color: var(--c); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; }
.qas-new-session-btn:hover { background: #f5f0ff; border-color: var(--c); }

/* ===== 主聊天区 ===== */
.qa-main { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; z-index: 1; }
.qam-messages { flex: 1; min-height: 0; min-width: 0; overflow-y: auto; overflow-x: hidden; padding: 32px 48px 24px; display: flex; flex-direction: column; gap: 20px; }

.qam-empty { max-width: 640px; margin: 0 auto; width: 100%; padding: 24px 0 40px; }
.qame-hero { text-align: center; padding: 24px 0; }
.qame-avatar { width: 64px; height: 64px; margin: 0 auto 14px; display: flex; align-items: center; justify-content: center; }
.qame-avatar > span { font-size: 40px; animation: float 3s ease-in-out infinite; }
@keyframes float { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-5px)} }
.qame-hero h2 { font-size: 22px; font-weight: 800; color: var(--text); margin: 0; letter-spacing: -.3px; }
.qame-hero p { font-size: 13px; color: var(--text3); margin: 6px 0 0; }
.qame-label { font-size: 12px; font-weight: 700; color: var(--text2); margin: 16px 0 8px; }
.qame-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.qamec-card { padding: 15px 17px; border-radius: 14px; cursor: pointer; background: rgba(255,255,255,.72); border: 1.5px solid rgba(220,210,245,.35); transition: all .25s; }
.qamec-card:hover { border-color: var(--c3); background: #fcfaff; transform: translateY(-2px); box-shadow: 0 6px 24px rgba(124,92,252,.08); }
.qamec-icon { font-size: 18px; margin-bottom: 6px; display: block; }
.qamec-text { font-size: 13px; font-weight: 600; color: var(--text); }
.qamec-tag { margin-top: 6px; font-size: 10px; color: var(--c); background: #ede4fe; padding: 2px 8px; border-radius: 8px; display: inline-block; }
.qame-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.qamec-chip { padding: 7px 14px; border-radius: 16px; font-size: 12px; background: rgba(248,246,255,.8); border: 1.5px solid rgba(200,185,240,.3); color: var(--text2); cursor: pointer; transition: all .2s; }
.qamec-chip:hover { border-color: var(--c2); background: #f5f0ff; color: var(--c); }

/* ── 消息 ── */
.qam-row { display: flex; width: 100%; min-width: 0; }
.qam-row.user { justify-content: flex-end; }
.qam-row.assistant { justify-content: flex-start; align-items: flex-start; gap: 12px; }
.qam-bubble { max-width: min(680px, 72%); min-width: 80px; padding: 14px 18px; border-radius: 18px; line-height: 1.7; word-break: break-word; overflow-wrap: anywhere; white-space: pre-wrap; font-size: 14px; }
.qam-bubble.user { background: linear-gradient(135deg, #7c5cfc, #9b7cff); color: #fff; border-radius: 18px 18px 4px 18px; }
.qam-bubble.ai { background: #fff; color: #1f2937; border: 1px solid #ebe4ff; border-radius: 18px 18px 18px 4px; box-shadow: 0 8px 24px rgba(124,92,252,.08); }
.qam-avatar { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; background: linear-gradient(135deg, #7c5cfc, #a78bfa); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px; }
.qam-ai-wrap { display: flex; flex-direction: column; gap: 8px; max-width: min(680px, 72%); min-width: 0; }

.qam-loading { display: flex; align-items: center; gap: 5px; font-size: 12px; color: var(--text3); }
.qam-loading span { width: 7px; height: 7px; border-radius: 50%; background: var(--c2); animation: bounce 1.2s ease-in-out infinite; }
.qam-loading span:nth-child(2) { animation-delay: .15s; }
.qam-loading span:nth-child(3) { animation-delay: .3s; }
@keyframes bounce { 0%,100%{opacity:.3;transform:scale(.7)} 50%{opacity:1;transform:scale(1.2)} }

.qam-err { min-width: 280px; max-width: 520px; padding: 18px 20px; }
.qame-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.qameh-icon { font-size: 20px; }
.qameh-title { font-size: 14px; font-weight: 700; color: #b91c1c; }
.qame-desc { font-size: 13px; color: var(--text3); margin: 0 0 14px; }
.qame-btns { display: flex; align-items: center; gap: 10px; }
.qame-btns button { height: 36px; padding: 0 16px; border-radius: 12px; font-size: 13px; font-weight: 700; white-space: nowrap; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; font-family: inherit; transition: all .2s; border: none; }
.qameb-retry { background: linear-gradient(135deg, #7c5cfc, #9b7cff); color: #fff; }
.qameb-retry:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(124,92,252,.35); }
.qameb-reask { background: #f5f0ff; color: var(--c); border: 1.5px solid rgba(200,185,240,.4) !important; }
.qameb-reask:hover { background: #ede4fe; }
button:disabled { opacity: .5; cursor: not-allowed; }

.qam-typing { position: relative; }
.qam-cursor { display: inline-block; animation: blink 1s infinite; color: var(--c); font-weight: 100; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
.qam-html { line-height: 1.7; }
.qam-html :deep(p) { margin: .2em 0; }
.qam-html :deep(h1),.qam-html :deep(h2),.qam-html :deep(h3) { margin: .3em 0 .1em; font-weight: 700; }
.qam-html :deep(pre) { background: #f5f0fc; border-radius: 10px; padding: 12px; overflow-x: auto; font-size: 13px; max-width: 100%; }
.qam-html :deep(code) { font-family: monospace; font-size: .9em; word-break: break-all; }
.qam-html :deep(ul),.qam-html :deep(ol) { padding-left: 1.5em; }
.qam-html :deep(blockquote) { border-left: 3px solid var(--c3); padding-left: 1em; color: var(--text3); }
.qam-empty-content { color: var(--text3); font-style: italic; }

.qam-actions { display: flex; gap: 6px; }
.qam-actions button { padding: 4px 12px; border-radius: 7px; border: 1.5px solid rgba(200,185,240,.4); background: #fff; font-size: 11px; cursor: pointer; color: var(--text3); font-family: inherit; }
.qam-actions button:hover:not(:disabled) { background: #f5f0ff; color: var(--c); }

.qam-followup { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; padding: 8px 48px; flex-shrink: 0; font-size: 11px; color: var(--text3); }
.qam-followup span:first-child { font-weight: 600; }
.qam-followup span+span { padding: 4px 12px; border-radius: 14px; font-size: 11px; background: #f5f0ff; color: var(--c); cursor: pointer; border: 1.5px solid transparent; transition: all .2s; }
.qam-followup span+span:hover { border-color: var(--c); background: #ede4fe; }

/* ===== 输入栏 ===== */
.qam-input { flex-shrink: 0; padding: 14px 32px 18px; background: linear-gradient(to top, rgba(255,255,255,.96), rgba(255,255,255,.65)); border-top: 1px solid rgba(220,210,245,.3); }
.qami-preview { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; padding: 6px 10px; background: #faf9fd; border-radius: 10px; border: 1.5px solid rgba(200,185,240,.3); }
.qami-preview img { max-height: 48px; border-radius: 6px; }
.qami-preview button { width: 22px; height: 22px; border-radius: 50%; border: none; background: #ef4444; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.qami-bar { display: flex; align-items: center; gap: 6px; padding: 5px 6px 5px 16px; border-radius: 22px; background: rgba(255,255,255,.88); backdrop-filter: blur(20px); border: 1.5px solid rgba(200,185,240,.35); box-shadow: 0 2px 24px rgba(120,100,180,.06); transition: all .35s; }
.qami-bar:focus-within { border-color: var(--c); box-shadow: 0 2px 32px rgba(124,92,252,.1), 0 0 0 4px rgba(124,92,252,.04); }
.qamib-img { width: 34px; height: 34px; border-radius: 9px; border: none; background: transparent; color: var(--text3); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .15s; flex-shrink: 0; }
.qamib-img:hover { color: var(--c); background: #f5f0ff; }
.qamib-img.active { color: var(--c); background: #ede4fe; }
.qamib-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: var(--text); padding: 9px 4px; font-family: inherit; }
.qamib-input::placeholder { color: #c0b8d0; }

.qami-ctx-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 8px;
  font-size: 11px; font-weight: 700; color: var(--c);
  background: rgba(124,92,252,.06); border: 1px solid rgba(167,139,250,.18);
  cursor: pointer; white-space: nowrap; flex-shrink: 0;
  transition: all .2s; animation: ctxTagIn .3s ease-out;
}
@keyframes ctxTagIn { from{opacity:0;transform:scale(.9)} to{opacity:1;transform:scale(1)} }
.qami-ctx-tag:hover { background: rgba(124,92,252,.1); border-color: var(--c2); transform: translateY(-1px); }
.qamib-send { width: 38px; height: 38px; border-radius: 50%; border: none; background: #f0ecf8; color: #c0b8d0; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .25s; flex-shrink: 0; }
.qamib-send.ready { background: linear-gradient(135deg, #7c5cfc, #a78bfa); color: #fff; }
.qamib-send.ready:hover:not(:disabled) { transform: scale(1.06); box-shadow: 0 4px 16px rgba(124,92,252,.3); }
.qamib-send:disabled { cursor: not-allowed; opacity: .5; }
.qamib-send.loading { background: var(--c); }
.qamib-spin { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,.3); border-top-color: #fff; border-radius: 50%; animation: spin .6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.qas-scroll::-webkit-scrollbar, .qam-messages::-webkit-scrollbar { width: 4px; }
.qas-scroll::-webkit-scrollbar-track, .qam-messages::-webkit-scrollbar-track { background: transparent; }
.qas-scroll::-webkit-scrollbar-thumb, .qam-messages::-webkit-scrollbar-thumb { background: rgba(200,185,240,.3); border-radius: 4px; }

@media (max-width: 900px) {
  .qa-sidebar { width: 280px; min-width: 280px; max-width: 280px; flex: 0 0 280px; }
  .qa-sidebar.collapsed { width: 0; min-width: 0; max-width: 0; flex: 0 0 0; }
  .qas-scroll { width: 280px; min-width: 280px; }
  .qa-sidebar.collapsed .qas-scroll { transform: translateX(-280px); }
  .qah-stats { display: none; }
  .qam-messages { padding: 20px 24px 16px; }
  .qam-input { padding: 12px 20px 14px; }
  .qame-cards { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .qa-sidebar { display: none; }
  .qas-toggle { display: none; }
  .qam-messages { padding: 16px; }
  .qam-input { padding: 10px 12px 14px; }
}
</style>
