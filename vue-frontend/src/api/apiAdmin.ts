// 管理员相关 API 接口

import request from '@/utils/request'
import { getAppConfig } from '@/config/appConfig'
import type {
  QueryUsersVo,
  CreateUserVo,
  UpdateUserVo,
  DeleteUserVo,
  BanUserVo,
  AdminResetPasswordVo,
  QuerySsoClientsVo,
  CreateSsoClientVo,
  UpdateSsoClientVo,
  DeleteSsoClientVo,
  ResetSsoClientSecretVo,
  RPageUser,
  RUser,
  RCreateUserResponse,
  RAdminResetPasswordResponse,
  RUserDetailResponse,
  RListUserSession,
  RPageUserLog,
  RPageSsoClient,
  RSsoClient,
  RCreateSsoClientResponse,
  RResetSsoClientSecretResponse,
  RSsoClientDetailResponse,
  RUploadSsoClientIconResponse,
  RVoid,
  RSystemConfigList,
  RAllowRegisterSwitch,
  RDigitalBynPrompt,
  QueryLlmProvidersVo,
  QueryLlmModelsVo,
  QueryPagedLlmModelsVo,
  CreateLlmProviderVo,
  UpdateLlmProviderVo,
  DeleteLlmProviderVo,
  CreateLlmModelVo,
  UpdateLlmModelVo,
  DeleteLlmModelVo,
  TestLlmModelVo,
  GetLlmModelAvailableRolesVo,
  UpdateLlmModelRolesVo,
  RPageLlmProvider,
  RPageLlmModel,
  RTestLlmModelResponse,
  RListLlmProvider,
  RListRoleType,
  GetAgentTypeAvailableRolesVo,
  UpdateAgentTypeRolesVo,
  RAgentTypeAvailableRoles,
  GetModuleAvailableRolesVo,
  UpdateModuleRolesVo,
  RModuleAvailableRoles,
  PageUserGroupsVo,
  CreateUserGroupVo,
  UpdateUserGroupVo,
  DeleteUserGroupVo,
  AddMembersToGroupVo,
  RemoveMembersFromGroupVo,
  GetModelAvailableGroupsVo,
  AuthorizeModelToGroupsVo,
  RevokeModelOfGroupsVo,
  GetAgentTypeAvailableGroupsVo,
  AuthorizeAgentTypeToGroupsVo,
  RevokeAgentTypeOfGroupsVo,
  GetModuleAvailableGroupsVo,
  AuthorizeModuleToGroupsVo,
  RevokeModuleOfGroupsVo,
  RPageUserGroup,
  RUserGroup,
  RListUserGroup,
  RListGroupMemberUser,
  RListString,
} from '@/types/admin'

// ==================== 用户管理 API ====================

/**
 * 查询用户列表
 * 分页查询用户列表，支持关键词搜索和多条件筛选
 */
export function adminListUsersApi(data: QueryUsersVo) {
  return request.post<RPageUser>('/admin/users/list', data)
}

/**
 * 创建用户
 * 管理员创建新用户，系统自动生成随机密码
 */
export function adminCreateUserApi(data: CreateUserVo) {
  return request.post<RCreateUserResponse>('/admin/users/create', data)
}

/**
 * 修改用户信息
 * 修改用户基本信息、角色、邮箱验证状态等
 */
export function adminUpdateUserApi(data: UpdateUserVo) {
  return request.post<RUser>('/admin/users/update', data)
}

/**
 * 删除用户
 * 逻辑删除用户（设置 is_del=1），并失效所有 Token
 */
export function adminDeleteUserApi(data: DeleteUserVo) {
  return request.post<RVoid>('/admin/users/delete', data)
}

/**
 * 封禁/解封用户
 * 修改用户封禁状态，封禁时立即失效所有 Token
 */
export function adminBanUserApi(data: BanUserVo) {
  return request.post<RVoid>('/admin/users/ban', data)
}

/**
 * 重置用户密码
 * 强制重置用户密码为随机密码，并失效所有 Token
 */
export function adminResetPasswordApi(data: AdminResetPasswordVo) {
  return request.post<RAdminResetPasswordResponse>('/admin/users/resetPassword', data)
}

/**
 * 查看用户详情
 * 获取用户完整信息，包括会话、日志统计等
 */
export function adminGetUserDetailApi(userId: string) {
  return request.get<RUserDetailResponse>(`/admin/users/${userId}`)
}

/**
 * 查看用户活跃会话
 * 获取指定用户的所有活跃会话列表
 */
export function adminGetUserSessionsApi(userId: string) {
  return request.get<RListUserSession>(`/admin/users/${userId}/sessions`)
}

/**
 * 查看用户操作日志
 * 分页查询指定用户的操作日志
 */
