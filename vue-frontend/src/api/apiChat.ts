// Agentic Chat 相关 API 接口

import request from '@/utils/request'
import { getAppConfig } from '@/config/appConfig'
import type {
  SessionsPageVo,
  ModelsPageVo,
  EditMessageVo,
  DeleteMessageVo,
  RenameSessionVo,
  DeleteSessionVo,
  RSessionsPage,
  RSessionDetail,
  RModelsPage,
  RModelsList,
  RAvailableAgentTypes,
  RClawSecurityLevel,
  PyodideToolResultVo,
  ShareSessionVo,
  RShareSession,
  RSharedSessionDetail,
  RSharedSessionsPage,
  ClawToolApprovalVo,
  RClawFileList,
  ClawFileRenameVo,
  ClawFileDeleteVo,
  RInflightStreams,
} from '@/types/chat'
import type { RVoid } from '@/types/user'

/**
 * 分页查询会话列表
 * 按更新时间倒序
 */
export function getSessionsPageApi(data: SessionsPageVo) {
  return request.post<RSessionsPage>('/agenticChatApp/sessions/page', data)
}

/**
 * 获取会话详情（含消息列表）
 * 消息按 messageIndex 升序排列
 */
export function getSessionDetailApi(sessionId: string) {
  return request.get<RSessionDetail>(`/agenticChatApp/sessions/${sessionId}`)
}

/**
 * 查询当前用户可用的系统模型（分页）
 */
export function getAvailableModelsApi(data: ModelsPageVo = { pageNum: 1, pageSize: 100 }) {
  return request.post<RModelsPage>('/llmModel/available/page', data)
}

/**
 * 查询当前用户可用的系统模型列表（全量）
 * 一次性返回全部可用模型，适用于模型选择器
 */
export function getAvailableModelsListApi() {
  return request.get<RModelsList>('/llmModel/available/list')
}

/**
 * 设置默认模型
 * modelId 为空字符串表示清除默认模型
 */
export function setDefaultModelApi(modelId: string, modelType: number = 0) {
  return request.post<RVoid>('/llmModel/setDefaultModel', null, {
    params: { modelId, modelType },
  })
}

/**
 * 获取模型图标 URL
 * 走 sys 表降级链：model 图标 → provider 图标 → 系统默认图标 → 404
 */
export function getModelIconUrl(modelId: string, cacheBust?: string | number): string {
  const baseUrl = getAppConfig().apiBaseUrl
  const url = `${baseUrl}/llmModel/icon/${modelId}?modelType=0`
  return cacheBust ? `${url}&v=${cacheBust}` : url
}

/**
 * 编辑消息
 * deleteAfterMsgs=true 时删除该消息及之后所有消息（用于"重新编辑并发送"）
 */
export function editMessageApi(data: EditMessageVo) {
  return request.post<RVoid>('/agenticChatApp/messages/edit', data)
}

/**
 * 删除消息
 * 删除该消息及之后的所有消息
 */
export function deleteMessageApi(data: DeleteMessageVo) {
  return request.post<RVoid>('/agenticChatApp/messages/delete', data)
}

/**
 * 重命名会话
 */
export function renameSessionApi(data: RenameSessionVo) {
  return request.post<RVoid>('/agenticChatApp/sessions/editName', data)
}

/**
 * 删除会话
 */
export function deleteSessionApi(data: DeleteSessionVo) {
  return request.post<RVoid>('/agenticChatApp/sessions/delete', data)
}

/**
 * 获取当前用户可用的 agentType 列表
 */
export function getAvailableAgentTypesApi() {
  return request.get<RAvailableAgentTypes>('/agenticChatApp/availableAgentTypes')
}

/**
 * 获取当前用户的 Claw-like 安全级别
 * 0=无权限（纯聊天）、1=工作区文件、2=完全访问，生效级别由后端决定
 */
export function getClawSecurityLevelApi() {
  return request.get<RClawSecurityLevel>('/agenticChatApp/clawSecurityLevel')
}

/**
 * 提交 Pyodide 代码执行结果
 */
export function submitPyodideToolResultApi(data: PyodideToolResultVo) {
  return request.post<RVoid>('/agenticChatApp/pyodideToolResult', data)
}

/**
 * 分享会话
 */
export function shareSessionApi(data: ShareSessionVo) {
  return request.post<RShareSession>('/agenticChatApp/sessions/share', data)
}

