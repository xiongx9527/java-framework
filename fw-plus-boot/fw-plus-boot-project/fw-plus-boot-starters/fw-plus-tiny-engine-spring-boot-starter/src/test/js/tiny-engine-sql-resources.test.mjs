import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const STARTER_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const REPOSITORY_ROOT = path.resolve(STARTER_ROOT, '../../../..')
const STARTER_SQL_ROOT = path.join(STARTER_ROOT, 'src/main/resources/META-INF/tiny-engine/sql')
const CORE_SQL_ROOT = path.join(
  REPOSITORY_ROOT,
  'fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql'
)

test('TinyEngine starter owns every SQL resource declared by its manifest', () => {
  const manifestFile = path.join(STARTER_SQL_ROOT, 'manifest.properties')
  assert.equal(existsSync(manifestFile), true, 'starter must package the TinyEngine SQL manifest')
  assert.equal(existsSync(CORE_SQL_ROOT), false, 'core must not duplicate starter-owned SQL resources')

  const entries = readFileSync(manifestFile, 'utf8')
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('#'))

  assert.equal(entries.length, 29)
  for (const entry of entries) {
    const separator = entry.indexOf('=')
    assert.notEqual(separator, -1, `invalid manifest entry: ${entry}`)
    const location = entry.slice(separator + 1)
    const prefix = 'classpath:META-INF/tiny-engine/sql/'
    assert.equal(location.startsWith(prefix), true, `invalid SQL classpath location: ${location}`)
    assert.equal(existsSync(path.join(STARTER_SQL_ROOT, location.slice(prefix.length))), true, location)
  }
})

test('TinyEngine production manifest excludes test fixture data', () => {
  const manifest = readFileSync(path.join(STARTER_SQL_ROOT, 'manifest.properties'), 'utf8')
  assert.doesNotMatch(manifest, /init_data_for_test_v1\.0\.0\.sql/)
})
