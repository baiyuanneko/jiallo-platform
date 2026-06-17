<script setup lang="ts">
import { computed, ref, nextTick } from 'vue'
import { MessageOutlined, EditOutlined, DeleteOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import type { AgenticChatSession } from '@/types/chat'

const props = defineProps<{
  session: AgenticChatSession
  isActive: boolean
}>()

const emit = defineEmits<{
  singleClick: []
  doubleClick: []
  rename: [sessionId: string, newName: string]
  delete: [sessionId: string]
}>()

let clickTimer: ReturnType<typeof setTimeout> | null = null

function handleClick() {
  if (isRenaming.value) return
  if (clickTimer) {
    clearTimeout(clickTimer)
    clickTimer = null
    emit('doubleClick')
  } else {
    clickTimer = setTimeout(() => {
      clickTimer = null
      emit('singleClick')
    }, 250)
  }
}

const isRenaming = ref(false)
const renameInput = ref('')
const renameInputRef = ref<HTMLInputElement | null>(null)
const moreVisible = ref(false)

const relativeTime = computed(() => {
  const now = Date.now()
  const then = new Date(props.session.updateTime).getTime()
  const diffMs = now - then

  if (diffMs < 60_000) return '刚刚'
  if (diffMs < 3_600_000) return `${Math.floor(diffMs / 60_000)} 分钟前`
  if (diffMs < 86_400_000) return `${Math.floor(diffMs / 3_600_000)} 小时前`
  if (diffMs < 2_592_000_000) return `${Math.floor(diffMs / 86_400_000)} 天前`
  return new Date(props.session.updateTime).toLocaleDateString()
})

function startRename() {
  moreVisible.value = false
  renameInput.value = props.session.sessionName || ''
  isRenaming.value = true
  nextTick(() => {
    renameInputRef.value?.focus()
    renameInputRef.value?.select()
  })
}

function confirmRename() {
  const trimmed = renameInput.value.trim()
  if (trimmed && trimmed !== props.session.sessionName) {
    emit('rename', props.session.id, trimmed)
  }
  isRenaming.value = false
}

function cancelRename() {
  isRenaming.value = false
}

function handleDelete() {
  moreVisible.value = false
  emit('delete', props.session.id)
}
</script>

<template>
  <div
    class="session-list-item"
    :class="{ 'session-list-item--active': isActive }"
    @click="handleClick"
  >
    <div class="session-list-item__icon">
      <MessageOutlined />
    </div>
    <div class="session-list-item__content">
      <div v-if="isRenaming" class="session-list-item__rename" @click.stop>
        <input
          ref="renameInputRef"
          v-model="renameInput"
          class="session-list-item__rename-input"
          @keydown.enter="confirmRename"
          @keydown.escape="cancelRename"
          @blur="confirmRename"
        />
      </div>
      <div v-else class="session-list-item__name">{{ session.sessionName || '新对话' }}</div>
      <div class="session-list-item__time">{{ relativeTime }}</div>
    </div>
    <a-dropdown v-if="!isRenaming" v-model:open="moreVisible" trigger="click" placement="bottomRight">
      <button class="session-list-item__more-btn" @click.stop>
        <EllipsisOutlined />
      </button>
      <template #overlay>
        <a-menu @click.stop>
          <a-menu-item key="rename" @click="startRename">
            <EditOutlined />
            <span style="margin-left: 8px">重命名</span>
          </a-menu-item>
          <a-menu-item key="delete" danger @click="handleDelete">
            <DeleteOutlined />
            <span style="margin-left: 8px">删除</span>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<style scoped>
.session-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}

.session-list-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.session-list-item--active {
  background: #e6f4ff;
  border-left-color: #1677ff;
}

.session-list-item--active:hover {
  background: #e6f4ff;
}

.session-list-item__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.04);
  color: #8c8c8c;
  font-size: 14px;
}

.session-list-item--active .session-list-item__icon {
  background: rgba(22, 119, 255, 0.1);
  color: #1677ff;
}

.session-list-item__content {
  flex: 1;
  min-width: 0;
}

.session-list-item__name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-list-item__time {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
  margin-top: 2px;
}

.session-list-item__more-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid #e0e0e0;
  background: #ffffff;
  color: #8c8c8c;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  transition:
    color 0.15s,
    border-color 0.15s,
    background 0.15s;
}

.session-list-item__more-btn:hover {
  color: #595959;
  border-color: #bfbfbf;
}

.session-list-item__rename {
  display: flex;
}

.session-list-item__rename-input {
  width: 100%;
  border: 1px solid #1677ff;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  outline: none;
  font-family: inherit;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .session-list-item {
    padding: 12px;
  }
}
</style>
