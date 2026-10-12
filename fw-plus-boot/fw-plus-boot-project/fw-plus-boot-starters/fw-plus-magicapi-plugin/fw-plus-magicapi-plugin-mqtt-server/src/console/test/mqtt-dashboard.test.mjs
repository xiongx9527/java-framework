import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const consoleRoot = fileURLToPath(new URL('..', import.meta.url))
const read = relativePath => readFileSync(path.join(consoleRoot, relativePath), 'utf8')
const pluginSource = read('src/index.js')
const wrapperSource = read('src/components/magic-mqtt-dashboard.vue')
const runtimeSource = read('src/service/magic-mqtt-dashboard-runtime.js')
const configurationSource = read('../../../../../fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfiguration.java')
const hostToolbarSource = read('../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue')

test('MQTT Dashboard 仅注册 Kafka 风格的 admin 工具栏', () => {
    assert.match(pluginSource, /name:\s*'mqtt'/)
    assert.match(pluginSource, /toolbars:\s*\[/)
    assert.match(pluginSource, /id:\s*'mqtt-dashboard'/)
    assert.match(pluginSource, /component:\s*MagicMqttDashboard/)
    assert.match(pluginSource, /adminOnly:\s*true/)
    assert.match(pluginSource, /allowClose:\s*true/)
    assert.match(pluginSource, /fullscreen:\s*true/)
    assert.doesNotMatch(pluginSource, /statusBarItems/)
})

test('MQTT admin 开关沿用插件元数据门控', () => {
    assert.match(configurationSource, /magic-api\.plugin\.mqtt\.admin\.enabled/)
    assert.match(configurationSource, /plugin\.setAdminEnabled\(Boolean\.parseBoolean\(adminEnabled\)\)/)
    assert.match(hostToolbarSource, /toolbar\.adminOnly/)
    assert.match(hostToolbarSource, /pluginData\.adminEnabled === true/)
})

test('Magic-API 包装器挂载和卸载完整上游应用', () => {
    assert.match(runtimeSource, /request:\s*opt\.request/)
    assert.match(runtimeSource, /modal:\s*opt\.modal/)
    assert.match(runtimeSource, /constants:\s*opt\.constants/)
    assert.match(runtimeSource, /dashboard:\s*opt\.dashboard/)
    assert.match(wrapperSource, /createMicaMqttDashboard/)
    assert.match(wrapperSource, /dashboard\.unmount\(\)/)
    assert.doesNotMatch(wrapperSource, /debugPublish|monitorSamples|clientSubscriptions/)
})

test('API 适配覆盖上游全部管理契约且不接受任意后端地址', () => {
    const apiSource = read('src/dashboard/utils/api.ts')
    const adapterSource = read('src/dashboard/utils/upstream-api-adapter.ts')
    for (const endpoint of [
        '/api/v1/stats', '/api/v1/clients', '/api/v1/clients/info',
        '/api/v1/client/subscriptions', '/api/v1/clients/delete',
        '/api/v1/mqtt/subscribe', '/api/v1/mqtt/unsubscribe'
    ]) {
        assert.ok(adapterSource.includes(`'${endpoint}'`), `缺少 ${endpoint} 适配`)
    }
    assert.match(apiSource, /getDashboardRuntime/)
    assert.match(apiSource, /assertCurrentGeneration/)
    assert.doesNotMatch(read('src/dashboard/views/Layout.vue'), /apiBaseUrl|管理地址/)
})

test('多服务端切换会停止旧监控和调试连接并清理状态', () => {
    const serverSource = read('src/dashboard/stores/server.ts')
    assert.match(serverSource, /stopRealtimeMonitor\(\)/)
    assert.match(serverSource, /disconnect\(true\)/)
    assert.match(serverSource, /resetForServer\(\)/)
    assert.match(serverSource, /sessionStorage\.setItem\(SERVER_STORAGE_KEY/)
    assert.match(serverSource, /find\(server => server\.defaultServer\)/)
    assert.match(serverSource, /当前 MQTT 服务端已停止或删除/)
})

test('MQTT 调试地址由所选服务端生成且密码不进入浏览器存储', () => {
    const debugSource = read('src/dashboard/stores/debug.ts')
    assert.match(debugSource, /bindServer/)
    assert.match(debugSource, /mqtt\.connect\(endpoint/)
    assert.match(debugSource, /password:\s*connection\.value\.password \|\| undefined/)
    assert.doesNotMatch(debugSource, /(?:local|session)Storage/)
    const viewSource = read('src/dashboard/views/Debug.vue')
    assert.match(viewSource, /connection\.host/)
    assert.match(viewSource, /connection\.port/)
    assert.match(viewSource, /:disabled="true"/)
})

test('上游许可证和插件资源随源码及 jar 提供', () => {
    const resourceRoot = path.join(consoleRoot, '../main/resources/magic-editor/mqtt')
    const noticePath = path.join(consoleRoot, '../main/resources/META-INF/NOTICE-mica-mqtt-dashboard.txt')
    const licensePath = path.join(consoleRoot, '../main/resources/META-INF/LICENSE-mica-mqtt-dashboard.txt')
    assert.equal(existsSync(path.join(resourceRoot, 'index.html')), false)
    assert.equal(existsSync(path.join(resourceRoot, 'js/mqtt.min.js')), false)
    assert.equal(existsSync(noticePath), true)
    assert.equal(existsSync(licensePath), true)
    assert.match(read('src/dashboard/LICENSE'), /Apache License/)
    assert.match(wrapperSource, /Apache-2\.0/)
})

test('Basic 认证保留上游记住密码行为且登录前可选择服务端', () => {
    const basicSource = read('src/dashboard/auth/basic.ts')
    assert.match(basicSource, /localStorage\.setItem\(MANAGEMENT_PASSWORD_KEY/)
    assert.match(basicSource, /localStorage\.getItem\(MANAGEMENT_PASSWORD_KEY/)
    assert.match(basicSource, /管理认证凭据/)
    const loginSource = read('src/dashboard/views/Login.vue')
    assert.match(loginSource, /useServerStore/)
    assert.match(loginSource, /selectedServerId/)
    assert.match(loginSource, /serverStore\.initialize/)
    assert.doesNotMatch(loginSource, /100vh/)
})
