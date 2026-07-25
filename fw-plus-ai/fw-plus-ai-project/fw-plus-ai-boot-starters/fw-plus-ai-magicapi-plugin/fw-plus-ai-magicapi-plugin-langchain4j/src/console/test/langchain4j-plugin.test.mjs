import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { normalizePluginArtifact } from './helpers/plugin-contract.mjs'

const artifactUrl = new URL('../../main/resources/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js', import.meta.url)
const fixtureUrl = new URL('./fixtures/legacy-contract.json', import.meta.url)

const loadFixture = async () => JSON.parse(await readFile(fixtureUrl, 'utf8'))

test('生成的 IIFE 完整保持旧插件行为并增加状态栏项', async () => {
    const fixture = await loadFixture()
    const artifactPath = fileURLToPath(artifactUrl)
    const source = await readFile(artifactUrl, 'utf8')
    const actual = normalizePluginArtifact(source, path.basename(artifactPath))

    assert.deepEqual({
        locales: actual.locales,
        registration: actual.registration,
        component: actual.component,
        services: actual.services,
        styles: actual.styles,
        icon: actual.icon
    }, fixture.legacyContract)
    assert.deepEqual(actual.statusBarItems, fixture.requiredAdditions.statusBarItems)
})

test('构建配置在有效层级声明 ESNext target 和固定 IIFE 输出', async () => {
    const packageJson = JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8'))
    assert.equal(packageJson.name, 'magic-langchain4j')
    assert.equal(packageJson.version, '1.0.0')
    assert.equal(packageJson.scripts.test, 'node --test test/langchain4j-plugin.test.mjs')
    assert.equal(packageJson.scripts.build, 'vite build')

    const viteConfig = (await import('../vite.config.js')).default
    assert.equal(viteConfig.build.target, 'esnext')
    assert.equal(Object.hasOwn(viteConfig.build.lib, 'target'), false)
    assert.equal(viteConfig.build.lib.name, 'MagicLangchain4j')
    assert.equal(viteConfig.build.lib.fileName('iife'), 'magic-langchain4j.1.0.0.iife.js')
    assert.deepEqual(viteConfig.build.rollupOptions.external, ['vue'])
})

test('旧行为 fixture 固定到指定 Git 对象且覆盖完整契约', async () => {
    const fixture = await loadFixture()
    assert.deepEqual(fixture.provenance, {
        sourceCommit: '8c4f3d4083bd86c83456c57493a610ce17fd5f5a',
        sourceBlob: '17b809a2ced85b9aa9bb3c4c29ff369a77a95982',
        sourcePath: 'src/main/resources/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js',
        sourceBytes: 81161,
        sourceSha256: '0f6638785af5a1f3683a32847fd3c4a51a7d9dc6806661763231cc7c27a2c7e7',
        normalization: 'test/helpers/plugin-contract.mjs'
    })
    assert.deepEqual(fixture.legacyContract.locales.map(item => item.locale), ['zh-cn', 'en'])
    assert.equal(fixture.legacyContract.component.rows.length, 8)
    assert.equal(fixture.legacyContract.styles.every(style => style.scoped), true)
    assert.equal(fixture.legacyContract.icon.symbolId, 'magic-langchain4j-langchain4j')
    assert.equal(fixture.legacyContract.services.success.requestResult, 'ok')
    assert.equal(fixture.legacyContract.services.failure.requestResult, 'connection refused')
})
