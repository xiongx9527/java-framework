import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const PACKAGE_ROOT = path.resolve(path.dirname(TEST_FILE), '..')
const REPO_ROOT = path.resolve(PACKAGE_ROOT, '../../..')
const GENERATE_CODE_MAIN_FILE = path.join(PACKAGE_ROOT, 'src/Main.vue')
const FS_INDEX_FILE = path.join(REPO_ROOT, 'packages/utils/src/fs/index.ts')

test('generate-code falls back to zip when browser rejects directory access', () => {
  const toolbar = readFileSync(GENERATE_CODE_MAIN_FILE, 'utf8')
  const fsIndex = readFileSync(FS_INDEX_FILE, 'utf8')

  assert.match(fsIndex, /export const writeFilesAsZip = async/)
  assert.match(toolbar, /isDirectoryAccessRejected/)
  assert.match(toolbar, /isDirectoryPickerCanceled/)
  assert.match(toolbar, /saveCodeAsZip/)
  assert.match(toolbar, /fs\.writeFilesAsZip\(saveData\)/)
  assert.match(toolbar, /已改为下载 zip 压缩包/)
  assert.match(toolbar, /state\.dirHandle = null/)
})
