// Agentic Chat API 相关类型定义

import type { ApiResponse } from './user'
import type { PageResult } from './common'

// ==================== 枚举 ====================

export enum MessageType {
  SYSTEM = 0,
  USER = 1,
  ASSISTANT = 2,
  TOOL_RESPONSE = 3,
}

export enum AgentType {
  CHAT_CLIENT = 0,
  REACT_AGENT = 1,
  DIGITAL_BYN = 2,
  CLAW_LIKE = 3,
}

export enum ModelType {
  SYS_MODEL = 0,
  USER_MODEL = 1,
}

export enum MediaType {
  IMAGE = 0,
  AUDIO = 1,
}

/** Claw-like 安全级别：0=无权限（纯聊天）、1=工作区文件、2=完全访问 */
export enum ClawSecurityLevel {
  NONE = 0,
  WORKSPACE = 1,
  FULL = 2,
}

/** Claw-like 审批模式：0=自动审批（LLM 判断）、1=绕过审批、2=人工审批 */
export enum ClawApprovalMode {
  AUTO = 0,
  BYPASS = 1,
  MANUAL = 2,
}

// ==================== 工具内容解析类型 ====================

/** type=2 assistant 消息中 toolContent 里每个工具调用的格式 */
export interface PersistedToolCall {
  name: string
  id: string
  arguments: string
  type?: string
}

/** type=3 toolResponse 消息中 toolContent 里每个结果的格式 */
export interface PersistedToolResult {
  name: string
  id: string
  responseData?: string
  response?: SearchResultItem[]
}

/** 历史消息中 mediaContent JSON 数组的每一项 */
export interface MediaContentItem {
  mediaType: number
  mediaContentUrl: string
}

// ==================== 请求参数类型 ====================

export interface ChatStreamVo {
  sessionId: string | null
  textContent: string
  mediaContentBase64List?: string[]
  agentType: AgentType
  modelType: ModelType
  modelId: string
  enabledAgenticTools: string[]
  enabledAgenticSkills?: string[]
  reasoningEffort?: string
  ragLibraryIds?: string[]
  /** 仅 claw-like 模式使用：用户选择的安全级别（后端钳制为 min(请求值, 授权值)） */
  clawSecurityLevel?: number
  /** 仅 claw-like 模式使用：审批模式 */
  clawApprovalMode?: number
}

export interface SessionsPageVo {
  pageNum: number
  pageSize: number
}

export interface ModelsPageVo {
  pageNum: number
  pageSize: number
}

export interface EditMessageVo {
  messageId: string
  textContent: string
  deleteAfterMsgs: boolean
}

export interface DeleteMessageVo {
  messageId: string
}

export interface RenameSessionVo {
  sessionId: string
  sessionName: string
}

export interface DeleteSessionVo {
  sessionId: string
}

// ==================== 响应数据类型 ====================

export interface AgenticChatSession {
  id: string
  sessionName: string
  modelId: string
  modelType: number
  agentType: number
  enabledTools: string[] | null
  messages: AgenticChatMessage[] | null
  createUser: string
  updateUser: string
  updateTime: string
  createTime: string
  isDel: boolean
  isShared: boolean
  sharedSessionId: string | null
  sharedSessionCreateTime: string | null
  shareTextContentOnly: boolean | null
}

export interface AgenticChatMessage {
  id: string
  sessionId: string
  messageIndex: number
  messageType: MessageType
  textContent: string | null
  mediaContent: string | null
  mediaType: MediaType | null
  promptTokenCount: number | null
  completionTokenCount: number | null
  reasoningTokenCount: number | null
  cachedTokenCount: number | null
  toolContent: string | null
  reasoningContent: string | null
  createUser: string
  updateUser: string
  updateTime: string
  createTime: string
  isDel: boolean
}

export interface ToolCall {
  name: string
  id: string
  arguments: string
}

export interface ToolResponse {
  name: string
  id: string
  result: string | SearchResultItem[] | RagSearchResultItem[] | PyodideExecutionResult
}

