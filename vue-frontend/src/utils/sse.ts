// SSE 流式连接工具 — 基于 fetch + ReadableStream 自定义解析

import axios from 'axios'
import { getAppConfig } from '@/config/appConfig'
import { getAccessToken, getTokenExpiry } from '@/utils/auth'
import type { ChatStreamVo, ToolCall, ToolResponse, ToolExecutionRequest } from '@/types/chat'

export interface SseCallbacks {
  onStreamUniqueKey: (key: string) => void
  onText: (chunk: string) => void
  onReasoning: (chunk: string) => void
  onToolCall: (data: ToolCall) => void
  onToolResult: (data: ToolResponse) => void
  onToolExecutionRequest: (data: ToolExecutionRequest) => void
  onSessionTitle: (sessionId: string, sessionName: string) => void
  onDone: (sessionId: string, messageId: string, userMessageId: string, promptTokenCount?: number | null, completionTokenCount?: number | null, reasoningTokenCount?: number | null, cachedTokenCount?: number | null) => void
  onError: (msg: string) => void
}

interface SseEvent {
  event: string
  data: string
}

// ==================== Token 刷新（与 request.ts 共享逻辑） ====================

let refreshingPromise: Promise<string> | null = null

async function refreshToken(): Promise<string> {
  if (refreshingPromise) return refreshingPromise

  refreshingPromise = (async () => {
    try {
      const { getRefreshToken, setAccessToken, setRefreshToken, setTokenExpiry } = await import(
        '@/utils/auth'
      )
      const oldRefreshToken = getRefreshToken()

      if (!oldRefreshToken) {
        throw new Error('No refresh token available')
      }

      const response = await axios.post(
        `${getAppConfig().apiBaseUrl}/user/refresh-token`,
        { refreshToken: oldRefreshToken },
      )

      if (response.data.code === 0) {
        const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data

        setAccessToken(accessToken)
        setRefreshToken(newRefreshToken)
        setTokenExpiry(Date.now() + expiresIn * 1000)

        const { useUserStore } = await import('@/stores/user')
        const userStore = useUserStore()
        userStore.accessToken = accessToken
        userStore.refreshToken = newRefreshToken
        userStore.tokenExpiresAt = Date.now() + expiresIn * 1000

        return accessToken
      } else {
        throw new Error(response.data.message || 'Token refresh failed')
      }
    } catch (error) {
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      userStore.logout()

      const { default: router } = await import('@/router')
      router.push('/login')

      throw error
    } finally {
      refreshingPromise = null
    }
  })()

  return refreshingPromise
}

/**
 * 获取有效的 access token：即将过期时主动刷新。
 */
async function getValidToken(): Promise<string> {
  const token = getAccessToken()
  if (!token) throw new Error('未登录')

  const expiresAt = getTokenExpiry()
  const now = Date.now()
  const isExpired = expiresAt > 0 && now >= expiresAt
  const willExpireSoon = expiresAt > 0 && !isExpired && expiresAt - now < 5 * 60 * 1000

  if (isExpired || willExpireSoon) {
    return refreshToken()
  }
  return token
}

// ==================== SSE 解析器 ====================

/**
 * 解析 SSE 文本流，正确处理跨 chunk 的事件边界。
 */
function createSseParser(onEvent: (ev: SseEvent) => void) {
  let buffer = ''

  return function processChunk(chunk: string) {
    buffer += chunk

    let pos: number
    while ((pos = buffer.indexOf('\n\n')) !== -1) {
      const block = buffer.substring(0, pos)
      buffer = buffer.substring(pos + 2)

      const lines = block.split('\n')
      let event = ''
      const dataParts: string[] = []

      for (const line of lines) {
        const colonIdx = line.indexOf(':')
        if (colonIdx === -1) {
          if (dataParts.length > 0) {
            dataParts.push(line)
          }
          continue
        }

        const field = line.substring(0, colonIdx)
        if (field === '') continue

        // SSE 规范要求剥离冒号后的第一个空格（作为可选分隔符），
        // 但后端直接将原始文本内容放在 data: 后面（不使用可选空格），
        // 因此不应剥离前导空格，否则会丢失实际内容中的空格。
        // 例如 " " 会变成 ""，" **" 会变成 "**"，导致 Markdown 语法破坏。
        const value = line.substring(colonIdx + 1)

        switch (field) {
          case 'event':
            event = value
            break
          case 'data':
            dataParts.push(value)
            break
        }
      }

      if (dataParts.length > 0) {
        const ev = { event, data: dataParts.join('\n') }
        if (!ev.event) {
          console.log('[SSE] onText chunk:', JSON.stringify(ev.data))
        }
        onEvent(ev)
      }
    }
  }
}

// ==================== 页面可见性跟踪 ====================

