<script setup lang="ts">
import { ref, computed, inject, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { UserOutlined, RobotOutlined, EditOutlined, DeleteOutlined, CopyOutlined, LoadingOutlined, ArrowDownOutlined, BulbOutlined } from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { useAutoScroll } from '@/composables/useAutoScroll'
import { usePreferences } from '@/composables/usePreferences'
import { vIncrementalMd } from '@/directives/incrementalMd'
import { useQuoteReply } from '@/composables/useQuoteReply'
import { usePyodide } from '@/composables/usePyodide'
import { renderMarkdown } from '@/utils/markdown'
import { expandedDetailViews } from '@/stores/blockState'
import { getModelIconUrl } from '@/api/apiChat'
import { MessageType } from '@/types/chat'
import type {
  AgenticChatMessage,
  PersistedToolCall,
  PersistedToolResult,
  MediaContentItem,
  RagSearchResultItem,
} from '@/types/chat'
import type { AppConfig } from '@/config/appConfig'
import { appConfigKey } from '@/config/appConfig'
import ReasoningBlock from './ReasoningBlock.vue'
import ToolCallBlock from './ToolCallBlock.vue'

const chatStore = useChatStore()
const userStore = useUserStore()
const { showTokenCount, showMessageTime, wideMode, showQuoteButton } = usePreferences()
const { startQuote } = useQuoteReply()
const pyodide = usePyodide()

const { simplifiedProcess } = usePreferences()

/** 流式输出中最后一个非文本显示块 */
const lastStreamingNonTextBlock = computed<{ type: 'reasoning' | 'tool-round'; content?: string; toolCalls?: any[]; toolResults?: any[]; _roundIdx?: number; _blockIdx?: number } | null>(() => {
  const sm = chatStore.streamingMessage
  if (!sm || !sm.rounds.length) return null
  const rounds = sm.rounds
  for (let r = rounds.length - 1; r >= 0; r--) {
    const blocks = rounds[r]!.displayBlocks
    for (let b = blocks.length - 1; b >= 0; b--) {
      const block = blocks[b]!
      if (block.type === 'reasoning') return { type: 'reasoning', content: block.content, _roundIdx: r, _blockIdx: b }
      if (block.type === 'tool-round') {
        return { type: 'tool-round', toolCalls: rounds[r]!.toolCalls, toolResults: rounds[r]!.toolResults, _roundIdx: r, _blockIdx: b }
      }
    }
  }
  return null
})

/**
 * 上一个已完成的思考内容。
 * 当最后一轮非文本块变为 tool-round 或文本开始时，保存当时的 reasoning 内容；
 * 当新一轮 reasoning 开始时，使用这个保存的值（不显示空白）。
 */
const savedReasoningContent = ref<string | null>(null)

// 监听最后非文本块变化：reasoning 被取代时保存完成内容
watch(() => lastStreamingNonTextBlock.value, (cur, prev) => {
  if (prev?.type === 'reasoning') {
    const isNewRound = cur?.type !== 'reasoning' || cur?._roundIdx !== prev._roundIdx || cur?._blockIdx !== prev._blockIdx
    if (isNewRound) {
      savedReasoningContent.value = prev.content ?? null
    }
  }
})

const streamingDetailExpanded = ref(false)

/** 计算 assistant turn 的工具调用和思考过程次数摘要 */
function getTurnRagRefItems(turn: ChatTurn): RagSearchResultItem[] {
  if (!turn.assistantTurn) return []
  const seen = new Set<string>()
  const items: RagSearchResultItem[] = []
  for (const round of turn.assistantTurn.toolRounds) {
    for (const tr of round.toolResults) {
      if (tr.name === 'searchRagKnowledgeBase') {
        const raw = tr.responseData || tr.response
        if (raw) {
          try {
            const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
            if (Array.isArray(parsed)) {
              for (const item of parsed) {
                const key = `${item.libraryName}|${item.fileName}|${item.chunkIndex}`
                if (!seen.has(key)) {
                  seen.add(key)
                  items.push(item)
                }
              }
            }
          } catch {}
        }
      }
    }
  }
  return items
}

function getTurnRagRefCount(turn: ChatTurn): number {
  return getTurnRagRefItems(turn).length
}

function showRagReferences(turnIndex: number, turn: ChatTurn) {
  chatStore.selectedRagReferences = { turnIndex, items: getTurnRagRefItems(turn) }
}

function turnProcessSummary(turn: ChatTurn): { toolCalls: number; reasoningBlocks: number } {
  const t = turn.assistantTurn
  if (!t) return { toolCalls: 0, reasoningBlocks: 0 }
  let toolCalls = 0
  let reasoningBlocks = 0
  for (const round of t.toolRounds) {
    toolCalls += round.toolCalls.length
    if (round.preToolReasoningContent) reasoningBlocks++
    if (round.reasoningContent) reasoningBlocks++
  }
  if (t.reasoningContent) reasoningBlocks++
  return { toolCalls, reasoningBlocks }
}

function isDetailExpanded(turn: ChatTurn): boolean {
  const firstMsgId = turn.assistantTurn?.firstMessageId
  if (!firstMsgId) return true
  return expandedDetailViews.get(firstMsgId) ?? false
}
function expandDetail(turn: ChatTurn) {
  const firstMsgId = turn.assistantTurn?.firstMessageId
  if (firstMsgId) expandedDetailViews.set(firstMsgId, true)
}
function collapseDetail(turn: ChatTurn) {
  const firstMsgId = turn.assistantTurn?.firstMessageId
  if (firstMsgId) expandedDetailViews.delete(firstMsgId)
}

/** 简化模式下，最后一个非文本块之前的文本（按 textPrefix 切分） */
const streamingTextBeforeBlock = computed(() => {
  const sm = chatStore.streamingMessage
  const block = lastStreamingNonTextBlock.value
  if (!sm || !block || block._roundIdx == null) return sm?.textContent ?? ''
  const roundIdx = block._roundIdx
  const textPrefix = sm.rounds[roundIdx]?.textPrefix ?? 0
  return sm.textContent.slice(0, textPrefix)
})
const streamingTextAfterBlock = computed(() => {
  const sm = chatStore.streamingMessage
  const block = lastStreamingNonTextBlock.value
  if (!sm || !block || block._roundIdx == null) return ''
  const roundIdx = block._roundIdx
  const textPrefix = sm.rounds[roundIdx]?.textPrefix ?? 0
  return sm.textContent.slice(textPrefix)
})

/** 流式消息中的工具调用和思考过程总次数 */
const streamingProcessSummary = computed(() => {
  const sm = chatStore.streamingMessage
  if (!sm) return { toolCalls: 0, reasoningBlocks: 0 }
  let toolCalls = 0
  let reasoningBlocks = 0
  for (const round of sm.rounds) {
    toolCalls += round.toolCalls.length
    for (const block of round.displayBlocks) {
      if (block.type === 'reasoning') reasoningBlocks++
    }
  }
  return { toolCalls, reasoningBlocks }
})

const workspaceBanner = computed(() => {
  const name = pyodide.workspaceName.value
  if (!name) return ''
  return `已挂载本机真实目录 ${name}，LLM 将有权阅读和编辑此目录中的内容，请注意安全风险，勿挂载敏感目录或系统目录。`
})
const appConfig = inject(appConfigKey)!
const containerRef = ref<HTMLElement | null>(null)

const { onScroll, watchForAutoScroll, userScrolledUp, scrollToBottom, resetAutoScroll } = useAutoScroll({ containerRef })

const messageCount = computed(() => chatStore.currentMessages.length)
// 连接所有流式内容的变化，任何一个字段更新都会触发 computed 重算从而驱动自动滚动
const streamingUpdateKey = computed(() => {
  const sm = chatStore.streamingMessage
  if (!sm) return ''
  const roundKeys = sm.rounds
    .map((r) => `${r.reasoningContent}|${r.toolCalls.length}|${r.toolResults.length}`)
    .join(';')
  return `${sm.textContent}|${sm.reasoningContent}|${roundKeys}`
})

// 流式内容更新时自动滚动到底部（仅在用户未主动上滚时）
watchForAutoScroll([streamingUpdateKey])

const showStreaming = computed(() => !!chatStore.streamingMessage)

// 流式完成时：保存用户滚动位置，在 Vue DOM 更新 + 浏览器渲染完成后恢复。
// 不操作 overflowY —— 避免可能的 CSS 副作用（scrollbar-gutter 不可靠）；
// double rAF：第一帧在 paint 前恢复位置，第二帧在 paint 后再次确认位置未被覆盖。
let restoreScrollAfterUpdate = 0
watch(showStreaming, (val, oldVal) => {
  if (val) {
    restoreScrollAfterUpdate = 0
  } else if (oldVal === true && userScrolledUp.value) {
    restoreScrollAfterUpdate = containerRef.value?.scrollTop ?? 0
    nextTick(() => {
      requestAnimationFrame(() => {
        if (containerRef.value && userScrolledUp.value) {
          containerRef.value.scrollTop = restoreScrollAfterUpdate
        }
        // paint 后二次确认：浏览器可能在 paint 期间因布局变化再次调整 scrollTop
        requestAnimationFrame(() => {
          if (containerRef.value && userScrolledUp.value) {
            containerRef.value.scrollTop = restoreScrollAfterUpdate
            restoreScrollAfterUpdate = 0
          }
        })
      })
    })
  }
})

const displayTurns = computed<ChatTurn[]>(() => {
  const turns = [...chatTurns.value]
  if (showStreaming.value) {
    turns.push({ type: 'assistant', id: '__streaming__' })
  }
  return turns
})


// 用户发送消息时立即滚动到底部并恢复自动滚屏
watch(messageCount, (newCount, oldCount) => {
  if (newCount > oldCount) {
    const msgs = chatStore.currentMessages
    const lastMsg = msgs[msgs.length - 1]
    if (lastMsg?.messageType === MessageType.USER) {
      resetAutoScroll()
    }
  }
})

// ===== 滚动位置记忆 =====

/** 保存当前 tab 的滚动位置 */
function saveScrollTop() {
  const tab = chatStore.getActiveTab()
  const el = containerRef.value
  if (tab && el) tab.scrollTop = el.scrollTop
}

/** 恢复指定 tab 的滚动位置，若没有记忆则滚动到底部 */
function restoreScrollTop(tabId?: string) {
  const tab = tabId
    ? chatStore.tabs.find((t) => t.id === tabId)
    : chatStore.getActiveTab()
  const el = containerRef.value
  if (!el) return
  if (tab?.scrollTop != null) {
    requestAnimationFrame(() => { el.scrollTop = tab.scrollTop! })
  } else {
    requestAnimationFrame(() => { el.scrollTop = el.scrollHeight })
  }
}

// 监听 tab 切换 → 保存旧 tab 位置，恢复新 tab 位置
watch(
  () => chatStore.activeTabId,
  (newTab, oldTab) => {
    if (oldTab) saveScrollTop()
    if (newTab) restoreScrollTop(newTab)
  },
)

interface ToolRound {
  /** 本轮工具调用期间的推理内容（工具调用之后产生的） */
  reasoningContent: string | null
  /** 工具调用之前的推理内容（解释为什么要调用工具） */
  preToolReasoningContent: string | null
  toolCalls: PersistedToolCall[]
  toolResults: PersistedToolResult[]
  /** 此轮工具调用之前的文本内容，来自同一条同时含 text 和 tool 的 ASSISTANT 消息 */
  textContent?: string | null
}

interface AssistantTurn {
  id: string
  firstMessageId: string
  textMessageId: string | null
  reasoningContent: string | null
  toolRounds: ToolRound[]
  textContent: string | null
  promptTokenCount: number | null
  completionTokenCount: number | null
  reasoningTokenCount: number | null
  cachedTokenCount: number | null
  createTime: string
}

interface ChatTurn {
  type: 'user' | 'assistant'
  id: string
  userMessage?: AgenticChatMessage
  assistantTurn?: AssistantTurn
}

function parseJsonArray<T>(raw: string | null): T[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed as T[]
    return [parsed] as T[]
  } catch {
    return []
  }
}

