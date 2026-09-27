import type { MqttDashboardServer } from '@/stores/server'

let selectedServerId = ''
let selectedServer: MqttDashboardServer | null = null
let generation = 0
let serverUnavailableHandler: ((message: string) => void) | null = null

export const getServerContext = () => ({ selectedServerId, selectedServer, generation })

export const setServerContext = (server: MqttDashboardServer | null) => {
  generation += 1
  selectedServer = server
  selectedServerId = server?.id || ''
  return generation
}

export const assertCurrentGeneration = (expected: number) => {
  if (expected !== generation) {
    throw new Error('MQTT 服务端已切换，忽略旧请求结果')
  }
}

export const setServerUnavailableHandler = (handler: ((message: string) => void) | null) => {
  serverUnavailableHandler = handler
}

export const notifyServerUnavailable = (message: string) => {
  serverUnavailableHandler?.(message)
}
