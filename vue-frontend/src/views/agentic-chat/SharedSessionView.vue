<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { UserOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { getSharedSessionDetailApi, getModelIconUrl } from '@/api/apiChat'
import { renderMarkdown } from '@/utils/markdown'
import { MessageType } from '@/types/chat'
import type {
  AgenticChatMessage,
  SharedSessionDetail,
  PersistedToolCall,
  PersistedToolResult,
} from '@/types/chat'
import ReasoningBlock from './ReasoningBlock.vue'
import ToolCallBlock from './ToolCallBlock.vue'

const route = useRoute()
const sharedSessionId = route.params.sharedSessionId as string

const loading = ref(true)
const error = ref('')
const session = ref<SharedSessionDetail | null>(null)

onMounted(async () => {
  try {
    const res = await getSharedSessionDetailApi(sharedSessionId)
    if (res.data.code === 0) {
      session.value = res.data.data
    } else {
      error.value = res.data.message || '加载失败'
    }
  } catch {
    error.value = '该分享不存在或已取消'
  } finally {
    loading.value = false
  }
})

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

interface ToolRound {
  reasoningContent: string | null
  toolCalls: PersistedToolCall[]
  toolResults: PersistedToolResult[]
}

interface AssistantTurn {
  id: string
  reasoningContent: string | null
  toolRounds: ToolRound[]
  textContent: string | null
  createTime: string
}

interface ChatTurn {
  type: 'user' | 'assistant'
  id: string
  userMessage?: AgenticChatMessage
  assistantTurn?: AssistantTurn
}

const modelDisplayLabel = computed(() => {
  const m = session.value?.model
  if (!m) return ''
  return m.modelDisplayName || m.modelName || ''
})

const modelIconSrc = computed(() => {
  const m = session.value?.model
  if (!m) return ''
  return getModelIconUrl(m.id, m.updateTime)
})

const modelIconFailed = ref(false)

watch(modelIconSrc, () => {
  modelIconFailed.value = false
})

const lastAssistantTurnId = computed(() => {
  const assistantTurns = chatTurns.value.filter((t) => t.type === 'assistant')
  return assistantTurns[assistantTurns.length - 1]?.id || ''
})

const chatTurns = computed<ChatTurn[]>(() => {
  const messages = session.value?.messages
  if (!messages) return []
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
      const toolRounds: ToolRound[] = []
      let textContent: string | null = null
      let reasoningContent: string | null = null
      let turnId = msg.id
      let createTime = msg.createTime

      while (i < messages.length) {
        const cur = messages[i]!
        if (cur.messageType === MessageType.USER) break

        if (cur.messageType === MessageType.ASSISTANT && cur.textContent) {
          textContent = cur.textContent
          reasoningContent = cur.reasoningContent
          turnId = cur.id
          createTime = cur.createTime
          i++
          break
        }

        if (cur.messageType === MessageType.ASSISTANT && cur.toolContent) {
          const calls = parseJsonArray<PersistedToolCall>(cur.toolContent)
          const roundReasoning = cur.reasoningContent
          turnId = cur.id
          i++

          if (i < messages.length && messages[i]!.messageType === MessageType.TOOL_RESPONSE) {
            const results = parseJsonArray<PersistedToolResult>(messages[i]!.toolContent)
            toolRounds.push({ reasoningContent: roundReasoning, toolCalls: calls, toolResults: results })
            i++
          } else {
            toolRounds.push({ reasoningContent: roundReasoning, toolCalls: calls, toolResults: [] })
          }
          continue
        }

        if (cur.messageType === MessageType.TOOL_RESPONSE) {
          const results = parseJsonArray<PersistedToolResult>(cur.toolContent)
          toolRounds.push({ reasoningContent: null, toolCalls: [], toolResults: results })
          i++
          continue
        }

        i++
      }

      turns.push({
        type: 'assistant',
        id: turnId,
        assistantTurn: { id: turnId, reasoningContent, toolRounds, textContent, createTime },
      })
      continue
    }

    i++
  }

  return turns
})

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<template>
  <div class="shared-session">
    <header class="shared-session__header">
      <div class="shared-session__brand">
        <RobotOutlined class="shared-session__brand-icon" />
        <span>Agentic Chat</span>
      </div>
      <h1 v-if="session" class="shared-session__title">{{ session.sessionName }}</h1>
      <p v-if="modelDisplayLabel" class="shared-session__model-info">
        · 此会话使用
        <img
          v-if="modelIconSrc && !modelIconFailed"
          :src="modelIconSrc"
          alt=""
          class="shared-session__model-icon"
          @error="modelIconFailed = true"
        />
        <RobotOutlined v-else class="shared-session__model-icon-fallback" />
        <span class="shared-session__model-name">{{ modelDisplayLabel }}</span>
        模型创建
      </p>
    </header>

    <div v-if="loading" class="shared-session__loading">
      <a-spin tip="加载中..." />
    </div>

    <div v-else-if="error" class="shared-session__error">
      <div class="shared-session__error-icon">!</div>
      <p class="shared-session__error-text">{{ error }}</p>
    </div>

    <div v-else-if="session" class="shared-session__messages">
      <div class="shared-session__inner">
        <template v-for="turn in chatTurns" :key="turn.id">
          <div v-if="turn.type === 'user' && turn.userMessage" class="shared-session__user-turn">
            <div class="shared-session__avatar shared-session__avatar--user">
              <UserOutlined />
            </div>
            <div class="shared-session__bubble shared-session__bubble--user">
              <div class="shared-session__text">{{ turn.userMessage.textContent }}</div>
              <div class="shared-session__time">{{ formatTime(turn.userMessage.createTime) }}</div>
            </div>
          </div>

          <div
            v-else-if="turn.type === 'assistant' && turn.assistantTurn"
            class="shared-session__assistant-turn"
          >
            <div class="shared-session__avatar shared-session__avatar--assistant">
              <RobotOutlined />
            </div>
            <div class="shared-session__assistant-body">
              <div class="shared-session__bubble shared-session__bubble--assistant">
              <template v-for="(round, rIdx) in turn.assistantTurn.toolRounds" :key="rIdx">
                <ReasoningBlock v-if="round.reasoningContent" :content="round.reasoningContent" />
                <div class="shared-session__tool-round">
                  <ToolCallBlock
                    v-for="(tc, tcIdx) in round.toolCalls"
                    :key="tc.id || tcIdx"
                    :name="tc.name"
                    :arguments="tc.arguments || ''"
                    :tool-call-id="tc.id"
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
                    :result="tr.responseData ?? tr.response"
                  />
                </div>
              </template>

              <ReasoningBlock
                v-if="turn.assistantTurn.reasoningContent"
                :content="turn.assistantTurn.reasoningContent"
              />

              <div
                v-if="turn.assistantTurn.textContent"
                class="shared-session__markdown"
                v-html="renderMarkdown(turn.assistantTurn.textContent)"
              />
              <div class="shared-session__time">{{ formatTime(turn.assistantTurn.createTime) }}</div>
            </div>
            <div
              v-if="turn.id === lastAssistantTurnId"
              class="shared-session__disclaimer"
            >
              内容由 AI 生成，请仔细甄别
            </div>
          </div>
        </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shared-session {
  min-height: 100vh;
  background: #fafafa;
}

