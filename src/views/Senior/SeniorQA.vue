<script setup>
import { computed } from 'vue'
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
  <div class="senior-page-shell">
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
  min-height: 100%;
  background:
    radial-gradient(circle at 90% 0%, rgba(255, 173, 96, 0.16) 0%, transparent 40%),
    radial-gradient(circle at 0% 100%, rgba(52, 160, 164, 0.12) 0%, transparent 45%),
    #f3f7fb;
  padding: 24px 18px;
}

.senior-page-container {
  margin: 0 auto;
  width: min(1200px, 100%);
  display: grid;
  gap: 16px;
}

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
