<script setup lang="ts">
import { onMounted, onUnmounted, watch, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { usePreferences } from '@/composables/usePreferences'
import ChatSidebar from './ChatSidebar.vue'
import ChatPanel from './ChatPanel.vue'
import ChatTabBar from './ChatTabBar.vue'
import FileSidebar from './FileSidebar.vue'
import { useFileSidebarStore } from '@/stores/fileSidebar'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const fileSidebarStore = useFileSidebarStore()
const { isMobilePwa } = usePreferences()

const isRouteDrivenUpdate = ref(false)
const isDragging = ref(false)

function startDrag(e: MouseEvent) {
  e.preventDefault()
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value) return
  const newWidth = Math.max(260, Math.min(700, window.innerWidth - e.clientX))
  fileSidebarStore.panelWidth = newWidth
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const MOBILE_BREAKPOINT = 768

function checkMobileAndCollapse() {
  if (window.innerWidth < MOBILE_BREAKPOINT) {
    fileSidebarStore.close()
  }
}

onMounted(async () => {
  await Promise.all([chatStore.loadSessions(), chatStore.loadModels(), chatStore.loadAgentTypes(), chatStore.loadRagModuleAuth()])
  const sessionId = route.params.sessionId as string | undefined
  if (sessionId) {
    chatStore.handleSidebarSingleClick(sessionId)
  } else if (chatStore.tabs.length === 0) {
    chatStore.startNewSession()
  }

  checkMobileAndCollapse()
  window.addEventListener('resize', checkMobileAndCollapse)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobileAndCollapse)
})

// URL → store: browser nav / direct URL access
watch(
  () => route.params.sessionId,
  (sessionId) => {
    if (isRouteDrivenUpdate.value) {
      isRouteDrivenUpdate.value = false
      return
    }
    const id = sessionId as string | undefined
    if (id) {
      const activeTab = chatStore.getActiveTab()
      if (!activeTab || activeTab.sessionId !== id) {
        chatStore.handleSidebarSingleClick(id)
      }
    } else {
      // No session in URL - don't auto-navigate away if tabs exist
      if (chatStore.tabs.length === 0) {
        chatStore.startNewSession()
      }
    }
  },
)

// store → URL: sync active tab's sessionId to URL
watch(
  () => chatStore.activeTabId ? chatStore.getActiveTab()?.sessionId : null,
  (newId) => {
    const currentPathId = route.params.sessionId as string | undefined
    if (newId && newId !== currentPathId) {
      isRouteDrivenUpdate.value = true
      router.replace({ name: 'agenticChat', params: { sessionId: newId } })
    } else if (!newId && currentPathId) {
      isRouteDrivenUpdate.value = true
      router.replace({ name: 'agenticChat' })
    }
  },
)

function goBack() {
  router.push('/app')
}

function closeSidebar() {
  chatStore.sidebarVisible = false
}
</script>

<template>
  <div class="agentic-chat" :class="{ 'agentic-chat--mobile-pwa': isMobilePwa }">
    <aside
      class="agentic-chat__sidebar"
      :class="{
        'agentic-chat__sidebar--visible': chatStore.sidebarVisible,
        'agentic-chat__sidebar--collapsed': chatStore.sidebarCollapsed,
      }"
    >
      <ChatSidebar />
    </aside>

    <div v-if="chatStore.sidebarVisible" class="agentic-chat__backdrop" @click="closeSidebar" />

    <main class="agentic-chat__main">
      <div class="agentic-chat__mobile-header">
        <button class="agentic-chat__back-btn" @click="goBack">
          <ArrowLeftOutlined />
        </button>
        <span class="agentic-chat__mobile-title">Agentic Chat</span>
      </div>
      <ChatTabBar />
      <ChatPanel />
    </main>

    <button
      v-if="!fileSidebarStore.visible"
      class="agentic-chat__right-trigger"
      @click="fileSidebarStore.open()"
      title="展开文件与任务"
    >
      <span class="agentic-chat__right-trigger-label">展开对话观测链</span>
      <svg width="16" height="16" viewBox="0 0 12 12">
        <path d="M7 3L4 6L7 9" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>

    <div
      v-if="fileSidebarStore.visible"
      class="agentic-chat__resize-handle"
      @mousedown="startDrag"
      :class="{ 'agentic-chat__resize-handle--dragging': isDragging }"
    ></div>
    <aside
      class="agentic-chat__right-panel"
      :class="{ 'agentic-chat__right-panel--visible': fileSidebarStore.visible }"
      :style="fileSidebarStore.visible ? { width: fileSidebarStore.panelWidth + 'px', flex: '0 0 ' + fileSidebarStore.panelWidth + 'px' } : {}"
    >
      <FileSidebar />
    </aside>
  </div>
</template>

<style scoped>
.agentic-chat {
  display: flex;
  width: 100%;
  height: 100vh;
  background: #fafafa;
  overflow: hidden;
  position: relative;
}

.agentic-chat__sidebar {
  width: 280px;
  flex: 0 0 280px;
  border-right: 1px solid #f0f0f0;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.agentic-chat__sidebar--collapsed {
  width: 50px;
  flex: 0 0 50px;
}

.agentic-chat__backdrop {
  display: none;
}

.agentic-chat__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.agentic-chat__mobile-header {
  display: none;
}

.agentic-chat__back-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: #595959;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  transition: background 0.2s;
}

.agentic-chat__back-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.agentic-chat__mobile-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.agentic-chat--mobile-pwa .agentic-chat__mobile-header {
  display: none;
}

.agentic-chat__right-trigger {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #fafafa;
  border: none;
  border: 3px solid #d9d9d9;
  border-right: none;
  cursor: pointer;
  color: #595959;
  padding: 12px 0;
  border-radius: 8px 0 0 8px;
  z-index: 100;
  transition: color 0.15s, background 0.15s, border-color 0.15s;
}

.agentic-chat__right-trigger:hover {
  color: #1677ff;
  background: #e6f4ff;
  border-color: #1677ff;
  border-right: none;
}

.agentic-chat__right-trigger-label {
  writing-mode: vertical-rl;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 3px;
  user-select: none;
}

.agentic-chat__right-panel {
  width: 0;
  flex: 0 0 0;
  overflow: hidden;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  transition: none;
}

.agentic-chat__right-panel--visible {
  border-left: 1px solid #f0f0f0;
  overflow: hidden;
}

.agentic-chat__resize-handle {
  width: 4px;
  cursor: col-resize;
  flex-shrink: 0;
  background: transparent;
  transition: background 0.15s;
  position: relative;
  z-index: 10;
}

.agentic-chat__resize-handle:hover,
.agentic-chat__resize-handle--dragging {
  background: #1677ff;
}

@media (max-width: 768px) {
  .agentic-chat__sidebar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1000;
    width: 280px;
    height: 100vh;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .agentic-chat__sidebar--visible {
    transform: translateX(0);
  }

  .agentic-chat__backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 999;
    background: rgba(0, 0, 0, 0.45);
  }

  .agentic-chat__mobile-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    background: #ffffff;
  }
}
</style>
