// 管理员 API 相关类型定义

import type { ApiResponse, User, UserLog, UserSession } from './user'
import type { PageResult } from './common'

// ==================== 通用类型 ====================

export type RoleType = 'USER' | 'ADMIN' | 'VIP_USER' | 'VANILLA' | 'AWAIT_EMAIL_VERIFICATION'

export type SsoClientPermissionType =
  | 'UID_ONLY'
  | 'ROLETYPE_ONLY'
  | 'UID_AND_ROLETYPE'
  | 'BASIC_INFO'
  | 'BASIC_INFO_WITH_ROLETYPE'
  | 'BASIC_INFO_WITH_EMAIL'
  | 'BASIC_INFO_WITH_ROLETYPE_AND_EMAIL'

export type { PageResult }

// ==================== 用户管理请求类型 ====================

export interface QueryUsersVo {
  pageNum?: number
  pageSize?: number
  keyword?: string
  roleType?: RoleType
  banned?: boolean
  emailVerified?: boolean
}

export interface CreateUserVo {
  username: string
  displayName?: string
  email?: string
  roleType: RoleType
  sendEmail?: boolean
}

export interface UpdateUserVo {
  userId: string
  displayName?: string
  email?: string
  emailVerified?: boolean
  roleType?: RoleType
}

export interface DeleteUserVo {
  userId: string
}

export interface BanUserVo {
  userId: string
  banned: boolean
}

export interface AdminResetPasswordVo {
  userId: string
  sendEmail?: boolean
}

// ==================== 用户管理响应类型 ====================

export interface CreateUserResponse {
  userId: string
  username: string
  initialPassword: string
  emailSent: boolean
}

export interface AdminResetPasswordResponse {
  newPassword: string
  emailSent: boolean
}

export interface UserDetailResponse {
  user: User
  groups: UserGroupSummary[]
  activeSessionCount: number
  lastLoginTime?: string
  lastLoginIp?: string
  totalLogCount: number
}

// ==================== SSO Client 类型 ====================

export interface SsoClient {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  clientUniqueName: string
  clientName: string
  clientDesc?: string
  clientWebsite?: string
  clientAuthorName?: string
  clientSecret?: string
  clientRedirectUri: string
  clientPermissionType: SsoClientPermissionType
  silentRedirect?: boolean
}

// ==================== SSO Client 管理请求类型 ====================

export interface QuerySsoClientsVo {
  pageNum?: number
  pageSize?: number
  keyword?: string
  permissionType?: SsoClientPermissionType
}

export interface CreateSsoClientVo {
  clientUniqueName: string
  clientName: string
  clientDesc?: string
  clientWebsite?: string
  clientAuthorName?: string
  clientRedirectUri: string
  clientPermissionType: SsoClientPermissionType
  silentRedirect?: boolean
}

export interface UpdateSsoClientVo {
  clientId: string
  clientName?: string
  clientDesc?: string
  clientWebsite?: string
  clientAuthorName?: string
  clientRedirectUri?: string
  clientPermissionType?: SsoClientPermissionType
  silentRedirect?: boolean
}

export interface DeleteSsoClientVo {
  clientId: string
}

export interface ResetSsoClientSecretVo {
  clientId: string
}

export interface UploadSsoClientIconVo {
  clientId: string
  file: File
}

// ==================== SSO Client 管理响应类型 ====================

export interface CreateSsoClientResponse {
  clientId: string
  clientUniqueName: string
  clientSecret: string
}

export interface ResetSsoClientSecretResponse {
  clientSecret: string
}

export interface SsoClientDetailResponse {
  ssoClient: SsoClient
}

export interface UploadSsoClientIconResponse {
  clientId: string
  iconUrl: string
}

// ==================== LLM Provider / Model 类型 ====================

export interface LlmProvider {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  providerName: string
  baseUrl?: string
}

export interface LlmModel {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  providerId: string
  modelName: string
  realModelName?: string
  modelDisplayName?: string
  isVerifiedModel?: boolean
}

export interface QueryLlmProvidersVo {
  pageNum?: number
  pageSize?: number
}

export interface QueryLlmModelsVo {
  providerId: string
  pageNum?: number
  pageSize?: number
}

