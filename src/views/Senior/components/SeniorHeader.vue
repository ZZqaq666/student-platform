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
  padding: 28px 32px;
  border-radius: 20px;
  background: rgba(255,255,255,.75);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(230,224,244,.6);
  box-shadow: 0 2px 20px rgba(120,100,180,.05);
  position: relative;
  overflow: hidden;
}

.senior-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #7c5cfc, #a78bfa, #fbbf24, #5cc9a0, #7c5cfc);
  background-size: 200% 100%;
  animation: gradientFlow 6s ease infinite;
}

@keyframes gradientFlow {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
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
  font-size: 28px;
  font-weight: 900;
  color: #1a1528;
  letter-spacing: -.5px;
  background: linear-gradient(135deg, #1a1528 20%, #7c5cfc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-subtitle {
  margin: 6px 0 0;
  color: #807a90;
  font-size: 14px;
}

.stats-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  background: rgba(248,246,255,.7);
  border: 1px solid rgba(220,210,245,.35);
  border-radius: 14px;
  padding: 16px 14px;
  transition: all .3s cubic-bezier(.34,1.56,.64,1);
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #7c5cfc, #a78bfa);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform .35s cubic-bezier(.34,1.56,.64,1);
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(124,92,252,.1);
  border-color: #c4b5fd;
}

.stat-card:hover::after {
  transform: scaleX(1);
}

.stat-value {
  margin: 0;
  font-size: 28px;
  font-weight: 900;
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  margin: 4px 0 0;
  font-size: 12px;
  font-weight: 600;
  color: #9088a0;
  letter-spacing: 0.02em;
}

.ghost-btn,
.solid-btn {
  border-radius: 999px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all .3s cubic-bezier(.34,1.56,.64,1);
  font-family: inherit;
  position: relative;
  overflow: hidden;
}

.ghost-btn {
  background: rgba(255,255,255,.85);
  border: 1.5px solid rgba(200,185,240,.35);
  color: #5b4a8a;
}

.ghost-btn:hover {
  border-color: #c4b5fd;
  color: #7c5cfc;
  box-shadow: 0 4px 14px rgba(124,92,252,.06);
  transform: translateY(-1px);
}

.solid-btn {
  background: linear-gradient(135deg, #7c5cfc, #a78bfa);
  border: none;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(124,92,252,.22);
}

.solid-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #a78bfa, #7c5cfc);
  opacity: 0;
  transition: opacity .3s;
}

.solid-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(124,92,252,.4);
}

.solid-btn:hover::before {
  opacity: 1;
}

.solid-btn::after {
  content: '✨';
  position: relative;
  z-index: 1;
  margin-left: 4px;
  font-size: 12px;
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
