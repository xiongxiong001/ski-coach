import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  // 登录注册(无需鉴权)
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/Register.vue'),
    meta: { requiresAuth: false, title: '注册' }
  },

  // 主布局(需鉴权)
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'videos',
        name: 'VideoList',
        component: () => import('@/views/video/VideoList.vue'),
        meta: { title: '我的视频' }
      },
      {
        path: 'videos/:id',
        name: 'VideoDetail',
        component: () => import('@/views/video/VideoDetail.vue'),
        meta: { title: '视频详情' }
      },
      {
        path: 'reports/:id',
        name: 'ReportDetail',
        component: () => import('@/views/report/ReportDetail.vue'),
        meta: { title: '教练报告' }
      },
      {
        path: 'comparisons',
        name: 'ComparisonList',
        component: () => import('@/views/comparison/ComparisonList.vue'),
        meta: { title: '对比报告' }
      },
      {
        path: 'comparison/create',
        name: 'ComparisonCreate',
        component: () => import('@/views/comparison/ComparisonCreate.vue'),
        meta: { title: '创建对比' }
      },
      {
        path: 'comparisons/:id',
        name: 'ComparisonDetail',
        component: () => import('@/views/comparison/ComparisonDetail.vue'),
        meta: { title: '对比详情' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },

  // 404 兜底
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta?.title ? `${to.meta.title} - Ski Coach` : 'Ski Coach'

  const userStore = useUserStore()

  // 不需要鉴权的页面
  if (to.meta.requiresAuth === false) {
    // 已登录用户访问登录/注册页 → 跳到首页
    if (userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
      return next({ name: 'Home' })
    }
    return next()
  }

  // 需要鉴权但未登录 → 跳到登录
  if (!userStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  next()
})

export default router
