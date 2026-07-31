import assert from 'node:assert/strict'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const REPO_ROOT = path.resolve(MODULE_ROOT, '../../../..')
const COVERAGE_DOC = path.join(REPO_ROOT, 'docs/tiny-engine/backend-source-coverage.md')
const BACKEND_ROOT = process.env.TINY_ENGINE_BACKEND_SOURCE || '/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java'
const REQUIRED_COMMIT = 'c116fa649122390baf212f53d82358617c63924d'

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function listFiles(root, predicate) {
  const result = []
  function walk(dir) {
    for (const name of readdirSync(dir)) {
      const full = path.join(dir, name)
      const rel = path.relative(root, full)
      if (rel.startsWith('.git') || rel.includes(`${path.sep}target${path.sep}`)) continue
      const stat = statSync(full)
      if (stat.isDirectory()) {
        walk(full)
      } else if (predicate(rel)) {
        result.push(rel.split(path.sep).join('/'))
      }
    }
  }
  walk(root)
  return result.sort()
}

test('backend coverage matrix records the exact source revision', () => {
  assert.equal(existsSync(BACKEND_ROOT), true)
  const commit = execFileSync('git', ['-C', BACKEND_ROOT, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim()
  assert.equal(commit, REQUIRED_COMMIT)
  const status = execFileSync('git', ['-C', BACKEND_ROOT, 'status', '--porcelain', '--untracked-files=no'], { encoding: 'utf8' }).trim()
  assert.equal(status, '')
  const doc = readFileSync(COVERAGE_DOC, 'utf8')
  assert.match(doc, new RegExp(REQUIRED_COMMIT))
})

test('backend coverage matrix does not omit package, config, deploy, document or model asset categories', () => {
  const doc = readFileSync(COVERAGE_DOC, 'utf8')
  const requiredCategories = [
    'common',
    'config',
    'controller',
    'dynamic',
    'event',
    'gateway/ai',
    'login',
    'mapper',
    'model',
    'rag',
    'service',
    'app/src/main/java',
    'app/src/main/resources/application',
    'app/src/main/resources/sql',
    'docker-deploy-data',
    'Dockerfile',
    'docker-compose.yml',
    'example-docker.yml',
    'documents/agent',
    'documents/tinyengine',
    'settings',
    'all-MiniLM-L6-v2'
  ]
  for (const category of requiredCategories) {
    assert.match(doc, new RegExp(escapeRegExp(category)), `missing category ${category}`)
  }
})

test('backend coverage matrix lists every mapper xml and sql file', () => {
  const doc = readFileSync(COVERAGE_DOC, 'utf8')
  const mapperFiles = listFiles(path.join(BACKEND_ROOT, 'base/src/main/resources/mappers'), rel => rel.endsWith('.xml'))
  const sqlFiles = listFiles(path.join(BACKEND_ROOT, 'app/src/main/resources/sql'), rel => rel.endsWith('.sql'))
  for (const file of mapperFiles) {
    assert.match(doc, new RegExp(escapeRegExp(file)), `missing mapper ${file}`)
  }
  for (const file of sqlFiles) {
    assert.match(doc, new RegExp(escapeRegExp(file)), `missing sql ${file}`)
  }
})