.shared-session__header {
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}

.shared-session__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.shared-session__brand-icon {
  font-size: 16px;
  color: #1677ff;
}

.shared-session__model-info {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8c8c8c;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.shared-session__model-icon {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  object-fit: contain;
  vertical-align: middle;
}

.shared-session__model-icon-fallback {
  font-size: 14px;
  color: #8c8c8c;
}

.shared-session__model-name {
  color: #595959;
  font-weight: 500;
}

.shared-session__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.shared-session__loading {
  display: flex;
  justify-content: center;
  padding: 80px 20px;
}

.shared-session__error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.shared-session__error-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff1f0;
  color: #ff4d4f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.shared-session__error-text {
  font-size: 16px;
  color: #595959;
  margin: 0;
}

.shared-session__messages {
  padding: 20px;
}

.shared-session__inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shared-session__user-turn,
.shared-session__assistant-turn {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.shared-session__avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.shared-session__avatar--user {
  background: #1677ff;
  color: #ffffff;
}

.shared-session__avatar--assistant {
  background: #ffffff;
  color: #8c8c8c;
  border: 1px solid #f0f0f0;
}

.shared-session__bubble {
  min-width: 0;
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  position: relative;
}

.shared-session__bubble--user {
  background: #e6f4ff;
  color: #262626;
}

.shared-session__bubble--assistant {
  background: #ffffff;
  color: #262626;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shared-session__assistant-body {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.shared-session__disclaimer {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
  margin-top: 4px;
  padding-left: 2px;
  background: #fafafa;
  padding: 3px 8px;
  border-radius: 4px;
}

.shared-session__text {
  white-space: pre-wrap;
}

.shared-session__time {
  font-size: 11px;
  color: #bfbfbf;
  line-height: 1.4;
  margin-top: 2px;
}

.shared-session__tool-round {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.shared-session__markdown {
  white-space: normal;
  overflow-wrap: break-word;
}

.shared-session__markdown :deep(p) {
  margin: 0 0 8px;
}

.shared-session__markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.shared-session__markdown :deep(ul),
.shared-session__markdown :deep(ol) {
  padding-left: 1.5em;
  margin: 4px 0 8px;
}

.shared-session__markdown :deep(ul:last-child),
.shared-session__markdown :deep(ol:last-child) {
  margin-bottom: 0;
}

.shared-session__markdown :deep(li) {
  margin-bottom: 2px;
}

.shared-session__markdown :deep(li:last-child) {
  margin-bottom: 0;
}

.shared-session__markdown :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  margin: 8px 0;
}

.shared-session__markdown :deep(code) {
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.shared-session__markdown :deep(:not(pre) > code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.shared-session__markdown :deep(h1) {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 8px;
  line-height: 1.4;
  color: #262626;
}

.shared-session__markdown :deep(h2) {
  font-size: 17px;
  font-weight: 600;
  margin: 14px 0 8px;
  line-height: 1.4;
  color: #262626;
}

.shared-session__markdown :deep(h3) {
  font-size: 15px;
  font-weight: 600;
  margin: 12px 0 6px;
  line-height: 1.4;
  color: #262626;
}

.shared-session__markdown :deep(h4),
.shared-session__markdown :deep(h5),
.shared-session__markdown :deep(h6) {
  font-size: 14px;
  font-weight: 600;
  margin: 10px 0 6px;
  line-height: 1.4;
  color: #262626;
}

.shared-session__markdown :deep(h1:first-child),
.shared-session__markdown :deep(h2:first-child),
.shared-session__markdown :deep(h3:first-child) {
  margin-top: 0;
}

.shared-session__markdown :deep(hr) {
  border: none;
  border-top: 1px solid #e8e8e8;
  margin: 12px 0;
}

.shared-session__markdown :deep(blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #d9d9d9;
  background: #fafafa;
  border-radius: 0 4px 4px 0;
  color: #595959;
}

.shared-session__markdown :deep(blockquote p) {
  margin: 0 0 4px;
}

.shared-session__markdown :deep(blockquote p:last-child) {
  margin-bottom: 0;
}

.shared-session__markdown :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
  overflow-x: auto;
  display: block;
}

.shared-session__markdown :deep(thead) {
  background: #fafafa;
}

.shared-session__markdown :deep(th) {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  font-weight: 600;
  text-align: left;
  white-space: nowrap;
}

.shared-session__markdown :deep(td) {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
}

.shared-session__markdown :deep(a) {
  color: #1677ff;
  text-decoration: none;
}

.shared-session__markdown :deep(a:hover) {
  text-decoration: underline;
}

.shared-session__markdown :deep(strong) {
  font-weight: 600;
}

.shared-session__markdown :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 4px 0;
}

@media (max-width: 768px) {
  .shared-session__header {
    padding: 12px 16px;
  }

  .shared-session__messages {
    padding: 12px;
  }

  .shared-session__avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .shared-session__bubble {
    padding: 8px 12px;
  }
}
</style>
