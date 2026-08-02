<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import { adminGetTgBotConfigApi, adminSetTgBotConfigApi } from '@/api/apiAdmin'
import type { TgBotConfig, TgBotChatIdMode } from '@/types/admin'
import { ui } from '@/utils/ui'

const loading = ref(false)
const saveLoading = ref(false)

const enabled = ref(false)
const token = ref('')
const chatIdMode = ref<TgBotChatIdMode>('whitelist')
const whitelistChatIds = ref('')
const blacklistChatIds = ref('')

async function fetchConfig() {
  loading.value = true
  try {
    const res = await adminGetTgBotConfigApi()
    const data = res.data.data
    if (data) {
      enabled.value = data.enabled
      token.value = data.token ?? ''
      chatIdMode.value = data.chatIdMode ?? 'whitelist'
      whitelistChatIds.value = data.whitelistChatIds ?? ''
      blacklistChatIds.value = data.blacklistChatIds ?? ''
    }
  } catch (error) {
    console.error('获取 Telegram 机器人配置失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saveLoading.value = true
  try {
    const data: TgBotConfig = {
      enabled: enabled.value,
      token: token.value.trim(),
      chatIdMode: chatIdMode.value,
      whitelistChatIds: whitelistChatIds.value,
      blacklistChatIds: blacklistChatIds.value,
    }
    await adminSetTgBotConfigApi(data)
    ui.success('保存成功，机器人已按新配置重载')
    fetchConfig()
  } catch (error) {
    console.error('保存 Telegram 机器人配置失败:', error)
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<template>
  <div class="telegram-bot-config">
    <a-card class="content-card" :loading="loading">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">Telegram 机器人</span>
      </div>

      <div class="card-header">
        <h3>Telegram 通用机器人管理</h3>
      </div>

      <div class="form-list">
        <div class="form-item">
          <div class="form-label">启用状态</div>
          <div class="form-control">
            <a-switch v-model:checked="enabled" checked-children="启用" un-checked-children="禁用" />
            <span class="form-hint">默认禁用；保存后机器人会按新配置即时重载</span>
          </div>
        </div>

        <div class="form-item">
          <div class="form-label">机器人 Token</div>
          <div class="form-control">
            <a-input-password
              v-model:value="token"
              placeholder="从 @BotFather 获取的 Bot Token"
              style="max-width: 480px"
            />
          </div>
        </div>

        <div class="form-item">
          <div class="form-label">ChatId 处理模式</div>
          <div class="form-control">
            <a-radio-group v-model:value="chatIdMode" button-style="solid">
              <a-radio-button value="whitelist">白名单</a-radio-button>
              <a-radio-button value="blacklist">黑名单</a-radio-button>
            </a-radio-group>
            <span class="form-hint">
              白名单模式仅响应名单内 ChatId；黑名单模式响应名单外所有 ChatId
            </span>
          </div>
        </div>

        <div class="form-item">
          <div class="form-label">白名单 ChatId 列表</div>
          <div class="form-control">
            <a-textarea
              v-model:value="whitelistChatIds"
              :auto-size="{ minRows: 2, maxRows: 6 }"
              placeholder="逗号分隔，如：123456789, 987654321"
              style="max-width: 480px"
            />
          </div>
        </div>

        <div class="form-item">
          <div class="form-label">黑名单 ChatId 列表</div>
          <div class="form-control">
            <a-textarea
              v-model:value="blacklistChatIds"
              :auto-size="{ minRows: 2, maxRows: 6 }"
              placeholder="逗号分隔，如：123456789, 987654321"
              style="max-width: 480px"
            />
          </div>
        </div>

        <div class="form-actions">
          <a-button type="primary" :loading="saveLoading" @click="handleSave">
            <template #icon><SaveOutlined /></template>
            保存
          </a-button>
        </div>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.telegram-bot-config {
  min-height: 100%;
}

.content-card {
  min-height: 100%;
  border-radius: 0;
  box-shadow: none;
}

.page-path {
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.5;
}

.page-path-root {
  color: #262626;
  font-weight: 500;
}

.page-path-separator,
.page-path-current {
  color: #8c8c8c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.form-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 640px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

.form-control {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.form-hint {
  font-size: 12px;
  color: #8c8c8c;
}

.form-actions {
  padding-top: 4px;
}
</style>
