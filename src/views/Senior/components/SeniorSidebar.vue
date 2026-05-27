<script setup>
import { computed } from 'vue'

const props = defineProps({
  seniors: {
    type: Array,
    default: () => []
  },
  currentFilter: {
    type: String,
    default: 'all'
  },
  selectedSeniorId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:currentFilter', 'select-senior'])

const filterOptions = computed(() => {
  return [
    { value: 'all', label: '全部' },
    { value: 'math', label: '数学' },
    { value: 'physics', label: '物理' },
    { value: 'cs', label: '计算机' },
    { value: 'english', label: '英语' }
  ]
})

function updateFilter(nextFilter) {
  emit('update:currentFilter', nextFilter)
}

function selectSenior(seniorId) {
  emit('select-senior', seniorId)
}
</script>

<template>
  <section class="sidebar-panel">
    <header class="panel-header">
      <h2 class="panel-title">答疑学长学姐</h2>
      <p class="panel-subtitle">选择方向后可精准查看对应问答</p>
    </header>

    <div class="filter-tags">
      <button
        v-for="option in filterOptions"
        :key="option.value"
        class="tag-btn"
        :class="{ active: currentFilter === option.value }"
        type="button"
        @click="updateFilter(option.value)"
      >
        {{ option.label }}
      </button>
    </div>

    <div class="senior-list">
      <article
        v-for="senior in seniors"
        :key="senior.id"
        class="senior-card"
        :class="{ active: selectedSeniorId === senior.id }"
        @click="selectSenior(senior.id)"
      >
        <div class="avatar-wrap">
          <span class="avatar">{{ senior.name.slice(0, 1) }}</span>
          <span class="online-dot" :class="{ on: senior.online }"></span>
        </div>

        <div class="senior-info">
          <div class="name-row">
            <p class="name">{{ senior.name }}</p>
            <span v-if="senior.certified" class="cert-badge">认证</span>
          </div>
          <p class="meta">{{ senior.grade }} · {{ senior.major }}</p>
          <div class="tags">
            <span v-for="tag in senior.tags" :key="`${senior.id}-${tag}`" class="tag">{{ tag }}</span>
          </div>
          <p class="stats">答疑 {{ senior.answerCount }} 次 · 评分 {{ senior.rating }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.sidebar-panel {
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(230,224,244,.6);
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 2px 20px rgba(120,100,180,.04);
}

.panel-header {
  margin-bottom: 14px;
}

.panel-title {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  color: #1a1528;
}

.panel-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9088a0;
  font-weight: 500;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.tag-btn {
  border: 1.5px solid rgba(200,185,240,.35);
  border-radius: 999px;
  padding: 5px 12px;
  background: rgba(255,255,255,.7);
  color: #5b4a8a;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all .25s cubic-bezier(.4,0,.2,1);
  font-family: inherit;
}

.tag-btn:hover {
  border-color: #c4b5fd;
  color: #7c5cfc;
  transform: translateY(-1px);
}

.tag-btn.active {
  border-color: transparent;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  color: #ffffff;
}

.senior-list {
  display: grid;
  gap: 8px;
}

.senior-card {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 10px;
  border: 1.5px solid rgba(220,210,245,.35);
  border-radius: 14px;
  padding: 12px;
  cursor: pointer;
  transition: all .35s cubic-bezier(.34,1.56,.64,1);
  background: rgba(255,255,255,.5);
  position: relative;
  overflow: hidden;
}

.senior-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(124,92,252,.04), transparent 60%);
  opacity: 0;
  transition: opacity .35s;
}

.senior-card::after {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(180deg, #7c5cfc, #a78bfa);
  border-radius: 0 3px 3px 0;
  transition: height .35s cubic-bezier(.34,1.56,.64,1);
}

.senior-card:hover {
  border-color: #c4b5fd;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(124,92,252,.08);
}

.senior-card:hover::before {
  opacity: 1;
}

.senior-card:hover::after {
  height: 60%;
}

.senior-card.active {
  border-color: #7c5cfc;
  background: linear-gradient(135deg, #f5f0ff, #ece2fc);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(124,92,252,.1);
}

.senior-card.active::after {
  height: 70%;
}

.avatar-wrap {
  position: relative;
  width: 44px;
  height: 44px;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8e0fa, #c4b5fd);
  color: #5b4a8a;
  font-weight: 800;
  font-size: 16px;
}

.online-dot {
  position: absolute;
  right: 1px;
  bottom: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  background: #ccc;
}

.online-dot.on {
  background: #22c55e;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name {
  margin: 0;
  color: #2d2438;
  font-weight: 700;
  font-size: 14px;
}

.cert-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 6px;
  color: #7c5cfc;
  background: #f0ecfc;
  font-weight: 600;
}

.meta {
  margin: 4px 0;
  color: #9088a0;
  font-size: 12px;
}

.tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  color: #7c5cfc;
  background: #f5f0ff;
  border-radius: 6px;
  padding: 2px 6px;
  font-weight: 500;
}

.stats {
  margin: 6px 0 0;
  font-size: 12px;
  color: #b0a8c0;
}
</style>
