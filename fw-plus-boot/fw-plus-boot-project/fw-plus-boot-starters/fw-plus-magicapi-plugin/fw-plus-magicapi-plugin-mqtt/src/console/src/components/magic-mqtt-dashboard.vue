<!--
  Adapted from mica-mqtt-dashboard at commit
  2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80.
  Copyright Dreamlu. Licensed under the Apache License, Version 2.0.
-->
<template>
  <div class="mqtt-dashboard">
    <header class="mqtt-header">
      <div class="mqtt-brand">
        <magic-icon icon="#magic-mqtt-mqtt" size="22px" />
        <strong>{{ $i('mqtt.dashboard.title') }}</strong>
      </div>
      <div class="mqtt-server-controls">
        <label for="mqtt-server-select">{{ $i('mqtt.dashboard.server') }}</label>
        <select
          id="mqtt-server-select"
          v-model="selectedServerId"
          :disabled="serverLoading || servers.length === 0"
          @change="changeServer"
        >
          <option value="">{{ $i('mqtt.dashboard.selectServer') }}</option>
          <option v-for="server in servers" :key="server.id" :value="server.id">
            {{ server.name }}{{ server.defaultServer ? ` (${$i('mqtt.dashboard.defaultServer')})` : '' }}
          </option>
        </select>
        <button class="icon-button" type="button" :title="$i('mqtt.dashboard.refresh')" :disabled="serverLoading" @click="loadServers(false)">
          <magic-icon icon="refresh" size="16px" />
        </button>
      </div>
    </header>

    <div v-if="selectedServer" class="mqtt-server-summary">
      <span class="status-dot"></span>
      <span>{{ $i('mqtt.dashboard.online') }}</span>
      <span class="summary-separator"></span>
      <code>{{ selectedServer.ip || '0.0.0.0' }}:{{ selectedServer.port }}</code>
      <span class="summary-separator"></span>
      <code v-if="selectedServer.websocketEnabled">{{ debugEndpoint }}</code>
      <span v-else class="muted">{{ $i('mqtt.dashboard.websocketDisabled') }}</span>
    </div>

    <div v-if="errorMessage" class="mqtt-notice mqtt-notice-error" role="alert">
      <span>{{ errorMessage }}</span>
      <button v-if="selectedServerId" type="button" @click="loadActiveView">{{ $i('mqtt.dashboard.retry') }}</button>
    </div>
    <div v-if="operationMessage" class="mqtt-notice mqtt-notice-success" role="status">{{ operationMessage }}</div>

    <div v-if="serverLoading && !servers.length" class="mqtt-empty-state">
      <span class="mqtt-spinner"></span>
      <p>{{ $i('mqtt.dashboard.loading') }}</p>
    </div>
    <div v-else-if="!servers.length" class="mqtt-empty-state">
      <magic-icon icon="#magic-mqtt-mqtt" size="46px" />
      <p>{{ $i('mqtt.dashboard.noServer') }}</p>
      <button type="button" class="primary-button" @click="loadServers(false)">
        <magic-icon icon="refresh" size="15px" />
        {{ $i('mqtt.dashboard.refresh') }}
      </button>
    </div>
    <div v-else-if="!selectedServer" class="mqtt-empty-state">
      <p>{{ unavailableSelection ? $i('mqtt.dashboard.unavailable') : $i('mqtt.dashboard.selectServer') }}</p>
    </div>

    <template v-else>
      <nav class="mqtt-tabs" aria-label="MQTT Dashboard views">
        <button
          v-for="view in views"
          :key="view.id"
          type="button"
          :class="{ active: activeView === view.id }"
          @click="changeView(view.id)"
        >
          {{ $i(view.label) }}
        </button>
      </nav>

      <main class="mqtt-content">
        <div v-if="loading" class="mqtt-loading-line"><span class="mqtt-spinner"></span>{{ $i('mqtt.dashboard.loading') }}</div>

        <section v-show="activeView === 'dashboard'" class="mqtt-view mqtt-overview">
          <div class="stats-grid">
            <article v-for="item in overviewStats" :key="item.label" class="stat-tile" :class="`tone-${item.tone}`">
              <span class="stat-label">{{ $i(item.label) }}</span>
              <strong>{{ item.formatted ? item.formatted(item.value) : formatNumber(item.value) }}</strong>
            </article>
          </div>

          <div class="section-heading">
            <h2>{{ $i('mqtt.dashboard.recentClients') }}</h2>
            <button class="text-button" type="button" @click="changeView('clients')">{{ $i('mqtt.dashboard.clients') }}</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ $i('mqtt.dashboard.clientId') }}</th>
                  <th>{{ $i('mqtt.dashboard.username') }}</th>
                  <th>{{ $i('mqtt.dashboard.address') }}</th>
                  <th>{{ $i('mqtt.dashboard.protocol') }}</th>
                  <th>{{ $i('mqtt.dashboard.connectedAt') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="client in recentClients" :key="client.clientId" @click="openClient(client.clientId)">
                  <td><code>{{ client.clientId }}</code></td>
                  <td>{{ client.username || '-' }}</td>
                  <td>{{ clientAddress(client) }}</td>
                  <td>{{ client.protoFullName || client.protoName || '-' }}</td>
                  <td>{{ formatTime(client.connectedAt) }}</td>
                </tr>
                <tr v-if="recentClients.length === 0"><td colspan="5" class="empty-cell">{{ $i('mqtt.dashboard.emptyClients') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-show="activeView === 'clients'" class="mqtt-view">
          <div class="section-toolbar">
            <input v-model.trim="clientFilter" type="search" :placeholder="$i('mqtt.dashboard.filterClient')" />
            <button class="icon-button" type="button" :title="$i('mqtt.dashboard.refresh')" @click="loadClients(clientPage.pageNumber)">
              <magic-icon icon="refresh" size="16px" />
            </button>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ $i('mqtt.dashboard.clientId') }}</th>
                  <th>{{ $i('mqtt.dashboard.username') }}</th>
                  <th>{{ $i('mqtt.dashboard.address') }}</th>
                  <th>{{ $i('mqtt.dashboard.keepAlive') }}</th>
                  <th>{{ $i('mqtt.dashboard.transport') }}</th>
                  <th>{{ $i('mqtt.dashboard.connectedAt') }}</th>
                  <th class="actions-column">{{ $i('mqtt.dashboard.action') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="client in filteredClients" :key="client.clientId">
                  <td><button class="link-button" type="button" @click="openClient(client.clientId)">{{ client.clientId }}</button></td>
                  <td>{{ client.username || '-' }}</td>
                  <td>{{ clientAddress(client) }}</td>
                  <td>{{ client.keepAlive }}s</td>
                  <td>{{ client.webSocket ? 'WebSocket' : 'TCP' }}{{ client.ssl ? ' + TLS' : '' }}</td>
                  <td>{{ formatTime(client.connectedAt) }}</td>
                  <td class="row-actions">
                    <button class="icon-button" type="button" :title="$i('mqtt.dashboard.details')" @click="openClient(client.clientId)">
                      <magic-icon icon="search" size="15px" />
                    </button>
                    <button class="icon-button danger" type="button" :title="$i('mqtt.dashboard.kick')" @click="confirmKick(client.clientId)">
                      <magic-icon icon="delete" size="15px" />
                    </button>
                  </td>
                </tr>
                <tr v-if="filteredClients.length === 0"><td colspan="7" class="empty-cell">{{ $i('mqtt.dashboard.emptyClients') }}</td></tr>
              </tbody>
            </table>
          </div>
          <div class="pagination-bar">
            <span>{{ $i('mqtt.dashboard.pageSummary', clientPage.pageNumber, clientPage.totalRow) }}</span>
            <div>
              <button type="button" :disabled="clientPage.pageNumber <= 1" @click="loadClients(clientPage.pageNumber - 1)">{{ $i('mqtt.dashboard.previous') }}</button>
              <button type="button" :disabled="clientPage.pageNumber * clientPage.pageSize >= clientPage.totalRow" @click="loadClients(clientPage.pageNumber + 1)">{{ $i('mqtt.dashboard.next') }}</button>
            </div>
          </div>

          <div v-if="selectedClient" class="client-detail">
            <div class="section-heading">
              <div>
                <span class="eyebrow">{{ $i('mqtt.dashboard.details') }}</span>
                <h2>{{ selectedClient.clientId }}</h2>
              </div>
              <button class="icon-button" type="button" :title="$i('mqtt.dashboard.close')" @click="closeClientDetail">
                <magic-icon icon="close" size="16px" />
              </button>
            </div>
            <dl class="detail-grid">
              <div><dt>{{ $i('mqtt.dashboard.username') }}</dt><dd>{{ selectedClient.username || '-' }}</dd></div>
              <div><dt>{{ $i('mqtt.dashboard.address') }}</dt><dd>{{ clientAddress(selectedClient) }}</dd></div>
              <div><dt>{{ $i('mqtt.dashboard.protocol') }}</dt><dd>{{ selectedClient.protoFullName || '-' }}</dd></div>
              <div><dt>{{ $i('mqtt.dashboard.keepAlive') }}</dt><dd>{{ selectedClient.keepAlive }}s</dd></div>
              <div><dt>{{ $i('mqtt.dashboard.transport') }}</dt><dd>{{ selectedClient.webSocket ? 'WebSocket' : 'TCP' }}{{ selectedClient.ssl ? ' + TLS' : '' }}</dd></div>
              <div><dt>{{ $i('mqtt.dashboard.connectedAt') }}</dt><dd>{{ formatTime(selectedClient.connectedAt) }}</dd></div>
            </dl>

            <div class="section-heading subscription-heading"><h3>{{ $i('mqtt.dashboard.subscriptions') }}</h3></div>
            <form class="inline-form" @submit.prevent="addSubscription">
              <input v-model.trim="subscriptionForm.topic" :placeholder="$i('mqtt.dashboard.topic')" />
              <select v-model.number="subscriptionForm.qos">
                <option :value="0">QoS 0</option>
                <option :value="1">QoS 1</option>
                <option :value="2">QoS 2</option>
              </select>
              <button class="primary-button" type="submit">
                <magic-icon icon="plus" size="15px" />
                {{ $i('mqtt.dashboard.subscribe') }}
              </button>
            </form>
            <div class="subscription-list">
              <div v-for="subscription in clientSubscriptions" :key="subscription.topicFilter" class="subscription-row">
                <code>{{ subscription.topicFilter }}</code>
                <span>QoS {{ subscription.mqttQoS }}</span>
                <button class="icon-button danger" type="button" :title="$i('mqtt.dashboard.unsubscribe')" @click="removeSubscription(subscription)">
                  <magic-icon icon="delete" size="14px" />
                </button>
              </div>
              <p v-if="clientSubscriptions.length === 0" class="muted">{{ $i('mqtt.dashboard.emptySubscriptions') }}</p>
            </div>
          </div>
        </section>

        <section v-show="activeView === 'monitor'" class="mqtt-view">
          <div class="section-heading">
            <h2>{{ $i('mqtt.dashboard.liveSamples') }}</h2>
            <button type="button" :disabled="monitorSamples.length === 0" @click="monitorSamples = []">{{ $i('mqtt.dashboard.clear') }}</button>
          </div>
          <div class="monitor-panels">
            <article class="chart-panel">
              <h3>{{ $i('mqtt.dashboard.connectionTrend') }}</h3>
              <div class="bar-chart" aria-label="Connection trend">
                <span v-for="sample in monitorSamples" :key="`c-${sample.sampledAt}`" :style="{ height: barHeight(sample.connections, maxConnections) }" :title="String(sample.connections)"></span>
              </div>
            </article>
            <article class="chart-panel">
              <h3>{{ $i('mqtt.dashboard.messageTrend') }}</h3>
              <div class="bar-chart messages-chart" aria-label="Message trend">
                <span v-for="sample in monitorSamples" :key="`m-${sample.sampledAt}`" :style="{ height: barHeight(sample.receivedPackets + sample.sentPackets, maxMessages) }" :title="String(sample.receivedPackets + sample.sentPackets)"></span>
              </div>
            </article>
          </div>
          <div class="table-wrap monitor-table">
            <table>
              <thead><tr><th>{{ $i('mqtt.dashboard.sampleTime') }}</th><th>{{ $i('mqtt.dashboard.connections') }}</th><th>{{ $i('mqtt.dashboard.receivedPackets') }}</th><th>{{ $i('mqtt.dashboard.sentPackets') }}</th><th>{{ $i('mqtt.dashboard.receivedBytes') }}</th><th>{{ $i('mqtt.dashboard.sentBytes') }}</th></tr></thead>
              <tbody>
                <tr v-for="sample in monitorSamples.slice().reverse()" :key="sample.sampledAt">
                  <td>{{ formatTime(sample.sampledAt) }}</td><td>{{ formatNumber(sample.connections) }}</td><td>{{ formatNumber(sample.receivedPackets) }}</td><td>{{ formatNumber(sample.sentPackets) }}</td><td>{{ formatBytes(sample.receivedBytes) }}</td><td>{{ formatBytes(sample.sentBytes) }}</td>
                </tr>
                <tr v-if="monitorSamples.length === 0"><td colspan="6" class="empty-cell">{{ $i('mqtt.dashboard.loading') }}</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-show="activeView === 'debug'" class="mqtt-view mqtt-debug">
          <div v-if="debugError" class="mqtt-notice mqtt-notice-error">{{ debugError }}</div>
          <div class="debug-grid">
            <div class="debug-controls">
              <div class="section-heading"><h2>{{ $i('mqtt.dashboard.debugEndpoint') }}</h2><span class="connection-badge" :class="{ connected: debugConnected }">{{ debugConnected ? $i('mqtt.dashboard.connected') : $i('mqtt.dashboard.disconnected') }}</span></div>
              <code class="endpoint-value">{{ debugEndpoint || '-' }}</code>
              <form class="stacked-form" @submit.prevent="toggleDebugConnection">
                <label>{{ $i('mqtt.dashboard.clientId') }}<input v-model.trim="debugForm.clientId" :disabled="debugConnected" :placeholder="$i('mqtt.dashboard.clientIdPlaceholder')" /></label>
                <label>{{ $i('mqtt.dashboard.username') }}<input v-model.trim="debugForm.username" :disabled="debugConnected" autocomplete="off" /></label>
                <label>{{ $i('mqtt.dashboard.password') }}<input v-model="debugForm.password" :disabled="debugConnected" type="password" autocomplete="new-password" /></label>
                <label class="checkbox-label"><input v-model="debugForm.clean" :disabled="debugConnected" type="checkbox" />{{ $i('mqtt.dashboard.cleanSession') }}</label>
                <button class="primary-button" :class="{ danger: debugConnected }" type="submit" :disabled="debugConnecting">
                  <magic-icon :icon="debugConnected ? 'close' : 'run'" size="15px" />
                  {{ debugConnecting ? $i('mqtt.dashboard.connecting') : (debugConnected ? $i('mqtt.dashboard.disconnect') : $i('mqtt.dashboard.connect')) }}
                </button>
              </form>

              <form class="inline-form debug-form" @submit.prevent="debugSubscribe">
                <input v-model.trim="debugSubscription.topic" :placeholder="$i('mqtt.dashboard.topic')" />
                <select v-model.number="debugSubscription.qos"><option :value="0">QoS 0</option><option :value="1">QoS 1</option><option :value="2">QoS 2</option></select>
                <button class="primary-button" type="submit" :disabled="!debugConnected">{{ $i('mqtt.dashboard.subscribe') }}</button>
              </form>
              <div class="debug-subscriptions">
                <button v-for="topic in debugSubscriptions" :key="topic" type="button" :title="$i('mqtt.dashboard.unsubscribe')" @click="debugUnsubscribe(topic)">{{ topic }} <span>×</span></button>
              </div>

              <form class="stacked-form publish-form" @submit.prevent="debugPublish">
                <label>{{ $i('mqtt.dashboard.topic') }}<input v-model.trim="publishForm.topic" /></label>
                <label>{{ $i('mqtt.dashboard.payload') }}<textarea v-model="publishForm.payload" rows="4"></textarea></label>
                <div class="publish-options">
                  <select v-model.number="publishForm.qos"><option :value="0">QoS 0</option><option :value="1">QoS 1</option><option :value="2">QoS 2</option></select>
                  <label class="checkbox-label"><input v-model="publishForm.retain" type="checkbox" />{{ $i('mqtt.dashboard.retain') }}</label>
                </div>
                <button class="primary-button" type="submit" :disabled="!debugConnected">{{ $i('mqtt.dashboard.publish') }}</button>
              </form>
            </div>

            <div class="debug-messages">
              <div class="section-heading"><h2>{{ $i('mqtt.dashboard.messages') }}</h2><button type="button" :disabled="debugMessages.length === 0" @click="debugMessages = []">{{ $i('mqtt.dashboard.clear') }}</button></div>
              <div class="message-list">
                <article v-for="message in debugMessages" :key="message.id" class="message-row" :class="message.direction">
                  <div><span>{{ message.direction === 'in' ? $i('mqtt.dashboard.received') : $i('mqtt.dashboard.sent') }}</span><time>{{ formatTime(message.timestamp) }}</time></div>
                  <code>{{ message.topic }}</code>
                  <pre>{{ message.payload }}</pre>
                </article>
                <div v-if="debugMessages.length === 0" class="mqtt-empty-state compact">{{ $i('mqtt.dashboard.emptyMessages') }}</div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { getMagicMqttDashboardRuntime } from '../service/magic-mqtt-dashboard-runtime.js'

const SERVER_STORAGE_KEY = 'magic-mqtt-dashboard-server'
const PAGE_SIZE = 20
const MONITOR_INTERVAL = 5000
const MAX_MONITOR_SAMPLES = 24
const MAX_DEBUG_MESSAGES = 200

const runtime = getMagicMqttDashboardRuntime()
const request = runtime.request
const modal = runtime.modal
const constants = runtime.constants || {}
const $i = runtime.format || (key => key)

const views = [
  { id: 'dashboard', label: 'mqtt.dashboard.overview' },
  { id: 'clients', label: 'mqtt.dashboard.clients' },
  { id: 'monitor', label: 'mqtt.dashboard.monitor' },
  { id: 'debug', label: 'mqtt.dashboard.debug' }
]

const servers = ref([])
const selectedServerId = ref('')
const unavailableSelection = ref(false)
const activeView = ref('dashboard')
const serverLoading = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const operationMessage = ref('')
const requestGeneration = ref(0)

const emptyStats = () => ({ connections: {}, messages: {}, nodes: {}, sampledAt: 0 })
const stats = ref(emptyStats())
const recentClients = ref([])
const clients = ref([])
const clientFilter = ref('')
const clientPage = reactive({ pageNumber: 1, pageSize: PAGE_SIZE, totalRow: 0 })
const selectedClient = ref(null)
const clientSubscriptions = ref([])
const subscriptionForm = reactive({ topic: '', qos: 0 })
const monitorSamples = ref([])
let monitorTimer = null
let operationTimer = null

const debugForm = reactive({ clientId: '', username: '', password: '', keepalive: 60, clean: true })
const debugSubscription = reactive({ topic: '', qos: 0 })
const publishForm = reactive({ topic: '', payload: '', qos: 0, retain: false })
const debugConnecting = ref(false)
const debugConnected = ref(false)
const debugError = ref('')
const debugSubscriptions = ref([])
const debugMessages = ref([])
let mqttClient = null
let mqttLibraryPromise = null
let messageSequence = 0

const selectedServer = computed(() => servers.value.find(server => server.id === selectedServerId.value) || null)
const filteredClients = computed(() => {
  const filter = clientFilter.value.toLowerCase()
  if (!filter) return clients.value
  return clients.value.filter(client => `${client.clientId || ''} ${client.username || ''}`.toLowerCase().includes(filter))
})
const debugEndpoint = computed(() => {
  const server = selectedServer.value
  if (!server || !server.websocketEnabled || !server.webPort) return ''
  const protocol = server.sslEnabled ? 'wss' : 'ws'
  let host = server.ip
  if (!host || host === '0.0.0.0' || host === '::' || host === '::0') host = window.location.hostname
  if (host.includes(':') && !host.startsWith('[')) host = `[${host}]`
  return `${protocol}://${host}:${server.webPort}`
})
const overviewStats = computed(() => [
  { label: 'mqtt.dashboard.connections', value: stats.value.connections?.size, tone: 'blue' },
  { label: 'mqtt.dashboard.accepted', value: stats.value.connections?.accepted, tone: 'green' },
  { label: 'mqtt.dashboard.closed', value: stats.value.connections?.closed, tone: 'gray' },
  { label: 'mqtt.dashboard.users', value: stats.value.nodes?.users, tone: 'amber' },
  { label: 'mqtt.dashboard.receivedPackets', value: stats.value.messages?.receivedPackets, tone: 'green' },
  { label: 'mqtt.dashboard.sentPackets', value: stats.value.messages?.sendPackets, tone: 'blue' },
  { label: 'mqtt.dashboard.receivedBytes', value: stats.value.messages?.receivedBytes, formatted: formatBytes, tone: 'green' },
  { label: 'mqtt.dashboard.sentBytes', value: stats.value.messages?.sendBytes, formatted: formatBytes, tone: 'blue' }
])
const maxConnections = computed(() => Math.max(1, ...monitorSamples.value.map(sample => sample.connections)))
const maxMessages = computed(() => Math.max(1, ...monitorSamples.value.map(sample => sample.receivedPackets + sample.sentPackets)))

function requestError(data, response, error) {
  if (data && typeof data === 'object') return data.message || data.data || JSON.stringify(data)
  if (typeof data === 'string' && data) return data
  if (error && error.message) return error.message
  if (response && response.status) return `${$i('mqtt.dashboard.requestFailed')} (${response.status})`
  return $i('mqtt.dashboard.requestFailed')
}

function executeRequest(factory) {
  return new Promise((resolve, reject) => {
    let settled = false
    const response = factory()
    response
      .success(data => {
        settled = true
        resolve(data)
      })
      .exception((code, message) => {
        settled = true
        reject(new Error(message || `${$i('mqtt.dashboard.requestFailed')} (${code})`))
      })
      .error((data, rawResponse, error) => {
        settled = true
        reject(new Error(requestError(data, rawResponse, error)))
      })
      .end(success => {
        if (!success && !settled) reject(new Error($i('mqtt.dashboard.requestFailed')))
      })
  })
}

const apiGet = (url, params) => executeRequest(() => request.sendGet(url, params))
const apiPost = (url, data) => executeRequest(() => request.sendPost(url, data))
const apiJson = (url, data) => executeRequest(() => request.sendJson(url, data))
const serverPath = suffix => `/mqtt/dashboard/${encodeURIComponent(selectedServerId.value)}${suffix}`

function isCurrent(generation, serverId) {
  return generation === requestGeneration.value && serverId === selectedServerId.value
}

function showError(error, generation, serverId) {
  if (!isCurrent(generation, serverId)) return
  errorMessage.value = error && error.message ? error.message : $i('mqtt.dashboard.requestFailed')
  verifySelectedServer(generation, serverId)
}

async function verifySelectedServer(generation, serverId) {
  try {
    const data = await apiGet('/mqtt/dashboard/servers')
    if (!isCurrent(generation, serverId)) return
    servers.value = Array.isArray(data) ? data : []
    if (!servers.value.some(server => server.id === serverId)) {
      deactivateServer(true)
      errorMessage.value = $i('mqtt.dashboard.unavailable')
    }
  } catch (error) {
    // Keep the original management error, including host authorization failures.
  }
}

function showOperation(message) {
  operationMessage.value = message
  window.clearTimeout(operationTimer)
  operationTimer = window.setTimeout(() => { operationMessage.value = '' }, 2800)
}

async function loadServers(initial = false) {
  serverLoading.value = true
  errorMessage.value = ''
  const previousId = selectedServerId.value
  try {
    const data = await apiGet('/mqtt/dashboard/servers')
    servers.value = Array.isArray(data) ? data : []
    if (initial) {
      const storedId = sessionStorage.getItem(SERVER_STORAGE_KEY)
      const restored = servers.value.find(server => server.id === storedId)
      const target = restored || servers.value.find(server => server.defaultServer) || servers.value[0]
      if (target) activateServer(target.id)
    } else if (previousId && !servers.value.some(server => server.id === previousId)) {
      deactivateServer(true)
    } else if (previousId) {
      await loadActiveView()
    }
  } catch (error) {
    errorMessage.value = error.message
    if (initial) servers.value = []
  } finally {
    serverLoading.value = false
  }
}

function activateServer(serverId) {
  if (!servers.value.some(server => server.id === serverId)) return
  requestGeneration.value += 1
  stopMonitor()
  disconnectDebug(true)
  selectedServerId.value = serverId
  unavailableSelection.value = false
  errorMessage.value = ''
  operationMessage.value = ''
  stats.value = emptyStats()
  recentClients.value = []
  clients.value = []
  clientPage.pageNumber = 1
  clientPage.totalRow = 0
  closeClientDetail()
  monitorSamples.value = []
  sessionStorage.setItem(SERVER_STORAGE_KEY, serverId)
  loadActiveView()
}

function deactivateServer(unavailable) {
  requestGeneration.value += 1
  stopMonitor()
  disconnectDebug(true)
  selectedServerId.value = ''
  unavailableSelection.value = unavailable
  sessionStorage.removeItem(SERVER_STORAGE_KEY)
  stats.value = emptyStats()
  clients.value = []
  recentClients.value = []
  monitorSamples.value = []
  closeClientDetail()
}

function changeServer() {
  if (selectedServerId.value) {
    activateServer(selectedServerId.value)
  } else {
    deactivateServer(false)
  }
}

function changeView(view) {
  if (activeView.value === view) return
  if (activeView.value === 'monitor') stopMonitor()
  activeView.value = view
  errorMessage.value = ''
  loadActiveView()
}

async function loadActiveView() {
  if (!selectedServerId.value) return
  if (activeView.value === 'dashboard') return loadOverview()
  if (activeView.value === 'clients') return loadClients(1)
  if (activeView.value === 'monitor') return startMonitor()
}

async function loadOverview() {
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  loading.value = true
  errorMessage.value = ''
  stats.value = emptyStats()
  recentClients.value = []
  try {
    const [statsData, pageData] = await Promise.all([
      apiGet(serverPath('/stats')),
      apiGet(serverPath('/clients'), { _page: 1, _limit: 5 })
    ])
    if (!isCurrent(generation, serverId)) return
    stats.value = statsData || emptyStats()
    recentClients.value = pageData && Array.isArray(pageData.list) ? pageData.list : []
  } catch (error) {
    showError(error, generation, serverId)
  } finally {
    if (isCurrent(generation, serverId)) loading.value = false
  }
}

async function loadClients(page = 1) {
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  loading.value = true
  errorMessage.value = ''
  clients.value = []
  closeClientDetail()
  try {
    const data = await apiGet(serverPath('/clients'), { _page: page, _limit: PAGE_SIZE })
    if (!isCurrent(generation, serverId)) return
    clients.value = data && Array.isArray(data.list) ? data.list : []
    clientPage.pageNumber = data?.pageNumber || page
    clientPage.pageSize = data?.pageSize || PAGE_SIZE
    clientPage.totalRow = data?.totalRow || 0
  } catch (error) {
    showError(error, generation, serverId)
  } finally {
    if (isCurrent(generation, serverId)) loading.value = false
  }
}

async function openClient(clientId) {
  const targetServerId = selectedServerId.value
  if (activeView.value !== 'clients') {
    if (activeView.value === 'monitor') stopMonitor()
    activeView.value = 'clients'
    await loadClients(1)
    if (selectedServerId.value !== targetServerId) return
  }
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  loading.value = true
  errorMessage.value = ''
  selectedClient.value = null
  clientSubscriptions.value = []
  try {
    const [client, subscriptions] = await Promise.all([
      apiGet(serverPath('/clients/info'), { clientId }),
      apiGet(serverPath('/client/subscriptions'), { clientId })
    ])
    if (!isCurrent(generation, serverId)) return
    selectedClient.value = client
    clientSubscriptions.value = Array.isArray(subscriptions) ? subscriptions : []
  } catch (error) {
    showError(error, generation, serverId)
  } finally {
    if (isCurrent(generation, serverId)) loading.value = false
  }
}

function closeClientDetail() {
  selectedClient.value = null
  clientSubscriptions.value = []
  subscriptionForm.topic = ''
  subscriptionForm.qos = 0
}

function confirmKick(clientId) {
  const run = async () => {
    const generation = requestGeneration.value
    const serverId = selectedServerId.value
    try {
      await apiPost(serverPath('/clients/delete'), { clientId })
      if (!isCurrent(generation, serverId)) return
      showOperation($i('mqtt.dashboard.operationSuccess'))
      await loadClients(clientPage.pageNumber)
    } catch (error) {
      showError(error, generation, serverId)
    }
  }
  if (modal && typeof modal.confirm === 'function') {
    modal.confirm($i('mqtt.dashboard.kick'), $i('mqtt.dashboard.kickConfirm', clientId), run)
  } else if (window.confirm($i('mqtt.dashboard.kickConfirm', clientId))) {
    run()
  }
}

async function addSubscription() {
  if (!selectedClient.value || !subscriptionForm.topic) {
    errorMessage.value = $i('mqtt.dashboard.topicRequired')
    return
  }
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  try {
    await apiJson(serverPath('/mqtt/subscribe'), {
      clientId: selectedClient.value.clientId,
      topic: subscriptionForm.topic,
      qos: subscriptionForm.qos
    })
    if (!isCurrent(generation, serverId)) return
    subscriptionForm.topic = ''
    showOperation($i('mqtt.dashboard.operationSuccess'))
    await reloadSubscriptions(selectedClient.value.clientId, generation, serverId)
  } catch (error) {
    showError(error, generation, serverId)
  }
}

async function removeSubscription(subscription) {
  if (!selectedClient.value) return
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  try {
    await apiJson(serverPath('/mqtt/unsubscribe'), {
      clientId: selectedClient.value.clientId,
      topic: subscription.topicFilter,
      qos: subscription.mqttQoS || 0
    })
    if (!isCurrent(generation, serverId)) return
    showOperation($i('mqtt.dashboard.operationSuccess'))
    await reloadSubscriptions(selectedClient.value.clientId, generation, serverId)
  } catch (error) {
    showError(error, generation, serverId)
  }
}

async function reloadSubscriptions(clientId, generation, serverId) {
  const data = await apiGet(serverPath('/client/subscriptions'), { clientId })
  if (isCurrent(generation, serverId)) clientSubscriptions.value = Array.isArray(data) ? data : []
}

function startMonitor() {
  stopMonitor()
  sampleStats()
  monitorTimer = window.setInterval(sampleStats, MONITOR_INTERVAL)
}

function stopMonitor() {
  if (monitorTimer) window.clearInterval(monitorTimer)
  monitorTimer = null
}

async function sampleStats() {
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  if (!serverId || activeView.value !== 'monitor') return
  try {
    const data = await apiGet(serverPath('/stats'))
    if (!isCurrent(generation, serverId) || activeView.value !== 'monitor') return
    const sample = {
      sampledAt: data?.sampledAt || Date.now(),
      connections: data?.connections?.size || 0,
      receivedPackets: data?.messages?.receivedPackets || 0,
      sentPackets: data?.messages?.sendPackets || 0,
      receivedBytes: data?.messages?.receivedBytes || 0,
      sentBytes: data?.messages?.sendBytes || 0
    }
    monitorSamples.value = [...monitorSamples.value, sample].slice(-MAX_MONITOR_SAMPLES)
    errorMessage.value = ''
  } catch (error) {
    showError(error, generation, serverId)
    stopMonitor()
  }
}

function mqttLibraryUrl() {
  const pageUrl = `${window.location.protocol}//${window.location.host}${window.location.pathname}`
  const baseUrl = new URL(constants.BASE_URL || './', pageUrl).toString().replace(/\/+$/, '')
  return `${baseUrl}/mqtt/js/mqtt.min.js`
}

function loadMqttLibrary() {
  if (window.mqtt) return Promise.resolve(window.mqtt)
  if (mqttLibraryPromise) return mqttLibraryPromise
  mqttLibraryPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = 'magic-mqtt-debug-library'
    script.src = mqttLibraryUrl()
    script.onload = () => window.mqtt ? resolve(window.mqtt) : reject(new Error($i('mqtt.dashboard.mqttLibraryFailed')))
    script.onerror = () => reject(new Error($i('mqtt.dashboard.mqttLibraryFailed')))
    document.head.appendChild(script)
  })
  return mqttLibraryPromise
}

async function toggleDebugConnection() {
  if (debugConnected.value || mqttClient) {
    disconnectDebug(false)
    return
  }
  const server = selectedServer.value
  debugError.value = ''
  if (!server || !server.websocketEnabled || !debugEndpoint.value) {
    debugError.value = $i('mqtt.dashboard.websocketRequired')
    return
  }
  if (window.location.protocol === 'https:' && !server.sslEnabled) {
    debugError.value = $i('mqtt.dashboard.secureContextRequired')
    return
  }
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  const endpoint = debugEndpoint.value
  const connectOptions = {
    clientId: debugForm.clientId || `magic_dashboard_${Math.random().toString(16).slice(2, 10)}`,
    username: debugForm.username || undefined,
    password: debugForm.password || undefined,
    keepalive: debugForm.keepalive,
    clean: debugForm.clean,
    reconnectPeriod: 0,
    connectTimeout: 10000
  }
  debugConnecting.value = true
  try {
    const mqtt = await loadMqttLibrary()
    if (!isCurrent(generation, serverId) || !debugConnecting.value) return
    const client = mqtt.connect(endpoint, connectOptions)
    mqttClient = client
    client.on('connect', () => {
      if (!isCurrent(generation, serverId) || mqttClient !== client) {
        client.end(true)
        return
      }
      debugConnected.value = true
      debugConnecting.value = false
      debugError.value = ''
    })
    client.on('message', (topic, payload, packet) => {
      if (mqttClient !== client) return
      pushDebugMessage('in', topic, payload.toString(), packet?.qos || 0, packet?.retain || false)
    })
    client.on('error', error => {
      if (mqttClient !== client) return
      debugError.value = `${$i('mqtt.dashboard.connectFailed')}: ${error.message || error}`
      debugConnecting.value = false
      debugConnected.value = false
      mqttClient = null
      try { client.end(true) } catch (closeError) { console.warn(closeError) }
    })
    client.on('close', () => {
      if (mqttClient !== client) return
      debugConnected.value = false
      debugConnecting.value = false
    })
  } catch (error) {
    if (!isCurrent(generation, serverId) || !debugConnecting.value) return
    debugError.value = error.message || $i('mqtt.dashboard.connectFailed')
    debugConnecting.value = false
  }
}

function disconnectDebug(clearCredentials) {
  const client = mqttClient
  mqttClient = null
  if (client) {
    try { client.end(true) } catch (error) { console.warn(error) }
  }
  debugConnected.value = false
  debugConnecting.value = false
  debugSubscriptions.value = []
  if (clearCredentials) {
    debugForm.username = ''
    debugForm.password = ''
    debugForm.clientId = ''
    debugError.value = ''
    debugMessages.value = []
  }
}

function debugSubscribe() {
  if (!mqttClient || !debugConnected.value || !debugSubscription.topic) {
    if (!debugSubscription.topic) debugError.value = $i('mqtt.dashboard.topicRequired')
    return
  }
  const client = mqttClient
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  const topic = debugSubscription.topic
  client.subscribe(topic, { qos: debugSubscription.qos }, error => {
    if (!isCurrent(generation, serverId) || mqttClient !== client) return
    if (error) {
      debugError.value = error.message || String(error)
      return
    }
    if (!debugSubscriptions.value.includes(topic)) debugSubscriptions.value.push(topic)
    debugSubscription.topic = ''
  })
}

function debugUnsubscribe(topic) {
  if (!mqttClient) return
  const client = mqttClient
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  client.unsubscribe(topic, error => {
    if (!isCurrent(generation, serverId) || mqttClient !== client) return
    if (error) {
      debugError.value = error.message || String(error)
      return
    }
    debugSubscriptions.value = debugSubscriptions.value.filter(item => item !== topic)
  })
}

function debugPublish() {
  if (!mqttClient || !debugConnected.value) return
  if (!publishForm.topic) {
    debugError.value = $i('mqtt.dashboard.topicRequired')
    return
  }
  if (!publishForm.payload) {
    debugError.value = $i('mqtt.dashboard.payloadRequired')
    return
  }
  const client = mqttClient
  const generation = requestGeneration.value
  const serverId = selectedServerId.value
  const topic = publishForm.topic
  const payload = publishForm.payload
  const qos = publishForm.qos
  const retain = publishForm.retain
  client.publish(topic, payload, { qos, retain }, error => {
    if (!isCurrent(generation, serverId) || mqttClient !== client) return
    if (error) {
      debugError.value = error.message || String(error)
      return
    }
    pushDebugMessage('out', topic, payload, qos, retain)
    publishForm.payload = ''
  })
}

function pushDebugMessage(direction, topic, payload, qos, retain) {
  const message = { id: ++messageSequence, direction, topic, payload, qos, retain, timestamp: Date.now() }
  debugMessages.value = [message, ...debugMessages.value].slice(0, MAX_DEBUG_MESSAGES)
}

function clientAddress(client) {
  if (!client || !client.ipAddress) return '-'
  return client.port ? `${client.ipAddress}:${client.port}` : client.ipAddress
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString()
}

function formatBytes(value) {
  let bytes = Number(value || 0)
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let unit = -1
  do { bytes /= 1024; unit += 1 } while (bytes >= 1024 && unit < units.length - 1)
  return `${bytes.toFixed(bytes >= 10 ? 1 : 2)} ${units[unit]}`
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function barHeight(value, max) {
  return `${Math.max(4, Math.round((Number(value || 0) / max) * 100))}%`
}

onMounted(() => loadServers(true))
onUnmounted(() => {
  requestGeneration.value += 1
  stopMonitor()
  disconnectDebug(true)
  window.clearTimeout(operationTimer)
})
</script>

<style scoped>
.mqtt-dashboard {
  --mqtt-bg: #f6f7f9;
  --mqtt-panel: #ffffff;
  --mqtt-border: #dfe3e8;
  --mqtt-text: #20242a;
  --mqtt-muted: #68717d;
  --mqtt-primary: #1769aa;
  --mqtt-green: #23855b;
  --mqtt-red: #c43d3d;
  --mqtt-amber: #a76312;
  box-sizing: border-box;
  min-width: 0;
  height: 100%;
  min-height: 420px;
  overflow: auto;
  color: var(--mqtt-text);
  background: var(--mqtt-bg);
  font-size: 13px;
}

.mqtt-dashboard *, .mqtt-dashboard *::before, .mqtt-dashboard *::after { box-sizing: border-box; }
button, input, select, textarea { font: inherit; letter-spacing: 0; }
button, select { min-height: 32px; }
button { cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .48; }
input, select, textarea { color: var(--mqtt-text); background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 4px; outline: none; }
input:focus, select:focus, textarea:focus { border-color: var(--mqtt-primary); box-shadow: 0 0 0 2px rgba(23, 105, 170, .12); }
code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; letter-spacing: 0; }

.mqtt-header { position: sticky; top: 0; z-index: 5; display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 54px; padding: 10px 18px; background: var(--mqtt-panel); border-bottom: 1px solid var(--mqtt-border); }
.mqtt-brand, .mqtt-server-controls, .mqtt-server-summary, .section-toolbar, .pagination-bar, .row-actions, .inline-form, .publish-options, .checkbox-label { display: flex; align-items: center; }
.mqtt-brand { gap: 9px; font-size: 15px; white-space: nowrap; }
.mqtt-server-controls { justify-content: flex-end; gap: 8px; min-width: 0; }
.mqtt-server-controls label { color: var(--mqtt-muted); }
.mqtt-server-controls select { width: min(320px, 36vw); min-width: 160px; padding: 0 28px 0 9px; }
.mqtt-server-summary { flex-wrap: wrap; gap: 8px; min-height: 34px; padding: 7px 18px; color: var(--mqtt-muted); background: #eef3f7; border-bottom: 1px solid var(--mqtt-border); }
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--mqtt-green); }
.summary-separator { width: 1px; height: 14px; background: #c9d0d7; }
.muted { color: var(--mqtt-muted); }

.icon-button { display: inline-grid; place-items: center; width: 32px; min-width: 32px; height: 32px; padding: 0; color: var(--mqtt-text); background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 4px; }
.icon-button:hover, .text-button:hover, .link-button:hover { color: var(--mqtt-primary); border-color: #9abed9; }
.icon-button.danger { color: var(--mqtt-red); }
.primary-button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 34px; padding: 0 13px; color: #fff; background: var(--mqtt-primary); border: 1px solid var(--mqtt-primary); border-radius: 4px; }
.primary-button.danger { background: var(--mqtt-red); border-color: var(--mqtt-red); }
.text-button, .link-button { min-height: 28px; padding: 0; color: var(--mqtt-primary); background: transparent; border: 0; }

.mqtt-notice { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 12px 18px 0; padding: 10px 12px; border: 1px solid; border-radius: 4px; }
.mqtt-notice button { padding: 0 10px; background: transparent; border: 1px solid currentColor; border-radius: 4px; }
.mqtt-notice-error { color: #9f2929; background: #fff1f1; border-color: #e8b7b7; }
.mqtt-notice-success { color: #176b49; background: #eef9f3; border-color: #a9d9c3; }
.mqtt-empty-state { display: grid; place-items: center; align-content: center; gap: 14px; min-height: 300px; padding: 28px; color: var(--mqtt-muted); text-align: center; }
.mqtt-empty-state.compact { min-height: 180px; }
.mqtt-spinner { width: 18px; height: 18px; border: 2px solid #c8d0d7; border-top-color: var(--mqtt-primary); border-radius: 50%; animation: mqtt-spin .8s linear infinite; }
@keyframes mqtt-spin { to { transform: rotate(360deg); } }

.mqtt-tabs { position: sticky; top: 54px; z-index: 4; display: flex; gap: 2px; padding: 8px 18px 0; background: var(--mqtt-panel); border-bottom: 1px solid var(--mqtt-border); }
.mqtt-tabs button { min-width: 90px; padding: 0 16px; color: var(--mqtt-muted); background: transparent; border: 0; border-bottom: 2px solid transparent; }
.mqtt-tabs button.active { color: var(--mqtt-primary); border-bottom-color: var(--mqtt-primary); font-weight: 600; }
.mqtt-content { position: relative; padding: 18px; }
.mqtt-view { min-width: 0; max-width: 1500px; margin: 0 auto; }
.mqtt-loading-line { position: sticky; top: 100px; z-index: 3; display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 34px; margin: -8px auto 10px; width: fit-content; padding: 0 12px; color: var(--mqtt-muted); background: rgba(255,255,255,.94); border: 1px solid var(--mqtt-border); border-radius: 4px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 12px; }
.stat-tile { position: relative; min-height: 90px; padding: 17px 16px; overflow: hidden; background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 6px; }
.stat-tile::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 4px; background: var(--mqtt-primary); }
.stat-tile.tone-green::before { background: var(--mqtt-green); }
.stat-tile.tone-gray::before { background: #7b8490; }
.stat-tile.tone-amber::before { background: var(--mqtt-amber); }
.stat-tile strong { display: block; margin-top: 10px; font-size: 23px; line-height: 1; }
.stat-label { color: var(--mqtt-muted); }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 22px 0 10px; }
.section-heading h2, .section-heading h3 { margin: 0; font-size: 15px; }
.section-heading button, .pagination-bar button { min-height: 30px; padding: 0 10px; color: var(--mqtt-text); background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 4px; }
.eyebrow { display: block; margin-bottom: 4px; color: var(--mqtt-muted); font-size: 11px; text-transform: uppercase; }

.table-wrap { width: 100%; overflow: auto; background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 6px; }
table { width: 100%; min-width: 760px; border-collapse: collapse; }
th, td { height: 42px; padding: 8px 11px; text-align: left; border-bottom: 1px solid #edf0f2; white-space: nowrap; }
th { color: var(--mqtt-muted); background: #f9fafb; font-size: 12px; font-weight: 600; }
tbody tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #f5f9fc; }
.mqtt-overview tbody tr { cursor: pointer; }
.empty-cell { height: 86px; color: var(--mqtt-muted); text-align: center; }
.actions-column { width: 84px; text-align: right; }
.row-actions { justify-content: flex-end; gap: 5px; }
.section-toolbar { justify-content: flex-end; gap: 8px; margin-bottom: 10px; }
.section-toolbar input { width: min(360px, 100%); height: 34px; padding: 0 10px; }
.pagination-bar { justify-content: space-between; gap: 12px; min-height: 48px; color: var(--mqtt-muted); }
.pagination-bar div { display: flex; gap: 6px; }

.client-detail { margin-top: 14px; padding-top: 1px; border-top: 1px solid var(--mqtt-border); }
.detail-grid { display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)); gap: 1px; margin: 0; overflow: hidden; background: var(--mqtt-border); border: 1px solid var(--mqtt-border); border-radius: 6px; }
.detail-grid div { min-height: 66px; padding: 11px 13px; background: var(--mqtt-panel); }
.detail-grid dt { margin-bottom: 7px; color: var(--mqtt-muted); }
.detail-grid dd { margin: 0; overflow-wrap: anywhere; }
.subscription-heading { margin-bottom: 8px; }
.inline-form { gap: 8px; }
.inline-form input { flex: 1; min-width: 100px; height: 34px; padding: 0 9px; }
.inline-form select { width: 92px; padding: 0 8px; }
.subscription-list { margin-top: 9px; background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 6px; }
.subscription-list > .muted { padding: 13px; margin: 0; }
.subscription-row { display: grid; grid-template-columns: minmax(120px, 1fr) 72px 34px; align-items: center; gap: 8px; min-height: 42px; padding: 5px 8px 5px 12px; border-bottom: 1px solid #edf0f2; }
.subscription-row:last-child { border-bottom: 0; }

.monitor-panels { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.chart-panel { padding: 14px; background: var(--mqtt-panel); border: 1px solid var(--mqtt-border); border-radius: 6px; }
.chart-panel h3 { margin: 0 0 12px; font-size: 13px; }
.bar-chart { display: flex; align-items: flex-end; gap: 4px; width: 100%; height: 150px; padding-top: 8px; border-bottom: 1px solid var(--mqtt-border); }
.bar-chart span { flex: 1 1 0; min-width: 3px; max-width: 22px; background: var(--mqtt-primary); border-radius: 2px 2px 0 0; }
.messages-chart span { background: var(--mqtt-green); }
.monitor-table { margin-top: 12px; }

.debug-grid { display: grid; grid-template-columns: minmax(300px, 420px) minmax(380px, 1fr); gap: 16px; }
.debug-controls, .debug-messages { min-width: 0; }
.debug-controls > .section-heading, .debug-messages > .section-heading { margin-top: 0; }
.connection-badge { padding: 3px 7px; color: var(--mqtt-muted); background: #edf0f2; border-radius: 3px; font-size: 11px; }
.connection-badge.connected { color: #176b49; background: #dff3e8; }
.endpoint-value { display: block; min-height: 37px; padding: 10px; overflow-wrap: anywhere; background: #edf2f5; border-radius: 4px; }
.stacked-form { display: grid; gap: 10px; margin-top: 12px; }
.stacked-form label:not(.checkbox-label) { display: grid; gap: 5px; color: var(--mqtt-muted); }
.stacked-form input, .stacked-form textarea { width: 100%; min-height: 34px; padding: 7px 9px; resize: vertical; }
.checkbox-label { gap: 7px; color: var(--mqtt-muted); }
.checkbox-label input { width: 15px; min-height: 15px; margin: 0; box-shadow: none; }
.debug-form { margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--mqtt-border); }
.debug-subscriptions { display: flex; flex-wrap: wrap; gap: 5px; min-height: 10px; margin-top: 8px; }
.debug-subscriptions button { min-height: 26px; padding: 2px 7px; color: var(--mqtt-primary); background: #e8f1f8; border: 0; border-radius: 3px; }
.debug-subscriptions span { margin-left: 4px; }
.publish-form { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--mqtt-border); }
.publish-options { justify-content: space-between; gap: 12px; }
.publish-options select { width: 92px; padding: 0 8px; }
.message-list { height: 590px; overflow: auto; padding: 10px; background: #20262d; border-radius: 6px; }
.message-row { margin-bottom: 8px; padding: 10px; color: #e9eef2; background: #2d353e; border-left: 3px solid var(--mqtt-green); border-radius: 3px; }
.message-row.out { border-left-color: #4d91c6; }
.message-row > div { display: flex; justify-content: space-between; gap: 10px; margin-bottom: 7px; color: #aeb9c3; font-size: 11px; }
.message-row code { display: block; margin-bottom: 7px; color: #8fd2b2; overflow-wrap: anywhere; }
.message-row pre { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; }
.message-list .mqtt-empty-state { color: #aeb9c3; }

@media (max-width: 960px) {
  .stats-grid { grid-template-columns: repeat(2, minmax(140px, 1fr)); }
  .detail-grid { grid-template-columns: repeat(2, minmax(140px, 1fr)); }
  .debug-grid { grid-template-columns: 1fr; }
  .message-list { height: 380px; }
}

@media (max-width: 640px) {
  .mqtt-header { position: static; align-items: stretch; flex-direction: column; }
  .mqtt-server-controls { justify-content: stretch; }
  .mqtt-server-controls select { flex: 1; width: auto; min-width: 0; }
  .mqtt-tabs { top: 0; overflow-x: auto; padding-left: 10px; padding-right: 10px; }
  .mqtt-tabs button { min-width: 78px; padding: 0 10px; }
  .mqtt-content { padding: 12px 10px; }
  .stats-grid, .monitor-panels, .detail-grid { grid-template-columns: 1fr; }
  .inline-form { align-items: stretch; flex-direction: column; }
  .inline-form select { width: 100%; }
  .pagination-bar { align-items: flex-start; flex-direction: column; }
}
</style>
