import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const pluginRoot = path.resolve(testDir, '../../../../fw-plus-magicapi-plugin')

const plugins = [{
    name: 'config',
    orders: [100]
}, {
    name: 'db',
    orders: [200]
}, {
    name: 'freemarker',
    orders: [300, 310]
}, {
    name: 'kafka',
    orders: [400, 410]
}, {
    name: 'mcp',
    orders: [500, 510]
}, {
    name: 'mqttclient',
    orders: [600]
}, {
    name: 'pulsar',
    orders: [700]
}, {
    name: 'rabbitmq',
    orders: [800]
}, {
    name: 'rocketmq',
    orders: [900]
}, {
    name: 's3',
    orders: [1000]
}, {
    name: 'satoken',
    orders: [1100]
}, {
    name: 'vue3',
    orders: [1200, 1210]
}, {
    name: 'word',
    orders: [1300, 1310, 1320]
}, {
    name: 'ws',
    orders: [1400]
}]

const extractToolbarItems = source => {
    const start = source.indexOf('toolbars: [')
    assert.notEqual(start, -1, '应注册 toolbars')

    const items = []
    let objectStart = -1
    let depth = 0
    let quote = null
    let escaped = false
    for (let index = source.indexOf('[', start) + 1; index < source.length; index++) {
        const character = source[index]
        if (quote) {
            if (escaped) {
                escaped = false
            } else if (character === '\\') {
                escaped = true
            } else if (character === quote) {
                quote = null
            }
            continue
        }
        if (character === '\'' || character === '"' || character === '`') {
            quote = character
        } else if (character === '{') {
            if (depth === 0) {
                objectStart = index
            }
            depth++
        } else if (character === '}') {
            depth--
            if (depth === 0) {
                items.push(source.slice(objectStart, index + 1))
            }
        } else if (character === ']' && depth === 0) {
            return items
        }
    }
    throw new Error('toolbars 未正确闭合')
}

test('所有静态插件 toolbar 都有约定的有限数值 order', () => {
    for (const plugin of plugins) {
        const pluginPath = path.join(pluginRoot, `fw-plus-magicapi-plugin-${plugin.name}/src/console/src/index.js`)
        const toolbarItems = extractToolbarItems(fs.readFileSync(pluginPath, 'utf8'))

        assert.equal(toolbarItems.length, plugin.orders.length, `${plugin.name} toolbar 项数量应保持不变`)
        toolbarItems.forEach((item, index) => {
            const order = item.match(/\border:\s*(-?(?:\d+\.?\d*|\.\d+)(?:e[+-]?\d+)?)/i)
            assert.ok(order, `${plugin.name} 的第 ${index + 1} 个 toolbar 应声明数值 order`)
            assert.ok(Number.isFinite(Number(order[1])), `${plugin.name} 的第 ${index + 1} 个 toolbar order 应为有限数值`)
            assert.equal(Number(order[1]), plugin.orders[index], `${plugin.name} 的第 ${index + 1} 个 toolbar 应使用约定顺序`)
        })
    }
})
