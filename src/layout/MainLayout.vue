<template>
  <div class="main-layout">
    <!-- 全局顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-left">
        <router-link to="/" class="logo-link">
          <span class="logo-icon">🎓</span>
          <span class="logo-text">学科<span class="logo-accent">智问</span></span>
        </router-link>
      </div>

      <!-- 中间导航链接 -->
      <div class="nav-middle" v-if="!isHomePage">
        <router-link v-for="link in navLinks" :key="link.path" :to="link.path"
          class="nav-link" :class="{ 'nav-active': isActive(link.path) }">
          <span class="nav-icon">{{ link.icon }}</span>
          <span>{{ link.label }}</span>
        </router-link>
      </div>

      <!-- 用户操作区 -->
      <div v-if="!isLogin" class="nav-right">
        <router-link to="/login" class="login-link">
          <button class="login-btn">登录 / 注册</button>
        </router-link>
      </div>

      <!-- 登录后显示用户信息 -->
      <div v-else class="user-info">
        <!-- 通知铃铛 -->
        <div class="notification-bell" @click="toSeniorZone">
          <span class="bell-icon">🔔</span>
          <span v-if="hasNotification" class="bell-dot" />
          <div class="bell-tooltip">答疑消息有回复</div>
        </div>

        <!-- 用户头像 -->
        <div class="user-avatar">
          <span class="avatar-text">{{ userName.charAt(0) }}</span>
        </div>

        <span class="user-name">{{ userName }}</span>

        <button class="logout-btn" @click="showLogoutModal = true">
          <span>退出</span>
        </button>
      </div>
    </nav>

    <!-- 页面主体内容区 -->
    <main class="layout-main">
      <keep-alive include="KnowledgeGraph">
        <router-view />
      </keep-alive>
    </main>

    <!-- 退出登录确认弹框 -->
    <div v-if="showLogoutModal" class="lo-overlay" @click="showLogoutModal = false">
      <div class="lo-dialog" @click.stop>
        <div class="lo-icon-wrap">
          <span class="lo-icon">👋</span>
        </div>
        <h3 class="lo-title">退出登录</h3>
        <p class="lo-desc">确定要退出当前账号吗？</p>
        <div class="lo-actions">
          <button class="lo-btn lo-btn--cancel" @click="showLogoutModal = false">取消</button>
          <button class="lo-btn lo-btn--confirm" @click="confirmLogout">确定退出</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const token = ref(localStorage.getItem('token'))
const showLogoutModal = ref(false)
const isLogin = computed(() => !!token.value)

const userName = computed(() => {
  return localStorage.getItem('userName') || '用户'
})

const hasNotification = ref(true)

const toSeniorZone = () => {
  router.push('/senior')
  hasNotification.value = false
}

const isHomePage = computed(() => route.path === '/')

const isActive = (path) => route.path === path

const navLinks = [
  { path: '/qa', label: '问答中心', icon: '💬' },
  { path: '/graph', label: '知识图谱', icon: '🌐' },
  { path: '/bookshelf', label: '个人书架', icon: '📚' },
  { path: '/senior', label: '学长答疑', icon: '👨‍🎓' },
]

const confirmLogout = () => {
  showLogoutModal.value = false
  localStorage.removeItem('token')
  localStorage.removeItem('userName')
  localStorage.removeItem('rememberedUser')
  token.value = null
  router.replace('/login')
  ElMessage.success('退出登录成功')
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: #f9fafd;
  display: flex;
  flex-direction: column;
}

/* ================================================
   导航栏 - 玻璃拟态精致版
   ================================================ */
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  height: 60px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow:
    0 1px 0 rgba(0, 0, 0, 0.05),
    0 4px 16px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 999;
}

/* ---- Logo ---- */
.nav-left { flex-shrink: 0; }

.logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.logo-icon {
  font-size: 24px;
  line-height: 1;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: 0.5px;
}

.logo-accent {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ---- 导航链接 ---- */
.nav-middle {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 5px;
  text-decoration: none;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: 10px;
  transition: all 0.25s ease;
  position: relative;
}

.nav-link:hover {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
}

.nav-active {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  font-weight: 600;
}

.nav-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  border-radius: 0 0 3px 3px;
}

.nav-icon {
  font-size: 15px;
  line-height: 1;
}

