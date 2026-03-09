<template>
  <div class="home-page">
    <!-- 快速入口 -->
    <div class="section-title">快速入口</div>
    <el-row :gutter="16" class="quick-entry">
      <el-col :span="6">
        <div class="entry-card blue" @click="toPage('/qa')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">问答中心</div>
              <div class="card-subtitle">ai问答</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">💬</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="entry-card cyan" @click="toPage('/graph')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">知识图谱</div>
              <div class="card-subtitle">辅助</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">🌐</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="entry-card blue" @click="toPage('/bookshelf')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">个人书架</div>
              <div class="card-subtitle">藏书阁</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">📚</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="entry-card cyan" @click="toPage('/senior')">
          <div class="card-content">
            <div class="card-text">
              <div class="card-title">学长学姐答疑专区</div>
              <div class="card-subtitle">进步方式</div>
            </div>
            <div class="card-icon">
              <div class="icon-bg">
                <span class="icon">👨‍🎓</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近一本书的学习进度 -->
    <div class="book-progress-section">
      <div class="section-title">最近学习</div>
      <div class="book-progress-card">
        <div class="book-info">
          <div class="book-image-placeholder">
            <span class="placeholder-text">书籍封面</span>
          </div>
          <div class="book-details">
            <h3 class="book-title">高等数学（上册）</h3>
            <p class="book-author">同济大学数学系</p>
            <div class="progress-info">
              <span class="progress-text">已读 65%</span>
              <div class="progress-bar">
                <div class="progress-fill" style="width: 65%;"></div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 学习曲线图（包含日期和进度） -->
        <div class="chart-section">
          <h4 class="chart-title">学习趋势</h4>
          <div class="chart-container">
            <svg viewBox="0 0 800 250" class="learning-chart">
              <defs>
                <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" style="stop-color:#3b82f6;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#06b6d4;stop-opacity:1" />
                </linearGradient>
                <linearGradient id="areaGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" style="stop-color:#3b82f6;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#3b82f6;stop-opacity:0" />
                </linearGradient>
              </defs>
              
              <!-- 网格线 -->
              <line x1="60" y1="40" x2="750" y2="40" stroke="#e5e7eb" stroke-width="1"/>
              <line x1="60" y1="90" x2="750" y2="90" stroke="#e5e7eb" stroke-width="1"/>
              <line x1="60" y1="140" x2="750" y2="140" stroke="#e5e7eb" stroke-width="1"/>
              <line x1="60" y1="190" x2="750" y2="190" stroke="#e5e7eb" stroke-width="1"/>
              
              <!-- Y轴标签 -->
              <text x="30" y="45" text-anchor="middle" fill="#9ca3af" font-size="12">100%</text>
              <text x="30" y="95" text-anchor="middle" fill="#9ca3af" font-size="12">75%</text>
              <text x="30" y="145" text-anchor="middle" fill="#9ca3af" font-size="12">50%</text>
              <text x="30" y="195" text-anchor="middle" fill="#9ca3af" font-size="12">25%</text>
              
              <!-- X轴日期标签 -->
              <text x="100" y="230" text-anchor="middle" fill="#9ca3af" font-size="12">02-20</text>
              <text x="250" y="230" text-anchor="middle" fill="#9ca3af" font-size="12">02-22</text>
              <text x="400" y="230" text-anchor="middle" fill="#9ca3af" font-size="12">02-24</text>
              <text x="550" y="230" text-anchor="middle" fill="#9ca3af" font-size="12">02-26</text>
              <text x="700" y="230" text-anchor="middle" fill="#9ca3af" font-size="12">02-28</text>
              
              <!-- 学习曲线区域 -->
              <path d="M 100,180 Q 175,160 250,140 T 400,100 T 550,80 T 700,50 L 700,200 L 100,200 Z" 
                    fill="url(#areaGradient)"/>
              
              <!-- 学习曲线 -->
              <path d="M 100,180 Q 175,160 250,140 T 400,100 T 550,80 T 700,50" 
                    fill="none" 
                    stroke="url(#lineGradient)" 
                    stroke-width="3"/>
              
              <!-- 数据点 -->
              <circle cx="100" cy="180" r="5" fill="#3b82f6"/>
              <circle cx="250" cy="140" r="5" fill="#3b82f6"/>
              <circle cx="400" cy="100" r="5" fill="#3b82f6"/>
              <circle cx="550" cy="80" r="5" fill="#3b82f6"/>
              <circle cx="700" cy="50" r="6" fill="#3b82f6" stroke="white" stroke-width="2"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
// 跳转到对应页面
const toPage = (path) => router.push(path)
</script>

<style scoped>
.home-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* 页面标题 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 区块标题 */
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

/* 快速入口卡片 */
.quick-entry {
  margin-bottom: 32px;
}

.entry-card {
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100px;
  display: flex;
  align-items: center;
}

.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.entry-card.blue {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.entry-card.cyan {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.card-text {
  color: white;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 14px;
  opacity: 0.9;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-bg {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  font-size: 24px;
}

/* 书籍学习进度部分 */
.book-progress-section {
  margin-top: 24px;
}

.book-progress-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.book-info {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.book-image-placeholder {
  width: 120px;
  height: 160px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.placeholder-text {
  color: #6366f1;
  font-size: 14px;
  font-weight: 500;
}

.book-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.book-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.book-author {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 20px 0;
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-text {
  font-size: 16px;
  font-weight: 600;
  color: #3b82f6;
}

.progress-bar {
  height: 10px;
  background: #e5e7eb;
  border-radius: 5px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 5px;
  transition: width 0.3s ease;
}

/* 图表部分 */
.chart-section {
  border-top: 1px solid #e5e7eb;
  padding-top: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px 0;
}

.chart-container {
  width: 100%;
  height: 260px;
}

.learning-chart {
  width: 100%;
  height: 100%;
}
</style>
