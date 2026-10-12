import { createMemoryHistory, createRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import Layout from '@/views/Layout.vue'
import Login from '@/views/Login.vue'
import Dashboard from '@/views/Dashboard.vue'
import Clients from '@/views/Clients.vue'
import ClientDetail from '@/views/ClientDetail.vue'
import Monitor from '@/views/Monitor.vue'
import Debug from '@/views/Debug.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: Dashboard, meta: { title: '仪表盘', requiresAuth: true } },
      { path: 'clients', name: 'Clients', component: Clients, meta: { title: '客户端管理', requiresAuth: true } },
      { path: 'clients/:clientId', name: 'ClientDetail', component: ClientDetail, meta: { title: '客户端详情', requiresAuth: true } },
      { path: 'monitor', name: 'Monitor', component: Monitor, meta: { title: '状态监控', requiresAuth: true } },
      { path: 'debug', name: 'Debug', component: Debug, meta: { title: 'MQTT 调试', requiresAuth: true } }
    ]
  }
]

export const createDashboardRouter = () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes
  })

  router.beforeEach((to) => {
    const authStore = useAuthStore()
    authStore.initAuth()
    if (to.meta.requiresAuth !== false && !authStore.isAuthenticated) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/dashboard'
    }
    return true
  })
  return router
}
