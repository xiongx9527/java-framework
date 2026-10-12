import { api } from '@/utils/api'
import type { ApiCredentials, AuthProvider, LoginResult } from './types'

// 独立 Dashboard 的管理认证凭据，用于保留上游 Basic“记住密码”功能；与 MQTT 调试凭据无关。
const MANAGEMENT_USERNAME_KEY = 'mica_mqtt_dashboard_management_username'
const MANAGEMENT_PASSWORD_KEY = 'mica_mqtt_dashboard_management_password'

export function createBasicAuthProvider(): AuthProvider {
  let isAuthenticated = false
  let username = ''
  let password = ''

  const init = () => {
    const storedUsername = localStorage.getItem(MANAGEMENT_USERNAME_KEY)
    const storedPassword = localStorage.getItem(MANAGEMENT_PASSWORD_KEY)
    if (storedUsername && storedPassword) {
      username = storedUsername
      password = storedPassword
      isAuthenticated = true
    }
  }

  const login = async (user: string, pass: string): Promise<LoginResult> => {
    try {
      const response = await api.get('/api/v1/stats', { auth: { username: user, password: pass } })
      if (response.status === 200) {
        username = user
        password = pass
        isAuthenticated = true
        localStorage.setItem(MANAGEMENT_USERNAME_KEY, user)
        localStorage.setItem(MANAGEMENT_PASSWORD_KEY, pass)
        return { success: true }
      }
      return { success: false, message: '认证失败' }
    } catch (error: any) {
      if (error.response?.status === 401) return { success: false, message: '用户名或密码错误' }
      return { success: false, message: error.message || '登录失败，请检查服务器连接' }
    }
  }

  const getApiCredentials = (): ApiCredentials | null => isAuthenticated ? { auth: { username, password } } : null

  const logout = () => {
    username = ''
    password = ''
    isAuthenticated = false
    localStorage.removeItem(MANAGEMENT_USERNAME_KEY)
    localStorage.removeItem(MANAGEMENT_PASSWORD_KEY)
  }

  return {
    mode: 'basic', init, isAuthenticated: () => isAuthenticated, getUsername: () => username,
    login,
    startLogin: async () => { throw new Error('basic 模式不支持 OAuth 登录') },
    handleCallback: async (): Promise<LoginResult> => ({ success: false, message: 'basic 模式不支持 OAuth 回调' }),
    getApiCredentials, logout
  }
}
