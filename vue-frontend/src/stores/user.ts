// 用户状态管理 Store

import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { loginApi, registerApi, getUserInfoApi, logoutApi } from '@/api/apiUser'
import { ui } from '@/utils/ui'
import {
  getAccessToken,
  setAccessToken,
  getRefreshToken,
  setRefreshToken,
  getTokenExpiry,
  setTokenExpiry,
  getAvatarVersion,
  setAvatarVersion,
  clearAuthData,
} from '@/utils/auth'
import type {
  LoginUserVo,
  RegisterUserVo,
  User,
  EmailVerificationRequired,
  CaptchaError,
  TokenResponse,
} from '@/types/user'
import { isEmailVerificationRequired } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const accessToken = ref<string>('')
  const refreshToken = ref<string>('')
  const tokenExpiresAt = ref<number>(0)
  const userInfo = ref<User | null>(null)
  const avatarVersion = ref<number>(0) // 头像版本，用于缓存破坏

  // 计算属性
  const isLoggedIn = computed(() => !!accessToken.value)
  const isTokenExpired = computed(() => {
    if (!tokenExpiresAt.value) return false
    return Date.now() > tokenExpiresAt.value
  })

  /**
   * 用户登录
   * @returns true - 登录成功
   * @returns EmailVerificationRequired - 需要邮箱验证
   * @returns CaptchaError - 验证码错误
   * @returns false - 登录失败
   */
  async function login(
    credentials: LoginUserVo,
  ): Promise<boolean | EmailVerificationRequired | CaptchaError> {
    try {
      const res = await loginApi(credentials)

      if (res.data.code === 0) {
        const data = res.data.data

        // 检查是否需要邮箱验证
        if (isEmailVerificationRequired(data)) {
          return data
        }

        await applyTokenResponse(data)

        ui.success('登录成功')
        return true
      } else {
        // 检查是否是验证码错误
        if (res.data.message === 'INCORRECT_CAPTCHA') {
          return { error: 'INCORRECT_CAPTCHA' }
        }
        ui.error(res.data.message || '登录失败')
        return false
      }
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
  }

  /**
   * 用户注册
   * @returns true - 注册成功，不需要邮箱验证
   * @returns EmailVerificationRequired - 需要邮箱验证
   * @returns CaptchaError - 验证码错误
   * @returns false - 注册失败
   */
  async function register(
    data: RegisterUserVo,
  ): Promise<boolean | EmailVerificationRequired | CaptchaError> {
    try {
      const res = await registerApi(data)
      const apiResponse = res.data

      // 情况1：没填邮箱但系统要求邮箱
      if (apiResponse.message === 'EMAIL_VERIFICATION_REQUIRED') {
        return {
          userId: '',
          email: '',
          message: 'EMAIL_VERIFICATION_REQUIRED',
        }
      }

      if (apiResponse.code === 0 && apiResponse.data) {
        const { registerResult, user, tokenResponse } = apiResponse.data

        // 情况2：填了邮箱，需要验证
        if (registerResult === 'require_email_verification') {
          return {
            userId: user.id,
            email: user.email || '',
            message: 'EMAIL_VERIFICATION_REQUIRED',
          }
        }

        applyTokens(tokenResponse)

        // 保存用户信息（注册直接返回了用户信息）
        userInfo.value = user

        // 持久化
        persistTokens()

        ui.success('注册成功')
        return true
      } else {
        // 检查是否是验证码错误
        if (apiResponse.message === 'INCORRECT_CAPTCHA') {
          return { error: 'INCORRECT_CAPTCHA' }
        }
        ui.error(apiResponse.message || '注册失败')
        return false
      }
    } catch (error) {
      console.error('注册失败:', error)
      return false
    }
  }

  /**
   * 获取用户信息
   */
  async function fetchUserInfo() {
    try {
      const res = await getUserInfoApi()

      if (res.data.code === 0) {
        userInfo.value = res.data.data
        return true
      } else {
        return false
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return false
    }
  }

  function applyTokens(tokenResponse: TokenResponse) {
    accessToken.value = tokenResponse.accessToken
    refreshToken.value = tokenResponse.refreshToken
    tokenExpiresAt.value = Date.now() + tokenResponse.expiresIn * 1000
  }

  function persistTokens() {
    setAccessToken(accessToken.value)
    setRefreshToken(refreshToken.value)
    setTokenExpiry(tokenExpiresAt.value)
  }

  async function applyTokenResponse(tokenResponse: TokenResponse) {
    applyTokens(tokenResponse)
    persistTokens()
    await fetchUserInfo()
  }

  /**
   * 登出
   */
  async function logout() {
    // 调用后端登出接口，将 tokens 加入黑名单
    if (refreshToken.value) {
      try {
        await logoutApi({ refreshToken: refreshToken.value })
      } catch (error) {
        // 即使后端登出失败，也继续清除本地状态
        console.error('登出接口调用失败:', error)
      }
    }

    // 清除状态
    accessToken.value = ''
    refreshToken.value = ''
    tokenExpiresAt.value = 0
    userInfo.value = null
    avatarVersion.value = 0

    // 清除 localStorage
    clearAuthData()
  }

  // 认证初始化状态
  const authInitialized = ref(false)
  const authInitPromise = ref<Promise<void> | null>(null)

  /**
   * 从 localStorage 恢复登录状态
   */
  function initAuth() {
    if (authInitPromise.value) {
      return authInitPromise.value
    }

    authInitPromise.value = (async () => {
      const token = getAccessToken()
      const refresh = getRefreshToken()
      const expiry = getTokenExpiry()
      const version = getAvatarVersion()

      if (token && refresh) {
        accessToken.value = token
        refreshToken.value = refresh
        tokenExpiresAt.value = expiry
        avatarVersion.value = version

        // 获取用户信息（如果 token 过期，拦截器会自动尝试刷新）
        await fetchUserInfo()
      }

      authInitialized.value = true
    })()

    return authInitPromise.value
  }

  /**
   * 等待认证初始化完成
   */
  async function waitForAuthInit() {
    if (authInitialized.value) return
    if (authInitPromise.value) {
      await authInitPromise.value
    }
  }

  /**
   * 增加头像版本号（用于缓存破坏）
   */
  function incrementAvatarVersion() {
    avatarVersion.value++
    // 持久化到 localStorage
    setAvatarVersion(avatarVersion.value)
  }

  return {
    // 状态
    accessToken,
    refreshToken,
    tokenExpiresAt,
    userInfo,
    avatarVersion,
    authInitialized,
    // 计算属性
    isLoggedIn,
    isTokenExpired,
    // 方法
    login,
    register,
    logout,
    fetchUserInfo,
    initAuth,
    waitForAuthInit,
    incrementAvatarVersion,
    applyTokenResponse,
  }
})
