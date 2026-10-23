<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import { adminGetTgBotConfigApi, adminSetTgBotConfigApi, adminListLlmModelsApi } from '@/api/apiAdmin'
import type { TgBotConfig, TgBotChatIdMode, LlmModel } from '@/types/admin'
import { ui } from '@/utils/ui'

const loading = ref(false)
const saveLoading = ref(false)

const enabled = ref(false)
const token = ref('')
const chatIdMode = ref<TgBotChatIdMode>('whitelist')
const whitelistChatIds = ref('')
const blacklistChatIds = ref('')

// 功能开关
const chatLogEnabled = ref(false)
const webSearchEnabled = ref(false)
const chatEnabled = ref(false)
const rankEnabled = ref(false)
const chatModelId = ref<string | undefined>(undefined)
const chatSystemPrompt = ref('')

// /help、/about 中展示的机器人信息（留空保存则使用默认值）
const DEFAULT_BOT_NAME = 'Telegram 智能机器人'
const DEFAULT_BOT_UPDATE_DATE = '2026-10-24'
const botName = ref(DEFAULT_BOT_NAME)
const botUpdateDate = ref(DEFAULT_BOT_UPDATE_DATE)

const availableModels = ref<LlmModel[]>([])
const modelsLoading = ref(false)

// 活跃排行依赖消息记录：关闭记录时同步关闭排行
watch(chatLogEnabled, (value) => {
  if (!value) rankEnabled.value = false
})

async function fetchModels() {
  modelsLoading.value = true
  try {
    const res = await adminListLlmModelsApi({ pageNum: 1, pageSize: 200 })
    if (res.data.code === 0) {
      availableModels.value = res.data.data.records
    }
  } catch (error) {
    console.error('获取模型列表失败:', error)
  } finally {
    modelsLoading.value = false
  }
}

