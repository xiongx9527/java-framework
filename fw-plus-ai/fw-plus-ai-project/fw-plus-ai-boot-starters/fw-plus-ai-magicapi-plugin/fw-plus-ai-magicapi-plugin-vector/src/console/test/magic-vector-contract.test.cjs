const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '..')

const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('registers vector datasource metadata', () => {
  const source = read('src/index.js')

  assert.match(source, /name:\s*['"]vector['"]/)
  assert.match(source, /type:\s*['"]vector-datasource['"]/)
  assert.match(source, /MagicVectorDataSource/)
  assert.match(source, /MagicVectorDatasourceInfo/)
})

test('uses vector datasource test endpoint', () => {
  const source = read('src/service/magic-vector-datasource.js')

  assert.match(source, /sendJson\(['"]\/vector\/datasource\/test['"]/)
})

test('builds MagicVector iife bundle name', () => {
  const source = read('vite.config.js')

  assert.match(source, /name:\s*['"]MagicVector['"]/)
  assert.match(source, /magic-vector\.\$\{pkg\.version\}\.\$\{format\}\.js/)
})
