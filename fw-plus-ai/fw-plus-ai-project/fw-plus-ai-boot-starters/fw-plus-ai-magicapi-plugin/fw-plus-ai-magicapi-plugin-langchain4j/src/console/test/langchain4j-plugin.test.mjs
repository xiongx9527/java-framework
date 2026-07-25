import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import vm from 'node:vm'
import { fileURLToPath } from 'node:url'

const artifactUrl = new URL('../../main/resources/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js', import.meta.url)

const createPluginOptions = () => {
    const locales = []
    const extensionAttributes = []
    const loadedClasses = []
    const requests = []
    const alerts = []

    return {
        observations: { locales, extensionAttributes, loadedClasses, requests, alerts },
        options: {
            i18n: {
                add(locale, messages) {
                    locales.push({ locale, messages })
                },
                format(key) {
                    return `translated:${key}`
                }
            },
            JavaClass: {
                setExtensionAttribute(type, provider) {
                    extensionAttributes.push({ type, provider })
                },
                loadClass(type) {
                    loadedClasses.push(type)
                }
            },
            request: {
                sendJson(url, payload) {
                    requests.push({ url, payload })
                    return {
                        success(callback) {
                            callback('ok')
                            return this
                        }
                    }
                }
            },
            modal: {
                alert(message, title) {
                    alerts.push({ message, title })
                }
            }
        }
    }
}

const assertLegacyPluginContract = factory => {
    const { options, observations } = createPluginOptions()
    const plugin = factory(options)

    assert.deepEqual(observations.locales.map(item => item.locale), ['zh-cn', 'en'])
    const englishMessages = observations.locales.find(item => item.locale === 'en').messages
    assert.equal(
        englishMessages.langchain4j.datasource.form.placeholder.other,
        'Please Enter Langchain4j Other Config\uff0ceg\uff1a {xxxx: "yyyy"}'
    )
    assert.equal(plugin.datasources.length, 1)

    const datasource = plugin.datasources[0]
    assert.equal(datasource.type, 'langchain4j-datasource')
    assert.equal(datasource.icon, '#magic-langchain4j-langchain4j')
    assert.equal(datasource.title, 'translated:langchain4j.datasource.title')
    assert.equal(datasource.name, 'translated:langchain4j.datasource.name')
    assert.equal(datasource.component.__name, 'magic-langchain4j-datasource-info')
    assert.equal(datasource.service.dialogWidth, '1100px')

    assert.equal(observations.extensionAttributes.length, 1)
    const extension = observations.extensionAttributes[0]
    assert.equal(extension.type, 'com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo')

    let requestedResourceType
    datasource.service.injectResources(type => {
        requestedResourceType = type
        return [{ children: [{ key: 'chat', name: 'Chat model' }] }]
    })
    const attributes = extension.provider()
    assert.equal(requestedResourceType, 'qdrant-datasource')
    assert.deepEqual(Array.from(attributes, item => ({
        name: item.name,
        type: item.type,
        comment: item.comment
    })), [{
        name: 'chat',
        type: 'com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo',
        comment: 'Chat model'
    }])
    assert.deepEqual(observations.loadedClasses, [
        'com.jugui.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo'
    ])

    const info = { key: 'chat' }
    datasource.service.doTest(info)
    assert.deepEqual(observations.requests, [{ url: '/langchain4j/datasource/test', payload: info }])
    assert.deepEqual(observations.alerts, [{
        message: 'translated:datasource.connected',
        title: 'translated:datasource.test'
    }])

    return plugin
}

test('现有 IIFE 保持 LangChain4j 插件、数据源和组件注册契约', async () => {
    assert.equal(path.basename(fileURLToPath(artifactUrl)), 'magic-langchain4j.1.0.0.iife.js')

    const source = await readFile(artifactUrl, 'utf8')
    const context = {
        Vue: {
            defineComponent(component) {
                return component
            }
        },
        document: {
            createElement() {
                return {}
            },
            head: {
                appendChild() {}
            }
        }
    }
    vm.createContext(context)
    vm.runInContext(source, context, { filename: fileURLToPath(artifactUrl) })

    assert.equal(typeof context.MagicLangchain4j, 'function')
    assertLegacyPluginContract(context.MagicLangchain4j)
})

test('源码构建入口保持旧契约并提供 LangChain4j 状态栏项', async () => {
    const packageJson = JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8'))
    assert.equal(packageJson.name, 'magic-langchain4j')
    assert.equal(packageJson.version, '1.0.0')
    assert.equal(packageJson.scripts.test, 'node --test test/langchain4j-plugin.test.mjs')
    assert.equal(packageJson.scripts.build, 'vite build')

    const viteConfig = (await import('../vite.config.js')).default
    assert.equal(viteConfig.build.lib.name, 'MagicLangchain4j')
    assert.equal(viteConfig.build.lib.fileName('iife'), 'magic-langchain4j.1.0.0.iife.js')
    assert.equal(viteConfig.build.rollupOptions.external[0], 'vue')

    const sourceFactory = (await import('../src/index.js')).default
    const plugin = assertLegacyPluginContract(sourceFactory)
    assert.equal(plugin.statusBarItems.length, 1)

    const statusBarItem = plugin.statusBarItems[0]
    assert.equal(statusBarItem.icon, '#magic-langchain4j-langchain4j')
    assert.equal(statusBarItem.title, 'Langchain4j')
    assert.equal(statusBarItem.displayKey, 'langchain4j')
    assert.equal(statusBarItem.order, 800)

    const originalWindow = globalThis.window
    const opened = []
    globalThis.window = {
        open(...args) {
            opened.push(args)
        }
    }
    try {
        statusBarItem.onClick()
    } finally {
        if (originalWindow === undefined) {
            delete globalThis.window
        } else {
            globalThis.window = originalWindow
        }
    }
    assert.deepEqual(opened, [['https://docs.langchain4j.dev/', '_blank']])
})

test('源码图标安装器注册既有 SVG symbol', async () => {
    const { installLangchain4jIcon, langchain4jIconSymbol } = await import('../src/assets/icon.js')
    const inserted = []
    const body = {
        firstChild: {},
        insertBefore(node) {
            inserted.push(node)
        }
    }
    const document = {
        body,
        getElementById() {
            return null
        },
        createElementNS() {
            return {
                style: {},
                setAttribute() {}
            }
        }
    }

    installLangchain4jIcon(document)

    assert.match(langchain4jIconSymbol, /id="magic-langchain4j-langchain4j"/)
    assert.equal(inserted.length, 1)
    assert.equal(inserted[0].id, '__svg__icons__dom__1717728641709__langchain4j')
    assert.equal(inserted[0].innerHTML, langchain4jIconSymbol)
})
