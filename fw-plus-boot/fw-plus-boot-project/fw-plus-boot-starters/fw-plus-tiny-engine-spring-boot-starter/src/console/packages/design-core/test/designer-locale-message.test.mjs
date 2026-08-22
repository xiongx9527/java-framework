import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const REPO_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const DESIGN_APP_FILE = path.join(REPO_ROOT, 'packages/design-core/src/App.vue')
const COMMON_I18N_FILE = path.join(REPO_ROOT, 'packages/common/js/i18n.ts')

test('designer app applies locale change messages to the main i18n instance', () => {
  const app = readFileSync(DESIGN_APP_FILE, 'utf8')
  const commonI18n = readFileSync(COMMON_I18N_FILE, 'utf8')

  assert.match(app, /import \{ applyLocaleToI18n \} from '@opentiny\/tiny-engine-common\/js\/i18n'/)
  assert.doesNotMatch(app, /@opentiny\/tiny-engine-common\/i18n/)
  assert.match(app, /useTranslate/)
  assert.match(app, /TINY_ENGINE_LOCALE_CHANGE/)
  assert.match(app, /message\?\.channel !== BROADCAST_CHANNEL\.CanvasLang/)
  assert.match(app, /applyLocaleToI18n/)
  assert.match(app, /const locale = applyLocaleToI18n\(value\)/)
  assert.match(app, /currentLanguage\.value = locale/)
  assert.match(app, /window\.addEventListener\('message', handleLocaleMessage\)/)
  assert.match(app, /window\.removeEventListener\('message', handleLocaleMessage\)/)

  assert.match(commonI18n, /import enUs from '\.\.\/i18n\/en-us\.json'/)
  assert.match(commonI18n, /import zhCn from '\.\.\/i18n\/zh-cn\.json'/)
  assert.match(commonI18n, /mergeLocaleMessage\(i18nKeyMaps\.enUS, enUs\)/)
  assert.match(commonI18n, /mergeLocaleMessage\(i18nKeyMaps\.zhCN, zhCn\)/)
})