/* ---- 登录按钮 ---- */
.nav-right { flex-shrink: 0; }

.login-link { text-decoration: none; }

.login-btn {
  padding: 9px 22px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.login-btn:active {
  transform: translateY(0) scale(0.97);
}

/* ---- 用户信息区 ---- */
.user-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* 通知铃铛 */
.notification-bell {
  position: relative;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
}

.notification-bell:hover {
  background: #f1f5f9;
}

.bell-icon {
  font-size: 18px;
  line-height: 1;
  animation: bellRing 3s ease-in-out infinite;
}

@keyframes bellRing {
  0%, 90%, 100% { transform: rotate(0); }
  92% { transform: rotate(10deg); }
  94% { transform: rotate(-10deg); }
  96% { transform: rotate(6deg); }
  98% { transform: rotate(-6deg); }
}

.bell-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 9px;
  height: 9px;
  background: #ef4444;
  border-radius: 50%;
  border: 2px solid #fff;
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

.bell-tooltip {
  position: absolute;
  top: calc(100% + 8px);
  right: -20px;
  padding: 7px 14px;
  background: #1e293b;
  color: #fff;
  font-size: 12px;
  border-radius: 8px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.25s ease;
  z-index: 1000;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.bell-tooltip::before {
  content: '';
  position: absolute;
  bottom: 100%;
  right: 26px;
  border: 5px solid transparent;
  border-bottom-color: #1e293b;
}

.notification-bell:hover .bell-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateY(2px);
}

/* 用户头像 */
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.avatar-text {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

/* 用户名 */
.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 退出按钮 */
.logout-btn {
  padding: 7px 16px;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.25s ease;
}

.logout-btn:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fecaca;
}

/* ================================================
   内容区
   ================================================ */
.layout-main {
  flex: 1;
  width: 100%;
}

/* ================================================
   退出登录弹框
   ================================================ */
.lo-overlay {
  position: fixed; inset: 0; z-index: 3000;
  background: rgba(15,12,30,0.18);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  animation: loFadeIn .2s ease-out;
}
@keyframes loFadeIn { from { opacity: 0; } to { opacity: 1; } }

.lo-dialog {
  width: 360px; padding: 32px 28px 24px; border-radius: 20px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.6);
  box-shadow: 0 24px 64px rgba(80,60,160,0.12), 0 4px 12px rgba(0,0,0,0.04), 0 0 0 1px rgba(255,255,255,0.4) inset;
  text-align: center;
  animation: loScaleIn .25s cubic-bezier(.16,1,.3,1);
}
@keyframes loScaleIn { from { opacity: 0; transform: translateY(12px) scale(.95); } to { opacity: 1; transform: translateY(0) scale(1); } }

.lo-icon-wrap {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, rgba(99,102,241,.08), rgba(139,92,246,.06));
  border: 1px solid rgba(167,139,250,.15);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
}
.lo-icon { font-size: 24px; }

.lo-title {
  margin: 0 0 6px; font-size: 18px; font-weight: 800; color: #1e293b; letter-spacing: -0.2px;
}

.lo-desc {
  margin: 0 0 24px; font-size: 13px; color: #64748b; font-weight: 500;
}

.lo-actions { display: flex; gap: 10px; }

.lo-btn {
  flex: 1; padding: 10px 0; border-radius: 12px; border: none;
  font-size: 14px; font-weight: 600; font-family: inherit; cursor: pointer;
  transition: all .2s ease;
}

.lo-btn--cancel {
  background: rgba(241,245,249,.8); color: #64748b;
  border: 1px solid rgba(226,232,240,.6);
}
.lo-btn--cancel:hover { background: #e2e8f0; }

.lo-btn--confirm {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff; box-shadow: 0 4px 14px rgba(239,68,68,.25);
}
.lo-btn--confirm:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(239,68,68,.4); }
.lo-btn--confirm:active { transform: translateY(0) scale(.97); }

/* ================================================
   响应式
   ================================================ */
@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
    height: auto;
    min-height: 56px;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .nav-middle {
    order: 3;
    width: 100%;
    justify-content: center;
    overflow-x: auto;
  }

  .nav-link {
    font-size: 13px;
    padding: 6px 10px;
  }

  .user-info {
    gap: 8px;
  }

  .user-name {
    display: none;
  }
}
</style>
