<script setup lang="ts">
import { ref } from 'vue'
import { MoreOutlined } from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { usePreferences } from '@/composables/usePreferences'

const chatStore = useChatStore()
const { effectiveMultiTab } = usePreferences()

const draggedIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

function onDragStart(e: DragEvent, index: number) {
  draggedIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
  }
}

function onDragOver(e: DragEvent, index: number) {
  e.preventDefault()
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'move'
  }
  dragOverIndex.value = index
}

function onDragLeave() {
  dragOverIndex.value = null
}

function onDrop(e: DragEvent, toIndex: number) {
  e.preventDefault()
  const from = draggedIndex.value
  if (from !== null && from !== toIndex) {
    chatStore.reorderTabs(from, toIndex)
  }
  draggedIndex.value = null
  dragOverIndex.value = null
}

function onDragEnd() {
  draggedIndex.value = null
  dragOverIndex.value = null
}
</script>

<template>
  <div v-if="effectiveMultiTab && chatStore.tabs.length > 0" class="chat-tab-bar">
    <div class="chat-tab-bar__scroll">
      <div
        v-for="(tab, index) in chatStore.tabs"
        :key="tab.id"
        draggable="true"
        class="chat-tab-bar__tab"
        :class="{
          active: tab.id === chatStore.activeTabId,
          preview: tab.isPreview,
          'chat-tab-bar__tab--dragging': draggedIndex === index,
          'chat-tab-bar__tab--drag-over': dragOverIndex === index && draggedIndex !== index,
        }"
        @click="chatStore.switchToTab(tab.id)"
        @dblclick="tab.isPreview = false"
        @dragstart="onDragStart($event, index)"
        @dragover="onDragOver($event, index)"
        @dragleave="onDragLeave"
        @drop="onDrop($event, index)"
        @dragend="onDragEnd"
      >
        <span class="chat-tab-bar__name">{{ tab.sessionName || '新对话' }}</span>
        <button class="chat-tab-bar__close" @click.stop="chatStore.closeTab(tab.id)">
          <span class="chat-tab-bar__close-icon">&times;</span>
        </button>
      </div>
    </div>
    <a-dropdown trigger="click" placement="bottomRight">
      <button class="chat-tab-bar__more-btn">
        <MoreOutlined />
        <span class="chat-tab-bar__more-label">管理标签页</span>
      </button>
      <template #overlay>
        <a-menu>
          <a-menu-item key="close-others" @click="chatStore.closeOtherTabs()">
            关闭除当前标签外其余标签
          </a-menu-item>
          <a-menu-item key="close-all" danger @click="chatStore.closeAllTabs()">
            清空全部标签
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<style scoped>
.chat-tab-bar {
  display: flex;
  align-items: stretch;
  height: 35px;
  min-height: 35px;
  background: #f0f0f0;
  border-bottom: 1px solid #e0e0e0;
  overflow: hidden;
  user-select: none;
}

.chat-tab-bar__scroll {
  display: flex;
  align-items: stretch;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
}

.chat-tab-bar__scroll::-webkit-scrollbar {
  height: 0;
}

.chat-tab-bar__tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 12px;
  min-width: 0;
  max-width: 180px;
  cursor: pointer;
  border-right: 1px solid #e0e0e0;
  background: #e8e8e8;
  transition: background 0.15s;
  flex-shrink: 0;
}

.chat-tab-bar__tab:hover {
  background: #ebebeb;
}

.chat-tab-bar__tab.active {
  background: #ffffff;
  border-top: 2px solid #1677ff;
  padding-top: 0;
}

.chat-tab-bar__name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #595959;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 35px;
}

.chat-tab-bar__tab.active .chat-tab-bar__name {
  color: #262626;
}

.chat-tab-bar__tab.preview .chat-tab-bar__name {
  font-style: italic;
}

.chat-tab-bar__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: none;
  background: transparent;
  border-radius: 3px;
  cursor: pointer;
  padding: 0;
  opacity: 0;
  transition:
    opacity 0.1s,
    background 0.1s;
  flex-shrink: 0;
}

.chat-tab-bar__tab:hover .chat-tab-bar__close,
.chat-tab-bar__tab.active .chat-tab-bar__close {
  opacity: 1;
}

.chat-tab-bar__close:hover {
  background: rgba(0, 0, 0, 0.08);
}

.chat-tab-bar__close-icon {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1;
}

.chat-tab-bar__close:hover .chat-tab-bar__close-icon {
  color: #262626;
}

.chat-tab-bar__tab--dragging {
  opacity: 0.4;
}

.chat-tab-bar__tab--drag-over {
  background: #e6f4ff;
  border-left: 2px solid #1677ff;
}

.chat-tab-bar__more-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 10px;
  height: 100%;
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
  font-family: inherit;
}

.chat-tab-bar__more-btn:hover {
  color: #262626;
  background: rgba(0, 0, 0, 0.04);
}

.chat-tab-bar__more-label {
  font-size: 12px;
  margin-left: 2px;
}
</style>
