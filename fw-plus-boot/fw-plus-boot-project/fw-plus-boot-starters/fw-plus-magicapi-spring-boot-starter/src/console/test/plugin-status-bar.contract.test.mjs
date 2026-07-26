import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const pluginRoot = path.resolve(testDir, '../../../../fw-plus-magicapi-plugin')

const escapeRegExp = value => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const plugins = [{
    name: 's3',
    icon: '#magic-s3-s3',
    title: 'X-file-storage',
    displayKey: 's3',
    order: 100,
    action: /window\.open\('https:\/\/x-file-storage\.xuyanwu\.cn\/', '_blank'\)/
}, {
    name: 'kafka',
    icon: '#magic-kafka-kafka',
    title: 'Kafka',
    displayKey: 'kafka',
    order: 200,
    action: /window\.open\('https:\/\/kafka\.apache\.org\/', '_blank'\)/
}, {
    name: 'pulsar',
    icon: '#magic-pulsar-pulsar',
    title: 'Pulsar',
    displayKey: 'pulsar',
    order: 300,
    action: /runtime\.bus\.\$emit\(runtime\.Message\.ADD_FOOTER_TOOLBAR, pulsarAdminToolbar\)/
}, {
    name: 'config',
    icon: '#magic-config-config',
    title: 'Config',
    displayKey: 'config',
    order: 400,
    action: /window\.open\('https:\/\/spring\.io\/projects\/spring-cloud-config\/', '_blank'\)/
}, {
    name: 'freemarker',
    icon: '#magic-freemarker-ftl',
    title: 'Freemarker',
    displayKey: 'freemarker',
    order: 500,
    action: /window\.open\('https:\/\/freemarker\.apache\.org\/', '_blank'\)/
}, {
    name: 'dtp',
    icon: '#magic-dtp-dtp',
    title: 'Dynamictp',
    displayKey: 'dtp',
    order: 600,
    action: /window\.open\('https:\/\/dynamictp\.cn\/', '_blank'\)/
}, {
    name: 'debezium',
    icon: '#magic-debezium-debezium',
    title: 'Debezium',
    displayKey: 'debezium',
    order: 700,
    action: /window\.open\('https:\/\/debezium\.io\/', '_blank'\)/
}, {
    name: 'ws',
    icon: '#magic-ws-ws',
    title: 'Websocket client',
    displayKey: 'ws',
    order: 900,
    action: /onClick:\s*\(\)\s*=>\s*openWs\(opt\.constants\s*&&\s*opt\.constants\.BASE_URL\)/
}, {
    name: 'mqtt',
    icon: '#magic-mqtt-mqtt',
    title: 'Mqtt client',
    displayKey: 'mqtt',
    order: 1000,
    action: /onClick:\s*\(\)\s*=>\s*openMqtt\(opt\.constants\s*&&\s*opt\.constants\.BASE_URL\)/
}, {
    name: 'vue3',
    icon: '#magic-vue3-vue',
    title: 'Vue SFC',
    displayKey: 'vue3',
    order: 1300,
    action: /window\.open\('https:\/\/play\.vuejs\.org\/', '_blank'\)/
}, {
    name: 'satoken',
    icon: '#magic-satoken-satoken',
    title: 'Satoken',
    displayKey: 'satoken',
    order: 1400,
    action: /window\.open\('https:\/\/sa-token\.cc\/', '_blank'\)/
}]

test('Boot 插件状态栏入口保持原标题、图标、显示键、动作和稳定顺序', () => {
    for (const plugin of plugins) {
        const pluginPath = path.join(pluginRoot, `fw-plus-magicapi-plugin-${plugin.name}/src/console/src/index.js`)
        const source = fs.readFileSync(pluginPath, 'utf8')
        const statusBarItems = source.match(/statusBarItems:\s*\[\{([\s\S]*?)\}\]/)

        assert.ok(statusBarItems, `${plugin.name} 应注册 statusBarItems`)
        const item = statusBarItems[1]
        assert.match(item, new RegExp(`icon:\\s*'${escapeRegExp(plugin.icon)}'`), `${plugin.name} 应保留原图标语义`)
        assert.match(item, new RegExp(`title:\\s*'${escapeRegExp(plugin.title)}'`), `${plugin.name} 应保留原标题`)
        assert.match(item, new RegExp(`displayKey:\\s*'${escapeRegExp(plugin.displayKey)}'`), `${plugin.name} 应保留 displayKey`)
        assert.match(item, new RegExp(`order:\\s*${plugin.order}(?:,|\\s)`), `${plugin.name} 应使用约定顺序`)
        assert.match(item, plugin.action, `${plugin.name} 应保留原点击行为`)
    }
})

for (const plugin of [{ name: 'ws', suffix: 'ws' }, { name: 'mqtt', suffix: 'mqtt' }]) {
    test(`${plugin.name} 状态栏入口使用插件工厂运行时后端基址`, () => {
        const pluginPath = path.join(pluginRoot, `fw-plus-magicapi-plugin-${plugin.name}/src/console/src/index.js`)
        const source = fs.readFileSync(pluginPath, 'utf8')

		assert.doesNotMatch(source, /import\.meta\.env\.MAGIC_API_BASE_URL/)
		assert.match(source, new RegExp(`open${plugin.name === 'ws' ? 'Ws' : 'Mqtt'}\\(opt\\.constants\\s*&&\\s*opt\\.constants\\.BASE_URL\\)`))
		assert.match(source, new RegExp(`return link \\+ '/${plugin.suffix}/index\\.html'`))
		assert.match(source, new RegExp(`window\\.open\\(resolve${plugin.name === 'ws' ? 'Ws' : 'Mqtt'}Url\\(baseURL, pageURL\\), '_blank'\\)`))
    })
}
