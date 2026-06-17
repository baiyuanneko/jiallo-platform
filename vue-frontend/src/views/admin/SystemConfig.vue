<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { EditOutlined } from '@ant-design/icons-vue'
import {
  adminGetAllConfigApi,
  adminAllowRegisterSwitchApi,
  adminSimpleTaskModelSwitchApi,
  adminListLlmModelsApi,
  adminGetDigitalBynPromptApi,
  adminSetDigitalBynPromptApi,
} from '@/api/apiAdmin'
import type { SystemConfig, LlmModel } from '@/types/admin'
import { ui } from '@/utils/ui'

const configList = ref<SystemConfig[]>([])
const loading = ref(false)
const allowRegister = ref(true)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editAllowRegister = ref(true)

// 简单任务模型配置
const simpleTaskModelId = ref('')
const simpleTaskModelName = ref('未配置')
const editModelDialogVisible = ref(false)
const editModelLoading = ref(false)
const editModelId = ref('')
const availableModels = ref<LlmModel[]>([])
const modelsLoading = ref(false)

// byn 数字分身提示词配置
const digitalBynPrompt = ref<string | null>(null)
const editPromptDialogVisible = ref(false)
const editPromptLoading = ref(false)
const editPromptText = ref('')

async function fetchConfig() {
  loading.value = true
  try {
    const res = await adminGetAllConfigApi()
    configList.value = res.data.data || []
    const allowRegisterConfig = configList.value.find((item) => item.configKey === 'allowRegister')
    allowRegister.value = !allowRegisterConfig || allowRegisterConfig.configValue === 'true'
    const simpleTaskConfig = configList.value.find((item) => item.configKey === 'simpleTaskModelId')
    simpleTaskModelId.value = simpleTaskConfig?.configValue ?? ''
    simpleTaskModelName.value = simpleTaskModelId.value ? '已配置' : '未配置'
  } catch (error) {
    console.error('获取配置失败:', error)
  } finally {
    loading.value = false
  }
}

function openEditDialog() {
  editAllowRegister.value = allowRegister.value
  editDialogVisible.value = true
}

async function handleSave() {
  editLoading.value = true
  try {
    await adminAllowRegisterSwitchApi(editAllowRegister.value)
    ui.success('保存成功')
    editDialogVisible.value = false
    fetchConfig()
  } catch (error) {
    console.error('保存配置失败:', error)
  } finally {
    editLoading.value = false
  }
}

