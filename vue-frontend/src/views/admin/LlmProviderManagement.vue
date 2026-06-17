<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  CheckCircleFilled,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  AppstoreOutlined,
  ExperimentOutlined,
  PictureOutlined,
} from '@ant-design/icons-vue'
import {
  adminCreateLlmModelApi,
  adminCreateLlmProviderApi,
  adminDeleteLlmModelApi,
  adminDeleteLlmProviderApi,
  adminListLlmModelsByProviderApi,
  adminListLlmProvidersApi,
  adminTestLlmModelApi,
  adminUpdateLlmModelApi,
  adminUpdateLlmProviderApi,
  adminUploadProviderIconApi,
  adminUploadModelIconApi,
  getProviderIconUrl,
} from '@/api/apiAdmin'
import { getModelIconUrl } from '@/api/apiChat'
import type {
  CreateLlmModelVo,
  CreateLlmProviderVo,
  LlmModel,
  LlmProvider,
  QueryLlmProvidersVo,
  UpdateLlmModelVo,
  UpdateLlmProviderVo,
} from '@/types/admin'
import { ui } from '@/utils/ui'
import { getAccessToken } from '@/utils/auth'

const providerLoading = ref(false)
const providerTotal = ref(0)
const providerTableData = ref<LlmProvider[]>([])
const providerQueryParams = reactive<QueryLlmProvidersVo>({
  pageNum: 1,
  pageSize: 10,
})

const providerColumns = [
  { title: '图标', key: 'icon', width: 60 },
  { title: '供应商名称', dataIndex: 'providerName', key: 'providerName', width: 220 },
  { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 320, fixed: 'right' as const },
]

const providerCreateVisible = ref(false)
const providerEditVisible = ref(false)
const providerSubmitting = ref(false)
const providerCreateFormRef = ref()
const providerEditFormRef = ref()
const providerCreateForm = reactive<CreateLlmProviderVo>({
  providerName: '',
  baseUrl: '',
  apiKey: '',
})
const providerEditForm = reactive<UpdateLlmProviderVo>({
  id: '',
  providerName: '',
  baseUrl: '',
  apiKey: '',
})

const providerRules = {
  providerName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
}

const modelDrawerVisible = ref(false)
const currentProvider = ref<LlmProvider | null>(null)
const modelLoading = ref(false)
const modelTotal = ref(0)
const modelTableData = ref<LlmModel[]>([])
const modelIconBuster = ref(Date.now())
const modelQueryParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

const modelColumns = [
  { title: '图标', key: 'icon', width: 60 },
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName', width: 160 },
  { title: '真实模型名称', dataIndex: 'realModelName', key: 'realModelName', width: 200 },
  { title: '展示名称', dataIndex: 'modelDisplayName', key: 'modelDisplayName', width: 180 },
  { title: '已验证', key: 'isVerifiedModel', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 340, fixed: 'right' as const },
]

const modelCreateVisible = ref(false)
const modelEditVisible = ref(false)
const modelSubmitting = ref(false)
const modelCreateFormRef = ref()
const modelEditFormRef = ref()
const modelCreateForm = reactive<CreateLlmModelVo>({
  providerId: '',
  modelName: '',
  realModelName: '',
  modelDisplayName: '',
})
const modelEditForm = reactive<UpdateLlmModelVo>({
  id: '',
  modelName: '',
  realModelName: '',
  modelDisplayName: '',
  isVerifiedModel: null,
})

const modelRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

const testingModelId = ref('')
const testResultVisible = ref(false)
const testResultModelName = ref('')
const testResultContent = ref('')
const testResultElapsedSeconds = ref('0.0')

const providerIconInputRef = ref<HTMLInputElement | null>(null)
const uploadingProviderIconId = ref('')
const modelIconInputRef = ref<HTMLInputElement | null>(null)
const uploadingModelIconId = ref('')

// Icon preview: provider icons need auth (blob fetch), model icons use direct URLs
const providerIconUrls = ref<Record<string, string>>({})

