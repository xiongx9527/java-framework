import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import * as pluginExtension from '../src/scripts/plugin-extension.js'

const source = relativePath => readFile(fileURLToPath(new URL(`../src/${relativePath}`, import.meta.url)), 'utf8')

const createStatusBarItem = (id, order, displayKey = id) => ({
    id,
    icon: `${id}-icon`,
    title: `${id}-title`,
    displayKey,
    order,
    onClick() {}
})

const createPluginsChangedBus = () => {
    const listeners = []
    return {
        subscribe(listener) {
            listeners.push(listener)
        },
        emitPluginsChanged() {
            listeners.forEach(listener => listener())
        }
    }
}

test('状态栏首次刷新会过滤配置禁用项和无效项，并稳定排序可见插件项', () => {
    assert.equal(typeof pluginExtension.createPluginStatusBarItemsController, 'function')
    if (typeof pluginExtension.createPluginStatusBarItemsController !== 'function') {
        return
    }

    const visibleItems = []
    const controller = pluginExtension.createPluginStatusBarItemsController({
        getPlugins: () => [{
            statusBarItems: [
                createStatusBarItem('later', undefined),
                createStatusBarItem('hidden', 1, 'hiddenFeature'),
                createStatusBarItem('second', 10),
                createStatusBarItem('first', 10),
                { id: 'invalid', icon: 'invalid', title: 'invalid', displayKey: 'invalid' }
            ]
        }],
        getHeader: () => ({ hiddenFeature: false }),
        setItems: items => visibleItems.splice(0, visibleItems.length, ...items),
        subscribe() {}
    })

    controller.refresh()

    assert.deepEqual(visibleItems.map(item => item.id), ['second', 'first', 'later'])
})

test('PLUGINS_CHANGED 后刷新会使用变化后的插件集合更新可见状态栏项', () => {
    assert.equal(typeof pluginExtension.createPluginStatusBarItemsController, 'function')
    if (typeof pluginExtension.createPluginStatusBarItemsController !== 'function') {
        return
    }

    const plugins = [{ statusBarItems: [createStatusBarItem('initial', 20)] }]
    const visibleItems = []
    const bus = createPluginsChangedBus()
    const controller = pluginExtension.createPluginStatusBarItemsController({
        getPlugins: () => plugins,
        getHeader: () => ({}),
        setItems: items => visibleItems.splice(0, visibleItems.length, ...items),
        subscribe: listener => bus.subscribe(listener)
    })

    controller.refresh()
    assert.deepEqual(visibleItems.map(item => item.id), ['initial'])

    plugins.splice(0, plugins.length, {
        statusBarItems: [createStatusBarItem('newer', 20), createStatusBarItem('newer-first', 10)]
    })
    bus.emitPluginsChanged()

    assert.deepEqual(visibleItems.map(item => item.id), ['newer-first', 'newer'])
})

test('宿主工具栏通过通用插件扩展并保留 adminOnly 过滤', async () => {
    const toolbar = await source('components/panel/footer/magic-toolbar.vue')

    assert.match(toolbar, /constants\.PLUGINS\.filter\(it => it\.toolbars && it\.toolbars\.length > 0\)/)
    assert.match(toolbar, /const isPluginToolbarVisible = \(toolbar, pluginData\) => !toolbar\.adminOnly \|\| \(pluginData && pluginData\.adminEnabled === true\)/)
    assert.match(toolbar, /stableSortByOrder\(pluginToolbars\)/)
})

test('宿主状态栏接入通用插件刷新控制器并在加载后刷新', async () => {
    const statusBar = await source('components/panel/footer/magic-status-bar.vue')

    assert.match(statusBar, /createPluginStatusBarItemsController/)
    assert.match(statusBar, /getPlugins: \(\) => constants\.PLUGINS/)
    assert.match(statusBar, /getHeader: \(\) => props\.config\.header/)
    assert.match(statusBar, /subscribe: listener => bus\.\$on\(Message\.PLUGINS_CHANGED, listener\)/)
    assert.match(statusBar, /insertPluginStatusBarItems\(array\)/)
    assert.match(statusBar, /refreshPluginStatusBarItems\(\)/)
})

test('宿主状态栏不保留插件专属逻辑，并继续使用通用插件契约', async () => {
    const statusBar = await source('components/panel/footer/magic-status-bar.vue')
    const forbidden = [
        /has[A-Z]\w+Plugin/,
        /checkPlugin/,
        /open(Config|Ftl|Dtp|Langchain4j|Debezium|Satoken|Ws|Mqtt|Druid|Swagger|SpringBootAdmin|Eureka|Vue3)/,
        /加载完毕|Loaded/,
        /setInterval\(/
    ]

    forbidden.forEach(pattern => assert.doesNotMatch(statusBar, pattern))
    assert.match(statusBar, /array\.splice\(0, 0, \.\.\.items\)/)
    assert.match(statusBar, /config\.header\[it\.displayKey\]/)
    assert.match(statusBar, /Message\.PLUGINS_CHANGED/)
})

test('宿主不再包含 Kafka 或 Pulsar 专属 UI 分支和文案', async () => {
    const [toolbar, statusBar, english, chinese] = await Promise.all([
        source('components/panel/footer/magic-toolbar.vue'),
        source('components/panel/footer/magic-status-bar.vue'),
        source('scripts/i18n/en.js'),
        source('scripts/i18n/zh-cn.js')
    ])

    assert.doesNotMatch(toolbar, /plugin\.name === '(?:kafka|pulsar)'/)
    assert.doesNotMatch(statusBar, /\b(?:hasKafkaPlugin|hasPulsarPlugin|openKafka|openPulsar|magic-kafka|magic-pulsar|kafka-subscribe|pulsar-subscribe)\b/)
    assert.doesNotMatch(english, /^\s{4}(?:kafka|pulsar):\s*\{/m)
    assert.doesNotMatch(chinese, /^\s{4}(?:kafka|pulsar):\s*\{/m)
})
