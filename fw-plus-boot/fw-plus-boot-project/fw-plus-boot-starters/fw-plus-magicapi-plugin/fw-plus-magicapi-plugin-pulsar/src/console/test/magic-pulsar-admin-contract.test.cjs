const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const consoleRoot = path.join(__dirname, '..')
const pluginPath = path.join(consoleRoot, 'src/index.js')
const componentPath = path.join(consoleRoot, 'src/components/magic-pulsar.vue')
const runtimePath = path.join(consoleRoot, 'src/service/magic-pulsar-runtime.js')
const zhCnPath = path.join(consoleRoot, 'src/i18n/zh-cn.js')
const enPath = path.join(consoleRoot, 'src/i18n/en.js')
const hostComponentPath = path.join(__dirname, '../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/main/magic-pulsar.vue')

test('Pulsar 插件注册订阅编辑与独立管理 toolbar', () => {
    const source = fs.readFileSync(pluginPath, 'utf8')

    assert.match(source, /name:\s*'pulsar'/)
    assert.match(source, /type:\s*'pulsar-subscribe'/)
    assert.match(source, /id:\s*'pulsar'/)
    assert.match(source, /title:\s*i18n\.format\('pulsar\.title'\)/)
    assert.match(source, /icon:\s*'#magic-pulsar-pulsar'/)
    assert.match(source, /component:\s*MagicPulsar/)
    assert.match(source, /adminOnly:\s*true/)
    assert.match(source, /allowClose:\s*true/)
    assert.match(source, /fullscreen:\s*true/)
})

test('Pulsar 插件注册状态栏入口并复用管理 toolbar', () => {
    const source = fs.readFileSync(pluginPath, 'utf8')
    const statusBarItemPattern = /statusBarItems:\s*\[\{\s*icon:\s*'#magic-pulsar-pulsar',\s*title:\s*'Pulsar',\s*displayKey:\s*'pulsar',\s*onClick:\s*\(\)\s*=>\s*runtime\.bus\.\$emit\(runtime\.Message\.ADD_FOOTER_TOOLBAR,\s*pulsarAdminToolbar\)\s*\}\]/

    assert.match(source, /const pulsarAdminToolbar\s*=\s*\{/)
    assert.match(source, statusBarItemPattern)
    assert.match(source, /toolbars:\s*\[[\s\S]*pulsarAdminToolbar\s*\]/)
})

test('Pulsar 管理页面归属插件并使用插件运行时适配服务', () => {
    assert.equal(fs.existsSync(componentPath), true, 'Pulsar 管理页面应迁入插件组件目录')
    assert.equal(fs.existsSync(runtimePath), true, 'Pulsar 插件运行时适配服务应存在')
    assert.equal(fs.existsSync(hostComponentPath), false, '宿主不应保留 Pulsar 管理页面')

    const componentSource = fs.readFileSync(componentPath, 'utf8')
    const runtimeSource = fs.readFileSync(runtimePath, 'utf8')
    assert.match(componentSource, /from '\.\.\/service\/magic-pulsar-runtime\.js'/)
    assert.match(componentSource, /<magic-monaco-editor/)
    assert.doesNotMatch(componentSource, /from ['"]\.\.\/\.\.\/scripts\//)
    assert.doesNotMatch(componentSource, /from ['"]\.\.\/\.\.\/common\//)
    assert.doesNotMatch(componentSource, /icon="pulsar"/)
    assert.doesNotMatch(componentSource, /\$i\('kafka\./)
    assert.match(runtimeSource, /setPulsarRuntime = opt =>/)
    assert.match(runtimeSource, /request:\s*opt\.request/)
    assert.match(runtimeSource, /modal:\s*opt\.modal/)
    assert.match(runtimeSource, /bus:\s*opt\.bus/)
    assert.match(runtimeSource, /Message:\s*opt\.Message/)
    assert.match(runtimeSource, /format:\s*opt\.i18n\.format/)
})

test('Pulsar 管理文案随插件 i18n 发布', () => {
    const zhCnSource = fs.readFileSync(zhCnPath, 'utf8')
    const enSource = fs.readFileSync(enPath, 'utf8')

    assert.match(zhCnSource, /title:\s*'Pulsar管理'/)
    assert.match(enSource, /title:\s*'Pulsar Management'/)

    ;[
        'resetOffsets',
        'pleaseSelectConsumerGroup',
        'confirm',
        'loadConsumerGroupsFailed',
        'resetOffsetsSuccess',
        'resetOffsetsFailed'
    ].forEach(key => {
        const pattern = new RegExp(`${key}:`)
        assert.match(zhCnSource, pattern)
        assert.match(enSource, pattern)
    })
})
