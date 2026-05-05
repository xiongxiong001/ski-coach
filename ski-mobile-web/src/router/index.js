import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  // 登录注册
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false, title: '登录', noTab: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/Register.vue'),
    meta: { requiresAuth: false, title: '注册', noTab: true }
  },

  // 主布局(含底部TabBar)
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页', tab: 'home' }
      },
      {
        path: 'videos',
        name: 'VideoList',
        component: () => import('@/views/video/VideoList.vue'),
        meta: { title: '我的视频', tab: 'videos' }
      },
      {
        path: 'comparisons',
        name: 'ComparisonList',
        component: () => import('@/views/comparison/ComparisonList.vue'),
        meta: { title: '对比报告', tab: 'comparisons' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '我的', tab: 'profile' }
      }
    ]
  },

  // 详情页(无TabBar,沉浸体验)
  {
    path: '/videos/:id',
    name: 'VideoDetail',
    component: () => import('@/views/video/VideoDetail.vue'),
    meta: { requiresAuth: true, title: '视频详情', noTab: true }
  },
  {
    path: '/reports/:id',
    name: 'ReportDetail',
    component: () => import('@/views/report/ReportDetail.vue'),
    meta: { requiresAuth: true, title: 'AI 教练报告', noTab: true }
  },
  {
    path: '/comparison/create',
    name: 'ComparisonCreate',
    component: () => import('@/views/comparison/ComparisonCreate.vue'),
    meta: { requiresAuth: true, title: '创建对比', noTab: true }
  },
  {
    path: '/comparison/waiting',
    name: 'ComparisonWaiting',
    component: () => import('@/views/comparison/ComparisonWaiting.vue'),
    meta: { requiresAuth: true, title: '生成中', noTab: true }
  },
  {
    path: '/comparisons/:id',
    name: 'ComparisonDetail',
    component: () => import('@/views/comparison/ComparisonDetail.vue'),
    meta: { requiresAuth: true, title: '对比报告', noTab: true }
  },

  // 用户反馈
  {
    path: '/feedback',
    name: 'FeedbackCreate',
    component: () => import('@/views/feedback/FeedbackCreate.vue'),
    meta: { requiresAuth: true, title: '用户反馈', noTab: true }
  },
  {
    path: '/feedback/list',
    name: 'FeedbackList',
    component: () => import('@/views/feedback/FeedbackList.vue'),
    meta: { requiresAuth: true, title: '我的反馈', noTab: true }
  },
  {
    path: '/feedback/:id',
    name: 'FeedbackDetail',
    component: () => import('@/views/feedback/FeedbackDetail.vue'),
    meta: { requiresAuth: true, title: '反馈详情', noTab: true }
  },

  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta?.title ? `${to.meta.title} - Ski Coach` : 'Ski Coach'

  const userStore = useUserStore()
  if (to.meta.requiresAuth === false) {
    if (userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
      return next({ name: 'Home' })
    }
    return next()
  }
  if (!userStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
