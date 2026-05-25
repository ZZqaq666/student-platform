<template>
  <div class="login-root" @mousemove="onMouseMove">
    <!-- 顶部导航栏 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <span class="logo-mark" />
          <span class="logo-text">学生学科智能问答平台</span>
        </div>
        <div class="nav-right">
          <router-link to="/login" class="nav-auth-link nav-auth-link--active">登录</router-link>
          <router-link to="/register" class="nav-auth-link">注册</router-link>
        </div>
      </div>
    </nav>

    <!-- 内容区 -->
    <div class="login-page">
      <!-- AI 智能体 -->
      <div class="mascot-section" ref="mascotRef">
        <div class="mascot-scene" :style="sceneParallax">
          <!-- 环境光晕 -->
          <div class="ambient ambient--1" :style="ambient1Parallax"></div>
          <div class="ambient ambient--2" :style="ambient2Parallax"></div>
          <div class="ambient ambient--3"></div>

          <!-- 轨道环 -->
          <div class="orbit orbit--1" :style="orbit1Parallax"></div>
          <div class="orbit orbit--2" :style="orbit2Parallax"></div>
          <div class="orbit orbit--3" :style="orbit3Parallax"></div>

          <!-- 粒子场 -->
          <span
            v-for="i in 30"
            :key="'p' + i"
            class="dot"
            :style="dotStyle(i)"
          ></span>

          <!-- AI 主体 -->
          <div class="ai-entity" :style="entityParallax">
            <div class="entity-aura"></div>

            <div class="entity-head">
              <div class="head-glass"></div>
              <div class="head-highlight"></div>
              <div class="face-panel">
                <div class="face-eyes" :style="eyesParallax">
                  <div class="eye eye--l">
                    <div class="eye-beam"></div>
                  </div>
                  <div class="eye eye--r">
                    <div class="eye-beam"></div>
                  </div>
                </div>
                <div class="face-line"></div>
              </div>
            </div>

            <div class="entity-core">
              <div class="core-ring core-ring--1"></div>
              <div class="core-ring core-ring--2"></div>
              <div class="core-center"></div>
            </div>

            <div class="entity-shadow"></div>
          </div>
        </div>
      </div>

      <!-- 右侧登录卡片 -->
      <el-card class="login-card">
        <h2 class="login-title">
          <span class="title-char" v-for="(ch, idx) in '用户登录'" :key="idx" :style="{ animationDelay: idx * 0.08 + 's' }">{{ ch }}</span>
        </h2>
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              class="form-input"
            />
          </el-form-item>

          <div class="form-row">
            <el-checkbox v-model="loginForm.remember" class="remember-checkbox">
              记住密码
            </el-checkbox>
          </div>

          <el-form-item>
            <el-button type="primary" @click="handleLogin" class="login-btn">
              <span>登 录</span>
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiService } from '@/api/api.js'
import { useAuthStore } from '@/store/auth.js'

const loginApi = async (data) => {
  const response = await apiService.auth.login(data)
  return response
}

const router = useRouter()
const loginFormRef = ref()

const getStoredUser = () => {
  try {
    const stored = localStorage.getItem('rememberedUser')
    if (stored) {
      const user = JSON.parse(stored)
      if (user.expiry && Date.now() < user.expiry) return user
      else localStorage.removeItem('rememberedUser')
    }
  } catch (e) {
    localStorage.removeItem('rememberedUser')
  }
  return null
}

