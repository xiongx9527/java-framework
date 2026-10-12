import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import { createDashboardRouter } from './router'
import { setCredentialsProvider, setUnauthorizedHandler } from './utils/api'
import { useAuthStore } from './stores/auth'
import { useDebugStore } from './stores/debug'
import { useMonitorStore } from './stores/monitor'

export const createMicaMqttDashboard = (target: Element | null) => {
  if (!target) throw new Error('MQTT Dashboard 挂载节点不存在')

  const app = createApp(App)
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  const pinia = createPinia()
  const router = createDashboardRouter()
  app.use(pinia)
  setCredentialsProvider(() => useAuthStore(pinia).getApiCredentials())
  setUnauthorizedHandler(() => useAuthStore(pinia).logout())
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })
  app.mount(target)
  router.replace('/dashboard')

  return {
    unmount() {
      useMonitorStore(pinia).stopRealtimeMonitor()
      useDebugStore(pinia).disconnect(true)
      app.unmount()
    }
  }
}
