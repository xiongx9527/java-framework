import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const source = relativePath => readFile(fileURLToPath(new URL(`../src/${relativePath}`, import.meta.url)), 'utf8')

test('宿主工具栏通过通用插件扩展并保留 adminOnly 过滤', async () => {
    const toolbar = await source('components/panel/footer/magic-toolbar.vue')

    assert.match(toolbar, /constants\.PLUGINS\.filter\(it => it\.toolbars && it\.toolbars\.length > 0\)/)
    assert.match(toolbar, /const isPluginToolbarVisible = \(toolbar, pluginData\) => !toolbar\.adminOnly \|\| \(pluginData && pluginData\.adminEnabled === true\)/)
})

test('宿主状态栏通过通用插件 statusBarItems 扩展并在加载后刷新', async () => {
    const statusBar = await source('components/panel/footer/magic-status-bar.vue')

    assert.match(statusBar, /plugin\.statusBarItems && Array\.isArray\(plugin\.statusBarItems\)/)
    assert.match(statusBar, /insertPluginStatusBarItems\(array\)/)
    assert.match(statusBar, /refreshPluginStatusBarItems\(\)/)
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