const storedUser = getStoredUser()
const loginForm = ref({
  username: storedUser?.username || '',
  password: storedUser?.password || '',
  remember: !!storedUser
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[一-龥a-zA-Z]+$/, message: '用户名只能由汉字或字母组成', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  await loginFormRef.value.validate()
  try {
    const { remember, ...data } = loginForm.value
    const res = await loginApi(data)
    if (res.code === 200) {
      const authStore = useAuthStore()
      authStore.setToken(res.data.token, res.data.expiry)
      authStore.setUser(res.data.user)
      localStorage.setItem('userName', loginForm.value.username)
      if (loginForm.value.remember) {
        const expiry = Date.now() + 10 * 24 * 60 * 60 * 1000
        localStorage.setItem('rememberedUser', JSON.stringify({
          username: loginForm.value.username,
          password: loginForm.value.password,
          expiry
        }))
      } else {
        localStorage.removeItem('rememberedUser')
      }
      router.push('/')
      ElMessage.success('登录成功')
    }
  } catch (error) {
    ElMessage.error(error.errorMessage || '登录失败')
  }
}

/* ===== 鼠标视差系统 (spring physics) ===== */
const mouseX = ref(window.innerWidth / 2)
const mouseY = ref(window.innerHeight / 2)
const mascotRef = ref(null)
const targetX = ref(0)
const targetY = ref(0)
const smoothX = ref(0)
const smoothY = ref(0)

const onMouseMove = (e) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

let rafId = null
const SPRING = 0.055

const tick = () => {
  if (mascotRef.value) {
    const r = mascotRef.value.getBoundingClientRect()
    const cx = r.left + r.width / 2
    const cy = r.top + r.height / 2
    targetX.value = ((mouseX.value - cx) / (r.width / 2)) * 0.7
    targetY.value = ((mouseY.value - cy) / (r.height / 2)) * 0.7
  }

  smoothX.value += (targetX.value - smoothX.value) * SPRING
  smoothY.value += (targetY.value - smoothY.value) * SPRING

  rafId = requestAnimationFrame(tick)
}

onMounted(() => { rafId = requestAnimationFrame(tick) })
onUnmounted(() => { if (rafId) cancelAnimationFrame(rafId) })

/* 各层级视差变换 */
const sceneParallax = computed(() => ({
  transform: `perspective(900px) rotateY(${smoothX.value * 4}deg) rotateX(${-smoothY.value * 3}deg)`,
}))

const entityParallax = computed(() => ({
  transform: `translate3d(${smoothX.value * 10}px, ${smoothY.value * 8}px, 0)`,
}))

const eyesParallax = computed(() => ({
  transform: `translate3d(${smoothX.value * 5}px, ${smoothY.value * 4}px, 0)`,
}))

const ambient1Parallax = computed(() => ({
  transform: `translate3d(${smoothX.value * -12}px, ${smoothY.value * -10}px, 0)`,
}))

const ambient2Parallax = computed(() => ({
  transform: `translate3d(${smoothX.value * 8}px, ${smoothY.value * -7}px, 0)`,
}))

const orbit1Parallax = computed(() => ({
  transform: `translate3d(${smoothX.value * -5}px, ${smoothY.value * -4}px, 0)`,
}))

const orbit2Parallax = computed(() => ({
  transform: `translate3d(${smoothX.value * 4}px, ${smoothY.value * -5}px, 0)`,
}))

const orbit3Parallax = computed(() => ({
  transform: `translate3d(${smoothX.value * -4}px, ${smoothY.value * 6}px, 0)`,
}))

/* 粒子样式 */
const dotStyle = (i) => {
  const angle = (i / 30) * Math.PI * 2 + Math.sin(i * 1.3) * 0.4
  const radius = 150 + Math.sin(i * 2.1) * 80
  const x = Math.cos(angle) * radius
  const y = Math.sin(angle) * radius
  return {
    '--x': `${x.toFixed(0)}px`,
    '--y': `${y.toFixed(0)}px`,
    '--size': `${2 + Math.random() * 3}px`,
    '--delay': `${Math.random() * 6}s`,
    '--duration': `${5 + Math.random() * 7}s`,
    '--drift': `${(Math.random() - 0.5) * 30}px`,
    '--opacity': (0.1 + Math.random() * 0.3).toFixed(2),
  }
}
</script>

<style scoped>
:global(body) { margin: 0; }

/* ===== 浅色渐变背景 ===== */
.login-root {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #e8f4fd 0%, #d4eafe 25%, #f0e6ff 50%, #d4eafe 75%, #e8f4fd 100%);
  background-size: 400% 400%;
  animation: bgShift 15s ease infinite;
}

@keyframes bgShift {
  0%, 100% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
}

/* ===== 导航栏 ===== */
.top-nav {
  height: 64px;
  background: linear-gradient(90deg, #1e9fff 0%, #32a3ff 40%, #4ab3ff 100%);
  box-shadow: 0 2px 12px rgba(30, 159, 255, 0.3);
  position: relative;
  z-index: 10;
}

.nav-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  padding: 0 20px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 600;
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: url('@/assets/智能云.png') no-repeat center/cover;
  margin-left: -6px;
  animation: logoPulse 3s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

.nav-right { display: flex; gap: 20px; font-size: 14px; }

.nav-auth-link {
  color: #e6f4ff;
  text-decoration: none;
  position: relative;
  padding-bottom: 4px;
  transition: color 0.3s;
}

.nav-auth-link:hover { color: #fff; }
.nav-auth-link--active { color: #fff; }

.nav-auth-link--active::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: 0;
  height: 2px;
  background-color: #fff;
  border-radius: 999px;
}

/* ===== 内容区 ===== */
.login-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  z-index: 1;
  gap: 20px;
}

/* ===== AI 智能体区域 ===== */
.mascot-section {
  flex: 0 0 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  margin-right: 10px;
  transform: translateX(-30px);
}

.mascot-scene {
  position: relative;
  width: 440px;
  height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease-out;
}

/* ---- 环境光晕 ---- */
.ambient {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  transition: transform 0.15s ease-out;
}

.ambient--1 {
  width: 300px;
  height: 300px;
  background: rgba(99,102,241,0.2);
  top: 20px;
  left: 30px;
}

.ambient--2 {
  width: 220px;
  height: 220px;
  background: rgba(59,130,246,0.18);
  top: 120px;
  right: 50px;
}

.ambient--3 {
  width: 160px;
  height: 160px;
  background: rgba(139,92,246,0.14);
  bottom: 60px;
  left: 50%;
  transform: translateX(-50%);
}

/* ---- 轨道环 ---- */
.orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(30,159,255,0.12);
  pointer-events: none;
  transition: transform 0.2s ease-out;
}

.orbit--1 {
  width: 380px;
  height: 380px;
  animation: orbitSpin 22s linear infinite;
}

.orbit--2 {
  width: 430px;
  height: 430px;
  border-color: rgba(114,46,209,0.08);
  animation: orbitSpin 28s linear infinite reverse;
}

.orbit--3 {
  width: 340px;
  height: 200px;
  border-radius: 50%;
  border-color: rgba(99,102,241,0.1);
  animation: orbitSpin 18s linear infinite;
  transform: rotateX(70deg);
}

@keyframes orbitSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ---- 粒子 ---- */
.dot {
  position: absolute;
  top: 50%;
  left: 50%;
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  background: rgba(30,64,120,var(--opacity));
  box-shadow: 0 0 4px rgba(30,100,180,calc(var(--opacity) * 0.4));
  transform: translate(var(--x), var(--y));
  animation: dotFloat var(--duration) var(--delay) infinite ease-in-out;
  pointer-events: none;
}

@keyframes dotFloat {
  0%, 100% {
    transform: translate(var(--x), var(--y));
    opacity: var(--opacity);
  }
  25% {
    transform: translate(calc(var(--x) + var(--drift)), calc(var(--y) - var(--drift)));
    opacity: calc(var(--opacity) * 1.8);
  }
  50% {
    transform: translate(calc(var(--x) - var(--drift) * 0.6), calc(var(--y) + var(--drift) * 0.8));
  }
  75% {
    transform: translate(calc(var(--x) + var(--drift) * 0.5), calc(var(--y) + var(--drift) * 0.3));
    opacity: calc(var(--opacity) * 1.4);
  }
}

/* ---- AI 主体 ---- */
.ai-entity {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
  animation: entityFloat 5s ease-in-out infinite;
  transition: transform 0.12s ease-out;
}

@keyframes entityFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}

