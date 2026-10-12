import { getMagicMqttDashboardRuntime } from '../service/magic-mqtt-dashboard-runtime.js'

export type DashboardAuthMode = 'host' | 'basic' | 'oauth'

export interface DashboardRuntimeConfig {
  authMode?: DashboardAuthMode
  oauthIssuer?: string
  oauthClientId?: string
  oauthScopes?: string[]
  oauthRedirectUri?: string
  apiBaseUrl?: string
}

export const getDashboardRuntime = (): any => getMagicMqttDashboardRuntime() || {}

export const getDashboardConfig = (): DashboardRuntimeConfig => {
  const runtime = getDashboardRuntime()
  const globalConfig = typeof window === 'undefined'
    ? {}
    : ((window as any).MICA_MQTT_DASHBOARD_CONFIG || {})
  return {
    authMode: runtime.dashboard?.authMode || globalConfig.authMode || 'host',
    oauthIssuer: runtime.dashboard?.oauthIssuer || globalConfig.oauthIssuer,
    oauthClientId: runtime.dashboard?.oauthClientId || globalConfig.oauthClientId,
    oauthScopes: runtime.dashboard?.oauthScopes || globalConfig.oauthScopes,
    oauthRedirectUri: runtime.dashboard?.oauthRedirectUri || globalConfig.oauthRedirectUri,
    apiBaseUrl: runtime.dashboard?.apiBaseUrl || globalConfig.apiBaseUrl
  }
}
