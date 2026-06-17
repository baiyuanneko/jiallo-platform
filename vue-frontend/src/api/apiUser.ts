// 用户相关 API 接口

import request from '@/utils/request'
import type {
  LoginUserVo,
  ExternalLoginVo,
  RegisterUserVo,
  EditUserInfoVo,
  ChangePasswordVo,
  VerifyEmailCodeVo,
  ChangeLoginEmailVo,
  BindEmailVo,
  BindEmailVerifyVo,
  ViewUserLogVo,
  ForgetPasswordVo,
  ResetPasswordVo,
  RevokeSessionVo,
  LogoutVo,
  RLoginResult,
  RTokenResponse,
  RUser,
  RUserRegisterResult,
  RPageUserLog,
  RUpdateAvatarResponse,
  RForgetPasswordResult,
  RListUserSession,
  RListMyUserGroup,
  RCaptchaDto,
  RVoid,
} from '@/types/user'

/**
 * 获取登录验证码
 */
export function getLoginCaptchaApi() {
  return request.get<RCaptchaDto>('/user/getLoginCaptcha')
}

/**
 * 用户登录
 * 返回 TokenResponse 或 EmailVerificationRequired
 */
export function loginApi(data: LoginUserVo) {
  return request.post<RLoginResult>('/user/login', data, {
    skipErrorHandler: true,
  })
}

/**
 * 外部 SSO token 登录
 */
export function externalLoginApi(data: ExternalLoginVo) {
  return request.post<RTokenResponse>('/externalLogin/hajimaruyo', data, {
    skipErrorHandler: true,
  })
}

/**
 * 获取注册验证码
 */
export function getRegisterCaptchaApi() {
  return request.get<RCaptchaDto>('/user/getRegisterCaptcha')
}

/**
 * 用户注册
 */
export function registerApi(data: RegisterUserVo) {
  return request.post<RUserRegisterResult>('/user/register', data, {
    skipErrorHandler: true,
  })
}

/**
 * 刷新 Token
 * 注意：此接口在 request.ts 中直接调用，不经过拦截器
 */
export function refreshTokenApi(refreshToken: string) {
  return request.post<RTokenResponse>('/user/refresh-token', { refreshToken })
}

/**
 * 用户登出
 * 将 Access Token 和 Refresh Token 加入黑名单
 */
export function logoutApi(data: LogoutVo) {
  return request.post<RVoid>('/user/logout', data)
}

/**
 * 获取当前用户信息
 */
export function getUserInfoApi() {
  return request.get<RUser>('/user/info')
}

/**
 * 修改用户信息
 */
export function editUserInfoApi(data: EditUserInfoVo) {
  return request.post<RUser>('/user/editInfo', data)
}

/**
 * 修改密码
 */
export function changePasswordApi(data: ChangePasswordVo) {
  return request.post<RVoid>('/user/changePassword', data)
}

/**
 * 重新发送邮箱验证码（需登录状态）
 */
export function resendEmailCodeApi() {
  return request.post<RVoid>('/user/resendEmailCode')
}

/**
 * 重新发送邮箱验证码（无需登录状态）
 * 用于注册/登录时未获取 JWT 的情况
 */
export function resendEmailCodeWithoutLoginApi(data: LoginUserVo) {
  return request.post<RVoid>('/user/resendEmailCodeWithoutLogin', data)
}

/**
 * 验证邮箱验证码
 * 用于注册/登录时的邮箱验证，验证成功返回 TokenResponse
 */
export function verifyEmailCodeApi(data: VerifyEmailCodeVo) {
  return request.post<RTokenResponse>('/user/verifyEmailCode', data)
}

/**
 * 修改登录邮箱（等待验证状态的用户）
 */
export function changeLoginEmailApi(data: ChangeLoginEmailVo) {
  return request.post<RVoid>('/user/changeLoginEmail', data)
}

/**
 * 绑定邮箱
 * 发送验证码到指定邮箱
 */
export function bindEmailApi(data: BindEmailVo) {
  return request.post<RVoid>('/user/bindEmail', data)
}

/**
 * 验证绑定邮箱
 * 验证邮箱验证码，完成绑定
 */
export function bindEmailVerifyApi(data: BindEmailVerifyVo) {
  return request.post<RVoid>('/user/bindEmailVerify', data)
}

/**
 * 解绑邮箱
 */
export function unbindEmailApi() {
  return request.post<RVoid>('/user/unbindEmail')
}

/**
 * 查看用户日志
 * 分页查询当前用户的操作日志
 */
export function viewUserLogApi(data: ViewUserLogVo) {
  return request.post<RPageUserLog>('/user/viewUserLog', data)
}

/**
 * 上传用户头像
 */
export function uploadAvatarApi(formData: FormData) {
  return request.post<RUpdateAvatarResponse>('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 获取当前用户头像（带 Bearer Token 认证）
 * 返回 Blob 对象，需要转换为 Blob URL 用于 img src
 * @param cacheBust 缓存破坏参数，用于绕过浏览器缓存
 */
export async function getMyAvatarApi(cacheBust?: string | number): Promise<Blob> {
  const url = cacheBust ? `/user/avatar/me?t=${cacheBust}` : '/user/avatar/me'
  const response = await request.get(url, {
    responseType: 'blob',
  })
  return response.data
}

/**
 * 忘记密码
 * 发送密码重置验证码到用户邮箱
 */
export function forgetPasswordApi(data: ForgetPasswordVo) {
  return request.post<RForgetPasswordResult>('/user/forgetPassword', data)
}

/**
 * 重置密码
 * 使用验证码重置密码
 */
export function resetPasswordApi(data: ResetPasswordVo) {
  return request.post<RVoid>('/user/resetPassword', data)
}

/**
 * 获取用户当前会话列表
 * 获取当前登录用户的未失效会话
 */
export function listActiveSessionsApi() {
  return request.get<RListUserSession>('/user/listActiveSessions')
}

/**
 * 获取当前用户所属用户组
 */
export function listMyGroupsApi() {
  return request.get<RListMyUserGroup>('/user/myGroups')
}

/**
 * 注销指定会话
 * 根据会话ID注销指定的会话
 */
export function revokeSessionByIdApi(data: RevokeSessionVo) {
  return request.post<RVoid>('/user/revokeSessionById', data)
}

/**
 * 注销所有会话
 * 注销当前用户的所有会话（包括当前会话）
 */
export function revokeAllSessionsApi() {
  return request.post<RVoid>('/user/revokeAllSessions')
}
