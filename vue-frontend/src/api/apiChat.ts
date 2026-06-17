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
  PyodideToolResultVo,
  ShareSessionVo,
  RShareSession,
  RSharedSessionDetail,
  RSharedSessionsPage,
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
