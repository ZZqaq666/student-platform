<script setup>
import { computed, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  detailState: {
    type: String,
    default: 'idle'
  },
  detailError: {
    type: String,
    default: ''
  },
  question: {
    type: Object,
    default: null
  },
  currentUserId: {
    type: [Number, String],
    default: ''
  }
})

const emit = defineEmits([
  'update:modelValue',
  'retry',
  'toggle-like',
  'accept-answer',
  'submit-follow-up'
])

const followUpDraft = shallowRef('')

const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
  }
})

const orderedAnswers = computed(() => {
  if (!props.question) {
    return []
  }

  const acceptedId = props.question.acceptedAnswerId
  return [...props.question.answers].sort((left, right) => {
    if (left.id === acceptedId) {
      return -1
    }
    if (right.id === acceptedId) {
      return 1
    }
    return right.likes - left.likes
  })
})

const canAccept = computed(() => {
  if (!props.question) {
    return false
  }

  return String(props.question.authorId) === String(props.currentUserId) && !props.question.acceptedAnswerId
})

function retry() {
  emit('retry')
}

function toggleLike(answerId) {
  emit('toggle-like', answerId)
}

function acceptAnswer(answerId) {
  emit('accept-answer', answerId)
}

function submitFollowUp() {
  const content = followUpDraft.value.trim()
  if (!content) {
    ElMessage.warning('追问内容不能为空。')
    return
  }

  emit('submit-follow-up', content)
  followUpDraft.value = ''
}

function onClose() {
  followUpDraft.value = ''
}
</script>

<template>
  <el-dialog v-model="visible" width="760px" class="detail-dialog" title="问题详情" @closed="onClose">
    <div v-if="detailState === 'loading'" class="state-panel">
      <el-skeleton :rows="7" animated />
    </div>

    <div v-else-if="detailState === 'error'" class="state-panel error">
      <p class="state-title">加载失败</p>
      <p class="state-text">{{ detailError }}</p>
      <el-button type="primary" plain @click="retry">重试</el-button>
    </div>

    <div v-else-if="detailState === 'empty'" class="state-panel">
      <p class="state-title">暂无可展示内容</p>
      <p class="state-text">该问题信息不完整，请稍后再试。</p>
    </div>

    <div v-else-if="question" class="detail-content">
      <section class="question-overview">
        <header class="overview-head">
          <span class="status-chip" :class="{ solved: question.solved }">
            {{ question.solved ? '已解决' : '待解决' }}
          </span>
          <span class="overview-time">{{ question.createdAt }}</span>
        </header>
        <h3 class="overview-title">{{ question.title }}</h3>
        <p class="overview-text">{{ question.content }}</p>
        <div v-if="question.images.length" class="overview-image-list">
          <img
            v-for="image in question.images"
            :key="`${question.id}-${image.id}`"
            :src="image.url"
            :alt="image.name"
            class="overview-image"
          >
        </div>
        <div class="overview-meta">
          <span>提问者：{{ question.author }}</span>
          <span>浏览 {{ question.views }}</span>
          <span>点赞 {{ question.likes }}</span>
        </div>
      </section>

      <section class="answer-section">
        <h4 class="section-title">回答（{{ question.answerCount }}）</h4>
        <article v-for="answer in orderedAnswers" :key="answer.id" class="answer-card">
          <div class="answer-head">
            <div class="head-left">
              <p class="answerer">{{ answer.authorName }}</p>
              <span v-if="answer.certified" class="cert-tag">认证</span>
              <span class="badge">{{ answer.badge }}</span>
            </div>
            <span class="answer-time">{{ answer.time }}</span>
          </div>

          <p class="answer-text">{{ answer.content }}</p>

          <footer class="answer-actions">
            <button
              class="flat-btn"
              type="button"
              :class="{ active: answer.likedByCurrentUser }"
              @click="toggleLike(answer.id)"
            >
              {{ answer.likedByCurrentUser ? '已点赞' : '点赞' }}（{{ answer.likes }}）
            </button>

            <button
              v-if="canAccept || question.acceptedAnswerId === answer.id"
              class="flat-btn"
              :class="{ accepted: question.acceptedAnswerId === answer.id }"
              type="button"
              :disabled="!canAccept"
              @click="acceptAnswer(answer.id)"
            >
              {{ question.acceptedAnswerId === answer.id ? '已采纳' : '采纳回答' }}
            </button>
          </footer>
        </article>

        <p v-if="!orderedAnswers.length" class="no-answer">暂无人工回复，欢迎继续追问</p>
      </section>

      <section class="follow-up-section">
        <h4 class="section-title">追问互动</h4>
        <el-input
          v-model="followUpDraft"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
          placeholder="可继续补充你的问题背景或验证情况"
        />
        <div class="follow-up-actions">
          <el-button type="primary" @click="submitFollowUp">提交追问</el-button>
        </div>

        <div v-if="question.followUps.length" class="follow-up-list">
          <article v-for="item in question.followUps" :key="item.id" class="follow-item">
            <header class="follow-head">
              <span>{{ item.author }}</span>
              <span>{{ item.time }}</span>
            </header>
            <p class="follow-text">{{ item.content }}</p>
          </article>
        </div>
      </section>
    </div>
  </el-dialog>
