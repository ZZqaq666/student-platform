<template>
   <div class="main-layout"> 
     <!-- 全局顶部导航栏 --> 
     <nav class="navbar"> 
       <div class="nav-left"> 
         <span class="logo">📖 学科智问</span> 
       </div> 

       <!-- 中间导航链接 -->
       <div class="nav-middle" v-if="!isHomePage">
         <router-link to="qa" class="nav-link" :class="{ 'nav-active': isActive('/qa') }">问答中心</router-link>
         <router-link to="graph" class="nav-link" :class="{ 'nav-active': isActive('/graph') }">知识图谱</router-link>
         <router-link to="bookshelf" class="nav-link" :class="{ 'nav-active': isActive('/bookshelf') }">个人书架</router-link>
         <router-link to="senior" class="nav-link" :class="{ 'nav-active': isActive('/senior') }">学长学姐答疑</router-link>
       </div>

       <!-- 用户操作区 --> 
       <div v-if="!isLogin" class="nav-right"> 
         <router-link to="/login"> 
           <button class="login-btn">登录/注册</button> 
         </router-link> 
       </div> 
       <!-- 登录后显示用户信息 --> 
       <div v-else class="user-info"> 
         <!-- 闹铃图标 -->
         <div class="notification-icon" @click="toSeniorZone">
           <span class="bell-icon">🔔</span>
           <div v-if="hasNotification" class="notification-badge"></div>
           <div class="notification-tooltip">
             答疑消息有回复
           </div>
         </div>
         <div class="user-avatar">
           <div class="avatar-placeholder">{{ userName.charAt(0) }}</div>
         </div> 
         <span class="user-name">{{ userName }}</span> 
         <button class="logout-btn" @click="handleLogout">退出登录</button>
       </div> 
     </nav> 
 
     <!-- 页面主体内容区 - 路由页面将在这里渲染 --> 
     <main class="layout-main"> 
       <keep-alive include="KnowledgeGraph">
         <router-view /> 
       </keep-alive>
     </main> 
   </div> 
 </template>
 
 <script setup> 
import { computed, ref, watch } from 'vue' 
import { useRouter, useRoute } from 'vue-router' 
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 检查用户是否登录（通过localStorage中的token判断）
const token = ref(localStorage.getItem('token'))
const isLogin = computed(() => {
  return !!token.value
}) 

// 获取用户名（从localStorage中获取）
const userName = computed(() => {
  return localStorage.getItem('userName') || '用户'
})
 
 // 通知状态（模拟有新消息）
 const hasNotification = ref(true) 
 
 // 跳转到学长学姐答疑专区
 const toSeniorZone = () => {
   router.push('/senior')
   // 点击后清除通知
   hasNotification.value = false
 } 

 // 判断是否在首页
 const isHomePage = computed(() => {
   return route.path === '/'
 })

 // 判断导航链接是否激活
 const isActive = (path) => {
   return route.path === path
 }
 
 // 退出登录处理
 const handleLogout = async () => {
   try {
     // 显示确认对话框
     await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
       confirmButtonText: '确定',
       cancelButtonText: '取消',
       type: 'warning'
     })
     
     // 清除localStorage中的用户数据
     localStorage.removeItem('token')
     localStorage.removeItem('userName')
     localStorage.removeItem('rememberedUser')
     
     // 更新token ref
     token.value = null
     
     // 记录退出登录日志
     console.log('用户退出登录:', new Date().toISOString())
     
     // 重定向到登录页面
     router.replace('/login')
     
     // 显示退出成功消息
     ElMessage.success('退出登录成功')
   } catch (error) {
     // 如果用户取消操作，不显示错误信息
     if (error !== 'cancel') {
       console.error('退出登录失败:', error)
       ElMessage.error('退出登录失败，请重试')
     }
   }
 }
 </script> 
 
 <style scoped> 
 .main-layout { 
   min-height: 100vh; 
   background-color: #f3f4f6; 
   display: flex; 
   flex-direction: column; 
 } 
 
 /* 导航栏样式 */ 
 .navbar { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  padding: 0rem 2rem; 
  background-color: white; 
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); 
  position: sticky; 
  top: 0; 
  z-index: 999; 
  flex-wrap: wrap; 
  gap: 1rem; 
}

/* 确保导航栏在小屏幕上也能良好显示 */
@media (max-width: 768px) {
  .navbar {
    flex-direction: column;
    align-items: flex-start;
    padding: 1rem;
  }
  
  .nav-middle {
    width: 100%;
    justify-content: space-around;
    margin: 0.5rem 0;
  }
  
  .nav-right {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
  
  .user-info {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.5rem;
  }
  
  .logout-btn {
    padding: 0.25rem 0.5rem;
    font-size: 12px;
  }
} 
 
 .nav-left .logo { 
   font-size: 1.5rem; 
   font-weight: bold; 
   color: #2563eb; 
 } 
 
 .nav-middle { 
   display: flex; 
   gap: 1.5rem; 
 } 
 
 .nav-link { 
   text-decoration: none; 
   color: #374151; 
   font-weight: 500; 
   transition: color 0.3s; 
   padding: 0.5rem 0; 
   border-bottom: 2px solid transparent; 
 } 
 
 .nav-link:hover { 
   color: #2563eb; 
 } 
 
 .nav-active { 
   color: #2563eb; 
   border-bottom: 2px solid #2563eb; 
 } 
 
 .login-btn { 
   padding: 0.5rem 1rem; 
   background-color: #2563eb; 
   color: white; 
   border: none; 
   border-radius: 0.375rem; 
   cursor: pointer; 
   font-weight: 500; 
   transition: background-color 0.3s; 
 } 
 
 .login-btn:hover {
  background-color: #1d4ed8;
}

/* 用户信息样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 通知图标 */
.notification-icon {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  transition: background-color 0.3s ease;
}

.notification-icon:hover {
  background-color: #f3f4f6;
}

.bell-icon {
  font-size: 18px;
  position: relative;
  z-index: 1;
}

.notification-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 8px;
  height: 8px;
  background-color: #ef4444;
  border-radius: 50%;
  border: 2px solid white;
  z-index: 2;
}

.notification-tooltip {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #1f2937;
  color: white;
  font-size: 12px;
  border-radius: 8px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.notification-tooltip::before {
  content: '';
  position: absolute;
  bottom: 100%;
  right: 12px;
  border: 4px solid transparent;
  border-bottom-color: #1f2937;
}

.notification-icon:hover .notification-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateY(4px);
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-placeholder {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 50%;
  color: white;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.logout-btn {
  padding: 0.375rem 0.75rem;
  background-color: #f3f4f6;
  color: #374151;
  border: none;
  border-radius: 0.375rem;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.logout-btn:hover {
  background-color: #e5e7eb;
  color: #111827;
}

/* 主体内容区 */
.layout-main {
  flex: 1;
  width: 100%;
}
</style>