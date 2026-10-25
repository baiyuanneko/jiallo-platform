<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { SaveOutlined, PlusOutlined, DeleteOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { adminGetTgBotConfigApi, adminSetTgBotConfigApi, adminListLlmModelsApi } from '@/api/apiAdmin'
import type { TgBotConfig, TgBotChatIdMode, LlmModel, TgBotChatFeatureOverride } from '@/types/admin'
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

// ==================== ChatID 特定配置 ====================

/** 单项功能在 ChatID 特定配置中的取值：跟随全局 / 强制开启 / 强制关闭 */
type OverrideState = 'inherit' | 'on' | 'off'

type FeatureKey = 'chatLog' | 'webSearch' | 'chat' | 'rank'

interface OverrideRow {
  key: number
  chatId: string
  remark: string
  chatLog: OverrideState
  webSearch: OverrideState
  chat: OverrideState
  rank: OverrideState
}

const featureColumns: { key: FeatureKey; label: string }[] = [
  { key: 'chatLog', label: '消息记录' },
  { key: 'webSearch', label: '网络搜索' },
  { key: 'chat', label: '智能问答' },
  { key: 'rank', label: '活跃排行' },
]

const overrideRows = ref<OverrideRow[]>([])
let overrideRowKey = 0

function toState(value: boolean | null | undefined): OverrideState {
  if (value === true) return 'on'
  if (value === false) return 'off'
  return 'inherit'
}

function fromState(state: OverrideState): boolean | null {
  if (state === 'on') return true
  if (state === 'off') return false
  return null
}

function globalValue(key: FeatureKey): boolean {
  switch (key) {
    case 'chatLog':
      return chatLogEnabled.value
    case 'webSearch':
      return webSearchEnabled.value
    case 'chat':
      return chatEnabled.value
    case 'rank':
      return chatLogEnabled.value && rankEnabled.value
  }
}

function resolveState(state: OverrideState, global: boolean): boolean {
  return state === 'inherit' ? global : state === 'on'
}

/** 该 ChatID 最终生效的功能（与后端 Settings.featuresFor 规则一致，排行依赖最终生效的消息记录） */
function effectiveValue(row: OverrideRow, key: FeatureKey): boolean {
  const chatLog = resolveState(row.chatLog, globalValue('chatLog'))
  if (key === 'chatLog') return chatLog
  if (key === 'rank') return chatLog && resolveState(row.rank, globalValue('rank'))
  return resolveState(row[key], globalValue(key))
}

function stateOptions(key: FeatureKey) {
  return [
    { value: 'inherit', label: `跟随全局（${globalValue(key) ? '开' : '关'}）` },
    { value: 'on', label: '开启' },
    { value: 'off', label: '关闭' },
  ]
}

function parseIdList(raw: string): Set<string> {
  return new Set(
    raw
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s !== ''),
  )
}

const whitelistSet = computed(() => parseIdList(whitelistChatIds.value))
const blacklistSet = computed(() => parseIdList(blacklistChatIds.value))

/** 按当前黑白名单判断该 ChatID 是否被允许（不被允许时特定配置不生效） */
function isRowChatAllowed(row: OverrideRow): boolean {
  const id = row.chatId.trim()
  if (!id) return true
  return chatIdMode.value === 'whitelist' ? whitelistSet.value.has(id) : !blacklistSet.value.has(id)
}

/** 全局或任一 ChatID 开启了智能问答时展示 /chat 模型与提示词配置 */
const chatUsedAnywhere = computed(
  () => chatEnabled.value || overrideRows.value.some((row) => effectiveValue(row, 'chat')),
)

function addOverrideRow() {
  overrideRows.value.push({
    key: ++overrideRowKey,
    chatId: '',
    remark: '',
    chatLog: 'inherit',
    webSearch: 'inherit',
    chat: 'inherit',
    rank: 'inherit',
  })
}

function removeOverrideRow(key: number) {
  overrideRows.value = overrideRows.value.filter((row) => row.key !== key)
}

function toOverrideRows(list: TgBotChatFeatureOverride[] | undefined): OverrideRow[] {
  return (list ?? []).map((item) => ({
    key: ++overrideRowKey,
    chatId: item.chatId ?? '',
    remark: item.remark ?? '',
    chatLog: toState(item.chatLogEnabled),
    webSearch: toState(item.webSearchEnabled),
    chat: toState(item.chatEnabled),
    rank: toState(item.rankEnabled),
  }))
}

