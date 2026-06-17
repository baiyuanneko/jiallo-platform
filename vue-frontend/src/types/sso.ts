// SSO 登录相关类型定义

import type { ApiResponse } from './user'
import type { SsoClient, SsoClientPermissionType } from './admin'

// ==================== SSO 请求类型 ====================

export interface GetSsoClientInfoVo {
  clientUniqueName: string
}

export interface ConfirmLoginVo {
  clientUniqueName: string
}

// ==================== SSO 响应类型 ====================

export type RSsoClient = ApiResponse<SsoClient>
export type RString = ApiResponse<string>

// ==================== 权限描述映射 ====================

export const PERMISSION_DESCRIPTIONS: Record<SsoClientPermissionType, string[]> = {
  UID_ONLY: ['您的用户 ID'],
  ROLETYPE_ONLY: ['您的账户角色'],
  UID_AND_ROLETYPE: ['您的用户 ID', '账户角色'],
  BASIC_INFO: ['您的用户 ID', '用户名', '显示名称'],
  BASIC_INFO_WITH_ROLETYPE: ['您的用户 ID', '用户名', '显示名称', '账户角色'],
  BASIC_INFO_WITH_EMAIL: ['您的用户 ID', '用户名', '显示名称', '邮箱地址'],
  BASIC_INFO_WITH_ROLETYPE_AND_EMAIL: ['您的用户 ID', '用户名', '显示名称', '账户角色', '邮箱地址'],
}

// 重导出方便使用
export type { SsoClient, SsoClientPermissionType }
