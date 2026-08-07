import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const REPO_ROOT = path.resolve(PACKAGE_ROOT, '../../..')
const LANG_MAIN_FILE = path.join(PACKAGE_ROOT, 'src/Main.vue')
const COMMON_I18N_FILE = path.join(REPO_ROOT, 'packages/common/js/i18n.ts')
const DESIGN_APP_FILE = path.join(REPO_ROOT, 'packages/design-core/src/App.vue')
const CANVAS_LOCALE_FILE = path.join(REPO_ROOT, 'packages/canvas/render/src/canvas-function/locale.ts')
const PREVIEW_APP_FILE = path.join(REPO_ROOT, 'packages/design-core/src/preview/src/preview/srcFiles/App.vue')

test('language switch applies one persistent locale state across designer, canvas, and preview', () => {
  const toolbar = readFileSync(LANG_MAIN_FILE, 'utf8')
  const commonI18n = readFileSync(COMMON_I18N_FILE, 'utf8')
  const designApp = readFileSync(DESIGN_APP_FILE, 'utf8')
  const canvasLocale = readFileSync(CANVAS_LOCALE_FILE, 'utf8')
  const previewApp = readFileSync(PREVIEW_APP_FILE, 'utf8')

  assert.match(commonI18n, /export const LOCALE_STORAGE_KEY = 'tiny-engine-locale'/)
  assert.match(commonI18n, /export const getInitialLocale/)
  assert.match(commonI18n, /export const applyLocaleToI18n/)
  assert.match(commonI18n, /setItem\(LOCALE_STORAGE_KEY, locale\)/)

  assert.match(toolbar, /import \{ applyLocaleToI18n, getInitialLocale \}/)
  assert.match(toolbar, /const langVal = ref\(getInitialLocale\(\)\)/)
  assert.match(toolbar, /const applyLang = \(value\) =>/)
  assert.match(toolbar, /langVal\.value = applyLocaleToI18n\(value\)/)
  assert.match(toolbar, /applyLang\(data\.value\)/)
  assert.match(toolbar, /const nextLocale =/)
  assert.match(toolbar, /post\(langVal\.value\)/)
  assert.match(toolbar, /postLocaleMessage\(props\.langChannel, langVal\.value\)/)

  assert.match(designApp, /import \{ applyLocaleToI18n \}/)
  assert.match(designApp, /const locale = applyLocaleToI18n\(value\)/)
  assert.match(designApp, /currentLanguage\.value = locale/)

  assert.match(canvasLocale, /const LOCALE_STORAGE_KEY = 'tiny-engine-locale'/)
  assert.match(canvasLocale, /localStorage\.setItem\(LOCALE_STORAGE_KEY, localeValue\)/)
  assert.match(canvasLocale, /setLocale\(data\.value\)/)
  assert.match(canvasLocale, /setLocale\(message\.value\)/)

  assert.match(previewApp, /const LOCALE_STORAGE_KEY = 'tiny-engine-locale'/)
  assert.match(previewApp, /const initialLocale = getInitialLocale\(\)/)
  assert.match(previewApp, /i18n: \{ locale: initialLocale \}/)
  assert.match(previewApp, /localStorage\.setItem\(LOCALE_STORAGE_KEY, localeValue\)/)
  assert.match(previewApp, /setLocale\(data\.value\)/)
  assert.match(previewApp, /setLocale\(message\.value\)/)
})
