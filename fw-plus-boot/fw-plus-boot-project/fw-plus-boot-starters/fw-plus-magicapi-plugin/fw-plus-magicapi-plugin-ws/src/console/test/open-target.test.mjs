import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'

const consoleDirectory = fileURLToPath(new URL('..', import.meta.url))
const viteCli = path.join(consoleDirectory, 'node_modules/vite/bin/vite.js')
const iifeFile = path.join(consoleDirectory, 'dist/magic-ws.1.0.0.iife.js')

execFileSync(process.execPath, [viteCli, 'build'], { cwd: consoleDirectory, stdio: 'pipe' })

const openTarget = (baseURL, location) => {
	const opened = []
	const document = {
		readyState: 'complete',
		getElementById: () => ({ innerHTML: '' }),
		createElement: () => ({ setAttribute: () => {}, innerHTML: '' }),
		head: { appendChild: () => {} },
		body: { firstChild: null, insertBefore: () => {} }
	}
	const context = {
		URL,
		Vue: new Proxy({}, { get: () => value => value }),
		document,
		location,
		window: { location, open: (...args) => opened.push(args) }
	}
	vm.runInNewContext(readFileSync(iifeFile, 'utf8'), context)
	const plugin = context.MagicWs({
		i18n: { add: () => {}, format: value => value },
		bus: {},
		constants: { BASE_URL: baseURL },
		Message: {},
		request: {},
		modal: {},
		JavaClass: { setExtensionAttribute: () => {}, loadClass: () => {} }
	})
	plugin.statusBarItems[0].onClick()
	return opened
}

test('同源生产模式从控制台目录打开 WebSocket 客户端', () => {
	const location = { protocol: 'https:', host: 'console.example.test', pathname: '/magic-api/web/index.html' }
	assert.deepEqual(openTarget('./', location), [['https://console.example.test/magic-api/web/ws/index.html', '_blank']])
})

test('绝对后端基址打开对应 WebSocket 客户端', () => {
	const location = { protocol: 'https:', host: 'frontend.example.test', pathname: '/index.html' }
	assert.deepEqual(openTarget('https://api.example.test/gateway/magic/web', location),
		[['https://api.example.test/gateway/magic/web/ws/index.html', '_blank']])
})

test('宿主开发模式继承 3081 后端基址', () => {
	const location = { protocol: 'http:', host: '127.0.0.1:3000', pathname: '/index.html' }
	assert.deepEqual(openTarget('http://127.0.0.1:3081/magic-api/web', location),
		[['http://127.0.0.1:3081/magic-api/web/ws/index.html', '_blank']])
})
