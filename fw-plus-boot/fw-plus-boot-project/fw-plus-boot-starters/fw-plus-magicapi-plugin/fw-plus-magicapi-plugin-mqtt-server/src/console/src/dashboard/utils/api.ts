import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosRequestConfig } from 'axios'
import type { ApiCredentials } from '@/auth'
import { assertCurrentGeneration, getServerContext, notifyServerUnavailable } from '@/context'
import { getDashboardConfig, getDashboardRuntime } from '@/runtime'
import { mapUpstreamApiRequest } from '@/utils/upstream-api-adapter'

export interface DashboardResponse<T> {
  data: T
  status: number
}

type CredentialsProvider = () => ApiCredentials | null

let credentialsProvider: CredentialsProvider | null = null
let unauthorizedHandler: (() => void) | null = null

export const setCredentialsProvider = (provider: CredentialsProvider | null) => {
  credentialsProvider = provider
}

export const setUnauthorizedHandler = (handler: (() => void) | null) => {
  unauthorizedHandler = handler
}

const staleError = () => {
  const error = new Error('MQTT 服务端已切换，忽略旧请求结果') as any
  error.stale = true
  return error
}

interface MappedApiPath {
  path: string
  query: Record<string, string>
  serverBound: boolean
  generation?: number
}

const mapApiPath = (url: string): MappedApiPath => {
  const context = getServerContext()
  const mapped = mapUpstreamApiRequest(url, context.selectedServerId)
  return mapped.serverBound
    ? { ...mapped, generation: context.generation }
    : mapped
}

const hostRequest = <T>(method: 'get' | 'post', url: string, data?: any, config: AxiosRequestConfig = {}) => {
  const runtime = getDashboardRuntime()
  const request = runtime.request
  if (!request) return Promise.reject(new Error('Magic-API 宿主请求能力不可用'))
  const mapped = mapApiPath(url)
  const params = { ...mapped.query, ...(config.params || {}) }

  return new Promise<DashboardResponse<T>>((resolve, reject) => {
    let settled = false
    const response = method === 'get'
      ? request.sendGet(mapped.path, params)
      : (mapped.path.endsWith('/mqtt/subscribe') || mapped.path.endsWith('/mqtt/unsubscribe'))
        ? request.sendJson(mapped.path, data || {})
        : request.sendPost(mapped.path, Object.keys(params).length ? params : data)

    response
      .success((payload: T) => {
        settled = true
        try {
          if (mapped.serverBound) assertCurrentGeneration(mapped.generation as number)
          resolve({ data: { code: 1, data: payload } as T, status: 200 })
        } catch {
          reject(staleError())
        }
      })
      .exception((code: number, message: string) => {
        settled = true
        const error = new Error(message || `请求失败 (${code})`) as any
        error.response = { status: code, data: { code, message } }
        reject(error)
      })
      .error((payload: any, rawResponse: any, cause: any) => {
        settled = true
        const error = cause instanceof Error ? cause : new Error(payload?.message || '请求失败')
        ;(error as any).response = rawResponse
        reject(error)
      })
      .end((success: boolean) => {
        if (!success && !settled) reject(new Error('请求未成功完成'))
      })
  })
}

const directRequest = async <T>(method: 'get' | 'post', url: string, data?: any, config: AxiosRequestConfig = {}) => {
  const mapped = mapApiPath(url)
  const credentials = credentialsProvider?.()
  const requestConfig: AxiosRequestConfig = {
    ...config,
    method,
    baseURL: getDashboardConfig().apiBaseUrl || '',
    url: mapped.path,
    data,
    params: { ...mapped.query, ...(config.params || {}) },
    withCredentials: true,
    timeout: 10000
  }
  if (credentials?.auth) requestConfig.auth = credentials.auth
  if (credentials?.headers) requestConfig.headers = { ...requestConfig.headers, ...credentials.headers }
  const response = await axios.request(requestConfig)
  if (response.data?.code !== 1) {
    const error = new Error(response.data?.message || `请求失败 (${response.data?.code})`) as any
    error.response = { status: response.data?.code, data: response.data }
    throw error
  }
  if (mapped.serverBound) assertCurrentGeneration(mapped.generation as number)
  return response as DashboardResponse<T>
}

const request = async <T>(method: 'get' | 'post', url: string, data?: any, config: AxiosRequestConfig = {}) => {
  try {
    return getDashboardConfig().authMode === 'host'
      ? await hostRequest<T>(method, url, data, config)
      : await directRequest<T>(method, url, data, config)
  } catch (error: any) {
    if (error?.stale) throw error
    const status = error?.response?.status
    const errorMessage = error?.response?.data?.message || error?.message || ''
    if (/服务端不存在或未运行/.test(errorMessage)) notifyServerUnavailable(errorMessage)
    if (status === 401) {
      unauthorizedHandler?.()
      ElMessage.error('认证失败，请重新登录')
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在')
    } else {
      ElMessage.error(error?.response?.data?.message || error?.message || '请求失败')
    }
    throw error
  }
}

export const api = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>('get', url, undefined, config)
  },
  post<T>(url: string, data?: any, config?: AxiosRequestConfig) {
    return request<T>('post', url, data, config)
  }
}
