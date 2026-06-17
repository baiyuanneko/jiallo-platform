<script setup lang="ts">
import { toRef, ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  SearchOutlined,
  PlusOutlined,
  ReadOutlined,
  ArrowLeftOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
  ShareAltOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { Modal } from 'ant-design-vue'
import { getMyAvatarApi } from '@/api/apiUser'
import { ui } from '@/utils/ui'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import SessionListItem from './SessionListItem.vue'
import SettingsModal from './SettingsModal.vue'

const router = useRouter()

function openRagLibrary() {
  const url = router.resolve({ name: 'ragLibrary' }).href
  window.open(url, '_blank')
}
const chatStore = useChatStore()
const userStore = useUserStore()

const isPwa = ref(false)

onMounted(() => {
  const mq = window.matchMedia('(display-mode: standalone)')
  isPwa.value = mq.matches || ('standalone' in window.navigator && (window.navigator as any).standalone)
  mq.addEventListener('change', (e) => {
    isPwa.value = e.matches
  })
})

const hasMore = toRef(chatStore, 'sessionsHasMore')

const { sentinelRef, isLoading } = useInfiniteScroll({
  onLoadMore: () => chatStore.loadSessions(true),
  hasMore,
})

const settingsVisible = ref(false)

const avatarBlobUrl = ref('')
const userInitial = computed(() => userStore.userInfo?.username?.[0]?.toUpperCase() || 'U')
const displayName = computed(() => userStore.userInfo?.displayName || userStore.userInfo?.username || '未登录')

async function loadAvatar() {
  if (!userStore.userInfo?.avatarUrl) {
    if (avatarBlobUrl.value) {
      URL.revokeObjectURL(avatarBlobUrl.value)
      avatarBlobUrl.value = ''
    }
    return
  }
  try {
    const blob = await getMyAvatarApi(userStore.avatarVersion)
    if (avatarBlobUrl.value) {
      URL.revokeObjectURL(avatarBlobUrl.value)
    }
    avatarBlobUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('加载头像失败:', error)
  }
}

watch(() => userStore.userInfo?.avatarUrl, () => loadAvatar(), { immediate: true })
watch(() => userStore.avatarVersion, () => loadAvatar())

function goToUserCenter() {
  router.push('/user/center')
}

function goToSharedSessions() {
  router.push('/app/agentic-chat-ui/shared-sessions')
}

function handleLogout() {
  ui.confirm({
    title: '提示',
    content: '确定要登出吗？',
    type: 'warning',
  })
    .then(async () => {
      await userStore.logout()
      ui.success('已登出')
      router.push('/')
    })
    .catch(() => {})
}

function goBack() {
  router.push('/app')
}

function handleDeleteSession(sessionId: string, sessionName: string) {
  Modal.confirm({
    title: '删除会话',
    content: `确定删除"${sessionName || '新对话'}"吗？此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: () => chatStore.deleteSession(sessionId),
  })
}
</script>

<template>
  <div class="chat-sidebar">
    <template v-if="chatStore.sidebarCollapsed">
      <div class="chat-sidebar__collapsed">
        <a-tooltip title="展开侧边栏" placement="right">
          <button class="chat-sidebar__collapse-btn" @click="chatStore.sidebarCollapsed = false">
            <MenuUnfoldOutlined />
          </button>
        </a-tooltip>
        <button class="chat-sidebar__collapsed-btn" @click="chatStore.sidebarCollapsed = false">
          <SearchOutlined />
        </button>
        <button class="chat-sidebar__collapsed-btn" @click="chatStore.startNewSession()">
          <PlusOutlined />
        </button>
        <div class="chat-sidebar__collapsed-spacer" />
        <button class="chat-sidebar__collapsed-btn" @click="settingsVisible = true">
          <SettingOutlined />
        </button>
        <a-dropdown trigger="click" placement="right">
          <button class="chat-sidebar__collapsed-btn">
            <a-avatar :size="24" :src="avatarBlobUrl || undefined">{{ userInitial }}</a-avatar>
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="shared-sessions" @click="goToSharedSessions">
                <ShareAltOutlined />
                已分享会话
              </a-menu-item>
              <a-menu-item key="user-center" @click="goToUserCenter">
                <UserOutlined />
                进入用户中心
              </a-menu-item>
              <a-menu-item key="logout" danger @click="handleLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </template>
    <template v-else>
      <div class="chat-sidebar__header">
        <button v-if="!isPwa" class="chat-sidebar__back-btn" @click="goBack">
          <ArrowLeftOutlined />
          <span>返回应用列表</span>
        </button>
        <div class="chat-sidebar__title-row">
          <a-tooltip title="收起侧边栏" placement="right">
            <button class="chat-sidebar__collapse-btn" @click="chatStore.sidebarCollapsed = true">
              <MenuFoldOutlined />
            </button>
          </a-tooltip>
          <h2 class="chat-sidebar__title">Agentic Chat UI</h2>
        </div>
      </div>

      <div class="chat-sidebar__search">
        <a-input v-model:value="chatStore.searchKeyword" placeholder="搜索会话..." allow-clear>
          <template #prefix>
            <SearchOutlined class="chat-sidebar__search-icon" />
          </template>
        </a-input>
      </div>

      <div class="chat-sidebar__rag-entry" @click="chatStore.startNewSession()">
        <PlusOutlined class="chat-sidebar__rag-icon" />
        <span>新建对话</span>
      </div>
      <div v-if="chatStore.ragModuleAuthorized" class="chat-sidebar__rag-entry" @click="openRagLibrary()">
        <ReadOutlined class="chat-sidebar__rag-icon" />
        <span>知识库</span>
      </div>

      <div class="chat-sidebar__list">
        <SessionListItem
          v-for="session in chatStore.filteredSessions"
          :key="session.id"
          :session="session"
          :is-active="chatStore.currentSessionId === session.id"
          @single-click="chatStore.handleSidebarSingleClick(session.id)"
          @double-click="chatStore.handleSidebarDoubleClick(session.id)"
          @rename="(id, name) => chatStore.renameSession(id, name)"
          @delete="handleDeleteSession(session.id, session.sessionName)"
        />
        <div ref="sentinelRef" class="chat-sidebar__sentinel" />
        <div v-if="isLoading" class="chat-sidebar__loading">
          <a-spin size="small" />
        </div>
      </div>

      <div class="chat-sidebar__footer">
        <button class="chat-sidebar__settings-btn" @click="settingsVisible = true">
          <span class="chat-sidebar__settings-icon"><SettingOutlined /></span>
          <span>设置</span>
        </button>
        <a-dropdown trigger="click" placement="topLeft">
          <div class="chat-sidebar__user">
            <a-avatar :size="28" :src="avatarBlobUrl || undefined">{{ userInitial }}</a-avatar>
            <span class="chat-sidebar__username">{{ displayName }}</span>
          </div>
          <template #overlay>
            <a-menu>
              <a-menu-item key="shared-sessions" @click="goToSharedSessions">
                <ShareAltOutlined />
                已分享会话
              </a-menu-item>
              <a-menu-item key="user-center" @click="goToUserCenter">
                <UserOutlined />
                进入用户中心
              </a-menu-item>
              <a-menu-item key="logout" danger @click="handleLogout">
                <LogoutOutlined />
                退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </template>

    <SettingsModal v-model:visible="settingsVisible" />
  </div>
</template>

<style scoped>
.chat-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-sidebar__header {
  padding: 16px 16px 12px;
  flex-shrink: 0;
}

.chat-sidebar__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-sidebar__back-btn {
  display: flex;
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

.chat-sidebar__back-btn:hover {
  color: #1677ff;
}

.chat-sidebar__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #262626;
  letter-spacing: -0.02em;
}

.chat-sidebar__search {
  padding: 0 12px 12px;
  flex-shrink: 0;
}

.chat-sidebar__search-icon {
  color: #8c8c8c;
}

.chat-sidebar__action {
  padding: 0 12px 12px;
  flex-shrink: 0;
}

.chat-sidebar__rag-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin: 0 8px 4px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #262626;
  transition: background 0.15s;
}

.chat-sidebar__rag-entry:hover {
  background: rgba(0, 0, 0, 0.04);
}

.chat-sidebar__rag-icon {
  font-size: 16px;
  color: #595959;
}

.chat-sidebar__list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.chat-sidebar__sentinel {
  height: 1px;
}

.chat-sidebar__loading {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.chat-sidebar__footer {
  flex-shrink: 0;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #f5f5f5;
}

.chat-sidebar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 8px;
  border-radius: 6px;
  transition: background 0.15s;
  min-width: 0;
}

.chat-sidebar__user:hover {
  background: #e8e8e8;
}

.chat-sidebar__username {
  font-size: 14px;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-sidebar__settings-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  color: #262626;
  font-size: 14px;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;
  width: 100%;
  text-align: left;
}

.chat-sidebar__settings-btn:hover {
  background: #e8e8e8;
}

.chat-sidebar__settings-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.chat-sidebar__collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 18px;
  cursor: pointer;
  border-radius: 6px;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
}

.chat-sidebar__collapse-btn:hover {
  background: #e8e8e8;
  color: #262626;
}

.chat-sidebar__collapsed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  height: 100%;
}

.chat-sidebar__collapsed-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: #595959;
  font-size: 18px;
  cursor: pointer;
  border-radius: 8px;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
  padding: 0;
}

.chat-sidebar__collapsed-btn:hover {
  background: #e8e8e8;
  color: #262626;
}

.chat-sidebar__collapsed-spacer {
  flex: 1;
}

@media (max-width: 768px) {
  .chat-sidebar__header {
    padding: 16px 12px 8px;
  }

  .chat-sidebar__search {
    padding: 0 8px 8px;
  }

  .chat-sidebar__action {
    padding: 0 8px 8px;
  }

  .chat-sidebar__list {
    padding: 0 4px 4px;
  }

  /* 移动端靠汉堡菜单控制侧边栏，收起/展开按钮冗余 */
  .chat-sidebar__collapse-btn {
    display: none;
  }
}
</style>
