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
          <div class="qas-history">
            <div class="qash-head">
              <span class="qas-label">📋 历史对话</span>
              <button class="qash-new-btn" @click="startNewSession" title="新对话">+ 新对话</button>
            </div>
            <div class="qash-list" v-if="sessions.length > 0">
              <div
                v-for="s in sessions"
                :key="s.id"
                class="qash-item"
                :class="{ active: activeSessionId === s.id }"
                @click="s.isBackend ? loadBackendHistory(s) : loadSession(s)"
              >
                <div class="qashi-icon">{{ s.mode === 'exam' ? '🎓' : '💬' }}</div>
                <div class="qashi-info">
                  <div class="qashi-title">{{ s.title }}</div>
                  <div class="qashi-meta">{{ formatSessionTime(s.createdAt) }}</div>
                </div>
                <button class="qashi-del" @click.stop="confirmDelete(s)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14c0 1.1-.9 2-2 2H7c-1.1 0-2-.9-2-2V6m3 0V4c0-1.1.9-2 2-2h4c1.1 0 2 .9 2 2v2"/></svg>
                </button>
              </div>
            </div>
            <div v-else class="qash-empty">
              <div class="qashe-icon">📝</div>
              <div class="qashe-text">暂无对话记录</div>
              <button class="qashe-new-btn" @click="startNewSession">开始新对话</button>
            </div>
          </div>
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
            <div v-if="msg.role === 'user'" class="qam-bubble user">
              <img v-if="msg.image" :src="msg.image" class="qam-image" alt="用户上传的图片" />
              <span>{{ msg.question }}</span>
            </div>
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
                    <div class="qam-html" v-html="msg.displayContent"></div><span class="qam-cursor">|</span>
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
import { ref, computed, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { streamingApiService } from '@/api/streaming'
import { apiService } from '@/api/api'
import { processAiResponse, processLatexOnly, markdownToPlainText } from '@/utils/markdown/aiResponse'
import { ElMessageBox } from 'element-plus'

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
const aiSessionId = ref(null)

// localStorage和sessionStorage中会话ID的key
const SESSION_ID_KEY = 'qa_session_id'
const SESSION_ID_KEY_SESSION_STORAGE = 'qa_session_id_session'

// 从存储获取会话ID（优先localStorage，其次sessionStorage）
const getStoredSessionId = () => {
  try {
    // 优先从localStorage获取（持久化存储）
    let sessionId = localStorage.getItem(SESSION_ID_KEY)
    if (sessionId) return sessionId
    
    // 其次从sessionStorage获取（会话级存储）
    sessionId = sessionStorage.getItem(SESSION_ID_KEY_SESSION_STORAGE)
    return sessionId
  } catch (e) {
    return null
  }
}

// 保存会话ID到存储（同时保存到localStorage和sessionStorage）
const saveSessionId = (sessionId) => {
  try {
    // 保存到localStorage（持久化）
    localStorage.setItem(SESSION_ID_KEY, sessionId)
    // 保存到sessionStorage（会话级，防止localStorage被禁用）
    sessionStorage.setItem(SESSION_ID_KEY_SESSION_STORAGE, sessionId)
    aiSessionId.value = sessionId
  } catch (e) {
  }
}

// 清除存储的会话ID
const clearStoredSessionId = () => {
  try {
    localStorage.removeItem(SESSION_ID_KEY)
    sessionStorage.removeItem(SESSION_ID_KEY_SESSION_STORAGE)
    aiSessionId.value = null
  } catch (e) {
  }
}

// 验证会话ID是否有效（宽松验证：只要是非空字符串即可）
const isValidSessionId = (sessionId) => {
  if (!sessionId || typeof sessionId !== 'string') return false
  // 宽松验证：只要长度在合理范围内且不为空
  return sessionId.length >= 10 && sessionId.length <= 100
}

const fetchBackendHistory = async () => {
  try {
    const authStore = useAuthStore()
    const userId = authStore.userId
    if (!userId) return
    const r = await apiService.qa.getHistorySessions({ params: { userId } })
    if (r && r.data) {
      const backendSessions = r.data.map(session => ({
        id: session.sessionId ? session.sessionId : Date.now(),
        title: session.title || '对话记录',
        mode: 'text',
        resource: session.bookId ? { type: 'book', id: session.bookId, name: session.context } : null,
        messages: session.messages.map((msg, idx) => [
          { id: Date.now() + idx * 2, role: 'user', question: msg.question, timestamp: msg.createdAt },
          { id: Date.now() + idx * 2 + 1, role: 'assistant', content: msg.answer, displayContent: processMathText(msg.answer), status: 'success', timestamp: msg.createdAt }
        ]).flat(),
        msgCount: session.messages.length * 2,
        createdAt: session.createdAt,
        updatedAt: session.updatedAt,
        isBackend: true,
        backendIds: session.messages.map(m => m.id)
      }))
      
      sessions.value = [...backendSessions, ...sessions.value]
    }
  } catch (e) {
  }
}

// 从后端历史记录加载对话到聊天区
const loadBackendHistory = (item) => {
  if (messages.value.length > 0 && activeSessionId.value !== null) {
    saveCurrentSilent()
  }
  activeSessionId.value = item.id
  currentMode.value = 'text'
  messages.value = item.messages.map(m => ({ ...m }))
  inputText.value = ''
  isSending.value = false
  if (sidebarCollapsed.value) sidebarCollapsed.value = false
  // 加载历史对话后滚动到底部
  nextTick(() => scrollToBottom())
}

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
  // 加载历史对话后滚动到底部
  nextTick(() => scrollToBottom())
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
  // 清除会话ID，开始新会话
  clearStoredSessionId()
}

