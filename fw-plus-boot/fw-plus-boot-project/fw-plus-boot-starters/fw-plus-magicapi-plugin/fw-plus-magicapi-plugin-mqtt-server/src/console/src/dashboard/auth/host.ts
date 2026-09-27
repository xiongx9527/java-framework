import { getDashboardRuntime } from '@/runtime'
import type { AuthProvider, LoginResult } from './types'

export function createHostAuthProvider(): AuthProvider {
  const username = () => {
    const runtime = getDashboardRuntime()
    return runtime.dashboard?.username || runtime.username || 'Magic-API'
  }

  return {
    mode: 'host',
    init() {},
    isAuthenticated: () => true,
    getUsername: username,
    login: async (): Promise<LoginResult> => ({ success: true }),
    startLogin: async () => {},
    handleCallback: async (): Promise<LoginResult> => ({ success: true }),
    getApiCredentials: () => null,
    logout() {}
  }
}
