const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const servicePath = path.join(__dirname, '../src/service/magic-s3-browser.js')
const componentPath = path.join(__dirname, '../src/components/magic-s3-browser.vue')
const pluginPath = path.join(__dirname, '../src/index.js')
const hostToolbarPath = path.join(__dirname, '../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue')

test('S3 浏览器服务封装存储、列表、预签名和删除接口', () => {
    assert.equal(fs.existsSync(servicePath), true, 'S3 浏览器服务应存在')
    const source = fs.readFileSync(servicePath, 'utf8')
    assert.match(source, /\/s3\/datasource\/availableStorages/)
    assert.match(source, /listFilesResult/)
    assert.match(source, /generateUploadPresignedUrl/)
    assert.match(source, /downloadFile/)
    assert.match(source, /\/delete/)
    assert.match(source, /'sendPost'/)
})

test('S3 浏览器保留下载入口并保护列表请求上下文', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /@click="downloadFile"/)
    assert.match(source, /const fileRequestSeq = ref\(0\)/)
    assert.match(source, /const requestSeq = \+\+fileRequestSeq\.value/)
    assert.match(source, /const expectedContext = fileRequestContext\(more \? nextMarker\.value : null\)/)
    assert.match(source, /if \(requestSeq !== fileRequestSeq\.value \|\| !sameFileRequestContext\(expectedContext\)\) return/)
})

test('S3 管理入口通过通用 adminOnly 标记受 adminEnabled 门控', () => {
    const pluginSource = fs.readFileSync(pluginPath, 'utf8')
    const hostToolbarSource = fs.readFileSync(hostToolbarPath, 'utf8')
    assert.match(pluginSource, /adminOnly: true/)
    assert.match(hostToolbarSource, /isPluginToolbarVisible/)
    assert.match(hostToolbarSource, /toolbar\.adminOnly/)
    assert.match(hostToolbarSource, /pluginData\.adminEnabled === true/)
})
