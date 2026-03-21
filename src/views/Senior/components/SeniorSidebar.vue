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
  background: #ffffff;
  border: 1px solid #e5ebf5;
  border-radius: 20px;
  padding: 18px;
  box-shadow: 0 15px 24px -24px rgba(16, 42, 67, 0.5);
}

.panel-header {
  margin-bottom: 14px;
}

.panel-title {
  margin: 0;
  font-size: 19px;
  color: #15233c;
}

.panel-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #607388;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.tag-btn {
  border: 1px solid #d7e0ec;
  border-radius: 999px;
  padding: 6px 12px;
  background: #ffffff;
  color: #526681;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-btn:hover {
  border-color: #99b3d8;
  color: #2d4e74;
}

.tag-btn.active {
  border-color: transparent;
  background: linear-gradient(135deg, #1768ac 0%, #2a9d8f 100%);
  color: #ffffff;
}

.senior-list {
  display: grid;
  gap: 10px;
}

.senior-card {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 10px;
  border: 1px solid #e5edf8;
  border-radius: 14px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.senior-card:hover {
  border-color: #9db9de;
  transform: translateY(-1px);
}

.senior-card.active {
  border-color: #2a9d8f;
  background: #f2fbf9;
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
  background: #d8e8fd;
  color: #1b4c8d;
  font-weight: 700;
}

.online-dot {
  position: absolute;
  right: 1px;
  bottom: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  background: #bec9d8;
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
  color: #182f4d;
  font-weight: 600;
}

.cert-badge {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 8px;
  color: #0f766e;
  background: #d9f5ef;
}

.meta {
  margin: 4px 0;
  color: #5f7189;
  font-size: 13px;
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  font-size: 12px;
  color: #30557f;
  background: #edf4fd;
  border-radius: 8px;
  padding: 2px 6px;
}

.stats {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6c7d92;
}
</style>
