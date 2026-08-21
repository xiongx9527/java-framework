import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createSharedModelConfig } from '../../src/composables/core/sharedModelConfig'

const service = {
  id: 11,
  serviceKey: 'openai',
  provider: 'openai',
  label: 'OpenAI',
  baseUrl: 'https://example.test/v1',
  allowEmptyApiKey: false,
  enabled: true,
  hasApiKey: true,
  apiKeyMask: '********',
  models: [{ id: 22, name: 'gpt-test', label: 'GPT Test', enabled: true, capabilities: '{}' }]
}

const memoryStorage = (initial?: Record<string, unknown>) => {
  let value = initial ? JSON.stringify(initial) : null
  return {
    getItem: vi.fn(() => value),
    setItem: vi.fn((_key: string, next: string) => {
      value = next
    }),
    snapshot: () => (value ? JSON.parse(value) : null)
  }
}

const managerApi = (settings: Record<string, unknown> = {}) => ({
  listManageable: vi.fn().mockResolvedValue([service]),
  listUsable: vi.fn(),
  getSettings: vi.fn().mockResolvedValue(settings),
  updateSettings: vi.fn(),
  importLocal: vi.fn()
})

const developerApi = (settings: Record<string, unknown> = {}) => ({
  listUsable: vi.fn().mockResolvedValue([service]),
  listManageable: vi.fn(),
  getSettings: vi.fn().mockResolvedValue(settings),
  updateSettings: vi.fn(),
  importLocal: vi.fn()
})

