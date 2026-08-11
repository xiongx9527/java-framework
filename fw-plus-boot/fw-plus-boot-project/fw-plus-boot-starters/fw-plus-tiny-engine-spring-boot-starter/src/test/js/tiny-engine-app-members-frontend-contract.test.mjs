import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const APP_CENTER_ROOT = path.join(MODULE_ROOT, 'src/console/packages/workspace/application-center/src')
const MAIN_FILE = path.join(APP_CENTER_ROOT, 'Main.vue')
const DIALOG_FILE = path.join(APP_CENTER_ROOT, 'AppMemberDialog.vue')
const HTTP_FILE = path.join(APP_CENTER_ROOT, 'js/http.js')

test('application center exposes member management in card and table views only for owner and admin', () => {
  const main = readFileSync(MAIN_FILE, 'utf8')

  assert.equal(existsSync(DIALOG_FILE), true)
  assert.equal((main.match(/>成员管理</g) || []).length, 2)
  assert.match(main, /app_owner/)
  assert.match(main, /app_admin/)
  assert.match(main, /canManageMembers\(item\)/)
  assert.match(main, /canManageMembers\(data\.row\)/)
  assert.match(main, /canDeleteApplication\(data\.row\)/)
  assert.match(main, /canManageApplication\(data\.row\)/)
})

test('member dialog protects owner and current operator and supports retry plus explicit mutations', () => {
  const dialog = readFileSync(DIALOG_FILE, 'utf8')

  assert.match(dialog, /app_owner/)
  assert.match(dialog, /currentUserId/)
  assert.match(dialog, />重试</)
  assert.match(dialog, /addAppMember/)
  assert.match(dialog, /updateAppMember/)
  assert.match(dialog, /deleteAppMember/)
  assert.match(dialog, /loadMembers/)
})

test('application member HTTP client uses the shared MVC and WebFlux API contract', () => {
  const http = readFileSync(HTTP_FILE, 'utf8')

  assert.match(http, /fetchAppMembers/)
  assert.match(http, /addAppMember/)
  assert.match(http, /updateAppMember/)
  assert.match(http, /deleteAppMember/)
  assert.match(http, /\/app-center\/api\/apps\/\$\{appId\}\/members/)
})
