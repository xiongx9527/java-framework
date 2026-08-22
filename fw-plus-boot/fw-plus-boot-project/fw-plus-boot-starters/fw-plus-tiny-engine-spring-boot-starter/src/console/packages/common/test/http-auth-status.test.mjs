import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const HTTP_FILE = path.join(PACKAGE_ROOT, 'composable/http/index.ts')

test('http service distinguishes TinyEngine login failures from AI and permission failures', () => {
  const source = readFileSync(HTTP_FILE, 'utf8')
  const responseErrorHandler = source.slice(source.indexOf('const responseErrorHandler'))

  assert.match(responseErrorHandler, /LoginErrorCode\.includes\(data\.code\)/)
  assert.doesNotMatch(source, /const notAuthList/)
  assert.match(responseErrorHandler, /response\.status\s*===\s*401/)
  assert.doesNotMatch(responseErrorHandler, /response\.status\s*===\s*401\s*\|\|\s*response\.status\s*===\s*403/)
  assert.match(responseErrorHandler, /toLogin\(\)/)
  assert.match(responseErrorHandler, /skipShowError:\s*true/)
  assert.match(responseErrorHandler, /return Promise\.reject\(\{\s*type:\s*'AUTH_ERROR'/s)
})
