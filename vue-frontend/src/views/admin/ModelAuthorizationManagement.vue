<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  UndoOutlined,
} from '@ant-design/icons-vue'
import {
  adminAuthorizeLlmModelRolesApi,
  adminGetLlmModelAvailableRolesApi,
  adminListAllLlmProvidersApi,
  adminListLlmModelsApi,
  adminRevokeLlmModelRolesApi,
  adminGetModelAvailableGroupsApi,
  adminAuthorizeModelToGroupsApi,
  adminRevokeModelOfGroupsApi,
  adminPageUserGroupsApi,
} from '@/api/apiAdmin'
import type { LlmModel, LlmProvider, QueryPagedLlmModelsVo, RoleType, UserGroup } from '@/types/admin'
import { ui } from '@/utils/ui'

const ALL_ROLE_TYPES: RoleType[] = [
  'USER',
  'ADMIN',
  'VIP_USER',
  'VANILLA',
  'AWAIT_EMAIL_VERIFICATION',
]

const ROLE_LABELS: Record<RoleType, string> = {
  USER: '普通用户',
  ADMIN: '管理员',
  VIP_USER: 'VIP 用户',
  VANILLA: 'Vanilla',
  AWAIT_EMAIL_VERIFICATION: '待邮箱验证',
}

const providerList = ref<LlmProvider[]>([])
const providerLoading = ref(false)

const modelTableData = ref<LlmModel[]>([])
const modelLoading = ref(false)
const modelTotal = ref(0)
const modelQueryParams = reactive<QueryPagedLlmModelsVo>({
  providerId: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10,
})

const authorizationVisible = ref(false)
const authorizationLoading = ref(false)
const authorizationActionLoadingRole = ref<RoleType | ''>('')
const currentModel = ref<LlmModel | null>(null)
const currentAuthorizedRoles = ref<RoleType[]>([])

// 用户组授权相关
const authorizationTab = ref('roles')
const allGroups = ref<UserGroup[]>([])
const currentAuthorizedGroups = ref<string[]>([])
const groupActionLoading = ref('')

const modelColumns = [
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName', width: 180 },
  { title: '供应商名称', key: 'providerName', width: 180 },
  { title: '真实模型名称', dataIndex: 'realModelName', key: 'realModelName', width: 220 },
  { title: '展示名称', dataIndex: 'modelDisplayName', key: 'modelDisplayName', width: 220 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 160, fixed: 'right' as const },
]

const roleColumns = [
  { title: '角色', key: 'roleType', width: 200 },
  { title: '说明', key: 'roleLabel' },
  { title: '当前状态', key: 'authorized', width: 120 },
  { title: '操作', key: 'actions', width: 140 },
]

const providerNameMap = computed<Record<string, string>>(() =>
  providerList.value.reduce(
    (acc, provider) => {
      acc[provider.id] = provider.providerName
      return acc
    },
    {} as Record<string, string>,
  ),
)

const providerOptions = computed(() =>
  providerList.value.map((provider) => ({
    label: provider.providerName,
    value: provider.id,
  })),
)

const roleRows = computed(() =>
  ALL_ROLE_TYPES.map((roleType) => ({
    roleType,
    roleLabel: ROLE_LABELS[roleType],
    authorized: currentAuthorizedRoles.value.includes(roleType),
  })),
)

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

function getProviderName(providerId: string): string {
  return providerNameMap.value[providerId] || '-'
}

async function fetchProviders() {
  providerLoading.value = true
  try {
    const res = await adminListAllLlmProvidersApi()
    providerList.value = res.data.data || []
  } catch (error) {
    console.error('获取全部供应商失败:', error)
  } finally {
    providerLoading.value = false
  }
}

async function fetchModels() {
  modelLoading.value = true
  try {
    const res = await adminListLlmModelsApi({
      providerId: modelQueryParams.providerId || undefined,
      keyword: modelQueryParams.keyword?.trim() || undefined,
      pageNum: modelQueryParams.pageNum,
      pageSize: modelQueryParams.pageSize,
    })
    modelTableData.value = res.data.data.records || []
    modelTotal.value = res.data.data.total || 0
  } catch (error) {
    console.error('获取模型列表失败:', error)
  } finally {
    modelLoading.value = false
  }
}

async function initializePage() {
  await fetchProviders()
  fetchModels()
}

