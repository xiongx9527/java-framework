import assert from 'node:assert/strict'
import test from 'node:test'

const loadFactory = async () => {
    try {
        return (await import('../src/index.js')).default
    } catch (error) {
        assert.fail(`Swagger 插件入口不可加载: ${error.message}`)
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

test('Swagger 只注册原状态栏入口', async () => {
    const factory = await loadFactory()
    const plugin = factory({})

    assert.deepEqual(Object.keys(plugin).sort(), ['name', 'statusBarItems'])
    assert.equal(plugin.name, 'swagger')
    assert.equal(plugin.statusBarItems.length, 1)
    assert.deepEqual({
        icon: plugin.statusBarItems[0].icon,
        title: plugin.statusBarItems[0].title,
        displayKey: plugin.statusBarItems[0].displayKey,
        order: plugin.statusBarItems[0].order
    }, {
        icon: '#magic-swagger-knife4j',
        title: 'Swagger Api Doc',
        displayKey: 'swagger',
        order: 1200
    })
    assert.equal(typeof plugin.statusBarItems[0].onClick, 'function')
})

test('Swagger 从宿主基址打开文档页', async () => {
    const opened = installBrowser()
    const factory = await loadFactory()

    factory({}).statusBarItems[0].onClick()

    assert.deepEqual(opened, [['https://example.test:9443/doc.html', '_blank']])
})