async function loadProviderIcons() {
  // Revoke old URLs
  Object.values(providerIconUrls.value).forEach((url) => {
    if (url) URL.revokeObjectURL(url)
  })
  const newUrls: Record<string, string> = {}
  const token = getAccessToken()
  if (!token) {
    providerIconUrls.value = newUrls
    return
  }
  await Promise.all(
    providerTableData.value.map(async (provider) => {
      try {
        const res = await fetch(getProviderIconUrl(provider.id), {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (res.ok) {
          const blob = await res.blob()
          newUrls[provider.id] = URL.createObjectURL(blob)
        }
      } catch {
        // no icon
      }
    }),
  )
  providerIconUrls.value = newUrls
}

onUnmounted(() => {
  Object.values(providerIconUrls.value).forEach((url) => {
    if (url) URL.revokeObjectURL(url)
  })
})

function showTotal(value: number): string {
  return `共 ${value} 条`
}

function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function getValue(value?: string): string {
  return value?.trim() ? value : '-'
}

async function fetchProviders() {
  providerLoading.value = true
  try {
    const res = await adminListLlmProvidersApi(providerQueryParams)
    providerTableData.value = res.data.data.records || []
    providerTotal.value = res.data.data.total || 0
    loadProviderIcons()
  } catch (error) {
    console.error('获取模型供应商失败:', error)
  } finally {
    providerLoading.value = false
  }
}

async function fetchModels() {
  if (!currentProvider.value) {
    modelTableData.value = []
    modelTotal.value = 0
    return
  }

  modelLoading.value = true
  try {
    const res = await adminListLlmModelsByProviderApi({
      providerId: currentProvider.value.id,
      pageNum: modelQueryParams.pageNum,
      pageSize: modelQueryParams.pageSize,
    })
    modelTableData.value = res.data.data.records || []
    modelTotal.value = res.data.data.total || 0
    modelIconBuster.value = Date.now()
  } catch (error) {
    console.error('获取模型列表失败:', error)
  } finally {
    modelLoading.value = false
  }
}

function handleProviderPageChange(page: number, pageSize?: number) {
  providerQueryParams.pageNum = page
  if (pageSize) {
    providerQueryParams.pageSize = pageSize
  }
  fetchProviders()
}

function handleModelPageChange(page: number, pageSize?: number) {
  modelQueryParams.pageNum = page
  if (pageSize) {
    modelQueryParams.pageSize = pageSize
  }
  fetchModels()
}

function openProviderCreateModal() {
  providerCreateForm.providerName = ''
  providerCreateForm.baseUrl = ''
  providerCreateForm.apiKey = ''
  providerCreateVisible.value = true
}

function openProviderEditModal(provider: LlmProvider) {
  providerEditForm.id = provider.id
  providerEditForm.providerName = provider.providerName
  providerEditForm.baseUrl = provider.baseUrl || ''
  providerEditForm.apiKey = ''
  providerEditVisible.value = true
}

async function submitProviderCreate() {
  try {
    await providerCreateFormRef.value?.validate()
  } catch {
    return
  }

  providerSubmitting.value = true
  try {
    await adminCreateLlmProviderApi({
      providerName: providerCreateForm.providerName.trim(),
      baseUrl: providerCreateForm.baseUrl?.trim() || '',
      apiKey: providerCreateForm.apiKey?.trim() || '',
    })
    ui.success('供应商创建成功')
    providerCreateVisible.value = false
    providerQueryParams.pageNum = 1
    fetchProviders()
  } catch (error) {
    console.error('创建供应商失败:', error)
  } finally {
    providerSubmitting.value = false
  }
}

async function submitProviderEdit() {
  try {
    await providerEditFormRef.value?.validate()
  } catch {
    return
  }

  providerSubmitting.value = true
  try {
    await adminUpdateLlmProviderApi({
      id: providerEditForm.id,
      providerName: providerEditForm.providerName.trim(),
      baseUrl: providerEditForm.baseUrl.trim(),
      apiKey: providerEditForm.apiKey?.trim() || '',
    })
    ui.success('供应商更新成功')
    providerEditVisible.value = false
    if (currentProvider.value?.id === providerEditForm.id) {
      currentProvider.value = {
        ...currentProvider.value,
        providerName: providerEditForm.providerName.trim(),
        baseUrl: providerEditForm.baseUrl.trim(),
      }
    }
    fetchProviders()
  } catch (error) {
    console.error('更新供应商失败:', error)
  } finally {
    providerSubmitting.value = false
  }
}

async function deleteProvider(provider: LlmProvider) {
  try {
    await ui.confirm({
      title: '确认删除供应商',
      content: `确定删除供应商“${provider.providerName}”吗？该供应商下的模型也会一起删除。`,
      type: 'warning',
    })
    await adminDeleteLlmProviderApi({ providerId: provider.id })
    ui.success('供应商已删除')
    if (currentProvider.value?.id === provider.id) {
      modelDrawerVisible.value = false
      currentProvider.value = null
      modelTableData.value = []
      modelTotal.value = 0
    }
    fetchProviders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除供应商失败:', error)
    }
  }
}

function openModelDrawer(provider: LlmProvider) {
  currentProvider.value = provider
  modelQueryParams.pageNum = 1
  modelDrawerVisible.value = true
  fetchModels()
}

function openModelCreateModal() {
  if (!currentProvider.value) return
  modelCreateForm.providerId = currentProvider.value.id
  modelCreateForm.modelName = ''
  modelCreateForm.realModelName = ''
  modelCreateForm.modelDisplayName = ''
  modelCreateVisible.value = true
}

function openModelEditModal(model: LlmModel) {
  modelEditForm.id = model.id
  modelEditForm.modelName = model.modelName
  modelEditForm.realModelName = model.realModelName || ''
  modelEditForm.modelDisplayName = model.modelDisplayName || ''
  modelEditForm.isVerifiedModel = model.isVerifiedModel ?? null
  modelEditVisible.value = true
}

async function submitModelCreate() {
  try {
    await modelCreateFormRef.value?.validate()
  } catch {
    return
  }

  modelSubmitting.value = true
  try {
    await adminCreateLlmModelApi({
      providerId: modelCreateForm.providerId,
      modelName: modelCreateForm.modelName.trim(),
      realModelName: modelCreateForm.realModelName?.trim() || '',
      modelDisplayName: modelCreateForm.modelDisplayName?.trim() || '',
    })
    ui.success('模型创建成功')
    modelCreateVisible.value = false
    modelQueryParams.pageNum = 1
    fetchModels()
  } catch (error) {
    console.error('创建模型失败:', error)
  } finally {
    modelSubmitting.value = false
  }
}

async function submitModelEdit() {
  try {
    await modelEditFormRef.value?.validate()
  } catch {
    return
  }

  modelSubmitting.value = true
  try {
    await adminUpdateLlmModelApi({
      id: modelEditForm.id,
      modelName: modelEditForm.modelName?.trim() || '',
      realModelName: modelEditForm.realModelName?.trim() || '',
      modelDisplayName: modelEditForm.modelDisplayName?.trim() || '',
      isVerifiedModel: modelEditForm.isVerifiedModel,
    })
    ui.success('模型更新成功')
    modelEditVisible.value = false
    fetchModels()
  } catch (error) {
    console.error('更新模型失败:', error)
  } finally {
    modelSubmitting.value = false
  }
}

async function deleteModel(model: LlmModel) {
  try {
    await ui.confirm({
      title: '确认删除模型',
      content: `确定删除模型“${model.modelName}”吗？`,
      type: 'warning',
    })
    await adminDeleteLlmModelApi({ modelId: model.id })
    ui.success('模型已删除')
    fetchModels()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模型失败:', error)
    }
  }
}

