import { ref, computed, nextTick, watch } from 'vue'
import { defineStore } from 'pinia'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { h } from 'vue'
import request from '@/utils/request'
import { getSessionsPageApi, getSessionDetailApi, getAvailableModelsListApi, editMessageApi, deleteMessageApi, renameSessionApi, deleteSessionApi, getAvailableAgentTypesApi, submitPyodideToolResultApi, shareSessionApi, unshareSessionApi, reshareSessionApi, stopChatStreamApi } from '@/api/apiChat'
import { createSseConnection, type SseCallbacks } from '@/utils/sse'
import { ui } from '@/utils/ui'
import { useUserStore } from '@/stores/user'
import { usePreferences } from '@/composables/usePreferences'
import { usePyodide } from '@/composables/usePyodide'
import {
  MessageType,
  AgentType,
  DEFAULT_ENABLED_TOOLS as DEFAULT_TOOLS,
} from '@/types/chat'
import type {
  AgenticChatSession,
  AgenticChatMessage,
  RagSearchResultItem,
  SysLlmModel,
  ChatStreamVo,
  ToolCall,
  ToolResponse,
  ToolExecutionRequest,
  ModelType,
} from '@/types/chat'

export interface StreamingToolCall {
  name: string
  id: string
  arguments: string
}

export interface ReasoningDisplayBlock {
  type: 'reasoning'
  content: string
}

export interface TextDisplayBlock {
  type: 'text'
  content: string
}

export interface ToolRoundDisplayBlock {
  type: 'tool-round'
}

export type DisplayBlock = ReasoningDisplayBlock | TextDisplayBlock | ToolRoundDisplayBlock

export interface StreamingToolRound {
  reasoningContent: string
  toolCalls: StreamingToolCall[]
  toolResults: ToolResponse[]
  /** 按流入顺序排列的显示块，用于准确还原 SSE 事件时序 */
  displayBlocks: DisplayBlock[]
  /** 创建此轮次时已输出的 textContent 长度，用于渲染时正确插入位置 */
  textPrefix: number
}

export interface StreamingMessage {
  rounds: StreamingToolRound[]
  textContent: string
  reasoningContent: string
}

export interface ChatTab {
  id: string
  sessionId: string | null
  sessionName: string
  isPreview: boolean
  isLoading: boolean
  messages: AgenticChatMessage[]
  streamingMessage: StreamingMessage | null
  isStreaming: boolean
  abortController: AbortController | null
  streamUniqueKey: string | null
  stopRequested?: boolean
  scrollTop?: number
  enabledTools: string[]
  enabledSkills: string[]
  ragLibraryIds: string[]
  modelId: string
  isShared: boolean
  sharedSessionId: string | null
  sharedSessionCreateTime: string | null
  shareTextContentOnly: boolean | null
}

