<script setup>
const props = defineProps({
  selectedSenior: {
    type: Object,
    default: null
  },
  listState: {
    type: String,
    default: 'success'
  },
  listFilter: {
    type: String,
    default: 'all'
  },
  listSort: {
    type: String,
    default: 'time'
  },
  questions: {
    type: Array,
    default: () => []
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 6
  },
  total: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits([
  'update:list-filter',
  'update:list-sort',
  'update:current-page',
  'open-detail'
])

function updateListFilter(value) {
  emit('update:list-filter', value)
}

function updateListSort(value) {
  emit('update:list-sort', value)
}

function updateCurrentPage(value) {
  emit('update:current-page', value)
}

function openDetail(questionId) {
  emit('open-detail', questionId)
}

function getDisplayImages(images) {
  if (!Array.isArray(images)) {
    return []
  }

  return images.slice(0, 3)
}
</script>

<template>
  <section class="question-panel">
    <header class="panel-header">
      <div>
        <h2 class="panel-title">{{ selectedSenior ? `${selectedSenior.name} 的问答` : '最新问答' }}</h2>
        <p class="panel-subtitle">支持筛选、排序与分页联动</p>
      </div>
      <div class="panel-filters">
        <el-select
          :model-value="listFilter"
          size="small"
          class="filter-select"
          placeholder="筛选"
          @update:model-value="updateListFilter"
        >
          <el-option label="全部" value="all" />
          <el-option label="热门" value="hot" />
          <el-option label="未解决" value="unsolved" />
          <el-option label="已解决" value="solved" />
        </el-select>

        <el-select
          :model-value="listSort"
          size="small"
          class="filter-select"
          placeholder="排序"
          @update:model-value="updateListSort"
        >
          <el-option label="按时间" value="time" />
          <el-option label="按点赞" value="likes" />
        </el-select>
      </div>
    </header>

    <div v-if="listState === 'loading'" class="status-card">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="listState === 'empty'" class="status-card empty">
      <p class="status-title">暂无匹配内容</p>
      <p class="status-text">请调整筛选条件或直接发布新问题。</p>
    </div>

    <div v-else class="question-list">
      <article
        v-for="question in questions"
        :key="question.id"
        class="question-card"
        @click="openDetail(question.id)"
      >
        <header class="question-top">
          <span class="category-chip" :class="`cat-${question.category}`">{{ question.categoryText }}</span>
          <span class="time">{{ question.createdAt }}</span>
        </header>

        <h3 class="title">{{ question.title }}</h3>
        <p class="preview">{{ question.preview }}</p>

        <div v-if="question.images.length" class="question-image-list">
          <img
            v-for="image in getDisplayImages(question.images)"
            :key="`${question.id}-${image.id}`"
            :src="image.url"
            :alt="image.name"
            class="question-image"
            @click.stop
          >
        </div>

        <div class="tag-list" v-if="question.tags.length">
          <span v-for="tag in question.tags" :key="`${question.id}-${tag}`" class="tag-chip"># {{ tag }}</span>
        </div>

        <footer class="card-footer">
          <div class="author">{{ question.author }}</div>
          <div class="stats">
            <span>浏览 {{ question.views }}</span>
            <span>回答 {{ question.answerCount }}</span>
            <span>赞 {{ question.likes }}</span>
            <span class="status" :class="{ solved: question.solved }">
              {{ question.solved ? '已解决' : '待解决' }}
            </span>
          </div>
        </footer>
      </article>
    </div>

    <footer class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @update:current-page="updateCurrentPage"
      />
    </footer>
  </section>
</template>

<style scoped>
.question-panel {
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(230,224,244,.6);
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 2px 20px rgba(120,100,180,.04);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.panel-title {
  margin: 0;
  color: #1a1528;
  font-size: 17px;
  font-weight: 800;
}

.panel-subtitle {
  margin: 4px 0 0;
  color: #9088a0;
  font-size: 12px;
  font-weight: 500;
}

.panel-filters {
  display: flex;
  gap: 8px;
}

.filter-select {
  width: 108px;
}

.status-card {
  border: 1.5px dashed rgba(200,185,240,.4);
  border-radius: 14px;
  padding: 16px;
  background: rgba(248,246,255,.5);
}

.status-card.empty {
  text-align: center;
}

.status-title {
  margin: 0;
  color: #2d2438;
  font-size: 15px;
  font-weight: 700;
}

.status-text {
  margin: 6px 0 0;
  color: #9088a0;
  font-size: 13px;
}

.question-list {
  display: grid;
  gap: 10px;
}

.question-card {
  background: rgba(255,255,255,.5);
  border: 1.5px solid rgba(220,210,245,.3);
  border-radius: 14px;
  padding: 14px;
  cursor: pointer;
  transition: all .35s cubic-bezier(.34,1.56,.64,1);
  position: relative;
  overflow: hidden;
}

.question-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 20%, rgba(167,139,250,.06), transparent 70%);
  opacity: 0;
  transition: opacity .4s;
}

.question-card::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #7c5cfc, #a78bfa, transparent);
  border-radius: 3px 0 0 3px;
  transform: scaleY(0);
  transform-origin: top;
  transition: transform .4s cubic-bezier(.34,1.56,.64,1);
}

.question-card:hover {
  border-color: #c4b5fd;
  background: rgba(248,246,255,.75);
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(124,92,252,.1);
}

.question-card:hover::before {
  opacity: 1;
}

.question-card:hover::after {
  transform: scaleY(1);
}

.question-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.category-chip {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.category-chip.cat-math {
  background: #f0ecfc;
  color: #7c5cfc;
}

.category-chip.cat-physics {
  background: #ecfdf3;
  color: #5cc9a0;
}

.category-chip.cat-cs {
  background: #fff7ed;
  color: #ffb84d;
}

.category-chip.cat-english,
.category-chip.cat-other {
  background: #fdf2f8;
  color: #fc7bab;
}

.time {
  font-size: 12px;
  color: #b0a8c0;
}

.title {
  margin: 10px 0 6px;
  color: #2d2438;
  font-size: 16px;
  font-weight: 700;
}

.preview {
  margin: 0;
  color: #807a90;
  line-height: 1.65;
  font-size: 13px;
}

.question-image-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.question-image {
  width: 100%;
  height: 88px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid rgba(230,224,244,.5);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.tag-chip {
  font-size: 11px;
  color: #7c5cfc;
  background: #f5f0ff;
  border-radius: 6px;
  padding: 2px 8px;
  font-weight: 500;
}

.card-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.author {
  color: #5b4a8a;
  font-size: 12px;
  font-weight: 600;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  font-size: 12px;
  color: #b0a8c0;
}

.status {
  font-weight: 700;
  color: #ffb84d;
}

.status.solved {
  color: #5cc9a0;
}

.pagination-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .panel-header {
    flex-direction: column;
  }

  .panel-filters {
    width: 100%;
  }

  .filter-select {
    flex: 1;
  }

  .question-image-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats {
    justify-content: flex-start;
  }
}
</style>
