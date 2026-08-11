import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const USE_PREVIEW_DATA_FILE = path.join(PACKAGE_ROOT, 'src/preview/src/preview/usePreviewData.ts')

test('app preview renders without requiring a pageid query parameter', () => {
  const source = readFileSync(USE_PREVIEW_DATA_FILE, 'utf8')

  assert.match(source, /const previewType = searchParams\.get\('previewType'\)/)
  assert.match(source, /const isAppPreview = previewType === 'app'/)
  assert.match(source, /if \(currentPage \|\| isAppPreview\) \{/)
  assert.match(source, /updatePreview\(\{ currentPage, ancestors, scripts, styles, componentsMap \}\)/)
})
