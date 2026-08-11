import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'

const consoleDirectory = fileURLToPath(new URL('..', import.meta.url))
const indexPath = path.join(consoleDirectory, 'src/index.js')
const servicePath = path.join(consoleDirectory, 'src/service/magic-mybatisflex-xml.js')
const componentPath = path.join(consoleDirectory, 'src/components/magic-mybatisflex-xml-info.vue')
const viteCli = path.join(consoleDirectory, 'node_modules/vite/bin/vite.js')
const iifeFile = path.join(consoleDirectory, 'dist/magic-mybatisflex-xml.1.0.0.iife.js')

const createOptions = () => ({
	i18n: {
		messages: {},
		add(locale, messages) {
			this.messages[locale] = messages
		},
		format(key) {
			return key
		}
	}
})

test('MyBatis-Flex XML 插件提供独立资源树入口', async () => {
	const serviceFactory = (await import('../src/service/magic-mybatisflex-xml.js')).default
	const zhCN = (await import('../src/i18n/zh-cn.js')).default
	const opt = createOptions()
	opt.i18n.add('zh-cn', zhCN)
	const service = serviceFactory(opt.i18n.format)
	const indexSource = readFileSync(indexPath, 'utf8')

	assert.match(indexSource, /name: 'mybatisflex-xml'/)
	assert.match(indexSource, /type: 'mybatisflex-xml'/)
	assert.match(indexSource, /#magic-mybatisflex-xml/)
	assert.equal(service.language, 'xml')
	assert.equal(service.requirePath, true)
	assert.equal(service.merge({}).dbKey, 'default')
	assert.match(opt.i18n.messages['zh-cn'].mybatisFlexXml.form.defaultDbKeyTip, /default/)
	assert.match(readFileSync(componentPath, 'utf8'), /defaultDbKeyTip/)
})

test('MyBatis-Flex XML 控制台前端可构建为 Magic-API IIFE 插件', () => {
	assert.equal(existsSync(indexPath), true)
	assert.equal(existsSync(servicePath), true)
	assert.equal(existsSync(componentPath), true)

	execFileSync(process.execPath, [viteCli, 'build'], {
		cwd: consoleDirectory,
		stdio: 'pipe'
	})

	const context = {
		document: {
			body: { appendChild() {} },
			head: { appendChild() {} },
			getElementById: () => ({}),
			createElement: () => ({ setAttribute() {}, appendChild() {}, style: {} }),
			createElementNS: () => ({ setAttribute() {}, appendChild() {}, style: {}, set innerHTML(value) { this.html = value } })
		},
		window: {}
	}
	vm.runInNewContext(readFileSync(iifeFile, 'utf8'), context)

	assert.equal(typeof context.MagicMybatisFlexXml, 'function')
	const plugin = context.MagicMybatisFlexXml(createOptions())
	assert.equal(plugin.resource[0].type, 'mybatisflex-xml')
})