/** 页面是否处于可见状态。切到后台时浏览器可能中断 SSE 连接。 */
let pageVisible = true

if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    pageVisible = document.visibilityState === 'visible'
  })
}

// ==================== SSE 连接 ====================

/**
 * 创建 SSE 流式连接
 *
 * 与 request.ts 拦截器对齐：
 * - 发起连接前检查 token 有效期，即将过期则主动刷新
 * - 遇到 401 响应时自动刷新 token 并重试一次
 *
 * @param vo 请求参数
 * @param callbacks 各事件的回调
 * @returns AbortController — 调用 ctrl.abort() 可中断连接
 */
export function createSseConnection(vo: ChatStreamVo, callbacks: SseCallbacks): AbortController {
  const ctrl = new AbortController()

  const baseUrl = getAppConfig().apiBaseUrl
  const url = `${baseUrl}/agenticChatApp/chatStream`

  const dispatch = (ev: SseEvent) => {
    const eventType = ev.event
    const data = ev.data

    switch (eventType) {
      case 'tool_call':
        try {
          callbacks.onToolCall(JSON.parse(data))
        } catch {
          console.error('Failed to parse tool_call event:', data)
        }
        break

      case 'tool_result':
        try {
          callbacks.onToolResult(JSON.parse(data))
        } catch {
          console.error('Failed to parse tool_result event:', data)
        }
        break

      case 'tool_execution_request':
        try {
          callbacks.onToolExecutionRequest(JSON.parse(data))
        } catch {
          console.error('Failed to parse tool_execution_request event:', data)
        }
        break

      case 'done': {
        try {
          const parsed = JSON.parse(data)
          callbacks.onDone(
            parsed.sessionId || '',
            parsed.messageId || '',
            parsed.userMessageId || '',
            parsed.promptTokenCount ?? null,
            parsed.completionTokenCount ?? null,
            parsed.reasoningTokenCount ?? null,
            parsed.cachedTokenCount ?? null,
          )
        } catch {
          callbacks.onDone('', '', '')
        }
        break
      }

      case 'streamUniqueKey':
        callbacks.onStreamUniqueKey(data)
        break

      case 'error':
        callbacks.onError(data)
        break

      case 'reasoning':
        callbacks.onReasoning(data)
        break

      case 'sessionTitle':
        try {
          const parsed = JSON.parse(data)
          callbacks.onSessionTitle(parsed.sessionId, parsed.sessionName)
        } catch {
          console.error('Failed to parse sessionTitle event:', data)
        }
        break

      default:
        callbacks.onText(data)
        break
    }
  }

  const parse = createSseParser(dispatch)

  async function doFetch(isRetry = false): Promise<void> {
    let token: string
    try {
      token = await getValidToken()
    } catch {
      callbacks.onError('登录已过期，请重新登录')
      return
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
        },
        body: JSON.stringify(vo),
        signal: ctrl.signal,
      })

      if (response.status === 401 && !isRetry) {
        // 401 且非重试 → 刷新 token 后重试一次
        try {
          const newToken = await refreshToken()
          // 用新 token 重试（直接发起，不再走 getValidToken 避免重复刷新）
          const retryResponse = await fetch(url, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${newToken}`,
              Accept: 'text/event-stream',
            },
            body: JSON.stringify(vo),
            signal: ctrl.signal,
          })

          if (!retryResponse.ok) {
            const errorText = await retryResponse.text().catch(() => '')
            callbacks.onError(
              `HTTP ${retryResponse.status}: ${errorText || retryResponse.statusText}`,
            )
            return
          }

          await consumeStream(retryResponse)
        } catch {
          callbacks.onError('登录已过期，请重新登录')
        }
        return
      }

      if (!response.ok) {
        const errorText = await response.text().catch(() => '')
        callbacks.onError(`HTTP ${response.status}: ${errorText || response.statusText}`)
        return
      }

      await consumeStream(response)
    } catch (err) {
      if (ctrl.signal.aborted) return
      if (err instanceof DOMException && err.name === 'AbortError') return
      if (err instanceof Error) {
        callbacks.onError(err.message)
      } else {
        callbacks.onError('SSE 连接异常')
      }
    }
  }

  async function consumeStream(response: Response): Promise<void> {
    const body = response.body
    if (!body) {
      callbacks.onError('响应体为空')
      return
    }

    const reader = body.getReader()
    const decoder = new TextDecoder()

    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        parse(decoder.decode(value, { stream: true }))
      }
    } catch (err) {
      if (ctrl.signal.aborted) return
      if (err instanceof Error) {
        // 切到后台再切回导致的网络中断是预期的行为，静默清理即可
        callbacks.onError(pageVisible ? err.message : '__BACKGROUND_INTERRUPT__')
      } else {
        callbacks.onError('SSE 流读取异常')
      }
    }
  }

  doFetch()

  return ctrl
}
