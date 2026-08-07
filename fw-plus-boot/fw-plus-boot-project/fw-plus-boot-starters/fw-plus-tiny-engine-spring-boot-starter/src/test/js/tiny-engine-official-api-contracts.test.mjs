import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'

const CONTRACT_FILE = resolve(process.cwd(), 'docs/tiny-engine/official-api-contracts.json')
const EXPECTED_KEYS = [
  'currentMvcHandler',
  'currentWebFluxHandler',
  'frontendCallSites',
  'id',
  'method',
  'officialController',
  'path',
  'query',
  'responseShape',
  'status'
]
const NONE_CONTROLLER = 'NONE（官方后端无同 method/path 路由）'
const NOT_APPLICABLE_SHAPE = 'N/A（官方后端无同 method/path 路由）'

const loadContracts = () => JSON.parse(readFileSync(CONTRACT_FILE, 'utf8'))

const expectedId = ({ method, path }) => {
  const route = path
    .replace(/^\//, '')
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/\{id\}/g, 'by-id')
    .replace(/\//g, '-')
    .replace(/[^a-zA-Z0-9-]/g, '-')
    .replace(/-+/g, '-')
    .toLowerCase()

  return `${route}-${method.toLowerCase()}`
}

test('每个官方 API 契约都有调用点、官方处理器和双 Web 栈状态', () => {
  const contracts = loadContracts()
  const ids = new Set()
  const methodPaths = new Set()

  assert.ok(contracts.length > 0)
  for (const contract of contracts) {
    assert.deepEqual(Object.keys(contract).sort(), EXPECTED_KEYS)
    assert.match(contract.id, /^[a-z0-9-]+$/)
    assert.equal(contract.id, expectedId(contract))
    assert.ok(!ids.has(contract.id), `重复契约 id: ${contract.id}`)
    ids.add(contract.id)
    assert.ok(['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].includes(contract.method))
    assert.match(contract.path, /^\/(app-center|material-center|platform-center)\//)
    const methodPath = `${contract.method} ${contract.path}`
    assert.ok(!methodPaths.has(methodPath), `重复 method+path: ${methodPath}`)
    methodPaths.add(methodPath)
    assert.ok(Array.isArray(contract.query))
    assert.ok(contract.query.every((item) => typeof item === 'string' && item.length > 0))
    assert.equal(new Set(contract.query).size, contract.query.length)
    assert.deepEqual(contract.query, [...contract.query].sort())
    assert.ok(Array.isArray(contract.frontendCallSites))
    assert.ok(contract.frontendCallSites.length > 0)
    assert.ok(contract.frontendCallSites.every((item) => typeof item === 'string' && item.length > 0))
    assert.equal(typeof contract.officialController, 'string')
    assert.ok(contract.officialController === NONE_CONTROLLER || /^[A-Za-z0-9]+Controller#[A-Za-z0-9]+$/.test(contract.officialController))
    assert.ok(contract.currentMvcHandler === null || typeof contract.currentMvcHandler === 'string')
    assert.ok(contract.currentWebFluxHandler === null || typeof contract.currentWebFluxHandler === 'string')
    assert.ok(['implemented', 'partial', 'missing'].includes(contract.status))
    assert.equal(typeof contract.responseShape, 'string')
    assert.ok(contract.responseShape.length > 0)

    if (contract.status === 'implemented') {
      assert.match(contract.currentMvcHandler, /^[A-Za-z0-9]+MvcController#[A-Za-z0-9]+$/)
      assert.match(contract.currentWebFluxHandler, /^[A-Za-z0-9]+WebFluxController#[A-Za-z0-9]+$/)
    } else if (contract.status === 'missing') {
      assert.equal(contract.currentMvcHandler, null)
      assert.equal(contract.currentWebFluxHandler, null)
    } else {
      assert.ok(Boolean(contract.currentMvcHandler) !== Boolean(contract.currentWebFluxHandler))
    }

    if (contract.officialController === NONE_CONTROLLER) {
      assert.equal(contract.responseShape, NOT_APPLICABLE_SHAPE)
    } else {
      assert.notEqual(contract.responseShape, NOT_APPLICABLE_SHAPE)
    }
  }
})

test('Axios params 与官方精确路由缺口进入契约事实源', () => {
  const contracts = loadContracts()
  const byMethodPath = new Map(contracts.map((contract) => [`${contract.method} ${contract.path}`, contract]))

  const expectedQueries = new Map([
    ['GET /app-center/api/app-template/list', ['currentPage', 'framework', 'industryId', 'name', 'pageSize', 'sceneId']],
    ['GET /app-center/api/apps/canvas/lock', ['id', 'state', 'type']],
    ['GET /app-center/api/apps/extension/delete', ['app', 'id']],
    ['GET /app-center/api/apps/page', ['createdBy', 'currentPage', 'name', 'orderBy', 'pageSize']],
    ['GET /app-center/api/code', ['app', 'history', 'id', 'platform', 'type']],
    ['GET /app-center/api/i18n/entries', ['_limit', 'host', 'host_type']],
    ['GET /app-center/api/preview/metadata', ['app', 'history', 'id', 'platform', 'type']],
    ['GET /material-center/api/block', ['framework', 'groups', 'label', 'label_contains']],
    ['GET /material-center/api/block-categories', ['appId']],
    ['GET /material-center/api/block-groups', ['app', 'appId', 'from', 'id']],
    ['GET /material-center/api/blocks', ['appId', 'categoryId', 'groupId']],
    ['GET /material-center/api/model/list', ['currentPage', 'id', 'nameCn', 'pageSize']],
    ['GET /platform-center/api/courses', ['category']]
  ])

  for (const [methodPath, query] of expectedQueries) {
    assert.deepEqual(byMethodPath.get(methodPath).query, query, methodPath)
  }

  const webFluxParityContracts = new Map([
    ['GET /material-center/api/block-history', ['TinyEngineMaterialMvcController#blockHistories', 'TinyEngineMaterialWebFluxController#blockHistories']],
    ['GET /material-center/api/block/tenants', ['TinyEngineMaterialMvcController#blockTenants', 'TinyEngineMaterialWebFluxController#blockTenants']],
    ['GET /material-center/api/block/users', ['TinyEngineMaterialMvcController#blockUsers', 'TinyEngineMaterialWebFluxController#blockUsers']],
    ['POST /material-center/api/resource/create/batch', ['TinyEngineMaterialMvcController#createResources', 'TinyEngineMaterialWebFluxController#createResources']],
    ['POST /material-center/api/resource/upload', ['TinyEngineMaterialMvcController#uploadResource', 'TinyEngineMaterialWebFluxController#uploadResource']],
    ['GET /platform-center/api/courses', ['TinyEnginePlatformMvcController#courses', 'TinyEnginePlatformWebFluxController#courses']],
    ['GET /platform-center/api/courses/{id}', ['TinyEnginePlatformMvcController#courseDetail', 'TinyEnginePlatformWebFluxController#courseDetail']],
    ['POST /app-center/api/schema2code', ['TinyEngineCodegenMvcController#schemaToCode', 'TinyEngineCodegenWebFluxController#schemaToCode']],
    ['GET /app-center/api/code', ['TinyEngineCodegenMvcController#generateCode', 'TinyEngineCodegenWebFluxController#generateCode']],
    ['GET /app-center/api/source_tpl', ['TinyEngineCodegenMvcController#sourceTemplate', 'TinyEngineCodegenWebFluxController#sourceTemplate']]
  ])
  for (const [methodPath, [mvcHandler, webFluxHandler]] of webFluxParityContracts) {
    const contract = byMethodPath.get(methodPath)
    assert.equal(contract.currentMvcHandler, mvcHandler, methodPath)
    assert.equal(contract.currentWebFluxHandler, webFluxHandler, methodPath)
    assert.equal(contract.status, 'implemented', methodPath)
  }

  const batchCreate = byMethodPath.get('POST /material-center/api/resource/create/batch')
  assert.equal(batchCreate.officialController, 'ResourceController#createResource')
  assert.equal(batchCreate.responseShape, 'Result<List<Resource>>')

  assert.deepEqual(
    Object.fromEntries(['implemented', 'partial', 'missing'].map((status) => [
      status,
      contracts.filter((contract) => contract.status === status).length
    ])),
    { implemented: 59, partial: 0, missing: 27 }
  )
  assert.equal(contracts.filter((contract) => contract.officialController === NONE_CONTROLLER).length, 24)
})
