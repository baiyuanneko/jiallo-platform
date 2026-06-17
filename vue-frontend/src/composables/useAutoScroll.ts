// 聊天消息自动滚动 Composable

import { ref, watch, type Ref } from 'vue'

export interface UseAutoScrollOptions {
  /** 要监听滚动的容器 */
  containerRef: Ref<HTMLElement | null>
}

export function useAutoScroll(options: UseAutoScrollOptions) {
  const { containerRef } = options
  const userScrolledUp = ref(false)

  /** 判断是否滚动到底部 */
  function isAtBottom(): boolean {
    const el = containerRef.value
    if (!el) return true
    const threshold = 80
    return el.scrollHeight - el.scrollTop - el.clientHeight <= threshold
  }

  /** 滚动到底部 */
  function scrollToBottom(smooth = true) {
    const el = containerRef.value
    if (!el) return

    if (smooth) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
    } else {
      el.scrollTop = el.scrollHeight
    }
  }

  /** 监听用户滚动行为 — 用户主动上滚后永久停止自动滚动（当前对话） */
  function onScroll() {
    if (!isAtBottom()) {
      userScrolledUp.value = true
    }
  }

  /** 当消息变化时自动滚动（仅在用户未主动上滚时） */
  function watchForAutoScroll(deps: Ref<unknown>[]) {
    deps.forEach((dep) => {
      watch(dep, () => {
        if (!userScrolledUp.value) {
          requestAnimationFrame(() => {
            scrollToBottom(false)
          })
        }
      })
    })
  }

  /** 重置用户滚动状态并滚动到底部，恢复自动滚动 */
  function resetAutoScroll() {
    userScrolledUp.value = false
    scrollToBottom(true)
  }

  return {
    userScrolledUp,
    scrollToBottom,
    onScroll,
    watchForAutoScroll,
    resetAutoScroll,
  }
}
