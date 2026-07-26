import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'acorn'

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

const getPropertyName = property => {
    if (property.type !== 'Property') {
        return undefined
    }
    if (property.computed) {
        return property.key.type === 'Literal' && typeof property.key.value === 'string'
            ? property.key.value
            : undefined
    }
    return property.key.type === 'Identifier' ? property.key.name : property.key.value
}

const findProperty = (objectExpression, name) => {
    const properties = objectExpression.properties.filter(property => getPropertyName(property) === name)
    assert.equal(properties.length, 1, `工厂返回对象应声明一个 ${name} 属性`)
    return properties[0]
}

const getFactoryReturnObject = source => {
    const program = parse(source, {
        ecmaVersion: 'latest',
        sourceType: 'module'
    })
    const defaultExports = program.body.filter(statement => statement.type === 'ExportDefaultDeclaration')
    assert.equal(defaultExports.length, 1, '插件应有且仅有一个 export default')

    const factory = defaultExports[0].declaration
    assert.ok(
        factory.type === 'ArrowFunctionExpression'
        || factory.type === 'FunctionExpression'
        || factory.type === 'FunctionDeclaration',
        'export default 应导出插件工厂函数'
    )
    if (factory.body.type === 'ObjectExpression') {
        return factory.body
    }

    assert.equal(factory.body.type, 'BlockStatement', '插件工厂应返回对象')
    const returns = factory.body.body.filter(statement => statement.type === 'ReturnStatement')
    assert.equal(returns.length, 1, '插件工厂顶层应有且仅有一个 return')
    assert.equal(returns[0].argument?.type, 'ObjectExpression', '插件工厂应直接返回对象')
    return returns[0].argument
}

const getStaticNumericValue = expression => {
    if (expression.type === 'Literal' && typeof expression.value === 'number') {
        return expression.value
    }
    if (
        expression.type === 'UnaryExpression'
        && (expression.operator === '+' || expression.operator === '-')
        && expression.argument.type === 'Literal'
        && typeof expression.argument.value === 'number'
    ) {
        return expression.operator === '-' ? -expression.argument.value : expression.argument.value
    }
    return undefined
}

const extractToolbarItems = (source, pluginName = 'fixture') => {
    const pluginObject = getFactoryReturnObject(source)
    const toolbars = findProperty(pluginObject, 'toolbars').value
    assert.equal(toolbars.type, 'ArrayExpression', `${pluginName} toolbars 应为数组`)

    return toolbars.elements.flatMap((element, index) => {
        if (
            pluginName === 'pulsar'
            && element?.type === 'Identifier'
            && element.name === 'pulsarAdminToolbar'
        ) {
            return []
        }
        assert.equal(element?.type, 'ObjectExpression', `${pluginName} 的第 ${index + 1} 个 toolbar 应为静态对象`)
        const orderProperties = element.properties.filter(property => getPropertyName(property) === 'order')
        assert.ok(orderProperties.length <= 1, `${pluginName} 的第 ${index + 1} 个 toolbar 最多声明一个 order`)
        return [{
            order: orderProperties.length === 1 ? getStaticNumericValue(orderProperties[0].value) : undefined
        }]
    })
}

test('不会把注释、字符串或嵌套 order 当作 toolbar 顶层 order', () => {
    const source = `
export default () => ({
    toolbars: [{
        // order: 100,
        title: 'order: 100',
        metadata: { order: 100 }
    }]
})
`

    assert.deepEqual(extractToolbarItems(source), [{ order: undefined }])
})

test('所有静态插件 toolbar 都有约定的有限数值 order', () => {
    for (const plugin of plugins) {
        const pluginPath = path.join(pluginRoot, `fw-plus-magicapi-plugin-${plugin.name}/src/console/src/index.js`)
        const toolbarItems = extractToolbarItems(fs.readFileSync(pluginPath, 'utf8'), plugin.name)

        assert.equal(toolbarItems.length, plugin.orders.length, `${plugin.name} toolbar 项数量应保持不变`)
        toolbarItems.forEach((item, index) => {
            assert.equal(typeof item.order, 'number', `${plugin.name} 的第 ${index + 1} 个 toolbar 应声明数值 order`)
            assert.ok(Number.isFinite(item.order), `${plugin.name} 的第 ${index + 1} 个 toolbar order 应为有限数值`)
            assert.equal(item.order, plugin.orders[index], `${plugin.name} 的第 ${index + 1} 个 toolbar 应使用约定顺序`)
        })
    }
})