/**
 * 取消分享
 */
export function unshareSessionApi(data: ShareSessionVo) {
  return request.post<RVoid>('/agenticChatApp/sessions/unshare', data)
}

/**
 * 重新分享（同步最新消息）
 */
export function reshareSessionApi(data: ShareSessionVo) {
  return request.post<RShareSession>('/agenticChatApp/sessions/reshare', data)
}

/**
 * 获取分享的会话详情（公开，无需认证）
 */
export function getSharedSessionDetailApi(sharedSessionId: string) {
  return request.get<RSharedSessionDetail>(`/agenticChatApp/sessions/shared/${sharedSessionId}`)
}

/**
 * 分页查询已分享的会话列表
 */
export function getSharedSessionsPageApi(data: SessionsPageVo) {
  return request.post<RSharedSessionsPage>('/agenticChatApp/sessions/shared/page', data)
}

/** 取消指定聊天流，本轮对话不会被保存到数据库 */
export function stopChatStreamApi(streamUniqueKey: string) {
  return request.post('/agenticChatApp/stopChatStream', { streamUniqueKey })
}

/**
 * 获取当前用户进行中的聊天流列表（断线重连/恢复用）
 */
export function getInflightStreamsApi() {
  return request.get<RInflightStreams>('/agenticChatApp/streams/inflight')
}

// ==================== Claw-like 工具审批 ====================

/**
 * 回传 Claw-like 工具调用的人工审批结果
 */
export function postClawToolApprovalApi(data: ClawToolApprovalVo) {
  return request.post<RVoid>('/agenticChatApp/clawToolApproval', data)
}

// ==================== Claw-like 工作区文件管理 ====================

/**
 * 列出工作区某目录下的文件（单层）
 * path 为工作区相对路径；工作区按用户隔离，sessionId 可选（传入时后端顺带校验会话归属）
 */
export function listClawFilesApi(sessionId: string | undefined, path: string) {
  return request.get<RClawFileList>('/agenticChatApp/clawFiles/list', {
    params: { sessionId, path },
  })
}

/**
 * 下载工作区文件（原始字节）
 */
export function downloadClawFileApi(sessionId: string | undefined, path: string) {
  return request.get<Blob>('/agenticChatApp/clawFiles/download', {
    params: { sessionId, path },
    responseType: 'blob',
  })
}

/**
 * 上传文件到工作区某目录
 */
export function uploadClawFileApi(sessionId: string | undefined, path: string, file: File) {
  const formData = new FormData()
  if (sessionId) formData.append('sessionId', sessionId)
  formData.append('path', path)
  formData.append('file', file)
  return request.post<RVoid>('/agenticChatApp/clawFiles/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 重命名工作区文件/目录
 */
export function renameClawFileApi(data: ClawFileRenameVo) {
  return request.post<RVoid>('/agenticChatApp/clawFiles/rename', data)
}

/**
 * 删除工作区文件/目录（目录递归删除）
 */
export function deleteClawFileApi(data: ClawFileDeleteVo) {
  return request.post<RVoid>('/agenticChatApp/clawFiles/delete', data)
}

// ==================== Claw-like 服务器文件管理（需完全访问级别） ====================

/**
 * 列出服务器某目录下的文件（单层）
 * path 为服务器绝对路径（默认 /）
 */
export function listClawServerFilesApi(path: string) {
  return request.get<RClawFileList>('/agenticChatApp/clawServerFiles/list', {
    params: { path },
  })
}

/**
 * 下载服务器文件（原始字节）
 */
export function downloadClawServerFileApi(path: string) {
  return request.get<Blob>('/agenticChatApp/clawServerFiles/download', {
    params: { path },
    responseType: 'blob',
  })
}

/**
 * 上传文件到服务器某目录
 */
export function uploadClawServerFileApi(path: string, file: File) {
  const formData = new FormData()
  formData.append('path', path)
  formData.append('file', file)
  return request.post<RVoid>('/agenticChatApp/clawServerFiles/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 重命名服务器文件/目录
 */
export function renameClawServerFileApi(data: ClawFileRenameVo) {
  return request.post<RVoid>('/agenticChatApp/clawServerFiles/rename', data)
}

/**
 * 删除服务器文件/目录（目录递归删除）
 */
export function deleteClawServerFileApi(data: ClawFileDeleteVo) {
  return request.post<RVoid>('/agenticChatApp/clawServerFiles/delete', data)
}
