import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'

const consoleDirectory = fileURLToPath(new URL('..', import.meta.url))
const indexPath = path.join(consoleDirectory, 'src/index.js')
const servicePath = path.join(consoleDirectory, 'src/service/magic-java-entity.js')
const componentPath = path.join(consoleDirectory, 'src/components/magic-java-entity-info.vue')
const viteCli = path.join(consoleDirectory, 'node_modules/vite/bin/vite.js')
const iifeFile = path.join(consoleDirectory, 'dist/magic-java-entity.1.0.0.iife.js')

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
	const service = (await import('../src/service/magic-java-entity.js')).default
	const zhCN = (await import('../src/i18n/zh-cn.js')).default
	const en = (await import('../src/i18n/en.js')).default
	return opt => {
		opt.i18n.add('zh-cn', zhCN)
		opt.i18n.add('en', en)
		return {
			name: 'java-entity',
			resource: [{
				type: 'java-entity',
				icon: '#magic-java-entity-java',
				title: 'javaEntity.name',
				service: service(opt.i18n.format)
			}],
			toolbars: [{
				type: 'java-entity',
				title: 'javaEntity.title',
				icon: '#magic-java-entity-java',
				order: 520,
				component: { __name: 'magic-java-entity-info' }
			}]
		}
	}
}

test('Java 实体插件提供 Magic-API 资源树编辑入口', async () => {
	const factory = await loadFactoryFromSource()
	const opt = createOptions()
	const plugin = factory(opt)

	assert.equal(plugin.name, 'java-entity')
	assert.equal(opt.i18n.messages['zh-cn'].javaEntity.name, 'Java实体')
	assert.equal(opt.i18n.messages.en.javaEntity.name, 'Java Entity')
	assert.equal(plugin.resource.length, 1)
	assert.deepEqual(plugin.resource.map(item => item.type), ['java-entity'])
})

test('Java 实体资源使用 Monaco Java 语言和完整源码模板', async () => {
	const factory = await loadFactoryFromSource()
	const service = factory(createOptions()).resource[0].service

	assert.equal(service.name, 'javaEntity.name')
	assert.equal(service.language, 'java')
	assert.equal(service.requirePath, true)
	assert.equal(service.runnable, false)
	assert.deepEqual(service.getIcon({ name: 'User' }), ['JAVA', '#d73a49'])
	assert.match(service.defaultScript, /package demo;/)
	assert.match(service.defaultScript, /import lombok\.Data;/)
	assert.match(service.defaultScript, /@TableName\("sys_user"\)/)
	assert.match(service.defaultScript, /@Table\(value = "sys_user"\)/)
	assert.match(service.defaultScript, /@JsonProperty\("user_name"\)/)
	assert.match(service.defaultScript, /public class User/)
	assert.equal(service.merge({}).enabled, true)
	assert.equal(service.merge({ enabled: false }).enabled, false)
})

test('Java 实体提供名称、路径和说明信息面板', async () => {
	const factory = await loadFactoryFromSource()
	const opt = createOptions()
	const plugin = factory(opt)
	const componentSource = readFileSync(componentPath, 'utf8')
	const indexSource = readFileSync(indexPath, 'utf8')

	assert.equal(existsSync(componentPath), true)
	assert.match(indexSource, /import MagicJavaEntityInfo from '\.\/components\/magic-java-entity-info\.vue'/)
	assert.doesNotMatch(indexSource, /mybatisplus-xml/)
	assert.doesNotMatch(indexSource, /mybatisflex-xml/)
	assert.equal(plugin.toolbars.length, 1)
	assert.deepEqual({
		type: plugin.toolbars[0].type,
		title: plugin.toolbars[0].title,
		icon: plugin.toolbars[0].icon,
		order: plugin.toolbars[0].order
	}, {
		type: 'java-entity',
		title: 'javaEntity.title',
		icon: '#magic-java-entity-java',
		order: 520
	})
	assert.equal(plugin.toolbars[0].component.__name, 'magic-java-entity-info')
	assert.match(componentSource, /v-model:value="info\.name"/)
	assert.match(componentSource, /v-model:value="info\.enabled"/)
	assert.match(componentSource, /v-model:value="info\.path"/)
	assert.match(componentSource, /v-model:value="info\.description"/)
	assert.equal(opt.i18n.messages['zh-cn'].javaEntity.form.enabled, '启用')
	assert.equal(opt.i18n.messages['zh-cn'].mybatisPlusXml, undefined)
	assert.equal(opt.i18n.messages['zh-cn'].mybatisFlexXml, undefined)
})

test('Java 实体控制台前端可构建为 Magic-API IIFE 插件', () => {
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

	assert.equal(typeof context.MagicJavaEntity, 'function')
	const plugin = context.MagicJavaEntity(createOptions())
	assert.equal(plugin.resource[0].type, 'java-entity')
	assert.equal(plugin.resource.length, 1)
})
