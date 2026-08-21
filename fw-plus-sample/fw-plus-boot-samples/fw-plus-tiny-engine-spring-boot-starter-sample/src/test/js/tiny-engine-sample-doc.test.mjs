import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const SAMPLE_ROOT = path.resolve(fileURLToPath(new URL('.', import.meta.url)), '../../..')
const REPO_ROOT = path.resolve(SAMPLE_ROOT, '../../..')
const SAMPLE_POM = path.join(SAMPLE_ROOT, 'pom.xml')
const SAMPLE_APP = path.join(SAMPLE_ROOT, 'src/main/java/com/xiongx9527/fw/plus/sample/tinyengine/TinyEngineStarterSampleApplication.java')
const SAMPLE_APPLICATION_YML = path.join(SAMPLE_ROOT, 'src/main/resources/application.yml')
const STARTER_DOC = path.join(REPO_ROOT, 'docs/tiny-engine/starter.md')

const read = file => readFileSync(file, 'utf8')

test('TinyEngine sample declares WebFlux as an explicit test web stack', () => {
  const pom = read(SAMPLE_POM)

  assert.match(pom, /<artifactId>spring-boot-starter-webflux<\/artifactId>/)
  assert.match(pom, /<artifactId>spring-boot-starter-webflux<\/artifactId>[\s\S]*?<scope>test<\/scope>/)
})

test('TinyEngine sample provides runnable application entry and runtime configuration', () => {
  assert.equal(existsSync(SAMPLE_APP), true, 'sample application entry must exist')
  assert.equal(existsSync(SAMPLE_APPLICATION_YML), true, 'sample application.yml must exist')

  assert.match(read(SAMPLE_APP), /@SpringBootApplication/)
  const yml = read(SAMPLE_APPLICATION_YML)
  assert.match(yml, /fw:\s*\n\s+plus:\s*\n\s+tiny-engine:/)
  assert.match(yml, /enabled:\s*true/)
  assert.match(yml, /auto-initialize:\s*false/)
})

test('TinyEngine starter guide documents external frontend and Magic-API entry configuration', () => {
  assert.equal(existsSync(STARTER_DOC), true, 'docs/tiny-engine/starter.md must exist')
  const doc = read(STARTER_DOC)

  for (const required of [
    'fw-plus-tiny-engine-spring-boot-starter',
    'spring-boot-starter-web',
    'spring-boot-starter-webflux',
    'fw-plus-magicapi-plugin-tiny-engine',
    'pluginData.attributes.path',
    'pluginData.attributes.url',
    'TinyEngineAuthenticationProvider',
    'TinyEngineUserSystemService',
    'designer-demo',
    'mockServer',
    '不内置',
    '外部部署'
  ]) {
    assert.match(doc, new RegExp(required.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
