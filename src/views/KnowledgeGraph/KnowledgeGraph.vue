<template>
  <div class="kg-root">
    <div class="kg-page" :class="{ 'kg-page--dimmed': showBookPanel }">
      <div class="kg-canvas-wrap">
        <span v-for="i in 14" :key="'p'+i" class="kg-particle" :style="particleStyle(i)" />

        <div class="kg-hero">
          <button class="kg-home-btn" @click="goToHome">← 返回首页</button>
          <h1 class="kg-hero-title">知识图谱</h1>
          <p class="kg-hero-sub">{{ currentBook ? currentBook.title : '探索学科知识关联' }}</p>
        </div>

        <div class="kg-toolbar">
          <button class="kg-tbtn kg-tbtn--primary" @click="openBookPanel">📖 选书</button>
          <button class="kg-tbtn" @click="simpleMode = !simpleMode">{{ simpleMode ? '展开全部' : '简化视图' }}</button>
          <button class="kg-tbtn" @click="exportPDF">📄 导出PDF</button>
        </div>

        <svg class="kg-svg" :viewBox="`-440 -340 880 680`" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <radialGradient id="coreGlow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stop-color="#7c5cfc" stop-opacity="0.3" />
              <stop offset="60%" stop-color="#7c5cfc" stop-opacity="0.06" />
              <stop offset="100%" stop-color="#7c5cfc" stop-opacity="0" />
            </radialGradient>
            <filter id="nodeGlow">
              <feDropShadow dx="0" dy="0" stdDeviation="4" flood-color="#7c5cfc" flood-opacity="0.35" />
            </filter>
            <filter id="nodeGlowStrong">
              <feDropShadow dx="0" dy="0" stdDeviation="8" flood-color="#7c5cfc" flood-opacity="0.5" />
            </filter>
          </defs>

          <circle v-for="r in orbitRadii" :key="'orb'+r" cx="0" cy="0" :r="r"
            fill="none" stroke="rgba(167,139,250,0.18)" stroke-width="1.5" stroke-dasharray="8 8" opacity="0.6" />

          <circle cx="0" cy="0" r="100" fill="url(#coreGlow)" class="kg-core-glow" />

          <g v-for="(l, li) in visibleLinks" :key="'l'+graphKey+'-'+li">
            <line :x1="l.x1" :y1="l.y1" :x2="l.x2" :y2="l.y2"
              :stroke="l.active ? 'rgba(167,139,250,0.55)' : 'rgba(160,140,220,0.22)'"
              :stroke-width="l.active ? 2 : 1.2"
              stroke-linecap="round"
              :style="{ animationDelay: (li * 0.03) + 's' }"
              class="kg-link-line" />
            <circle v-if="l.active" r="3" fill="#7c5cfc" class="kg-flow-dot">
              <animateMotion :dur="1.5 + Math.random() + 's'" repeatCount="indefinite"
                :path="`M${l.x1},${l.y1} L${l.x2},${l.y2}`" />
            </circle>
          </g>

          <g v-for="(n, ni) in visibleNodes" :key="'n'+graphKey+'-'+n.id"
            :transform="`translate(${n.x}, ${n.y})`"
            class="kg-node-group"
            :class="{ active: n.id === activeNodeId, dim: dimmed && n.id !== activeNodeId && !n.connected }"
            :style="{ animationDelay: (ni * 0.04) + 's' }"
            @click="tapNode(n)">
            <circle v-if="n.id === activeNodeId" :r="n.r + 14" fill="url(#coreGlow)" class="kg-glow-ring" />
            <circle v-if="n.id === activeNodeId" :r="n.r + 6" fill="none" stroke="#7c5cfc" stroke-width="2" opacity="0.5"
              stroke-dasharray="6 3" class="kg-select-ring" />
            <circle :r="n.r" :fill="nodeFill(n)" :stroke="n.id === activeNodeId ? '#fff' : 'rgba(255,255,255,.6)'"
              :stroke-width="n.id === activeNodeId ? 3 : 1.5"
              :filter="n.id === activeNodeId ? 'url(#nodeGlowStrong)' : 'url(#nodeGlow)'" />
            <text y="1" text-anchor="middle" :font-size="n.fontSize" :font-weight="n.bold ? 700 : 500"
              :fill="n.id === activeNodeId || n.category <= 1 ? '#fff' : '#4a3f5c'" class="kg-node-label">
              {{ n.label }}
            </text>
            <title>{{ n.name }}</title>
          </g>
        </svg>

        <div class="kg-legend">
          <span class="leg-dot" style="background:#7c5cfc"></span>核心
          <span class="leg-dot" style="background:#a78bfa"></span>模块
          <span class="leg-dot" style="background:#c4b5fd"></span>知识点
          <span class="leg-dot" style="background:#e0d6f8"></span>细分
        </div>
      </div>

      <div class="kg-side">
        <div class="ks-hero">
          <div class="ks-big">{{ activeNodeName }}</div>
          <div class="ks-tag-row">
            <span class="ks-lv">{{ activeNodeLevel }}</span>
            <span class="ks-pct">密度 {{ densityPct }}%</span>
          </div>
          <div class="ks-bar"><div class="ks-bar-fill" :style="{ width: densityPct + '%' }" /></div>
        </div>

        <div class="ks-card">
          <div class="ks-label">⭐ 关联节点</div>
          <div class="ks-tags">
            <span v-for="n in connectedNodes" :key="n.id" class="ks-tag" @click="tapNode(n)">{{ n.name }}</span>
            <span v-if="connectedNodes.length === 0" class="ks-none">点击图谱节点查看</span>
          </div>
        </div>

        <div class="ks-card">
          <div class="ks-label">📋 知识点</div>
          <ul class="ks-list">
            <li v-for="(k, i) in nodeFacts" :key="i">{{ k }}</li>
          </ul>
        </div>

        <button class="ks-btn" @click="openExerciseModal">📚 推荐关联习题</button>
      </div>
    </div>

    <!-- ================================================
         教材浏览器 — 独立浮层，不受背景 filter 影响
         ================================================ -->
    <div v-if="showBookPanel" class="sb-overlay" @click="showBookPanel = false">
      <div class="sb-card" @click.stop>
        <!-- Header -->
        <div class="sb-head">
          <div class="sb-head-l">
            <h2 class="sb-head-title">选择教材</h2>
            <p class="sb-head-sub">切换知识图谱展示内容</p>
          </div>
          <div class="sb-head-search">
            <svg class="sb-head-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <input ref="searchInputRef" v-model="bookSearch" placeholder="搜索教材 / 出版社 / 作者..." class="sb-head-search-input" @focus="searchFocused = true" @blur="searchFocused = false" />
            <button v-if="bookSearch" class="sb-head-search-clear" @mousedown.prevent @click="bookSearch = ''; searchInputRef?.focus()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>

            <!-- Spotlight 浮层 -->
            <div v-if="searchFocused" class="sp-drop" @mousedown.prevent>
              <div v-if="!bookSearch.trim()" class="sp-default">
                <div class="sp-sec" v-if="hotBooks.length">
                  <div class="sp-sec-head">🔥 热门教材</div>
                  <div class="sp-row">
                    <button v-for="b in hotBooks.slice(0, 5)" :key="'h'+b.id" class="sp-chip" @click="onSearchSelect(b)">{{ b.coverIcon || '📘' }} {{ b.title }}</button>
                  </div>
                </div>
                <div class="sp-sec" v-if="aiSearchBooks.length">
                  <div class="sp-sec-head">✨ AI 推荐</div>
                  <button v-for="b in aiSearchBooks.slice(0, 4)" :key="'a'+b.id" class="sp-line" @click="onSearchSelect(b)">
                    <span class="sp-line-cover">{{ b.coverIcon || '📘' }}</span>
                    <span class="sp-line-name">{{ b.title }}</span>
                    <span class="sp-line-extra">{{ b.direction }}</span>
                    <span class="sp-line-pct">{{ b.matchRate }}%</span>
                  </button>
                </div>
              </div>
              <div v-else-if="searchAllResults.length">
                <div class="sp-sec-head">搜索结果 <span class="sp-count">{{ searchAllResults.length }}</span></div>
                <button v-for="(b, i) in searchAllResults" :key="'r'+i" class="sp-line" :style="{ animationDelay: i * 0.03 + 's' }" @click="onSearchSelect(b)">
                  <span class="sp-line-cover">{{ b.coverIcon || '📘' }}</span>
                  <span class="sp-line-name">{{ b.title }}</span>
                  <span class="sp-line-extra">{{ b.publisher }}</span>
                  <span v-if="b.inShelf" class="sp-line-shelf">书架</span>
                  <span v-else class="sp-line-pct">{{ b.matchRate || 80 }}%</span>
                </button>
              </div>
              <div v-else class="sp-none">📡 未找到 "{{ bookSearch }}"</div>
            </div>
          </div>
          <button class="sb-head-close" @click="showBookPanel = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
        </div>

        <!-- Body -->
        <div class="sb-body">
          <!-- 我的教材 -->
          <div class="sb-section" v-if="myBooks.length">
            <div class="sb-section-head">
              <span class="sb-section-icon">📚</span><span>我的教材</span><span class="sb-section-count">{{ myBooks.length }}</span>
            </div>
            <div class="sb-section-list">
              <div v-for="(b, i) in myBooks" :key="b.id"
                class="sb-book"
                :class="{
                  active: currentBook?.id === b.id,
                  pending: pendingBook?.id === b.id && pendingBook?.id !== currentBook?.id
                }"
                :style="{ animationDelay: (0.03 + i * 0.04) + 's' }"
                @click="selectBook(b)">
                <div class="sb-book-shine"></div>
                <div class="sb-book-cover"><span>{{ b.coverIcon || '📘' }}</span></div>
                <div class="sb-book-info">
                  <div class="sb-book-name">{{ b.title }}</div>
                  <div class="sb-book-publisher">{{ b.publisher || '高等教育出版社' }}</div>
                  <div class="sb-book-tags">
                    <span class="sb-book-tag">{{ b.direction || '通用' }}</span>
                    <span class="sb-book-tag sb-book-tag--nodes">{{ b.nodeCount || 24 }} 节点</span>
                  </div>
                </div>
                <div class="sb-book-status">
                  <span v-if="pendingBook?.id === b.id && pendingBook?.id !== currentBook?.id" class="sb-book-pending-tag">待切换</span>
                  <span v-else-if="currentBook?.id === b.id" class="sb-book-active-tag">
                    <span class="sb-book-active-dot"></span>当前展示中
                  </span>
                  <span v-else-if="b.aiRecommended" class="sb-book-match">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l2.4 7.2h7.6l-6 4.8 2.4 7.2-6.4-4.8-6.4 4.8 2.4-7.2-6-4.8h7.6z"/></svg>
                    {{ b.matchRate }}%
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 平台教材 -->
          <div class="sb-section" v-if="platformBooks.length">
            <div class="sb-section-head sb-section-head--platform">
              <span class="sb-section-icon">🌐</span><span>{{ bookSearch ? '搜索结果' : '发现更多教材' }}</span><span class="sb-section-count">{{ platformBooks.length }}</span>
            </div>
            <div class="sb-section-list">
              <div v-for="(b, i) in platformBooks" :key="'p'+i"
                class="sb-book sb-book--light"
                :class="{
                  active: currentBook?.id === b.id,
                  pending: pendingBook?.id === b.id && pendingBook?.id !== currentBook?.id
                }"
                :style="{ animationDelay: (0.08 + i * 0.04) + 's' }"
                @click="selectBook(b)">
                <div class="sb-book-shine"></div>
                <div class="sb-book-cover sb-book-cover--thin"><span>{{ b.coverIcon || '📘' }}</span></div>
                <div class="sb-book-info">
                  <div class="sb-book-name">{{ b.title }}</div>
                  <div class="sb-book-publisher">{{ b.publisher }}</div>
                  <div class="sb-book-tags">
                    <span class="sb-book-tag">{{ b.direction || '通用' }}</span>
                    <span class="sb-book-tag sb-book-tag--hot">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C9.5 2 7 3.5 6 6c-1.5 3.5 0 7 3 9-3-2-4.5-5.5-3-9C7 3.5 9.5 2 12 2z"/></svg>
                      {{ formatStudents(b.students) }}
                    </span>
                    <span v-if="b.supportsGraph" class="sb-book-tag sb-book-tag--graph">支持图谱</span>
                  </div>
                </div>
                <div class="sb-book-status">
                  <span v-if="pendingBook?.id === b.id && currentBook?.id !== b.id" class="sb-book-pending-tag">待切换</span>
                  <span v-else-if="currentBook?.id === b.id" class="sb-book-active-tag">
                    <span class="sb-book-active-dot"></span>当前展示中
                  </span>
                  <span v-else class="sb-book-source">{{ b.source || '系统库' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 空态 -->
          <div v-if="myBooks.length === 0 && platformBooks.length === 0" class="sb-empty">
            <div class="sb-empty-wrap">
              <span class="sb-empty-icon">📡</span>
              <p class="sb-empty-title">未找到相关教材</p>
              <p class="sb-empty-hint">试试其他关键词，或浏览下方推荐</p>
              <div class="sb-empty-suggestions" v-if="suggestedBooks.length">
                <span class="sb-empty-sug-label">热门推荐</span>
                <div class="sb-empty-sug-list">
                  <button v-for="s in suggestedBooks" :key="s.id" class="sb-empty-sug-item" @click="selectBook(s)">
                    <span>{{ s.coverIcon || '📘' }}</span><span>{{ s.title }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部 -->
        <div class="sb-foot">
          <div class="sb-foot-l">
            <span class="sb-foot-label">当前知识图谱</span>
            <span class="sb-foot-name">{{ currentBook ? '《' + currentBook.title + '》' : '未选择' }}</span>
            <span v-if="pendingBook && pendingBook.id !== currentBook?.id" class="sb-foot-arrow">→</span>
            <span v-if="pendingBook && pendingBook.id !== currentBook?.id" class="sb-foot-next">{{ '《' + pendingBook.title + '》' }}</span>
          </div>
          <button class="sb-foot-btn" :class="{ ready: pendingBook && pendingBook.id !== currentBook?.id }" @click="applyBookSwitch">
            <span>{{ pendingBook && pendingBook.id !== currentBook?.id ? '切换知识图谱' : '关闭' }}</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="m9 18 6-6-6-6"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 习题推荐弹窗 -->
    <div v-if="showExerciseModal" class="ex-modal-overlay" @click="showExerciseModal = false">
      <div class="ex-modal" @click.stop>
        <div class="exm-top">
          <div class="exmt-left">
            <div class="exmt-node">🧠 {{ activeNodeName }}</div>
            <div class="exmt-title">AI 根据知识图谱智能推荐练习</div>
            <div class="exmt-meta">
              <span>共推荐 <strong>{{ exercises.length }}</strong> 题</span>
              <span class="exmt-dot">·</span>
              <span>已选 <strong>{{ selectedExCount }}</strong> 题</span>
            </div>
          </div>
          <div class="exmt-right">
            <div class="exmtr-item">📈 预计提升</div>
            <div class="exmtr-tags"><span v-for="(b, i) in benefitTags" :key="i">{{ b }}</span></div>
          </div>
          <button class="exmt-close" @click="showExerciseModal = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="exm-filters">
          <button v-for="f in exFilters" :key="f.k" :class="{ on: exFilter === f.k }" @click="changeExFilter(f.k)">
            <span class="exmf-icon">{{ f.icon }}</span><span>{{ f.label }}</span><span class="exmf-count">{{ getFilterCount(f.k) }}</span>
          </button>
        </div>
        <div class="exm-list">
          <div v-if="filteredExercises.length === 0" class="exml-empty"><span>📭</span><p>该难度暂无推荐题目</p></div>
          <div v-for="e in filteredExercises" :key="e.id" class="exml-card" :class="{ sel: e.selected }" @click="e.selected = !e.selected">
            <div class="exmlc-left"><span class="exmlc-diff" :class="e.diffKey"></span><span class="exmlc-type">{{ e.typeIcon || '📝' }}</span></div>
            <div class="exmlc-mid">
              <div class="exmlcm-title">{{ e.title }}</div>
              <div class="exmlcm-tags" v-if="e.tags?.length"><span v-for="t in e.tags" :key="t">{{ t }}</span></div>
              <div class="exmlcm-reason" v-if="e.aiReason"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>{{ e.aiReason }}</div>
            </div>
            <div class="exmlc-right">
              <div class="exmlcr-meta"><span class="exmlcr-time">⏱ {{ e.estTime || '8' }}min</span><span class="exmlcr-exp">+{{ e.expGain || '20' }}EXP</span></div>
              <div class="exmlcr-check" :class="{ on: e.selected }"><svg v-if="e.selected" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg></div>
            </div>
          </div>
        </div>
        <div class="exm-foot">
          <div class="exmf-left"><span>已选 <strong>{{ selectedExCount }}</strong> 题</span><span class="exmf-dot">·</span><span>预计 <strong>{{ totalEstTime }}</strong>min</span><span class="exmf-dot">·</span><span>获得 <strong>+{{ totalExp }}</strong> EXP</span></div>
          <div class="exmf-right">
            <button class="exmfr-refresh" @click="refreshRecommend" :disabled="isRefreshing"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>{{ isRefreshing ? '推荐中...' : '重新推荐' }}</button>
            <button class="exmfr-start" @click="startEx" :disabled="selectedExCount === 0">开始练习<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m9 18 6-6-6-6"/></svg></button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import api from '@/api/api.js'

const router = useRouter()
const goToHome = () => router.push('/')

const activeNodeId = ref('0')
const simpleMode = ref(false)
const dimmed = ref(false)
const showBookPanel = ref(false)
const bookshelfBooks = ref([])
const currentBook = ref(null)
const pendingBook = ref(null)
const graphKey = ref(0)
const showExerciseModal = ref(false)
const exFilter = ref('all')
const exercises = ref([])
const exFilters = [
  { k: 'all', label: '全部', icon: '📋' }, { k: 'basic', label: '基础', icon: '🟢' },
  { k: 'advanced', label: '进阶', icon: '🟡' }, { k: 'exam', label: '真题', icon: '🔴' }, { k: 'competition', label: '冲刺', icon: '⚡' }
]

const isRefreshing = ref(false)
const bookSearch = ref('')
const searchFocused = ref(false)
const searchInputRef = ref(null)

const graphData = reactive({
  nodes: [
    { id: '0', name: '高等数学', category: 0 },
    { id: '1', name: '微积分', category: 1 }, { id: '2', name: '线性代数', category: 1 },
    { id: '3', name: '概率论', category: 1 }, { id: '4', name: '常微分方程', category: 1 },
    { id: '5', name: '导数与微分', category: 2 }, { id: '6', name: '不定积分', category: 2 },
    { id: '7', name: '极限与连续', category: 2 }, { id: '8', name: '矩阵运算', category: 2 },
    { id: '9', name: '行列式', category: 2 }, { id: '10', name: '随机变量', category: 2 },
    { id: '11', name: '定积分', category: 3 }, { id: '12', name: '特征向量', category: 3 },
    { id: '13', name: '数学期望', category: 3 }, { id: '14', name: '齐次方程', category: 3 },
    { id: '15', name: '非齐次方程', category: 3 }, { id: '16', name: '中值定理', category: 2 },
    { id: '17', name: '泰勒公式', category: 3 }, { id: '18', name: '向量空间', category: 2 },
  ],
  links: [
    ['0','1'],['0','2'],['0','3'],['0','4'],
    ['1','5'],['1','6'],['1','7'],['1','16'],
    ['2','8'],['2','9'],['2','18'],
    ['3','10'],['3','13'],
    ['4','14'],['4','15'],
    ['5','11'],['8','12'],['16','17'],
  ]
})

/* ===== 教材→图谱 预设数据 ===== */
const bookGraphPresets = {
  '高等数学': {
    nodes: [
      { id: '0', name: '高等数学', category: 0 },
      { id: '1', name: '微积分', category: 1 }, { id: '2', name: '线性代数', category: 1 },
      { id: '3', name: '概率论', category: 1 }, { id: '4', name: '常微分方程', category: 1 },
      { id: '5', name: '导数与微分', category: 2 }, { id: '6', name: '不定积分', category: 2 },
      { id: '7', name: '极限与连续', category: 2 }, { id: '8', name: '矩阵运算', category: 2 },
      { id: '9', name: '行列式', category: 2 }, { id: '10', name: '随机变量', category: 2 },
      { id: '11', name: '定积分', category: 3 }, { id: '12', name: '特征向量', category: 3 },
      { id: '13', name: '数学期望', category: 3 }, { id: '14', name: '齐次方程', category: 3 },
      { id: '15', name: '非齐次方程', category: 3 }, { id: '16', name: '中值定理', category: 2 },
      { id: '17', name: '泰勒公式', category: 3 }, { id: '18', name: '向量空间', category: 2 },
    ],
    links: [
      ['0','1'],['0','2'],['0','3'],['0','4'],['1','5'],['1','6'],['1','7'],['1','16'],
      ['2','8'],['2','9'],['2','18'],['3','10'],['3','13'],['4','14'],['4','15'],
      ['5','11'],['8','12'],['16','17'],
    ]
  },
  '线性代数': {
    nodes: [
      { id: '0', name: '线性代数', category: 0 },
      { id: '1', name: '矩阵理论', category: 1 }, { id: '2', name: '向量空间', category: 1 },
      { id: '3', name: '线性变换', category: 1 }, { id: '4', name: '特征值', category: 1 },
      { id: '5', name: '矩阵运算', category: 2 }, { id: '6', name: '行列式', category: 2 },
      { id: '7', name: '逆矩阵', category: 2 }, { id: '8', name: '线性相关', category: 2 },
      { id: '9', name: '基与维数', category: 2 }, { id: '10', name: '正交变换', category: 2 },
      { id: '11', name: '矩阵分解', category: 3 }, { id: '12', name: '子空间', category: 3 },
      { id: '13', name: '特征向量', category: 3 }, { id: '14', name: '对角化', category: 3 },
      { id: '15', name: '二次型', category: 3 }, { id: '16', name: '秩与零度', category: 2 },
      { id: '17', name: '最小二乘', category: 3 }, { id: '18', name: '线性方程组', category: 2 },
    ],
    links: [
      ['0','1'],['0','2'],['0','3'],['0','4'],['1','5'],['1','6'],['1','7'],
      ['2','8'],['2','9'],['2','18'],['3','10'],['3','14'],['4','12'],['4','13'],
      ['5','11'],['5','16'],['10','15'],['13','17'],
    ]
  },
  '概率论': {
    nodes: [
      { id: '0', name: '概率论', category: 0 },
      { id: '1', name: '随机事件', category: 1 }, { id: '2', name: '随机变量', category: 1 },
      { id: '3', name: '数字特征', category: 1 }, { id: '4', name: '大数定律', category: 1 },
      { id: '5', name: '条件概率', category: 2 }, { id: '6', name: '分布函数', category: 2 },
      { id: '7', name: '贝叶斯公式', category: 2 }, { id: '8', name: '数学期望', category: 2 },
      { id: '9', name: '方差', category: 2 }, { id: '10', name: '协方差', category: 2 },
      { id: '11', name: '正态分布', category: 3 }, { id: '12', name: '二项分布', category: 3 },
      { id: '13', name: '泊松分布', category: 3 }, { id: '14', name: '指数分布', category: 3 },
      { id: '15', name: '中心极限', category: 3 }, { id: '16', name: '全概率', category: 2 },
      { id: '17', name: '切比雪夫', category: 3 }, { id: '18', name: '矩估计', category: 2 },
    ],
    links: [
      ['0','1'],['0','2'],['0','3'],['0','4'],['1','5'],['1','7'],['1','16'],
      ['2','6'],['2','11'],['3','8'],['3','9'],['3','10'],['4','15'],['4','17'],
      ['5','12'],['6','13'],['8','14'],['9','18'],
    ]
  },
  '数据结构': {
    nodes: [
      { id: '0', name: '数据结构', category: 0 },
      { id: '1', name: '线性结构', category: 1 }, { id: '2', name: '树形结构', category: 1 },
      { id: '3', name: '图形结构', category: 1 }, { id: '4', name: '查找排序', category: 1 },
      { id: '5', name: '栈与队列', category: 2 }, { id: '6', name: '链表', category: 2 },
      { id: '7', name: '二叉树', category: 2 }, { id: '8', name: '图遍历', category: 2 },
      { id: '9', name: '哈希表', category: 2 }, { id: '10', name: '排序算法', category: 2 },
      { id: '11', name: '循环队列', category: 3 }, { id: '12', name: '双向链表', category: 3 },
      { id: '13', name: '平衡树', category: 3 }, { id: '14', name: '最短路径', category: 3 },
      { id: '15', name: '拓扑排序', category: 3 }, { id: '16', name: '堆结构', category: 2 },
      { id: '17', name: 'B+树', category: 3 }, { id: '18', name: '字符串匹配', category: 2 },
    ],
    links: [
      ['0','1'],['0','2'],['0','3'],['0','4'],['1','5'],['1','6'],['1','11'],
      ['2','7'],['2','13'],['2','17'],['3','8'],['3','14'],['3','15'],
      ['4','9'],['4','10'],['5','12'],['10','16'],['9','18'],
    ]
  },
  '计算机': {
    nodes: [
      { id: '0', name: '计算机网络', category: 0 },
      { id: '1', name: '网络层', category: 1 }, { id: '2', name: '传输层', category: 1 },
      { id: '3', name: '应用层', category: 1 }, { id: '4', name: '网络安全', category: 1 },
      { id: '5', name: 'IP协议', category: 2 }, { id: '6', name: 'TCP/UDP', category: 2 },
      { id: '7', name: 'HTTP/DNS', category: 2 }, { id: '8', name: '加密算法', category: 2 },
      { id: '9', name: '路由选择', category: 2 }, { id: '10', name: '拥塞控制', category: 2 },
      { id: '11', name: '子网划分', category: 3 }, { id: '12', name: '三次握手', category: 3 },
      { id: '13', name: 'SSL/TLS', category: 3 }, { id: '14', name: '防火墙', category: 3 },
      { id: '15', name: '负载均衡', category: 3 }, { id: '16', name: '数据链路', category: 2 },
      { id: '17', name: 'DHCP', category: 3 }, { id: '18', name: 'CDN', category: 2 },
    ],
    links: [
      ['0','1'],['0','2'],['0','3'],['0','4'],['1','5'],['1','9'],['1','11'],
      ['2','6'],['2','10'],['2','12'],['3','7'],['3','17'],['4','8'],['4','13'],
      ['4','14'],['5','16'],['7','18'],['13','15'],
    ]
  },
}

const getGraphForBook = (book) => {
  if (!book) return null
  const title = book.title || ''
  for (const [key, data] of Object.entries(bookGraphPresets)) {
    if (title.includes(key)) return data
  }
  // 检查 publisher 或 direction 匹配
  if (book.direction) {
    for (const [key, data] of Object.entries(bookGraphPresets)) {
      if (book.direction.includes(key)) return data
    }
  }
  return null
}

const orbitRadii = [160, 280, 380]

const visibleNodes = computed(() => {
  const filter = simpleMode.value ? n => n.category <= 2 : () => true
  const nodes = graphData.nodes.filter(filter)
  return nodes.map((n, i) => {
    const sameCat = graphData.nodes.filter(x => x.category === n.category && filter(x))
    const idx = sameCat.findIndex(x => x.id === n.id)
    const total = sameCat.length
    const radius = [0, 160, 280, 380][n.category]
    const angle = total > 1 ? (idx / total) * Math.PI * 2 - Math.PI / 2 : 0
    const connected = graphData.links.some(l =>
      (l[0] === n.id && l[1] === activeNodeId.value) || (l[1] === n.id && l[0] === activeNodeId.value)
    )
    return {
      ...n, x: Math.cos(angle) * radius, y: Math.sin(angle) * radius,
      r: [60, 40, 28, 20][n.category],
      fontSize: [16, 14, 12, 10][n.category],
      bold: n.category <= 1,
      label: n.name.length > 5 ? n.name.slice(0, 4) + '…' : n.name, connected,
    }
  })
})

const visibleLinks = computed(() => {
  const ids = new Set(visibleNodes.value.map(n => n.id))
  return graphData.links.filter(l => ids.has(l[0]) && ids.has(l[1])).map(l => {
    const a = visibleNodes.value.find(n => n.id === l[0]); const b = visibleNodes.value.find(n => n.id === l[1])
    if (!a || !b) return null
    return { x1: a.x, y1: a.y, x2: b.x, y2: b.y, active: l[0] === activeNodeId.value || l[1] === activeNodeId.value }
  }).filter(Boolean)
})

const tapNode = (n) => { activeNodeId.value = n.id; dimmed.value = true }
const nodeFill = (n) => {
  const colors = ['#7c5cfc', '#a78bfa', '#c4b5fd', '#e0d6f8']
  return n.id === activeNodeId.value ? colors[n.category] : colors[n.category] + '99'
}

const activeNodeName = computed(() => graphData.nodes.find(x => x.id === activeNodeId.value)?.name || '高等数学')
const activeNodeLevel = computed(() => ['核心课程', '章节模块', '核心知识点', '细分知识点'][graphData.nodes.find(x => x.id === activeNodeId.value)?.category] || '')
const densityPct = computed(() => [100, 85, 65, 40][graphData.nodes.find(x => x.id === activeNodeId.value)?.category] || 80)
const connectedNodes = computed(() => visibleNodes.value.filter(n => graphData.links.some(l => (l[0] === activeNodeId.value && l[1] === n.id) || (l[1] === activeNodeId.value && l[0] === n.id))))
const nodeFacts = computed(() => {
  const m = { '高等数学': ['大学数学的基础课程', '涵盖微积分/线代/概率论', '理工科学生必修', '考研数学一的核心'], '微积分': ['研究函数的微分与积分', '导数、积分、极限', '现代数学的基石', '物理/工程广泛应用'], '线性代数': ['向量空间与线性变换', '矩阵、行列式、特征值', '数据科学与ML基础'], '概率论': ['随机现象数学规律', '随机变量、分布、期望', '统计学与AI基础'] }
  return m[activeNodeName.value] || [`${activeNodeName.value}是重要知识点`, '建议通过练习巩固', '考试中高频出现', '理解后可关联多个概念']
})

const particleStyle = (i) => ({
  left: `${(i * 29 + 11) % 100}%`, top: `${(i * 41 + 19) % 100}%`,
  animationDelay: `${(i * 0.6) % 5}s`, animationDuration: `${3 + (i % 4)}s`,
  width: `${4 + (i % 3) * 2}px`, height: `${4 + (i % 3) * 2}px`,
})

/* ===== 教材浏览器 ===== */
const COVERS = ['📘', '📗', '📙', '📕', '📓', '📔', '📒']
const PUBLISHERS = ['高等教育出版社', '清华大学出版社', '机械工业出版社', '人民邮电出版社', '科学出版社', '北京大学出版社', '电子工业出版社']
const DIRECTIONS = ['考研数学一', '考研数学二', '大学必修', '计算机基础', '理工科通识', '数据科学', '人工智能']

const seedRandom = (s) => { let v = s; return () => { v = (v * 16807 + 0) % 2147483647; return (v - 1) / 2147483646 } }

const enhanceBook = (book) => {
  const r = seedRandom(book.id || book.title?.length || 1)
  return {
    ...book,
    coverIcon: book.coverIcon || COVERS[Math.floor(r() * COVERS.length)],
    publisher: book.publisher || PUBLISHERS[Math.floor(r() * PUBLISHERS.length)],
    direction: book.direction || DIRECTIONS[Math.floor(r() * DIRECTIONS.length)],
    aiRecommended: book.aiRecommended ?? (r() > 0.45),
    matchRate: book.matchRate || Math.floor(r() * 13 + 84),
    nodeCount: book.nodeCount || (18 + Math.floor(r() * 30)),
    source: '我的书架', supportsGraph: true,
  }
}

const enhancedBooks = computed(() => bookshelfBooks.value.map(enhanceBook))

const formatStudents = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(0) + 'k'
  return String(n || 0)
}

const myBooks = computed(() => {
  if (!bookSearch.value.trim()) return enhancedBooks.value
  const q = bookSearch.value.trim().toLowerCase()
  return enhancedBooks.value.filter(b =>
    (b.title && b.title.toLowerCase().includes(q)) ||
    (b.publisher && b.publisher.toLowerCase().includes(q)) ||
    (b.author && b.author.toLowerCase().includes(q)) ||
    (b.direction && b.direction.toLowerCase().includes(q))
  )
})

/* 搜索浮层数据 */
const recentBooks = computed(() => enhancedBooks.value.slice(0, 5))

const hotBooks = computed(() => PLATFORM_POOL_ALL.slice(0, 6).map((b, i) => ({
  ...b, id: 'hot-' + i, coverIcon: COVERS[i % COVERS.length],
  matchRate: 88 - i * 3, nodeCount: 20 + i * 5,
})))

const aiSearchBooks = computed(() =>
  enhancedBooks.value.filter(b => b.aiRecommended).sort((a, b) => b.matchRate - a.matchRate).slice(0, 5)
)

const searchAllResults = computed(() => {
  const q = bookSearch.value.trim().toLowerCase()
  if (!q) return []
  const shelfIds = new Set(enhancedBooks.value.map(b => b.id))
  const results = []

  // 书架匹配
  enhancedBooks.value.forEach(b => {
    if (b.title?.toLowerCase().includes(q) || b.publisher?.toLowerCase().includes(q) || (b.author && b.author.toLowerCase().includes(q))) {
      results.push({ ...b, inShelf: true })
    }
  })

  // 平台池匹配
  PLATFORM_POOL_ALL.forEach((b, i) => {
    if (!shelfIds.has('plat-' + i) && (b.title.toLowerCase().includes(q) || b.publisher.toLowerCase().includes(q) || b.direction.toLowerCase().includes(q))) {
      results.push({ ...b, id: 'sr-' + i, coverIcon: COVERS[i % COVERS.length], matchRate: 75 + Math.floor(Math.random() * 18), inShelf: false })
    }
  })

  return results.slice(0, 10)
})

const onSearchSelect = (book) => {
  bookSearch.value = book.title || ''
  searchFocused.value = false
  pendingBook.value = book
}

const PLATFORM_POOL_ALL = [
  { title: '高等数学（同济第七版）', publisher: '高等教育出版社', direction: '考研数学一', students: 58000, supportsGraph: true },
  { title: '高等数学辅导讲义', publisher: '西安交通大学出版社', direction: '考研数学一', students: 42000, supportsGraph: true },
  { title: '线性代数（同济第六版）', publisher: '高等教育出版社', direction: '考研数学一', students: 46000, supportsGraph: true },
  { title: '概率论与数理统计', publisher: '高等教育出版社', direction: '考研数学一', students: 38000, supportsGraph: true },
  { title: '微积分学教程', publisher: '清华大学出版社', direction: '大学必修', students: 32000, supportsGraph: true },
  { title: '数据结构（C语言版）', publisher: '清华大学出版社', direction: '计算机基础', students: 55000, supportsGraph: true },
  { title: '计算机网络', publisher: '机械工业出版社', direction: '计算机基础', students: 48000, supportsGraph: true },
  { title: '大学物理（上）', publisher: '高等教育出版社', direction: '理工科通识', students: 35000, supportsGraph: false },
  { title: '新概念英语3', publisher: '外语教学与研究出版社', direction: '考研英语', students: 62000, supportsGraph: false },
  { title: '数学分析', publisher: '科学出版社', direction: '大学必修', students: 28000, supportsGraph: true },
  { title: '离散数学及其应用', publisher: '机械工业出版社', direction: '计算机基础', students: 31000, supportsGraph: true },
  { title: '考研数学复习全书', publisher: '北京大学出版社', direction: '考研数学一', students: 72000, supportsGraph: true },
]

const platformBooks = computed(() => {
  const q = bookSearch.value.trim().toLowerCase()
  if (!q) return []
  const shelfTitles = new Set(enhancedBooks.value.map(b => b.title?.toLowerCase()))
  return PLATFORM_POOL_ALL
    .filter(b => !shelfTitles.has(b.title.toLowerCase()) && (b.title.toLowerCase().includes(q) || b.publisher.toLowerCase().includes(q) || b.direction.toLowerCase().includes(q)))
    .map((b, i) => ({ id: 'plat-' + i, ...b, coverIcon: COVERS[(i + enhancedBooks.value.length) % COVERS.length], matchRate: 70 + Math.floor(Math.random() * 20), nodeCount: 16 + Math.floor(Math.random() * 28), source: '系统教材库', aiRecommended: Math.random() > 0.6 }))
})

const suggestedBooks = computed(() => {
  if (bookSearch.value.trim()) return []
  const aiBooks = enhancedBooks.value.filter(b => b.aiRecommended && currentBook.value?.id !== b.id).slice(0, 3)
  if (aiBooks.length >= 2) return aiBooks
  return PLATFORM_POOL_ALL.slice(0, 4).map((b, i) => ({ ...b, id: 'sug-' + i, coverIcon: COVERS[i % COVERS.length], nodeCount: 20 + i * 5, source: '系统库', supportsGraph: b.supportsGraph, aiRecommended: true, matchRate: 88 - i * 3 }))
})

const fetchBooks = async () => { try { const r = await api.get('/books'); bookshelfBooks.value = r.data || [] } catch (e) {} }

const openBookPanel = async () => {
  showBookPanel.value = true
  bookSearch.value = ''
  pendingBook.value = null
  await nextTick()
  searchInputRef.value?.focus()
}

const selectBook = (book) => {
  pendingBook.value = book
}

const applyBookSwitch = async () => {
  const target = pendingBook.value
  if (!target || target.id === currentBook.value?.id) {
    showBookPanel.value = false
    return
  }

  // 先尝试预设图谱数据
  const preset = getGraphForBook(target)
  if (preset) {
    graphData.nodes = preset.nodes
    graphData.links = preset.links
    activeNodeId.value = preset.nodes[0]?.id || '0'
    graphKey.value++
    currentBook.value = target
    showBookPanel.value = false
    return
  }

  // 尝试 API 加载
  try {
    const r = await api.get(`/books/${target.id}/chapters`)
    if (r.code === 200 && r.data?.nodes?.length) {
      graphData.nodes = r.data.nodes
      graphData.links = r.data.links || []
      activeNodeId.value = r.data.nodes[0]?.id || '0'
      graphKey.value++
      currentBook.value = target
      showBookPanel.value = false
      return
    }
  } catch (e) {}

  // 回退：用默认数据但改中心节点名
  const defaultNodes = bookGraphPresets['高等数学'].nodes
  const defaultLinks = bookGraphPresets['高等数学'].links
  const name = target.title?.length > 6 ? target.title.slice(0, 5) + '…' : (target.title || '教材')
  graphData.nodes = [{ id: '0', name, category: 0 }, ...defaultNodes.slice(1)]
  graphData.links = [...defaultLinks]
  activeNodeId.value = '0'
  graphKey.value++
  currentBook.value = target
  showBookPanel.value = false
}

/* ===== 习题 ===== */
const fetchExercises = async () => { try { const r = await api.get('/exercises', { params: { knowledgePoint: activeNodeName.value, difficulty: exFilter.value } }); exercises.value = (r.data || []).map((e, i) => enhanceExercise(e, i)) } catch (e) { exercises.value = generateMockExercises() } }
const openExerciseModal = async () => { showExerciseModal.value = true; await fetchExercises() }
const changeExFilter = async (f) => { exFilter.value = f; await fetchExercises() }
const enhanceExercise = (e, i) => ({ ...e, selected: false, diffKey: difficultyKey(e.difficulty), typeIcon: ['📝','🧮','📐','🔢','💡'][i % 5], tags: e.tags || pickTags(i), aiReason: e.aiReason || pickReason(i), estTime: e.estTime || [5,8,10,12,15][i % 5], expGain: e.expGain || [15,20,20,25,30][i % 5] })
const difficultyKey = (d) => { if (/基础/.test(d)) return 'easy'; if (/进阶/.test(d)) return 'mid'; if (/真题/.test(d)) return 'hard'; if (/竞赛|冲刺/.test(d)) return 'extreme'; return 'mid' }
const pickTags = (i) => { const all = [['极限','导数'],['积分','微积分'],['矩阵','线性代数'],['概率','分布'],['中值定理','应用'],['向量','空间'],['连续性','极限'],['微分','近似']]; return all[i % all.length] }
const pickReason = (i) => { const all = ['该知识点在考试中高频出现', '你在相关节点停留较久，建议强化', '与当前知识图谱节点强关联', '你最近在此类题型错误率较高', '考研中该题型占比逐年上升', '巩固基础后可解锁进阶内容']; return all[i % all.length] }
const generateMockExercises = () => {
  const base = [
    { title: '导数定义理解与应用', difficulty: '基础巩固', diffKey: 'easy' }, { title: '极限计算方法综合', difficulty: '基础巩固', diffKey: 'easy' },
    { title: '不定积分换元法', difficulty: '基础巩固', diffKey: 'easy' }, { title: '定积分应用：面积计算', difficulty: '基础巩固', diffKey: 'easy' },
    { title: '导数与极限综合应用', difficulty: '进阶提升', diffKey: 'mid' }, { title: '中值定理证明题', difficulty: '进阶提升', diffKey: 'mid' },
    { title: '多元函数求偏导', difficulty: '进阶提升', diffKey: 'mid' }, { title: '级数收敛性判断', difficulty: '进阶提升', diffKey: 'mid' },
    { title: '泰勒展开与近似计算', difficulty: '进阶提升', diffKey: 'mid' }, { title: '考研真题：极限大题', difficulty: '真题冲刺', diffKey: 'hard' },
    { title: '考研真题：积分综合', difficulty: '真题冲刺', diffKey: 'hard' }, { title: '竞赛：不等式证明', difficulty: '竞赛拓展', diffKey: 'extreme' },
  ]
  return base.map((e, i) => ({ id: 'ex' + (i + 1), ...e, selected: false, typeIcon: ['📝','🧮','📐','🔢','💡'][i % 5], tags: pickTags(i), aiReason: pickReason(i), estTime: [5,8,10,12,15][i % 5], expGain: [15,20,20,25,30][i % 5] }))
}
const startEx = () => { const sel = exercises.value.filter(e => e.selected); if (!sel.length) return; showExerciseModal.value = false; router.push({ path: '/practice', query: { from: 'graph', multiple: 'true', exerciseIds: sel.map(e => e.id).join(',') } }) }
const filteredExercises = computed(() => { if (exFilter.value === 'all') return exercises.value; const m = { basic: '基础巩固', advanced: '进阶提升', exam: '真题冲刺', competition: '竞赛拓展' }; return exercises.value.filter(e => e.difficulty === m[exFilter.value]) })
const selectedExCount = computed(() => exercises.value.filter(e => e.selected).length)
const getFilterCount = (k) => { if (k === 'all') return exercises.value.length; const m = { basic: '基础巩固', advanced: '进阶提升', exam: '真题冲刺', competition: '竞赛拓展' }; return exercises.value.filter(e => e.difficulty === m[k]).length }
const totalEstTime = computed(() => exercises.value.filter(e => e.selected).reduce((s, e) => s + (e.estTime || 8), 0))
const totalExp = computed(() => exercises.value.filter(e => e.selected).reduce((s, e) => s + (e.expGain || 20), 0))
const benefitTags = computed(() => { const tags = ['微积分理解', '导数应用', '极限计算']; if (activeNodeName.value.includes('代数')) tags.splice(1, 2, '矩阵运算', '向量空间'); if (activeNodeName.value.includes('概率')) tags.splice(1, 2, '分布函数', '期望计算'); return tags })
const refreshRecommend = async () => { isRefreshing.value = true; await fetchExercises(); await new Promise(r => setTimeout(r, 400)); isRefreshing.value = false }

onMounted(fetchBooks)

const exportPDF = async () => {
  const wrap = document.querySelector('.kg-canvas-wrap'); if (!wrap) return
  try {
    const clone = wrap.cloneNode(true); clone.style.cssText = 'width:1100px;height:680px;position:fixed;left:-9999px;top:0;background:#faf8ff;'
    clone.querySelectorAll('.kg-toolbar,.kg-tbtn,.kg-hero-sub,.kg-legend,.kg-home-btn,.kg-particle').forEach(el => el.remove())
    const titleEl = clone.querySelector('.kg-hero-title'); if (titleEl) { titleEl.style.fontSize = '28px'; titleEl.style.marginBottom = '12px' }
    document.body.appendChild(clone); await new Promise(r => setTimeout(r, 300))
    const canvas = await html2canvas(clone, { scale: 2, backgroundColor: '#faf8ff', useCORS: true }); document.body.removeChild(clone)
    const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' }); const w = pdf.internal.pageSize.getWidth(); const h = pdf.internal.pageSize.getHeight()
    pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 5, 5, w - 10, h - 10); pdf.save(`知识图谱_${activeNodeName.value}_${new Date().toLocaleDateString()}.pdf`)
  } catch (e) { console.error('导出PDF失败:', e) }
}
</script>

<style scoped>
.kg-root { height: calc(100vh - 60px); position: relative; }

.kg-page {
  --c: #7c5cfc; --c2: #a78bfa; --cl: #f5f0ff; --cb: #ece6fa; --t: #2d2438; --m: #9088a0;
  height: 100%; display: flex; overflow: hidden;
  background: #faf8ff; font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
}

/* 面板打开时仅侧栏微暗，画布保持明亮 */
.kg-page--dimmed .kg-side {
  filter: brightness(0.85) saturate(0.9);
  transition: filter 0.4s ease;
}

.kg-canvas-wrap {
  flex: 1; position: relative; overflow: hidden; cursor: grab;
  background: radial-gradient(ellipse at 50% 50%, #f8f5ff 0%, #f0eaf8 45%, #e8def0 100%);
}
.kg-canvas-wrap:active { cursor: grabbing; }

.kg-particle {
  position: absolute; border-radius: 50%; background: var(--c2);
  opacity: .2; pointer-events: none;
  animation: pFloat 4s ease-in-out infinite;
}
@keyframes pFloat { 0%,100% { transform: translateY(0) scale(1); opacity: .1; } 50% { transform: translateY(-20px) scale(2); opacity: .35; } }

.kg-hero { position: absolute; top: 28px; left: 32px; z-index: 5; display: flex; flex-direction: column; gap: 6px; }

.kg-home-btn {
  align-self: flex-start; padding: 9px 20px; border-radius: 10px;
  border: 1px solid var(--cb); background: rgba(255,255,255,.85); backdrop-filter: blur(8px);
  color: var(--c); font-size: 13px; font-weight: 700; cursor: pointer; transition: all .25s;
  box-shadow: 0 2px 8px rgba(120,100,180,.08);
}
.kg-home-btn:hover { background: var(--c); color: #fff; border-color: var(--c); transform: translateX(-2px); box-shadow: 0 4px 16px rgba(124,92,252,.25); }

.kg-hero-title {
  font-size: 36px; font-weight: 900; margin: 0; letter-spacing: 2px;
  background: linear-gradient(135deg, #5b3cb8 0%, #7c5cfc 40%, #a78bfa 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.kg-hero-sub { margin: 0; font-size: 14px; color: #9088a0; font-weight: 500; }

.kg-toolbar { position: absolute; top: 28px; right: 28px; z-index: 5; display: flex; gap: 6px; }

.kg-tbtn {
  padding: 8px 16px; border-radius: 10px; border: 1px solid var(--cb);
  background: rgba(255,255,255,.8); backdrop-filter: blur(8px);
  color: var(--t); font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all .2s; white-space: nowrap;
}
.kg-tbtn:hover { background: var(--cl); border-color: var(--c); color: var(--c); }
.kg-tbtn--primary {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff; border: none; box-shadow: 0 2px 12px rgba(124,92,252,.22);
}
.kg-tbtn--primary:hover { background: linear-gradient(135deg, #6b4aee, #9678f0); color: #fff; transform: translateY(-1px); box-shadow: 0 4px 18px rgba(124,92,252,.35); }

.kg-svg { width: 100%; height: 100%; position: relative; z-index: 1; }

.kg-core-glow { animation: coreBreathe 3s ease-in-out infinite; }
@keyframes coreBreathe { 0%,100% { opacity: .7; } 50% { opacity: 1; } }

.kg-node-group { cursor: pointer; transition: opacity .4s; animation: nodeFadeIn 0.35s ease-out both; }
@keyframes nodeFadeIn { from { opacity: 0; } to { opacity: 1; } }
.kg-node-group.dim { opacity: .25; }

.kg-link-line { animation: linkFadeIn 0.5s ease-out both; }
@keyframes linkFadeIn { from { opacity: 0; } to { opacity: 1; } }

.kg-glow-ring { animation: ringPulse 2.2s ease-in-out infinite; }
@keyframes ringPulse { 0%,100% { opacity: .4; } 50% { opacity: .85; } }

.kg-select-ring { animation: dashSpin 8s linear infinite; }
@keyframes dashSpin { to { stroke-dashoffset: -60; } }

.kg-flow-dot { filter: drop-shadow(0 0 4px rgba(124,92,252,.8)); }

.kg-legend {
  position: absolute; bottom: 18px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 14px; font-size: 11px; color: var(--m);
  background: rgba(255,255,255,.75); padding: 7px 20px; border-radius: 20px;
  backdrop-filter: blur(8px); border: 1px solid var(--cb);
}
.leg-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }

.kg-side {
  width: 260px; background: rgba(255,255,255,.78); backdrop-filter: blur(14px);
  border-left: 1px solid var(--cb); padding: 20px 16px;
  display: flex; flex-direction: column; gap: 14px; overflow-y: auto;
  transition: filter 0.4s ease;
}

.ks-hero { background: #faf9fd; border-radius: 16px; padding: 20px; border: 1px solid var(--cb); }
.ks-big { font-size: 20px; font-weight: 800; color: var(--t); }
.ks-tag-row { display: flex; align-items: center; gap: 8px; margin: 6px 0 10px; }
.ks-lv { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; background: var(--cl); color: var(--c); }
.ks-pct { font-size: 12px; color: var(--m); font-weight: 600; }
.ks-bar { height: 7px; background: #f0ecf8; border-radius: 10px; overflow: hidden; }
.ks-bar-fill { height: 100%; border-radius: 10px; background: linear-gradient(90deg, #a78bfa, #7c5cfc); transition: width .6s ease; }

.ks-card { background: #faf9fd; border-radius: 14px; padding: 16px; border: 1px solid var(--cb); }
.ks-label { font-size: 12px; font-weight: 700; color: var(--m); margin-bottom: 8px; letter-spacing: .3px; }
.ks-tags { display: flex; flex-wrap: wrap; gap: 5px; }
.ks-tag { padding: 5px 12px; border-radius: 20px; font-size: 12px; background: var(--cl); color: var(--c); cursor: pointer; transition: all .2s; border: 1px solid transparent; }
.ks-tag:hover { background: #ece2fc; border-color: var(--c); transform: translateY(-1px); }
.ks-none { font-size: 12px; color: var(--m); }
.ks-list { list-style: none; padding: 0; margin: 0; }
.ks-list li { padding: 8px 0; font-size: 13px; color: var(--t); line-height: 1.5; border-bottom: 1px solid #f4f0fa; }
.ks-list li::before { content: '•'; color: var(--c); font-weight: 700; margin-right: 8px; }
.ks-list li:last-child { border-bottom: none; }

.ks-btn {
  width: 100%; padding: 13px; border: none; border-radius: 14px;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff; font-size: 14px; font-weight: 700; cursor: pointer;
  transition: all .3s; box-shadow: 0 4px 16px rgba(124,92,252,.22);
}
.ks-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(124,92,252,.35); }

/* ================================================
   教材浏览器 — 独立浮层，不受父级 filter 影响
   ================================================ */

.sb-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(20,16,40,0.25);
  display: flex; align-items: center; justify-content: center;
  animation: sbOverlayIn 0.25s ease-out;
}
@keyframes sbOverlayIn { from { opacity: 0; } to { opacity: 1; } }

.sb-card {
  width: 720px; max-height: 82vh; border-radius: 20px;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255,255,255,0.65);
  box-shadow:
    0 24px 80px rgba(80,60,180,0.22),
    0 4px 16px rgba(0,0,0,0.05),
    0 0 0 1px rgba(255,255,255,0.5) inset;
  display: flex; flex-direction: column; overflow: hidden;
  animation: sbCardIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes sbCardIn {
  from { opacity: 0; transform: translateY(16px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ---- Header ---- */
.sb-head {
  display: flex; align-items: center; gap: 16px;
  padding: 18px 20px; flex-shrink: 0;
  border-bottom: 1px solid rgba(200,185,240,0.15);
}
.sb-head-l { flex-shrink: 0; }
.sb-head-title { margin: 0; font-size: 18px; font-weight: 800; color: #2d2438; letter-spacing: -0.2px; line-height: 1.2; }
.sb-head-sub { margin: 1px 0 0; font-size: 11px; color: #9088a0; font-weight: 500; }

.sb-head-search { flex: 1; position: relative; }
.sb-head-search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: #b0a8c0; pointer-events: none; }
.sb-head-search-input {
  width: 100%; height: 38px; padding: 0 36px 0 36px; border-radius: 10px;
  border: 1.5px solid rgba(200,185,240,0.3); background: rgba(255,255,255,0.55);
  font-size: 13px; font-family: inherit; color: #2d2438; outline: none; transition: all 0.25s ease;
}
.sb-head-search-input::placeholder { color: #b0a8c0; }
.sb-head-search-input:focus { border-color: #a78bfa; box-shadow: 0 0 0 3px rgba(124,92,252,0.06); background: rgba(255,255,255,0.85); }

.sb-head-search-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 24px; height: 24px; border-radius: 50%; border: none;
  background: rgba(0,0,0,0.05); color: #9088a0; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.sb-head-search-clear:hover { background: rgba(0,0,0,0.1); color: #2d2438; }

/* ---- 搜索浮层 ---- */
.sp-drop {
  position: absolute; top: calc(100% + 4px); left: 0; right: 0;
  background: rgba(255,255,255,0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(200,190,230,0.22);
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(80,60,180,0.12), 0 2px 8px rgba(0,0,0,0.04);
  max-height: 320px; overflow-y: auto; z-index: 10;
  padding: 6px;
  animation: spDropIn 0.16s ease-out;
}
@keyframes spDropIn { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }

.sp-drop::-webkit-scrollbar { width: 3px; }
.sp-drop::-webkit-scrollbar-thumb { background: rgba(180,165,220,0.2); border-radius: 3px; }

.sp-sec { padding: 2px 4px; }
.sp-sec + .sp-sec { border-top: 1px solid rgba(200,190,230,0.1); margin-top: 4px; padding-top: 8px; }

.sp-sec-head {
  font-size: 10px; font-weight: 700; color: #b0a8c0;
  text-transform: uppercase; letter-spacing: 0.4px;
  padding: 4px 8px 6px;
}
.sp-count { font-size: 9px; padding: 1px 6px; border-radius: 5px; background: rgba(124,92,252,0.07); color: #7c5cfc; font-weight: 700; }

/* 热门 / AI 推荐 胶囊 */
.sp-row { display: flex; gap: 5px; flex-wrap: wrap; padding: 0 4px; }

.sp-chip {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 5px 10px; border-radius: 8px; border: none;
  background: rgba(255,255,255,0.4); border: 1px solid rgba(200,190,230,0.15);
  font-size: 11px; font-family: inherit; color: #2d2438; cursor: pointer;
  transition: all 0.15s ease;
}
.sp-chip:hover { background: rgba(255,255,255,0.7); border-color: #a78bfa; transform: translateY(-1px); }

/* 搜索结果行 */
.sp-line {
  display: flex; align-items: center; gap: 9px;
  width: 100%; padding: 7px 10px; border-radius: 8px; border: none;
  background: transparent; cursor: pointer; font-family: inherit;
  transition: all 0.15s ease; text-align: left;
  animation: spDropIn 0.25s ease-out both;
}
.sp-line:hover { background: rgba(245,240,255,0.6); }

.sp-line-cover { font-size: 16px; flex-shrink: 0; width: 26px; text-align: center; }

.sp-line-name {
  flex: 1; min-width: 0; font-size: 12px; font-weight: 600; color: #2d2438;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.sp-line-extra {
  font-size: 10px; color: #9088a0; flex-shrink: 0;
  max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.sp-line-pct {
  font-size: 10px; font-weight: 700; color: #7c3aed; flex-shrink: 0;
  padding: 1px 6px; border-radius: 6px; background: rgba(167,139,250,0.07);
}

.sp-line-shelf {
  font-size: 9px; font-weight: 700; color: #16a34a; flex-shrink: 0;
  padding: 1px 5px; border-radius: 5px; background: rgba(22,163,74,0.06);
}

.sp-none { text-align: center; padding: 18px; font-size: 12px; color: #9088a0; }

.sb-head-close {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid rgba(200,185,240,0.25); background: rgba(255,255,255,0.5);
  color: #9088a0; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s ease; flex-shrink: 0;
}
.sb-head-close:hover { background: rgba(124,92,252,0.06); color: #7c5cfc; border-color: rgba(167,139,250,0.35); }

/* ---- Body ---- */
.sb-body { flex: 1; overflow-y: auto; padding: 6px 20px 12px; }
.sb-body::-webkit-scrollbar { width: 4px; }
.sb-body::-webkit-scrollbar-thumb { background: rgba(180,165,220,0.22); border-radius: 4px; }
.sb-body::-webkit-scrollbar-thumb:hover { background: rgba(160,140,200,0.4); }

.sb-section { margin-bottom: 6px; }
.sb-section-head {
  display: flex; align-items: center; gap: 7px; padding: 12px 4px 8px;
  font-size: 12px; font-weight: 700; color: #9088a0;
  text-transform: uppercase; letter-spacing: 0.4px;
  position: sticky; top: 0; z-index: 2;
  background: rgba(255,255,255,0.6); backdrop-filter: blur(8px);
  border-radius: 8px; margin-bottom: 2px;
}
.sb-section-icon { font-size: 14px; }
.sb-section-count { font-size: 10px; padding: 1px 7px; border-radius: 8px; background: rgba(180,165,220,0.15); color: #7c5cfc; font-weight: 700; }
.sb-section-head--platform .sb-section-count { background: rgba(59,130,246,0.1); color: #3b82f6; }

/* ---- 教材卡片 ---- */
.sb-book {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 14px; border-radius: 12px; height: 80px; box-sizing: border-box;
  background: rgba(255,255,255,0.45);
  border: 1.5px solid rgba(220,210,240,0.2);
  cursor: pointer; transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative; overflow: hidden;
  animation: sbBookIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
  margin-bottom: 6px;
}
@keyframes sbBookIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.sb-book:hover { background: rgba(255,255,255,0.75); border-color: rgba(167,139,250,0.3); transform: translateY(-2px); box-shadow: 0 6px 22px rgba(100,80,170,0.08); }

/* 当前展示中 */
.sb-book.active { border-color: #a78bfa; background: linear-gradient(135deg, rgba(250,246,255,0.9), rgba(245,238,255,0.85)); box-shadow: 0 0 0 3px rgba(124,92,252,0.08), 0 6px 24px rgba(124,92,252,0.08); }

/* 待切换 */
.sb-book.pending { border-color: #c4b5fd; background: linear-gradient(135deg, rgba(250,248,255,0.8), rgba(248,244,255,0.75)); box-shadow: 0 0 0 2px rgba(167,139,250,0.1), 0 4px 18px rgba(124,92,252,0.06); }

.sb-book-shine {
  position: absolute; inset: 0; z-index: 1; pointer-events: none;
  background: linear-gradient(105deg, transparent 38%, rgba(255,255,255,0.35) 48%, transparent 52%);
  transform: translateX(-120%); transition: transform 0.5s ease;
}
.sb-book:hover .sb-book-shine { transform: translateX(120%); }

.sb-book-cover {
  width: 48px; height: 58px; border-radius: 7px; flex-shrink: 0;
  background: linear-gradient(135deg, #f5f0ff, #e6ddf6);
  border: 1px solid rgba(180,160,220,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; position: relative; z-index: 2;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(80,50,140,0.06);
}
.sb-book:hover .sb-book-cover { transform: scale(1.06); }
.sb-book.active .sb-book-cover { background: linear-gradient(135deg, #ede3ff, #dcd0f6); border-color: rgba(167,139,250,0.3); }
.sb-book-cover--thin { width: 44px; }

.sb-book-info { flex: 1; min-width: 0; position: relative; z-index: 2; }
.sb-book-name { font-size: 15px; font-weight: 700; color: #2d2438; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 2px; }
.sb-book.active .sb-book-name { color: #1c1530; }
.sb-book-publisher { font-size: 11px; color: #9088a0; font-weight: 500; margin-bottom: 4px; }
.sb-book-tags { display: flex; gap: 5px; flex-wrap: wrap; }
.sb-book-tag { padding: 2px 8px; border-radius: 5px; font-size: 10px; font-weight: 600; background: rgba(240,236,250,0.6); color: #9088a0; }
.sb-book-tag--nodes { background: rgba(124,92,252,0.06); color: #7c5cfc; }
.sb-book-tag--hot { background: rgba(239,68,68,0.06); color: #ef4444; display: inline-flex; align-items: center; gap: 2px; }
.sb-book-tag--graph { background: rgba(16,185,129,0.06); color: #10b981; }

.sb-book-status { flex-shrink: 0; position: relative; z-index: 2; }

/* 当前展示中 */
.sb-book-active-tag {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 4px 11px; border-radius: 20px; font-size: 11px; font-weight: 700;
  color: #7c3aed; background: rgba(124,92,252,0.07);
  border: 1px solid rgba(167,139,250,0.25);
}
.sb-book-active-dot { width: 5px; height: 5px; border-radius: 50%; background: #7c5cfc; box-shadow: 0 0 8px rgba(124,92,252,0.5); animation: activeDotPulse 2s ease-in-out infinite; }
@keyframes activeDotPulse { 0%, 100% { box-shadow: 0 0 6px rgba(124,92,252,0.4); } 50% { box-shadow: 0 0 14px rgba(124,92,252,0.7); } }

/* 待切换 */
.sb-book-pending-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600;
  color: #a78bfa; background: rgba(167,139,250,0.05);
  border: 1px solid rgba(167,139,250,0.15);
}

.sb-book-match { display: inline-flex; align-items: center; gap: 3px; padding: 4px 9px; border-radius: 20px; font-size: 10px; font-weight: 700; color: #7c3aed; background: rgba(167,139,250,0.08); border: 1px solid rgba(167,139,250,0.18); }
.sb-book-source { font-size: 10px; color: #b0a8c0; font-weight: 500; }
.sb-book--light { background: rgba(255,255,255,0.3); border-color: rgba(200,190,230,0.12); }

/* ---- 空态 ---- */
.sb-empty { display: flex; align-items: center; justify-content: center; padding: 32px 20px; min-height: 200px; }
.sb-empty-wrap { text-align: center; }
.sb-empty-icon { font-size: 40px; display: block; margin-bottom: 12px; opacity: 0.5; }
.sb-empty-title { font-size: 15px; font-weight: 700; color: #2d2438; margin: 0 0 4px; }
.sb-empty-hint { font-size: 12px; color: #9088a0; margin: 0 0 16px; }
.sb-empty-suggestions { margin-top: 12px; }
.sb-empty-sug-label { font-size: 11px; font-weight: 600; color: #9088a0; text-transform: uppercase; letter-spacing: 0.3px; display: block; margin-bottom: 8px; }
.sb-empty-sug-list { display: flex; gap: 6px; flex-wrap: wrap; justify-content: center; }
.sb-empty-sug-item { display: inline-flex; align-items: center; gap: 5px; padding: 7px 14px; border-radius: 20px; border: 1px solid rgba(200,185,240,0.25); background: rgba(255,255,255,0.5); font-size: 12px; font-weight: 600; color: #2d2438; cursor: pointer; font-family: inherit; transition: all 0.2s; }
.sb-empty-sug-item:hover { border-color: #a78bfa; background: rgba(255,255,255,0.8); transform: translateY(-1px); }

/* ---- 底部 ---- */
.sb-foot { display: flex; align-items: center; justify-content: space-between; padding: 12px 20px; flex-shrink: 0; border-top: 1px solid rgba(200,185,240,0.15); background: rgba(255,255,255,0.35); }
.sb-foot-l { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.sb-foot-label { font-size: 11px; color: #9088a0; font-weight: 600; flex-shrink: 0; }
.sb-foot-name { font-size: 13px; font-weight: 700; color: #2d2438; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sb-foot-arrow { font-size: 14px; color: #a78bfa; font-weight: 700; flex-shrink: 0; }
.sb-foot-next { font-size: 13px; font-weight: 700; color: #a78bfa; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.sb-foot-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px; border-radius: 12px; border: none;
  background: rgba(180,170,210,0.15); color: #9088a0;
  font-size: 13px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0; position: relative; overflow: hidden;
}
.sb-foot-btn.ready {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #fff; box-shadow: 0 4px 14px rgba(124,92,252,0.25);
}
.sb-foot-btn.ready::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.2) 50%, transparent 60%);
  transform: translateX(-100%); animation: sbShimmer 2.8s infinite;
}
@keyframes sbShimmer { 100% { transform: translateX(100%); } }
.sb-foot-btn.ready:hover { transform: translateY(-2px); box-shadow: 0 8px 26px rgba(124,92,252,0.4); }
.sb-foot-btn:active { transform: translateY(0) scale(0.97); }

/* ====== 习题弹窗 ====== */
.ex-modal-overlay { position: fixed; inset: 0; background: rgba(45,36,56,.30); z-index: 2001; display: flex; align-items: center; justify-content: center; animation: exFadeIn .22s ease-out; }
@keyframes exFadeIn { from { opacity: 0; } to { opacity: 1; } }
.ex-modal { width: 820px; max-height: 88vh; border-radius: 24px; background: rgba(255,255,255,.92); backdrop-filter: blur(18px); box-shadow: 0 20px 60px rgba(124,92,252,.18), 0 0 0 1px rgba(200,185,240,.3); display: flex; flex-direction: column; overflow: hidden; animation: exScaleIn .22s cubic-bezier(.34,1.56,.64,1); }
@keyframes exScaleIn { from { transform: scale(.96); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.exm-top { display: flex; align-items: flex-start; padding: 24px 28px 18px; position: relative; border-bottom: 1px solid rgba(220,210,245,.4); background: linear-gradient(135deg, rgba(248,244,255,.8), rgba(255,255,255,.6)); }
.exmt-left { flex: 1; min-width: 0; } .exmt-node { font-size: 13px; font-weight: 700; color: var(--c2); margin-bottom: 6px; display: flex; align-items: center; gap: 4px; }
.exmt-title { font-size: 18px; font-weight: 800; color: var(--t); letter-spacing: -.3px; }
.exmt-meta { margin-top: 8px; font-size: 12px; color: var(--m); display: flex; align-items: center; gap: 4px; } .exmt-meta strong { color: var(--c); } .exmt-dot { color: var(--c3); }
.exmt-right { flex-shrink: 0; text-align: right; margin-right: 36px; } .exmtr-item { font-size: 12px; color: var(--m); font-weight: 600; margin-bottom: 6px; }
.exmtr-tags { display: flex; flex-direction: column; align-items: flex-end; gap: 3px; } .exmtr-tags span { font-size: 11px; color: #16a34a; font-weight: 600; }
.exmt-close { position: absolute; top: 16px; right: 18px; width: 32px; height: 32px; border-radius: 50%; border: none; background: rgba(0,0,0,.04); color: var(--m); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all .2s; }
.exmt-close:hover { background: rgba(0,0,0,.08); color: var(--t); }
.exm-filters { display: flex; gap: 8px; padding: 14px 28px; background: rgba(250,249,253,.6); border-bottom: 1px solid rgba(220,210,245,.3); overflow-x: auto; }
.exm-filters button { display: flex; align-items: center; gap: 5px; padding: 7px 15px; border-radius: 20px; border: 1.5px solid rgba(200,185,240,.35); background: #fff; font-size: 12px; font-weight: 600; color: var(--m); cursor: pointer; transition: all .2s; font-family: inherit; white-space: nowrap; flex-shrink: 0; }
.exm-filters button:hover { border-color: var(--c3); color: var(--c); } .exm-filters button.on { background: var(--c); color: #fff; border-color: var(--c); box-shadow: 0 2px 12px rgba(124,92,252,.2); }
.exmf-icon { font-size: 13px; } .exmf-count { font-size: 10px; opacity: .7; }
.exm-list { flex: 1; overflow-y: auto; padding: 16px 28px; display: flex; flex-direction: column; gap: 10px; min-height: 200px; }
.exml-empty { text-align: center; padding: 40px; color: var(--m); } .exml-empty span { font-size: 36px; display: block; margin-bottom: 8px; }
.exml-card { display: flex; align-items: center; gap: 14px; padding: 14px 16px; border-radius: 16px; border: 1.5px solid rgba(220,210,245,.3); background: #fff; cursor: pointer; transition: all .25s cubic-bezier(.4,0,.2,1); }
.exml-card:hover { border-color: var(--c3); transform: translateY(-1px); box-shadow: 0 4px 20px rgba(124,92,252,.06); }
.exml-card.sel { border-color: var(--c); background: linear-gradient(135deg, #faf7ff, #f5f0ff); box-shadow: 0 0 0 2px rgba(124,92,252,.06); }
.exmlc-left { flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 4px; } .exmlc-diff { width: 10px; height: 10px; border-radius: 50%; }
.exmlc-diff.easy { background: #4ade80; } .exmlc-diff.mid { background: #fbbf24; } .exmlc-diff.hard { background: #f87171; } .exmlc-diff.extreme { background: #c084fc; }
.exmlc-type { font-size: 20px; } .exmlc-mid { flex: 1; min-width: 0; } .exmlcm-title { font-size: 14px; font-weight: 700; color: var(--t); margin-bottom: 6px; }
.exmlcm-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; } .exmlcm-tags span { padding: 2px 8px; border-radius: 6px; font-size: 10px; font-weight: 600; background: #f5f0ff; color: var(--c); }
.exmlcm-reason { font-size: 11px; color: var(--m); display: flex; align-items: center; gap: 4px; } .exmlcm-reason svg { color: var(--c); flex-shrink: 0; }
.exmlc-right { flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.exmlcr-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; } .exmlcr-time { font-size: 11px; color: var(--m); } .exmlcr-exp { font-size: 11px; font-weight: 700; color: #16a34a; }
.exmlcr-check { width: 28px; height: 28px; border-radius: 8px; border: 2px solid rgba(200,185,240,.5); display: flex; align-items: center; justify-content: center; transition: all .2s; color: transparent; }
.exmlcr-check.on { background: var(--c); border-color: var(--c); color: #fff; }
.exm-foot { display: flex; align-items: center; justify-content: space-between; padding: 14px 28px; background: rgba(250,249,253,.8); border-top: 1px solid rgba(220,210,245,.35); }
.exmf-left { font-size: 12px; color: var(--m); display: flex; align-items: center; gap: 4px; } .exmf-left strong { color: var(--c); } .exmf-dot { color: var(--c3); }
.exmf-right { display: flex; align-items: center; gap: 10px; }
.exmfr-refresh { display: flex; align-items: center; gap: 5px; padding: 9px 16px; border-radius: 12px; border: 1.5px solid rgba(200,185,240,.4); background: #fff; color: var(--c); font-size: 13px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; }
.exmfr-refresh:hover:not(:disabled) { background: #f5f0ff; } .exmfr-refresh:disabled { opacity: .6; cursor: not-allowed; }
.exmfr-start { display: flex; align-items: center; gap: 6px; padding: 9px 22px; border-radius: 12px; border: none; background: linear-gradient(135deg, #7c5cfc, #9b7cff); color: #fff; font-size: 14px; font-weight: 700; cursor: pointer; font-family: inherit; transition: all .25s; box-shadow: 0 4px 16px rgba(124,92,252,.2); }
.exmfr-start:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(124,92,252,.35); }
.exmfr-start:disabled { opacity: .4; cursor: not-allowed; transform: none; box-shadow: none; }
.exm-filters::-webkit-scrollbar, .exm-list::-webkit-scrollbar { height: 0; width: 4px; }
.exm-list::-webkit-scrollbar-thumb { background: rgba(200,185,240,.4); border-radius: 4px; }

@media (max-width: 900px) {
  .kg-side { display: none; } .kg-hero-title { font-size: 24px; } .kg-hero { top: 14px; left: 16px; } .kg-toolbar { top: 14px; right: 12px; }
  .sb-card { width: 96vw; } .sb-head { flex-wrap: wrap; } .sb-head-search { order: 3; width: 100%; }
  .sb-foot-l { flex-wrap: wrap; }
}
</style>
