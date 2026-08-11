import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const consoleRoot = fileURLToPath(new URL('..', import.meta.url))
const pluginSource = readFileSync(path.join(consoleRoot, 'src/index.js'), 'utf8')
const dashboardSource = readFileSync(path.join(consoleRoot, 'src/components/magic-mqtt-dashboard.vue'), 'utf8')
const runtimeSource = readFileSync(path.join(consoleRoot, 'src/service/magic-mqtt-dashboard-runtime.js'), 'utf8')
const configurationSource = readFileSync(path.join(consoleRoot,
    '../../../../../fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfiguration.java'), 'utf8')
const hostToolbarSource = readFileSync(path.join(consoleRoot,
    '../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue'), 'utf8')

function createDashboardHarness() {
    const scriptStart = dashboardSource.indexOf('<script setup>') + '<script setup>'.length
    const scriptEnd = dashboardSource.indexOf('</script>', scriptStart)
    const scriptSource = dashboardSource.slice(scriptStart, scriptEnd).replace(/^import .*$/gm, '')
    const ref = value => ({ value })
    const reactive = value => value
    const computed = getter => Object.defineProperty({}, 'value', { get: getter })
    const browserWindow = {
        location: { protocol: 'http:', host: 'localhost', hostname: 'localhost', pathname: '/' },
        clearInterval,
        clearTimeout,
        setInterval,
        setTimeout
    }
    const storage = { getItem: () => null, setItem: () => {} }
    const factory = new Function(
        'computed', 'onMounted', 'onUnmounted', 'reactive', 'ref',
        'getMagicMqttDashboardRuntime', 'window', 'document', 'sessionStorage',
        `${scriptSource}\nreturn {
            debugConnected, debugError, debugMessages, debugPublish, debugSubscribe,
            debugSubscription, debugSubscriptions, debugUnsubscribe, publishForm,
            requestGeneration, selectedServerId,
            setMqttClient(client) { mqttClient = client }
        }`
    )
    return factory(
        computed, () => {}, () => {}, reactive, ref,
        () => ({ request: {}, modal: {}, constants: {}, format: key => key }),
        browserWindow, {}, storage
    )
}

function captureDebugOperation(harness, operation) {
    let callback
    const client = {
        publish: (topic, payload, options, handler) => { callback = handler },
        subscribe: (topic, options, handler) => { callback = handler },
        unsubscribe: (topic, handler) => { callback = handler }
    }
    harness.selectedServerId.value = 'default'
    harness.requestGeneration.value = 3
    harness.debugConnected.value = true
    harness.debugError.value = 'current error'
    harness.debugSubscription.topic = 'devices/alpha'
    harness.debugSubscriptions.value = ['devices/alpha']
    harness.publishForm.topic = 'devices/alpha'
    harness.publishForm.payload = 'payload'
    harness.setMqttClient(client)
    harness[operation](...(operation === 'debugUnsubscribe' ? ['devices/alpha'] : []))
    return callback
}

function debugState(harness) {
    return {
        error: harness.debugError.value,
        messages: harness.debugMessages.value.slice(),
        payload: harness.publishForm.payload,
        subscriptionTopic: harness.debugSubscription.topic,
        subscriptions: harness.debugSubscriptions.value.slice()
    }
}

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

test('MQTT admin 开关沿用 Kafka 插件元数据门控', () => {
    assert.match(configurationSource, /magic-api\.plugin\.mqtt\.admin\.enabled/)
    assert.match(configurationSource, /plugin\.setAdminEnabled\(Boolean\.parseBoolean\(adminEnabled\)\)/)
    assert.match(hostToolbarSource, /toolbar\.adminOnly/)
    assert.match(hostToolbarSource, /pluginData\.adminEnabled === true/)
})