/** 校验 ChatID 特定配置，返回错误信息；通过返回 null */
function validateOverrideRows(): string | null {
  const seen = new Set<string>()
  for (const row of overrideRows.value) {
    const id = row.chatId.trim()
    if (!/^-?\d{1,15}$/.test(id)) {
      return `ChatID 特定配置中「${id || '（空）'}」不是有效的 ChatID`
    }
    if (seen.has(id)) {
      return `ChatID 特定配置中存在重复的 ChatID：${id}`
    }
    seen.add(id)
  }
  return null
}

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
      overrideRows.value = toOverrideRows(data.chatFeatureOverrides)
    }
  } catch (error) {
    console.error('获取 Telegram 机器人配置失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const overrideError = validateOverrideRows()
  if (overrideError) {
    ui.error(overrideError)
    return
  }
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
      chatFeatureOverrides: overrideRows.value.map((row) => ({
        chatId: row.chatId.trim(),
        remark: row.remark.trim(),
        chatLogEnabled: fromState(row.chatLog),
        webSearchEnabled: fromState(row.webSearch),
        chatEnabled: fromState(row.chat),
        rankEnabled: fromState(row.rank),
      })),
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
          <div class="form-label">全局功能开关</div>
          <div class="form-hint">适用于所有允许的聊天；命令菜单按全局开关生成。可在下方 ChatID 特定配置中按聊天覆盖</div>
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

        <div class="form-item">
          <div class="form-label">ChatID 特定配置</div>
          <div class="form-hint">
            为指定 ChatID 覆盖全局功能开关，未设置的项跟随全局；不受黑白名单豁免（被过滤的聊天配置不生效）。
            在目标聊天中发送 <code>/jiallo_chatid</code> 可获取其 ChatID（该命令无视黑白名单）。
            对当前聊天未启用的命令会回复「此命令对于当前聊天未启用」
          </div>
          <div class="override-list">
            <div v-for="row in overrideRows" :key="row.key" class="override-row">
              <div class="override-row-head">
                <a-input
                  v-model:value="row.chatId"
                  placeholder="ChatID，如 -1001234567890"
                  :maxlength="20"
                  style="width: 220px"
                />
                <a-input v-model:value="row.remark" placeholder="备注（可选）" :maxlength="64" style="width: 200px" />
                <a-tooltip v-if="row.chatId.trim() && !isRowChatAllowed(row)" title="该 ChatID 当前不在黑白名单允许范围内，配置不会生效">
                  <WarningOutlined class="override-warning" />
                </a-tooltip>
                <a-button type="text" danger @click="removeOverrideRow(row.key)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </div>
              <div class="override-features">
                <div v-for="col in featureColumns" :key="col.key" class="override-feature">
                  <span class="override-feature-label">{{ col.label }}</span>
                  <a-select
                    v-model:value="row[col.key]"
                    :options="stateOptions(col.key)"
                    :disabled="col.key === 'rank' && !effectiveValue(row, 'chatLog')"
                    size="small"
                    style="width: 150px"
                  />
                  <a-tag :color="effectiveValue(row, col.key) ? 'success' : 'default'" class="override-effective">
                    {{ effectiveValue(row, col.key) ? '生效：开' : '生效：关' }}
                  </a-tag>
                </div>
              </div>
            </div>
            <div>
              <a-button type="dashed" @click="addOverrideRow">
                <template #icon><PlusOutlined /></template>
                添加 ChatID
              </a-button>
            </div>
          </div>
        </div>

        <template v-if="chatUsedAnywhere">
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

.form-hint code {
  font-size: 12px;
  color: #595959;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 4px;
}

.override-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 760px;
}

.override-row {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.override-row-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.override-warning {
  color: #faad14;
  font-size: 16px;
}

.override-features {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 8px 16px;
}

.override-feature {
  display: flex;
  align-items: center;
  gap: 8px;
}

.override-feature-label {
  width: 56px;
  font-size: 13px;
  color: #595959;
  flex-shrink: 0;
}

.override-effective {
  margin-inline-end: 0;
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
