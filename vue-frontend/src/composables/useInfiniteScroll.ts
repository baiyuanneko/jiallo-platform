// 无限滚动 Composable — 基于 IntersectionObserver

import { ref, onMounted, onUnmounted, type Ref } from 'vue'

export interface UseInfiniteScrollOptions {
  /** 触底时的回调 */
  onLoadMore: () => Promise<void>
  /** 是否还有更多数据 */
  hasMore: Ref<boolean>
  /** 距离底部多少像素触发（默认 100px） */
  threshold?: number
}

export function useInfiniteScroll(options: UseInfiniteScrollOptions) {
  const { onLoadMore, hasMore, threshold = 100 } = options
  const isLoading = ref(false)
  const sentinelRef = ref<HTMLElement | null>(null)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    observer = new IntersectionObserver(
      async (entries) => {
        const entry = entries[0]
        if (!entry || !entry.isIntersecting) return
        if (isLoading.value || !hasMore.value) return

        isLoading.value = true
        try {
          await onLoadMore()
        } finally {
          isLoading.value = false
        }
      },
      {
        rootMargin: `0px 0px ${threshold}px 0px`,
      },
    )

    if (sentinelRef.value) {
      observer.observe(sentinelRef.value)
    }
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return { sentinelRef, isLoading }
}
