import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const REPO_ROOT = path.resolve(MODULE_ROOT, '../../../..')
const FRONTEND_ROOT = path.join(MODULE_ROOT, 'src/console')
const SAMPLE_ROOT = path.join(REPO_ROOT, 'fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample')

test('TinyEngine AI sample configuration does not embed model endpoint or credentials', () => {
  const files = [
    path.join(SAMPLE_ROOT, 'src/main/resources/application.yml'),
    path.join(SAMPLE_ROOT, 'src/test/resources/application.yml')
  ]

  for (const file of files) {
    const sampleYaml = readFileSync(file, 'utf8')
    assert.doesNotMatch(sampleYaml, /^\s+(baseUrl|apiKey|modelName):\s*\S+/m, file)
    assert.doesNotMatch(sampleYaml, /sk-[A-Za-z0-9]/, file)
  }
})

test('TinyEngine Robot keeps official AI endpoints and upstream model presets', () => {
  const robotApi = readFileSync(
    path.join(FRONTEND_ROOT, 'packages/plugins/robot/src/services/api.ts'),
    'utf8'
  )
  const agentServices = readFileSync(
    path.join(FRONTEND_ROOT, 'packages/canvas/container/src/services/agentServices.ts'),
    'utf8'
  )
  const modelConfig = readFileSync(
    path.join(FRONTEND_ROOT, 'packages/plugins/robot/src/constants/model-config.ts'),
    'utf8'
  )

  assert.match(robotApi, /app-center\/api\/chat\/completions/)
  assert.match(robotApi, /app-center\/api\/ai\/chat/)
  assert.match(agentServices, /app-center\/api\/ai\/chat/)
  assert.match(modelConfig, /provider:\s*'bailian'/)
  assert.match(modelConfig, /provider:\s*'deepseek'/)
})
