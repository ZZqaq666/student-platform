<script setup>
import { computed, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SeniorHeader from './components/SeniorHeader.vue'
import SeniorSidebar from './components/SeniorSidebar.vue'
import SeniorQuestionList from './components/SeniorQuestionList.vue'
import SeniorAskDialog from './components/SeniorAskDialog.vue'
import SeniorDetailDialog from './components/SeniorDetailDialog.vue'
import { UI_STATES, useSeniorQA } from './composables/useSeniorQA'

const router = useRouter()

const {
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
} = useSeniorQA()

/* ===== 页面入场动画 & 背景装饰 ===== */
const pageReady = ref(false)
onMounted(async () => {
  await nextTick()
  setTimeout(() => { pageReady.value = true }, 60)
})

const decoStyle = (i, type) => {
  const seed = i * 7 + type.length * 13
  const pseudo = ((seed * 31) % 100) / 100
  if (type === 'star') {
    return {
      left: `${(i * 37 + 11) % 100}%`,
      top: `${(i * 23 + 7) % 100}%`,
      fontSize: `${pseudo * 10 + 8}px`,
      opacity: pseudo * 0.12 + 0.03,
      animationDelay: `${pseudo * 4}s`,
      animationDuration: `${pseudo * 3 + 3}s`,
    }
  }
  return {
    left: `${(i * 53 + 3) % 100}%`,
    top: `${(i * 47 + 5) % 100}%`,
    width: `${pseudo * 6 + 3}px`,
    height: `${pseudo * 6 + 3}px`,
    opacity: pseudo * 0.10 + 0.03,
    animationDelay: `${pseudo * 3}s`,
  }
}

const isSubmittingQuestion = computed(() => askState.value === UI_STATES.LOADING)

function goHome() {
  router.push('/')
}

async function handleAskSubmit(payload) {
  const result = await publishQuestion(payload)
  if (!result.ok) {
    ElMessage.error(result.message)
    return
  }

  ElMessage.success(result.message)
  closeAskDialog()
  resetAskState()
}

async function handleOpenDetail(questionId) {
  await openDetail(questionId)
}

function handleToggleLike(answerId) {
  const ok = toggleAnswerLike(answerId)
  if (!ok) {
    ElMessage.warning('操作未生效，请刷新后重试。')
  }
}

function handleAcceptAnswer(answerId) {
  const result = acceptAnswer(answerId)
  if (!result.ok) {
    ElMessage.warning(result.message)
    return
  }

  ElMessage.success(result.message)
}

function handleSubmitFollowUp(content) {
  const result = submitFollowUp(content)
  if (!result.ok) {
    ElMessage.warning(result.message)
    return
  }

  ElMessage.success(result.message)
}
</script>

<template>
  <div class="senior-page-shell" :class="{ ready: pageReady }">
    <!-- 背景装饰层 -->
    <div class="bg-decor">
      <span v-for="s in 6" :key="'star' + s" class="deco-star" :style="decoStyle(s, 'star')">✦</span>
      <span v-for="d in 5" :key="'dot' + d" class="deco-dot" :style="decoStyle(d, 'dot')" />
    </div>

    <div class="senior-page-container">
      <SeniorHeader :stats="stats" @ask="openAskDialog" @back-home="goHome" />

      <main class="content-grid">
        <SeniorSidebar
          :seniors="filteredSeniors"
          :current-filter="seniorFilter"
          :selected-senior-id="selectedSeniorId"
          @update:current-filter="setSeniorFilter"
          @select-senior="selectSenior"
        />

        <SeniorQuestionList
          :selected-senior="selectedSenior"
          :list-state="listState"
          :list-filter="listFilter"
          :list-sort="listSort"
          :questions="paginatedQuestions"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredAndSortedQuestions.length"
          @update:list-filter="setListFilter"
          @update:list-sort="setListSort"
          @update:current-page="setCurrentPage"
          @open-detail="handleOpenDetail"
        />
      </main>
    </div>

    <SeniorAskDialog
      v-model="askDialogVisible"
      :submitting="isSubmittingQuestion"
      @submit="handleAskSubmit"
    />

    <SeniorDetailDialog
      v-model="detailDialogVisible"
      :detail-state="detailState"
      :detail-error="detailError"
      :question="activeQuestion"
      :current-user-id="currentUser.id"
      @retry="retryLoadDetail"
      @toggle-like="handleToggleLike"
      @accept-answer="handleAcceptAnswer"
      @submit-follow-up="handleSubmitFollowUp"
      @update:model-value="closeDetailDialog"
    />
  </div>
</template>

<style scoped>
.senior-page-shell {
  position: relative;
  min-height: calc(100vh - 60px);
  background:
    radial-gradient(circle at 8% 12%, rgba(167, 139, 250, .14), transparent 26%),
    radial-gradient(circle at 92% 20%, rgba(91, 157, 252, .10), transparent 28%),
    radial-gradient(circle at 70% 88%, rgba(252, 123, 171, .08), transparent 30%),
    linear-gradient(175deg, #fffdfd 0%, #f9f6ff 34%, #f8fafd 64%, #fff8fb 100%);
  padding: 24px 18px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
  overflow: hidden;
}

/* ---- 背景装饰 ---- */
.bg-decor { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.deco-star { position: absolute; color: #fbbf24; animation: starTwinkle 3s ease-in-out infinite; }
@keyframes starTwinkle { 0%,100%{opacity:.03;transform:scale(1)} 50%{opacity:.14;transform:scale(1.3)} }
.deco-dot { position: absolute; border-radius: 50%; background: #c4b5fd; animation: dotPulse 4s ease-in-out infinite; }
@keyframes dotPulse { 0%,100%{opacity:.04;transform:scale(1)} 50%{opacity:.15;transform:scale(1.8)} }

/* ---- 入场动画 ---- */
.senior-page-container { position: relative; z-index: 1; margin: 0 auto; width: min(1200px, 100%); display: grid; gap: 16px; }
.senior-page-container > * { opacity: 0; transform: translateY(24px); }
.senior-page-shell.ready .senior-page-container > * { animation: sectionIn .7s ease-out forwards; }
.senior-page-shell.ready .senior-page-container > *:nth-child(1) { animation-delay: .05s; }
.senior-page-shell.ready .senior-page-container > *:nth-child(2) { animation-delay: .18s; }
@keyframes sectionIn { to { opacity: 1; transform: translateY(0); } }

.content-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

@media (max-width: 980px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .senior-page-shell {
    padding: 14px 10px 18px;
  }
}
</style>