function handleSearch() {
  modelQueryParams.pageNum = 1
  fetchModels()
}

function handleReset() {
  modelQueryParams.providerId = undefined
  modelQueryParams.keyword = ''
  modelQueryParams.pageNum = 1
  fetchModels()
}

function handlePageChange(page: number, pageSize?: number) {
  modelQueryParams.pageNum = page
  if (pageSize) {
    modelQueryParams.pageSize = pageSize
  }
  fetchModels()
}

async function fetchModelAvailableRoles() {
  if (!currentModel.value) return
  authorizationLoading.value = true
  try {
    const res = await adminGetLlmModelAvailableRolesApi({ modelId: currentModel.value.id })
    currentAuthorizedRoles.value = res.data.data || []
  } catch (error) {
    console.error('获取模型授权状态失败:', error)
  } finally {
    authorizationLoading.value = false
  }
}

async function openAuthorizationModal(model: LlmModel) {
  currentModel.value = model
  currentAuthorizedRoles.value = []
  currentAuthorizedGroups.value = []
  authorizationTab.value = 'roles'
  authorizationVisible.value = true
  await Promise.all([fetchModelAvailableRoles(), fetchModelAvailableGroups(), fetchAllGroups()])
}

function closeAuthorizationModal() {
  authorizationVisible.value = false
  currentModel.value = null
  currentAuthorizedRoles.value = []
  currentAuthorizedGroups.value = []
  authorizationActionLoadingRole.value = ''
  groupActionLoading.value = ''
}

async function fetchAllGroups() {
  try {
    const res = await adminPageUserGroupsApi({ pageNum: 1, pageSize: 1000 })
    allGroups.value = res.data.data?.records || []
  } catch (error) {
    console.error('获取用户组列表失败:', error)
  }
}

async function fetchModelAvailableGroups() {
  if (!currentModel.value) return
  try {
    const res = await adminGetModelAvailableGroupsApi({ modelId: currentModel.value.id })
    currentAuthorizedGroups.value = res.data.data || []
  } catch (error) {
    console.error('获取模型用户组授权状态失败:', error)
  }
}

async function toggleGroupAuthorization(groupId: string, authorized: boolean) {
  if (!currentModel.value) return
  groupActionLoading.value = groupId
  try {
    if (authorized) {
      await adminRevokeModelOfGroupsApi({ modelId: currentModel.value.id, groupIds: [groupId] })
      const group = allGroups.value.find((g) => g.id === groupId)
      ui.success(`已取消用户组 "${group?.groupName || groupId}" 的授权`)
    } else {
      await adminAuthorizeModelToGroupsApi({ modelId: currentModel.value.id, groupIds: [groupId] })
      const group = allGroups.value.find((g) => g.id === groupId)
      ui.success(`已授权到用户组 "${group?.groupName || groupId}"`)
    }
    await fetchModelAvailableGroups()
  } catch (error) {
    console.error('更新模型用户组授权失败:', error)
  } finally {
    groupActionLoading.value = ''
  }
}

const groupRows = computed(() =>
  allGroups.value.map((group) => ({
    groupId: group.id,
    groupName: group.groupName,
    description: group.description || '-',
    authorized: currentAuthorizedGroups.value.includes(group.id),
  })),
)

async function toggleRoleAuthorization(roleType: RoleType, authorized: boolean) {
  if (!currentModel.value) return

  authorizationActionLoadingRole.value = roleType
  try {
    if (authorized) {
      await adminRevokeLlmModelRolesApi({
        modelId: currentModel.value.id,
        roleTypes: [roleType],
      })
      ui.success(`已取消 ${ROLE_LABELS[roleType]} 的授权`)
    } else {
      await adminAuthorizeLlmModelRolesApi({
        modelId: currentModel.value.id,
        roleTypes: [roleType],
      })
      ui.success(`已授权到 ${ROLE_LABELS[roleType]}`)
    }
    await fetchModelAvailableRoles()
  } catch (error) {
    console.error('更新模型授权失败:', error)
  } finally {
    authorizationActionLoadingRole.value = ''
  }
}

onMounted(() => {
  initializePage()
})
</script>

