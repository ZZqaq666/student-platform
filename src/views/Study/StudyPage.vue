<template>
  <div class="study-page">
    <!-- 顶部导航栏 -->
    <div class="study-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">返回</button>
        <button class="menu-btn active">目录</button>
        <button class="search-btn">搜索</button>
      </div>
      <div class="header-right">
        <button class="icon-btn">
          <span class="icon">🔍</span>
        </button>
        <button class="icon-btn">
          <span class="icon">💬</span>
        </button>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="study-content">
      <!-- 左侧目录区 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h3>目录</h3>
          <button class="collapse-btn">▼</button>
        </div>
        <div class="toc-tree">
          <div class="toc-item">
            <span class="toc-title">第一章</span>
            <div class="toc-children">
              <div class="toc-item active">
                <span class="toc-title">1.1 知识卡片</span>
                <div class="toc-children">
                  <div class="toc-item">
                    <span class="toc-title">1.1.1 高亮章节</span>
                  </div>
                  <div class="toc-item">
                    <span class="toc-title">1.1.2 笔记</span>
                  </div>
                </div>
              </div>
              <div class="toc-item">
                <span class="toc-title">1.2 树形分析</span>
                <div class="toc-children">
                  <div class="toc-item">
                    <span class="toc-title">1.2.1 高亮章节</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="toc-item">
            <span class="toc-title">第二章</span>
            <div class="toc-children">
              <div class="toc-item">
                <span class="toc-title">2.1 高亮</span>
              </div>
              <div class="toc-item">
                <span class="toc-title">2.2 专业管理</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间阅读区 -->
      <div class="reading-area">
        <div class="reading-header">
          <h2>沉浸式</h2>
          <button class="expand-btn">▼</button>
        </div>
        <div class="reading-content">
          <p>知识卡片卡词语的造语句理词中，可以重型正文阅读申述中公务场景。可以将构成正式话的情景正式项，似以方相可使分连句文更传确表达。</p>
          <p>通俗周易行后是，实场中的文字啊确结，可正实文字对比应写入自己在边的工具应用的内容的类结。苏扬性盘文化社中，备通文</p>
          
          <div class="section-title">分清单</div>
          <div class="checklist">
            <div class="check-item">
              <input type="checkbox" checked>
              <span>选择文中在文中处词语理理理服务</span>
            </div>
            <div class="check-item">
              <input type="checkbox">
              <span>准备需要位位置标标标注词文</span>
            </div>
          </div>
          
          <p>一拉训，文选组中的在文字在文的边的理服务，卡片，正文信信正在二分法实用啊应用的可进定义。取后技巧</p>
          <p>人了似切割，复动的实理可，可始解啊</p>
          
          <button class="create-btn">建立新工具</button>
          
          <p>需哈，试选择一种检验工具，可检验确，预学中等内容</p>
          <p>过可还可通过分重组集可应用于中级内容和复习项，流法，</p>
          <p>能在中公务易场景的可性选择场世界，智与智能服人员交交的最在至理为学习的词，可对照实物过一遍后对复人员的实写的进正正文信写后</p>
        </div>
        <div class="pagination">
          <span>1</span>
          <span>2</span>
          <span>3</span>
          <span>6</span>
          <span>14</span>
          <span>›</span>
        </div>
      </div>

      <!-- 右侧辅助信息区 -->
      <div class="sidebar right-sidebar">
        <div class="sidebar-header">
          <h3>辅助信息</h3>
          <button class="collapse-btn">▼</button>
        </div>
        
        <div class="info-section">
          <h4>知识点卡片</h4>
          <div class="card-grid">
            <div class="info-card primary">
              <div class="card-icon">📚</div>
              <div class="card-title">知识点</div>
              <div class="card-subtitle">知识点学习</div>
            </div>
            <div class="info-card secondary">
              <div class="card-icon">📖</div>
              <div class="card-title">关联网课</div>
              <div class="card-subtitle">配套课程</div>
            </div>
          </div>
          
          <div class="card-grid">
            <div class="info-card primary">
              <div class="card-icon">🎓</div>
              <div class="card-title">关联网课</div>
              <div class="card-subtitle">知识点学习</div>
            </div>
            <div class="info-card secondary">
              <div class="card-icon">🔧</div>
              <div class="card-title">学习工具</div>
              <div class="card-subtitle">辅助学习</div>
            </div>
          </div>
        </div>
        
        <div class="info-section">
          <h4>关键区</h4>
          <div class="key-metrics">
            <div class="metric-item">
              <div class="metric-value">250</div>
              <div class="metric-label">分享文字</div>
              <div class="metric-icon">📤</div>
            </div>
            <div class="metric-item">
              <div class="metric-value">09:00:00</div>
              <div class="metric-label">总时长</div>
              <div class="metric-icon">⏱️</div>
            </div>
          </div>
          <div class="key-metrics">
            <div class="metric-item">
              <div class="metric-value">2:50</div>
              <div class="metric-label">分享文字</div>
              <div class="metric-icon">📤</div>
            </div>
            <div class="metric-item">
              <div class="metric-value">09:00:00</div>
              <div class="metric-label">适作二</div>
              <div class="metric-icon">⏱️</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部工具栏 -->
    <div class="study-footer">
      <div class="footer-left">
        <button class="footer-btn active">
          <span class="btn-icon">📖</span>
          <span class="btn-text">翻页</span>
        </button>
        <button class="footer-btn">
          <span class="btn-icon">📝</span>
          <span class="btn-text">笔记</span>
        </button>
        <button class="footer-btn">
          <span class="btn-icon">🌙</span>
          <span class="btn-text">夜间模式</span>
          <span class="btn-arrow">›</span>
        </button>
      </div>
      <div class="footer-right">
        <div class="ai-assistant">
          <div class="ai-avatar">🤖</div>
          <div class="ai-text">支持AI生成</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const goBack = () => {
  router.back()
}

