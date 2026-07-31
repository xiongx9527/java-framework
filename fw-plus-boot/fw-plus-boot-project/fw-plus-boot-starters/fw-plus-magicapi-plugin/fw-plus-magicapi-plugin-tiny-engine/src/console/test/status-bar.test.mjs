import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'

const consoleDirectory = fileURLToPath(new URL('..', import.meta.url))
const moduleDirectory = path.resolve(consoleDirectory, '../..')
const parentPom = path.resolve(moduleDirectory, '../pom.xml')
const viteCli = path.join(consoleDirectory, 'node_modules/vite/bin/vite.js')
const iifeFile = path.join(consoleDirectory, 'dist/magic-tiny-engine.1.0.0.iife.js')
const customBaseURL = 'https://api.example.test:18443/backend/'

const loadFactory = async () => {
  try {
    return (await import('../src/index.js')).default
  } catch (error) {
    assert.fail(`TinyEngine 插件入口不可加载: ${error.message}`)
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

const executeCustomBaseIife = pluginData => {
  execFileSync(process.execPath, [viteCli, 'build', '--mode', 'test'], {
    cwd: consoleDirectory,
    env: { ...process.env, MAGIC_API_BASE_URL: customBaseURL },
    stdio: 'pipe'
  })
  const opened = []
  const location = { protocol: 'https:', host: 'frontend.example.test', pathname: '/magic/index.html' }
  const context = {
    URL,
    location,
    window: { location, open: (...args) => opened.push(args) }
  }
  vm.runInNewContext(readFileSync(iifeFile, 'utf8'), context)
  context.MagicTinyEngine({ pluginData }).statusBarItems[0].onClick()
  return opened
}

test('TinyEngine Magic 插件模块被父 POM 聚合', () => {
  assert.equal(existsSync(parentPom), true)
  assert.match(readFileSync(parentPom, 'utf8'), /<module>fw-plus-magicapi-plugin-tiny-engine<\/module>/)
})

test('TinyEngine 只注册状态栏入口', async () => {
  const factory = await loadFactory()
  const plugin = factory({ pluginData: { attributes: {} } })

  assert.deepEqual(Object.keys(plugin).sort(), ['name', 'statusBarItems'])
  assert.equal(plugin.name, 'tiny-engine')
  assert.equal(plugin.statusBarItems.length, 1)
  assert.deepEqual({
    icon: plugin.statusBarItems[0].icon,
    title: plugin.statusBarItems[0].title,
    displayKey: plugin.statusBarItems[0].displayKey,
    order: plugin.statusBarItems[0].order
  }, {
    icon: 'app',
    title: 'TinyEngine',
    displayKey: 'tiny-engine',
    order: 1600
  })
  assert.equal(typeof plugin.statusBarItems[0].onClick, 'function')
})

test('TinyEngine 默认打开同源 /tiny-engine/ 入口', async () => {
  const opened = installBrowser()
  const factory = await loadFactory()

  factory({ pluginData: { attributes: {} } }).statusBarItems[0].onClick()

  assert.deepEqual(opened, [['https://example.test:9443/tiny-engine/', '_blank']])
})

test('TinyEngine 支持 pluginData.attributes.path 覆盖入口', async () => {
  const opened = installBrowser()
  const factory = await loadFactory()

  factory({ pluginData: { attributes: { path: '/custom-tiny/' } } }).statusBarItems[0].onClick()

  assert.deepEqual(opened, [['https://example.test:9443/custom-tiny/', '_blank']])
})

test('TinyEngine 非默认基址构建后的 IIFE 打开后端入口', () => {
  assert.deepEqual(executeCustomBaseIife({ pluginData: {} }), [['https://api.example.test:18443/tiny-engine/', '_blank']])
})