/* 外层光晕 */
.entity-aura {
  position: absolute;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99,102,241,0.16), transparent 70%);
  filter: blur(20px);
  top: -20px;
  animation: auraBreathe 4s ease-in-out infinite;
  pointer-events: none;
}

@keyframes auraBreathe {
  0%, 100% { transform: scale(1); opacity: 0.7; }
  50% { transform: scale(1.12); opacity: 1; }
}

/* 头部球体 */
.entity-head {
  position: relative;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
}

/* 毛玻璃球壳 */
.head-glass {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(
    circle at 35% 28%,
    rgba(255,255,255,0.7),
    rgba(255,255,255,0.25) 45%,
    rgba(255,255,255,0.4) 100%
  );
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255,255,255,0.6);
  box-shadow:
    0 0 60px rgba(99,102,241,0.15),
    0 0 30px rgba(59,130,246,0.1),
    inset 0 0 30px rgba(255,255,255,0.3),
    inset 0 1px 0 rgba(255,255,255,0.7);
}

/* 高光 */
.head-highlight {
  position: absolute;
  top: 28px;
  left: 42px;
  width: 60px;
  height: 30px;
  background: radial-gradient(ellipse, rgba(255,255,255,0.9), transparent);
  border-radius: 50%;
  transform: rotate(-20deg);
  pointer-events: none;
  z-index: 5;
}

/* 面部面板 */
.face-panel {
  position: relative;
  z-index: 4;
  width: 120px;
  height: 90px;
  border-radius: 36px;
  background: radial-gradient(
    circle at center,
    rgba(20,24,48,0.88),
    rgba(12,16,36,0.94)
  );
  border: 1px solid rgba(255,255,255,0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  box-shadow:
    inset 0 0 30px rgba(59,130,246,0.08),
    0 0 30px rgba(99,102,241,0.1);
}

/* 眼睛 */
.face-eyes {
  display: flex;
  gap: 28px;
  transition: transform 0.1s ease-out;
}

.eye {
  width: 8px;
  height: 20px;
  border-radius: 4px;
  background: #818cf8;
  position: relative;
  box-shadow:
    0 0 12px rgba(129,140,248,0.7),
    0 0 24px rgba(129,140,248,0.35),
    0 0 48px rgba(99,102,241,0.2);
  animation: eyeGlow 3.5s ease-in-out infinite;
}

.eye--r { animation-delay: 0.4s; }

@keyframes eyeGlow {
  0%, 100% { box-shadow: 0 0 12px rgba(129,140,248,0.7), 0 0 24px rgba(129,140,248,0.35), 0 0 48px rgba(99,102,241,0.2); }
  50% { box-shadow: 0 0 18px rgba(129,140,248,0.9), 0 0 36px rgba(129,140,248,0.5), 0 0 60px rgba(99,102,241,0.3); }
}

.eye-beam {
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  background: transparent;
  box-shadow: inset 0 0 6px rgba(255,255,255,0.6);
}

/* 表情线 */
.face-line {
  width: 28px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, rgba(148,163,184,0.5), transparent);
}