function getModelDisplayName(model: LlmModel) {
  return model.modelDisplayName || model.modelName
}

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
      chatLogEnabled.value = data.chatLogEnabled ?? false
      webSearchEnabled.value = data.webSearchEnabled ?? false
      chatEnabled.value = data.chatEnabled ?? false
      rankEnabled.value = (data.chatLogEnabled ?? false) && (data.rankEnabled ?? false)
      chatModelId.value = data.chatModelId || undefined
      chatSystemPrompt.value = data.chatSystemPrompt ?? ''
      botName.value = data.botName || DEFAULT_BOT_NAME
      botUpdateDate.value = data.botUpdateDate || DEFAULT_BOT_UPDATE_DATE
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
      chatLogEnabled: chatLogEnabled.value,
      webSearchEnabled: webSearchEnabled.value,
      chatEnabled: chatEnabled.value,
      rankEnabled: chatLogEnabled.value && rankEnabled.value,
      chatModelId: chatModelId.value ?? '',
      chatSystemPrompt: chatSystemPrompt.value,
      botName: botName.value.trim(),
      botUpdateDate: botUpdateDate.value.trim(),
    }
    await adminSetTgBotConfigApi(data)
    ui.success('保存成功，机器人将在约 10～20 秒内按新配置重载')
    fetchConfig()
  } catch (error) {
    console.error('保存 Telegram 机器人配置失败:', error)
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  fetchConfig()
  fetchModels()
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
            <span class="form-hint">已保存的 Token 打码显示，不修改则保持原值</span>
          </div>
        </div>

        <div class="form-item">
          <div class="form-label">机器人名称</div>
          <div class="form-control">
            <a-input
              v-model:value="botName"
              :placeholder="DEFAULT_BOT_NAME"
              :maxlength="64"
              style="max-width: 480px"
            />
            <span class="form-hint">在 /help、/about 中展示；留空则使用「{{ DEFAULT_BOT_NAME }}」</span>
          </div>
        </div>

        <div class="form-item">
          <div class="form-label">更新日期</div>
          <div class="form-control">
            <a-input
              v-model:value="botUpdateDate"
              :placeholder="DEFAULT_BOT_UPDATE_DATE"
              :maxlength="32"
              style="max-width: 480px"
            />
            <span class="form-hint">在 /help、/about 中展示；留空则使用「{{ DEFAULT_BOT_UPDATE_DATE }}」</span>
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

        <div class="form-item">
          <div class="form-label">功能列表</div>
          <div class="feature-list">
            <div class="feature-item">
              <a-switch v-model:checked="chatLogEnabled" size="small" />
              <div class="feature-info">
                <div class="feature-name">消息记录</div>
                <div class="feature-desc">
                  记录通过 ChatId 过滤的所有消息（含机器人回复）到 tg_chat_log，并建立全文检索分块。
                  群聊需在 @BotFather 中关闭隐私模式（/setprivacy → Disable，之后需将机器人移出并重新拉入群）或设为群管理员，否则只能收到命令消息。
                  开启前请告知群成员消息将被记录。
                </div>
              </div>
            </div>
            <div class="feature-item">
              <a-switch v-model:checked="webSearchEnabled" size="small" />
              <div class="feature-info">
                <div class="feature-name">网络搜索 <code>/websearch [--deepseek|--bocha] &lt;内容&gt;</code></div>
                <div class="feature-desc">默认使用 DeepSeek 网页搜索，不含网页抓取；每人每 15 秒限 1 次</div>
              </div>
            </div>
            <div class="feature-item">
              <a-switch v-model:checked="chatEnabled" size="small" />
              <div class="feature-info">
                <div class="feature-name">
                  智能问答 <code>/chat &lt;内容&gt;</code>（别名 <code>/ai</code>、<code>/llm</code>）
                </div>
                <div class="feature-desc">
                  一次性智能体调用（无记忆与上下文），可使用联网搜索、阅读网页、日期时间工具；每人每 15 秒限 1 次
                </div>
              </div>
            </div>
            <div class="feature-item">
              <a-switch v-model:checked="rankEnabled" size="small" :disabled="!chatLogEnabled" />
              <div class="feature-info">
                <div class="feature-name">活跃排行 <code>/rank</code></div>
                <div class="feature-desc">
                  群聊活跃成员排行（全部记录 Top 12、最近 14 日 Top 8）；依赖消息记录，仅统计开启记录之后的消息
                </div>
              </div>
            </div>
          </div>
        </div>

        <template v-if="chatEnabled">
          <div class="form-item">
            <div class="form-label">/chat 使用模型</div>
            <div class="form-control">
              <a-select
                v-model:value="chatModelId"
                :loading="modelsLoading"
                placeholder="选择 /chat 使用的模型"
                allow-clear
                show-search
                :filter-option="
                  (input: string, option: any) => option.label?.toLowerCase().includes(input.toLowerCase())
                "
                style="width: 100%; max-width: 480px"
              >
                <a-select-option
                  v-for="model in availableModels"
                  :key="model.id"
                  :value="model.id"
                  :label="getModelDisplayName(model)"
                >
                  {{ getModelDisplayName(model) }}
                </a-select-option>
              </a-select>
              <span v-if="!chatModelId" class="form-hint">未选择模型时 /chat 会提示未配置</span>
            </div>
          </div>

          <div class="form-item">
            <div class="form-label">/chat 系统提示词（可选）</div>
            <div class="form-control">
              <a-textarea
                v-model:value="chatSystemPrompt"
                :auto-size="{ minRows: 3, maxRows: 10 }"
                placeholder="留空使用内置提示词"
                style="max-width: 480px"
              />
            </div>
          </div>
        </template>

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

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 560px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.feature-item :deep(.ant-switch) {
  margin-top: 3px;
  flex-shrink: 0;
}

.feature-name {
  font-size: 14px;
  color: #262626;
}

.feature-name code {
  font-size: 12px;
  color: #595959;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 4px;
}

.feature-desc {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.6;
  color: #8c8c8c;
}
</style>
