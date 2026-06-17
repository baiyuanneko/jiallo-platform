<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  SearchOutlined,
  PlusOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  KeyOutlined,
  CopyOutlined,
  MoreOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import {
  adminListSsoClientsApi,
  adminCreateSsoClientApi,
  adminUpdateSsoClientApi,
  adminDeleteSsoClientApi,
  adminResetSsoClientSecretApi,
  adminGetSsoClientDetailApi,
  adminUploadSsoClientIconApi,
} from '@/api/apiAdmin'
import { getSsoClientIconUrl } from '@/api/apiSso'
import type {
  QuerySsoClientsVo,
  CreateSsoClientVo,
  UpdateSsoClientVo,
  SsoClient,
  SsoClientPermissionType,
} from '@/types/admin'
import { ui } from '@/utils/ui'

const tableData = ref<SsoClient[]>([])
const loading = ref(false)
const total = ref(0)

const queryParams = reactive<QuerySsoClientsVo>({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  permissionType: undefined,
})

const permissionTypeOptions: { label: string; value: SsoClientPermissionType }[] = [
  { label: '仅 UID', value: 'UID_ONLY' },
  { label: '仅角色类型', value: 'ROLETYPE_ONLY' },
  { label: 'UID 和角色类型', value: 'UID_AND_ROLETYPE' },
  { label: '基本信息', value: 'BASIC_INFO' },
  { label: '基本信息+角色类型', value: 'BASIC_INFO_WITH_ROLETYPE' },
  { label: '基本信息+邮箱', value: 'BASIC_INFO_WITH_EMAIL' },
  { label: '基本信息+角色类型+邮箱', value: 'BASIC_INFO_WITH_ROLETYPE_AND_EMAIL' },
]

const createDialogVisible = ref(false)
const createFormRef = ref()
const createForm = reactive<CreateSsoClientVo>({
  clientUniqueName: '',
  clientName: '',
  clientDesc: '',
  clientWebsite: '',
  clientAuthorName: '',
  clientRedirectUri: '',
  clientPermissionType: 'BASIC_INFO',
  silentRedirect: false,
})
const createLoading = ref(false)

const secretDialogVisible = ref(false)
const generatedSecret = ref('')
const generatedClientName = ref('')

const editDialogVisible = ref(false)
const editFormRef = ref()
const editForm = reactive<UpdateSsoClientVo>({
  clientId: '',
  clientName: '',
  clientDesc: '',
  clientWebsite: '',
  clientAuthorName: '',
  clientRedirectUri: '',
  clientPermissionType: undefined,
  silentRedirect: false,
})
const editLoading = ref(false)

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const clientDetail = ref<SsoClient | null>(null)

const uploadIconDialogVisible = ref(false)
const uploadIconLoading = ref(false)
const currentUploadClient = ref<SsoClient | null>(null)
const selectedIconFile = ref<File | null>(null)
const iconPreviewUrl = ref<string>('')

const createRules = {
  clientUniqueName: [
    { required: true, message: '请输入唯一标识', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/,
      message: '以字母开头，只能包含字母、数字、下划线和连字符',
      trigger: 'blur',
    },
  ],
  clientName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  clientRedirectUri: [
    { required: true, message: '请输入回调地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的 URL', trigger: 'blur' },
  ],
  clientPermissionType: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
}

const editRules = {
  clientRedirectUri: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
}

const tableColumns = [
  { title: '图标', key: 'icon', width: 80 },
  { title: '唯一标识', key: 'clientUniqueName', dataIndex: 'clientUniqueName', width: 170 },
  { title: '应用名称', key: 'clientName', dataIndex: 'clientName', width: 160 },
  { title: '描述', key: 'clientDesc', dataIndex: 'clientDesc' },
  { title: '权限类型', key: 'clientPermissionType', dataIndex: 'clientPermissionType', width: 190 },
  { title: '静默跳转', key: 'silentRedirect', dataIndex: 'silentRedirect', width: 110 },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' as const },
]

function getPermissionTypeName(type: string): string {
  const typeMap: Record<string, string> = {
    UID_ONLY: '仅 UID',
    ROLETYPE_ONLY: '仅角色类型',
    UID_AND_ROLETYPE: 'UID 和角色类型',
    BASIC_INFO: '基本信息',
    BASIC_INFO_WITH_ROLETYPE: '基本信息+角色类型',
    BASIC_INFO_WITH_EMAIL: '基本信息+邮箱',
    BASIC_INFO_WITH_ROLETYPE_AND_EMAIL: '基本信息+角色类型+邮箱',
  }
  return typeMap[type] || type
}

