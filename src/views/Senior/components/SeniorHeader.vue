<script setup>
defineProps({
  stats: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['ask', 'back-home'])

function onBackHome() {
  emit('back-home')
}

function onAsk() {
  emit('ask')
}
</script>

<template>
  <section class="senior-header">
    <div class="header-top">
      <button class="ghost-btn" type="button" @click="onBackHome">返回首页</button>
      <button class="solid-btn" type="button" @click="onAsk">我要提问</button>
    </div>

    <div class="header-main">
      <h1 class="header-title">学长学姐答疑专区</h1>
      <p class="header-subtitle">聚焦真实学习问题，快速获得可执行的解题建议</p>
    </div>

    <div class="stats-grid">
      <article class="stat-card">
        <p class="stat-value">{{ stats.seniorCount }}</p>
        <p class="stat-label">活跃学长学姐</p>
      </article>
      <article class="stat-card">
        <p class="stat-value">{{ stats.totalQuestions }}</p>
        <p class="stat-label">累计问答</p>
      </article>
      <article class="stat-card">
        <p class="stat-value">{{ stats.solvedRate }}%</p>
        <p class="stat-label">问题解决率</p>
      </article>
      <article class="stat-card">
        <p class="stat-value">{{ stats.avgRating }}</p>
        <p class="stat-label">平均满意度</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.senior-header {
  padding: 24px;
  border-radius: 24px;
  background: radial-gradient(circle at 0 0, #eef8ff 0%, #f9fff9 50%, #fff7ec 100%);
  border: 1px solid #deebff;
  box-shadow: 0 20px 40px -34px rgba(15, 42, 74, 0.45);
}

.header-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.header-main {
  margin-top: 18px;
}

.header-title {
  margin: 0;
  font-size: 30px;
  color: #13315c;
  letter-spacing: 0.02em;
}

.header-subtitle {
  margin: 8px 0 0;
  color: #4f647f;
  font-size: 15px;
}

.stats-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid #e6eef8;
  border-radius: 14px;
  padding: 14px 12px;
}

.stat-value {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #0e7490;
}

.stat-label {
  margin: 4px 0 0;
  font-size: 13px;
  color: #526377;
}

.ghost-btn,
.solid-btn {
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ghost-btn {
  background: #ffffff;
  border-color: #d7e3f7;
  color: #26426c;
}

.ghost-btn:hover {
  border-color: #8eb0e5;
  color: #1a3760;
}

.solid-btn {
  background: linear-gradient(120deg, #ff7f50 0%, #ff9147 100%);
  color: #ffffff;
}

.solid-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px -12px rgba(255, 126, 80, 0.9);
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 540px) {
  .senior-header {
    padding: 18px;
  }

  .header-title {
    font-size: 24px;
  }

  .header-top {
    flex-direction: column;
    align-items: stretch;
  }

  .ghost-btn,
  .solid-btn {
    width: 100%;
  }
}
</style>
