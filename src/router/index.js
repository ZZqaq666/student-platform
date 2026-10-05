import { createRouter, createWebHistory } from 'vue-router'
// 导入页面（懒加载）
const Login = () => import('@/views/Auth/Login.vue')
const Register = () => import('@/views/Auth/Register.vue')
const Home = () => import('@/views/Home/Home.vue')
const QA = () => import('@/views/Qa/QA.vue')
const KnowledgeGraph = () => import('@/views/KnowledgeGraph/KnowledgeGraph.vue')
const SeniorQA = () => import('@/views/Senior/SeniorQA.vue')

// 路由规则
const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { 
    path: '/', 
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: Home },
      { 
        path: 'qa', 
        name: 'QA', 
        component: QA
      },
      { path: 'bookshelf', name: 'Bookshelf', component: () => import('@/views/Bookshelf/Bookshelf.vue') },
      { path: 'practice', name: 'Practice', component: () => import('@/views/Practice/PracticePage.vue') },
      { path: 'wrong-book', name: 'WrongBook', component: () => import('@/views/WrongBook/WrongBook.vue') },
      { path: 'senior', name: 'SeniorQA', component: SeniorQA },
      { path: 'graph', name: 'KnowledgeGraph', component: KnowledgeGraph }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]


const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页
import { useAuthStore } from '@/stores/auth'

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isAuthenticated = authStore.checkAuth()
  
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

export default router