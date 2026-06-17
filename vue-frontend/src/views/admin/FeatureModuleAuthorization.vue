<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons-vue'
import {
  adminGetModuleAvailableRolesApi,
  adminAuthorizeModuleRolesApi,
  adminRevokeModuleRolesApi,
  adminGetModuleAvailableGroupsApi,
  adminAuthorizeModuleToGroupsApi,
  adminRevokeModuleOfGroupsApi,
  adminPageUserGroupsApi,
} from '@/api/apiAdmin'
import type { RoleType, UserGroup } from '@/types/admin'
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

interface ModuleInfo {
  code: string
  label: string
  desc: string
}

const moduleList: ModuleInfo[] = [
  { code: 'rag_knowledge_base', label: 'RAG 知识库', desc: 'RAG 知识库管理与检索' },
]

const tableLoading = ref(false)

const authorizationVisible = ref(false)
const authorizationLoading = ref(false)
const authorizationActionLoadingRole = ref<RoleType | ''>('')
const currentModule = ref<ModuleInfo | null>(null)
const currentAuthorizedRoles = ref<RoleType[]>([])

// 用户组授权相关
const authorizationTab = ref('roles')
const allGroups = ref<UserGroup[]>([])
const currentAuthorizedGroups = ref<string[]>([])
const groupActionLoading = ref('')

const columns = [
  { title: '模块代码', key: 'code', dataIndex: 'code', width: 180 },
  { title: '名称', key: 'label', dataIndex: 'label', width: 160 },
  { title: '说明', key: 'desc', dataIndex: 'desc' },
  { title: '操作', key: 'actions', width: 160 },
]

const roleColumns = [
  { title: '角色', key: 'roleType', width: 200 },
  { title: '说明', key: 'roleLabel' },
  { title: '当前状态', key: 'authorized', width: 120 },
  { title: '操作', key: 'actions', width: 140 },
]

const roleRows = computed(() =>
  ALL_ROLE_TYPES.map((roleType) => ({
    roleType,
    roleLabel: ROLE_LABELS[roleType],
    authorized: currentAuthorizedRoles.value.includes(roleType),
  })),
)

async function fetchModuleAvailableRoles() {
  if (!currentModule.value) return
  authorizationLoading.value = true
  try {
    const res = await adminGetModuleAvailableRolesApi({ moduleCode: currentModule.value.code })
    currentAuthorizedRoles.value = res.data.data || []
  } catch (error) {
    console.error('获取功能模块授权角色失败:', error)
  } finally {
    authorizationLoading.value = false
  }
}

function openAuthorizationModal(module: ModuleInfo) {
  currentModule.value = module
  currentAuthorizedRoles.value = []
  currentAuthorizedGroups.value = []
  authorizationTab.value = 'roles'
  authorizationVisible.value = true
  Promise.all([fetchModuleAvailableRoles(), fetchModuleAvailableGroups(), fetchAllGroups()])
}

async function fetchAllGroups() {
  try {
    const res = await adminPageUserGroupsApi({ pageNum: 1, pageSize: 1000 })
    allGroups.value = res.data.data?.records || []
  } catch (error) {
    console.error('获取用户组列表失败:', error)
  }
}

async function fetchModuleAvailableGroups() {
  if (!currentModule.value) return
  try {
    const res = await adminGetModuleAvailableGroupsApi({ moduleCode: currentModule.value.code })
    currentAuthorizedGroups.value = res.data.data || []
  } catch (error) {
    console.error('获取功能模块用户组授权状态失败:', error)
  }
}

async function toggleGroupAuthorization(groupId: string, authorized: boolean) {
  if (!currentModule.value) return
  groupActionLoading.value = groupId
  try {
    if (authorized) {
      await adminRevokeModuleOfGroupsApi({ moduleCode: currentModule.value.code, groupIds: [groupId] })
      const group = allGroups.value.find((g) => g.id === groupId)
      ui.success(`已取消用户组 "${group?.groupName || groupId}" 的授权`)
    } else {
      await adminAuthorizeModuleToGroupsApi({ moduleCode: currentModule.value.code, groupIds: [groupId] })
      const group = allGroups.value.find((g) => g.id === groupId)
      ui.success(`已授权到用户组 "${group?.groupName || groupId}"`)
    }
    await fetchModuleAvailableGroups()
  } catch (error) {
    console.error('更新功能模块用户组授权失败:', error)
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

async function handleAuthorizeRole(roleType: RoleType) {
  if (!currentModule.value) return
  authorizationActionLoadingRole.value = roleType
  try {
    await adminAuthorizeModuleRolesApi({ moduleCode: currentModule.value.code, roleTypes: [roleType] })
    ui.success('授权成功')
    fetchModuleAvailableRoles()
  } catch (error) {
    console.error('授权失败:', error)
  } finally {
    authorizationActionLoadingRole.value = ''
  }
}

async function handleRevokeRole(roleType: RoleType) {
  if (!currentModule.value) return
  authorizationActionLoadingRole.value = roleType
  try {
    await adminRevokeModuleRolesApi({ moduleCode: currentModule.value.code, roleTypes: [roleType] })
    ui.success('取消授权成功')
    fetchModuleAvailableRoles()
  } catch (error) {
    console.error('取消授权失败:', error)
  } finally {
    authorizationActionLoadingRole.value = ''
  }
}

onMounted(() => {
})
</script>

<template>
  <a-card class="content-card">
    <div class="page-path">
      <span class="page-path-root">后台管理</span>
      <span class="page-path-separator"> / </span>
      <span class="page-path-current">特定功能模块授权</span>
    </div>

    <div class="card-header">
      <h3>特定功能模块授权</h3>
    </div>

    <a-table
      row-key="code"
      :columns="columns"
      :data-source="moduleList"
      :pagination="false"
      :scroll="{ x: 800 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a-button
            type="primary"
            size="small"
            @click="openAuthorizationModal(record)"
          >
            <template #icon><SafetyCertificateOutlined /></template>
            授权
          </a-button>
        </template>
      </template>
    </a-table>
  </a-card>

  <a-modal
    v-model:open="authorizationVisible"
    title="功能模块授权"
    width="900px"
    :footer="null"
    @cancel="authorizationVisible = false"
  >
    <a-tabs v-model:activeKey="authorizationTab">
      <a-tab-pane key="roles" tab="角色授权">
        <a-table
          row-key="roleType"
          :columns="roleColumns"
          :data-source="roleRows"
          :loading="authorizationLoading"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'roleType'">
              <span>{{ ROLE_LABELS[record.roleType as RoleType] }}</span>
            </template>
            <template v-if="column.key === 'roleLabel'">
              {{ record.roleLabel }}
            </template>
            <template v-if="column.key === 'authorized'">
              <a-tag v-if="record.authorized" color="success">已授权</a-tag>
              <a-tag v-else color="default">未授权</a-tag>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button
                v-if="record.authorized"
                size="small"
                :loading="authorizationActionLoadingRole === record.roleType"
                @click="handleRevokeRole(record.roleType)"
              >
                取消授权
              </a-button>
              <a-button
                v-else
                type="primary"
                size="small"
                :loading="authorizationActionLoadingRole === record.roleType"
                @click="handleAuthorizeRole(record.roleType)"
              >
                授权
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
  </a-modal>
</template>

<style scoped>
.content-card {
  flex: 1;
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
</style>
