// Axios 实例配置

import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { getAppConfig } from '@/config/appConfig'
import { getAccessToken, setAccessToken, setRefreshToken, setTokenExpiry } from './auth'
import { ui } from './ui'
import type { ApiResponse, TokenResponse } from '@/types/user'

// 扩展 AxiosRequestConfig 类型以支持自定义选项
declare module 'axios' {
  export interface AxiosRequestConfig {
    skipErrorHandler?: boolean
  }
}

// 创建 axios 实例
const request = axios.create({
  baseURL: getAppConfig().apiBaseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 正在刷新的 Promise（防止并发刷新）
let refreshingPromise: Promise<string> | null = null

// 刷新 Token 的函数
async function refreshToken(): Promise<string> {
  // 如果已经在刷新中，返回同一个 Promise
  if (refreshingPromise) {
    return refreshingPromise
  }

  refreshingPromise = (async () => {
    try {
      const { getRefreshToken } = await import('./auth')
      const oldRefreshToken = getRefreshToken()

      if (!oldRefreshToken) {
        throw new Error('No refresh token available')
      }

      // 直接使用 axios 调用刷新接口（避免拦截器循环）
      const response = await axios.post<ApiResponse<TokenResponse>>(
        `${getAppConfig().apiBaseUrl}/user/refresh-token`,
        { refreshToken: oldRefreshToken },
      )

      if (response.data.code === 0) {
        const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data

        // 保存新的 tokens（包括新的 refresh token！）
        setAccessToken(accessToken)
        setRefreshToken(newRefreshToken)
        setTokenExpiry(Date.now() + expiresIn * 1000)

        // 更新 Pinia store（延迟导入避免循环依赖）
        const { useUserStore } = await import('@/stores/user')
        const userStore = useUserStore()
        userStore.accessToken = accessToken
        userStore.refreshToken = newRefreshToken
        userStore.tokenExpiresAt = Date.now() + expiresIn * 1000

        return accessToken
      } else {
        throw new Error(response.data.message || 'Token refresh failed')
      }
    } catch (error) {
      // 刷新失败，清除登录状态
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      userStore.logout()

      // 跳转到登录页
      const { default: router } = await import('@/router')
      router.push('/login')

      throw error
    } finally {
      refreshingPromise = null
    }
  })()

  return refreshingPromise
}

// 请求拦截器
request.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()

    if (token) {
      // 检查 token 是否即将过期（5分钟内）或已经过期
      const { getTokenExpiry } = await import('./auth')
      const expiresAt = getTokenExpiry()
      const now = Date.now()
      const isExpired = expiresAt > 0 && now >= expiresAt
      const willExpireSoon = expiresAt > 0 && !isExpired && expiresAt - now < 5 * 60 * 1000

      if (isExpired || willExpireSoon) {
        try {
          // token 已过期或即将过期，主动刷新
          const newToken = await refreshToken()
          config.headers.Authorization = `Bearer ${newToken}`
        } catch {
          // 刷新失败，说明 refresh token 也无效了
          // 直接抛出错误，不继续发送请求
          return Promise.reject(new Error('Token refresh failed, please login again'))
        }
      } else {
        config.headers.Authorization = `Bearer ${token}`
      }
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const config = response.config as InternalAxiosRequestConfig & { skipErrorHandler?: boolean }

    // 如果是 blob 类型响应，直接返回（不检查 code）
    if (config.responseType === 'blob') {
      return response
    }

    const res = response.data as ApiResponse

    // 业务错误处理（code !== 0）
    if (res.code !== 0) {
      // 跳过错误处理，让业务层自己判断
      if (config.skipErrorHandler) {
        return response
      }

      ui.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response
  },
  async (error: AxiosError<ApiResponse>) => {
    // 401 错误 - Token 过期或无效
    if (error.response?.status === 401) {
      const config = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

      // 避免无限重试
      if (!config._retry) {
        config._retry = true

        try {
          // 尝试刷新 token
          const newToken = await refreshToken()

          // 更新请求头
          config.headers.Authorization = `Bearer ${newToken}`

          // 重试原请求
          return request(config)
        } catch (refreshError) {
          // 刷新失败，已在 refreshToken 函数中处理
          ui.error('登录已过期，请重新登录')
          return Promise.reject(refreshError)
        }
      }
    }

    // 其他错误
    const message = error.response?.data?.message || error.message || '请求失败'
    ui.error(message)
    return Promise.reject(error)
  },
)

export default request
