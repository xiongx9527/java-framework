import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'

const consoleDirectory = fileURLToPath(new URL('..', import.meta.url))
const viteCli = path.join(consoleDirectory, 'node_modules/vite/bin/vite.js')
const iifeFile = path.join(consoleDirectory, 'dist/magic-druid.1.0.0.iife.js')
const legacyIconFile = fileURLToPath(new URL('../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/icons/druid.svg', import.meta.url))
const customBaseURL = 'https://api.example.test:18443/backend/'

const loadFactory = async () => {
    try {
        return (await import('../src/index.js')).default
    } catch (error) {
        assert.fail(`Druid 插件入口不可加载: ${error.message}`)
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

const executeCustomBaseIife = () => {
    execFileSync(process.execPath, [viteCli, 'build', '--mode', 'test'], {
        cwd: consoleDirectory,
        env: { ...process.env, MAGIC_API_BASE_URL: customBaseURL },
        stdio: 'pipe'
    })
    const opened = []
    const location = { protocol: 'https:', host: 'frontend.example.test', pathname: '/magic/index.html' }
    const context = {
        URL,
        document: { getElementById: () => ({}) },
        location,
        window: { location, open: (...args) => opened.push(args) }
    }
    vm.runInNewContext(readFileSync(iifeFile, 'utf8'), context)
    context.MagicDruid({}).statusBarItems[0].onClick()
    return opened
}

const extractSvgContent = (markup, tagName) => {
    const element = markup.match(new RegExp(`<${tagName}\\b([^>]*)>([\\s\\S]*)</${tagName}>`))
    assert.ok(element, `缺少 ${tagName} 元素`)
    const viewBox = element[1].match(/\bviewBox=["']([^"']+)["']/)
    assert.ok(viewBox, `${tagName} 缺少 viewBox`)
    return {
        viewBox: viewBox[1],
        body: element[2].replace(/<!--[^]*?-->/g, '').replace(/\s+/g, ' ').replace(/>\s+</g, '><').trim()
    }
}

const captureInstalledSymbol = () => {
    execFileSync(process.execPath, [viteCli, 'build'], { cwd: consoleDirectory, stdio: 'pipe' })
    const inserted = []
    const sprite = { insertAdjacentHTML: (_position, markup) => inserted.push(markup) }
    const location = { protocol: 'https:', host: 'frontend.example.test', pathname: '/magic/index.html' }
    const context = {
        URL,
        document: { getElementById: id => id === '__svg__icons__dom__' ? sprite : null },
        location,
        window: { location, open: () => {} }
    }
    vm.runInNewContext(readFileSync(iifeFile, 'utf8'), context)
    assert.equal(inserted.length, 1)
    return inserted[0]
}

test('Druid 只注册原状态栏入口', async () => {
    const factory = await loadFactory()
    const plugin = factory({})

    assert.deepEqual(Object.keys(plugin).sort(), ['name', 'statusBarItems'])
    assert.equal(plugin.name, 'druid')
    assert.equal(plugin.statusBarItems.length, 1)
    assert.deepEqual({
        icon: plugin.statusBarItems[0].icon,
        title: plugin.statusBarItems[0].title,
        displayKey: plugin.statusBarItems[0].displayKey,
        order: plugin.statusBarItems[0].order
    }, {
        icon: '#magic-druid-druid',
        title: 'Druid DataSource',
        displayKey: 'druid',
        order: 1100
    })
    assert.equal(typeof plugin.statusBarItems[0].onClick, 'function')
})

test('Druid 从宿主基址打开登录页', async () => {
    const opened = installBrowser()
    const factory = await loadFactory()

    factory({}).statusBarItems[0].onClick()

    assert.deepEqual(opened, [['https://example.test:9443/druid/login.html', '_blank']])
})

test('Druid 非默认基址构建后的 IIFE 打开后端登录页', () => {
    assert.deepEqual(executeCustomBaseIife(), [['https://api.example.test:18443/druid/login.html', '_blank']])
})

test('Druid symbol 保留迁移前宿主 SVG 的 viewBox 和视觉内容', () => {
    const legacy = extractSvgContent(readFileSync(legacyIconFile, 'utf8'), 'svg')
    const installed = extractSvgContent(captureInstalledSymbol(), 'symbol')

    assert.deepEqual(installed, legacy)
})
