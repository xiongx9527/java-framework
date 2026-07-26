import assert from 'node:assert/strict'
import test from 'node:test'

const loadPluginLoader = async () => {
    try {
        return await import('../src/scripts/plugin-loader.js')
    } catch (error) {
        return { error }
    }
}

const createDependencies = () => {
    const events = []
    const constants = { PLUGINS: [] }
    return {
        constants,
        Message: { PLUGINS_CHANGED: 'plugins.changed' },
        bus: {
            $emit(message) {
                events.push({ message, pluginCount: constants.PLUGINS.length })
            }
        },
        events
    }
}

test('插件加载向工厂传递当前插件元数据', async () => {
    const loader = await loadPluginLoader()
    assert.equal(loader.error, undefined)

    const plugin = { name: 'sample' }
    const dependencies = createDependencies()
    let pluginData

    loader.installPlugin({
        factory(context) {
            pluginData = context.pluginData
            return { name: 'plugin-instance' }
        },
        plugin,
        ...dependencies
    })

    assert.equal(pluginData, plugin)
})

test('插件加载在加入集合后才通知变更', async () => {
    const loader = await loadPluginLoader()
    assert.equal(loader.error, undefined)

    const dependencies = createDependencies()
    const pluginInstance = { name: 'plugin-instance' }

    loader.installPlugin({
        factory() {
            return pluginInstance
        },
        plugin: { name: 'sample' },
        ...dependencies
    })

    assert.deepEqual(dependencies.constants.PLUGINS, [pluginInstance])
    assert.deepEqual(dependencies.events, [{ message: 'plugins.changed', pluginCount: 1 }])
})

test('插件加载拒绝真值非对象工厂返回值', async () => {
    const loader = await loadPluginLoader()
    assert.equal(loader.error, undefined)

    for (const result of ['plugin-instance', 1, true, () => ({})]) {
        const dependencies = createDependencies()

        loader.installPlugin({
            factory() {
                return result
            },
            plugin: { name: 'sample' },
            ...dependencies
        })

        assert.deepEqual(dependencies.constants.PLUGINS, [])
        assert.deepEqual(dependencies.events, [])
    }
})