export function adminGetUserLogsApi(userId: string, pageNum: number = 1, pageSize: number = 20) {
  return request.get<RPageUserLog>(`/admin/users/${userId}/logs`, {
    params: { pageNum, pageSize },
  })
}

/**
 * 注销用户所有会话
 * 强制失效指定用户的所有会话
 */
export function adminRevokeUserSessionsApi(userId: string) {
  return request.post<RVoid>(`/admin/users/${userId}/revokeSessions`)
}

// ==================== SSO Client 管理 API ====================

/**
 * 查询 SSO Client 列表
 * 分页查询 SSO Client 列表，支持关键词搜索和权限类型筛选
 */
export function adminListSsoClientsApi(data: QuerySsoClientsVo) {
  return request.post<RPageSsoClient>('/admin/sso-clients/list', data)
}

/**
 * 创建 SSO Client
 * 管理员创建新的 SSO Client，系统自动生成 clientSecret
 */
export function adminCreateSsoClientApi(data: CreateSsoClientVo) {
  return request.post<RCreateSsoClientResponse>('/admin/sso-clients/create', data)
}

/**
 * 修改 SSO Client 信息
 * 修改 SSO Client 的基本信息、权限类型等
 */
export function adminUpdateSsoClientApi(data: UpdateSsoClientVo) {
  return request.post<RSsoClient>('/admin/sso-clients/update', data)
}

/**
 * 删除 SSO Client
 * 逻辑删除 SSO Client（设置 is_del=1）
 */
export function adminDeleteSsoClientApi(data: DeleteSsoClientVo) {
  return request.post<RVoid>('/admin/sso-clients/delete', data)
}

/**
 * 重置 SSO Client 密钥
 * 重置 SSO Client 的 clientSecret 为新的随机值
 */
export function adminResetSsoClientSecretApi(data: ResetSsoClientSecretVo) {
  return request.post<RResetSsoClientSecretResponse>('/admin/sso-clients/resetSecret', data)
}

/**
 * 查看 SSO Client 详情
 * 获取 SSO Client 完整信息
 */
export function adminGetSsoClientDetailApi(clientId: string) {
  return request.get<RSsoClientDetailResponse>(`/admin/sso-clients/${clientId}`)
}

/**
 * 上传 SSO Client 图标
 * 管理员上传客户端图标，支持 jpg/jpeg/png/gif/webp 格式，最大 2MB
 */
