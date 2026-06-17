<script setup lang="ts">
import { computed, h, ref, watch, nextTick, onMounted } from 'vue'
import { LoadingOutlined, MenuOutlined, RobotOutlined, ShareAltOutlined, DownOutlined, SyncOutlined, CopyOutlined, StopOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { setDefaultModelApi, getModelIconUrl } from '@/api/apiChat'
import { ui } from '@/utils/ui'
import { Modal } from 'ant-design-vue'
import ChatMessageList from './ChatMessageList.vue'
import ChatInput from './ChatInput.vue'

const chatStore = useChatStore()
const userStore = useUserStore()

const modelPopoverVisible = ref(false)
const settingDefaultModel = ref(false)
const modelSearchKeyword = ref('')
const modelIconFailed = ref(false)
const modelSearchInputRef = ref<HTMLInputElement | null>(null)
const chatInputRef = ref<any>(null)

function focusChatInput() {
  if (window.innerWidth > 768) {
    chatInputRef.value?.focusInput()
  }
}

onMounted(() => {
  // 桌面端首次加载时自动聚焦输入框
  focusChatInput()
})

const CUSTOM_MODEL_NAMES = new Set(['deepseek-v4-pro', 'deepseek-v4-flash'])

function getModelNameForCustom(m: { modelDisplayName?: string | null; modelName: string }): string | null {
  if (m.modelDisplayName && CUSTOM_MODEL_NAMES.has(m.modelDisplayName)) return m.modelDisplayName
  if (CUSTOM_MODEL_NAMES.has(m.modelName)) return m.modelName
  return null
}

function hasCustomModelName(m: { modelDisplayName?: string | null; modelName: string }): boolean {
  return getModelNameForCustom(m) !== null
}

function getCustomModelSrc(m: { modelDisplayName?: string | null; modelName: string }): string {
  const name = getModelNameForCustom(m)
  return `/images/${name}.png`
}

const currentModelLabel = computed(() => {
  const model = chatStore.availableModels.find((m) => m.id === chatStore.currentModelId)
  return model?.modelDisplayName || model?.modelName || ''
})

const currentModel = computed(() => {
  return chatStore.availableModels.find((m) => m.id === chatStore.currentModelId) || null
})

const currentModelIsVerified = computed(() => {
  return currentModel.value?.isVerifiedModel === true
})

const filteredModels = computed(() => {
  const kw = modelSearchKeyword.value.trim().toLowerCase()
  let list = chatStore.availableModels
  if (kw) {
    list = list.filter(
      (m) =>
        (m.modelDisplayName || '').toLowerCase().includes(kw) ||
        (m.modelName || '').toLowerCase().includes(kw) ||
        (m.providerName || '').toLowerCase().includes(kw),
    )
  }
  // Sort: current selected model first, then verified models, then rest
  const currentId = chatStore.currentModelId
  return [...list].sort((a, b) => {
    if (a.id === currentId) return -1
    if (b.id === currentId) return 1
    if (a.isVerifiedModel === true && b.isVerifiedModel !== true) return -1
    if (b.isVerifiedModel === true && a.isVerifiedModel !== true) return 1
    return 0
  })
})

watch(modelPopoverVisible, (open) => {
  if (!open) {
    modelSearchKeyword.value = ''
  } else if (window.innerWidth > 768) {
    // 非移动端：弹出后自动聚焦搜索框（a-popover 有入场动画，需延迟确保 DOM 就绪）
    setTimeout(() => modelSearchInputRef.value?.focus(), 100)
  }
})

const isLoading = computed(() => chatStore.getActiveTab()?.isLoading ?? false)

const showEmptyState = computed(() => {
  if (!chatStore.activeTabId) return true
  return (
    !chatStore.currentSessionId &&
    chatStore.currentMessages.length === 0 &&
    !chatStore.streamingMessage
  )
})

const modelIconUrl = computed(() => {
  if (!chatStore.currentModelId) return ''
  const model = chatStore.availableModels.find((m) => m.id === chatStore.currentModelId)
  return getModelIconUrl(chatStore.currentModelId, model?.updateTime)
})

watch(
  () => chatStore.currentModelId,
  () => {
    modelIconFailed.value = false
  },
)

async function handleSetDefaultModel() {
  if (!chatStore.currentModelId || settingDefaultModel.value) return
  settingDefaultModel.value = true
  try {
    await setDefaultModelApi(chatStore.currentModelId)
    if (userStore.userInfo) userStore.userInfo.preferredModelId = chatStore.currentModelId
    ui.success('已设为默认模型')
  } catch {
    ui.error('设置默认模型失败')
  } finally {
    settingDefaultModel.value = false
  }
}

const isShared = computed(() => chatStore.getActiveTab()?.isShared ?? false)
const shareLink = ref('')
const shareResultVisible = ref(false)
const shareLoading = ref(false)
const shareConfirmVisible = ref(false)
const shareTextOnly = ref(true)
const reshareConfirmVisible = ref(false)
const reshareLoading = ref(false)

const reshareFilterSensitive = computed(() => {
  const v = chatStore.getActiveTab()?.shareTextContentOnly
  return v === null || v === undefined ? true : v
})

function handleShare() {
  shareTextOnly.value = true
  shareConfirmVisible.value = true
}

async function confirmShare() {
  shareConfirmVisible.value = false
  shareLoading.value = true
  const link = await chatStore.shareCurrentSession(shareTextOnly.value)
  shareLoading.value = false
  if (link) {
    shareLink.value = link
    shareResultVisible.value = true
  }
}

async function handleCopyShareResultLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ui.success('链接已复制到剪贴板')
  } catch {
    ui.error('复制失败')
  }
}

