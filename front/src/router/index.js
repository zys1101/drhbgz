import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'
import { roleHome } from '../constants/aqi'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { roles: ['admin'] }
  },
  // ---------- NEPS 公众监督员端 ----------
  {
    path: '/sf/address',
    name: 'sfAddress',
    component: () => import('../views/supervisor/AddressView.vue'),
    meta: { roles: ['supervisor'] }
  },
  {
    path: '/sf/submit',
    name: 'sfSubmit',
    component: () => import('../views/supervisor/FeedbackSubmitView.vue'),
    meta: { roles: ['supervisor'] }
  },
  {
    path: '/sf/history',
    name: 'sfHistory',
    component: () => import('../views/supervisor/MyFeedbackView.vue'),
    meta: { roles: ['supervisor'] }
  },
  // ---------- NEPG 网格员端 ----------
  {
    path: '/gw/tasks',
    name: 'gwTasks',
    component: () => import('../views/gridworker/TaskListView.vue'),
    meta: { roles: ['grid'] }
  },
  {
    path: '/gw/measure',
    name: 'gwMeasure',
    component: () => import('../views/gridworker/TaskMeasureView.vue'),
    meta: { roles: ['grid'] }
  },
  {
    path: '/gw/leave',
    name: 'gwLeave',
    component: () => import('../views/gridworker/LeaveView.vue'),
    meta: { roles: ['grid'] }
  },
  // ---------- NEPM 系统管理端 ----------
  {
    path: '/admin/feedback',
    name: 'adminFeedback',
    component: () => import('../views/admin/FeedbackManageView.vue'),
    meta: { roles: ['admin'] }
  },
  {
    path: '/admin/aqiData',
    name: 'adminAqiData',
    component: () => import('../views/admin/AqiDataView.vue'),
    meta: { roles: ['admin'] }
  },
  {
    path: '/admin/stats',
    name: 'adminStats',
    component: () => import('../views/admin/StatsView.vue'),
    meta: { roles: ['admin'] }
  },
  // ---------- 人员管理（HR）----------
  {
    path: '/admin/hr/grid',
    name: 'hrGrid',
    component: () => import('../views/admin/HrGridView.vue'),
    meta: { roles: ['admin'] }
  },
  {
    path: '/admin/hr/supervisor',
    name: 'hrSupervisor',
    component: () => import('../views/admin/HrSupervisorView.vue'),
    meta: { roles: ['admin'] }
  },
  {
    path: '/aqi',
    name: 'aqi',
    component: () => import('../views/AqiList.vue'),
    meta: { roles: ['admin'] }
  },
  {
    path: '/aqiForm',
    name: 'aqiForm',
    component: () => import('../views/AqiForm.vue'),
    meta: { roles: ['admin'], hideInMenu: true }
  },
  // ---------- NEPV 决策者端 ----------
  {
    path: '/screen',
    name: 'screen',
    component: () => import('../views/ScreenView.vue'),
    meta: { roles: ['viewer', 'admin'], fullscreen: true }
  },
  // 兼容旧路径
  { path: '/aqiFeedback', redirect: '/admin/feedback' },
  { path: '/about', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 全局守卫：未登录去登录页；已登录按角色访问；登录后访问 /login 回角色首页
router.beforeEach(to => {
  const isLoggedIn = store.getters.isLoggedIn
  const role = store.getters.role

  if (to.meta.public) {
    if (isLoggedIn) {
      return roleHome(role)
    }
    return true
  }

  if (!isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 角色不匹配则送回自己角色的首页
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return roleHome(role)
  }
  return true
})

export default router
