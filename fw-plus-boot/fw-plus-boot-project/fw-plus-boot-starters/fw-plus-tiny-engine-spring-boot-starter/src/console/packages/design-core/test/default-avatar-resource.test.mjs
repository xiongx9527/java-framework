import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const REPO_ROOT = path.resolve(PACKAGE_ROOT, '../..')

test('default avatar image is available from designer public assets', () => {
  assert.equal(existsSync(path.join(PACKAGE_ROOT, 'public/img/defaultAvator.png')), true)
  assert.equal(existsSync(path.join(REPO_ROOT, 'designer-demo/public/img/defaultAvator.png')), true)
})