function handleUnshare() {
  Modal.confirm({
    title: '取消分享',
    content: '确定要取消分享此会话吗？已分享的链接将无法访问。',
    okText: '取消分享',
    okType: 'danger',
    cancelText: '返回',
    centered: true,
    onOk: () => chatStore.unshareCurrentSession(),
  })
}

function handleReshare() {
  reshareConfirmVisible.value = true
}

async function confirmReshare() {
  reshareConfirmVisible.value = false
  reshareLoading.value = true
  await chatStore.reshareCurrentSession()
  reshareLoading.value = false
}

async function handleCopyShareLink() {
  const tab = chatStore.getActiveTab()
  if (!tab?.sharedSessionId) return
  try {
    const link = `${window.location.origin}/app/agentic-chat-ui/shared/${tab.sharedSessionId}`
    await navigator.clipboard.writeText(link)
    ui.success('链接已复制到剪贴板')
  } catch {
    ui.error('复制失败')
  }
}
</script>

<template>
  <div class="chat-panel">
    <header class="chat-panel__header">
      <button class="chat-panel__icon-btn chat-panel__menu-btn" @click="chatStore.sidebarVisible = true">
        <MenuOutlined />
      </button>

      <!-- Model selector in header -->
      <a-popover
        v-model:open="modelPopoverVisible"
        trigger="click"
        placement="bottomLeft"
        overlay-class-name="chat-panel__popover"
      >
        <template #content>
          <div class="model-menu">
            <div class="model-menu__search">
              <input
                ref="modelSearchInputRef"
                v-model="modelSearchKeyword"
                class="model-menu__search-input"
                placeholder="搜索模型..."
              />
            </div>
            <div class="model-menu__list">
              <div
                v-for="m in filteredModels"
                :key="m.id"
                class="model-menu__item"
                :class="{ 'model-menu__item--active': chatStore.currentModelId === m.id }"
                  @click="
                    chatStore.setActiveModel(m.id);
                    modelPopoverVisible = false;
                    focusChatInput()
                  "
              >
                <img
                  class="model-menu__icon"
                  :src="getModelIconUrl(m.id, m.updateTime)"
                  alt=""
                  loading="lazy"
                  @error="($event.target as HTMLImageElement).style.display = 'none'"
                />
                <span class="model-menu__info">
                  <span class="model-menu__label">
                    <img
                      v-if="hasCustomModelName(m)"
                      :src="getCustomModelSrc(m)"
                      class="model-menu__custom-label"
                      alt=""
                    />
                    <span v-else class="model-menu__label-text">{{ m.modelDisplayName || m.modelName }}</span>
                    <a-tooltip
                      v-if="m.isVerifiedModel === true"
                      title="此模型已经过验证，具有较佳的可靠性和使用体验，且与 Jiallo 配合良好。"
                      placement="right"
                      :mouseenter-delay="0.3"
                    >
                      <span class="model-menu__verified">
                        <svg viewBox="0 0 24 24" width="14" height="14">
                          <path fill="#1677ff" d="M22.5 12.5c0-1.58-.875-2.95-2.148-3.6.154-.435.238-.905.238-1.4 0-2.21-1.71-3.998-3.818-3.998-.47 0-.92.084-1.336.25C14.818 2.415 13.51 1.5 12 1.5s-2.816.917-3.437 2.25c-.415-.165-.866-.25-1.336-.25-2.11 0-3.818 1.79-3.818 4 0 .494.083.964.237 1.4-1.272.65-2.147 2.018-2.147 3.6 0 1.495.782 2.798 1.942 3.486-.02.17-.032.34-.032.514 0 2.21 1.708 4 3.818 4 .47 0 .92-.086 1.335-.25.62 1.334 1.926 2.25 3.437 2.25 1.512 0 2.818-.916 3.437-2.25.415.163.865.248 1.336.248 2.11 0 3.818-1.79 3.818-4 0-.174-.012-.344-.033-.513 1.158-.687 1.943-1.99 1.943-3.484zm-6.616-3.334l-4.334 6.5c-.145.217-.382.334-.625.334-.143 0-.288-.04-.416-.126l-.115-.094-2.415-2.415c-.293-.293-.293-.768 0-1.06s.768-.294 1.06 0l1.77 1.767 3.825-5.74c.23-.345.696-.436 1.04-.207.346.23.44.696.21 1.04z"/>
                        </svg>
                      </span>
                    </a-tooltip>
                  </span>
                  <span class="model-menu__provider">{{ m.providerName }}</span>
                </span>
                <span v-if="chatStore.currentModelId === m.id" class="model-menu__check">✓</span>
              </div>
              <div v-if="filteredModels.length === 0 && chatStore.modelsLoaded" class="model-menu__empty">
                无匹配模型
              </div>
              <div
                v-if="!chatStore.modelsLoaded"
                class="model-menu__item model-menu__item--loading"
              >
                加载中...
              </div>
            </div>
          </div>
        </template>
        <button class="chat-panel__model-btn">
          <img
            v-if="currentModel && hasCustomModelName(currentModel)"
            :src="getCustomModelSrc(currentModel)"
            class="chat-panel__model-custom-label"
            alt=""
          />
          <span v-else class="chat-panel__model-name">{{ currentModelLabel || '选择模型' }}</span>
          <a-tooltip
            v-if="currentModelIsVerified"
            title="此模型已经过验证，具有较佳的可靠性和使用体验，且与 Jiallo 配合良好。"
            placement="bottom"
            :mouseenter-delay="0.3"
          >
            <span class="chat-panel__model-verified">
              <svg viewBox="0 0 24 24" width="14" height="14">
                <path fill="#1677ff" d="M22.5 12.5c0-1.58-.875-2.95-2.148-3.6.154-.435.238-.905.238-1.4 0-2.21-1.71-3.998-3.818-3.998-.47 0-.92.084-1.336.25C14.818 2.415 13.51 1.5 12 1.5s-2.816.917-3.437 2.25c-.415-.165-.866-.25-1.336-.25-2.11 0-3.818 1.79-3.818 4 0 .494.083.964.237 1.4-1.272.65-2.147 2.018-2.147 3.6 0 1.495.782 2.798 1.942 3.486-.02.17-.032.34-.032.514 0 2.21 1.708 4 3.818 4 .47 0 .92-.086 1.335-.25.62 1.334 1.926 2.25 3.437 2.25 1.512 0 2.818-.916 3.437-2.25.415.163.865.248 1.336.248 2.11 0 3.818-1.79 3.818-4 0-.174-.012-.344-.033-.513 1.158-.687 1.943-1.99 1.943-3.484zm-6.616-3.334l-4.334 6.5c-.145.217-.382.334-.625.334-.143 0-.288-.04-.416-.126l-.115-.094-2.415-2.415c-.293-.293-.293-.768 0-1.06s.768-.294 1.06 0l1.77 1.767 3.825-5.74c.23-.345.696-.436 1.04-.207.346.23.44.696.21 1.04z"/>
              </svg>
            </span>
          </a-tooltip>
          <svg class="chat-panel__model-arrow" width="12" height="12" viewBox="0 0 12 12">
            <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </a-popover>

      <button
        class="chat-panel__default-btn"
        :disabled="!chatStore.currentModelId || settingDefaultModel"
        @click="handleSetDefaultModel"
      >
        <span class="chat-panel__default-text">{{ settingDefaultModel ? '设置中...' : '设为默认' }}</span>
        <span class="chat-panel__default-text-mobile">{{ settingDefaultModel ? '设置中...' : '设置为默认模型' }}</span>
      </button>

      <!-- 分享按钮 -->
      <template v-if="chatStore.currentSessionId">
        <a-dropdown v-if="isShared" trigger="click" placement="bottomRight">
          <button class="chat-panel__share-btn chat-panel__share-btn--shared">
            <ShareAltOutlined />
            <span class="chat-panel__share-text">已分享此对话</span>
            <span class="chat-panel__share-text-mobile">分享</span>
            <DownOutlined style="font-size: 10px; margin-left: 2px" />
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="reshare" @click="handleReshare">
                <SyncOutlined /> 同步最新消息
              </a-menu-item>
              <a-menu-item key="copy-link" @click="handleCopyShareLink">
                <CopyOutlined /> 复制分享链接
              </a-menu-item>
              <a-menu-item key="unshare" danger @click="handleUnshare">
                <StopOutlined /> 取消分享
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <button v-else class="chat-panel__share-btn" @click="handleShare">
          <ShareAltOutlined />
          <span class="chat-panel__share-text">分享此对话</span>
          <span class="chat-panel__share-text-mobile">分享</span>
        </button>
      </template>

      <button class="chat-panel__new-btn" @click="chatStore.startNewSession()">
        <PlusOutlined />
        <span>新对话</span>
      </button>
      <span class="chat-panel__spacer" />
    </header>

    <ChatMessageList v-if="!showEmptyState && !isLoading" />

    <div v-else-if="isLoading" class="chat-panel__loading">
      <a-spin :indicator="h(LoadingOutlined)" tip="加载中..." />
    </div>

    <div v-else class="chat-panel__empty">
      <img
        v-if="modelIconUrl && !modelIconFailed"
        :src="modelIconUrl"
        alt=""
        class="chat-panel__empty-model-icon"
        @error="modelIconFailed = true"
      />
      <RobotOutlined v-else class="chat-panel__empty-fallback-icon" />
      <h4 class="chat-panel__empty-title">开始新对话</h4>
      <p class="chat-panel__empty-desc">输入消息开始与 AI 助手对话</p>
    </div>

    <ChatInput ref="chatInputRef" />

    <!-- 分享确认弹窗 -->
    <a-modal
      v-model:open="shareConfirmVisible"
      title="分享此对话"
      ok-text="确认分享"
      cancel-text="取消"
      centered
      :confirm-loading="shareLoading"
      @ok="confirmShare"
    >
      <p style="margin: 0 0 12px; color: #595959; font-size: 14px;">
        仅此会话到目前为止的消息会被分享。如果需要分享之后的消息，请使用"重新分享"功能。
      </p>
      <a-checkbox v-model:checked="shareTextOnly">
        仅分享文本内容（过滤推理过程和工具结果）
      </a-checkbox>
    </a-modal>

    <!-- 同步最新消息确认弹窗 -->
    <a-modal
      v-model:open="reshareConfirmVisible"
      title="同步最新消息"
      ok-text="确认同步"
      cancel-text="取消"
      centered
      :confirm-loading="reshareLoading"
      @ok="confirmReshare"
    >
      <p style="margin: 0 0 8px; color: #595959; font-size: 14px;">
        将用当前会话的最新消息覆盖分享链接中的内容。
      </p>
      <p style="margin: 0 0 8px; color: #8c8c8c; font-size: 13px;">
        当前分享模式：{{ reshareFilterSensitive ? '仅文本内容（不含推理过程和工具结果）' : '完整内容（含推理过程和工具结果）' }}
      </p>
      <p style="margin: 0; color: #8c8c8c; font-size: 13px;">
        若需修改分享模式，请取消分享后重新分享。
      </p>
    </a-modal>

    <!-- 分享成功弹窗 -->
    <a-modal
      v-model:open="shareResultVisible"
      title="分享成功"
      :footer="null"
      centered
      :width="520"
    >
      <p style="margin: 0 0 12px; color: #595959; font-size: 14px;">
        以下链接可公开访问此对话，无需登录。
      </p>
      <div class="chat-panel__share-link-box">
        <input
          class="chat-panel__share-link-input"
          :value="shareLink"
          readonly
          @focus="($event.target as HTMLInputElement).select()"
        />
        <button class="chat-panel__share-link-copy" @click="handleCopyShareResultLink">
          <CopyOutlined /> 复制
        </button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-panel__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #ffffff;
  flex-shrink: 0;
}