const confirmDelete = async (s) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条对话记录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    if (s.isBackend) {
      deleteBackendSession(s)
    } else {
      deleteSession(s.id)
    }
  } catch (e) {
    // 用户取消删除
  }
}

const deleteSession = (id) => {
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (activeSessionId.value === id) {
    activeSessionId.value = null
    messages.value = []
    inputText.value = ''
  }
}

const deleteBackendSession = async (s) => {
  if (!s.isBackend || !s.backendIds || s.backendIds.length === 0) return
  try {
    for (const id of s.backendIds) {
      await apiService.qa.deleteHistory(id)
    }
  } catch (e) {
  }
  sessions.value = sessions.value.filter(session => session.id !== s.id)
  if (activeSessionId.value === s.id) {
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
  aiSessionId.value = null
  clearStoredSessionId()
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
    const r = await apiService.qa.getBooks();
    if (r && r.data && r.data.length) {
      books.value = r.data;
      loading.value.books = false;
      return;
    }
  } catch (e) {
  }
  books.value = MOCK_BOOKS
  loading.value.books = false
}
const fetchChapters = async (id) => {
  loading.value.chapters = true
  try { 
    const r = await apiService.qa.getChapters(id); 
    if (r && r.data && r.data.length) { 
      chapters.value = r.data; 
      loading.value.chapters = false; 
      return 
    } 
  } catch (e) {
  }
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
  const aiMsg = reactive({ id: Date.now()+1, role: 'assistant', question: t, content: '', displayContent: '', status: 'loading', errorMessage: '', mode, context: ctx })
  messages.value.push({ id: Date.now(), role: 'user', question: t }, aiMsg)
  await scrollToBottom()

  closeSSE(); stopTyping()
  let fullContent = ''; let hasError = false
  const authStore = useAuthStore()
  const userId = authStore.userId

  // 获取会话ID：优先使用内存中的，其次从localStorage获取
  let currentSessionId = aiSessionId.value || getStoredSessionId()

  streamingApiService.stream.askQuestionStream(
    { question: t, subject: ctx, userId: userId, sessionId: currentSessionId },
    (msg) => {
      // 从响应中获取sessionId（通过SseMessage对象的sessionId字段传递）
      if (msg.sessionId) {
        // 验证会话ID格式
        if (isValidSessionId(msg.sessionId)) {
          // 如果收到新的sessionId，说明之前的无效或需要创建新会话
          if (!currentSessionId || currentSessionId !== msg.sessionId) {
            saveSessionId(msg.sessionId)
          }
        }
      }
      if (msg.error) { hasError=true; aiMsg.status='error'; aiMsg.errorMessage=msg.error||'服务暂时异常，请稍后重试'; isSending.value=false; return }
      if (msg.content) { 
        fullContent+=msg.content; 
        aiMsg.status='typing'; 
        aiMsg._targetContent = fullContent; // 始终更新目标内容
        if (!typingTimer) {
          startTypingEffect(aiMsg, fullContent)
        }
      }
    },
    () => {
      if (!hasError) {
        aiMsg.content=fullContent;
        aiMsg._targetContent = fullContent;
        
        // 如果打字机没在运行，启动它
        if (!typingTimer) {
          startTypingEffect(aiMsg, fullContent);
        }
        
        // 等待打字完成，每100ms检查一次
        const waitForTyping = setInterval(() => {
          if (!typingTimer) {
            aiMsg.status='success';
            isSending.value=false;
            clearInterval(waitForTyping);
          }
        }, 100);
        
        // 最长等待30秒，超过则强制完成
        setTimeout(() => {
          clearInterval(waitForTyping);
          if (!aiMsg.status === 'success') {
            aiMsg.status='success';
            isSending.value=false;
          }
        }, 30000);
      } else {
        isSending.value=false;
      }
    },
    (e) => { 
      stopTyping(); 
      aiMsg.status='error'; 
      const em=e?.message||''; 
      aiMsg.errorMessage=(em.includes('500')||em.includes('服务器'))?'服务暂时异常，请稍后重试':(em.includes('超时')||em.includes('timeout'))?'请求超时，请检查网络后重试':(em||'服务暂时异常，请稍后重试'); 
      isSending.value=false;
      // 如果发生错误，清除无效的会话ID
      if (em.includes('500') || em.includes('服务器')) {
        clearStoredSessionId()
      }
    }
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
  const authStore = useAuthStore()
  const userId = authStore.userId
  // 获取会话ID：优先使用内存中的，其次从localStorage获取
  let currentSessionId = aiSessionId.value || getStoredSessionId()
  streamingApiService.stream.askQuestionStream(
    {question:t,subject:ctx,userId:userId,sessionId:currentSessionId},
    (m)=>{
      // 处理sessionId
      if (m.sessionId && isValidSessionId(m.sessionId)) {
        if (!currentSessionId || currentSessionId !== m.sessionId) {
          saveSessionId(m.sessionId)
        }
      }
      if(m.error){err=true;aiMsg.status='error';aiMsg.errorMessage=m.error||'服务暂时异常，请稍后重试';isSending.value=false;return}
      if(m.content){
        full+=m.content;
        aiMsg.status='typing';
        aiMsg._targetContent = full;
        if (!typingTimer) {
          startTypingEffect(aiMsg, full)
        }
      }
    },
    ()=>{
      if(!err){
        aiMsg.content=full;
        aiMsg._targetContent = full;
        if(!typingTimer){
          startTypingEffect(aiMsg, full);
        }
        const waitForTyping = setInterval(()=>{
          if(!typingTimer){
            aiMsg.status='success';
            isSending.value=false;
            clearInterval(waitForTyping);
          }
        },100);
        setTimeout(()=>{
          clearInterval(waitForTyping);
          if(aiMsg.status !== 'success'){
            aiMsg.status='success';
            isSending.value=false;
          }
        },30000);
      }else{
        isSending.value=false;
      }
    },
    (e)=>{stopTyping();aiMsg.status='error';const em=e?.message||'';aiMsg.errorMessage=(em.includes('500')||em.includes('服务器'))?'服务暂时异常，请稍后重试':(em||'服务暂时异常，请稍后重试');isSending.value=false}
  )
}

const retry = async (aiMsg) => {
  if (aiMsg.role!=='assistant'||aiMsg.status!=='error'||isSending.value) return
  isSending.value=true; closeSSE(); stopTyping()
  aiMsg.status='loading'; aiMsg.errorMessage=''
  const t=aiMsg.question; const ctx=aiMsg.context||''; let full=''; let err=false
  const authStore = useAuthStore()
  const userId = authStore.userId
  // 获取会话ID：优先使用内存中的，其次从localStorage获取
  let currentSessionId = aiSessionId.value || getStoredSessionId()
  streamingApiService.stream.askQuestionStream(
    {question:t,subject:ctx,userId:userId,sessionId:currentSessionId},
    (m)=>{
      // 处理sessionId
      if (m.sessionId && isValidSessionId(m.sessionId)) {
        if (!currentSessionId || currentSessionId !== m.sessionId) {
          saveSessionId(m.sessionId)
        }
      }
      if(m.error){err=true;aiMsg.status='error';aiMsg.errorMessage=m.error||'服务暂时异常，请稍后重试';isSending.value=false;return}
      if(m.content){
        full+=m.content;
        aiMsg.status='typing';
        aiMsg._targetContent = full;
        if (!typingTimer) {
          startTypingEffect(aiMsg, full)
        }
      }
    },
    ()=>{
      if(!err){
        aiMsg.content=full;
        aiMsg._targetContent = full;
        if(!typingTimer){
          startTypingEffect(aiMsg, full);
        }
        const checkComplete = setInterval(()=>{
          if(!typingTimer && aiMsg.displayContent?.length >= full.length){
            aiMsg.displayContent=markdownToPlainText(full);
            aiMsg.status='success';
            isSending.value=false;
            clearInterval(checkComplete);
          }
        },100);
      }else{
        isSending.value=false;
      }
    },
    (e)=>{stopTyping();aiMsg.status='error';const em=e?.message||'';aiMsg.errorMessage=(em.includes('500')||em.includes('服务器'))?'服务暂时异常，请稍后重试':(em||'服务暂时异常，请稍后重试');isSending.value=false}
  )
}

const reAsk = (text) => { inputText.value = text || ''; nextTick(() => inputRef.value?.focus()) }

const startTypingEffect = (aiMsg, full) => {
  
  // 将Markdown转为纯文本，打字过程只显示纯文本，不暴露Markdown符号
  const plain = markdownToPlainText(full);
  aiMsg._plainContent = plain;
  
  // 如果定时器已存在，说明已经在打字，只需更新目标即可
  if (typingTimer) {
    return;
  }
  
  // 立即开始打字，不要等待
  const tick = () => {
    const current = aiMsg.displayContent?.length || 0;
    const target = aiMsg._plainContent?.length || 0;
    const remaining = target - current;
    
    if (remaining <= 0) {
      // 打字完成，直接用纯文本，不做任何Markdown转换（避免闪烁）
      aiMsg.displayContent = aiMsg._plainContent || '';
      aiMsg._plainContent = null;
      typingTimer = null;
      // 打字完成后也滚动到底部
      scrollToBottom();
      return;
    }
    
    // 根据剩余内容调整速度
    const speed = remaining > 1000 ? 5 : remaining > 500 ? 10 : 20;
    const step = remaining > 1000 ? 3 : remaining > 500 ? 2 : 1;
    
    // 显示下一个字符（纯文本，无Markdown符号）
    const partialText = (aiMsg._plainContent || '').substring(0, current + step);
    aiMsg.displayContent = partialText;
    
    // 打字过程中自动滚动到底部
    scrollToBottom();
    
    // 强制触发 Vue 更新
    nextTick();
    
    typingTimer = setTimeout(tick, speed);
  };
  
  // 立即执行一次
  tick();
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
  const aiMsg=reactive({id:Date.now()+1,role:'assistant',question:t,content:'',displayContent:'',status:'loading',errorMessage:'',mode:currentMode.value,context:buildContext()})
  messages.value.push({id:Date.now(),role:'user',question:'[图片提问]',image:uploadedImage.value},aiMsg); inputText.value=''
  const authStore=useAuthStore();const userId=authStore.userId
  closeSSE(); stopTyping()
  let fullContent = ''; let hasError = false
  
  try {
    const fd = new FormData()
    fd.append('question', t)
    if (userId) fd.append('userId', userId)
    const blob = await fetch(uploadedImage.value).then(r => r.blob())
    fd.append('image', blob, 'question.jpg')
    
    streamingApiService.stream.askImageQuestionStream(
      fd,
      (msg) => {
        if (msg.error) {
          hasError = true
          aiMsg.status = 'error'
          aiMsg.errorMessage = msg.error || '图片识别失败，请重试'
          isSending.value = false
          return
        }
        if (msg.content) {
          fullContent += msg.content
          aiMsg.status = 'typing'
          aiMsg.displayContent = fullContent.substring(0, (aiMsg.displayContent?.length || 0) + 1)
          if (!typingTimer) {
            startTypingEffect(aiMsg, fullContent)
          }
        }
      },
      () => {
        if (!hasError) {
          stopTyping()
          aiMsg.content = fullContent
          aiMsg.displayContent = markdownToPlainText(fullContent)
          aiMsg.status = 'success'
        }
        isSending.value = false
      },
      (e) => {
        stopTyping()
        aiMsg.status = 'error'
        const em = e?.message || ''
        aiMsg.errorMessage = (em.includes('500') || em.includes('服务器')) ? '服务暂时异常，请稍后重试' : (em.includes('超时') || em.includes('timeout')) ? '请求超时，请检查网络后重试' : (em || '图片识别失败，请重试')
        isSending.value = false
      }
    )
  } catch(e) {
    stopTyping()
    aiMsg.status = 'error'
    aiMsg.errorMessage = e?.message || e?.errorMessage || '图片上传失败，请重试'
    isSending.value = false
  } finally {
    uploadedImage.value = null
    questionType.value = 'text'
  }
}

/* ========== 考研 ========== */
const examSubjects=ref([]);const selectedExamSubject=ref('');const subjectSearchQuery=ref('');const showSubjectDropdown=ref(false)
const keyPoints=ref([]);const selectedKeyPoint=ref(null);const selectedPapers=ref([])
const filteredSubjects=computed(()=>{const q=subjectSearchQuery.value.toLowerCase();return q?examSubjects.value.filter(s=>s.name.toLowerCase().includes(q)):examSubjects.value})
const fetchExamSubjects=async()=>{try{const r=await apiService.qa.getExamSubjects();if(r&&r.data){examSubjects.value=r.data;if(r.data.length){selectedExamSubject.value=r.data[0].id;await fetchKeyPoints(r.data[0].id);await fetchExamPapers(r.data[0].id)}}}catch(e){}}
const getSelectedSubjectName=()=>{const s=examSubjects.value.find(x=>x.id===selectedExamSubject.value);return s?s.name:'选择科目'}
const fetchKeyPoints=async(sid)=>{try{const r=await apiService.qa.getKeyPoints(sid);if(r&&r.data)keyPoints.value=r.data}catch(e){}}
const fetchExamPapers=async(sid)=>{try{const r=await apiService.qa.getExamPapers(sid);if(r&&r.data)selectedPapers.value=r.data}catch(e){}}
const selectKeyPoint=(p)=>{selectedKeyPoint.value=p;sendQuestion('请详细讲解：'+p.name)}
const askAiPredict=()=>sendQuestion(`请基于${getSelectedSubjectName()}的历年真题趋势，分析今年的高频考点和预测方向`)

const goHome = () => router.push('/')

onMounted(async () => { 
  if(route.query.question) inputText.value=route.query.question
  
  // 刷新后从localStorage恢复会话ID，保持AI上下文连续性
  const storedSessionId = getStoredSessionId()
  if (storedSessionId && isValidSessionId(storedSessionId)) {
    aiSessionId.value = storedSessionId
  }
  
  await fetchBooks()
  await fetchExamSubjects()
  await fetchBackendHistory()
})
onUnmounted(() => { closeSSE(); stopTyping() })

const processMathText = (t) => processAiResponse((t||'').replace(/\n{2,}/g,'\n'))
</script>

<style scoped>
.qa-page {
  --c: #6366f1; --c2: #a78bfa; --c3: #c4b5fd;
  --border: rgba(220,210,245,.5); --border2: rgba(200,185,240,.4);
  --text: #1e293b; --text2: #475569; --text3: #94a3b8;
  height: calc(100vh - 60px);
  display: flex; flex-direction: column; overflow: hidden;
  background: linear-gradient(175deg, #fffdfd 0%, #f9f6ff 34%, #f8fafd 64%, #fdfaff 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
  color: var(--text); position: relative;
}
.qp-bg { position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image: radial-gradient(circle at 8% 12%, rgba(167,139,250,.10), transparent 26%), radial-gradient(circle at 92% 75%, rgba(99,102,241,.06), transparent 28%);
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
.qasm-btn.active { background: #ede4fe; border-color: var(--c); box-shadow: 0 0 0 2px rgba(99,102,241,.06); }
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
.qas-search:focus-within { border-color:var(--c);background:#fff;box-shadow:0 0 0 3px rgba(99,102,241,.05); }
.qas-search svg { color:var(--text3);flex-shrink:0; }
.qas-search input { flex:1;border:none;outline:none;background:transparent;font-size:12px;color:var(--text);font-family:inherit; }
.qas-search input::placeholder { color:#cbd5e1; }

/* 搜索入口 */
.qas-search-wrap { margin-bottom: 10px; }
.qas-search-wrap .qas-search { cursor: pointer; }
.qas-search-placeholder { font-size: 12px; color: #cbd5e1; flex: 1; }

/* 上下文栏 */
.qas-ctx { display:flex;align-items:center;gap:10px;padding:10px 12px;border-radius:12px;margin-bottom:8px;background:linear-gradient(135deg,rgba(99,102,241,.08),rgba(167,139,250,.04));border:1.5px solid rgba(167,139,250,.18);animation:ctxIn .3s ease-out; }
@keyframes ctxIn { from{opacity:0;transform:translateY(-4px)} to{opacity:1;transform:translateY(0)} }
.qas-ctx-left { display:flex;align-items:center;gap:9px;flex:1;min-width:0; }
.qas-ctx-icon { font-size:20px; }
.qas-ctx-info { display:flex;flex-direction:column;gap:0;min-width:0; }
.qas-ctx-name { font-size:12px;font-weight:700;color:var(--text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.qas-ctx-pub { font-size:10px;color:var(--text3);margin-top:1px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.qas-ctx-actions { display:flex;gap:6px;flex-shrink:0; }
.qas-ctx-switch { font-size:10px;padding:4px 10px;border-radius:6px;border:1px solid rgba(167,139,250,.2);background:rgba(255,255,255,.6);color:var(--c);cursor:pointer;transition:all .15s;white-space:nowrap;font-family:inherit; }
.qas-ctx-switch:hover { background:rgba(99,102,241,.08);border-color:var(--c2); }
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
.qas-panel-input::placeholder { color:#cbd5e1; }
.qas-panel-esc { font-size:9px;font-weight:600;color:#cbd5e1;padding:2px 7px;border-radius:4px;border:1px solid rgba(200,185,240,.2); }

.qas-panel-body { flex:1;overflow-y:auto;padding:6px 12px 14px; }
.qas-panel-body::-webkit-scrollbar { width:3px; }
.qas-panel-body::-webkit-scrollbar-thumb { background:rgba(180,165,220,.2);border-radius:3px; }

.qas-panel-sec { padding:4px 2px; }
.qas-panel-sec + .qas-panel-sec { border-top:1px solid rgba(200,190,230,.08);margin-top:4px;padding-top:10px; }

.qas-panel-sec-head { font-size:10px;font-weight:700;color:var(--text3);text-transform:uppercase;letter-spacing:.4px;padding:4px 6px 6px; }
.qas-panel-count { font-size:9px;padding:1px 6px;border-radius:5px;background:rgba(99,102,241,.07);color:var(--c); }

.qas-panel-chips { display:flex;gap:6px;flex-wrap:wrap;padding:0 4px; }
.qas-panel-chip { padding:5px 12px;border-radius:8px;border:1px solid rgba(200,185,240,.2);background:rgba(255,255,255,.5);font-size:11px;font-family:inherit;color:var(--text2);cursor:pointer;transition:all .15s; }
.qas-panel-chip:hover { border-color:var(--c2);background:rgba(255,255,255,.8);transform:translateY(-1px); }

.qas-panel-item { display:flex;align-items:center;gap:10px;width:100%;padding:8px 10px;border-radius:9px;border:none;background:transparent;cursor:pointer;font-family:inherit;transition:all .15s;text-align:left;animation:qaspiIn .25s ease-out both; }
@keyframes qaspiIn { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
.qas-panel-item:hover { background:rgba(245,240,255,.6); }
.qas-panel-item.on { background:rgba(99,102,241,.05);border:1px solid rgba(167,139,250,.2); }

.qas-panel-item-cover { width:30px;height:40px;border-radius:4px;flex-shrink:0;background:linear-gradient(140deg,#ede4fe,#ddd2f8);display:flex;align-items:center;justify-content:center;font-size:16px; }

.qas-panel-item-info { flex:1;min-width:0;display:flex;flex-direction:column;gap:1px; }
.qas-panel-item-name { font-size:12px;font-weight:600;color:var(--text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.qas-panel-item-meta { font-size:10px;color:var(--text3);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }

.qas-panel-item-ctx { font-size:9px;font-weight:700;color:var(--c);padding:2px 7px;border-radius:6px;background:rgba(99,102,241,.07);flex-shrink:0; }
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

.qash-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px 16px; text-align: center; }
.qashe-icon { font-size: 32px; margin-bottom: 10px; opacity: .6; }
.qashe-text { font-size: 12px; color: var(--text3); margin-bottom: 12px; }
.qashe-new-btn { padding: 6px 16px; border-radius: 8px; border: 1.5px solid var(--c3); background: #fff; color: var(--c); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; }
.qashe-new-btn:hover { background: #ede4fe; border-color: var(--c); }

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
.qamec-card:hover { border-color: var(--c3); background: #fcfaff; transform: translateY(-2px); box-shadow: 0 6px 24px rgba(99,102,241,.08); }
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
.qam-bubble.user { background: linear-gradient(135deg, #6366f1, #9b7cff); color: #fff; border-radius: 18px 18px 4px 18px; display: flex; flex-direction: column; gap: 8px; }
.qam-bubble.ai { background: #fff; color: #1f2937; border: 1px solid #ebe4ff; border-radius: 18px 18px 18px 4px; box-shadow: 0 8px 24px rgba(99,102,241,.08); }
.qam-image { max-width: 300px; max-height: 200px; border-radius: 8px; object-fit: cover; }
.qam-avatar { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; background: linear-gradient(135deg, #6366f1, #a78bfa); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px; }
.qam-ai-wrap { display: flex; flex-direction: column; gap: 8px; max-width: min(680px, 72%); min-width: 0; }

.qam-loading { display: flex; align-items: center; gap: 5px; font-size: 12px; color: var(--text3); white-space: nowrap; }
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
.qameb-retry { background: linear-gradient(135deg, #6366f1, #9b7cff); color: #fff; }
.qameb-retry:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,.35); }
.qameb-reask { background: #f5f0ff; color: var(--c); border: 1.5px solid rgba(200,185,240,.4) !important; }
.qameb-reask:hover { background: #ede4fe; }
button:disabled { opacity: .5; cursor: not-allowed; }

.qam-typing { position: relative; }
.qam-typing pre {
  margin: 0;
  padding: 0;
  background: transparent;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}
.qam-typing .katex {
  font-size: 1.1em;
}
.qam-typing .katex-display {
  margin: 0.5em 0;
}
.qam-cursor { display: inline-block; animation: blink 1s infinite; color: var(--c); font-weight: 100; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
.qam-html { 
  line-height: 1.75; 
  width: 100%; 
  word-break: break-word; 
  overflow-wrap: anywhere; 
  font-size: 14px; 
  color: var(--text);
  /* 强制所有子元素继承字体大小，防止浏览器默认样式干扰 */
  * {
    font-size: inherit;
    line-height: inherit;
  }
}
.qam-html :deep(p) { margin: 0.3em 0; word-break: break-word; overflow-wrap: anywhere; font-size: 14px; line-height: 1.8; }
.qam-html :deep(h1) { margin: 0.8em 0 0.4em; font-weight: 700; word-break: break-word; line-height: 1.35; font-size: 22px; }
.qam-html :deep(h2) { margin: 0.7em 0 0.35em; font-weight: 700; word-break: break-word; line-height: 1.35; font-size: 19px; }
.qam-html :deep(h3) { margin: 0.6em 0 0.3em; font-weight: 700; word-break: break-word; line-height: 1.4; font-size: 16px; }
.qam-html :deep(h4) { margin: 0.5em 0 0.25em; font-weight: 600; word-break: break-word; line-height: 1.4; font-size: 15px; }
.qam-html :deep(h5),.qam-html :deep(h6) { margin: 0.5em 0 0.25em; font-weight: 600; word-break: break-word; line-height: 1.4; font-size: 14px; }
.qam-html :deep(pre) { background: #f5f0fc; border-radius: 10px; padding: 12px; overflow-x: auto; font-size: 13px; max-width: 100%; word-break: break-all; white-space: pre-wrap; }
.qam-html :deep(code) { font-family: monospace; font-size: 13px; word-break: break-all; overflow-wrap: anywhere; background: rgba(167,139,250,0.08); padding: 2px 5px; border-radius: 4px; }
.qam-html :deep(pre code) { background: transparent; padding: 0; font-size: inherit; }
.qam-html :deep(ul),.qam-html :deep(ol) { padding-left: 1.5em; word-break: break-word; margin: 0.3em 0; font-size: 14px; }
.qam-html :deep(li) { margin: 0.2em 0; line-height: 1.7; font-size: 14px; }
.qam-html :deep(li p) { margin: 0.2em 0; font-size: 14px; }
.qam-html :deep(li ul),.qam-html :deep(li ol) { font-size: 14px; }
.qam-html :deep(blockquote) { border-left: 3px solid var(--c3); padding-left: 1em; color: var(--text2); word-break: break-word; margin: 0.5em 0; background: rgba(167,139,250,0.03); padding: 0.5em 1em; border-radius: 0 8px 8px 0; font-size: 14px; }
.qam-html :deep(strong),.qam-html :deep(b) { font-weight: 700; color: var(--text); font-size: inherit; }
.qam-html :deep(em),.qam-html :deep(i) { font-style: italic; font-size: inherit; }
.qam-html :deep(a) { color: var(--c); text-decoration: underline; font-size: inherit; }
.qam-html :deep(table) { font-size: 14px; }
.qam-html :deep(th),.qam-html :deep(td) { font-size: 14px; }
.qam-html :deep(hr) { border: none; height: 1px; background: var(--border); margin: 1em 0; }
.qam-html :deep(div) { font-size: inherit; }

/* KaTeX内联公式样式 */
.qam-html :deep(.katex) { font-size: 1em; }
.qam-html :deep(.katex-display) { margin: 12px 0; overflow-x: auto; overflow-y: hidden; max-width: 100%; display: block; }
.qam-html :deep(.katex-display > .katex) { display: inline-block; white-space: nowrap; }
.qam-html :deep(table) { width: 100%; border-collapse: collapse; overflow-x: auto; display: block; }
.qam-html :deep(th),.qam-html :deep(td) { border: 1px solid var(--border); padding: 6px 8px; word-break: break-word; }
.qam-html :deep(th) { background: rgba(99,102,241,.05); font-weight: 600; }
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
.qami-bar:focus-within { border-color: var(--c); box-shadow: 0 2px 32px rgba(99,102,241,.1), 0 0 0 4px rgba(99,102,241,.04); }
.qamib-img { width: 34px; height: 34px; border-radius: 9px; border: none; background: transparent; color: var(--text3); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .15s; flex-shrink: 0; }
.qamib-img:hover { color: var(--c); background: #f5f0ff; }
.qamib-img.active { color: var(--c); background: #ede4fe; }
.qamib-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: var(--text); padding: 9px 4px; font-family: inherit; }
.qamib-input::placeholder { color: #c0b8d0; }

.qami-ctx-tag { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; color: var(--c); background: rgba(99,102,241,.06); border: 1px solid rgba(167,139,250,.18); cursor: pointer; white-space: nowrap; flex-shrink: 0; transition: all .2s; animation: ctxTagIn .3s ease-out; }
@keyframes ctxTagIn { from{opacity:0;transform:scale(.9)} to{opacity:1;transform:scale(1)} }
.qami-ctx-tag:hover { background: rgba(99,102,241,.1); border-color: var(--c2); transform: translateY(-1px); }
.qamib-send { width: 38px; height: 38px; border-radius: 50%; border: none; background: #f0ecf8; color: #c0b8d0; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .25s; flex-shrink: 0; }
.qamib-send.ready { background: linear-gradient(135deg, #6366f1, #a78bfa); color: #fff; }
.qamib-send.ready:hover:not(:disabled) { transform: scale(1.06); box-shadow: 0 4px 16px rgba(99,102,241,.3); }
.qamib-send:disabled { cursor: not-allowed; opacity: .5; }
.qamib-send.loading { background: var(--c); }
.qamib-spin { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,.3); border-top-color: #fff; border-radius: 50%; animation: spin .6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 滚动条样式 */
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
