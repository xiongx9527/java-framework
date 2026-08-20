import assert from 'node:assert/strict'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const FRONTEND_ROOT = path.join(MODULE_ROOT, 'src/console')
const POM_FILE = path.join(MODULE_ROOT, 'pom.xml')
const ENGINE_CONFIG_FILE = path.join(FRONTEND_ROOT, 'designer-demo/engine.config.js')
const REGISTRY_FILE = path.join(FRONTEND_ROOT, 'designer-demo/registry.js')
const GLOBAL_SERVICE_FILE = path.join(FRONTEND_ROOT, 'packages/common/composable/defaultGlobalService.ts')
const COLLABORATION_TOOLBAR_FILE = path.join(FRONTEND_ROOT, 'packages/toolbars/collaboration/src/Main.vue')
const GENERATE_CODE_TOOLBAR_FILE = path.join(FRONTEND_ROOT, 'packages/toolbars/generate-code/src/Main.vue')
const USER_TOOLBAR_FILE = path.join(FRONTEND_ROOT, 'packages/toolbars/user/src/Main.vue')
const LOGO_TOOLBAR_FILE = path.join(FRONTEND_ROOT, 'packages/toolbars/logo/src/Main.vue')
const LOCK_TOOLBAR_HTTP_FILE = path.join(FRONTEND_ROOT, 'packages/toolbars/lock/src/http.ts')
const FS_INDEX_FILE = path.join(FRONTEND_ROOT, 'packages/utils/src/fs/index.ts')
const PRODUCTION_ENV_FILE = path.join(FRONTEND_ROOT, 'designer-demo/env/.env.production')
const MOCK_BUNDLE_FILE = path.join(FRONTEND_ROOT, 'designer-demo/public/mock/bundle.json')

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
  assert.equal(existsSync(path.join(FRONTEND_ROOT, 'pnpm-lock.yaml')), true)
  assert.doesNotMatch(gitignore, /^pnpm-lock\.yaml$/m)
})

test('TinyEngine frontend workspace does not reference the removed mock server package', () => {
  const workspace = readFileSync(path.join(FRONTEND_ROOT, 'pnpm-workspace.yaml'), 'utf8')
  const rootPackage = readFileSync(path.join(FRONTEND_ROOT, 'package.json'), 'utf8')
  const designerPackage = readFileSync(path.join(FRONTEND_ROOT, 'designer-demo/package.json'), 'utf8')

  assert.doesNotMatch(workspace, /mockServer/)
  assert.doesNotMatch(rootPackage, /tiny-engine-mock|mockServer/)
  assert.doesNotMatch(designerPackage, /tiny-engine-mock|mockServer/)
})

test('TinyEngine packaged console requires login when backend security is enabled', () => {
  const engineConfig = readFileSync(ENGINE_CONFIG_FILE, 'utf8')

  assert.match(engineConfig, /enableLogin:\s*true/)
})

test('TinyEngine packaged preview uses its static entry and a concrete platform id', () => {
  const engineConfig = readFileSync(ENGINE_CONFIG_FILE, 'utf8')
  const registry = readFileSync(REGISTRY_FILE, 'utf8')

  assert.match(engineConfig, /platformId:\s*0/)
  assert.match(registry, /previewUrl:\s*`\$\{baseURLWithoutSlash\}\/preview\.html`/)
  assert.doesNotMatch(registry, /\['prod',\s*'alpha'\]\.includes\(import\.meta\.env\.MODE\)/)
})

test('TinyEngine packaged preview disables unavailable Tailwind browser resources', () => {
  const engineConfig = readFileSync(ENGINE_CONFIG_FILE, 'utf8')

  assert.match(engineConfig, /enableTailwindCSS:\s*false/)
})

test('TinyEngine packaged preview avoids npmmirror browser CORS for import maps', () => {
  const productionEnv = readFileSync(PRODUCTION_ENV_FILE, 'utf8')
  const mockBundle = readFileSync(MOCK_BUNDLE_FILE, 'utf8')

  assert.match(productionEnv, /VITE_CDN_DOMAIN=https:\/\/unpkg\.com/)
  assert.doesNotMatch(productionEnv, /^VITE_CDN_DOMAIN=https:\/\/registry\.npmmirror\.com$/m)
  assert.doesNotMatch(productionEnv, /^VITE_CDN_TYPE=npmmirror$/m)
  assert.doesNotMatch(mockBundle, /https:\/\/registry\.npmmirror\.com\/@opentiny\/vue-runtime/)
  assert.doesNotMatch(mockBundle, /https:\/\/registry\.npmmirror\.com\/@opentiny\/vue-theme/)
  assert.match(mockBundle, /https:\/\/unpkg\.com\/@opentiny\/vue-runtime@3\.22\/dist3\/tiny-vue-huicharts\.mjs/)
  assert.match(mockBundle, /https:\/\/unpkg\.com\/@opentiny\/vue-theme@~3\.20\/index\.css/)
})

