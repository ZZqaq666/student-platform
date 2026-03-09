import { createRouter, createWebHistory } from 'vue-router'
// 导入页面（懒加载）
const Login = () => import('@/views/Auth/Login.vue')
const Register = () => import('@/views/Auth/Register.vue')
const Home = () => import('@/views/Home/Home.vue')
const QA = () => import('@/views/QA/QA.vue')
const KnowledgeGraph = () => import('@/views/KnowledgeGraph/KnowledgeGraph.vue')

// 路由规则
const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { 
    path: '/', 
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: Home },
      { 
        path: 'qa', 
        name: 'QA', 
        component: QA
      },
      { path: 'bookshelf', name: 'Bookshelf', component: () => import('@/views/Bookshelf/Bookshelf.vue') },
      { path: 'graph', name: 'KnowledgeGraph', component: KnowledgeGraph },
      { path: 'practice', name: 'Practice', component: () => import('@/views/Practice/PracticePage.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]


const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页
router.beforeEach((to, from, next) => {
  const isLogin = localStorage.getItem('token') // 假设token存在localStorage
  if (to.meta.requiresAuth && !isLogin) {
    next('/login')
  } else {
    next()
  }
})

export default router