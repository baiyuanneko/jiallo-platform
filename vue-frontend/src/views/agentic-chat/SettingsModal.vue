<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { SettingOutlined, InfoCircleOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { usePreferences } from '@/composables/usePreferences'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const open = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

type TabKey = 'appearance' | 'features' | 'about'
const activeTab = ref<TabKey>('appearance')

const { showTokenCount, showMessageTime, wideMode, showQuoteButton, rememberToolSelection, rememberAgentType, enableMultiTab, ctrlEnterToSend, simplifiedProcess, rememberReasoningEffort } = usePreferences()

const windowWidth = ref(640)
onMounted(() => {
  windowWidth.value = window.innerWidth
})

const modalWidth = computed(() => {
  return windowWidth.value <= 768 ? windowWidth.value - 24 : 640
})
</script>

<template>
  <a-modal
    v-model:open="open"
    title="设置"
    :footer="null"
    :width="modalWidth"
    centered
    destroy-on-close
    :body-style="{ paddingTop: '24px' }"
    @cancel="open = false"
  >
    <div class="settings-modal__body">
      <div class="settings-modal__sidebar">
        <button
          class="settings-modal__tab"
          :class="{ 'settings-modal__tab--active': activeTab === 'appearance' }"
          @click="activeTab = 'appearance'"
        >
          <SettingOutlined />
          <span>界面</span>
        </button>
        <button
          class="settings-modal__tab"
          :class="{ 'settings-modal__tab--active': activeTab === 'features' }"
          @click="activeTab = 'features'"
        >
          <ThunderboltOutlined />
          <span>功能</span>
        </button>
        <button
          class="settings-modal__tab"
          :class="{ 'settings-modal__tab--active': activeTab === 'about' }"
          @click="activeTab = 'about'"
        >
          <InfoCircleOutlined />
          <span>关于</span>
        </button>
      </div>

      <div class="settings-modal__content">
        <template v-if="activeTab === 'appearance'">
          <h3 class="settings-modal__section-title">显示设置</h3>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">Token 计数</div>
              <div class="settings-modal__item-desc">在消息气泡中显示 Token 用量信息</div>
            </div>
            <a-switch v-model:checked="showTokenCount" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">消息发送时间</div>
              <div class="settings-modal__item-desc">在消息气泡中显示发送时间</div>
            </div>
            <a-switch v-model:checked="showMessageTime" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">左对齐（宽屏模式）</div>
              <div class="settings-modal__item-desc">消息气泡靠左对齐，不限制最大宽度</div>
            </div>
            <a-switch v-model:checked="wideMode" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">多标签页支持</div>
              <div class="settings-modal__item-desc">开启后在顶部显示标签页，支持同时管理多个对话</div>
            </div>
            <a-switch v-model:checked="enableMultiTab" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">简化的模型中间过程显示</div>
              <div class="settings-modal__item-desc">以更简单和结构化的方式显示模型的思考过程与 Tool Call 过程</div>
            </div>
            <a-switch v-model:checked="simplifiedProcess" />
          </div>
        </template>

        <template v-if="activeTab === 'features'">
          <h3 class="settings-modal__section-title">功能</h3>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">选中时显示引用按钮</div>
              <div class="settings-modal__item-desc">选中消息文字时显示引用按钮，点击后回复该段内容</div>
            </div>
            <a-switch v-model:checked="showQuoteButton" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">记住此前选择的模型能力</div>
              <div class="settings-modal__item-desc">新建对话时沿用上次使用的模型能力配置</div>
            </div>
            <a-switch v-model:checked="rememberToolSelection" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">记住此前的调用模式选择</div>
              <div class="settings-modal__item-desc">新建对话时沿用上次使用的调用模式配置</div>
            </div>
            <a-switch v-model:checked="rememberAgentType" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">使用 Ctrl + Enter 发送消息</div>
              <div class="settings-modal__item-desc">回车键用于换行，Ctrl+Enter 键用于发送消息</div>
            </div>
            <a-switch v-model:checked="ctrlEnterToSend" />
          </div>
          <div class="settings-modal__item">
            <div class="settings-modal__item-text">
              <div class="settings-modal__item-label">记住模型推理级别设置</div>
              <div class="settings-modal__item-desc">新建对话时沿用上次使用的推理级别配置</div>
            </div>
            <a-switch v-model:checked="rememberReasoningEffort" />
          </div>
        </template>

        <template v-if="activeTab === 'about'">
          <h3 class="settings-modal__section-title">关于</h3>
          <div class="settings-modal__about">
            <div class="settings-modal__about-row">
              <span class="settings-modal__about-label">应用名称</span>
              <span class="settings-modal__about-value">Jiallo Chat</span>
            </div>
            <div class="settings-modal__about-row">
              <span class="settings-modal__about-label">版本</span>
              <span class="settings-modal__about-value">std20260427</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.settings-modal__body {
  display: flex;
  min-height: 300px;
  margin: -24px;
}

.settings-modal__sidebar {
  width: 160px;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
  padding: 12px 0;
  display: flex;
  flex-direction: column;
}

.settings-modal__tab {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  border: none;
  background: transparent;
  padding: 10px 20px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;
}

.settings-modal__tab:hover {
  background: #f5f5f5;
  color: #262626;
}

.settings-modal__tab--active {
  background: #e6f4ff;
  color: #1677ff;
  font-weight: 500;
}

.settings-modal__tab--active:hover {
  background: #e6f4ff;
  color: #1677ff;
}

.settings-modal__content {
  flex: 1;
  padding: 24px;
}

.settings-modal__section-title {
  margin: 0 0 20px;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.settings-modal__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 0;
}

.settings-modal__item + .settings-modal__item {
  border-top: 1px solid #f5f5f5;
}

.settings-modal__item-text {
  flex: 1;
}

.settings-modal__item-label {
  font-size: 14px;
  color: #262626;
  margin-bottom: 4px;
}

.settings-modal__item-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.settings-modal__about-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.settings-modal__about-label {
  color: #8c8c8c;
  font-size: 14px;
}

.settings-modal__about-value {
  color: #262626;
  font-size: 14px;
}

@media (max-width: 768px) {
  .settings-modal__body {
    flex-direction: column;
    min-height: auto;
    margin: -12px -24px;
  }

  .settings-modal__sidebar {
    width: 100%;
    flex-direction: row;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    padding: 0;
    overflow-x: auto;
  }

  .settings-modal__tab {
    flex: 1;
    justify-content: center;
    text-align: center;
    white-space: nowrap;
    padding: 12px 8px;
  }

  .settings-modal__content {
    padding: 16px;
  }
}
</style>
