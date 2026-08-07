import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const REPO_ROOT = path.resolve(PACKAGE_ROOT, '../../..')
const LANG_MAIN_FILE = path.join(PACKAGE_ROOT, 'src/Main.vue')
const PREVIEW_APP_FILE = path.join(REPO_ROOT, 'packages/design-core/src/preview/src/preview/srcFiles/App.vue')
const CANVAS_LOCALE_FILE = path.join(REPO_ROOT, 'packages/canvas/render/src/canvas-function/locale.ts')

test('language toolbar falls back to postMessage when BroadcastChannel is unavailable', () => {
  const toolbar = readFileSync(LANG_MAIN_FILE, 'utf8')
  const previewApp = readFileSync(PREVIEW_APP_FILE, 'utf8')
  const canvasLocale = readFileSync(CANVAS_LOCALE_FILE, 'utf8')

  assert.match(toolbar, /:content="toolbarTitle"/)
  assert.match(toolbar, /const toolbarTitle = computed/)
  assert.match(toolbar, /TINY_ENGINE_LOCALE_CHANGE/)
  assert.match(toolbar, /postLocaleMessage/)
  assert.match(toolbar, /contentWindow\?\.postMessage\(message/)
  assert.match(toolbar, /window\.postMessage\(message/)

  assert.match(previewApp, /TINY_ENGINE_LOCALE_CHANGE/)
  assert.match(previewApp, /window\.addEventListener\('message', handleLocaleMessage\)/)
  assert.match(previewApp, /window\.removeEventListener\('message', handleLocaleMessage\)/)
  assert.match(previewApp, /setLocale\(message\.value\)/)
  assert.match(previewApp, /i18n\.global\.locale\.value = localeValue/)

  assert.match(canvasLocale, /TINY_ENGINE_LOCALE_CHANGE/)
  assert.match(canvasLocale, /window\.addEventListener\('message', handleLocaleMessage\)/)
  assert.match(canvasLocale, /window\.removeEventListener\('message', handleLocaleMessage\)/)
  assert.match(canvasLocale, /setLocale\(message\.value\)/)
  assert.match(canvasLocale, /locale as WritableComputedRef<unknown>\)\.value = localeValue/)
})
