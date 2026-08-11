import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const COMPONENT_MAIN_FILE = path.join(PACKAGE_ROOT, 'src/meta/component/src/Main.vue')

test('materials panel falls back locale objects without rendering raw JSON', () => {
  const source = readFileSync(COMPONENT_MAIN_FILE, 'utf8')

  assert.match(source, /const getLocaleText = /)
  assert.match(source, /value\?\.\[locale\.value\]/)
  assert.match(source, /value\?\.zh_CN/)
  assert.match(source, /:title="getLocaleText\(item\.label, item\.group\)"/)
  assert.match(source, /:title="getLocaleText\(child\.name\)"/)
  assert.match(source, /getLocaleText\(child\.name\)/)
})
