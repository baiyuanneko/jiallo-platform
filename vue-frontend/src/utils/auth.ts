// Token 存储工具函数

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const TOKEN_EXPIRY_KEY = 'token_expiry'
const AVATAR_VERSION_KEY = 'avatar_version'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function getTokenExpiry(): number {
  const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY)
  return expiry ? parseInt(expiry, 10) : 0
}

export function setTokenExpiry(timestamp: number): void {
  localStorage.setItem(TOKEN_EXPIRY_KEY, timestamp.toString())
}

export function getAvatarVersion(): number {
  const version = localStorage.getItem(AVATAR_VERSION_KEY)
  return version ? parseInt(version, 10) : 0
}

export function setAvatarVersion(version: number): void {
  localStorage.setItem(AVATAR_VERSION_KEY, version.toString())
}

export function clearAuthData(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXPIRY_KEY)
  localStorage.removeItem(AVATAR_VERSION_KEY)
}
