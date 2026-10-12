import { computed, ref } from 'vue'
import type { AxiosRequestConfig } from 'axios'
import { defineStore } from 'pinia'
import { api } from '@/utils/api'
import { setServerContext, setServerUnavailableHandler } from '@/context'
import { useClientStore } from '@/stores/client'
import { useDebugStore } from '@/stores/debug'
import { useMonitorStore } from '@/stores/monitor'

const SERVER_STORAGE_KEY = 'magic-mqtt-dashboard-server'

export interface MqttDashboardServer {
  id: string
  key: string
  name: string
  ip: string
  port: number
  webPort: number
  websocketEnabled: boolean
  sslEnabled: boolean
  running: boolean
  defaultServer: boolean
}

export const useServerStore = defineStore('server', () => {
  const servers = ref<MqttDashboardServer[]>([])
  const selectedServerId = ref('')
  const loading = ref(false)
  const error = ref('')
  const initialized = ref(false)
  const generation = ref(0)

  const selectedServer = computed(() => servers.value.find(server => server.id === selectedServerId.value) || null)

  setServerUnavailableHandler((message: string) => {
    applyServer(null)
    error.value = message || '当前 MQTT 服务端已停止或删除，请重新选择'
  })

  const resetBoundState = () => {
    useMonitorStore().stopRealtimeMonitor()
    useMonitorStore().resetForServer()
    useDebugStore().disconnect(true)
    useDebugStore().resetForServer()
    useClientStore().resetForServer()
  }

  const applyServer = (server: MqttDashboardServer | null) => {
    resetBoundState()
    selectedServerId.value = server?.id || ''
    generation.value = setServerContext(server)
    if (server) sessionStorage.setItem(SERVER_STORAGE_KEY, server.id)
    else sessionStorage.removeItem(SERVER_STORAGE_KEY)
    if (server) useDebugStore().bindServer(server)
  }

  const selectServer = (serverId: string) => {
    const server = servers.value.find(item => item.id === serverId)
    if (!server) {
      applyServer(null)
      error.value = 'MQTT 服务端不存在或已停止，请重新选择'
      return false
    }
    error.value = ''
    applyServer(server)
    return true
  }

  const loadServers = async (requestConfig: AxiosRequestConfig = {}) => {
    loading.value = true
    error.value = ''
    try {
      const response = await api.get<{ code: number; data: MqttDashboardServer[] }>('/mqtt/dashboard/servers', requestConfig)
      const nextServers = Array.isArray(response.data.data) ? response.data.data : []
      const previousId = selectedServerId.value
      servers.value = nextServers

      if (!initialized.value) {
        const restoredId = sessionStorage.getItem(SERVER_STORAGE_KEY)
        const initial = nextServers.find(server => server.id === restoredId)
          || nextServers.find(server => server.defaultServer)
          || nextServers[0]
          || null
        applyServer(initial)
        initialized.value = true
      } else if (previousId && !nextServers.some(server => server.id === previousId)) {
        applyServer(null)
        error.value = '当前 MQTT 服务端已停止或删除，请重新选择'
      }
    } catch (cause: any) {
      error.value = cause?.message || '加载 MQTT 服务端失败'
    } finally {
      loading.value = false
    }
  }

  const initialize = async () => {
    if (!initialized.value) await loadServers()
  }

  return {
    servers,
    selectedServerId,
    selectedServer,
    loading,
    error,
    initialized,
    generation,
    initialize,
    loadServers,
    selectServer
  }
})