function resolveMediaUrls(raw: string | null): string[] {
  if (!raw) return []
  // data: URL — 用户刚上传的临时消息
  if (raw.startsWith('data:')) {
    return [raw]
  }
  // JSON 数组格式
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      if (parsed.length === 0) return []
      // base64 字符串数组 — 临时消息
      if (typeof parsed[0] === 'string') return parsed as string[]
      // MediaContentItem 数组 — 历史消息，需通过 getMessageMedia 接口获取
      const userId = userStore.userInfo?.id
      if (!userId) return []
      return (parsed as MediaContentItem[]).map((item) => {
        const filename = item.mediaContentUrl.split('/').pop()!
        return `${appConfig.apiBaseUrl}/agenticChatApp/getMessageMedia?userId=${userId}&mediaFilename=${encodeURIComponent(filename)}`
      })
    }
  } catch {
    // 纯 base64（旧格式）
    return [`data:image/png;base64,${raw}`]
  }
  return []
}

const modelIconUrl = computed(() => {
  if (!chatStore.currentModelId) return ''
  const model = chatStore.availableModels.find((m) => m.id === chatStore.currentModelId)
  return getModelIconUrl(chatStore.currentModelId, model?.updateTime)
})

const modelIconFailed = ref(false)

watch(modelIconUrl, () => {
  modelIconFailed.value = false
})