async function testModel(model: LlmModel) {
  testingModelId.value = model.id
  const startTime = performance.now()
  try {
    const res = await adminTestLlmModelApi({ id: model.id })
    const elapsedSeconds = (performance.now() - startTime) / 1000
    testResultModelName.value = model.modelName
    testResultContent.value = res.data.data || ''
    testResultElapsedSeconds.value = elapsedSeconds.toFixed(1)
    testResultVisible.value = true
    ui.success('模型测试成功')
  } catch (error) {
    console.error('测试模型失败:', error)
  } finally {
    testingModelId.value = ''
  }
}

function closeTestResultModal() {
  testResultVisible.value = false
}

function triggerProviderIconUpload(provider: LlmProvider) {
  uploadingProviderIconId.value = provider.id
  providerIconInputRef.value?.click()
}

async function handleProviderIconUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !uploadingProviderIconId.value) return
  try {
    await adminUploadProviderIconApi(uploadingProviderIconId.value, file)
    ui.success('供应商图标上传成功')
    loadProviderIcons()
  } catch {
    ui.error('供应商图标上传失败')
  } finally {
    uploadingProviderIconId.value = ''
    target.value = ''
  }
}

function triggerModelIconUpload(model: LlmModel) {
  uploadingModelIconId.value = model.id
  modelIconInputRef.value?.click()
}

async function handleModelIconUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !uploadingModelIconId.value) return
  try {
    await adminUploadModelIconApi(uploadingModelIconId.value, file)
    ui.success('模型图标上传成功')
  } catch {
    ui.error('模型图标上传失败')
  } finally {
    uploadingModelIconId.value = ''
    target.value = ''
  }
}

onMounted(() => {
  fetchProviders()
})
</script>

