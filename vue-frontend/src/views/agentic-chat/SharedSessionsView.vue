<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined, CopyOutlined, StopOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { getSharedSessionsPageApi, unshareSessionApi } from '@/api/apiChat'
import { ui } from '@/utils/ui'
import type { SharedSessionItem } from '@/types/chat'

const router = useRouter()

const sessions = ref<SharedSessionItem[]>([])
const pageNum = ref(1)
const hasMore = ref(true)
const loading = ref(false)

async function loadSessions(loadMore = false) {
  if (loading.value) return
  if (loadMore && !hasMore.value) return

  loading.value = true
  try {
    if (!loadMore) {
      pageNum.value = 1
      hasMore.value = true
    }

    const res = await getSharedSessionsPageApi({
      pageNum: pageNum.value,
      pageSize: 20,
    })

    if (res.data.code === 0) {
      const page = res.data.data
      if (loadMore) {
        sessions.value.push(...page.records)
      } else {
        sessions.value = page.records
      }
      hasMore.value = page.records.length >= 20
      pageNum.value++
    }
  } catch {
    ui.error('加载已分享会话列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSessions()
})

function goBack() {
  router.push('/app/agentic-chat-ui')
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function handleCopyLink(item: SharedSessionItem) {
  try {
    const link = `${window.location.origin}/app/agentic-chat-ui/shared/${item.id}`
    await navigator.clipboard.writeText(link)
    ui.success('链接已复制到剪贴板')
  } catch {
    ui.error('复制失败')
  }
}

function handleUnshare(item: SharedSessionItem) {
  Modal.confirm({
    title: '取消分享',
    content: `确定要取消分享"${item.sessionName || '未命名会话'}"吗？已分享的链接将无法访问。`,
    okText: '取消分享',
    okType: 'danger',
    cancelText: '返回',
    centered: true,
    onOk: async () => {
      try {
        const res = await unshareSessionApi({ sessionId: item.originalSessionId })
        if (res.data.code === 0) {
          sessions.value = sessions.value.filter((s) => s.id !== item.id)
          ui.success('已取消分享')
        }
      } catch {
        ui.error('取消分享失败')
      }
    },
  })
}

async function handleLoadMore() {
  await loadSessions(true)
}
</script>

<template>
  <div class="shared-sessions">
    <header class="shared-sessions__header">
      <button class="shared-sessions__back-btn" @click="goBack">
        <ArrowLeftOutlined />
        <span>返回</span>
      </button>
      <h1 class="shared-sessions__title">已分享会话</h1>
    </header>

    <div class="shared-sessions__content">
      <div v-if="!loading && sessions.length === 0" class="shared-sessions__empty">
        <RobotOutlined class="shared-sessions__empty-icon" />
        <p>暂无已分享的会话</p>
      </div>

      <div v-else class="shared-sessions__list">
        <div v-for="item in sessions" :key="item.id" class="shared-sessions__item">
          <div class="shared-sessions__item-info">
            <div class="shared-sessions__item-name">{{ item.sessionName || '未命名会话' }}</div>
            <div class="shared-sessions__item-time">分享时间：{{ formatDate(item.createTime) }}</div>
          </div>
          <div class="shared-sessions__item-actions">
            <button class="shared-sessions__action-btn" @click="handleCopyLink(item)">
              <CopyOutlined /> 复制链接
            </button>
            <button class="shared-sessions__action-btn shared-sessions__action-btn--danger" @click="handleUnshare(item)">
              <StopOutlined /> 取消分享
            </button>
          </div>
        </div>

        <div v-if="loading" class="shared-sessions__loading">
          <a-spin size="small" />
        </div>

        <div v-if="hasMore && !loading" class="shared-sessions__more">
          <button class="shared-sessions__more-btn" @click="handleLoadMore">加载更多</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shared-sessions {
  min-height: 100vh;
  background: #f5f5f5;
}

.shared-sessions__header {
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}

.shared-sessions__back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  padding: 4px 0;
  margin-bottom: 4px;
  color: #8c8c8c;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}

.shared-sessions__back-btn:hover {
  color: #1677ff;
}

.shared-sessions__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.shared-sessions__content {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.shared-sessions__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #bfbfbf;
  gap: 12px;
}

.shared-sessions__empty-icon {
  font-size: 40px;
  color: #d9d9d9;
}

.shared-sessions__empty p {
  margin: 0;
  font-size: 14px;
}

.shared-sessions__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shared-sessions__item {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.2s;
}

.shared-sessions__item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.shared-sessions__item-info {
  flex: 1;
  min-width: 0;
}

.shared-sessions__item-name {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shared-sessions__item-time {
  font-size: 12px;
  color: #bfbfbf;
  margin-top: 4px;
}

.shared-sessions__item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.shared-sessions__action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #d9d9d9;
  background: transparent;
  color: #595959;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: all 0.2s;
}

.shared-sessions__action-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.shared-sessions__action-btn--danger:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.shared-sessions__loading {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.shared-sessions__more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.shared-sessions__more-btn {
  border: 1px solid #d9d9d9;
  background: transparent;
  color: #595959;
  font-size: 13px;
  padding: 8px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;
}

.shared-sessions__more-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
}

@media (max-width: 768px) {
  .shared-sessions__content {
    padding: 12px;
  }

  .shared-sessions__item {
    flex-direction: column;
    align-items: flex-start;
  }

  .shared-sessions__item-actions {
    width: 100%;
  }
}
</style>