export interface SearchResultItem {
  title: string
  url: string
  siteName: string
  snippet: string | null
  summary: string | null
  siteIcon: string | null
  datePublished: string | null
}

export interface RagSearchResultItem {
  libraryName: string
  fileName: string
  chunkIndex: number
  chunkContent: string
  relevance: number
}

export interface SysLlmModel {
  id: string
  providerId: string
  providerName: string
  modelName: string
  realModelName: string
  modelDisplayName: string
  modelIconUrl?: string | null
  isVerifiedModel?: boolean | null
  createUser: string
  updateUser: string
  updateTime: string
  createTime: string
  isDel: boolean
}

// ==================== Pyodide 代码执行相关类型 ====================

/** SSE tool_execution_request 事件数据 */
export interface ToolExecutionRequest {
  requestId: string
  code: string
}

/** Pyodide 执行结果 */
export interface PyodideExecutionResult {
  stdout: string | null
  stderr: string | null
  error: string | null
  exitCode: number | null
}

/** POST /pyodideToolResult 请求体 */
export interface PyodideToolResultVo {
  requestId: string
  stdout: string | null
  stderr: string | null
  error: string | null
  exitCode: number | null
}

// ==================== Claw-like 工具审批相关类型 ====================

/** SSE claw_tool_approval_request 事件数据 */
export interface ClawToolApprovalRequest {
  requestId: string
  name: string
  arguments: string
}

/** POST /clawToolApproval 请求体 */
export interface ClawToolApprovalVo {
  requestId: string
  approved: boolean
}

// ==================== Claw-like 文件管理相关类型 ====================

/** 文件列表条目（工作区/服务器文件 API 返回） */
export interface ClawFileEntry {
  name: string
  isDir: boolean
  size: number
  updateTime: string
}

/** 重命名请求体 */
export interface ClawFileRenameVo {
  sessionId?: string
  path: string
  newName: string
}

/** 删除请求体 */
export interface ClawFileDeleteVo {
  sessionId?: string
  path: string
}

// ==================== SSE 事件数据类型 ====================

export interface SseDoneEvent {
  sessionId: string
  promptTokenCount?: number | null
  completionTokenCount?: number | null
  reasoningTokenCount?: number | null
  cachedTokenCount?: number | null
}

// ==================== 分享相关类型 ====================

/** 分享会话详情（公开接口返回） */
export interface SharedSessionDetail {
  id: string
  sessionName: string
  modelId: string
  modelType: number
  agentType: number
  originalSessionId: string
  shareTextContentOnly: boolean | null
  model: SysLlmModel | null
  createTime: string
  updateTime: string
  messages: AgenticChatMessage[] | null
}

/** 分享/取消分享/重新分享 请求体 */
export interface ShareSessionVo {
  sessionId: string
  shareTextContentOnly?: boolean
}

/** 已分享会话列表项 */
export interface SharedSessionItem {
  id: string
  sessionName: string
  modelId: string
  modelType: number
  agentType: number
  originalSessionId: string
  shareTextContentOnly: boolean | null
  createUser: string
  updateUser: string
  createTime: string
  updateTime: string
  messages: null
}

// ==================== 响应别名 ====================

export type RSessionsPage = ApiResponse<PageResult<AgenticChatSession>>
export type RSessionDetail = ApiResponse<AgenticChatSession>
export type RModelsPage = ApiResponse<PageResult<SysLlmModel>>
export type RModelsList = ApiResponse<SysLlmModel[]>
export type RAvailableAgentTypes = ApiResponse<number[]>
export type RClawSecurityLevel = ApiResponse<number>
export type RClawFileList = ApiResponse<ClawFileEntry[]>
export type RShareSession = ApiResponse<string>
export type RSharedSessionDetail = ApiResponse<SharedSessionDetail>
export type RSharedSessionsPage = ApiResponse<PageResult<SharedSessionItem>>

// ==================== 默认工具配置 ====================

/** 默认启用的工具列表 */
export const DEFAULT_ENABLED_TOOLS = ['bocha_web_search', 'memory_tools', 'web_fetch', 'get_datetime']