export interface QueryPagedLlmModelsVo {
  providerId?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface CreateLlmProviderVo {
  providerName: string
  baseUrl?: string
  apiKey?: string
}

export interface UpdateLlmProviderVo {
  id: string
  providerName: string
  baseUrl: string
  apiKey?: string
}

export interface DeleteLlmProviderVo {
  providerId: string
}

export interface CreateLlmModelVo {
  providerId: string
  modelName: string
  realModelName?: string
  modelDisplayName?: string
}

export interface UpdateLlmModelVo {
  id: string
  modelName?: string
  realModelName?: string
  modelDisplayName?: string
  isVerifiedModel?: boolean | null
}

export interface DeleteLlmModelVo {
  modelId: string
}

export interface TestLlmModelVo {
  id: string
}

export interface GetLlmModelAvailableRolesVo {
  modelId: string
}

export interface UpdateLlmModelRolesVo {
  modelId: string
  roleTypes: RoleType[]
}

// ==================== 响应类型别名 ====================

export type RPageUser = ApiResponse<PageResult<User>>
export type RUser = ApiResponse<User>
export type RCreateUserResponse = ApiResponse<CreateUserResponse>
export type RAdminResetPasswordResponse = ApiResponse<AdminResetPasswordResponse>
export type RUserDetailResponse = ApiResponse<UserDetailResponse>
export type RListUserSession = ApiResponse<UserSession[]>
export type RPageUserLog = ApiResponse<PageResult<UserLog>>

export type RPageSsoClient = ApiResponse<PageResult<SsoClient>>
export type RSsoClient = ApiResponse<SsoClient>
export type RCreateSsoClientResponse = ApiResponse<CreateSsoClientResponse>
export type RResetSsoClientSecretResponse = ApiResponse<ResetSsoClientSecretResponse>
export type RSsoClientDetailResponse = ApiResponse<SsoClientDetailResponse>
export type RUploadSsoClientIconResponse = ApiResponse<UploadSsoClientIconResponse>
export type RPageLlmProvider = ApiResponse<PageResult<LlmProvider>>
export type RPageLlmModel = ApiResponse<PageResult<LlmModel>>
export type RTestLlmModelResponse = ApiResponse<string>
export type RListLlmProvider = ApiResponse<LlmProvider[]>
export type RListRoleType = ApiResponse<RoleType[]>

export type RVoid = ApiResponse<void>

// ==================== AgentType 授权类型 ====================

export interface GetAgentTypeAvailableRolesVo {
  agentType: number
}

export interface UpdateAgentTypeRolesVo {
  agentType: number
  roleTypes: RoleType[]
}

export type RAgentTypeAvailableRoles = ApiResponse<RoleType[]>

// ==================== 功能模块授权类型 ====================

export interface GetModuleAvailableRolesVo {
  moduleCode: string
}

export interface UpdateModuleRolesVo {
  moduleCode: string
  roleTypes: RoleType[]
}

export type RModuleAvailableRoles = ApiResponse<RoleType[]>

// ==================== 用户组管理类型 ====================

export interface UserGroup {
  id: string
  isDel?: boolean
  createUser?: string
  updateUser?: string
  updateTime?: string
  createTime?: string
  groupName: string
  description?: string
}

export interface UserGroupSummary {
  groupId: string
  groupName: string
  description?: string
}

export interface GroupMemberUser {
  userId: string
  username: string
  displayName?: string
}

export interface PageUserGroupsVo {
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface CreateUserGroupVo {
  groupName: string
  description?: string
}

export interface UpdateUserGroupVo {
  groupId: string
  groupName?: string
  description?: string
}

export interface DeleteUserGroupVo {
  groupId: string
}

export interface AddMembersToGroupVo {
  groupId: string
  userIds: string[]
}

export interface RemoveMembersFromGroupVo {
  groupId: string
  userIds: string[]
}

export interface GetModelAvailableGroupsVo {
  modelId: string
}

export interface AuthorizeModelToGroupsVo {
  modelId: string
  groupIds: string[]
}

export interface RevokeModelOfGroupsVo {
  modelId: string
  groupIds: string[]
}

export interface GetAgentTypeAvailableGroupsVo {
  agentType: number
}

export interface AuthorizeAgentTypeToGroupsVo {
  agentType: number
  groupIds: string[]
}

export interface RevokeAgentTypeOfGroupsVo {
  agentType: number
  groupIds: string[]
}

export interface GetModuleAvailableGroupsVo {
  moduleCode: string
}

export interface AuthorizeModuleToGroupsVo {
  moduleCode: string
  groupIds: string[]
}

export interface RevokeModuleOfGroupsVo {
  moduleCode: string
  groupIds: string[]
}

export type RPageUserGroup = ApiResponse<PageResult<UserGroup>>
export type RUserGroup = ApiResponse<UserGroup>
export type RListUserGroup = ApiResponse<UserGroup[]>
export type RListGroupMemberUser = ApiResponse<GroupMemberUser[]>
export type RListString = ApiResponse<string[]>

// ==================== 系统配置类型 ====================

export interface SystemConfig {
  id: string
  configKey: string
  configValue: string
  configInstruction: string | null
  createTime: string
  updateTime: string
}

export type RSystemConfigList = ApiResponse<SystemConfig[]>
export type RAllowRegisterSwitch = ApiResponse<null>
export type RDigitalBynPrompt = ApiResponse<string | null>
