import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const consoleRoot = fileURLToPath(new URL('..', import.meta.url))
const dashboardRoot = path.join(consoleRoot, 'src/dashboard')
const read = relativePath => readFileSync(path.join(consoleRoot, relativePath), 'utf8')

const upstreamModules = [
    'src/dashboard/App.vue',
    'src/dashboard/router/index.ts',
    'src/dashboard/stores/app.ts',
    'src/dashboard/stores/auth.ts',
    'src/dashboard/stores/client.ts',
    'src/dashboard/stores/debug.ts',
    'src/dashboard/stores/monitor.ts',
    'src/dashboard/views/Layout.vue',
    'src/dashboard/views/Login.vue',
    'src/dashboard/views/Dashboard.vue',
    'src/dashboard/views/Clients.vue',
    'src/dashboard/views/ClientDetail.vue',
    'src/dashboard/views/Monitor.vue',
    'src/dashboard/views/Debug.vue'
]

test('直接保留 mica-mqtt-dashboard 上游模块化源码结构', () => {
    for (const modulePath of upstreamModules) {
        assert.equal(existsSync(path.join(consoleRoot, modulePath)), true, `${modulePath} 应存在`)
    }
    const provenance = JSON.parse(read('src/dashboard/upstream.json'))
    assert.equal(provenance.repository, 'https://gitee.com/dreamlu/mica-mqtt-dashboard')
    assert.equal(provenance.commit, '2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80')
    assert.equal(existsSync(path.join(dashboardRoot, 'LICENSE')), true)
})

test('Magic-API 工具栏只通过薄包装器挂载上游应用', () => {
    const wrapper = read('src/components/magic-mqtt-dashboard.vue')
    assert.match(wrapper, /createMicaMqttDashboard/)
    assert.match(wrapper, /onMounted/)
    assert.match(wrapper, /onUnmounted/)
    assert.doesNotMatch(wrapper, /debugPublish|monitorSamples|clientSubscriptions|mqtt\.connect/)
    assert.ok(wrapper.split('\n').length < 100, '包装器不应重新实现上游页面')
})

test('插件本地打包上游全部运行依赖并由 JavaScript 注入 CSS', () => {
    const pkg = JSON.parse(read('package.json'))
    for (const dependency of [
        '@element-plus/icons-vue', 'axios', 'dayjs', 'echarts', 'element-plus',
        'mqtt', 'pinia', 'vue-echarts', 'vue-router'
    ]) {
        assert.ok(pkg.dependencies?.[dependency], `缺少上游运行依赖 ${dependency}`)
    }
    assert.ok(pkg.devDependencies?.['vite-plugin-css-injected-by-js'])
    const vite = read('vite.config.ts')
    assert.match(vite, /cssInjectedByJsPlugin/)
    assert.match(vite, /formats:\s*\['iife'\]/)
    assert.match(vite, /external:\s*\['vue'\]/)
})

test('上游应用通过内存路由嵌入宿主且不覆盖 Magic-API 地址', () => {
    const router = read('src/dashboard/router/index.ts')
    assert.match(router, /createMemoryHistory/)
    assert.doesNotMatch(router, /createWebHistory/)
    const bootstrap = read('src/dashboard/bootstrap.ts')
    assert.match(bootstrap, /createApp/)
    assert.match(bootstrap, /createPinia/)
    assert.match(bootstrap, /app\.use\(router\)/)
})

test('多服务端选择是上游应用的统一管理上下文', () => {
    const serverStore = read('src/dashboard/stores/server.ts')
    assert.match(serverStore, /\/mqtt\/dashboard\/servers/)
    assert.match(serverStore, /sessionStorage/)
    assert.match(serverStore, /generation/)
    assert.match(serverStore, /stopRealtimeMonitor/)
    assert.match(serverStore, /disconnect/)
    const api = read('src/dashboard/utils/api.ts')
    const adapter = read('src/dashboard/utils/upstream-api-adapter.ts')
    assert.match(api, /mapUpstreamApiRequest/)
    assert.match(adapter, /\/mqtt\/dashboard\/\$\{encodeURIComponent\(serverId\)\}/)
    assert.match(api, /generation/)
})

