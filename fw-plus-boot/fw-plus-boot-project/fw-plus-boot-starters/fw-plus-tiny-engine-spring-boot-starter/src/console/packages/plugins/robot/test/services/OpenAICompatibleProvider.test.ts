import { describe, expect, it, vi } from 'vitest'

vi.mock('../../src/utils', () => ({
  formatMessages: (messages: unknown[]) => messages
}))

import { OpenAICompatibleProvider } from '../../src/services/OpenAICompatibleProvider'

describe('OpenAICompatibleProvider TinyEngine proxy credentials', () => {
  it('keeps model credentials out of the browser proxy request', async () => {
    const request = vi.fn().mockResolvedValue({ data: { choices: [] } })
    const provider = new OpenAICompatibleProvider({
      apiUrl: 'app-center/api/chat/completions',
      defaultModel: 'gpt-test',
      httpClientType: 'axios',
      axiosClient: { request },
      beforeRequest: (body) => ({ ...body, appId: 2, serviceId: 11, modelName: 'gpt-test' })
    } as any)

    await provider.chat({ messages: [{ role: 'user', content: 'hello' }] } as any)

    expect(request).toHaveBeenCalledOnce()
    const config = request.mock.calls[0][0]
    expect(config.headers.Authorization).toBeUndefined()
    expect(config.data).toMatchObject({ appId: 2, serviceId: 11, modelName: 'gpt-test' })
    expect(config.data).not.toHaveProperty('apiKey')
    expect(config.data).not.toHaveProperty('baseUrl')
    expect(config.data).not.toHaveProperty('model')
  })
})