.chat-panel__icon-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: #595959;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.chat-panel__icon-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.chat-panel__menu-btn {
  display: none;
}

.chat-panel__model-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s;
}

.chat-panel__model-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.chat-panel__model-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
}

.chat-panel__model-custom-label {
  height: 25px;
  display: block;
}

.chat-panel__model-verified {
  display: inline-flex;
  align-items: center;
  margin-left: 2px;
  cursor: help;
}

.chat-panel__model-arrow {
  color: #bfbfbf;
  flex-shrink: 0;
}

.chat-panel__spacer {
  flex: 1;
}

.chat-panel__default-btn {
  border: none;
  background: transparent;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 6px;
  font-family: inherit;
  line-height: 1;
  white-space: nowrap;
  transition: color 0.2s;
}

.chat-panel__default-btn:hover:not(:disabled) {
  color: #1677ff;
}

.chat-panel__default-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chat-panel__share-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: #1677ff;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  font-family: inherit;
  white-space: nowrap;
  transition: background 0.2s;
  flex-shrink: 0;
}

.chat-panel__share-btn:hover {
  background: #4096ff;
}

.chat-panel__share-btn--shared {
  background: #e6f4ff;
  color: #1677ff;
}

.chat-panel__share-btn--shared:hover {
  background: #bae0ff;
  color: #0958d9;
}

