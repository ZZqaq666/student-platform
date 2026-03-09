<template>
  <div class="register-root">
    <!-- 顶部导航栏（参考效果图） -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <span class="logo-mark" />
          <span class="logo-text">学生学科智能问答平台</span>
        </div>
      
        <div class="nav-right">
          <router-link to="/login" class="nav-auth-link">登录</router-link>
          <router-link to="/register" class="nav-auth-link nav-auth-link--active">注册</router-link>
        </div>
      </div>
    </nav>

    <!-- 整页背景 + 内容区 -->
    <div class="register-page">
    <!-- 左侧图片区域 -->
    <div class="register-illustration">
      <img src="@/assets/学生学科问答平台图案美化.png" alt="学生学科智能问答平台">
    </div>

      <!-- 右侧注册卡片 -->
      <el-card class="register-card">
        <h2 class="register-title">用户注册</h2>
        <el-form :model="registerForm" :rules="rules" ref="registerFormRef" class="register-form">
          <!-- 用户名输入框 -->
          <el-form-item prop="username">
            <el-input 
              v-model="registerForm.username" 
              placeholder="请输入用户名" 
              prefix-icon="User"
              class="form-input"
            />
          </el-form-item>
          
          <!-- 密码输入框 -->
          <el-form-item prop="password">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="请输入密码" 
              prefix-icon="Lock"
              class="form-input"
            />
          </el-form-item>

          <!-- 确认密码输入框 -->
          <el-form-item prop="confirmPwd">
            <el-input 
              v-model="registerForm.confirmPwd" 
              type="password" 
              placeholder="请确认密码" 
              prefix-icon="Lock"
              class="form-input"
            />
          </el-form-item>

          <!-- 注册按钮 -->
          <el-form-item>
            <el-button type="primary" @click="handleRegister" class="register-btn">注册</el-button>
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

const router = useRouter()
const registerFormRef = ref()
const registerForm = ref({ username: '', password: '', confirmPwd: '' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z]+$/, message: '用户名只能由汉字或字母组成', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== registerForm.value.password) callback(new Error('两次密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

// 注册逻辑
const handleRegister = async () => {
  await registerFormRef.value.validate()
  // 存储用户信息到本地存储
  localStorage.setItem('user', JSON.stringify({
    username: registerForm.value.username,
    password: registerForm.value.password
  }))
  // 存储用户名
  localStorage.setItem('userName', registerForm.value.username)
  ElMessage.success('注册成功')
  setTimeout(() => router.push('/login'), 1000)
}
</script>

<style scoped>
:global(body) {
  margin: 0;
}

/* 整页根容器，固定铺满可视窗口，去掉滚动条 */
.register-root {
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
.register-page {
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

.register-illustration {
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

.register-illustration img {
  width: 600px;
  max-width: 100%;
  display: block;
  opacity: 0.8;
  filter: none;
  border: none;
  outline: none;
  box-shadow: none;
}

.register-card {
  width: 380px;
  padding: 32px 32px 26px;
  border-radius: 8px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
  border: none;
  margin-left: 40px;
}

.register-title {
  text-align: center;
  color: #1890ff;
  font-size: 20px;
  margin-bottom: 28px;
  font-weight: 600;
}

.register-form {
  width: 100%;
}

.form-input :deep(.el-input__wrapper) {
  border-radius: 4px;
  height: 40px;
}

.register-btn {
  width: 100%;
  height: 40px;
  border-radius: 4px;
  font-size: 15px;
  letter-spacing: 4px;
}
</style>