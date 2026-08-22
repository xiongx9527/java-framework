import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const LOGIN_FILE = path.join(PACKAGE_ROOT, 'src/login/Login.vue')

test('login stores token from either TinyEngine token or accessToken response field', () => {
  const source = readFileSync(LOGIN_FILE, 'utf8')

  assert.match(source, /data\.token\s*\|\|\s*data\.accessToken/)
  assert.doesNotMatch(source, /localStorage\.setItem\('engineToken',\s*data\.token\)/)
})