.chat-panel__share-link-box {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-panel__share-link-input {
  flex: 1;
  min-width: 0;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  color: #262626;
  background: #fafafa;
  outline: none;
}

.chat-panel__share-link-input:focus {
  border-color: #1677ff;
}

.chat-panel__share-link-copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: #1677ff;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 6px;
  font-family: inherit;
  white-space: nowrap;
  transition: background 0.2s;
  flex-shrink: 0;
}

.chat-panel__share-link-copy:hover {
  background: #4096ff;
}

.chat-panel__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
}

.chat-panel__loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-panel__empty-model-icon {
  width: 64px;
  height: 64px;
  object-fit: contain;
  border-radius: 16px;
}

.chat-panel__empty-fallback-icon {
  font-size: 40px;
  color: #bfbfbf;
}

.chat-panel__empty-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.chat-panel__empty-desc {
  margin: 0;
  font-size: 14px;
  color: #8c8c8c;
}

.chat-panel__share-text-mobile,
.chat-panel__default-text-mobile {
  display: none;
}

.chat-panel__new-btn {
  display: none;
}

@media (max-width: 768px) {
  .chat-panel__header {
    padding: 6px 12px;
    gap: 2px 6px;
    flex-wrap: wrap;
  }

  .chat-panel__menu-btn {
    display: flex;
  }

  .chat-panel__model-name {
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .chat-panel__spacer {
    display: none;
  }

  .chat-panel__share-text,
  .chat-panel__share-text-mobile {
    display: none;
  }

  .chat-panel__default-text {
    display: none;
  }

  .chat-panel__default-text-mobile {
    display: inline;
  }

  .chat-panel__share-btn {
    border: none;
    background: transparent;
    color: #595959;
    padding: 0;
    width: 32px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
  }

  .chat-panel__share-btn:hover {
    background: rgba(0, 0, 0, 0.04);
    color: #262626;
  }

  .chat-panel__share-btn--shared {
    border: none;
    background: transparent;
    color: #595959;
  }

  .chat-panel__share-btn--shared:hover {
    background: rgba(0, 0, 0, 0.04);
    color: #262626;
  }

  .chat-panel__default-btn {
    order: 99;
    width: auto;
    margin-left: 42px;
    font-size: 12px;
    color: #8c8c8c;
  }

  .chat-panel__new-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    border: 1px solid #d9d9d9;
    background: #fff;
    color: #595959;
    font-size: 13px;
    cursor: pointer;
    padding: 4px 10px;
    border-radius: 6px;
    font-family: inherit;
    white-space: nowrap;
    flex-shrink: 0;
    transition: background 0.2s;
  }

  .chat-panel__new-btn:hover {
    background: #f5f5f5;
    color: #262626;
  }

  .chat-panel__new-btn span {
    font-size: 13px;
  }

  .chat-panel__empty {
    padding: 24px 16px;
  }
}
</style>

