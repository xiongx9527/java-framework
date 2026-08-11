import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const REPO_ROOT = path.resolve(PACKAGE_ROOT, '../..')
const PREVIEW_HELPER_FILE = path.join(PACKAGE_ROOT, 'js/preview.ts')
const PREVIEW_COMPONENT_FILE = path.join(REPO_ROOT, 'packages/design-core/src/preview/src/preview/Preview.vue')

test('preview carries designer viewport params and applies them on preview page', () => {
  const previewHelper = readFileSync(PREVIEW_HELPER_FILE, 'utf8')
  const previewComponent = readFileSync(PREVIEW_COMPONENT_FILE, 'utf8')

  assert.match(previewHelper, /useLayout/)
  assert.match(previewHelper, /useLayout\(\)\.getDimension\(\)/)
  assert.match(previewHelper, /appendViewportQueryParam\(queryParams,\s*'deviceType',\s*dimension\.deviceType\)/)
  assert.match(previewHelper, /appendViewportQueryParam\(queryParams,\s*'width',\s*dimension\.width\)/)
  assert.match(previewHelper, /appendViewportQueryParam\(queryParams,\s*'minWidth',\s*dimension\.minWidth\)/)
  assert.match(previewHelper, /appendViewportQueryParam\(queryParams,\s*'maxWidth',\s*dimension\.maxWidth\)/)
  assert.match(previewHelper, /appendViewportQueryParam\(queryParams,\s*'height',\s*dimension\.height\)/)
  assert.match(previewHelper, /previewWindow\.location\.href = openUrl/)

  assert.match(previewComponent, /applyViewportFromQuery/)
  assert.match(previewComponent, /queryParams\.get\('width'\)/)
  assert.match(previewComponent, /queryParams\.get\('deviceType'\)/)
  assert.match(previewComponent, /document\.getElementsByClassName\('iframe-container'\)\[0\]/)
  assert.match(previewComponent, /previewContainer\.dataset\.deviceType = deviceType/)
})
