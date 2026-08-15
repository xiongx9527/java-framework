import { describe, expect, it, vi } from 'vitest'

vi.mock('@opentiny/tiny-engine-meta-register', () => ({
  META_SERVICE: { Http: 'http' },
  getMetaApi: vi.fn()
}))

import { getMetaApi } from '@opentiny/tiny-engine-meta-register'
import { modelConfigApi } from '../../src/services/modelConfigApi'

describe('modelConfigApi', () => {
  it('uses the application-scoped management and mutation routes', async () => {
    const http = {
      get: vi.fn().mockResolvedValue([]),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({})
    }
    vi.mocked(getMetaApi).mockReturnValue(http as any)

    await modelConfigApi.listUsable(2)
    await modelConfigApi.listManageable(2)
    await modelConfigApi.getSettings(2)
    await modelConfigApi.updateSettings(2, { defaultModel: 'openai::gpt-test' })
    await modelConfigApi.createService(2, { serviceKey: 'openai' } as any)
    await modelConfigApi.updateService(2, 11, { label: 'OpenAI' } as any)
    await modelConfigApi.deleteService(2, 11)
    await modelConfigApi.addModel(2, 11, { name: 'gpt-test' } as any)
    await modelConfigApi.updateModel(2, 11, 22, { label: 'GPT Test' } as any)
    await modelConfigApi.deleteModel(2, 11, 22)
    await modelConfigApi.importLocal(2, { settings: {}, apiKeys: {} } as any)

    expect(http.get).toHaveBeenNthCalledWith(1, 'app-center/api/apps/2/ai-model-services')
    expect(http.get).toHaveBeenNthCalledWith(2, 'app-center/api/apps/2/ai-model-services/manage')
    expect(http.get).toHaveBeenNthCalledWith(3, 'app-center/api/apps/2/ai-model-services/settings')
    expect(http.put).toHaveBeenCalledWith(
      'app-center/api/apps/2/ai-model-services/settings',
      expect.objectContaining({ defaultModel: 'openai::gpt-test' })
    )
    expect(http.post).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services', expect.any(Object))
    expect(http.put).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services/11', expect.any(Object))
    expect(http.delete).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services/11')
    expect(http.post).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services/11/models', expect.any(Object))
    expect(http.put).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services/11/models/22', expect.any(Object))
    expect(http.delete).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services/11/models/22')
    expect(http.post).toHaveBeenCalledWith('app-center/api/apps/2/ai-model-services/import-local', expect.any(Object))
  })
})
