import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const FRONTEND_ROOT = path.join(MODULE_ROOT, 'src/console')

const packages = [
  '@opentiny/tiny-engine-block-compiler',
  '@opentiny/tiny-engine-meta-register',
  '@opentiny/tiny-engine-common',
  '@opentiny/tiny-engine-canvas'
]

const ANSI_ESCAPE = /\x1B\[[0-?]*[ -\/]*[@-~]/g

test('TinyEngine library builds do not emit TypeScript diagnostics', { timeout: 180_000 }, () => {
  const failures = []

  for (const packageName of packages) {
    const result = spawnSync('pnpm', ['--filter', packageName, 'build'], {
      cwd: FRONTEND_ROOT,
      encoding: 'utf8',
      env: process.env,
      maxBuffer: 20 * 1024 * 1024
    })
    const output = `${result.stdout || ''}\n${result.stderr || ''}`.replace(ANSI_ESCAPE, '')

    if (result.status !== 0 || /error TS\d+:/.test(output)) {
      failures.push({ packageName, status: result.status, output })
    }
  }

  assert.deepEqual(failures, [])
})