const chatTurns = computed<ChatTurn[]>(() => {
  const messages = chatStore.currentMessages
  const turns: ChatTurn[] = []
  let i = 0

  while (i < messages.length) {
    const msg = messages[i]!

    if (msg.messageType === MessageType.USER) {
      turns.push({ type: 'user', id: msg.id, userMessage: msg })
      i++
      continue
    }

    if (
      msg.messageType === MessageType.ASSISTANT ||
      msg.messageType === MessageType.TOOL_RESPONSE
    ) {
      const assistantMsgs: AgenticChatMessage[] = []
      const toolRounds: ToolRound[] = []
      let textContent: string | null = null
      let reasoningContent: string | null = null
      let turnId = msg.id
      const firstMessageId = msg.id
      let textMessageId: string | null = null
      let promptTokenCount: number | null = null
      let completionTokenCount: number | null = null
      let reasoningTokenCount: number | null = null
      let cachedTokenCount: number | null = null
      const createTime = msg.createTime

      while (i < messages.length) {
        const cur = messages[i]!
        if (cur.messageType === MessageType.USER) break

        if (cur.messageType === MessageType.ASSISTANT) {
          turnId = cur.id

          // Has tool calls → store its text on the round, pair with following TOOL_RESPONSE
          if (cur.toolContent) {
            const roundText = cur.textContent
            const calls = parseJsonArray<PersistedToolCall>(cur.toolContent)
            // 工具调用前的推理（解释为什么调用工具），出现在调用工具的 ASSISTANT 消息上
            const preToolReasoning = cur.reasoningContent
            i++

            if (i < messages.length && messages[i]!.messageType === MessageType.TOOL_RESPONSE) {
              const results = parseJsonArray<PersistedToolResult>(messages[i]!.toolContent)
              toolRounds.push({
                reasoningContent: null,
                preToolReasoningContent: preToolReasoning,
                toolCalls: calls,
                toolResults: results,
                textContent: roundText,
              })
              i++
            } else {
              toolRounds.push({
                reasoningContent: null,
                preToolReasoningContent: preToolReasoning,
                toolCalls: calls,
                toolResults: [],
                textContent: roundText,
              })
            }
            continue
          }

          // Pure text ASSISTANT (no tool content): capture as turn-level text and reasoning.
          // 如果前面有工具轮次，它的 reasoningContent 是工具调用后的推理，属于最后那个轮次。
          if (cur.textContent) {
            textContent = cur.textContent
            textMessageId = cur.id
            promptTokenCount = cur.promptTokenCount ?? promptTokenCount
            completionTokenCount = cur.completionTokenCount ?? completionTokenCount
            reasoningTokenCount = cur.reasoningTokenCount ?? reasoningTokenCount
            cachedTokenCount = cur.cachedTokenCount ?? cachedTokenCount
          }
          if (cur.reasoningContent) {
            if (toolRounds.length > 0) {
              const tr = toolRounds[toolRounds.length - 1]!
              toolRounds[toolRounds.length - 1] = { ...tr, reasoningContent: cur.reasoningContent }
            } else {
              reasoningContent = cur.reasoningContent
            }
          }
          i++
          continue
        }

        if (cur.messageType === MessageType.TOOL_RESPONSE) {
          const results = parseJsonArray<PersistedToolResult>(cur.toolContent)
          toolRounds.push({ reasoningContent: null, preToolReasoningContent: null, toolCalls: [], toolResults: results })
          i++
          continue
        }

        i++
      }

      turns.push({
        type: 'assistant',
        id: turnId,
        assistantTurn: { id: turnId, firstMessageId, textMessageId, reasoningContent, toolRounds, textContent, promptTokenCount, completionTokenCount, reasoningTokenCount, cachedTokenCount, createTime },
      })
      continue
    }

    i++
  }

  return turns
})

const lastAssistantTurnId = computed(() => {
  const assistantTurns = chatTurns.value.filter((t) => t.type === 'assistant')
  return assistantTurns[assistantTurns.length - 1]?.id || ''
})

/** 按工具调用轮次分割文本内容，用于流式输出时按时间顺序交错渲染 */
const streamingTextSegments = computed(() => {
  const sm = chatStore.streamingMessage
  if (!sm || !sm.rounds.length) {
    return sm?.textContent ? [renderMarkdown(sm.textContent)] : []
  }
  const full = sm.textContent
  const segments: string[] = []
  let start = 0
  for (const round of sm.rounds) {
    segments.push(renderMarkdown(full.slice(start, round.textPrefix)))
    start = round.textPrefix
  }
  segments.push(renderMarkdown(full.slice(start)))
  return segments
})
const isStreamingReasoning = computed(() => {
  const s = chatStore.streamingMessage
  if (!s) return false
  return !s.textContent
})
const hasStreamingContent = computed(() => {
  const s = chatStore.streamingMessage
  if (!s) return false
  return (
    !!s.textContent ||
    s.rounds.some((r) => r.toolCalls.length > 0 || !!r.reasoningContent)
  )
})

function isTempId(id: string): boolean {
  return id.startsWith('temp-')
}

function canAct(id: string): boolean {
  return !isTempId(id) && !chatStore.isStreaming
}

function handleCopyPlain(markdown: string) {
  const html = renderMarkdown(markdown)
  const div = document.createElement('div')
  div.innerHTML = html
  // 将块级元素换行，去掉多余空白
  const plain = div.textContent?.replace(/\n{3,}/g, '\n\n').trim() || ''
  navigator.clipboard.writeText(plain).then(() => {
    message.success('已复制纯文本')
  })
}

function handleCopyMarkdown(markdown: string) {
  navigator.clipboard.writeText(markdown).then(() => {
    message.success('已复制 Markdown')
  })
}

function handleCopyRichText(markdown: string) {
  const html = renderMarkdown(markdown)
  const blob = new Blob([html], { type: 'text/html' })
  const textBlob = new Blob([markdown], { type: 'text/plain' })
  navigator.clipboard.write([new ClipboardItem({ 'text/html': blob, 'text/plain': textBlob })]).then(() => {
    message.success('已复制富文本')
  })
}

function handleDeleteMessage(messageId: string) {
  Modal.confirm({
    title: '确认删除',
    content: '将删除该消息及之后的所有消息，确定继续吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: () => chatStore.deleteMessage(messageId),
  })
}

const editingMessageId = ref<string | null>(null)
const editingText = ref('')

function startEdit(messageId: string, currentText: string) {
  editingMessageId.value = messageId
  editingText.value = currentText
}

function cancelEdit() {
  editingMessageId.value = null
  editingText.value = ''
}

async function handleResend(messageId: string) {
  const trimmed = editingText.value.trim()
  if (!trimmed) return
  const text = trimmed
  editingMessageId.value = null
  editingText.value = ''
  await chatStore.editAndResendMessage(messageId, text)
}

async function handleSaveEdit(messageId: string) {
  const trimmed = editingText.value.trim()
  if (!trimmed) return
  const text = trimmed
  editingMessageId.value = null
  editingText.value = ''
  await chatStore.editMessageSaveOnly(messageId, text)
}

async function handleSaveAssistantEdit(messageId: string) {
  const trimmed = editingText.value.trim()
  if (!trimmed) return
  const text = trimmed
  editingMessageId.value = null
  editingText.value = ''
  await chatStore.editMessageSaveOnly(messageId, text)
}

