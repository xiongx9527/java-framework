import assert from 'node:assert/strict'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const FRONTEND_ROOT = path.join(MODULE_ROOT, 'src/console')
const POM_FILE = path.join(MODULE_ROOT, 'pom.xml')

function walk(root) {
  const files = []
  const skippedDirectories = new Set(['node_modules', 'dist', 'coverage', '.git'])
  function visit(directory) {
    for (const name of readdirSync(directory)) {
      const full = path.join(directory, name)
      const rel = path.relative(root, full).split(path.sep).join('/')
      const stat = statSync(full)
      if (stat.isDirectory()) {
        if (skippedDirectories.has(name)) {
          continue
        }
        visit(full)
      } else {
        files.push(rel)
      }
    }
  }
  visit(root)
  return files.sort()
}

test('TinyEngine starter includes the frontend workspace source', () => {
  assert.equal(existsSync(FRONTEND_ROOT), true)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'package.json')), true)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'pnpm-workspace.yaml')), true)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'designer-demo/package.json')), true)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'packages/design-core/package.json')), true)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'packages/canvas/package.json')), true)
})

test('TinyEngine frontend source excludes docs and mock backend', () => {
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'docs')), false)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'mockServer')), false)
  assert.equal(existsSync(path.join(FRONTEND_ROOT, '.env.local')), false)

  const forbiddenSegments = [
    '/docs/',
    '/mockServer/'
  ]
  const forbiddenFiles = walk(FRONTEND_ROOT).filter(file => {
    const normalized = `/${file}/`
    return forbiddenSegments.some(segment => normalized.includes(segment))
  })
  assert.deepEqual(forbiddenFiles, [])
})

test('TinyEngine generated frontend artifacts are ignored by git', () => {
  const gitignore = readFileSync(path.join(FRONTEND_ROOT, '.gitignore'), 'utf8')

  assert.match(gitignore, /^node_modules$/m)
  assert.match(gitignore, /^dist\/$/m)
  assert.match(gitignore, /^pnpm-lock\.yaml$/m)
})

test('TinyEngine frontend workspace does not reference the removed mock server package', () => {
  const workspace = readFileSync(path.join(FRONTEND_ROOT, 'pnpm-workspace.yaml'), 'utf8')
  const rootPackage = readFileSync(path.join(FRONTEND_ROOT, 'package.json'), 'utf8')
  const designerPackage = readFileSync(path.join(FRONTEND_ROOT, 'designer-demo/package.json'), 'utf8')

  assert.doesNotMatch(workspace, /mockServer/)
  assert.doesNotMatch(rootPackage, /tiny-engine-mock|mockServer/)
  assert.doesNotMatch(designerPackage, /tiny-engine-mock|mockServer/)
})

test('TinyEngine frontend workspace has package-safe pnpm build configuration', () => {
  const workspace = readFileSync(path.join(FRONTEND_ROOT, 'pnpm-workspace.yaml'), 'utf8')
  const rootPackage = JSON.parse(readFileSync(path.join(FRONTEND_ROOT, 'package.json'), 'utf8'))

  assert.equal(rootPackage.scripts.build, 'pnpm run build:plugin && pnpm run build:prod')
  assert.match(rootPackage.scripts['build:plugin'], /--filter !@opentiny\/tiny-engine-cli/)
  assert.match(workspace, /allowBuilds:\s*\n(?:.*\n)*?\s+esbuild: true/)
  assert.match(workspace, /overrides:\s*\n\s+'@opentiny\/vue': ~3\.20\.0/)
  assert.match(workspace, /overrides:\s*\n(?:.*\n)*?\s+'@opentiny\/vue-\*': ~3\.20\.0/)
  assert.match(workspace, /overrides:\s*\n(?:.*\n)*?\s+rollup: ~4\.44\.0/)
  assert.match(workspace, /patchedDependencies:\s*\n\s+'@vue\/repl@4\.6\.1': patches\/@vue__repl@4\.6\.1\.patch/)
})

test('TinyEngine packages that import vue-renderless internals pin the compatible runtime dependency', () => {
  const packages = [
    'packages/common/package.json',
    'packages/canvas/package.json',
    'packages/plugins/block/package.json',
    'packages/plugins/datasource/package.json',
    'packages/plugins/materials/package.json',
    'packages/plugins/page/package.json',
    'packages/plugins/tree/package.json',
    'packages/settings/styles/package.json'
  ]

  for (const packageFile of packages) {
    const packageJson = JSON.parse(readFileSync(path.join(FRONTEND_ROOT, packageFile), 'utf8'))
    assert.equal(packageJson.dependencies['@opentiny/vue-renderless'], '~3.20.0', packageFile)
  }
})

test('TinyEngine console is built during package and exposed as Spring static resources', () => {
  const pom = readFileSync(POM_FILE, 'utf8')
  assert.match(pom, /<artifactId>exec-maven-plugin<\/artifactId>/)
  assert.match(pom, /<id>npm-install<\/id>/)
  assert.match(pom, /<id>npm-build<\/id>/)
  assert.match(pom, /<phase>prepare-package<\/phase>/)
  assert.match(pom, /<workingDirectory>\$\{project\.basedir\}\/src\/console<\/workingDirectory>/)
  assert.match(pom, /<executable>pnpm<\/executable>/)
  assert.match(pom, /<argument>install<\/argument>/)
  assert.match(pom, /<argument>run<\/argument>\s*<argument>build<\/argument>/s)
  assert.match(pom, /<id>copy-console-dist<\/id>/)
  assert.match(pom, /<outputDirectory>\$\{project\.build\.outputDirectory\}\/META-INF\/resources\/tiny-engine<\/outputDirectory>/)
  assert.match(pom, /<directory>src\/console\/designer-demo\/dist<\/directory>/)
  assert.doesNotMatch(pom, /<targetPath>tiny-engine-frontend<\/targetPath>/)
})
