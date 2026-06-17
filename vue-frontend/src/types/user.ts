// 用户 API 相关类型定义

// ==================== 请求参数类型 ====================

export interface LoginUserVo {
  username: string
  password: string
  verifyCodeId?: string
  verifyCode?: string
}

// 登录结果 - 可能是 TokenResponse 或需要邮箱验证
export interface EmailVerificationRequired {
  userId: string
  email: string
  message: 'EMAIL_VERIFICATION_REQUIRED'
}

// 验证码错误
export interface CaptchaError {
  error: 'INCORRECT_CAPTCHA'
}

export type LoginResult = TokenResponse | EmailVerificationRequired

export function isEmailVerificationRequired(
  result: LoginResult,
): result is EmailVerificationRequired {
  return (result as EmailVerificationRequired).message === 'EMAIL_VERIFICATION_REQUIRED'
}

export function isCaptchaError(result: unknown): result is CaptchaError {
  return (result as CaptchaError)?.error === 'INCORRECT_CAPTCHA'
}

export interface RegisterUserVo {
  username: string
  password: string
  email?: string
  verifyCodeId?: string
  verifyCode?: string
}

export interface RefreshTokenVo {
  refreshToken: string
}

export interface ExternalLoginVo {
  ssoToken: string
}

export interface EditUserInfoVo {
  displayName?: string
}

export interface ChangePasswordVo {
  newPassword: string
}

export interface VerifyEmailCodeVo {
  userId: string
  code: string
}

export interface ChangeLoginEmailVo {
  loginInfo: LoginUserVo
  newEmail: string
}

export interface BindEmailVo {
  email: string
}

export interface BindEmailVerifyVo {
  emailCode: string
}

export interface ViewUserLogVo {
  pageNum?: number
  pageSize?: number
}

export interface ForgetPasswordVo {
  username: string
}

export interface ResetPasswordVo {
  userId: string
  code: string
  newPassword: string
}

export interface RevokeSessionVo {
  sessionId: string
}

export interface LogoutVo {
  refreshToken: string
}

// ==================== 响应数据类型 ====================

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface User {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  username: string
  displayName?: string
  passwordUpdatedAt?: string
  email?: string
  emailVerified?: boolean
  mobile?: string
  mobileVerified?: boolean
  roleType: 'USER' | 'ADMIN' | 'VIP_USER' | 'VANILLA'
  banned?: boolean
  avatarUrl?: string
  preferredModelId?: string | null
  preferredModelType?: number | null
}

export interface UserRegisterResult {
  registerResult: string
  user: User
  tokenResponse: TokenResponse
}

export interface UserLog {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  userId: string
  ip?: string
  userAgent?: string
  behaviour:
    | 'SUCCESSFUL_LOGIN'
    | 'FAILED_LOGIN'
    | 'PASSWORD_CHANGE'
    | 'SUCCESSFUL_REGISTER'
    | 'EMAIL_CODE_INVALID'
    | 'SEND_EMAIL_CODE'
  message?: string
}

export interface PageUserLog {
  records: UserLog[]
  total: number
  size: number
  current: number
  pages?: number
}

// ==================== 通用响应结构 ====================

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface UpdateAvatarResponse {
  avatarUrl: string
}

export interface ForgetPasswordResult {
  userId: string
  email: string
}

export interface UserSession {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  userId: string
  ip?: string
  userAgent?: string
  refreshTokenExpireTime?: string
  isCurrentSession?: boolean
}

export interface MyUserGroup {
  groupId: string
  groupName: string
  description?: string
}

// ==================== 验证码相关类型 ====================

export interface CaptchaDto {
  captchaId: string
  captchaBase64: string
}

export type RTokenResponse = ApiResponse<TokenResponse>
export type RLoginResult = ApiResponse<LoginResult>
export type RUser = ApiResponse<User>
export type RUserRegisterResult = ApiResponse<UserRegisterResult>
export type RPageUserLog = ApiResponse<PageUserLog>
export type RUpdateAvatarResponse = ApiResponse<UpdateAvatarResponse>
export type RForgetPasswordResult = ApiResponse<ForgetPasswordResult>
export type RListUserSession = ApiResponse<UserSession[]>
export type RListMyUserGroup = ApiResponse<MyUserGroup[]>
export type RCaptchaDto = ApiResponse<CaptchaDto>
export type RVoid = ApiResponse<void>