onMounted(() => {
  // 可以在这里加载书籍内容
  console.log('Study page mounted')
})
</script>

<style scoped>
.study-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.study-header {
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn, .menu-btn, .search-btn {
  padding: 8px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover, .menu-btn:hover, .search-btn:hover {
  background: #e5e7eb;
}

.menu-btn.active {
  background: #3b82f6;
  color: white;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #e5e7eb;
}

.icon {
  font-size: 16px;
}

/* 主体内容区 */
.study-content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* 左侧目录区 */
.sidebar {
  width: 280px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f3f4f6;
}

.sidebar-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.collapse-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
}

.toc-tree {
  padding: 12px 0;
}

.toc-item {
  position: relative;
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.toc-item:hover {
  background: #f9fafb;
}

.toc-item.active {
  background: #eff6ff;
  color: #3b82f6;
}

.toc-title {
  font-size: 14px;
  display: block;
}

.toc-children {
  margin-left: 16px;
  margin-top: 4px;
}

/* 中间阅读区 */
.reading-area {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.reading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.reading-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.expand-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
}

.reading-content {
  flex: 1;
  line-height: 1.8;
  color: #374151;
  font-size: 15px;
}

.reading-content p {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 24px 0 16px 0;
}

.checklist {
  margin: 16px 0;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.check-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #3b82f6;
}

.create-btn {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin: 16px 0;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}

.pagination span {
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.pagination span:hover {
  background: #f3f4f6;
  color: #374151;
}

/* 右侧辅助信息区 */
.right-sidebar {
  width: 280px;
}

.info-section {
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
}

.info-section:last-child {
  border-bottom: none;
}

.info-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
}

.card-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.info-card {
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.info-card.primary {
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
  border: 1px solid #bfdbfe;
}

.info-card.secondary {
  background: linear-gradient(135deg, #d1fae5, #ecfdf5);
  border: 1px solid #a7f3d0;
}

.card-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 12px;
  color: #6b7280;
}

.key-metrics {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.metric-label {
  font-size: 12px;
  color: #6b7280;
  margin-left: 8px;
  flex: 1;
}

.metric-icon {
  font-size: 16px;
  color: #6b7280;
}

/* 底部工具栏 */
.study-footer {
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  bottom: 0;
  z-index: 100;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.footer-btn:hover {
  background: #e5e7eb;
}

.footer-btn.active {
  background: #3b82f6;
  color: white;
}

.btn-icon {
  font-size: 16px;
}

.btn-arrow {
  font-size: 12px;
  margin-left: 4px;
}

.ai-assistant {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-assistant:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.ai-avatar {
  font-size: 20px;
}

.ai-text {
  font-size: 14px;
  font-weight: 500;
  color: #92400e;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .study-content {
    flex-direction: column;
  }
  
  .sidebar,
  .right-sidebar {
    width: 100%;
  }
  
  .reading-area {
    order: -1;
  }
}
</style>