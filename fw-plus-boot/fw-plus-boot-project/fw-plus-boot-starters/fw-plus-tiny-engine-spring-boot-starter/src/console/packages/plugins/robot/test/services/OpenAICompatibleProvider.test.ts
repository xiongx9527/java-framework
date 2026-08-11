import { describe, expect, it, vi } from 'vitest'

vi.mock('../../src/utils', () => ({
  formatMessages: (messages: unknown[]) => messages
}))

import { OpenAICompatibleProvider } from '../../src/services/OpenAICompatibleProvider'

describe('OpenAICompatibleProvider TinyEngine proxy credentials', () => {
  it('keeps the engine Authorization header available and sends the model key in the request body', async () => {
    const request = vi.fn().mockResolvedValue({ data: { choices: [] } })
    const provider = new OpenAICompatibleProvider({
      apiKey: 'model-key',
      apiUrl: 'app-center/api/chat/completions',
      defaultModel: 'gpt-test',
      httpClientType: 'axios',
      axiosClient: { request },
      beforeRequest: (body) => ({ ...body, baseUrl: 'https://example.test/v1' })
    } as any)

    await provider.chat({ messages: [{ role: 'user', content: 'hello' }] } as any)

    expect(request).toHaveBeenCalledOnce()
    const config = request.mock.calls[0][0]
    expect(config.headers.Authorization).toBeUndefined()
    expect(config.data.apiKey).toBe('model-key')
    expect(config.data.baseUrl).toBe('https://example.test/v1')
  })
})