</template>

<style scoped>
.state-panel {
  border: 1px dashed #d4e0ef;
  border-radius: 12px;
  padding: 18px;
  text-align: center;
}

.state-panel.error {
  border-color: #f2d0c9;
  background: #fff8f6;
}

.state-title {
  margin: 0;
  color: #183154;
  font-size: 16px;
}

.state-text {
  margin: 8px 0 12px;
  color: #63778f;
}

.detail-content {
  display: grid;
  gap: 16px;
  max-height: 72vh;
  overflow-y: auto;
  padding-right: 4px;
}

.question-overview,
.answer-section,
.follow-up-section {
  border: 1px solid #e6edf8;
  border-radius: 12px;
  padding: 14px;
  background: #ffffff;
}

.overview-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-chip {
  font-size: 12px;
  font-weight: 700;
  color: #ab6500;
  background: #fff3e6;
  border-radius: 999px;
  padding: 4px 10px;
}

.status-chip.solved {
  color: #0f766e;
  background: #def7f3;
}

.overview-time {
  color: #6f8096;
  font-size: 12px;
}

.overview-title {
  margin: 10px 0 6px;
  color: #132a45;
}

.overview-text {
  margin: 0;
  color: #4e6076;
  line-height: 1.65;
}

.overview-image-list {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.overview-image {
  width: 100%;
  height: 104px;
  border-radius: 10px;
  border: 1px solid #e2eaf6;
  object-fit: cover;
}

.overview-meta {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #697b90;
  font-size: 12px;
}

.section-title {
  margin: 0 0 12px;
  color: #1a3355;
  font-size: 16px;
}

.answer-card {
  border: 1px solid #e5ebf5;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 10px;
}

.answer-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.head-left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.answerer {
  margin: 0;
  color: #193355;
  font-weight: 600;
}

.cert-tag {
  font-size: 12px;
  color: #0f766e;
  background: #d9f5ef;
  border-radius: 8px;
  padding: 2px 6px;
}

.badge {
  font-size: 12px;
  color: #7d5100;
  background: #ffefcf;
  border-radius: 8px;
  padding: 2px 6px;
}

.answer-time {
  font-size: 12px;
  color: #6d8097;
}

.answer-text {
  margin: 10px 0;
  color: #455a74;
  line-height: 1.7;
}

.answer-actions {
  display: flex;
  gap: 8px;
}

.flat-btn {
  border: 1px solid #d7e0ec;
  border-radius: 8px;
  background: #ffffff;
  color: #355b81;
  font-size: 13px;
  padding: 5px 10px;
  cursor: pointer;
}

.flat-btn.active {
  border-color: #2f77c6;
  color: #2f77c6;
  background: #edf5ff;
}

.flat-btn.accepted {
  border-color: transparent;
  color: #0f766e;
  background: #dcf6f1;
}

.flat-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.no-answer {
  margin: 0;
  border-radius: 10px;
  border: 1px dashed #d9e2ef;
  background: #f8fbff;
  padding: 12px;
  color: #6b7e94;
}

.follow-up-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.follow-up-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.follow-item {
  border: 1px solid #e6edf8;
  border-radius: 10px;
  padding: 10px;
}

.follow-head {
  display: flex;
  justify-content: space-between;
  color: #5f738c;
  font-size: 12px;
}

.follow-text {
  margin: 6px 0 0;
  color: #425772;
}

@media (max-width: 768px) {
  .detail-dialog :deep(.el-dialog) {
    width: 96% !important;
    margin-top: 3vh;
  }

  .detail-content {
    max-height: 70vh;
  }

  .overview-image-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .answer-head,
  .overview-head,
  .follow-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