function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchClients() {
  loading.value = true
  try {
    const res = await adminListSsoClientsApi(queryParams)
    if (res.data.code === 0) {
      tableData.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取 SSO Client 列表失败:', error)
    ui.error('获取 SSO Client 列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchClients()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.permissionType = undefined
  queryParams.pageNum = 1
  fetchClients()
}

function handlePageChange(page: number, pageSize?: number) {
  queryParams.pageNum = page
  if (pageSize) {
    queryParams.pageSize = pageSize
  }
  fetchClients()
}

function openCreateDialog() {
  createForm.clientUniqueName = ''
  createForm.clientName = ''
  createForm.clientDesc = ''
  createForm.clientWebsite = ''
  createForm.clientAuthorName = ''
  createForm.clientRedirectUri = ''
  createForm.clientPermissionType = 'BASIC_INFO'
  createForm.silentRedirect = false
  createDialogVisible.value = true
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }

  createLoading.value = true
  try {
    const res = await adminCreateSsoClientApi(createForm)
    if (res.data.code === 0) {
      ui.success('SSO Client 创建成功')
      createDialogVisible.value = false
      generatedSecret.value = res.data.data.clientSecret
      generatedClientName.value = res.data.data.clientUniqueName
      secretDialogVisible.value = true
      fetchClients()
    }
  } catch (error) {
    console.error('创建 SSO Client 失败:', error)
  } finally {
    createLoading.value = false
  }
}

async function copySecret() {
  try {
    await navigator.clipboard.writeText(generatedSecret.value)
    ui.success('密钥已复制到剪贴板')
  } catch {
    ui.error('复制失败，请手动复制')
  }
}

function openEditDialog(client: SsoClient) {
  editForm.clientId = client.id
  editForm.clientName = client.clientName
  editForm.clientDesc = client.clientDesc || ''
  editForm.clientWebsite = client.clientWebsite || ''
  editForm.clientAuthorName = client.clientAuthorName || ''
  editForm.clientRedirectUri = client.clientRedirectUri
  editForm.clientPermissionType = client.clientPermissionType
  editForm.silentRedirect = client.silentRedirect || false
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }

  editLoading.value = true
  try {
    const res = await adminUpdateSsoClientApi(editForm)
    if (res.data.code === 0) {
      ui.success('SSO Client 更新成功')
      editDialogVisible.value = false
      fetchClients()
    }
  } catch (error) {
    console.error('更新 SSO Client 失败:', error)
  } finally {
    editLoading.value = false
  }
}

async function viewClientDetail(client: SsoClient) {
  detailDialogVisible.value = true
  detailLoading.value = true
  clientDetail.value = null

  try {
    const res = await adminGetSsoClientDetailApi(client.id)
    if (res.data.code === 0) {
      clientDetail.value = res.data.data.ssoClient
    }
  } catch (error) {
    console.error('获取 SSO Client 详情失败:', error)
    ui.error('获取 SSO Client 详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function resetSecret(client: SsoClient) {
  try {
    await ui.confirm({
      title: '确认重置密钥',
      content: `确定要重置 "${client.clientName}" 的密钥吗？重置后旧密钥将立即失效。`,
      type: 'warning',
    })

    const res = await adminResetSsoClientSecretApi({ clientId: client.id })
    if (res.data.code === 0) {
      ui.success('密钥重置成功')
      generatedSecret.value = res.data.data.clientSecret
      generatedClientName.value = client.clientUniqueName
      secretDialogVisible.value = true
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密钥失败:', error)
    }
  }
}

async function deleteClient(client: SsoClient) {
  try {
    await ui.confirm({
      title: '确认删除',
      content: `确定要删除 "${client.clientName}" 吗？此操作不可恢复！`,
      type: 'warning',
    })

    const res = await adminDeleteSsoClientApi({ clientId: client.id })
    if (res.data.code === 0) {
      ui.success('SSO Client 已删除')
      fetchClients()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除 SSO Client 失败:', error)
    }
  }
}

function openUploadIconDialog(client: SsoClient) {
  currentUploadClient.value = client
  selectedIconFile.value = null
  iconPreviewUrl.value = ''
  uploadIconDialogVisible.value = true
}

function handleIconFileChange(file: File | null) {
  if (!file) {
    selectedIconFile.value = null
    iconPreviewUrl.value = ''
    return
  }

  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ui.error('只支持 jpg、png、gif、webp 格式的图片')
    return
  }

  const maxSize = 2 * 1024 * 1024
  if (file.size > maxSize) {
    ui.error('图片大小不能超过 2MB')
    return
  }

  selectedIconFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    iconPreviewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

function handleBeforeUpload(file: File) {
  handleIconFileChange(file)
  return false
}

async function handleUploadIconSubmit() {
  if (!currentUploadClient.value || !selectedIconFile.value) {
    ui.warning('请选择图片文件')
    return
  }

  uploadIconLoading.value = true
  try {
    const res = await adminUploadSsoClientIconApi(
      currentUploadClient.value.id,
      selectedIconFile.value,
    )
    if (res.data.code === 0) {
      ui.success('图标上传成功')
      uploadIconDialogVisible.value = false
      fetchClients()
    }
  } catch (error) {
    console.error('上传图标失败:', error)
  } finally {
    uploadIconLoading.value = false
  }
}

function getClientIconUrl(clientUniqueName: string): string {
  return getSsoClientIconUrl(clientUniqueName, Date.now())
}

function getClientMenuItems() {
  return [
    { key: 'upload-icon', label: '上传图标', icon: UploadOutlined },
    { key: 'reset-secret', label: '重置密钥', icon: KeyOutlined },
    { key: 'delete', label: '删除', icon: DeleteOutlined, danger: true },
  ]
}

function handleClientMenuClick(key: string, client: SsoClient) {
  if (key === 'upload-icon') {
    openUploadIconDialog(client)
  } else if (key === 'reset-secret') {
    resetSecret(client)
  } else if (key === 'delete') {
    deleteClient(client)
  }
}

onMounted(() => {
  fetchClients()
})
</script>

<template>
  <div class="sso-client-management">
    <a-card class="content-card">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">SSO Client 管理</span>
      </div>

      <div class="card-header">
        <h3>SSO Client 管理</h3>
        <a-button type="primary" @click="openCreateDialog">
          <template #icon><PlusOutlined /></template>
          创建 SSO Client
        </a-button>
      </div>

      <div class="search-bar">
        <a-input
          v-model:value="queryParams.keyword"
          placeholder="搜索唯一标识、名称、描述"
          allow-clear
          style="width: 280px"
          @press-enter="handleSearch"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="queryParams.permissionType"
          placeholder="权限类型"
          allow-clear
          style="width: 220px"
          :options="permissionTypeOptions"
        />
        <a-button type="primary" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          搜索
        </a-button>
        <a-button @click="handleReset">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </div>

      <a-table
        :columns="tableColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="false"
        :row-key="(record: SsoClient) => record.id"
        size="middle"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <img
              :src="getClientIconUrl(record.clientUniqueName)"
              :alt="record.clientName"
              class="table-icon"
              @error="(e: Event) => (((e.target as HTMLImageElement).style.display = 'none'))"
            />
          </template>
          <template v-else-if="column.key === 'clientDesc'">
            {{ record.clientDesc || '-' }}
          </template>
          <template v-else-if="column.key === 'clientPermissionType'">
            <a-tag color="processing">
              {{ getPermissionTypeName(record.clientPermissionType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'silentRedirect'">
            <a-tag :color="record.silentRedirect ? 'success' : 'default'">
              {{ record.silentRedirect ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="table-actions">
              <a-button type="link" @click="viewClientDetail(record)">
                <template #icon><EyeOutlined /></template>
                详情
              </a-button>
              <a-button type="link" @click="openEditDialog(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-dropdown trigger="click">
                <a-button type="link">
                  <template #icon><MoreOutlined /></template>
                  更多
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item
                      v-for="item in getClientMenuItems()"
                      :key="item.key"
                      :danger="item.danger"
                      @click="handleClientMenuClick(item.key, record)"
                    >
                      <component :is="item.icon" />
                      {{ item.label }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </template>
        </template>
      </a-table>

      <div class="pagination-wrapper">
        <a-pagination
          v-model:current="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-size-options="['10', '20', '50', '100']"
          :total="total"
          show-size-changer
          show-quick-jumper
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="createDialogVisible"
      title="创建 SSO Client"
      :confirm-loading="createLoading"
      ok-text="创建"
      cancel-text="取消"
      :width="640"
      @ok="handleCreateSubmit"
    >
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-form-item label="唯一标识" name="clientUniqueName">
          <a-input v-model:value="createForm.clientUniqueName" placeholder="用于 URL，如 myapp" />
        </a-form-item>
        <a-form-item label="应用名称" name="clientName">
          <a-input v-model:value="createForm.clientName" placeholder="显示给用户的名称" />
        </a-form-item>
        <a-form-item label="应用描述" name="clientDesc">
          <a-textarea v-model:value="createForm.clientDesc" :rows="2" placeholder="应用描述（可选）" />
        </a-form-item>
        <a-form-item label="官网地址" name="clientWebsite">
          <a-input v-model:value="createForm.clientWebsite" placeholder="https://example.com（可选）" />
        </a-form-item>
        <a-form-item label="开发者名称" name="clientAuthorName">
          <a-input v-model:value="createForm.clientAuthorName" placeholder="开发者或公司名称（可选）" />
        </a-form-item>
        <a-form-item label="回调地址" name="clientRedirectUri">
          <a-input v-model:value="createForm.clientRedirectUri" placeholder="https://example.com/callback" />
        </a-form-item>
        <a-form-item label="权限类型" name="clientPermissionType">
          <a-select v-model:value="createForm.clientPermissionType" :options="permissionTypeOptions" />
        </a-form-item>
        <a-form-item label="静默跳转">
          <div class="switch-row">
            <a-switch v-model:checked="createForm.silentRedirect" />
            <span class="form-tip">开启后将跳过授权确认页面</span>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="editDialogVisible"
      title="编辑 SSO Client"
      :confirm-loading="editLoading"
      ok-text="保存"
      cancel-text="取消"
      :width="640"
      @ok="handleEditSubmit"
    >
      <a-form ref="editFormRef" :model="editForm" :rules="editRules" layout="vertical">
        <a-form-item label="应用名称" name="clientName">
          <a-input v-model:value="editForm.clientName" placeholder="显示给用户的名称" />
        </a-form-item>
        <a-form-item label="应用描述" name="clientDesc">
          <a-textarea v-model:value="editForm.clientDesc" :rows="2" placeholder="应用描述" />
        </a-form-item>
        <a-form-item label="官网地址" name="clientWebsite">
          <a-input v-model:value="editForm.clientWebsite" placeholder="https://example.com" />
        </a-form-item>
        <a-form-item label="开发者名称" name="clientAuthorName">
          <a-input v-model:value="editForm.clientAuthorName" placeholder="开发者或公司名称" />
        </a-form-item>
        <a-form-item label="回调地址" name="clientRedirectUri">
          <a-input v-model:value="editForm.clientRedirectUri" placeholder="https://example.com/callback" />
        </a-form-item>
        <a-form-item label="权限类型" name="clientPermissionType">
          <a-select v-model:value="editForm.clientPermissionType" :options="permissionTypeOptions" />
        </a-form-item>
        <a-form-item label="静默跳转">
          <div class="switch-row">
            <a-switch v-model:checked="editForm.silentRedirect" />
            <span class="form-tip">开启后将跳过授权确认页面</span>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="secretDialogVisible"
      title="Client Secret"
      :footer="null"
      :mask-closable="false"
      :width="520"
    >
      <a-alert
        message="请妥善保存以下密钥，关闭后将无法再次查看！"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <div class="secret-display">
        <div class="secret-item">
          <span class="secret-label">唯一标识：</span>
          <span class="secret-value">{{ generatedClientName }}</span>
        </div>
        <div class="secret-item">
          <span class="secret-label">Client Secret：</span>
          <a-input :value="generatedSecret" readonly />
          <a-button @click="copySecret">
            <template #icon><CopyOutlined /></template>
            复制
          </a-button>
        </div>
      </div>
      <div class="modal-actions">
        <a-button type="primary" @click="secretDialogVisible = false">我已保存</a-button>
      </div>
    </a-modal>

    <a-modal
      v-model:open="detailDialogVisible"
      title="SSO Client 详情"
      :footer="null"
      :width="640"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="clientDetail">
          <div class="detail-info-list">
            <div class="detail-info-item">
              <span class="detail-label">Client ID</span>
              <span class="detail-value">{{ clientDetail.id }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">唯一标识</span>
              <span class="detail-value">{{ clientDetail.clientUniqueName }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">应用名称</span>
              <span class="detail-value">{{ clientDetail.clientName }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">应用描述</span>
              <span class="detail-value">{{ clientDetail.clientDesc || '-' }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">官网地址</span>
              <span class="detail-value">
                <a
                  v-if="clientDetail.clientWebsite"
                  :href="clientDetail.clientWebsite"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {{ clientDetail.clientWebsite }}
                </a>
                <span v-else>-</span>
              </span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">开发者名称</span>
              <span class="detail-value">{{ clientDetail.clientAuthorName || '-' }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">回调地址</span>
              <span class="detail-value">{{ clientDetail.clientRedirectUri }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">权限类型</span>
              <span class="detail-value">
                <a-tag color="processing">
                  {{ getPermissionTypeName(clientDetail.clientPermissionType) }}
                </a-tag>
              </span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">静默跳转</span>
              <span class="detail-value">
                <a-tag :color="clientDetail.silentRedirect ? 'success' : 'default'">
                  {{ clientDetail.silentRedirect ? '是' : '否' }}
                </a-tag>
              </span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">创建时间</span>
              <span class="detail-value">{{ formatTime(clientDetail.createTime) }}</span>
            </div>
            <div class="detail-info-item">
              <span class="detail-label">更新时间</span>
              <span class="detail-value">{{ formatTime(clientDetail.updateTime) }}</span>
            </div>
          </div>
        </template>
      </a-spin>
      <div class="modal-actions">
        <a-button @click="detailDialogVisible = false">关闭</a-button>
      </div>
    </a-modal>

    <a-modal
      v-model:open="uploadIconDialogVisible"
      title="上传客户端图标"
      :footer="null"
      :width="520"
    >
      <div class="upload-icon-container">
        <a-alert
          message="支持格式：jpg、png、gif、webp；文件大小：最大 2MB；推荐尺寸：256×256 或 512×512"
          type="info"
          show-icon
        />

        <a-upload
          :before-upload="handleBeforeUpload"
          :show-upload-list="false"
          accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
        >
          <div class="upload-dropzone">
            <div v-if="iconPreviewUrl" class="icon-preview">
              <img :src="iconPreviewUrl" alt="预览" />
            </div>
            <div v-else class="icon-upload-placeholder">
              <UploadOutlined class="upload-icon" />
              <div class="upload-text">点击选择图片</div>
            </div>
          </div>
        </a-upload>

        <div v-if="currentUploadClient" class="client-info-tip">
          正在为 <strong>{{ currentUploadClient.clientName }}</strong> 上传图标
        </div>
      </div>

      <div class="modal-actions">
        <a-button @click="uploadIconDialogVisible = false">取消</a-button>
        <a-button
          type="primary"
          :loading="uploadIconLoading"
          :disabled="!selectedIconFile"
          @click="handleUploadIconSubmit"
        >
          上传
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.sso-client-management {
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

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.switch-row,
.table-actions,
.modal-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-actions {
  margin-top: 20px;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #8c8c8c;
}

.table-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  background: rgba(0, 0, 0, 0.02);
}

.secret-display {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.secret-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.secret-item :deep(.ant-input) {
  flex: 1;
}

.secret-label {
  width: 100px;
  font-weight: 500;
  color: #595959;
  flex-shrink: 0;
}

.secret-value {
  color: #262626;
}

.detail-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-info-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-info-item:last-child {
  border-bottom: none;
}

.detail-label {
  width: 100px;
  font-weight: 500;
  color: #595959;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #262626;
  word-break: break-all;
}

.detail-value a {
  color: #1677ff;
  text-decoration: none;
}

.detail-value a:hover {
  text-decoration: underline;
}

.upload-icon-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-dropzone {
  width: 100%;
  min-height: 240px;
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: #fafafa;
  cursor: pointer;
}

.icon-preview {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-preview img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 8px;
}

.icon-upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.upload-icon {
  font-size: 64px;
  color: #bfbfbf;
}

.upload-text {
  font-size: 14px;
  color: #595959;
}

.client-info-tip {
  font-size: 13px;
  color: #595959;
  text-align: center;
  padding: 8px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
}

.client-info-tip strong {
  color: #1677ff;
}

@media (max-width: 768px) {
  .secret-item,
  .detail-info-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .secret-label,
  .detail-label {
    width: auto;
  }

  :deep(.ant-table-wrapper) {
    overflow-x: auto;
  }
}
</style>