test('MQTT 调试依赖本地 mqtt 包且调试密码不会持久化', () => {
    const debugStore = read('src/dashboard/stores/debug.ts')
    assert.match(debugStore, /from 'mqtt'/)
    assert.doesNotMatch(debugStore, /createElement\(['"]script|https?:\/\//)
    const persistentWrites = debugStore.split('\n').filter(line => /(?:local|session)Storage\.setItem/.test(line))
    assert.equal(persistentWrites.some(line => /password/i.test(line)), false)
})

test('布局保留上游导航主题并提供后端驱动的服务端选择器', () => {
    const layout = read('src/dashboard/views/Layout.vue')
    assert.match(layout, /useServerStore/)
    assert.match(layout, /serverStore\.initialize/)
    assert.match(layout, /selectedServerId/)
    assert.match(layout, /server\.id/)
    assert.match(layout, /toggleSidebar/)
    assert.match(layout, /toggleTheme/)
    assert.match(layout, /router-view/)
})

test('宿主认证为默认模式且 Basic OAuth 实现仍保留', () => {
    const authFactory = read('src/dashboard/auth/index.ts')
    assert.match(authFactory, /createHostAuthProvider/)
    assert.match(authFactory, /createBasicAuthProvider/)
    assert.match(authFactory, /createOidcAuthProvider/)
    assert.match(authFactory, /authMode.*host/)
    assert.equal(existsSync(path.join(dashboardRoot, 'auth/basic.ts')), true)
    assert.equal(existsSync(path.join(dashboardRoot, 'auth/oidc.ts')), true)
})

test('调试地址绑定所选服务端且界面不能输入任意主机端口', () => {
    const debugStore = read('src/dashboard/stores/debug.ts')
    assert.match(debugStore, /bindServer/)
    assert.match(debugStore, /websocketEnabled/)
    assert.match(debugStore, /mqtt\.connect\(endpoint/)
    assert.match(debugStore, /disconnect\(true\)/)
    const debugView = read('src/dashboard/views/Debug.vue')
    assert.match(debugView, /:disabled="true"/)
    assert.match(debugView, /connection\.host/)
    assert.match(debugView, /connection\.port/)
})

test('监控保留上游趋势并支持配置数据保留数量', () => {
    const monitorStore = read('src/dashboard/stores/monitor.ts')
    assert.match(monitorStore, /maxDataPoints/)
    assert.match(monitorStore, /slice\(-maxDataPoints\.value\)/)
    assert.match(monitorStore, /resetForServer/)
    const monitorView = read('src/dashboard/views/Monitor.vue')
    assert.match(monitorView, /v-model="maxDataPoints"/)
    assert.match(monitorView, /v-chart/)
})

test('客户端和监控 store 在服务端切换时清理旧数据', () => {
    assert.match(read('src/dashboard/stores/client.ts'), /resetForServer/)
    assert.match(read('src/dashboard/stores/monitor.ts'), /resetForServer/)
    assert.match(read('src/dashboard/stores/debug.ts'), /resetForServer/)
})

test('嵌入应用只占工具栏容器且主题不会修改宿主 documentElement', () => {
    const app = read('src/dashboard/App.vue')
    assert.match(app, /mica-mqtt-dashboard-app/)
    assert.match(app, /height:\s*100%/)
    assert.doesNotMatch(app, /100vh|100vw|\bbody\s*\{|html\.dark/)
    const appStore = read('src/dashboard/stores/app.ts')
    assert.doesNotMatch(appStore, /document\.documentElement/)
    const layout = read('src/dashboard/views/Layout.vue')
    assert.match(layout, /dark:\s*theme === 'dark'/)
})

test('状态监控可显式启停并展示字节趋势', () => {
    const monitorView = read('src/dashboard/views/Monitor.vue')
    assert.match(monitorView, /realtimeEnabled/)
    assert.match(monitorView, /toggleRealtimeMonitor/)
    assert.match(monitorView, /bytesChartOption/)
    assert.match(monitorView, /接收字节/)
    assert.match(monitorView, /发送字节/)
    assert.match(monitorView, /startRealtimeMonitor/)
    assert.match(monitorView, /stopRealtimeMonitor/)
})

test('前端依赖通过 npm shrinkwrap 锁定以保证 Maven 构建可复现', () => {
    const shrinkwrapPath = path.join(consoleRoot, 'npm-shrinkwrap.json')
    assert.equal(existsSync(shrinkwrapPath), true)
    const shrinkwrap = JSON.parse(readFileSync(shrinkwrapPath, 'utf8'))
    assert.equal(shrinkwrap.lockfileVersion, 3)
    assert.ok(shrinkwrap.packages?.['node_modules/mqtt'])
    assert.ok(shrinkwrap.packages?.['node_modules/element-plus'])
})

test('Basic 和 OAuth 登录在获得临时凭据后再加载受保护的服务端列表', () => {
    const login = read('src/dashboard/views/Login.vue')
    assert.match(login, /serverStore\.loadServers\(\{\s*auth:/)
    assert.match(login, /handleCallback[\s\S]*serverStore\.initialize\(\)/)
    const serverStore = read('src/dashboard/stores/server.ts')
    assert.match(serverStore, /loadServers = async \(requestConfig/)
    assert.match(serverStore, /api\.get[^\n]*\/mqtt\/dashboard\/servers[^\n]*requestConfig/)
})

test('服务端在请求期间失效时会清空旧数据并要求重新选择', () => {
    const context = read('src/dashboard/context.ts')
    assert.match(context, /setServerUnavailableHandler/)
    assert.match(context, /notifyServerUnavailable/)
    const api = read('src/dashboard/utils/api.ts')
    assert.match(api, /notifyServerUnavailable/)
    assert.match(api, /服务端不存在或未运行/)
    const serverStore = read('src/dashboard/stores/server.ts')
    assert.match(serverStore, /setServerUnavailableHandler/)
    assert.match(serverStore, /applyServer\(null\)/)
})

test('Axios 直连模式把 JsonBean 非成功 code 转为异常', () => {
    const api = read('src/dashboard/utils/api.ts')
    assert.match(api, /response\.data\?\.code !== 1/)
    assert.match(api, /response\.data\?\.message/)
    assert.match(api, /throw error/)
})

test('管理认证凭据与 MQTT 调试凭据使用明确分离的存储边界', () => {
    const basic = read('src/dashboard/auth/basic.ts')
    assert.match(basic, /MANAGEMENT_USERNAME_KEY/)
    assert.match(basic, /MANAGEMENT_PASSWORD_KEY/)
    assert.match(basic, /独立 Dashboard 的管理认证凭据/)
    assert.doesNotMatch(basic, /mqtt_username|mqtt_password/)
    const debug = read('src/dashboard/stores/debug.ts')
    assert.doesNotMatch(debug, /localStorage|sessionStorage|MANAGEMENT_PASSWORD_KEY/)
})

test('上游 API 契约由独立适配模块显式映射到多服务端后端路径', () => {
    const adapterPath = path.join(dashboardRoot, 'utils/upstream-api-adapter.ts')
    assert.equal(existsSync(adapterPath), true)
    const adapter = read('src/dashboard/utils/upstream-api-adapter.ts')
    for (const [upstream, backend] of [
        ['/api/v1/stats', '/stats'],
        ['/api/v1/clients', '/clients'],
        ['/api/v1/clients/info', '/clients/info'],
        ['/api/v1/client/subscriptions', '/client/subscriptions'],
        ['/api/v1/clients/delete', '/clients/delete'],
        ['/api/v1/mqtt/subscribe', '/mqtt/subscribe'],
        ['/api/v1/mqtt/unsubscribe', '/mqtt/unsubscribe']
    ]) {
        assert.ok(adapter.includes(`'${upstream}': '${backend}'`), `缺少 ${upstream} -> ${backend}`)
    }
    assert.match(adapter, /\/mqtt\/dashboard\/\$\{encodeURIComponent\(serverId\)\}/)
    assert.match(read('src/dashboard/utils/api.ts'), /mapUpstreamApiRequest/)
})
