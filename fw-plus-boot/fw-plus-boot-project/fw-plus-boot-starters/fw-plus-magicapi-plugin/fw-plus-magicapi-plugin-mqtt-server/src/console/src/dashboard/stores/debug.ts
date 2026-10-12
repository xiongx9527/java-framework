import { defineStore } from 'pinia'
import { ref } from 'vue'
import mqtt from 'mqtt'
import type { MqttClient } from 'mqtt'
import type { MqttDashboardServer } from '@/stores/server'

export interface MqttMessage {
  id: string
  timestamp: number
  topic: string
  payload: string
  qos: number
  retain: boolean
  direction: 'in' | 'out'
}

export interface MqttConnection {
  clientId: string
  host: string
  port: number
  endpoint: string
  username?: string
  password?: string
  clean: boolean
  keepalive: number
}

const defaultConnection = (): MqttConnection => ({
  clientId: `mica_web_${Date.now()}`,
  host: '',
  port: 0,
  endpoint: '',
  clean: true,
  keepalive: 60
})

export const useDebugStore = defineStore('debug', () => {
  const messages = ref<MqttMessage[]>([])
  const connection = ref<MqttConnection>(defaultConnection())
  const mqttClient = ref<MqttClient | null>(null)
  const connected = ref(false)
  const connecting = ref(false)
  const subscriptions = ref<string[]>([])
  const maxMessages = ref(1000)
  const boundServerId = ref('')
  let connectionAttempt = 0

  const resolveHost = (server: MqttDashboardServer) => {
    const ip = server.ip
    if (!ip || ip === '0.0.0.0' || ip === '::' || ip === '::0') return window.location.hostname
    return ip
  }

  const bindServer = (server: MqttDashboardServer) => {
    disconnect(true)
    const host = resolveHost(server)
    const protocol = server.sslEnabled ? 'wss' : 'ws'
    const formattedHost = host.includes(':') && !host.startsWith('[') ? `[${host}]` : host
    boundServerId.value = server.id
    connection.value = {
      ...defaultConnection(),
      host,
      port: server.webPort,
      endpoint: server.websocketEnabled && server.webPort > 0 ? `${protocol}://${formattedHost}:${server.webPort}` : ''
    }
  }

  const connect = async () => {
    if (connecting.value || connected.value) return
    const endpoint = connection.value.endpoint
    if (!endpoint) throw new Error('所选 MQTT 服务端未启用 WebSocket')
    const attempt = ++connectionAttempt
    connecting.value = true

    return new Promise<void>((resolve, reject) => {
      let settled = false
      const client = mqtt.connect(endpoint, {
        clientId: connection.value.clientId,
        username: connection.value.username || undefined,
        password: connection.value.password || undefined,
        clean: connection.value.clean,
        keepalive: connection.value.keepalive
      })
      const timeout = window.setTimeout(() => {
        if (attempt !== connectionAttempt) return
        connecting.value = false
        connected.value = false
        client.end(true)
        settled = true
        reject(new Error('连接超时'))
      }, 10000)

      client.on('connect', () => {
        if (attempt !== connectionAttempt) {
          client.end(true)
          return
        }
        window.clearTimeout(timeout)
        connected.value = true
        connecting.value = false
        mqttClient.value = client
        settled = true
        resolve()
      })

      client.on('message', (topic, payload, packet) => {
        if (attempt !== connectionAttempt || client !== mqttClient.value) return
        addMessage({
          id: `msg_${Date.now()}_${Math.random()}`,
          timestamp: Date.now(), topic, payload: payload.toString(),
          qos: packet.qos, retain: packet.retain, direction: 'in'
        })
      })

      client.on('error', (error) => {
        if (attempt !== connectionAttempt) return
        window.clearTimeout(timeout)
        connecting.value = false
        connected.value = false
        if (!settled) reject(error)
      })

      client.on('close', () => {
        if (attempt !== connectionAttempt) return
        connected.value = false
        connecting.value = false
        if (mqttClient.value === client) mqttClient.value = null
      })
    })
  }

  const disconnect = (clearCredentials = false) => {
    connectionAttempt += 1
    const client = mqttClient.value
    mqttClient.value = null
    if (client) client.end(true)
    connected.value = false
    connecting.value = false
    subscriptions.value = []
    if (clearCredentials) {
      connection.value.username = ''
      connection.value.password = ''
    }
  }

  const subscribe = (topic: string, qos = 0) => {
    if (!mqttClient.value || !connected.value) return false
    mqttClient.value.subscribe(topic, { qos: qos as 0 | 1 | 2 })
    if (!subscriptions.value.includes(topic)) subscriptions.value.push(topic)
    return true
  }

  const unsubscribe = (topic: string) => {
    if (!mqttClient.value || !connected.value) return false
    mqttClient.value.unsubscribe(topic)
    subscriptions.value = subscriptions.value.filter(item => item !== topic)
    return true
  }

  const publish = (topic: string, payload: string, qos = 0, retain = false) => {
    if (!mqttClient.value || !connected.value) return false
    mqttClient.value.publish(topic, payload, { qos: qos as 0 | 1 | 2, retain })
    addMessage({ id: `msg_${Date.now()}_${Math.random()}`, timestamp: Date.now(), topic, payload, qos, retain, direction: 'out' })
    return true
  }

  const addMessage = (message: MqttMessage) => {
    messages.value.unshift(message)
    if (messages.value.length > maxMessages.value) messages.value = messages.value.slice(0, maxMessages.value)
    updateFilteredMessages()
  }

  const clearMessages = () => {
    messages.value = []
    updateFilteredMessages()
  }

  const filteredMessages = ref<MqttMessage[]>([])
  const messageFilter = ref({ topic: '', direction: 'all' as 'all' | 'in' | 'out' })
  const updateFilteredMessages = () => {
    let filtered = messages.value
    if (messageFilter.value.topic) filtered = filtered.filter(message => message.topic.includes(messageFilter.value.topic))
    if (messageFilter.value.direction !== 'all') filtered = filtered.filter(message => message.direction === messageFilter.value.direction)
    filteredMessages.value = filtered.slice(0, maxMessages.value)
  }

  const watchFilter = updateFilteredMessages
  const resetForServer = () => {
    disconnect(true)
    messages.value = []
    filteredMessages.value = []
    boundServerId.value = ''
  }

  return {
    messages, connection, mqttClient, connected, connecting, subscriptions, maxMessages,
    filteredMessages, messageFilter, boundServerId, bindServer, connect, disconnect,
    subscribe, unsubscribe, publish, addMessage, clearMessages, updateFilteredMessages,
    watchFilter, resetForServer
  }
})