/* 能量核心 */
.entity-core {
  position: relative;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -6px;
  z-index: 2;
}

.core-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(129,140,248,0.25);
  animation: coreSpin 8s linear infinite;
}

.core-ring--1 {
  width: 56px;
  height: 56px;
}

.core-ring--2 {
  width: 44px;
  height: 44px;
  animation-direction: reverse;
  animation-duration: 6s;
  border-color: rgba(59,130,246,0.2);
}

@keyframes coreSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.core-center {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle, #a5b4fc, #6366f1);
  box-shadow:
    0 0 20px rgba(129,140,248,0.6),
    0 0 40px rgba(99,102,241,0.3);
  animation: corePulse 2.8s ease-in-out infinite;
}

@keyframes corePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.25); }
}

/* 悬浮阴影 */
.entity-shadow {
  width: 160px;
  height: 24px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(99,102,241,0.2), transparent);
  filter: blur(14px);
  margin-top: 8px;
  animation: shadowBreathe 5s ease-in-out infinite;
}

@keyframes shadowBreathe {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(0.7); opacity: 0.25; }
}

/* ---- hover ---- */
.ai-entity:hover {
  animation: entityFloatHover 5s ease-in-out infinite;
}

.ai-entity:hover .entity-head {
  filter: brightness(1.05);
}

.ai-entity:hover .head-glass {
  box-shadow:
    0 0 80px rgba(99,102,241,0.22),
    0 0 40px rgba(59,130,246,0.15),
    inset 0 0 40px rgba(255,255,255,0.4),
    inset 0 1px 0 rgba(255,255,255,0.8);
}

@keyframes entityFloatHover {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-16px) scale(1.02); }
}

/* ===== 登录卡片 ===== */
.login-card {
  width: 400px;
  padding: 36px 36px 28px !important;
  border-radius: 16px !important;
  border: 1px solid rgba(255,255,255,0.6) !important;
  background: rgba(255,255,255,0.65) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(30,159,255,0.12), 0 2px 8px rgba(0,0,0,0.06), inset 0 0 0 1px rgba(255,255,255,0.5) !important;
  animation: cardEnter 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  flex-shrink: 0;
}

@keyframes cardEnter {
  0% { opacity: 0; transform: translateY(40px) scale(0.94); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.login-title {
  text-align: center;
  font-size: 26px;
  margin-bottom: 32px;
  font-weight: 700;
}

.title-char {
  display: inline-block;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: charBounce 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes charBounce {
  0% { opacity: 0; transform: translateY(-20px) scale(0.3); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.login-form { width: 100%; }

.form-input :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  height: 46px;
  background: rgba(255,255,255,0.7) !important;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04) !important;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.form-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(30,159,255,0.12) !important;
  transform: translateY(-1px);
}

.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(30,159,255,0.15), 0 4px 16px rgba(30,159,255,0.12) !important;
  background: rgba(255,255,255,0.95) !important;
}

.form-input :deep(.el-input__prefix) {
  color: #94a3b8;
  transition: transform 0.3s ease;
}

.form-input:focus-within :deep(.el-input__prefix) {
  transform: scale(1.15);
  color: #1890ff;
}

.form-row {
  display: flex;
  align-items: center;
  font-size: 13px;
  margin: 4px 0 18px;
}

.remember-checkbox { color: #666; }

.login-btn {
  width: 100% !important;
  height: 46px !important;
  border-radius: 10px !important;
  font-size: 16px !important;
  letter-spacing: 6px !important;
  font-weight: 600 !important;
  border: none !important;
  background: linear-gradient(135deg, #1890ff, #722ed1) !important;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease !important;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24,144,255,0.4) !important;
}

.login-btn:active { transform: translateY(0) scale(0.98) !important; }

.login-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  transform: translateX(-100%);
  animation: btnShimmer 2.5s infinite;
}

@keyframes btnShimmer {
  100% { transform: translateX(100%); }
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .mascot-section { display: none; }
  .login-card { margin: 0 !important; width: 92%; max-width: 400px; }
}
</style>
