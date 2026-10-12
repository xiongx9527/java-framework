/**
 * 将 mica-mqtt-dashboard 上游 `/api/v1/*` 契约适配为本插件的
 * `/mqtt/dashboard/{serverId}/*` 进程内多服务端管理契约。
 */
const UPSTREAM_API_PATHS: Record<string, string> = {
  '/api/v1/stats': '/stats',
  '/api/v1/clients': '/clients',
  '/api/v1/clients/info': '/clients/info',
  '/api/v1/client/subscriptions': '/client/subscriptions',
  '/api/v1/clients/delete': '/clients/delete',
  '/api/v1/mqtt/subscribe': '/mqtt/subscribe',
  '/api/v1/mqtt/unsubscribe': '/mqtt/unsubscribe'
}

export interface MappedDashboardRequest {
  path: string
  query: Record<string, string>
  serverBound: boolean
}

export const mapUpstreamApiRequest = (url: string, serverId: string): MappedDashboardRequest => {
  if (url === '/mqtt/dashboard/servers') {
    return { path: url, query: {}, serverBound: false }
  }

  const parsed = new URL(url, 'http://magic-mqtt.local')
  const suffix = UPSTREAM_API_PATHS[parsed.pathname]
  if (!suffix) throw new Error(`不支持的 MQTT Dashboard API: ${parsed.pathname}`)
  if (!serverId) throw new Error('请选择 MQTT 服务端')

  return {
    path: `/mqtt/dashboard/${encodeURIComponent(serverId)}${suffix}`,
    query: Object.fromEntries(parsed.searchParams.entries()),
    serverBound: true
  }
}