export function adminUploadSsoClientIconApi(clientId: string, file: File) {
  const formData = new FormData()
  formData.append('clientId', clientId)
  formData.append('file', file)

  return request.post<RUploadSsoClientIconResponse>('/admin/sso-clients/uploadIcon', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// ==================== LLM Provider / Model 管理 API ====================

/**
 * 查询全部 LLM Provider
 * 获取系统中的全部模型供应商
 */
export function adminListAllLlmProvidersApi() {
  return request.post<RListLlmProvider>('/admin/sys-llm-providers/all')
}

/**
 * 查询 LLM Provider 列表
 * 分页查询系统中的模型供应商
 */
export function adminListLlmProvidersApi(data: QueryLlmProvidersVo) {
  return request.post<RPageLlmProvider>('/admin/sys-llm-providers/list', data)
}

/**
 * 创建 LLM Provider
 * 新增模型供应商
 */
export function adminCreateLlmProviderApi(data: CreateLlmProviderVo) {
  return request.post<RVoid>('/admin/sys-llm-providers/add', data)
}

/**
 * 更新 LLM Provider
 * 修改模型供应商信息
 */
export function adminUpdateLlmProviderApi(data: UpdateLlmProviderVo) {
  return request.post<RVoid>('/admin/sys-llm-providers/update', data)
}

/**
 * 删除 LLM Provider
 * 逻辑删除供应商及其下属模型
 */
export function adminDeleteLlmProviderApi(data: DeleteLlmProviderVo) {
  return request.post<RVoid>('/admin/sys-llm-providers/delete', data)
}

/**
 * 获取供应商图标 URL
 * 降级链：provider 图标 → 系统默认图标 → 404
 */
export function getProviderIconUrl(providerId: string): string {
  const baseUrl = getAppConfig().apiBaseUrl
  return `${baseUrl}/admin/sys-llm-providers/icon/${providerId}`
}

/**
 * 上传供应商图标
 */
export function adminUploadProviderIconApi(providerId: string, file: File) {
  const formData = new FormData()
  formData.append('providerId', providerId)
  formData.append('file', file)
  return request.post<RVoid>('/admin/sys-llm-providers/uploadIcon', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 分页查询 LLM Model 列表
 * 支持按 providerId 和关键词筛选
 */
export function adminListLlmModelsApi(data: QueryPagedLlmModelsVo) {
  return request.post<RPageLlmModel>('/admin/sys-llm-models/list', data)
}

/**
 * 查询指定 Provider 下的 LLM Model 列表
 * 分页查询某个供应商下的模型
 */
export function adminListLlmModelsByProviderApi(data: QueryLlmModelsVo) {
  return request.post<RPageLlmModel>('/admin/sys-llm-models/listByProviderId', data)
}

/**
 * 创建 LLM Model
 * 为供应商新增模型
 */
export function adminCreateLlmModelApi(data: CreateLlmModelVo) {
  return request.post<RVoid>('/admin/sys-llm-models/add', data)
}

/**
 * 更新 LLM Model
 * 修改模型信息
 */
export function adminUpdateLlmModelApi(data: UpdateLlmModelVo) {
  return request.post<RVoid>('/admin/sys-llm-models/update', data)
}

/**
 * 删除 LLM Model
 * 逻辑删除模型
 */
export function adminDeleteLlmModelApi(data: DeleteLlmModelVo) {
  return request.post<RVoid>('/admin/sys-llm-models/delete', data)
}

/**
 * 上传模型图标
 */
export function adminUploadModelIconApi(modelId: string, file: File) {
  const formData = new FormData()
  formData.append('modelId', modelId)
  formData.append('file', file)
  return request.post<RVoid>('/admin/sys-llm-models/uploadIcon', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 测试 LLM Model
 * 调用模型并返回测试回答
 */
export function adminTestLlmModelApi(data: TestLlmModelVo) {
  return request.post<RTestLlmModelResponse>('/admin/sys-llm-models/test', data)
}

/**
 * 查询模型当前授权角色
 * 获取指定模型的可用角色列表
 */
export function adminGetLlmModelAvailableRolesApi(data: GetLlmModelAvailableRolesVo) {
  return request.post<RListRoleType>('/admin/sys-llm-models/available-roles', data)
}

/**
 * 授权模型到角色
 * 支持一次授权多个角色
 */
export function adminAuthorizeLlmModelRolesApi(data: UpdateLlmModelRolesVo) {
  return request.post<RVoid>('/admin/sys-llm-models/authorize-roles', data)
}

/**
 * 取消模型角色授权
 * 支持一次取消多个角色
 */
export function adminRevokeLlmModelRolesApi(data: UpdateLlmModelRolesVo) {
  return request.post<RVoid>('/admin/sys-llm-models/revoke-roles', data)
}

// ==================== 系统配置 API ====================

/**
 * 获取所有系统配置
 * 获取系统配置列表
 */
export function adminGetAllConfigApi() {
  return request.get<RSystemConfigList>('/admin/config/getAll')
}

/**
 * 切换是否允许注册
 * 开启或关闭用户注册功能
 */
export function adminAllowRegisterSwitchApi(allowRegister: boolean) {
  return request.post<RAllowRegisterSwitch>('/admin/config/allowRegisterSwitch', null, {
    params: { allowRegister },
  })
}

/**
 * 设置简单任务模型（用于自动生成会话标题等）
 * 传入空字符串表示禁用
 */
export function adminSimpleTaskModelSwitchApi(modelId: string) {
  return request.post<RVoid>('/admin/config/simpleTaskModelSwitch', null, {
    params: { modelId },
  })
}

/**
 * 获取 byn 数字分身系统提示词
 */
export function adminGetDigitalBynPromptApi() {
  return request.get<RDigitalBynPrompt>('/admin/config/digitalBynSystemPrompt')
}

/**
 * 设置 byn 数字分身系统提示词
 * 传空字符串表示清除提示词
 */
export function adminSetDigitalBynPromptApi(prompt: string) {
  return request.post<RVoid>('/admin/config/digitalBynSystemPrompt', { prompt })
}

// ==================== AgentType 授权管理 API ====================

/**
 * 查询 AgentType 当前授权角色
 */
export function adminGetAgentTypeAvailableRolesApi(data: GetAgentTypeAvailableRolesVo) {
  return request.post<RAgentTypeAvailableRoles>('/admin/agent-type-availability/available-roles', data)
}

/**
 * 授权 AgentType 到角色
 */
export function adminAuthorizeAgentTypeRolesApi(data: UpdateAgentTypeRolesVo) {
  return request.post<RVoid>('/admin/agent-type-availability/authorize-roles', data)
}

/**
 * 取消 AgentType 角色授权
 */
export function adminRevokeAgentTypeRolesApi(data: UpdateAgentTypeRolesVo) {
  return request.post<RVoid>('/admin/agent-type-availability/revoke-roles', data)
}

// ==================== 功能模块授权 API ====================

/**
 * 查询功能模块当前授权角色
 */
export function adminGetModuleAvailableRolesApi(data: GetModuleAvailableRolesVo) {
  return request.post<RModuleAvailableRoles>('/admin/feature-module-availability/available-roles', data)
}

/**
 * 授权功能模块到角色
 */
export function adminAuthorizeModuleRolesApi(data: UpdateModuleRolesVo) {
  return request.post<RVoid>('/admin/feature-module-availability/authorize-roles', data)
}

/**
 * 取消功能模块角色授权
 */
export function adminRevokeModuleRolesApi(data: UpdateModuleRolesVo) {
  return request.post<RVoid>('/admin/feature-module-availability/revoke-roles', data)
}

// ==================== 用户组管理 API ====================

/**
 * 分页查询用户组
 */
export function adminPageUserGroupsApi(data: PageUserGroupsVo) {
  return request.post<RPageUserGroup>('/admin/user-groups/list', data)
}

/**
 * 查询全部用户组
 */
export function adminListAllUserGroupsApi() {
  return request.get<RListUserGroup>('/admin/user-groups/all')
}

/**
 * 创建用户组
 */
export function adminCreateUserGroupApi(data: CreateUserGroupVo) {
  return request.post<RUserGroup>('/admin/user-groups/create', data)
}

/**
 * 修改用户组
 */
export function adminUpdateUserGroupApi(data: UpdateUserGroupVo) {
  return request.post<RUserGroup>('/admin/user-groups/update', data)
}

/**
 * 删除用户组
 */
export function adminDeleteUserGroupApi(data: DeleteUserGroupVo) {
  return request.post<RVoid>('/admin/user-groups/delete', data)
}

/**
 * 查询用户组成员
 */
export function adminGetGroupMembersApi(groupId: string) {
  return request.post<RListGroupMemberUser>('/admin/user-groups/members', null, {
    params: { groupId },
  })
}

/**
 * 添加用户组成员
 */
export function adminAddMembersToGroupApi(data: AddMembersToGroupVo) {
  return request.post<RVoid>('/admin/user-groups/addMembers', data)
}

/**
 * 移除用户组成员
 */
export function adminRemoveMembersFromGroupApi(data: RemoveMembersFromGroupVo) {
  return request.post<RVoid>('/admin/user-groups/removeMembers', data)
}

// ==================== 模型-用户组授权 API ====================

/**
 * 查询模型可用用户组
 */
export function adminGetModelAvailableGroupsApi(data: GetModelAvailableGroupsVo) {
  return request.post<RListString>('/admin/sys-llm-models/available-groups', data)
}

/**
 * 授权模型到用户组
 */
export function adminAuthorizeModelToGroupsApi(data: AuthorizeModelToGroupsVo) {
  return request.post<RVoid>('/admin/sys-llm-models/authorize-groups', data)
}

/**
 * 取消模型用户组授权
 */
export function adminRevokeModelOfGroupsApi(data: RevokeModelOfGroupsVo) {
  return request.post<RVoid>('/admin/sys-llm-models/revoke-groups', data)
}

// ==================== AgentType-用户组授权 API ====================

/**
 * 查询 AgentType 可用用户组
 */
export function adminGetAgentTypeAvailableGroupsApi(data: GetAgentTypeAvailableGroupsVo) {
  return request.post<RListString>('/admin/agent-type-availability/available-groups', data)
}

/**
 * 授权 AgentType 到用户组
 */
export function adminAuthorizeAgentTypeToGroupsApi(data: AuthorizeAgentTypeToGroupsVo) {
  return request.post<RVoid>('/admin/agent-type-availability/authorize-groups', data)
}

/**
 * 取消 AgentType 用户组授权
 */
export function adminRevokeAgentTypeOfGroupsApi(data: RevokeAgentTypeOfGroupsVo) {
  return request.post<RVoid>('/admin/agent-type-availability/revoke-groups', data)
}

// ==================== 功能模块-用户组授权 API ====================

/**
 * 查询功能模块可用用户组
 */
export function adminGetModuleAvailableGroupsApi(data: GetModuleAvailableGroupsVo) {
  return request.post<RListString>('/admin/feature-module-availability/available-groups', data)
}

/**
 * 授权功能模块到用户组
 */
export function adminAuthorizeModuleToGroupsApi(data: AuthorizeModuleToGroupsVo) {
  return request.post<RVoid>('/admin/feature-module-availability/authorize-groups', data)
}

/**
 * 取消功能模块用户组授权
 */
export function adminRevokeModuleOfGroupsApi(data: RevokeModuleOfGroupsVo) {
  return request.post<RVoid>('/admin/feature-module-availability/revoke-groups', data)
}