describe('shared application model config', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('loads the manageable view for owner/admin without retaining an apiKey', async () => {
    const api = managerApi({ defaultModel: 'openai::gpt-test', chatMode: 'chat', enableThinking: true })
    const store = createSharedModelConfig(api as any, memoryStorage() as any)

    await store.init(2, 'app_admin')

    expect(api.listManageable).toHaveBeenCalledWith(2)
    expect(api.getSettings).toHaveBeenCalledWith(2)
    expect(store.state.canManage).toBe(true)
    expect(JSON.stringify(store.state.services)).not.toContain('model-secret')
    expect(store.state.services[0]).toMatchObject({ hasApiKey: true, apiKeyMask: '********' })
    expect(store.buildChatSelection()).toMatchObject({
      appId: 2,
      serviceId: 11,
      modelName: 'gpt-test',
      chatMode: 'chat',
      enableThinking: true
    })
    expect(store.buildChatSelection()).not.toHaveProperty('apiKey')
    expect(store.buildChatSelection()).not.toHaveProperty('baseUrl')
  })

  it('keeps disabled services and models visible in the management view without selecting them', async () => {
    const disabled = {
      ...service,
      id: 12,
      serviceKey: 'disabled',
      enabled: false,
      models: [{ id: 23, name: 'disabled-model', label: 'Disabled', enabled: false, capabilities: '{}' }]
    }
    const api = managerApi({ defaultModel: 'disabled::disabled-model' })
    api.listManageable.mockResolvedValue([disabled])
    const store = createSharedModelConfig(api as any, memoryStorage() as any)

    await store.init(2, 'app_owner')

    expect(store.state.services[0]).toMatchObject({ id: 12, enabled: false })
    expect(store.state.services[0].models[0]).toMatchObject({ name: 'disabled-model', enabled: false })
    expect(store.buildChatSelection()).toMatchObject({ serviceId: 12, modelName: 'disabled-model' })
  })

  it('restores application-shared default and quick selections by stable service key', async () => {
    const api = managerApi({ defaultModel: 'openai::gpt-test', quickModel: 'openai::gpt-test' })
    const store = createSharedModelConfig(api as any, memoryStorage() as any)

    await store.init(2, 'app_admin')

    expect(store.state.defaultModel).toEqual({ serviceId: '11', modelName: 'gpt-test' })
    expect(store.state.quickModel).toEqual({ serviceId: '11', modelName: 'gpt-test' })
  })

  it('loads only the usable view for app_dev and disables management', async () => {
    const api = developerApi({ defaultModel: 'openai::gpt-test' })
    const store = createSharedModelConfig(api as any, memoryStorage() as any)

    await store.init(2, 'app_dev')

    expect(api.listUsable).toHaveBeenCalledWith(2)
    expect(api.listManageable).not.toHaveBeenCalled()
    expect(store.state.canManage).toBe(false)
  })

  it('persists shared model selections and chat behavior through the remote settings API', async () => {
    const api = developerApi({ defaultModel: 'openai::gpt-test' })
    api.updateSettings.mockResolvedValue({
      defaultModel: 'openai::gpt-test',
      quickModel: 'openai::gpt-test',
      chatMode: 'chat',
      enableThinking: true
    })
    const store = createSharedModelConfig(api as any, memoryStorage() as any)

    await store.init(2, 'app_dev')
    await store.updateSettings({ quickModel: '11::gpt-test', chatMode: 'chat', enableThinking: true })

    expect(api.updateSettings).toHaveBeenCalledWith(2, {
      quickModel: '11::gpt-test',
      chatMode: 'chat',
      enableThinking: true
    })
    expect(store.state.quickModel).toEqual({ serviceId: '11', modelName: 'gpt-test' })
    expect(store.buildChatSelection()).toMatchObject({ chatMode: 'chat', enableThinking: true })
  })

  it('does not silently fall back when the shared default selection is missing or invalid', async () => {
    const api = developerApi({ defaultModel: 'missing::unknown' })
    const store = createSharedModelConfig(api as any, memoryStorage() as any)

    await store.init(2, 'app_dev')

    expect(store.state.defaultModel).toEqual({ serviceId: 'missing', modelName: 'unknown' })
    expect(store.buildChatSelection()).toMatchObject({ serviceId: 0, modelName: 'unknown' })
  })

  it('does not overwrite existing remote configuration with local settings', async () => {
    const storage = memoryStorage({ services: [{ apiKey: 'legacy-secret' }], unknown: 'keep' })
    const api = managerApi({ defaultModel: 'openai::gpt-test' })
    const store = createSharedModelConfig(api as any, storage as any)

    await store.init(2, 'app_owner')

    expect(api.importLocal).not.toHaveBeenCalled()
    expect(storage.snapshot()).toMatchObject({ services: [{ apiKey: 'legacy-secret' }], unknown: 'keep' })
  })

  it('does not import when remote behavior settings already exist without services', async () => {
    const local = { services: [{ id: 'legacy', serviceKey: 'legacy', apiKey: 'legacy-secret', models: [] }], unknown: 'keep' }
    const storage = memoryStorage(local)
    const api = managerApi({ chatMode: 'chat', remoteUnknown: false })
    api.listManageable.mockResolvedValue([])
    const store = createSharedModelConfig(api as any, storage as any)

    await store.init(2, 'app_owner')

    expect(api.importLocal).not.toHaveBeenCalled()
    expect(storage.snapshot()).toEqual(local)
  })

  it('cleans migrated model fields but preserves chat behavior and unknown fields after a successful first import', async () => {
    const storage = memoryStorage({
      services: [{ id: 'legacy', serviceKey: 'legacy', apiKey: 'legacy-secret', models: [{ name: 'old', settings: { temperature: 0.2 } }] }],
      defaultModel: { serviceId: 'legacy', modelName: 'old' },
      quickModel: { serviceId: 'legacy', modelName: 'old-fast' },
      chatMode: 'chat',
      enableThinking: true,
      unknown: 'keep'
    })
    const api = managerApi()
    api.listManageable.mockResolvedValueOnce([]).mockResolvedValueOnce([service])
    api.getSettings
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce({ defaultModel: 'legacy::old', chatMode: 'chat', enableThinking: true })
    api.importLocal.mockResolvedValue([service])
    const store = createSharedModelConfig(api as any, storage as any)

    await store.init(2, 'app_owner')

    expect(api.importLocal).toHaveBeenCalledOnce()
    expect(api.importLocal.mock.calls[0][1].apiKeys).toEqual({ legacy: 'legacy-secret' })
    expect(api.importLocal.mock.calls[0][1].settings.services[0].models[0].settings).toBe(
      '{\"temperature\":0.2}'
    )
    expect(storage.snapshot()).toEqual({ chatMode: 'chat', enableThinking: true, unknown: 'keep' })
  })

  it('keeps the full local settings when import fails', async () => {
    const local = { services: [{ id: 'legacy', serviceKey: 'legacy', apiKey: 'legacy-secret', models: [] }], unknown: 'keep' }
    const storage = memoryStorage(local)
    const api = managerApi()
    api.listManageable.mockResolvedValue([])
    api.importLocal.mockRejectedValue(new Error('failed'))
    const store = createSharedModelConfig(api as any, storage as any)

    await expect(store.init(2, 'app_owner')).rejects.toThrow('failed')
    expect(storage.snapshot()).toEqual(local)
  })
})
