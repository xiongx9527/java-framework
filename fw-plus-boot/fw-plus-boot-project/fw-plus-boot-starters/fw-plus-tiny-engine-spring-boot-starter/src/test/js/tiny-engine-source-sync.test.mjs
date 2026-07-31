import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, mkdtempSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const SYNC_SCRIPT = path.join(MODULE_ROOT, 'scripts/sync-tiny-engine-console.sh')

test('sync copies build inputs and excludes local state', () => {
	const root = mkdtempSync(path.join(tmpdir(), 'tiny-engine-sync-'))
	const source = path.join(root, 'source')
	const target = path.join(root, 'target')
	mkdirSync(path.join(source, 'designer-demo'), { recursive: true })
	mkdirSync(path.join(source, 'packages', 'demo'), { recursive: true })
	mkdirSync(path.join(source, 'node_modules', 'bad'), { recursive: true })
	writeFileSync(path.join(source, 'package.json'), '{}')
	writeFileSync(path.join(source, 'package-lock.json'), '{}')
	writeFileSync(path.join(source, 'pnpm-workspace.yaml'), 'packages: []')
	writeFileSync(path.join(source, 'LICENSE'), 'MIT')
	execFileSync('git', ['init'], { cwd: source })
	execFileSync('git', ['config', 'user.email', 'tiny-engine-sync@example.invalid'], { cwd: source })
	execFileSync('git', ['config', 'user.name', 'TinyEngine Sync Test'], { cwd: source })
	execFileSync('git', ['add', '.'], { cwd: source })
	execFileSync('git', ['commit', '-m', 'fixture'], { cwd: source })
	execFileSync('sh', [SYNC_SCRIPT, source, target], { cwd: MODULE_ROOT })
	assert.match(readFileSync(path.join(target, '.tinyengine-source.properties'), 'utf8'), /source\.commit=[0-9a-f]{40}/)
	assert.equal(existsSync(path.join(target, 'node_modules')), false)
	assert.equal(existsSync(path.join(target, 'package-lock.json')), false)
	assert.equal(existsSync(path.join(target, 'designer-demo')), true)
})