test('Dashboard 使用宿主运行时并覆盖完整管理视图', () => {
    assert.match(runtimeSource, /request:\s*opt\.request/)
    assert.match(runtimeSource, /modal:\s*opt\.modal/)
    assert.match(runtimeSource, /constants:\s*opt\.constants/)
    assert.match(dashboardSource, /mqtt\/dashboard\/servers/)
    assert.match(dashboardSource, /dashboard/)
    assert.match(dashboardSource, /clients/)
    assert.match(dashboardSource, /monitor/)
    assert.match(dashboardSource, /debug/)
    assert.match(dashboardSource, /sessionStorage\.setItem\(SERVER_STORAGE_KEY/)
    assert.doesNotMatch(dashboardSource, /localStorage/)
})

test('多服务端切换会隔离旧状态且失效服务端不会静默回退', () => {
    assert.match(dashboardSource, /requestGeneration\.value \+= 1/)
    assert.match(dashboardSource, /stopMonitor\(\)\s+disconnectDebug\(true\)/)
    assert.match(dashboardSource, /verifySelectedServer\(generation, serverId\)/)
    assert.match(dashboardSource, /restored \|\| servers\.value\.find\(server => server\.defaultServer\) \|\| servers\.value\[0\]/)
})

test('异步加载 MQTT 库期间切换服务端会取消旧连接尝试', () => {
    const functionStart = dashboardSource.indexOf('async function toggleDebugConnection()')
    const functionEnd = dashboardSource.indexOf('\nfunction disconnectDebug', functionStart)
    const functionSource = dashboardSource.slice(functionStart, functionEnd)
    const awaitLibrary = functionSource.indexOf('await loadMqttLibrary()')

    assert.ok(functionStart >= 0 && functionEnd > functionStart)
    assert.ok(functionSource.indexOf('const generation = requestGeneration.value') < awaitLibrary)
    assert.ok(functionSource.indexOf('const serverId = selectedServerId.value') < awaitLibrary)
    assert.ok(functionSource.indexOf('const endpoint = debugEndpoint.value') < awaitLibrary)
    assert.match(functionSource.slice(awaitLibrary), /if \(!isCurrent\(generation, serverId\) \|\| !debugConnecting\.value\) return/)
    assert.match(functionSource, /mqtt\.connect\(endpoint, connectOptions\)/)
})

test('过期 MQTT 操作回调不会修改当前服务端的调试状态', () => {
    const operations = ['debugSubscribe', 'debugUnsubscribe', 'debugPublish']
    const staleContexts = [
        ['请求代际变化', harness => { harness.requestGeneration.value += 1 }],
        ['服务端变化', harness => { harness.selectedServerId.value = 'datasource:other' }],
        ['MQTT 客户端变化', harness => { harness.setMqttClient({}) }]
    ]

    for (const operation of operations) {
        for (const [contextName, makeStale] of staleContexts) {
            const harness = createDashboardHarness()
            const callback = captureDebugOperation(harness, operation)
            const expected = debugState(harness)
            makeStale(harness)

            callback(new Error('late failure'))
            callback(null)

            assert.deepEqual(debugState(harness), expected, `${operation} 应忽略${contextName}后的回调`)
        }
    }
})

test('MQTT 调试地址由所选服务端生成且密码不进入浏览器存储', () => {
    assert.match(dashboardSource, /const debugEndpoint = computed/)
    assert.match(dashboardSource, /password: debugForm\.password \|\| undefined/)
    assert.doesNotMatch(dashboardSource, /v-model(?:\.trim)?="debugForm\.host"/)
    const storageStatements = dashboardSource.split('\n').filter(line => line.includes('Storage.'))
    assert.equal(storageStatements.some(line => line.toLowerCase().includes('password')), false)
})

test('旧状态栏页面被移除但 MQTT 调试库仍随 jar 提供', () => {
    const resourceRoot = path.join(consoleRoot, '../main/resources/magic-editor/mqtt')
    const noticePath = path.join(consoleRoot, '../main/resources/META-INF/NOTICE-mica-mqtt-dashboard.txt')
    assert.equal(existsSync(path.join(resourceRoot, 'index.html')), false)
    assert.equal(existsSync(path.join(resourceRoot, 'js/mqtt.min.js')), true)
    assert.equal(existsSync(noticePath), true)
    assert.match(dashboardSource, /mica-mqtt-dashboard/)
    assert.match(dashboardSource, /Apache License, Version 2\.0/)
})
