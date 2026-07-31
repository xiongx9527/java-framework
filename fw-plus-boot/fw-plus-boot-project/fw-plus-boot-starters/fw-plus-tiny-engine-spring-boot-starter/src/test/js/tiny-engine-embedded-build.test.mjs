import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import path from 'node:path'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const CONSOLE_ROOT = path.join(MODULE_ROOT, 'src/console')

test('fw-plus build is deterministic and deployable below a context path', () => {
  const rootPackage = JSON.parse(readFileSync(path.join(CONSOLE_ROOT, 'package.json')))
  assert.equal(
    rootPackage.scripts['build:fwplus'],
    'pnpm --filter @opentiny/tiny-engine-* --filter @opentiny/tiny-engine --filter !@opentiny/tiny-engine-cli --filter !@opentiny/tiny-engine-mock build && pnpm --filter designer-demo build:fwplus'
  )
  const vite = readFileSync(path.join(CONSOLE_ROOT, 'designer-demo/vite.config.js'), 'utf8')
  assert.match(vite, /base:\s*'\.\/'/)
  assert.match(vite, /outDir:\s*'dist'/)
  assert.doesNotMatch(vite, /\/Users\/|localhost:|127\.0\.0\.1:/)
  const workspace = readFileSync(path.join(CONSOLE_ROOT, 'pnpm-workspace.yaml'), 'utf8')
  assert.match(workspace, /esbuild:\s*true/)
})

test('fw-plus embedded console mock bundle does not ship localhost upload endpoints', () => {
  const bundle = readFileSync(path.join(CONSOLE_ROOT, 'designer-demo/public/mock/bundle.json'), 'utf8')
  const templateBundle = readFileSync(
    path.join(CONSOLE_ROOT, 'packages/engine-cli/template/designer/public/mock/bundle.json'),
    'utf8'
  )

  assert.doesNotMatch(bundle, /localhost:|127\.0\.0\.1:/)
  assert.doesNotMatch(templateBundle, /localhost:|127\.0\.0\.1:/)
  assert.match(bundle, /"action":\s*"\/api\/upload"/)
  assert.match(templateBundle, /"action":\s*"\/api\/upload"/)
})

test('fw-plus build keeps pnpm 11 overrides in workspace config', () => {
  const rootPackage = JSON.parse(readFileSync(path.join(CONSOLE_ROOT, 'package.json')))
  const workspace = readFileSync(path.join(CONSOLE_ROOT, 'pnpm-workspace.yaml'), 'utf8')
  const tinyVuePackages = [
    '@opentiny/vue',
    '@opentiny/vue-design-smb',
    '@opentiny/vue-icon',
    '@opentiny/vue-locale',
    '@opentiny/vue-renderless',
    '@opentiny/vue-runtime',
    '@opentiny/vue-theme'
  ]

  assert.equal(rootPackage.pnpm, undefined)
  assert.match(workspace, /patchedDependencies:\s*\n\s+'@vue\/repl@4\.6\.1':\s*patches\/@vue__repl@4\.6\.1\.patch/)
  assert.match(workspace, /overrides:\s*\n(?:\s+.+\n)*\s+rollup:\s*~4\.44\.0/)
  for (const packageName of tinyVuePackages) {
    const escapedPackageName = packageName.replace('/', '\\/')
    assert.match(workspace, new RegExp(`'${escapedPackageName}':\\s*3\\.20\\.0`))
  }
})

test('fw-plus build uses package dist outputs instead of build-time source alias', async () => {
  const aliasPluginUrl = pathToFileURL(
    path.join(CONSOLE_ROOT, 'packages/build/vite-config/src/vite-plugins/devAliasPlugin.js')
  )
  const { devAliasPlugin } = await import(aliasPluginUrl)
  const originalOrigin = process.env.VITE_ORIGIN
  process.env.VITE_ORIGIN = ''

  try {
    const config = devAliasPlugin({}, true).config({}, { command: 'build' })
    assert.deepEqual(config.resolve.alias, {})
  } finally {
    if (originalOrigin === undefined) {
      delete process.env.VITE_ORIGIN
    } else {
      process.env.VITE_ORIGIN = originalOrigin
    }
  }
})
