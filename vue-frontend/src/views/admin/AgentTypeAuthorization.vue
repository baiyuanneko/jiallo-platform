<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons-vue'
import {
  adminAuthorizeAgentTypeRolesApi,
  adminGetAgentTypeAvailableRolesApi,
  adminRevokeAgentTypeRolesApi,
  adminGetAgentTypeAvailableGroupsApi,
  adminAuthorizeAgentTypeToGroupsApi,
  adminRevokeAgentTypeOfGroupsApi,
  adminPageUserGroupsApi,
} from '@/api/apiAdmin'
import { AgentType } from '@/types/chat'
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

interface AgentTypeInfo {
  value: number
  label: string
  desc: string
}

const agentTypeList: AgentTypeInfo[] = [
  { value: AgentType.CHAT_CLIENT, label: 'LLM 工具调用', desc: '将工具调用以函数调用格式进行发送' },
  { value: AgentType.REACT_AGENT, label: 'ReAct Agent', desc: 'ReAct 推理与行动循环' },
  // { value: AgentType.DIGITAL_BYN, label: 'byn-medium-v1', desc: 'byn 的数字分身' },
]

const tableLoading = ref(false)

const authorizationVisible = ref(false)
const authorizationLoading = ref(false)
const authorizationActionLoadingRole = ref<RoleType | ''>('')
const currentAgentType = ref<AgentTypeInfo | null>(null)
const currentAuthorizedRoles = ref<RoleType[]>([])

// 用户组授权相关
const authorizationTab = ref('roles')
const allGroups = ref<UserGroup[]>([])
const currentAuthorizedGroups = ref<string[]>([])
const groupActionLoading = ref('')

const columns = [
  { title: '智能体ID', key: 'agentType', dataIndex: 'value', width: 120 },
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

async function fetchAgentTypeAvailableRoles() {
  if (!currentAgentType.value) return
  authorizationLoading.value = true
  try {
    const res = await adminGetAgentTypeAvailableRolesApi({ agentType: currentAgentType.value.value })
    currentAuthorizedRoles.value = res.data.data || []
  } catch (error) {
    console.error('获取 AgentType 授权状态失败:', error)
  } finally {
    authorizationLoading.value = false
  }
}

async function openAuthorizationModal(agentType: AgentTypeInfo) {
  currentAgentType.value = agentType
  currentAuthorizedRoles.value = []
  currentAuthorizedGroups.value = []
  authorizationTab.value = 'roles'
  authorizationVisible.value = true
  await Promise.all([fetchAgentTypeAvailableRoles(), fetchAgentTypeAvailableGroups(), fetchAllGroups()])
}

function closeAuthorizationModal() {
  authorizationVisible.value = false
  currentAgentType.value = null
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

async function fetchAgentTypeAvailableGroups() {
  if (!currentAgentType.value) return
  try {
    const res = await adminGetAgentTypeAvailableGroupsApi({ agentType: currentAgentType.value.value })
    currentAuthorizedGroups.value = res.data.data || []
  } catch (error) {
    console.error('获取 AgentType 用户组授权状态失败:', error)
  }
}

async function toggleGroupAuthorization(groupId: string, authorized: boolean) {
  if (!currentAgentType.value) return
  groupActionLoading.value = groupId
  try {
    if (authorized) {
      await adminRevokeAgentTypeOfGroupsApi({ agentType: currentAgentType.value.value, groupIds: [groupId] })
      const group = allGroups.value.find((g) => g.id === groupId)
      ui.success(`已取消用户组 "${group?.groupName || groupId}" 的授权`)
    } else {
      await adminAuthorizeAgentTypeToGroupsApi({ agentType: currentAgentType.value.value, groupIds: [groupId] })
      const group = allGroups.value.find((g) => g.id === groupId)
      ui.success(`已授权到用户组 "${group?.groupName || groupId}"`)
    }
    await fetchAgentTypeAvailableGroups()
  } catch (error) {
    console.error('更新 AgentType 用户组授权失败:', error)
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
  if (!currentAgentType.value) return

  authorizationActionLoadingRole.value = roleType
  try {
    if (authorized) {
      await adminRevokeAgentTypeRolesApi({
        agentType: currentAgentType.value.value,
        roleTypes: [roleType],
      })
      ui.success(`已取消 ${ROLE_LABELS[roleType]} 的授权`)
    } else {
      await adminAuthorizeAgentTypeRolesApi({
        agentType: currentAgentType.value.value,
        roleTypes: [roleType],
      })
      ui.success(`已授权到 ${ROLE_LABELS[roleType]}`)
    }
    await fetchAgentTypeAvailableRoles()
  } catch (error) {
    console.error('更新 AgentType 授权失败:', error)
  } finally {
    authorizationActionLoadingRole.value = ''
  }
}

function refresh() {
  if (authorizationVisible.value && currentAgentType.value) {
    fetchAgentTypeAvailableRoles()
  }
}

onMounted(() => {
  // No data to fetch on mount — data is loaded on modal open
})
</script>

<template>
  <div class="agent-type-authorization">
    <a-card class="content-card">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">智能体授权</span>
      </div>

      <div class="card-header">
        <div>
          <h3>智能体授权</h3>
          <p class="card-subtitle">管理各 AgentType 的角色授权，未授权的 AgentType 将不会在聊天界面中展示。</p>
        </div>
        <a-button @click="refresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>

      <a-table
        row-key="value"
        :columns="columns"
        :data-source="agentTypeList"
        :loading="tableLoading"
        :pagination="false"
        :scroll="{ x: 640 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'agentType'">
            <span>{{ record.value }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button size="small" type="primary" ghost @click="openAuthorizationModal(record)">
              <template #icon><SafetyCertificateOutlined /></template>
              编辑角色授权
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="authorizationVisible"
      title="AgentType 授权状态"
      width="900px"
      ok-text="关闭"
      :cancel-button-props="{ style: { display: 'none' } }"
      @ok="closeAuthorizationModal"
      @cancel="closeAuthorizationModal"
    >
      <template v-if="currentAgentType">
        <div class="authorization-header">
          <div class="authorization-title">{{ currentAgentType.label }}</div>
          <div class="authorization-meta">
            <span>AgentType: {{ currentAgentType.value }}</span>
            <span>{{ currentAgentType.desc }}</span>
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
.agent-type-authorization {
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

  :deep(.ant-table-wrapper) {
    overflow-x: auto;
  }
}
</style>
