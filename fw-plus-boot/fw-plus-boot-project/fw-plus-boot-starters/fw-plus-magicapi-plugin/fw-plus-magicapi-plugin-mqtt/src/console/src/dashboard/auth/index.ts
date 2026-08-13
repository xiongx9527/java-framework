import { createBasicAuthProvider } from './basic'
import { createHostAuthProvider } from './host'
import { createOidcAuthProvider } from './oidc'
import { getDashboardConfig } from '@/runtime'
import type { AuthMode, AuthProvider } from './types'

export * from './types'

export function createAuthProvider(): AuthProvider {
  const config = getDashboardConfig()
  const authMode = (config.authMode || 'host') as AuthMode

  if (authMode === 'host') return createHostAuthProvider()
  if (authMode === 'basic') return createBasicAuthProvider()
  if (authMode === 'oauth') {
    if (!config.oauthIssuer || !config.oauthClientId) {
      throw new Error('OAuth 模式缺少 oauthIssuer / oauthClientId 配置')
    }
    return createOidcAuthProvider({
      issuer: config.oauthIssuer,
      clientId: config.oauthClientId,
      scopes: config.oauthScopes || ['openid', 'profile'],
      redirectUri: config.oauthRedirectUri
    })
  }
  throw new Error(`无效的认证模式: ${authMode}`)
}