<template>
  <div class="model-authorization-management">
    <a-card class="content-card">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">模型与授权</span>
      </div>

      <div class="card-header">
        <div>
          <h3>模型与授权</h3>
          <p class="card-subtitle">按供应商筛选模型，并逐个角色查看或调整模型授权状态。</p>
        </div>
        <a-button :loading="providerLoading || modelLoading" @click="initializePage">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>

      <div class="filter-bar">
        <a-select
          v-model:value="modelQueryParams.providerId"
          allow-clear
          show-search
          class="filter-item filter-item--provider"
          placeholder="选择供应商"
          :options="providerOptions"
          :loading="providerLoading"
          option-filter-prop="label"
        />
        <a-input
          v-model:value="modelQueryParams.keyword"
          class="filter-item"
          placeholder="搜索模型名称 / 真实模型名称 / 展示名称"
          @press-enter="handleSearch"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-space>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">
            <template #icon><UndoOutlined /></template>
            重置
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="id"
        :columns="modelColumns"
        :data-source="modelTableData"
        :loading="modelLoading"
        :pagination="false"
        :scroll="{ x: 1280 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'providerName'">
            <span>{{ getProviderName(record.providerId) }}</span>
          </template>
          <template v-else-if="column.key === 'realModelName'">
            <span>{{ getValue(record.realModelName) }}</span>
          </template>
          <template v-else-if="column.key === 'modelDisplayName'">
            <span>{{ getValue(record.modelDisplayName) }}</span>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button size="small" type="primary" ghost @click="openAuthorizationModal(record)">
              <template #icon><SafetyCertificateOutlined /></template>
              编辑角色授权
            </a-button>
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
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-modal
      v-model:open="authorizationVisible"
      title="模型授权状态"
      width="900px"
      ok-text="关闭"
      :cancel-button-props="{ style: { display: 'none' } }"
      @ok="closeAuthorizationModal"
      @cancel="closeAuthorizationModal"
    >
      <template v-if="currentModel">
        <div class="authorization-header">
          <div class="authorization-title">{{ currentModel.modelName }}</div>
          <div class="authorization-meta">
            <span>供应商：{{ getProviderName(currentModel.providerId) }}</span>
          </div>
        </div>

        <a-tabs v-model:activeKey="authorizationTab">
          <a-tab-pane key="roles" tab="角色授权">
            <a-table
              row-key="roleType"
              :columns="roleColumns"
              :data-source="roleRows"
              :loading="authorizationLoading"
              :pagination="false"
              :scroll="{ x: 760 }"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'roleType'">
                  <span>{{ record.roleType }}</span>
                </template>
                <template v-else-if="column.key === 'roleLabel'">
                  <span>{{ record.roleLabel }}</span>
                </template>
                <template v-else-if="column.key === 'authorized'">
                  <a-tag :color="record.authorized ? 'success' : 'default'">
                    {{ record.authorized ? '已授权' : '未授权' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button
                    size="small"
                    :type="record.authorized ? 'default' : 'primary'"
                    :danger="record.authorized"
                    :loading="authorizationActionLoadingRole === record.roleType"
                    @click="toggleRoleAuthorization(record.roleType, record.authorized)"
                  >
                    {{ record.authorized ? '取消授权' : '授权' }}
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="groups" tab="用户组授权">
            <a-table
              row-key="groupId"
              :columns="[
                { title: '用户组名称', key: 'groupName', dataIndex: 'groupName', width: 200 },
                { title: '描述', key: 'description', dataIndex: 'description' },
                { title: '当前状态', key: 'authorized', width: 120 },
                { title: '操作', key: 'actions', width: 140 },
              ]"
              :data-source="groupRows"
              :pagination="false"
              :scroll="{ x: 660 }"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'authorized'">
                  <a-tag :color="record.authorized ? 'success' : 'default'">
                    {{ record.authorized ? '已授权' : '未授权' }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button
                    size="small"
                    :type="record.authorized ? 'default' : 'primary'"
                    :danger="record.authorized"
                    :loading="groupActionLoading === record.groupId"
                    @click="toggleGroupAuthorization(record.groupId, record.authorized)"
                  >
                    {{ record.authorized ? '取消授权' : '授权' }}
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-modal>
  </div>
</template>

<style scoped>
.model-authorization-management {
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filter-item {
  width: 320px;
  max-width: 100%;
}

.filter-item--provider {
  width: 360px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.authorization-header {
  margin-bottom: 16px;
}

.authorization-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.authorization-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #8c8c8c;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }

  .filter-item,
  .filter-item--provider {
    width: 100%;
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