<template>
  <div class="llm-provider-management">
    <input
      ref="providerIconInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleProviderIconUpload"
    />
    <input
      ref="modelIconInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleModelIconUpload"
    />

    <a-card class="content-card">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">系统模型供应商管理</span>
      </div>

      <div class="card-header">
        <div>
          <h3>系统模型供应商管理</h3>
          <p class="card-subtitle">管理系统中的模型供应商，以及各供应商下挂载的模型。</p>
        </div>
        <a-space>
          <a-button @click="fetchProviders">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button type="primary" @click="openProviderCreateModal">
            <template #icon><PlusOutlined /></template>
            新增供应商
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="id"
        :columns="providerColumns"
        :data-source="providerTableData"
        :loading="providerLoading"
        :pagination="false"
        :scroll="{ x: 980 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <img
              v-if="providerIconUrls[record.id]"
              :src="providerIconUrls[record.id]"
              alt="图标"
              class="icon-preview"
            />
            <span v-else class="icon-placeholder">—</span>
          </template>
          <template v-else-if="column.key === 'baseUrl'">
            <span>{{ getValue(record.baseUrl) }}</span>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space wrap>
              <a-button
                size="small"
                :loading="uploadingProviderIconId === record.id"
                @click="triggerProviderIconUpload(record)"
              >
                <template #icon><PictureOutlined /></template>
                图标
              </a-button>
              <a-button size="small" @click="openModelDrawer(record)">
                <template #icon><AppstoreOutlined /></template>
                模型管理
              </a-button>
              <a-button size="small" @click="openProviderEditModal(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button danger size="small" @click="deleteProvider(record)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <div class="table-pagination">
        <a-pagination
          :current="providerQueryParams.pageNum"
          :page-size="providerQueryParams.pageSize"
          :total="providerTotal"
          show-size-changer
          show-quick-jumper
          :show-total="showTotal"
          @change="handleProviderPageChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="providerCreateVisible"
      title="新增模型供应商"
      ok-text="创建"
      cancel-text="取消"
      :confirm-loading="providerSubmitting"
      @ok="submitProviderCreate"
    >
      <a-form ref="providerCreateFormRef" :model="providerCreateForm" :rules="providerRules" layout="vertical">
        <a-form-item label="供应商名称" name="providerName">
          <a-input v-model:value="providerCreateForm.providerName" placeholder="例如 OpenAI" />
        </a-form-item>
        <a-form-item label="Base URL" name="baseUrl">
          <a-input v-model:value="providerCreateForm.baseUrl" placeholder="例如 https://api.openai.com/v1" />
        </a-form-item>
        <a-form-item label="API Key" name="apiKey">
          <a-input-password v-model:value="providerCreateForm.apiKey" placeholder="可选，留空则不设置" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="providerEditVisible"
      title="编辑模型供应商"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="providerSubmitting"
      @ok="submitProviderEdit"
    >
      <a-form ref="providerEditFormRef" :model="providerEditForm" :rules="providerRules" layout="vertical">
        <a-form-item label="供应商名称" name="providerName">
          <a-input v-model:value="providerEditForm.providerName" />
        </a-form-item>
        <a-form-item label="Base URL" name="baseUrl">
          <a-input v-model:value="providerEditForm.baseUrl" placeholder="编辑时后端要求必填" />
        </a-form-item>
        <a-form-item label="API Key" name="apiKey">
          <a-input-password
            v-model:value="providerEditForm.apiKey"
            placeholder="留空则保持当前密钥不变"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="modelDrawerVisible"
      width="min(1080px, 100vw)"
      title="供应商模型管理"
      placement="right"
      :destroy-on-close="false"
    >
      <template v-if="currentProvider">
        <div class="drawer-header">
          <div class="drawer-provider">
            <div class="drawer-provider-title">{{ currentProvider.providerName }}</div>
            <div class="drawer-provider-subtitle">
              Base URL：{{ getValue(currentProvider.baseUrl) }}
            </div>
          </div>
          <a-space>
            <a-button @click="fetchModels">
              <template #icon><ReloadOutlined /></template>
              刷新模型
            </a-button>
            <a-button type="primary" @click="openModelCreateModal">
              <template #icon><PlusOutlined /></template>
              新增模型
            </a-button>
          </a-space>
        </div>

        <a-table
          row-key="id"
          :columns="modelColumns"
          :data-source="modelTableData"
          :loading="modelLoading"
          :pagination="false"
          :scroll="{ x: 980 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'icon'">
              <img
                :src="getModelIconUrl(record.id, modelIconBuster)"
                alt="图标"
                class="icon-preview"
                @error="($event.target as HTMLImageElement).style.display = 'none'"
              />
            </template>
            <template v-else-if="column.key === 'realModelName'">
              <span>{{ getValue(record.realModelName) }}</span>
            </template>
            <template v-else-if="column.key === 'isVerifiedModel'">
              <check-circle-filled v-if="record.isVerifiedModel" style="color: #1677ff; font-size: 16px" />
              <close-circle-outlined v-else style="color: #d9d9d9; font-size: 16px" />
            </template>
            <template v-else-if="column.key === 'modelDisplayName'">
              <span>{{ getValue(record.modelDisplayName) }}</span>
            </template>
            <template v-else-if="column.key === 'createTime'">
              <span>{{ formatTime(record.createTime) }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space wrap>
                <a-button
                  size="small"
                  :loading="uploadingModelIconId === record.id"
                  @click="triggerModelIconUpload(record)"
                >
                  <template #icon><PictureOutlined /></template>
                  图标
                </a-button>
                <a-button
                  size="small"
                  type="primary"
                  ghost
                  :loading="testingModelId === record.id"
                  @click="testModel(record)"
                >
                  <template #icon><ExperimentOutlined /></template>
                  测试可用性
                </a-button>
                <a-button size="small" @click="openModelEditModal(record)">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button danger size="small" @click="deleteModel(record)">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>

        <div class="table-pagination">
          <a-pagination
            :current="modelQueryParams.pageNum"
            :page-size="modelQueryParams.pageSize"
            :total="modelTotal"
            show-size-changer
            show-quick-jumper
            :show-total="showTotal"
            @change="handleModelPageChange"
          />
        </div>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="modelCreateVisible"
      title="新增模型"
      ok-text="创建"
      cancel-text="取消"
      :confirm-loading="modelSubmitting"
      @ok="submitModelCreate"
    >
      <a-form ref="modelCreateFormRef" :model="modelCreateForm" :rules="modelRules" layout="vertical">
        <a-form-item label="模型名称" name="modelName">
          <a-input v-model:value="modelCreateForm.modelName" placeholder="例如 gpt-4o-mini" />
        </a-form-item>
        <a-form-item label="真实模型名称" name="realModelName">
          <a-input v-model:value="modelCreateForm.realModelName" placeholder="可选" />
        </a-form-item>
        <a-form-item label="模型展示名称" name="modelDisplayName">
          <a-input v-model:value="modelCreateForm.modelDisplayName" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="modelEditVisible"
      title="编辑模型"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="modelSubmitting"
      @ok="submitModelEdit"
    >
      <a-form ref="modelEditFormRef" :model="modelEditForm" :rules="modelRules" layout="vertical">
        <a-form-item label="模型名称" name="modelName">
          <a-input v-model:value="modelEditForm.modelName" />
        </a-form-item>
        <a-form-item label="真实模型名称" name="realModelName">
          <a-input v-model:value="modelEditForm.realModelName" />
        </a-form-item>
        <a-form-item label="模型展示名称" name="modelDisplayName">
          <a-input v-model:value="modelEditForm.modelDisplayName" />
        </a-form-item>
        <a-form-item label="已验证模型">
          <a-switch
            :checked="modelEditForm.isVerifiedModel === true"
            @change="(val: boolean) => { modelEditForm.isVerifiedModel = val }"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="testResultVisible"
      title="模型测试结果"
      ok-text="关闭"
      :cancel-button-props="{ style: { display: 'none' } }"
      @ok="closeTestResultModal"
      @cancel="closeTestResultModal"
    >
      <div class="test-result">
        <div class="test-result-title">模型：{{ testResultModelName }}</div>
        <div class="test-result-desc">
          模型输出测试结果（耗时 {{ testResultElapsedSeconds }} 秒）：
        </div>
        <pre class="test-result-content">{{ testResultContent }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.llm-provider-management {
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
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.card-subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 13px;
}

.icon-preview {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  object-fit: cover;
  vertical-align: middle;
}

.icon-placeholder {
  color: #d9d9d9;
  font-size: 14px;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.drawer-provider-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.drawer-provider-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #8c8c8c;
  word-break: break-all;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.test-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.test-result-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.test-result-desc {
  font-size: 13px;
  color: #8c8c8c;
}

.test-result-content {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #262626;
}

@media (max-width: 768px) {
  .card-header,
  .drawer-header {
    flex-direction: column;
  }

  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }

  :deep(.ant-table-wrapper) {
    overflow-x: auto;
  }
}
</style>
