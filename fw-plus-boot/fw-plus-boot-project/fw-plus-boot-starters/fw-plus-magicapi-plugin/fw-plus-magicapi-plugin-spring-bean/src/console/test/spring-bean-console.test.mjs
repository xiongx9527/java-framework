import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'

const consoleDirectory = fileURLToPath(new URL('..', import.meta.url))
const indexPath = path.join(consoleDirectory, 'src/index.js')
const servicePath = path.join(consoleDirectory, 'src/service/magic-spring-bean.js')
const componentPath = path.join(consoleDirectory, 'src/components/magic-spring-bean-info.vue')
const viteCli = path.join(consoleDirectory, 'node_modules/vite/bin/vite.js')
const iifeFile = path.join(consoleDirectory, 'dist/magic-spring-bean.1.0.0.iife.js')

const createOptions = () => {
	const languages = []
	return {
		i18n: {
			messages: {},
			add(locale, messages) {
				this.messages[locale] = messages
			},
			format(key) {
				return key
			}
		},
		monaco: {
			languages: {
				register(language) {
					languages.push(language.id)
				},
				getLanguages() {
					return languages.map(id => ({ id }))
				},
				setLanguageConfiguration() {},
				setMonarchTokensProvider() {}
			}
		}
	}
}

const loadFactoryFromSource = async () => {
	const service = (await import('../src/service/magic-spring-bean.js')).default
	const zhCN = (await import('../src/i18n/zh-cn.js')).default
	const en = (await import('../src/i18n/en.js')).default
	return opt => {
		opt.i18n.add('zh-cn', zhCN)
		opt.i18n.add('en', en)
		return {
			name: 'spring-bean',
			resource: [{
				type: 'spring-bean',
				icon: '#magic-spring-bean-java',
				title: 'springBean.name',
				service: service(opt.i18n.format)
			}],
			toolbars: [{
				type: 'spring-bean',
				title: 'springBean.title',
				icon: '#magic-spring-bean-java',
				order: 530,
				component: { __name: 'magic-spring-bean-info' }
			}]
		}
	}
}

test('Spring Bean 插件提供 Magic-API 资源树编辑入口', async () => {
	const factory = await loadFactoryFromSource()
	const opt = createOptions()
	const plugin = factory(opt)

	assert.equal(plugin.name, 'spring-bean')
	assert.equal(opt.i18n.messages['zh-cn'].springBean.name, 'Spring Bean')
	assert.equal(opt.i18n.messages.en.springBean.name, 'Spring Bean')
	assert.equal(plugin.resource.length, 1)
	assert.deepEqual({
		type: plugin.resource[0].type,
		icon: plugin.resource[0].icon,
		title: plugin.resource[0].title
	}, {
		type: 'spring-bean',
		icon: '#magic-spring-bean-java',
		title: 'springBean.name'
	})
})

test('Spring Bean 资源使用 Java 语言和无组件注解事务模板', async () => {
	const factory = await loadFactoryFromSource()
	const service = factory(createOptions()).resource[0].service

	assert.equal(service.name, 'springBean.name')
	assert.equal(service.language, 'java')
	assert.equal(service.requirePath, true)
	assert.equal(service.runnable, false)
	assert.deepEqual(service.getIcon({ name: 'UserService' }), ['BEAN', '#2da44e'])
	assert.match(service.defaultScript, /package demo;/)
	assert.match(service.defaultScript, /import org\.springframework\.transaction\.annotation\.Transactional;/)
	assert.match(service.defaultScript, /@Transactional\(rollbackFor = Exception\.class\)/)
	assert.doesNotMatch(service.defaultScript, /@Component/)
	assert.doesNotMatch(service.defaultScript, /@Service/)
	assert.match(service.defaultScript, /public class UserService/)
	assert.equal(service.merge({}).enabled, true)
	assert.equal(service.merge({ enabled: false }).enabled, false)
})

test('Spring Bean 提供名称、路径和说明信息面板', async () => {
	const factory = await loadFactoryFromSource()
	const opt = createOptions()
	const plugin = factory(opt)
	const componentSource = readFileSync(componentPath, 'utf8')
	const indexSource = readFileSync(indexPath, 'utf8')

	assert.equal(existsSync(componentPath), true)
	assert.match(indexSource, /import MagicSpringBeanInfo from '\.\/components\/magic-spring-bean-info\.vue'/)
	assert.equal(plugin.toolbars.length, 1)
	assert.deepEqual({
		type: plugin.toolbars[0].type,
		title: plugin.toolbars[0].title,
		icon: plugin.toolbars[0].icon,
		order: plugin.toolbars[0].order
	}, {
		type: 'spring-bean',
		title: 'springBean.title',
		icon: '#magic-spring-bean-java',
		order: 530
	})
	assert.equal(plugin.toolbars[0].component.__name, 'magic-spring-bean-info')
	assert.match(componentSource, /v-model:value="info\.name"/)
	assert.match(componentSource, /v-model:value="info\.enabled"/)
	assert.match(componentSource, /v-model:value="info\.path"/)
	assert.match(componentSource, /v-model:value="info\.description"/)
	assert.equal(opt.i18n.messages['zh-cn'].springBean.form.enabled, '启用')
})

test('Spring Bean 控制台前端可构建为 Magic-API IIFE 插件', () => {
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

	assert.equal(typeof context.MagicSpringBean, 'function')
	const plugin = context.MagicSpringBean(createOptions())
	assert.equal(plugin.resource[0].type, 'spring-bean')
})