async function openEditModelDialog() {
  editModelId.value = simpleTaskModelId.value
  editModelDialogVisible.value = true
  if (availableModels.value.length === 0) {
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
}

async function handleModelSave() {
  editModelLoading.value = true
  try {
    await adminSimpleTaskModelSwitchApi(editModelId.value)
    ui.success('保存成功')
    editModelDialogVisible.value = false
    fetchConfig()
  } catch (error) {
    console.error('保存配置失败:', error)
  } finally {
    editModelLoading.value = false
  }
}

function getModelDisplayName(model: LlmModel) {
  return model.modelDisplayName || model.modelName
}

async function fetchDigitalBynPrompt() {
  try {
    const res = await adminGetDigitalBynPromptApi()
    digitalBynPrompt.value = res.data.data ?? null
  } catch (error) {
    console.error('获取数字分身提示词失败:', error)
  }
}

function openEditPromptDialog() {
  editPromptText.value = digitalBynPrompt.value ?? ''
  editPromptDialogVisible.value = true
}

async function handlePromptSave() {
  editPromptLoading.value = true
  try {
    await adminSetDigitalBynPromptApi(editPromptText.value)
    ui.success('保存成功')
    editPromptDialogVisible.value = false
    fetchDigitalBynPrompt()
  } catch (error) {
    console.error('保存数字分身提示词失败:', error)
  } finally {
    editPromptLoading.value = false
  }
}

onMounted(() => {
  fetchConfig()
  fetchDigitalBynPrompt()
})
</script>

<template>
  <div class="system-config">
    <a-card class="content-card" :loading="loading">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">系统配置</span>
      </div>

      <div class="card-header">
        <h3>系统配置</h3>
      </div>

      <div class="config-list">
        <div class="config-item">
          <div class="config-info">
            <div class="config-label">是否允许新账户注册</div>
            <div class="config-value">
              <a-tag :color="allowRegister ? 'success' : 'error'">
                {{ allowRegister ? '允许' : '不允许' }}
              </a-tag>
            </div>
          </div>
          <div class="config-action">
            <a-button type="primary" @click="openEditDialog">
              <template #icon><EditOutlined /></template>
              修改
            </a-button>
          </div>
        </div>

        <div class="config-item">
          <div class="config-info">
            <div class="config-label">简单任务模型</div>
            <div class="config-value">
              <a-tag v-if="simpleTaskModelId" color="success">{{ simpleTaskModelName }}</a-tag>
              <a-tag v-else color="default">未配置</a-tag>
              <span class="config-hint">用于自动生成会话标题等轻量任务</span>
            </div>
          </div>
          <div class="config-action">
            <a-button type="primary" @click="openEditModelDialog">
              <template #icon><EditOutlined /></template>
              修改
            </a-button>
          </div>
        </div>

        <!-- <div class="config-item">
          <div class="config-info">
            <div class="config-label">byn 数字分身系统提示词</div>
            <div class="config-value">
              <a-tag v-if="digitalBynPrompt" color="success">已配置</a-tag>
              <a-tag v-else color="default">未配置</a-tag>
              <span class="config-hint">设置 agentType=2 模式下注入的固定系统提示词</span>
            </div>
          </div>
          <div class="config-action">
            <a-button type="primary" @click="openEditPromptDialog">
              <template #icon><EditOutlined /></template>
              修改
            </a-button>
          </div>
        </div> -->

        <a-modal
          v-if="false"
          v-model:open="editPromptDialogVisible"
          title="修改 byn 数字分身系统提示词"
          :confirm-loading="editPromptLoading"
          ok-text="保存"
          cancel-text="取消"
          @ok="handlePromptSave"
          width="640px"
        >
          <div class="edit-group">
            <div class="edit-label">系统提示词（留空则清除配置）</div>
            <a-textarea
              v-model:value="editPromptText"
              :auto-size="{ minRows: 6, maxRows: 20 }"
              placeholder="请输入 byn 数字分身的系统提示词..."
            />
          </div>
        </a-modal>
      </div>
    </a-card>

    <a-modal
      v-model:open="editDialogVisible"
      title="修改注册设置"
      :confirm-loading="editLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSave"
    >
      <div class="edit-group">
        <div class="edit-label">是否允许注册</div>
        <a-radio-group v-model:value="editAllowRegister" button-style="solid">
          <a-radio-button :value="true">允许</a-radio-button>
          <a-radio-button :value="false">不允许</a-radio-button>
        </a-radio-group>
      </div>
    </a-modal>

    <a-modal
      v-model:open="editModelDialogVisible"
      title="修改简单任务模型"
      :confirm-loading="editModelLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleModelSave"
    >
      <div class="edit-group">
        <div class="edit-label">选择模型（留空则禁用标题自动生成）</div>
        <a-select
          v-model:value="editModelId"
          :loading="modelsLoading"
          placeholder="选择模型或清空以禁用"
          allow-clear
          show-search
          :filter-option="
            (input: string, option: any) => option.label?.toLowerCase().includes(input.toLowerCase())
          "
          style="width: 100%"
        >
          <a-select-option
            v-for="model in availableModels"
            :key="model.id"
            :value="model.id"
            :label="getModelDisplayName(model)"
          >
            {{ getModelDisplayName(model) }}
            <span v-if="model.modelDisplayName" class="model-name-hint">（{{ model.modelName }}）</span>
          </a-select-option>
        </a-select>
      </div>
    </a-modal>

    <a-modal
      v-model:open="editPromptDialogVisible"
      title="修改 byn 数字分身系统提示词"
      :confirm-loading="editPromptLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handlePromptSave"
      width="640px"
    >
      <div class="edit-group">
        <div class="edit-label">系统提示词（留空则清除配置）</div>
        <a-textarea
          v-model:value="editPromptText"
          :auto-size="{ minRows: 6, maxRows: 20 }"
          placeholder="请输入 byn 数字分身的系统提示词..."
        />
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.system-config {
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

.config-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background-color: #fafafa;
}

.config-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-label {
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}

.config-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 8px;
}

.edit-label {
  font-size: 14px;
  color: #595959;
}

.config-hint {
  font-size: 12px;
  color: #8c8c8c;
}

.model-name-hint {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