<style>
/* Global styles for popover (scoped doesn't apply to popover content) */
.chat-panel__popover .ant-popover-inner {
  padding: 4px 0 !important;
  border-radius: 10px !important;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.1),
    0 1px 3px rgba(0, 0, 0, 0.06) !important;
}

.chat-panel__popover .ant-popover-inner-content {
  padding: 0 !important;
}

.model-menu {
  width: 240px;
  display: flex;
  flex-direction: column;
}

.model-menu__search {
  padding: 8px 8px 4px;
}

.model-menu__search-input {
  width: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 13px;
  color: #262626;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.model-menu__search-input::placeholder {
  color: #bfbfbf;
}

.model-menu__search-input:focus {
  border-color: #1677ff;
}

.model-menu__list {
  max-height: 280px;
  overflow-y: auto;
  padding: 4px 0;
}

.model-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  font-size: 13px;
  color: #262626;
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 6px;
  margin: 0 4px;
}

.model-menu__icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  object-fit: contain;
  flex-shrink: 0;
}

.model-menu__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.model-menu__item:hover {
  background: #f5f5f5;
}

.model-menu__item--active {
  color: #1677ff;
}

.model-menu__item--active .model-menu__label {
  font-weight: 500;
}

.model-menu__item--loading {
  color: #bfbfbf;
  pointer-events: none;
}

.model-menu__empty {
  padding: 16px 12px;
  text-align: center;
  font-size: 13px;
  color: #bfbfbf;
}

.model-menu__check {
  margin-left: auto;
  color: #1677ff;
  font-size: 14px;
  font-weight: 600;
}

.model-menu__custom-label {
  height: 25px;
  display: block;
}

.model-menu__label {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
  line-height: 1.3;
}

.model-menu__label-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0;
}

.model-menu__verified {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  cursor: help;
}

.model-menu__provider {
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