test('TinyEngine public mode skips the current-user request', () => {
  const globalService = readFileSync(GLOBAL_SERVICE_FILE, 'utf8')

  assert.match(globalService, /if \(getMergeMeta\('engine\.config'\)\?\.enableLogin\) \{\s*fetchUserInfo\(\)/)
})

test('TinyEngine tenant creation keeps login required and logout uses the official user API', () => {
  const userToolbar = readFileSync(USER_TOOLBAR_FILE, 'utf8')
  const logoToolbar = readFileSync(LOGO_TOOLBAR_FILE, 'utf8')

  assert.match(userToolbar, /localStorage\.getItem\('engineToken'\)/)
  assert.match(userToolbar, /请先登录后再创建组织/)
  assert.match(userToolbar, /post\('\/platform-center\/api\/user\/logout'/)
  assert.match(logoToolbar, /post\('\/platform-center\/api\/user\/logout'/)
  assert.doesNotMatch(userToolbar, /post\('\/platform-center\/api\/tenant\/create'[\s\S]*?localStorage\.getItem\('engineToken'\)[\s\S]*?\)/)
  assert.doesNotMatch(logoToolbar, /\/platform-center\/api\/logout/)
})

test('TinyEngine collaboration avatar is bundled instead of requested from a missing public path', () => {
  const collaborationToolbar = readFileSync(COLLABORATION_TOOLBAR_FILE, 'utf8')

  assert.match(collaborationToolbar, /import defaultAvator from ['"]\.\.\/\.\.\/\.\.\/design-core\/public\/img\/defaultAvator\.png['"]/)
  assert.doesNotMatch(collaborationToolbar, /['"]img\/defaultAvator\.png['"]/)
})

test('TinyEngine frontend workspace has package-safe pnpm build configuration', () => {
  const workspace = readFileSync(path.join(FRONTEND_ROOT, 'pnpm-workspace.yaml'), 'utf8')
  const rootPackage = JSON.parse(readFileSync(path.join(FRONTEND_ROOT, 'package.json'), 'utf8'))
  const npmrc = readFileSync(path.join(FRONTEND_ROOT, '.npmrc'), 'utf8')

  assert.equal(rootPackage.packageManager, 'pnpm@11.9.0')
  assert.match(npmrc, /^registry=https:\/\/registry\.npmjs\.org\/$/m)
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
  assert.match(pom, /<artifactId>maven-resources-plugin<\/artifactId>/)
  assert.match(pom, /<id>copy-console-dist<\/id>/)
  assert.match(pom, /<outputDirectory>\$\{project\.build\.outputDirectory\}\/META-INF\/resources\/tiny-engine<\/outputDirectory>/)
  assert.match(pom, /<directory>src\/console\/designer-demo\/dist<\/directory>/)
  assert.doesNotMatch(pom, /<targetPath>/)
})

test('TinyEngine packages the collaboration avatar at its legacy public URL', () => {
  const pom = readFileSync(POM_FILE, 'utf8')

  assert.match(pom, /<id>copy-console-public-images<\/id>/)
  assert.match(pom, /<outputDirectory>\$\{project\.build\.outputDirectory\}\/META-INF\/resources\/tiny-engine\/img<\/outputDirectory>/)
  assert.match(pom, /<directory>src\/console\/packages\/design-core\/public\/img<\/directory>/)
  assert.match(pom, /<include>defaultAvator\.png<\/include>/)
})

test('TinyEngine generate-code falls back to zip when browser rejects the selected directory', () => {
  const toolbar = readFileSync(GENERATE_CODE_TOOLBAR_FILE, 'utf8')
  const fsIndex = readFileSync(FS_INDEX_FILE, 'utf8')

  assert.match(fsIndex, /export const writeFilesAsZip = async/)
  assert.match(toolbar, /isDirectoryAccessRejected/)
  assert.match(toolbar, /fs\.writeFilesAsZip\(saveData\)/)
  assert.match(toolbar, /已改为下载 zip 压缩包/)
})

test('TinyEngine page lock bypasses the TinyEngine asset base URL when calling the backend API', () => {
  const lockHttp = readFileSync(LOCK_TOOLBAR_HTTP_FILE, 'utf8')

  assert.match(lockHttp, /window\.location\.origin/)
  assert.match(lockHttp, /get\(`\$\{window\.location\.origin\}\/app-center\/api\/apps\/canvas\/lock\?\$\{params\}`\)/)
  assert.doesNotMatch(lockHttp, /get\(`app-center\/api\/apps\/canvas\/lock\?\$\{params\}`\)/)
  assert.doesNotMatch(lockHttp, /get\(`\/app-center\/api\/apps\/canvas\/lock\?\$\{params\}`\)/)
})
