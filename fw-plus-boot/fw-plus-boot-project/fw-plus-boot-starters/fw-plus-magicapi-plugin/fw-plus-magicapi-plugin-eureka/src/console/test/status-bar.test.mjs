import assert from 'node:assert/strict'
import test from 'node:test'

const loadFactory = async () => {
    try {
        return (await import('../src/index.js')).default
    } catch (error) {
        assert.fail(`Eureka 插件入口不可加载: ${error.message}`)
    }
}

const installBrowser = () => {
    const opened = []
    globalThis.location = {
        protocol: 'https:',
        host: 'example.test:9443',
        pathname: '/magic/index.html'
    }
    globalThis.window = {
        location: globalThis.location,
        open: (...args) => opened.push(args)
    }
    return opened
}

test('Eureka 只注册原状态栏入口', async () => {
    const factory = await loadFactory()
    const plugin = factory({ pluginData: { attributes: {} } })

    assert.deepEqual(Object.keys(plugin).sort(), ['name', 'statusBarItems'])
    assert.equal(plugin.name, 'eureka')
    assert.equal(plugin.statusBarItems.length, 1)
    assert.deepEqual({
        icon: plugin.statusBarItems[0].icon,
        title: plugin.statusBarItems[0].title,
        displayKey: plugin.statusBarItems[0].displayKey,
        order: plugin.statusBarItems[0].order
    }, {
        icon: '#magic-eureka-eureka',
        title: 'Eureka Dashboard',
        displayKey: 'eureka',
        order: 1600
    })
    assert.equal(typeof plugin.statusBarItems[0].onClick, 'function')
})

test('Eureka 默认路径打开宿主根路径', async () => {
    const opened = installBrowser()
    const factory = await loadFactory()

    factory({ pluginData: { attributes: { path: '/' } } }).statusBarItems[0].onClick()

    assert.deepEqual(opened, [['https://example.test:9443/', '_blank']])
})

test('Eureka 从 pluginData.attributes.path 打开自定义路径', async () => {
    const opened = installBrowser()
    const factory = await loadFactory()
    const pluginData = { attributes: { path: '/custom-console' } }

    factory({ pluginData }).statusBarItems[0].onClick()

    assert.deepEqual(opened, [['https://example.test:9443/custom-console', '_blank']])
})