export const useChatStore = defineStore('chat', () => {
  const userStore = useUserStore()
  const { rememberToolSelection, savedEnabledTools, savedEnabledSkills, rememberAgentType, savedAgentType, effectiveMultiTab } = usePreferences()
  const pyodide = usePyodide()

  function getNewTabTools(): string[] {
    let tools: string[]
    if (rememberToolSelection.value && savedEnabledTools.value !== null) {
      tools = [...savedEnabledTools.value]
    } else {
      tools = [...DEFAULT_TOOLS]
    }
    if (!ragModuleAuthorized.value) {
      tools = tools.filter(t => t !== 'rag_knowledge_base')
    }
    return tools
  }

  function getNewTabSkills(): string[] {
    if (rememberToolSelection.value && savedEnabledSkills.value !== null) {
      return [...savedEnabledSkills.value]
    }
    return []
  }

  const sessions = ref<AgenticChatSession[]>([])
  const sessionsPage = ref(1)
  const sessionsHasMore = ref(true)
  const sessionsLoading = ref(false)

  // Tab system
  const tabs = ref<ChatTab[]>([])
  const activeTabId = ref<string | null>(null)
  let newTabCounter = 0

  // Keep only the active tab when multi-tab is disabled
  watch(
    [() => tabs.value.length, effectiveMultiTab],
    () => {
      if (!effectiveMultiTab.value && tabs.value.length > 1) {
        const active = tabs.value.find((t) => t.id === activeTabId.value)
        tabs.value = active ? [active] : tabs.value.slice(-1)
      }
    },
    { immediate: true },
  )

  const availableModels = ref<SysLlmModel[]>([])
  const currentModelId = ref<string>('')
  const currentAgentType = ref<AgentType>(
    rememberAgentType.value && savedAgentType.value !== null
      ? savedAgentType.value
      : AgentType.CHAT_CLIENT,
  )
  const availableAgentTypes = ref<number[]>([])
  const modelsLoaded = ref(false)
  const reasoningEffort = ref<string | null>(null)

  const searchKeyword = ref('')
const selectedRagChunk = ref<RagSearchResultItem | null>(null)
const selectedRagReferences = ref<{ turnIndex: number; items: RagSearchResultItem[] } | null>(null)
const ragModuleAuthorized = ref(false)
  const sidebarVisible = ref(true)
  const sidebarCollapsed = ref(false)

  const activeStreamCount = computed(() => tabs.value.filter((t) => t.isStreaming).length)

  function getActiveTab(): ChatTab | undefined {
    return tabs.value.find(t => t.id === activeTabId.value)
  }

  // Computed proxies for backward compatibility
  const currentSessionId = computed(() => getActiveTab()?.sessionId ?? null)
  const currentMessages = computed(() => getActiveTab()?.messages ?? [])
  const currentSessionName = computed(() => getActiveTab()?.sessionName ?? '')
  const streamingMessage = computed(() => getActiveTab()?.streamingMessage ?? null)
  const isStreaming = computed(() => getActiveTab()?.isStreaming ?? false)
  const enabledTools = computed(() => getActiveTab()?.enabledTools ?? [])
  const enabledSkills = computed(() => getActiveTab()?.enabledSkills ?? [])
  const ragLibraryIds = computed(() => getActiveTab()?.ragLibraryIds ?? [])

  const filteredSessions = computed(() => {
    if (!searchKeyword.value.trim()) return sessions.value
    const kw = searchKeyword.value.toLowerCase()
    return sessions.value.filter((s) => s.sessionName.toLowerCase().includes(kw))
  })

  async function loadSessions(loadMore = false) {
    if (sessionsLoading.value) return
    if (!loadMore && !sessionsHasMore.value) return
    if (loadMore && !sessionsHasMore.value) return

    sessionsLoading.value = true
    try {
      if (!loadMore) {
        sessionsPage.value = 1
        sessionsHasMore.value = true
      }

      const res = await getSessionsPageApi({
        pageNum: sessionsPage.value,
        pageSize: 20,
      })

      if (res.data.code === 0) {
        const page = res.data.data
        if (loadMore) {
          sessions.value.push(...page.records)
        } else {
          sessions.value = page.records
        }
        sessionsHasMore.value = page.records.length >= 20
        sessionsPage.value++
      }
    } catch {
      ui.error('加载会话列表失败')
    } finally {
      sessionsLoading.value = false
    }
  }

  async function refreshSessions() {
    sessionsPage.value = 1
    sessionsHasMore.value = true
    await loadSessions(false)
  }

  async function loadModels() {
    if (modelsLoaded.value) return
    try {
      const res = await getAvailableModelsListApi()
      if (res.data.code === 0) {
        availableModels.value = res.data.data || []
        if (availableModels.value.length > 0 && !currentModelId.value) {
          const preferredId = userStore.userInfo?.preferredModelId
          if (preferredId && availableModels.value.some((m) => m.id === preferredId)) {
            currentModelId.value = preferredId
          } else {
            currentModelId.value = availableModels.value[0]!.id
          }
        }
        modelsLoaded.value = true
      }
    } catch {
      ui.error('加载模型列表失败')
    }
  }

  async function loadAgentTypes() {
    try {
      const res = await getAvailableAgentTypesApi()
      if (res.data.code === 0) {
        availableAgentTypes.value = res.data.data || []
        // Fallback: if current agentType is not in the available list, pick the first available
        if (
          availableAgentTypes.value.length > 0 &&
          !availableAgentTypes.value.includes(currentAgentType.value)
        ) {
          currentAgentType.value = availableAgentTypes.value[0]! as AgentType
        }
      }
    } catch {
      ui.error('加载 AgentType 列表失败')
    }
  }

  async function loadRagModuleAuth() {
    try {
      const res = await request.post('/ragLibrary/checkAuth')
      ragModuleAuthorized.value = res.data.data === true
    } catch {
      ragModuleAuthorized.value = false
    }
    if (!ragModuleAuthorized.value) {
      tabs.value.forEach((tab) => {
        const idx = tab.enabledTools.indexOf('rag_knowledge_base')
        if (idx >= 0) tab.enabledTools.splice(idx, 1)
      })
    }
  }

  function pushNewTab(sessionId: string | null, sessionName: string, isPreview: boolean): ChatTab {
    const id = `tab-${++newTabCounter}-${Date.now()}`
    const tab: ChatTab = {
      id,
      sessionId,
      sessionName: sessionName || '新对话',
      isPreview,
      isLoading: !!sessionId,
      messages: [],
      streamingMessage: null,
      isStreaming: false,
      abortController: null,
      streamUniqueKey: null,
      enabledTools: getNewTabTools(),
      enabledSkills: getNewTabSkills(),

      ragLibraryIds: [],
      modelId: currentModelId.value || availableModels.value[0]?.id || '',
      isShared: false,
      sharedSessionId: null,
      sharedSessionCreateTime: null,
      shareTextContentOnly: null,
    }
    tabs.value.push(tab)
    activeTabId.value = id
    return tab
  }

  function replaceActiveTab(sessionId: string | null, sessionName: string, isPreview: boolean): ChatTab {
    const active = getActiveTab()
    if (active) {
      if (active.abortController) active.abortController.abort()
    }
    const id = `tab-${++newTabCounter}-${Date.now()}`
    const tab: ChatTab = {
      id,
      sessionId,
      sessionName: sessionName || '新对话',
      isPreview,
      isLoading: !!sessionId,
      messages: [],
      streamingMessage: null,
      isStreaming: false,
      abortController: null,
      streamUniqueKey: null,
      enabledTools: getNewTabTools(),
      enabledSkills: getNewTabSkills(),

      ragLibraryIds: [],
      modelId: currentModelId.value || availableModels.value[0]?.id || '',
      isShared: false,
      sharedSessionId: null,
      sharedSessionCreateTime: null,
      shareTextContentOnly: null,
    }
    if (active) {
      const idx = tabs.value.findIndex(t => t.id === active.id)
      if (idx >= 0) {
        tabs.value.splice(idx, 1, tab)
      } else {
        tabs.value.push(tab)
      }
    } else {
      tabs.value.push(tab)
    }
    activeTabId.value = id
    return tab
  }

  async function loadSessionIntoActiveTab(sessionId: string) {
    sidebarVisible.value = false
    try {
      const res = await getSessionDetailApi(sessionId)
      if (res.data.code === 0) {
        const s = res.data.data
        const tab = getActiveTab()!
        tab.messages = s.messages || []
        tab.sessionName = s.sessionName
        const restoredTools = s.enabledTools?.length ? [...s.enabledTools] : [...DEFAULT_TOOLS]
        tab.enabledTools = !ragModuleAuthorized.value ? restoredTools.filter(t => t !== 'rag_knowledge_base') : restoredTools
        if (s.modelId) {
          tab.modelId = s.modelId
          currentModelId.value = s.modelId
        }
        tab.isShared = s.isShared ?? false
        tab.sharedSessionId = s.sharedSessionId ?? null
        tab.sharedSessionCreateTime = s.sharedSessionCreateTime ?? null
        tab.shareTextContentOnly = s.shareTextContentOnly ?? null
      }
    } catch {
      ui.error('加载会话详情失败')
    } finally {
      const tab = getActiveTab()
      if (tab) tab.isLoading = false
    }
  }

  async function handleSidebarSingleClick(sessionId: string) {
    const existing = tabs.value.find(t => t.sessionId === sessionId)
    if (existing) {
      activeTabId.value = existing.id
      if (existing.modelId) currentModelId.value = existing.modelId
      sidebarVisible.value = false
      return
    }

    if (effectiveMultiTab.value) {
      const active = getActiveTab()
      if (!active || !active.isPreview) {
        pushNewTab(sessionId, '', true)
      } else {
        replaceActiveTab(sessionId, '', true)
      }
    } else {
      replaceActiveTab(sessionId, '', true)
    }
    await loadSessionIntoActiveTab(sessionId)
  }

  async function handleSidebarDoubleClick(sessionId: string) {
    const existing = tabs.value.find(t => t.sessionId === sessionId)
    if (existing) {
      existing.isPreview = false
      activeTabId.value = existing.id
      if (existing.modelId) currentModelId.value = existing.modelId
      sidebarVisible.value = false
      return
    }

    if (effectiveMultiTab.value) {
      const active = getActiveTab()
      if (!active) {
        pushNewTab(sessionId, '', false)
      } else if (active.isPreview) {
        replaceActiveTab(sessionId, '', false)
      } else {
        pushNewTab(sessionId, '', false)
      }
    } else {
      replaceActiveTab(sessionId, '', false)
    }
    await loadSessionIntoActiveTab(sessionId)
  }

  function startNewSession() {
    // Reset to user's preferred default model for new conversations
    const preferredId = userStore.userInfo?.preferredModelId
    if (preferredId && availableModels.value.some((m) => m.id === preferredId)) {
      currentModelId.value = preferredId
    } else if (availableModels.value.length > 0) {
      currentModelId.value = availableModels.value[0]!.id
    }
    if (effectiveMultiTab.value) {
      const id = `new-${++newTabCounter}`
      tabs.value.push({
        id,
        sessionId: null,
        sessionName: '新对话',
        isPreview: true,
        isLoading: false,
        messages: [],
        streamingMessage: null,
        isStreaming: false,
        abortController: null,
      streamUniqueKey: null,
        enabledTools: getNewTabTools(),
      enabledSkills: getNewTabSkills(),
      ragLibraryIds: [],
      modelId: currentModelId.value || availableModels.value[0]?.id || '',
        isShared: false,
        sharedSessionId: null,
        sharedSessionCreateTime: null,
        shareTextContentOnly: null,
      })
      activeTabId.value = id
    } else {
      replaceActiveTab(null, '新对话', true)
    }
    sidebarVisible.value = false
  }

  function switchToTab(tabId: string) {
    if (activeTabId.value === tabId) return
    activeTabId.value = tabId
    const tab = getActiveTab()
    if (tab?.modelId) currentModelId.value = tab.modelId
  }

  function closeTab(tabId: string) {
    const idx = tabs.value.findIndex(t => t.id === tabId)
    if (idx === -1) return
    const tab = tabs.value[idx]!
    if (tab.abortController) tab.abortController.abort()
    tabs.value.splice(idx, 1)
    if (tabs.value.length === 0) {
      startNewSession()
      return
    }
    if (activeTabId.value === tabId) {
      activeTabId.value = (tabs.value[idx] ?? tabs.value[tabs.value.length - 1])!.id
      const remaining = getActiveTab()
      if (remaining?.modelId) currentModelId.value = remaining.modelId
    }
  }

  async function sendMessage(textContent: string, mediaBase64List?: string[]) {
    const tab = getActiveTab()
    if (!tab || tab.isStreaming) return
    if (!textContent.trim() && !mediaBase64List?.length) return

    // Pin the tab when sending a message
    if (tab.isPreview) tab.isPreview = false

    const userMessage: AgenticChatMessage = {
      id: `temp-${Date.now()}`,
      sessionId: tab.sessionId || '',
      messageIndex: tab.messages.length,
      messageType: MessageType.USER,
      textContent,
      mediaContent: mediaBase64List?.length ? JSON.stringify(mediaBase64List) : null,
      mediaType: null,
      promptTokenCount: null,
      completionTokenCount: null,
      reasoningTokenCount: null,
      cachedTokenCount: null,
      toolContent: null,
      reasoningContent: null,
      createUser: '',
      updateUser: '',
      updateTime: new Date().toISOString(),
      createTime: new Date().toISOString(),
      isDel: false,
    }
    tab.messages.push(userMessage)
    const userMsgId = userMessage.id

    tab.streamingMessage = {
      rounds: [],
      textContent: '',
      reasoningContent: '',
    }
    tab.isStreaming = true

    const vo: ChatStreamVo = {
      sessionId: tab.sessionId,
      textContent,
      agentType: currentAgentType.value,
      modelType: 0 as ModelType,
      modelId: tab.modelId || currentModelId.value,
      enabledAgenticTools: tab.enabledTools,
      enabledAgenticSkills: tab.enabledSkills,
      ragLibraryIds: ragLibraryIds.value,
    }

    if (reasoningEffort.value) {
      vo.reasoningEffort = reasoningEffort.value
    }

    if (mediaBase64List?.length) {
      vo.mediaContentBase64List = mediaBase64List
    }

    // Save tool & skill selection for next new tab
    if (rememberToolSelection.value) {
      savedEnabledTools.value = [...tab.enabledTools]
      savedEnabledSkills.value = [...tab.enabledSkills]
    }

    // Save agent type for next session
    if (rememberAgentType.value) {
      savedAgentType.value = currentAgentType.value
    }

    const currentRound = (): StreamingToolRound => {
      const rounds = tab.streamingMessage?.rounds
      if (!rounds || rounds.length === 0) {
        const sm = tab.streamingMessage!
        const textPrefix = sm.textContent.length
        const round: StreamingToolRound = {
          reasoningContent: '',
          toolCalls: [],
          toolResults: [],
          displayBlocks: [],
          textPrefix,
        }
        // 推入本轮之前已到达的文本块，保证事件时序
        if (textPrefix > 0) {
          round.displayBlocks.push({ type: 'text', content: sm.textContent })
        }
        sm.rounds.push(round)
        return round
      }
      const lastRound = rounds[rounds.length - 1]!
      // 当前轮的所有工具调用都已收到对应结果时，后续内容进入新一轮
      if (lastRound.toolCalls.length > 0 && lastRound.toolResults.length >= lastRound.toolCalls.length) {
        const sm = tab.streamingMessage!
        const textPrefix = sm.textContent.length
        const round: StreamingToolRound = {
          reasoningContent: '',
          toolCalls: [],
          toolResults: [],
          displayBlocks: [],
          textPrefix,
        }
        sm.rounds.push(round)
        return round
      }
      return lastRound
    }

    const callbacks: SseCallbacks = {
      onStreamUniqueKey(key: string) {
        tab.streamUniqueKey = key
      },

      onText(chunk) {
        if (tab.streamingMessage) {
          tab.streamingMessage.textContent += chunk
          const rounds = tab.streamingMessage.rounds
          if (rounds.length > 0) {
            const blocks = rounds[rounds.length - 1]!.displayBlocks
            const last = blocks[blocks.length - 1]
            if (last?.type === 'text') {
              last.content += chunk
            } else {
              blocks.push({ type: 'text', content: chunk })
            }
          }
        }
      },

      onReasoning(chunk) {
        if (tab.streamingMessage) {
          const round = currentRound()
          round.reasoningContent += chunk
          const blocks = round.displayBlocks
          const last = blocks[blocks.length - 1]
          if (last?.type === 'reasoning') {
            last.content += chunk
          } else {
            blocks.push({ type: 'reasoning', content: chunk })
          }
        }
      },

      onSessionTitle(sessionId: string, sessionName: string) {
        if (sessionId) {
          if (!tab.sessionId) {
            tab.sessionId = sessionId
          }
          tab.sessionName = sessionName
          const session = sessions.value.find((s) => s.id === sessionId)
          if (session) {
            session.sessionName = sessionName
          }
        }
      },

      onToolCall(data: ToolCall) {
        // pyodide_code_runner is fully managed by onToolExecutionRequest
        if (data.name === 'pyodide_code_runner') return
        if (tab.streamingMessage) {
          const round = currentRound()
          round.toolCalls.push({
            name: data.name,
            id: data.id,
            arguments: data.arguments,
          })
          const blocks = round.displayBlocks
          const last = blocks[blocks.length - 1]
          if (last?.type !== 'tool-round') {
            blocks.push({ type: 'tool-round' })
          }
        }
      },

      onToolResult(data: ToolResponse) {
        // pyodide_code_runner results are handled by onToolExecutionRequest
        if (data.name === 'pyodide_code_runner') return
        if (tab.streamingMessage) {
          const round = currentRound()
          round.toolResults.push(data)
        }
      },

      onDone(sessionId: string, messageId: string, userMessageId: string, promptTokenCount?: number | null, completionTokenCount?: number | null, reasoningTokenCount?: number | null, cachedTokenCount?: number | null) {
        if (tab.streamingMessage) {
          const sm = tab.streamingMessage
          const sid = sessionId || tab.sessionId || ''
          let msgIndex = tab.messages.length

          let allPostToolReasoning = ''
          let orphanText = '' // 无工具轮次或未分配到轮次的文本
          for (const round of sm.rounds) {
            // 从 displayBlocks 提取本轮内容（文本、推理顺序），不再使用 textPrefix 切片
            // 工具调用之前的文本保留在轮次，之后的文本拆到最终回答
            let roundText = ''
            let postToolText = ''
            let preToolReasoning = ''
            let postToolReasoning = ''
            let seenTool = false
            for (const block of round.displayBlocks) {
              if (block.type === 'text') {
                if (seenTool) {
                  postToolText += block.content
                } else {
                  roundText += block.content
                }
              } else if (block.type === 'tool-round') {
                seenTool = true
              } else if (block.type === 'reasoning') {
                if (seenTool) {
                  postToolReasoning += block.content
                } else {
                  preToolReasoning += block.content
                }
              }
            }

            // 工具调用之后的文本合并到最终回答
            allPostToolReasoning += postToolReasoning
            if (postToolText) orphanText += postToolText
            // 跳过无工具调用的轮次（内容合并到最终回答）
            if (round.toolCalls.length === 0) {
              allPostToolReasoning += preToolReasoning
              allPostToolReasoning += postToolReasoning
              if (roundText) orphanText += roundText
              continue
            }

            const assistantToolMsg: AgenticChatMessage = {
              id: `temp-assistant-tool-${Date.now()}-${msgIndex}`,
              sessionId: sid,
              messageIndex: msgIndex++,
              messageType: MessageType.ASSISTANT,
              textContent: roundText || null,
              mediaContent: null,
              mediaType: null,
              promptTokenCount: null,
              completionTokenCount: null,
              reasoningTokenCount: null,
              cachedTokenCount: null,
              toolContent: JSON.stringify(
                round.toolCalls.map((tc) => ({
                  name: tc.name,
                  id: tc.id,
                  arguments: tc.arguments,
                  type: 'function',
                })),
              ),
              reasoningContent: preToolReasoning || null,
              createUser: '',
              updateUser: '',
              updateTime: new Date().toISOString(),
              createTime: new Date().toISOString(),
              isDel: false,
            }
            tab.messages.push(assistantToolMsg)

            const toolResponseMsg: AgenticChatMessage = {
              id: `temp-tool-resp-${Date.now()}-${msgIndex}`,
              sessionId: sid,
              messageIndex: msgIndex++,
              messageType: MessageType.TOOL_RESPONSE,
              textContent: null,
              mediaContent: null,
              mediaType: null,
              promptTokenCount: null,
              completionTokenCount: null,
              reasoningTokenCount: null,
              cachedTokenCount: null,
              toolContent: JSON.stringify(
                round.toolResults.map((tr) => ({
                  name: tr.name,
                  id: tr.id,
                  responseData:
                    typeof tr.result === 'string' ? tr.result : JSON.stringify(tr.result),
                })),
              ),
              reasoningContent: null,
              createUser: '',
              updateUser: '',
              updateTime: new Date().toISOString(),
              createTime: new Date().toISOString(),
              isDel: false,
            }
            tab.messages.push(toolResponseMsg)
          }

          const assistantAnswerMsg: AgenticChatMessage = {
            id: messageId || `temp-assistant-${Date.now()}`,
            sessionId: sid,
            messageIndex: msgIndex,
            messageType: MessageType.ASSISTANT,
            textContent: orphanText || (sm.rounds.length === 0 ? sm.textContent : null),
            mediaContent: null,
            mediaType: null,
            promptTokenCount: promptTokenCount ?? null,
            completionTokenCount: completionTokenCount ?? null,
            reasoningTokenCount: reasoningTokenCount ?? null,
            cachedTokenCount: cachedTokenCount ?? null,
            toolContent: null,
            reasoningContent: allPostToolReasoning || null,
            createUser: '',
            updateUser: '',
            updateTime: new Date().toISOString(),
            createTime: new Date().toISOString(),
            isDel: false,
          }
          // 用户中止时在最后一条助手消息追加灰色标记（blockquote 自带灰色样式）
          if (tab.stopRequested && assistantAnswerMsg.textContent !== null) {
            assistantAnswerMsg.textContent += '\n\n> （用户已中止）'
          }
          tab.messages.push(assistantAnswerMsg)
          tab.stopRequested = false
        }

        // Replace user message temp ID with real ID
        if (userMessageId && userMsgId) {
          const idx = tab.messages.findIndex((m) => m.id === userMsgId)
          if (idx >= 0) {
            tab.messages.splice(idx, 1, { ...tab.messages[idx]!, id: userMessageId })
          }
        }

        if (sessionId && !tab.sessionId) {
          tab.sessionId = sessionId
        }

        tab.streamingMessage = null
        tab.isStreaming = false
        tab.abortController = null
        tab.streamUniqueKey = null

        refreshSessions()
      },

      onError(msg: string) {
        // 防重复：isStreaming 已被置 false 说明已处理过
        if (!tab.isStreaming) return

        // 切到后台再切回导致的网络中断 → 保留已有回复 + 追加中断提示
        if (msg === '__BACKGROUND_INTERRUPT__') {
          const sm = tab.streamingMessage
          if (sm && sm.textContent) {
            const interruptedMsg: AgenticChatMessage = {
              id: `temp-interrupted-${Date.now()}`,
              sessionId: tab.sessionId || '',
              messageIndex: tab.messages.length,
              messageType: MessageType.ASSISTANT,
              textContent: sm.textContent + '\n\n> （流式输出被意外中断）',
              mediaContent: null,
              mediaType: null,
              promptTokenCount: null,
              completionTokenCount: null,
              reasoningTokenCount: null,
              cachedTokenCount: null,
              toolContent: null,
              reasoningContent: null,
              createUser: '',
              updateUser: '',
              updateTime: new Date().toISOString(),
              createTime: new Date().toISOString(),
              isDel: false,
            }
            tab.messages.push(interruptedMsg)
          }
          tab.streamingMessage = null
          tab.isStreaming = false
          tab.abortController = null
          tab.streamUniqueKey = null
          tab.stopRequested = false
          refreshSessions()
          return
        }

        tab.streamingMessage = null
        tab.isStreaming = false
        tab.abortController = null
        tab.streamUniqueKey = null
        // Remove the failed user message
        const msgIdx = tab.messages.findIndex((m) => m.id === userMsgId)
        let failedMedia: string | null = null
        if (msgIdx >= 0) {
          failedMedia = tab.messages[msgIdx]!.mediaContent
          tab.messages.splice(msgIdx, 1)
        }

        // Check if conversation has media（包含刚删除的失败消息）
        const hasMedia = !!failedMedia || tab.messages.some((m) => m.mediaContent)

        // 检测 DeepSeek V4 上下文完整性错误
        const isDeepseekV4Error =
          msg.includes('in the thinking mode must be passed back to the API') &&
          availableModels.value.some(
            (m) =>
              m.id === currentModelId.value &&
              (m.modelName?.toLowerCase().includes('deepseek-v4') ||
                m.realModelName?.toLowerCase().includes('deepseek-v4') ||
                m.modelDisplayName?.toLowerCase().includes('deepseek-v4')),
          )

        Modal.error({
          title: '消息发送失败',
          content: () =>
            h('div', { style: 'white-space: pre-line' }, [
              h('p', { style: 'margin: 0 0 8px' }, msg || '流式响应异常'),
              ...(isDeepseekV4Error
                ? [
                    h('p', {
                      style:
                        'margin: 0 0 8px; padding: 10px 14px; background: #fff1f0; border: 1px solid #ffccc7; border-radius: 6px; color: #cf1322; font-size: 13px; font-weight: 600; line-height: 1.6;',
                    }, [
                      h(ExclamationCircleOutlined, { style: 'margin-right: 6px; color: #cf1322; font-size: 14px;' }),
                      '注意：当前你正在使用 DeepSeek V4 系列模型，官方 API 对上下文完整性要求较为严格。',
                      '当前报错极有可能是最后一条 AI 消息被用户主动中断或因各种原因未成功流出导致的。',
                      '请对会话中最后一条用户消息点击编辑按钮，并进行「编辑并重发」操作，',
                      '以修复对应破损的 AI 消息再继续。如果问题持续出现请联系管理员。',
                    ]),
                  ]
                : []),
              ...(hasMedia
                ? [
                    h('p', {
                      style:
                        'margin: 0 0 8px; padding: 10px 14px; background: #fffbe6; border: 1px solid #ffe58f; border-radius: 6px; color: #ad6800; font-size: 13px; font-weight: 600; line-height: 1.6;',
                    }, [
                      h(ExclamationCircleOutlined, { style: 'margin-right: 6px; color: #faad14; font-size: 14px;' }),
                      '注意：检测到该对话包含多模态内容（如图像），错误原因有可能是模型不支持多模态内容，可尝试切换模型。',
                    ]),
                  ]
                : []),
              h('p', { style: 'margin: 0 0 8px; color: #8c8c8c; font-size: 13px' }, [
                '可尝试重试、切换模型或创建新对话。如此错误持续出现请联系管理员。',
              ]),
            ]),
          okText: '确定',
          centered: true,
        })
      },

      onToolExecutionRequest(data: ToolExecutionRequest) {
        const { requestId, code } = data

        if (tab.streamingMessage) {
          const round = currentRound()
          round.toolCalls.push({
            name: 'pyodide_code_runner',
            id: requestId,
            arguments: JSON.stringify({ code }),
          })
        }

        nextTick().then(() => {
          const delay = pyodide.isInitialized.value ? 100 : 500
          return new Promise<void>((resolve) => setTimeout(resolve, delay))
        }).then(() => {
          return pyodide.executeCode(code)
        }).then((result) => {
          if (tab.streamingMessage) {
            const round = currentRound()
            round.toolResults.push({
              name: 'pyodide_code_runner',
              id: requestId,
              result,
            })
          }
          return submitPyodideToolResultApi({ requestId, ...result })
        }).catch((err: unknown) => {
          const result = {
            stdout: null as string | null,
            stderr: null as string | null,
            error: err instanceof Error ? err.message : '执行失败',
            exitCode: 1 as number | null,
          }
          if (tab.streamingMessage) {
            const round = currentRound()
            round.toolResults.push({
              name: 'pyodide_code_runner',
              id: requestId,
              result,
            })
          }
          return submitPyodideToolResultApi({ requestId, ...result })
        })
      },
    }

    tab.abortController = createSseConnection(vo, callbacks)
  }

  /** 通知服务端停止当前流，并清理客户端状态 */
  function stopStreaming() {
    const tab = getActiveTab()
    if (!tab) return
    tab.stopRequested = true
    // 通知服务端取消 LLM 调用，服务端会持久化当前已流式内容，
    // 并通过 SSE 发送 sessionTitle / done 事件
    if (tab.streamUniqueKey) {
      stopChatStreamApi(tab.streamUniqueKey).catch(() => {})
    }
    // 不中断 SSE 连接，等待 done 事件自然到达
    // onDone 回调会处理内容持久化和状态清理
  }

  async function editAssistantMessage(messageId: string, newText: string) {
    const tab = getActiveTab()
    if (!tab) return
    try {
      await editMessageApi({ messageId, textContent: newText, deleteAfterMsgs: false })
      const msg = tab.messages.find((m) => m.id === messageId)
      if (msg) msg.textContent = newText
    } catch {
      ui.error('编辑消息失败')
    }
  }

  async function editMessageSaveOnly(messageId: string, newText: string) {
    const tab = getActiveTab()
    if (!tab) return
    try {
      await editMessageApi({ messageId, textContent: newText, deleteAfterMsgs: false })
      const msg = tab.messages.find((m) => m.id === messageId)
      if (msg) msg.textContent = newText
    } catch {
      ui.error('编辑消息失败')
    }
  }

  async function editAndResendMessage(messageId: string, newText: string) {
    const tab = getActiveTab()
    if (!tab || tab.isStreaming) return
    try {
      await editMessageApi({ messageId, textContent: newText, deleteAfterMsgs: true })
      const idx = tab.messages.findIndex((m) => m.id === messageId)
      if (idx >= 0) {
        tab.messages.splice(idx)
      }
      sendMessage(newText)
    } catch {
      ui.error('操作失败')
    }
  }

  async function editAndClearAfter(messageId: string, newText: string) {
    const tab = getActiveTab()
    if (!tab || tab.isStreaming) return
    try {
      await editMessageApi({ messageId, textContent: newText, deleteAfterMsgs: true })
      const msg = tab.messages.find((m) => m.id === messageId)
      if (msg) {
        msg.textContent = newText
        tab.messages = tab.messages.filter(
          (m) => m.messageIndex <= msg.messageIndex,
        )
      }
    } catch {
      ui.error('编辑消息失败')
    }
  }

  async function deleteMessage(messageId: string) {
    const tab = getActiveTab()
    if (!tab || tab.isStreaming) return
    try {
      await deleteMessageApi({ messageId })
      const msg = tab.messages.find((m) => m.id === messageId)
      if (msg) {
        tab.messages = tab.messages.filter(
          (m) => m.messageIndex < msg.messageIndex,
        )
      }
    } catch {
      ui.error('删除消息失败')
    }
  }

  async function renameSession(sessionId: string, sessionName: string) {
    try {
      await renameSessionApi({ sessionId, sessionName })
      const session = sessions.value.find((s) => s.id === sessionId)
      if (session) {
        session.sessionName = sessionName
      }
      // Update tab name if it matches
      const tab = tabs.value.find(t => t.sessionId === sessionId)
      if (tab) {
        tab.sessionName = sessionName
      }
    } catch {
      ui.error('重命名会话失败')
    }
  }

  async function deleteSession(sessionId: string) {
    try {
      await deleteSessionApi({ sessionId })
      sessions.value = sessions.value.filter((s) => s.id !== sessionId)
      // Close any open tab for this session
      const tab = tabs.value.find(t => t.sessionId === sessionId)
      if (tab) closeTab(tab.id)
      ui.success('会话已删除')
    } catch {
      ui.error('删除会话失败')
    }
  }

  function toggleTool(toolId: string) {
    const tab = getActiveTab()
    if (!tab) return
    // 禁止选中未授权的 RAG 知识库工具
    if (toolId === 'rag_knowledge_base' && !ragModuleAuthorized.value) {
      return
    }
    const idx = tab.enabledTools.indexOf(toolId)
    if (idx >= 0) {
      tab.enabledTools.splice(idx, 1)
    } else {
      tab.enabledTools.push(toolId)
    }
  }

  function toggleSkill(skillId: string) {
    const tab = getActiveTab()
    if (!tab) return
    const idx = tab.enabledSkills.indexOf(skillId)
    if (idx >= 0) {
      tab.enabledSkills.splice(idx, 1)
    } else {
      tab.enabledSkills.push(skillId)
    }
  }

  function setRagLibraryIds(ids: string[]) {
    const tab = getActiveTab()
    if (tab) tab.ragLibraryIds = ids
  }

  function setActiveModel(modelId: string) {
    currentModelId.value = modelId
    const tab = getActiveTab()
    if (tab) tab.modelId = modelId
  }

  function reorderTabs(fromIndex: number, toIndex: number) {
    const moved = tabs.value.splice(fromIndex, 1)[0]
    if (moved) tabs.value.splice(toIndex, 0, moved)
  }

  function closeOtherTabs() {
    const active = getActiveTab()
    if (!active) return
    tabs.value = [active]
    activeTabId.value = active.id
  }

  function closeAllTabs() {
    // 先清除所有标签，再创建新对话
    for (const t of tabs.value) {
      if (t.abortController) t.abortController.abort()
    }
    tabs.value = []
    activeTabId.value = null
    startNewSession()
  }

  async function shareCurrentSession(shareTextContentOnly: boolean): Promise<string | null> {
    const tab = getActiveTab()
    if (!tab?.sessionId) return null
    try {
      const res = await shareSessionApi({ sessionId: tab.sessionId, shareTextContentOnly })
      if (res.data.code === 0) {
        tab.isShared = true
        tab.sharedSessionId = res.data.data
        tab.sharedSessionCreateTime = new Date().toISOString()
        tab.shareTextContentOnly = shareTextContentOnly
        return `${window.location.origin}/app/agentic-chat-ui/shared/${res.data.data}`
      }
    } catch {
      ui.error('分享失败')
    }
    return null
  }

  async function unshareCurrentSession() {
    const tab = getActiveTab()
    if (!tab?.sessionId) return
    try {
      const res = await unshareSessionApi({ sessionId: tab.sessionId })
      if (res.data.code === 0) {
        tab.isShared = false
        tab.sharedSessionId = null
        tab.sharedSessionCreateTime = null
        tab.shareTextContentOnly = null
        ui.success('已取消分享')
      }
    } catch {
      ui.error('取消分享失败')
    }
  }

  async function reshareCurrentSession() {
    const tab = getActiveTab()
    if (!tab?.sessionId) return
    try {
      const res = await reshareSessionApi({ sessionId: tab.sessionId })
      if (res.data.code === 0) {
        const link = `${window.location.origin}/app/agentic-chat-ui/shared/${res.data.data}`
        await navigator.clipboard.writeText(link)
        ui.success('已同步最新消息到分享会话')
      }
    } catch {
      ui.error('同步失败')
    }
  }

  return {
    sessions,
    sessionsHasMore,
    sessionsLoading,
    tabs,
    activeTabId,
    currentSessionId,
    currentMessages,
    currentSessionName,
    streamingMessage,
    isStreaming,
    enabledTools,
    enabledSkills,
    ragLibraryIds,
    availableModels,
    currentModelId,
    currentAgentType,
    availableAgentTypes,
    modelsLoaded,
    searchKeyword,
    selectedRagChunk,
    selectedRagReferences,
    ragModuleAuthorized,
    sidebarVisible,
    sidebarCollapsed,
    reasoningEffort,
    activeStreamCount,
    filteredSessions,
    getActiveTab,
    loadSessions,
    refreshSessions,
    loadModels,
    loadAgentTypes,
    loadRagModuleAuth,
    startNewSession,
    handleSidebarSingleClick,
    handleSidebarDoubleClick,
    switchToTab,
    closeTab,
    sendMessage,
    stopStreaming,
    editAssistantMessage,
    editMessageSaveOnly,
    editAndResendMessage,
    editAndClearAfter,
    deleteMessage,
    renameSession,
    deleteSession,
    toggleTool,
    toggleSkill,
    setRagLibraryIds,
    setActiveModel,
    reorderTabs,
    closeOtherTabs,
    closeAllTabs,
    shareCurrentSession,
    unshareCurrentSession,
    reshareCurrentSession,
  }
})
