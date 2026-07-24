const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const servicePath = path.join(__dirname, '../src/service/magic-s3-browser.js')

test('S3 浏览器服务封装存储、列表、预签名和删除接口', () => {
    assert.equal(fs.existsSync(servicePath), true, 'S3 浏览器服务应存在')
    const source = fs.readFileSync(servicePath, 'utf8')
    assert.match(source, /\/s3\/datasource\/availableStorages/)
    assert.match(source, /listFilesResult/)
    assert.match(source, /generateUploadPresignedUrl/)
    assert.match(source, /\/delete/)
    assert.match(source, /'sendPost'/)
})