async function handleAssistantEditAndClear(messageId: string) {
  const trimmed = editingText.value.trim()
  if (!trimmed) return
  const text = trimmed
  editingMessageId.value = null
  editingText.value = ''
  await chatStore.editAndClearAfter(messageId, text)
}

function formatTokenCount(
  prompt: number | null | undefined,
  completion: number | null | undefined,
  reasoning: number | null | undefined,
  cached: number | null | undefined,
): string | null {
  if (prompt == null && completion == null && reasoning == null && cached == null) return null
  const parts: string[] = []
  if (prompt != null) parts.push(`Prompt: ${prompt}`)
  if (completion != null) parts.push(`Completion: ${completion}`)
  if (reasoning != null) parts.push(`Reasoning: ${reasoning}`)
  if (cached != null) parts.push(`Cached: ${cached}`)
  return parts.length > 0 ? parts.join(' · ') : null
}

function formatTime(timeStr: string | null | undefined): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const yyyy = date.getFullYear()
  const MM = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${MM}-${dd} ${hh}:${mm}`
}

// Quote button state
const quoteButtonVisible = ref(false)
const quoteButtonStyle = ref<Record<string, string>>({})

function positionQuoteButton(selection: Selection) {
  const range = selection.getRangeAt(0)
  const container = range.startContainer instanceof HTMLElement
    ? range.startContainer
    : range.startContainer.parentElement
  const bubble = container?.closest('.chat-message-list__bubble')
  if (!bubble) return

  const rangeRect = range.getBoundingClientRect()
  quoteButtonStyle.value = {
    top: `${rangeRect.top - 36}px`,
    left: `${rangeRect.left + rangeRect.width / 2}px`,
  }
  quoteButtonVisible.value = true
}

// When user clicks (not drags) to dismiss, suppress the following mouseup
let suppressNextMouseUp = false

function onDocumentMouseUp() {
  if (suppressNextMouseUp) {
    suppressNextMouseUp = false
    return
  }
  if (!showQuoteButton.value) return
  // Use nextTick so the browser has finalized the selection
  nextTick(() => {
    const selection = window.getSelection()
    if (!selection || selection.isCollapsed || !selection.rangeCount) {
      quoteButtonVisible.value = false
      return
    }
    const text = selection.toString().trim()
    if (!text) {
      quoteButtonVisible.value = false
      return
    }
    positionQuoteButton(selection)
  })
}

function onDocumentMouseDown(e: MouseEvent) {
  // Hide the button if clicking anywhere other than the quote button itself
  const target = e.target as HTMLElement
  if (!target.closest('.chat-message-list__quote-btn')) {
    quoteButtonVisible.value = false
    suppressNextMouseUp = true
  }
}

function onDocumentMouseMove() {
  // User is dragging to make a new selection — allow mouseup to show button
  suppressNextMouseUp = false
}

onMounted(() => {
  document.addEventListener('mouseup', onDocumentMouseUp)
  document.addEventListener('mousedown', onDocumentMouseDown)
  document.addEventListener('mousemove', onDocumentMouseMove)
  document.addEventListener('click', onCopyBtnClick)
  // 新打开的历史对话：组件挂载后滚动到底部（若无已保存位置）
  const tab = chatStore.getActiveTab()
  if (tab && tab.messages.length > 0 && tab.scrollTop == null) {
    nextTick(() => {
      if (containerRef.value) containerRef.value.scrollTop = containerRef.value.scrollHeight
    })
  }
})

onUnmounted(() => {
  document.removeEventListener('mouseup', onDocumentMouseUp)
  document.removeEventListener('mousedown', onDocumentMouseDown)
  document.removeEventListener('mousemove', onDocumentMouseMove)
  document.removeEventListener('click', onCopyBtnClick)
  // 保存滚动位置，防止组件重建后 onMounted 错误滚到底部
  const tab = chatStore.getActiveTab()
  const el = containerRef.value
  if (tab && el) tab.scrollTop = el.scrollTop
})

function onCopyBtnClick(e: MouseEvent) {
  const btn = (e.target as HTMLElement).closest('.copy-btn') as HTMLElement | null
  if (!btn) return
  const pre = btn.closest('pre.hljs')
  if (!pre) return
  const code = pre.querySelector('code')
  if (!code) return
  const text = code.textContent || ''
  const label = btn.querySelector('span')
  if (!label) return
  navigator.clipboard.writeText(text).then(() => {
    label.textContent = '已复制'
    setTimeout(() => { label.textContent = '复制' }, 2000)
  }).catch(() => {
    label.textContent = '复制失败'
    setTimeout(() => { label.textContent = '复制' }, 2000)
  })
}

function handleQuoteClick() {
  const selection = window.getSelection()
  if (selection) {
    const text = selection.toString().trim()
    if (text) {
      startQuote(text)
      selection.removeAllRanges()
    }
  }
  quoteButtonVisible.value = false
}
</script>

<template>
  <div class="chat-message-list">
    <div ref="containerRef" class="chat-message-list__scroll-area" @scroll="onScroll">
    <div class="chat-message-list__inner" :class="{ 'chat-message-list__inner--wide': wideMode }">
      <div
        v-if="quoteButtonVisible"
        class="chat-message-list__quote-btn"
        :style="quoteButtonStyle"
        @mousedown.prevent="handleQuoteClick"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z" />
          <path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3c0 1 0 1 1 1z" />
        </svg>
        <span>引用</span>
      </div>
      <template v-for="(turn, index) in displayTurns" :key="turn.type + '-' + index">
        <div v-if="turn.type === 'user' && turn.userMessage" class="chat-message-list__user-turn">
          <div class="chat-message-list__avatar chat-message-list__avatar--user">
            <UserOutlined />
          </div>
          <div class="chat-message-list__bubble chat-message-list__bubble--user">
            <template v-if="editingMessageId === turn.userMessage.id">
              <div class="chat-message-list__edit-area">
                <textarea
                  v-model="editingText"
                  class="chat-message-list__edit-textarea"
                  rows="3"
                  @keydown.escape="cancelEdit"
                />
                <div class="chat-message-list__edit-actions">
                  <button class="chat-message-list__edit-btn chat-message-list__edit-btn--primary" @click="handleResend(turn.userMessage.id)">
                    编辑并重发
                  </button>
                  <button class="chat-message-list__edit-btn chat-message-list__edit-btn--save" @click="handleSaveEdit(turn.userMessage.id)">
                    编辑并保存
                  </button>
                  <button class="chat-message-list__edit-btn" @click="cancelEdit">
                    取消
                  </button>
                </div>
                <div class="chat-message-list__edit-hint">编辑并重发将删除该消息及之后的所有消息并重新发送</div>
              </div>
            </template>
            <template v-else>
              <div class="chat-message-list__text">{{ turn.userMessage.textContent }}</div>
              <img
                v-for="(url, imgIdx) in resolveMediaUrls(turn.userMessage.mediaContent)"
                :key="imgIdx"
                :src="url"
                alt="上传的图片"
                class="chat-message-list__image"
              />
              <div v-if="showMessageTime" class="chat-message-list__time">{{ formatTime(turn.userMessage.createTime) }}</div>
              <div
                v-if="showTokenCount && formatTokenCount(turn.userMessage.promptTokenCount, turn.userMessage.completionTokenCount, turn.userMessage.reasoningTokenCount, turn.userMessage.cachedTokenCount)"
                class="chat-message-list__token-info"
              >
                {{ formatTokenCount(turn.userMessage.promptTokenCount, turn.userMessage.completionTokenCount, turn.userMessage.reasoningTokenCount, turn.userMessage.cachedTokenCount) }}
              </div>
              <div v-if="canAct(turn.userMessage.id) && editingMessageId !== turn.userMessage.id" class="chat-message-list__actions">
                <button class="chat-message-list__action-btn" @click="handleCopyPlain(turn.userMessage.textContent || '')">
                  <CopyOutlined /> 复制
                </button>
                <button class="chat-message-list__action-btn" @click="startEdit(turn.userMessage.id, turn.userMessage.textContent || '')">
                  <EditOutlined /> 编辑
                </button>
                <button class="chat-message-list__action-btn chat-message-list__action-btn--danger" @click="handleDeleteMessage(turn.userMessage.id)">
                  <DeleteOutlined /> 删除
                </button>
              </div>
            </template>
          </div>
        </div>

        <div
          v-else-if="turn.type === 'assistant' && turn.assistantTurn"
          class="chat-message-list__assistant-turn"
        >
          <div class="chat-message-list__avatar chat-message-list__avatar--assistant">
            <img
              v-if="modelIconUrl && !modelIconFailed"
              :src="modelIconUrl"
              alt="AI"
              class="chat-message-list__model-icon"
              @error="modelIconFailed = true"
            />
            <RobotOutlined v-else />
          </div>
          <div class="chat-message-list__assistant-body">
            <div class="chat-message-list__bubble chat-message-list__bubble--assistant">
            <div v-if="workspaceBanner" class="chat-message-list__workspace-banner">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              <span>{{ workspaceBanner }}</span>
            </div>
            <template v-if="simplifiedProcess && !isDetailExpanded(turn) && (turnProcessSummary(turn).toolCalls > 0 || turnProcessSummary(turn).reasoningBlocks > 0)">
              <div class="chat-message-list__process-summary">
                <svg class="chat-message-list__process-summary-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <span class="chat-message-list__process-summary-text">
                  已完成思考 · {{ turnProcessSummary(turn).toolCalls }} 轮工具调用 · {{ turnProcessSummary(turn).reasoningBlocks }} 轮深度思考
                </span>
                <button class="chat-message-list__process-summary-btn" @click="expandDetail(turn)">查看详情</button>
              </div>
            </template>
            <template v-else>
              <template v-for="(round, rIdx) in turn.assistantTurn.toolRounds" :key="rIdx">
                <div v-if="round.textContent" class="chat-message-list__markdown" v-html="renderMarkdown(round.textContent)" />
                <ReasoningBlock v-if="round.preToolReasoningContent" :content="round.preToolReasoningContent" :blockKey="'reasoning-' + index + '-' + rIdx + '-pre'" />
                <div class="chat-message-list__tool-round">
                  <ToolCallBlock
                    v-for="(tc, tcIdx) in round.toolCalls"
                    :key="tc.id || tcIdx"
                    :name="tc.name"
                    :arguments="tc.arguments || ''"
                    :tool-call-id="tc.id"
                    :blockKey="'tool-' + tc.id"
                    :result="
                      round.toolResults.find((tr) => tr.id === tc.id)?.responseData ??
                      round.toolResults.find((tr) => tr.id === tc.id)?.response
                    "
                  />
                  <ToolCallBlock
                    v-for="(tr, trIdx) in round.toolResults.filter(
                      (tr) => !round.toolCalls.some((tc) => tc.id === tr.id),
                    )"
                    :key="'orphan-' + trIdx"
                    :name="tr.name"
                    :blockKey="'tool-orphan-' + index + '-' + rIdx + '-' + trIdx"
                    :result="tr.responseData ?? tr.response"
                  />
                </div>
                <ReasoningBlock v-if="round.reasoningContent" :content="round.reasoningContent" :blockKey="'reasoning-' + index + '-' + rIdx + '-post'" />
              </template>

              <ReasoningBlock
                v-if="turn.assistantTurn.reasoningContent"
                :content="turn.assistantTurn.reasoningContent"
                :blockKey="'reasoning-' + index + '-turn'"
              />
              <button
                v-if="simplifiedProcess && (turnProcessSummary(turn).toolCalls > 0 || turnProcessSummary(turn).reasoningBlocks > 0)"
                class="chat-message-list__collapse-btn"
                @click="collapseDetail(turn)"
              >收起模型调用中间过程详情</button>
            </template>

            <div
              v-if="editingMessageId && editingMessageId === turn.assistantTurn.textMessageId"
              class="chat-message-list__edit-area"
            >
              <textarea
                v-model="editingText"
                class="chat-message-list__edit-textarea"
                rows="3"
                @keydown.escape="cancelEdit"
              />
              <div class="chat-message-list__edit-actions">
                <button class="chat-message-list__edit-btn chat-message-list__edit-btn--save" @click="handleSaveAssistantEdit(turn.assistantTurn.textMessageId!)">编辑并保存</button>
                <button class="chat-message-list__edit-btn chat-message-list__edit-btn--danger" @click="handleAssistantEditAndClear(turn.assistantTurn.textMessageId!)">编辑并清除后续消息</button>
                <button class="chat-message-list__edit-btn" @click="cancelEdit">取消</button>
              </div>
            </div>
            <div
              v-else-if="turn.assistantTurn.textContent"
              class="chat-message-list__markdown"
              v-html="renderMarkdown(turn.assistantTurn.textContent)"
            />
            <div v-if="turn.assistantTurn.textMessageId && canAct(turn.assistantTurn.textMessageId) && editingMessageId !== turn.assistantTurn.textMessageId" class="chat-message-list__actions">
              <button class="chat-message-list__action-btn chat-message-list__action-btn--mobile-hide" @click="handleCopyPlain(turn.assistantTurn.textContent || '')">
                <CopyOutlined /> 复制（纯文本）
              </button>
              <button class="chat-message-list__action-btn chat-message-list__action-btn--mobile-keep" @click="handleCopyMarkdown(turn.assistantTurn.textContent || '')">
                <CopyOutlined /> 复制（Markdown）
              </button>
              <button class="chat-message-list__action-btn chat-message-list__action-btn--mobile-hide" @click="handleCopyRichText(turn.assistantTurn.textContent || '')">
                <CopyOutlined /> 复制（富文本）
              </button>
              <button
                class="chat-message-list__action-btn"
                @click="startEdit(turn.assistantTurn.textMessageId!, turn.assistantTurn.textContent || '')"
              >
                <EditOutlined /> 编辑
              </button>
              <button class="chat-message-list__action-btn chat-message-list__action-btn--danger" @click="handleDeleteMessage(turn.assistantTurn.textMessageId!)">
                <DeleteOutlined /> 删除
              </button>
            </div>
            <div
              v-if="getTurnRagRefCount(turn) > 0"
              class="chat-message-list__rag-ref"
              @click="showRagReferences(index, turn)"
            >
              {{ getTurnRagRefCount(turn) }} 引用
            </div>
            <div v-if="showMessageTime" class="chat-message-list__time">{{ formatTime(turn.assistantTurn.createTime) }}</div>
            <div
              v-if="showTokenCount && formatTokenCount(turn.assistantTurn.promptTokenCount, turn.assistantTurn.completionTokenCount, turn.assistantTurn.reasoningTokenCount, turn.assistantTurn.cachedTokenCount)"
              class="chat-message-list__token-info"
            >
              {{ formatTokenCount(turn.assistantTurn.promptTokenCount, turn.assistantTurn.completionTokenCount, turn.assistantTurn.reasoningTokenCount, turn.assistantTurn.cachedTokenCount) }}
            </div>
            <!-- 占位：保持与 streaming 模板相同高度，避免布局变化触发浏览器滚动 -->
            <div class="chat-message-list__waiting" style="visibility: hidden; pointer-events: none;">
              <LoadingOutlined class="chat-message-list__waiting-icon" />
              <span>正在等待响应</span>
            </div>
          </div>
          <div
            v-if="turn.id === lastAssistantTurnId && !showStreaming"
            class="chat-message-list__disclaimer"
          >
            内容由 AI 生成，请仔细甄别
          </div>
        </div>
      </div>

      <div
        v-else-if="turn.type === 'assistant' && !turn.assistantTurn"
        class="chat-message-list__assistant-turn"
      >
        <div class="chat-message-list__avatar chat-message-list__avatar--assistant">
          <img
            v-if="modelIconUrl && !modelIconFailed"
            :src="modelIconUrl"
            alt="AI"
            class="chat-message-list__model-icon"
            @error="modelIconFailed = true"
          />
          <RobotOutlined v-else />
        </div>
        <div class="chat-message-list__assistant-body">
          <div class="chat-message-list__bubble chat-message-list__bubble--assistant">
          <div v-if="workspaceBanner" class="chat-message-list__workspace-banner">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>{{ workspaceBanner }}</span>
          </div>
          <!-- Simplified streaming -->
          <template v-if="simplifiedProcess">
            <!-- No non-text blocks → just show the streaming text as-is -->
            <div
              v-if="!lastStreamingNonTextBlock"
              key="streaming-text-raw"
              class="chat-message-list__markdown"
              v-incremental-md="chatStore.streamingMessage?.textContent ?? ''"
            ></div>

            <!-- Summary: always shown in simplified mode (not in detail expanded) -->
            <template v-if="!streamingDetailExpanded">
              <div class="chat-message-list__process-summary">
                <LoadingOutlined v-if="chatStore.isStreaming" class="chat-message-list__process-summary-loading" />
                <svg v-else class="chat-message-list__process-summary-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <span class="chat-message-list__process-summary-text">
                  {{ chatStore.isStreaming ? '' : '已完成思考 · ' }}{{ streamingProcessSummary.toolCalls }} 轮工具调用 · {{ streamingProcessSummary.reasoningBlocks }} 轮深度思考
                </span>
                <button class="chat-message-list__process-summary-btn" @click="streamingDetailExpanded = true">查看详情</button>
              </div>
            </template>

            <!-- Reasoning or tool-call phase → show reasoning card below summary bar -->
            <template v-if="lastStreamingNonTextBlock && !streamingDetailExpanded && !streamingTextAfterBlock && (lastStreamingNonTextBlock.type === 'reasoning' || lastStreamingNonTextBlock.type === 'tool-round')">
              <div class="chat-message-list__reasoning-card">
                <div class="chat-message-list__reasoning-card-header">
                  <BulbOutlined class="chat-message-list__reasoning-card-icon" />
                  <span>思考过程</span>
                  <LoadingOutlined v-if="lastStreamingNonTextBlock.type === 'reasoning' && savedReasoningContent" class="chat-message-list__reasoning-card-loading" />
                </div>
                <!-- All reasoning rounds: stream content -->
                <div v-if="lastStreamingNonTextBlock?.type === 'reasoning'" class="chat-message-list__reasoning-card-text">
                  <span v-show="!lastStreamingNonTextBlock.content" class="chat-message-list__reasoning-card-text--dim">正在思考中</span>
                  <span v-show="!!lastStreamingNonTextBlock.content" v-incremental-md="lastStreamingNonTextBlock.content ?? ''"></span>
                </div>
                <!-- Tool-call phase: show completed reasoning content or fallback -->
                <div v-if="lastStreamingNonTextBlock.type === 'tool-round'" class="chat-message-list__reasoning-card-text">
                  <div v-if="savedReasoningContent" v-html="renderMarkdown(savedReasoningContent)"></div>
                  <span v-else class="chat-message-list__reasoning-card-text--dim">等待工具调用结果...</span>
                </div>
              </div>
            </template>

            <!-- Detail expanded → rounds → final text → 收起 -->
            <template v-if="streamingDetailExpanded">
              <div v-for="(round, rIdx) in chatStore.streamingMessage!.rounds" :key="rIdx">
                <div v-for="(block, bIdx) in round.displayBlocks" :key="bIdx">
                  <div v-if="block.type === 'text'" class="chat-message-list__markdown" v-incremental-md="block.content" />
                  <div v-if="block.type === 'reasoning'">
                    <ReasoningBlock
                      :content="(block as any).content ?? ''"
                      :blockKey="'reasoning-' + index + '-' + rIdx + '-' + (round.displayBlocks.slice(bIdx + 1).some(b => b.type === 'tool-round') ? 'pre' : 'post')"
                      :active="
                        rIdx === chatStore.streamingMessage!.rounds.length - 1 &&
                        bIdx === round.displayBlocks.length - 1 &&
                        isStreamingReasoning
                      "
                    />
                  </div>
                  <div v-if="block.type === 'tool-round'" class="chat-message-list__tool-round">
                    <ToolCallBlock
                      v-for="(tc, tcIdx) in round.toolCalls"
                      :key="tc.id || tcIdx"
                      :name="tc.name"
                      :arguments="tc.arguments"
                      :blockKey="'tool-' + tc.id"
                      :result="round.toolResults.find((tr) => tr.id === tc.id)?.result"
                      :is-searching="!round.toolResults.some((tr) => tr.id === tc.id)"
                    />
                  </div>
                </div>
              </div>
              <div
                v-if="streamingTextAfterBlock"
                key="streaming-text-after"
                class="chat-message-list__markdown"
                v-incremental-md="streamingTextAfterBlock"
              ></div>
              <button
                class="chat-message-list__collapse-btn"
                @click="streamingDetailExpanded = false"
              >收起模型调用中间过程详情</button>
            </template>

            <div
              v-if="streamingTextAfterBlock && !streamingDetailExpanded"
              key="streaming-text-after"
              class="chat-message-list__markdown"
              v-incremental-md="streamingTextAfterBlock"
            ></div>
          </template>
          <!-- Full streaming: show all blocks -->
          <template v-else-if="chatStore.streamingMessage!.rounds.length > 0">
            <template v-for="(round, rIdx) in chatStore.streamingMessage!.rounds" :key="rIdx">
              <template v-for="(block, bIdx) in round.displayBlocks" :key="bIdx">
                <div v-if="block.type === 'text'" class="chat-message-list__markdown" v-incremental-md="block.content" />
                <ReasoningBlock
                  v-else-if="block.type === 'reasoning'"
                  :content="block.content"
                  :blockKey="'reasoning-' + index + '-' + rIdx + '-' + (round.displayBlocks.slice(bIdx + 1).some(b => b.type === 'tool-round') ? 'pre' : 'post')"
                  :active="
                    rIdx === chatStore.streamingMessage!.rounds.length - 1 &&
                    bIdx === round.displayBlocks.length - 1 &&
                    isStreamingReasoning
                  "
                />
                <div v-else-if="block.type === 'tool-round'" class="chat-message-list__tool-round">
                  <ToolCallBlock
                    v-for="(tc, tcIdx) in round.toolCalls"
                    :key="tc.id || tcIdx"
                    :name="tc.name"
                    :arguments="tc.arguments"
                    :blockKey="'tool-' + tc.id"
                    :result="round.toolResults.find((tr) => tr.id === tc.id)?.result"
                    :is-searching="!round.toolResults.some((tr) => tr.id === tc.id)"
                  />
                </div>
              </template>
            </template>
          </template>
          <div
            v-else-if="!simplifiedProcess"
            key="streaming-text"
            class="chat-message-list__markdown"
            v-incremental-md="chatStore.streamingMessage?.textContent ?? ''"
          ></div>
          <div class="chat-message-list__waiting">
            <LoadingOutlined class="chat-message-list__waiting-icon" />
            <span>正在等待响应</span>
          </div>
        </div>
      </div>
    </div>
    </template>
  </div>
  </div>
    <button
      v-show="userScrolledUp"
      class="chat-message-list__scroll-bottom-btn"
      title="回到最新位置"
      @click="resetAutoScroll"
    >
      <ArrowDownOutlined />
    </button>
  </div>
</template>

<style scoped>
.chat-message-list {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #fafafa;
}

.chat-message-list__scroll-area {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  overflow-anchor: none;
  scrollbar-gutter: stable;
}

.chat-message-list__inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-message-list__inner--wide {
  max-width: none;
  margin: 0;
}

.chat-message-list__user-turn,
.chat-message-list__assistant-turn {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.chat-message-list__avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.chat-message-list__avatar--user {
  background: #1677ff;
  color: #ffffff;
}

.chat-message-list__avatar--assistant {
  background: #ffffff;
  color: #8c8c8c;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.chat-message-list__model-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.chat-message-list__bubble {
  min-width: 0;
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  position: relative;
}

.chat-message-list__bubble--user {
  background: #e6f4ff;
  color: #262626;
}

.chat-message-list__assistant-body {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.chat-message-list__process-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  margin: 4px 0;
  width: 100%;
  box-sizing: border-box;
}

.chat-message-list__process-summary-icon {
  flex-shrink: 0;
  color: #52c41a;
}

.chat-message-list__process-summary-loading {
  flex-shrink: 0;
  font-size: 14px;
  color: #1677ff;
  animation: chat-message-list-spin 1s linear infinite;
}

@keyframes chat-message-list-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chat-message-list__process-summary-btn {
  border: none;
  background: #1677ff;
  color: #ffffff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: background 0.15s;
}

.chat-message-list__process-summary-btn:hover {
  background: #4096ff;
}

.chat-message-list__collapse-btn {
  border: none;
  background: #1677ff;
  color: #ffffff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: background 0.15s;
  align-self: flex-start;
}

.chat-message-list__collapse-btn:hover {
  background: #4096ff;
}

.chat-message-list__reasoning-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 8px 12px;
  margin: 4px 0;
  width: 100%;
  box-sizing: border-box;
}

.chat-message-list__reasoning-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  margin-bottom: 6px;
}

.chat-message-list__reasoning-card-icon {
  font-size: 14px;
  color: #8c8c8c;
}

.chat-message-list__reasoning-card-loading {
  font-size: 12px;
  color: #1677ff;
  animation: chat-message-list-spin 1s linear infinite;
}

.chat-message-list__reasoning-card-text {
  font-size: 13px;
  line-height: 1.6;
  color: #595959;
  white-space: normal;
  overflow-wrap: break-word;
}

.chat-message-list__reasoning-card-text:empty::before {
  content: '正在思考中';
  color: #bfbfbf;
  font-style: italic;
  display: block;
}

.chat-message-list__reasoning-card-text :deep(p) {
  margin: 0 0 4px;
}

.chat-message-list__reasoning-card-text :deep(p:last-child) {
  margin-bottom: 0;
}

.chat-message-list__reasoning-card-text :deep(ul),
.chat-message-list__reasoning-card-text :deep(ol) {
  padding-left: 1.5em;
  margin: 4px 0;
}

.chat-message-list__reasoning-card-text :deep(li) {
  margin-bottom: 2px;
}

.chat-message-list__bubble--assistant {
  background: #ffffff;
  color: #262626;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-message-list__disclaimer {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
  margin-top: 4px;
  padding-left: 2px;
  background: #fafafa;
  padding: 3px 8px;
  border-radius: 4px;
}

.chat-message-list__workspace-banner {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: #ad6800;
}

.chat-message-list__text {
  white-space: pre-wrap;
}

.chat-message-list__actions {
  position: absolute;
  top: 100%;
  left: 0;
  display: flex;
  gap: 4px;
  padding: 4px 8px;
  background: #ffffff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  opacity: 0;
  transform: translateY(2px);
  pointer-events: none;
  transition: opacity 0.2s, transform 0.2s;
  z-index: 10;
  white-space: nowrap;
}

.chat-message-list__bubble:hover .chat-message-list__actions {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.chat-message-list__quote-btn {
  position: fixed;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #ffffff;
  color: #595959;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  z-index: 20;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: background 0.15s, color 0.15s;
  user-select: none;
}

.chat-message-list__quote-btn:hover {
  background: #f5f5f5;
  color: #262626;
}

.chat-message-list__action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
  font-family: inherit;
  line-height: 1.4;
}

.chat-message-list__action-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #262626;
}

.chat-message-list__action-btn--danger:hover {
  color: #ff4d4f;
  background: #fff1f0;
}

.chat-message-list__edit-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-message-list__edit-textarea {
  width: 100%;
  min-height: 60px;
  padding: 8px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

.chat-message-list__edit-textarea:focus {
  border-color: #1677ff;
}

.chat-message-list__edit-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.chat-message-list__edit-btn {
  border: 1px solid #d9d9d9;
  background: #ffffff;
  color: #595959;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.15s, color 0.15s;
}

.chat-message-list__edit-btn:hover {
  border-color: #4096ff;
  color: #1677ff;
}

.chat-message-list__edit-btn--save {
  background: #1677ff;
  border-color: #1677ff;
  color: #ffffff;
}

.chat-message-list__edit-btn--save:hover {
  background: #4096ff;
  border-color: #4096ff;
  color: #ffffff;
}

.chat-message-list__edit-btn--primary {
  background: #1677ff;
  border-color: #1677ff;
  color: #ffffff;
}

.chat-message-list__edit-btn--primary:hover {
  background: #4096ff;
  border-color: #4096ff;
  color: #ffffff;
}

.chat-message-list__edit-hint {
  font-size: 11px;
  color: #bfbfbf;
  line-height: 1.4;
}

.chat-message-list__tool-round {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-message-list__token-info {
  font-size: 11px;
  color: #bfbfbf;
  line-height: 1.4;
  margin-top: 2px;
}

.chat-message-list__rag-ref {
  font-size: 13px;
  font-weight: 600;
  color: #1677ff;
  cursor: pointer;
  margin-top: 4px;
  display: inline-block;
  width: fit-content;
  padding: 2px 8px;
  border-radius: 4px;
  background: #e6f4ff;
  transition: background 0.15s;
}

.chat-message-list__rag-ref:hover {
  background: #bae0ff;
}

.chat-message-list__time {
  font-size: 11px;
  color: #bfbfbf;
  line-height: 1.4;
  margin-top: 2px;
}

.chat-message-list__markdown {
  white-space: normal;
  overflow-wrap: break-word;
}

/* 流式场景下不需要 pre-wrap：markdown-it 已通过 <br>/<p> 控制换行，
   pre-wrap 会把 HTML 源码中 <br> 后的 \n 也保留为可见换行，造成重复空行 */

.chat-message-list__markdown :deep(p) {
  margin: 0 0 8px;
}

.chat-message-list__markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.chat-message-list__markdown :deep(ul),
.chat-message-list__markdown :deep(ol) {
  padding-left: 1.5em;
  margin: 4px 0 8px;
}

.chat-message-list__markdown :deep(ul:last-child),
.chat-message-list__markdown :deep(ol:last-child) {
  margin-bottom: 0;
}

.chat-message-list__markdown :deep(li) {
  margin-bottom: 2px;
}

.chat-message-list__markdown :deep(li:last-child) {
  margin-bottom: 0;
}

.chat-message-list__markdown :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 0;
  border-radius: 8px;
  overflow: hidden;
  font-size: 13px;
  margin: 8px 0;
}

.chat-message-list__markdown :deep(.code-toolbar) {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 4px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.chat-message-list__markdown :deep(.copy-btn) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  background: transparent;
  color: #c8c8c8;
  cursor: pointer;
  font-family: inherit;
  line-height: 1.5;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}

.chat-message-list__markdown :deep(.copy-btn:hover) {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.1);
}

.chat-message-list__markdown :deep(pre code) {
  display: block;
  padding: 12px 16px;
  overflow-x: auto;
}

.chat-message-list__markdown :deep(code) {
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.chat-message-list__markdown :deep(:not(pre) > code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

/* Headings */
.chat-message-list__markdown :deep(h1) {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 8px;
  line-height: 1.4;
  color: #262626;
}

.chat-message-list__markdown :deep(h2) {
  font-size: 17px;
  font-weight: 600;
  margin: 14px 0 8px;
  line-height: 1.4;
  color: #262626;
}

.chat-message-list__markdown :deep(h3) {
  font-size: 15px;
  font-weight: 600;
  margin: 12px 0 6px;
  line-height: 1.4;
  color: #262626;
}

.chat-message-list__markdown :deep(h4),
.chat-message-list__markdown :deep(h5),
.chat-message-list__markdown :deep(h6) {
  font-size: 14px;
  font-weight: 600;
  margin: 10px 0 6px;
  line-height: 1.4;
  color: #262626;
}

.chat-message-list__markdown :deep(h1:first-child),
.chat-message-list__markdown :deep(h2:first-child),
.chat-message-list__markdown :deep(h3:first-child) {
  margin-top: 0;
}

/* Horizontal rule */
.chat-message-list__markdown :deep(hr) {
  border: none;
  border-top: 1px solid #e8e8e8;
  margin: 12px 0;
}

/* Blockquote */
.chat-message-list__markdown :deep(blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #d9d9d9;
  background: #fafafa;
  border-radius: 0 4px 4px 0;
  color: #595959;
}

.chat-message-list__markdown :deep(blockquote p) {
  margin: 0 0 4px;
}

.chat-message-list__markdown :deep(blockquote p:last-child) {
  margin-bottom: 0;
}

/* Table */
.chat-message-list__markdown :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
  overflow-x: auto;
  display: block;
}

.chat-message-list__markdown :deep(thead) {
  background: #e8e8e8;
}

.chat-message-list__markdown :deep(th) {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  font-weight: 600;
  text-align: left;
  white-space: nowrap;
}

.chat-message-list__markdown :deep(td) {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
}

.chat-message-list__markdown :deep(tr:hover) {
  background: #f5f5f5;
}

.chat-message-list__markdown :deep(thead tr:hover) {
  background: #fafafa;
}

/* Links */
.chat-message-list__markdown :deep(a) {
  color: #1677ff;
  text-decoration: none;
}

.chat-message-list__markdown :deep(a:hover) {
  text-decoration: underline;
}

/* Strong / emphasis */
.chat-message-list__markdown :deep(strong) {
  font-weight: 600;
}

/* Image inside markdown */
.chat-message-list__markdown :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 4px 0;
}

.chat-message-list__image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 8px;
  margin-top: 8px;
  display: block;
}

.chat-message-list__waiting {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  color: #8c8c8c;
  font-size: 13px;
}

.chat-message-list__waiting-icon {
  font-size: 16px;
  color: #1677ff;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chat-message-list__scroll-bottom-btn {
  position: absolute;
  bottom: 32px;
  right: 32px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: #595959;
  font-size: 18px;
  transition: opacity 0.2s, transform 0.2s, color 0.2s, border-color 0.2s, box-shadow 0.2s;
  animation: chat-message-list-fade-in 0.2s ease;
}

.chat-message-list__scroll-bottom-btn:hover {
  color: #1677ff;
  border-color: #1677ff;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);
}

.chat-message-list__scroll-bottom-btn:active {
  transform: scale(0.95);
}

@keyframes chat-message-list-fade-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .chat-message-list__scroll-area {
    padding: 12px;
  }

  .chat-message-list__inner {
    gap: 12px;
  }

  .chat-message-list__avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .chat-message-list__image {
    max-width: 200px;
    max-height: 200px;
  }

  .chat-message-list__bubble {
    padding: 8px 12px;
  }

  .chat-message-list__action-btn--mobile-hide {
    display: none;
  }
}
</style>
