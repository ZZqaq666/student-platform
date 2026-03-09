<template>
  <div class="login-root">
    <!-- 顶部导航栏（参考效果图） -->
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

    <!-- 整页背景 + 内容区 -->
    <div class="login-page">
    <!-- 左侧图片区域 -->
    <div class="login-illustration">
      <img src="@/assets/学生学科问答平台图案美化.png" alt="学生学科智能问答平台">
    </div>

      <!-- 右侧登录卡片 -->
      <el-card class="login-card">
        <h2 class="login-title">用户登录</h2>
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <!-- 用户名 -->
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              class="form-input"
            />
          </el-form-item>

          <!-- 密码 -->
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              class="form-input"
            />
          </el-form-item>

          <!-- 记住密码 -->
          <div class="form-row">
            <el-checkbox v-model="loginForm.remember" class="remember-checkbox">
              记住密码
            </el-checkbox>
          </div>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button type="primary" @click="handleLogin" class="login-btn">登 录</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

// 模拟登录接口
const loginApi = (data) => new Promise((resolve, reject) => {
  setTimeout(() => {
    // 从本地存储获取注册的用户信息
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      const user = JSON.parse(storedUser)
      if (user.username === data.username && user.password === data.password) {
        resolve({ code: 200, data: { token: 'test_token' } })
      } else {
        reject(new Error('用户名或密码错误'))
      }
    } else {
      reject(new Error('用户未注册'))
    }
  }, 500)
})

const router = useRouter()
const loginFormRef = ref()
// 初始化登录表单，检查localStorage中是否有存储的用户信息
const getStoredUser = () => {
  try {
    const stored = localStorage.getItem('rememberedUser')
    if (stored) {
      const user = JSON.parse(stored)
      // 检查是否过期
      if (user.expiry && Date.now() < user.expiry) {
        return user
      } else {
        localStorage.removeItem('rememberedUser')
        return null
      }
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
  remember: !!storedUser // 如果有存储的用户信息，则默认勾选
})
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z]+$/, message: '用户名只能由汉字或字母组成', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ]
}

// 登录逻辑
const handleLogin = async () => {
  await loginFormRef.value.validate()
  try {
    const res = await loginApi(loginForm.value)
    if (res.code === 200) {
        localStorage.setItem('token', res.data.token)
        // 存储用户名
        localStorage.setItem('userName', loginForm.value.username)
        // 处理记住用户名和密码功能
        if (loginForm.value.remember) {
          // 设置10天的过期时间
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
    ElMessage.error(error.message)
  }
}
</script>

<style scoped>
:global(body) {
  margin: 0;
}

/* 整页根容器，固定铺满可视窗口，去掉滚动条 */
.login-root {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
}
/* 顶部导航栏 */
.top-nav {
  height: 64px;
  background: linear-gradient(90deg, #1e9fff 0%, #32a3ff 40%, #4ab3ff 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.nav-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
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
  margin-left: -6px; /* 使图标略微向左贴边 */
  /* 去掉外层圆形框，只保留图标本身 */
}

.nav-center {
  display: flex;
  gap: 26px;
  font-size: 14px;
}

.nav-link {
  color: #e6f4ff;
  text-decoration: none;
  position: relative;
  padding-bottom: 4px;
  transition: color 0.2s;
}

.nav-link:hover {
  color: #ffffff;
}

.nav-right {
  display: flex;
  gap: 20px;
  font-size: 14px;
}

.nav-auth-link {
  color: #e6f4ff;
  text-decoration: none;
  position: relative;
  padding-bottom: 4px;
}

.nav-auth-link--active {
  color: #ffffff;
}

.nav-auth-link--active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background-color: #ffffff;
  border-radius: 999px;
}

/* 整页背景和布局（使用本地登录背景图，尽量保持清晰） */
.login-page {
  flex: 1;
  background-image: url('@/assets/登录背景.png');
  background-repeat: no-repeat;
  background-position: center top;
  background-size: 100% auto; /* 按宽度铺满，保持原图比例，避免过度拉伸 */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.login-illustration {
  margin-right: 80px;
  margin-left: -100px;
  margin-bottom: 40px;
  width: 600px;
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 0;
  border: none;
  background: transparent;
  overflow: visible;
}

.login-illustration img {
  width: 600px;
  max-width: 100%;
  display: block;
  opacity: 0.8;
  filter: none;
  border: none;
  outline: none;
  box-shadow: none;
}

.login-card {
  width: 380px;
  padding: 32px 32px 26px;
  border-radius: 8px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
  border: none;
  margin-left: 40px;
}

.login-title {
  text-align: center;
  color: #1890ff;
  font-size: 20px;
  margin-bottom: 28px;
  font-weight: 600;
}

.login-form {
  width: 100%;
}

.form-input :deep(.el-input__wrapper) {
  border-radius: 4px;
  height: 40px;
}

.form-row {
  display: flex;
  align-items: center;
  font-size: 12px;
  margin: 4px 0 18px;
}

.remember-checkbox {
  color: #666;
}

.login-btn {
  width: 100%;
  height: 40px;
  border-radius: 4px;
  font-size: 15px;
  letter-spacing: 4px;
}
</style>