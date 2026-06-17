<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  SearchOutlined,
  PlusOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  LockOutlined,
  UnlockOutlined,
  KeyOutlined,
  DesktopOutlined,
  TeamOutlined,
  CopyOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue'
import {
  adminListUsersApi,
  adminCreateUserApi,
  adminUpdateUserApi,
  adminDeleteUserApi,
  adminBanUserApi,
  adminResetPasswordApi,
  adminListAllUserGroupsApi,
  adminGetUserDetailApi,
  adminGetUserSessionsApi,
  adminGetUserLogsApi,
  adminRevokeUserSessionsApi,
  adminAddMembersToGroupApi,
  adminRemoveMembersFromGroupApi,
} from '@/api/apiAdmin'
import type {
  QueryUsersVo,
  CreateUserVo,
  UpdateUserVo,
  RoleType,
  UserDetailResponse,
  UserGroup,
} from '@/types/admin'
import type { User, UserLog, UserSession } from '@/types/user'
import { useUserStore } from '@/stores/user'
import { UAParser } from 'ua-parser-js'
import { ui } from '@/utils/ui'

const userStore = useUserStore()

const tableData = ref<User[]>([])
const loading = ref(false)
const total = ref(0)

const queryParams = reactive<QueryUsersVo>({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  roleType: undefined,
  banned: undefined,
  emailVerified: undefined,
})

const roleTypeOptions: { label: string; value: RoleType }[] = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
  { label: 'VIP 用户', value: 'VIP_USER' },
  { label: '香子兰用户', value: 'VANILLA' },
  { label: '待邮箱验证', value: 'AWAIT_EMAIL_VERIFICATION' },
]

const createDialogVisible = ref(false)
const createFormRef = ref()
const createForm = reactive<CreateUserVo>({
  username: '',
  displayName: '',
  email: '',
  roleType: 'USER',
  sendEmail: false,
})
const createLoading = ref(false)

const passwordDialogVisible = ref(false)
const generatedPassword = ref('')
const generatedUsername = ref('')

const editDialogVisible = ref(false)
const editFormRef = ref()
const editForm = reactive<UpdateUserVo>({
  userId: '',
  displayName: '',
  email: '',
  emailVerified: false,
  roleType: undefined,
})
const editLoading = ref(false)

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailActiveTab = ref('info')
const userDetail = ref<UserDetailResponse | null>(null)
const allUserGroups = ref<UserGroup[]>([])
const groupLoading = ref(false)
const groupActionLoading = ref(false)
const selectedGroupIds = ref<string[]>([])
const userSessions = ref<UserSession[]>([])
const userLogs = ref<UserLog[]>([])
const userLogsTotal = ref(0)
const userLogsPage = ref(1)
const userLogsPageSize = ref(10)
const sessionsLoading = ref(false)
const logsLoading = ref(false)

const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' },
  ],
  roleType: [{ required: true, message: '请选择角色类型', trigger: 'change' }],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
}

const editRules = {
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
}

const currentUserId = computed(() => userStore.userInfo?.id)

const tableColumns = [
  { title: '用户名', key: 'username', dataIndex: 'username', width: 140 },
  { title: '显示名称', key: 'displayName', dataIndex: 'displayName', width: 140 },
  { title: '邮箱', key: 'email', dataIndex: 'email' },
  { title: '角色', key: 'roleType', dataIndex: 'roleType', width: 130 },
  { title: '状态', key: 'banned', dataIndex: 'banned', width: 100 },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' as const },
]

const logColumns = [
  { title: '操作类型', key: 'behaviour', dataIndex: 'behaviour', width: 130 },
  { title: '描述', key: 'message', dataIndex: 'message' },
  { title: 'IP', key: 'ip', dataIndex: 'ip', width: 130 },
  { title: '时间', key: 'createTime', dataIndex: 'createTime', width: 160 },
]

function getRoleTypeName(roleType: string) {
  const roleMap: Record<string, string> = {
    USER: '普通用户',
    ADMIN: '管理员',
    VIP_USER: 'VIP 用户',
    VANILLA: '香子兰用户',
    AWAIT_EMAIL_VERIFICATION: '待邮箱验证',
  }
  return roleMap[roleType] || roleType
}

function getRoleTypeColor(roleType: string) {
  const colorMap: Record<string, string> = {
    USER: 'default',
    ADMIN: 'error',
    VIP_USER: 'warning',
    VANILLA: 'processing',
    AWAIT_EMAIL_VERIFICATION: 'warning',
  }
  return colorMap[roleType] || 'default'
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function isCurrentUser(user: User): boolean {
  return user.id === userStore.userInfo?.id
}

function getUserGroupName(group: { groupName: string; description?: string }) {
  return group.groupName
}

const userGroupOptions = computed(() =>
  allUserGroups.value.map((group) => ({
    label: group.groupName,
    value: group.id,
  })),
)

const assignedGroupIds = computed(() => userDetail.value?.groups.map((group) => group.groupId) || [])

const availableGroupOptions = computed(() =>
  userGroupOptions.value.filter((option) => !assignedGroupIds.value.includes(String(option.value))),
)

const assignedGroups = computed(() => userDetail.value?.groups || [])

async function fetchUsers() {
  loading.value = true
  try {
    const res = await adminListUsersApi(queryParams)
    if (res.data.code === 0) {
      tableData.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ui.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadAllUserGroups() {
  if (allUserGroups.value.length > 0) return
  groupLoading.value = true
  try {
    const res = await adminListAllUserGroupsApi()
    if (res.data.code === 0) {
      allUserGroups.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取用户组列表失败:', error)
  } finally {
    groupLoading.value = false
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchUsers()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.roleType = undefined
  queryParams.banned = undefined
  queryParams.emailVerified = undefined
  queryParams.pageNum = 1
  fetchUsers()
}

function handlePageChange(page: number, pageSize?: number) {
  queryParams.pageNum = page
  if (pageSize) {
    queryParams.pageSize = pageSize
  }
  fetchUsers()
}

function openCreateDialog() {
  createForm.username = ''
  createForm.displayName = ''
  createForm.email = ''
  createForm.roleType = 'USER'
  createForm.sendEmail = false
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
    const res = await adminCreateUserApi(createForm)
    if (res.data.code === 0) {
      ui.success('用户创建成功')
      createDialogVisible.value = false
      generatedPassword.value = res.data.data.initialPassword
      generatedUsername.value = res.data.data.username
      passwordDialogVisible.value = true
      fetchUsers()
    }
  } catch (error) {
    console.error('创建用户失败:', error)
  } finally {
    createLoading.value = false
  }
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(generatedPassword.value)
    ui.success('密码已复制到剪贴板')
  } catch {
    ui.error('复制失败，请手动复制')
  }
}

function openEditDialog(user: User) {
  editForm.userId = user.id
  editForm.displayName = user.displayName || ''
  editForm.email = user.email || ''
  editForm.emailVerified = user.emailVerified || false
  editForm.roleType = user.roleType
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
    const res = await adminUpdateUserApi(editForm)
    if (res.data.code === 0) {
      ui.success('用户信息更新成功')
      editDialogVisible.value = false
      fetchUsers()
    }
  } catch (error) {
    console.error('更新用户失败:', error)
  } finally {
    editLoading.value = false
  }
}

async function viewUserDetail(user: User, tab: 'info' | 'sessions' | 'logs' | 'groups' = 'info') {
  detailDialogVisible.value = true
  detailActiveTab.value = tab
  detailLoading.value = true
  userDetail.value = null
  selectedGroupIds.value = []
  userSessions.value = []
  userLogs.value = []
  userLogsTotal.value = 0
  userLogsPage.value = 1

  try {
    const res = await adminGetUserDetailApi(user.id)
    if (res.data.code === 0) {
      userDetail.value = res.data.data
      if (tab === 'groups') {
        await loadAllUserGroups()
      }
    }
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ui.error('获取用户详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function refreshUserDetail(userId: string) {
  try {
    const res = await adminGetUserDetailApi(userId)
    if (res.data.code === 0) {
      userDetail.value = res.data.data
    }
  } catch (error) {
    console.error('刷新用户详情失败:', error)
  }
}

async function loadUserSessions() {
  if (!userDetail.value) return
  sessionsLoading.value = true
  try {
    const res = await adminGetUserSessionsApi(userDetail.value.user.id)
    if (res.data.code === 0) {
      userSessions.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取用户会话失败:', error)
  } finally {
    sessionsLoading.value = false
  }
}

async function loadUserLogs() {
  if (!userDetail.value) return
  logsLoading.value = true
  try {
    const res = await adminGetUserLogsApi(
      userDetail.value.user.id,
      userLogsPage.value,
      userLogsPageSize.value,
    )
    if (res.data.code === 0) {
      userLogs.value = res.data.data.records
      userLogsTotal.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取用户日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

function handleDetailTabChange(key: string) {
  if (key === 'sessions' && userSessions.value.length === 0) {
    loadUserSessions()
  } else if (key === 'logs' && userLogs.value.length === 0) {
    loadUserLogs()
  } else if (key === 'groups') {
    loadAllUserGroups()
  }
}

async function addSelectedGroups() {
  if (!userDetail.value || selectedGroupIds.value.length === 0) return
  groupActionLoading.value = true
  try {
    const userId = userDetail.value.user.id
    for (const groupId of selectedGroupIds.value) {
      await adminAddMembersToGroupApi({
        groupId,
        userIds: [userId],
      })
    }
    ui.success('用户组添加成功')
    selectedGroupIds.value = []
    await refreshUserDetail(userId)
  } catch (error) {
    console.error('添加用户组失败:', error)
    ui.error('添加用户组失败')
  } finally {
    groupActionLoading.value = false
  }
}

async function removeGroup(groupId: string) {
  if (!userDetail.value) return
  groupActionLoading.value = true
  try {
    await adminRemoveMembersFromGroupApi({
      groupId,
      userIds: [userDetail.value.user.id],
    })
    ui.success('用户组移除成功')
    await refreshUserDetail(userDetail.value.user.id)
  } catch (error) {
    console.error('移除用户组失败:', error)
    ui.error('移除用户组失败')
  } finally {
    groupActionLoading.value = false
  }
}

function handleLogsPageChange(page: number, pageSize?: number) {
  userLogsPage.value = page
  if (pageSize) {
    userLogsPageSize.value = pageSize
  }
  loadUserLogs()
}

async function toggleBan(user: User) {
  if (isCurrentUser(user)) {
    ui.warning('不能封禁自己')
    return
  }

  const action = user.banned ? '解封' : '封禁'
  try {
    await ui.confirm({
      title: `确认${action}`,
      content: `确定要${action}用户 "${user.username}" 吗？`,
      type: 'warning',
    })

    const res = await adminBanUserApi({
      userId: user.id,
      banned: !user.banned,
    })
    if (res.data.code === 0) {
      ui.success(`${action}成功`)
      fetchUsers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}用户失败:`, error)
    }
  }
}

async function resetPassword(user: User) {
  try {
    await ui.confirm({
      title: '确认重置密码',
      content: `确定要重置用户 "${user.username}" 的密码吗？重置后将生成新的随机密码。`,
      type: 'warning',
    })

    const res = await adminResetPasswordApi({ userId: user.id })
    if (res.data.code === 0) {
      ui.success('密码重置成功')
      generatedPassword.value = res.data.data.newPassword
      generatedUsername.value = user.username
      passwordDialogVisible.value = true
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
    }
  }
}

async function revokeUserSessions(user: User) {
  try {
    await ui.confirm({
      title: '确认注销会话',
      content: `确定要注销用户 "${user.username}" 的所有会话吗？该用户将被强制下线。`,
      type: 'warning',
    })

    const res = await adminRevokeUserSessionsApi(user.id)
    if (res.data.code === 0) {
      ui.success('会话已全部注销')
      if (detailDialogVisible.value && userDetail.value?.user.id === user.id) {
        loadUserSessions()
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销会话失败:', error)
    }
  }
}

async function deleteUser(user: User) {
  if (isCurrentUser(user)) {
    ui.warning('不能删除自己')
    return
  }

  try {
    await ui.confirm({
      title: '确认删除',
      content: `确定要删除用户 "${user.username}" 吗？此操作不可恢复！`,
      type: 'warning',
    })

    const res = await adminDeleteUserApi({ userId: user.id })
    if (res.data.code === 0) {
      ui.success('用户已删除')
      fetchUsers()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
    }
  }
}

function parseUserAgent(userAgent: string) {
  const parser = new UAParser(userAgent)
  const result = parser.getResult()
  return {
    browser: result.browser.name || '未知浏览器',
    browserVersion: result.browser.version || '',
    os: result.os.name || '未知系统',
    osVersion: result.os.version || '',
  }
}

function formatDeviceInfo(session: UserSession): string {
  if (!session.userAgent) return '未知设备'
  const info = parseUserAgent(session.userAgent)
  return `${info.browser} ${info.browserVersion} · ${info.os} ${info.osVersion}`
}

function getBehaviourName(behaviour: string): string {
  const behaviourMap: Record<string, string> = {
    SUCCESSFUL_LOGIN: '成功登录',
    FAILED_LOGIN: '登录失败',
    PASSWORD_CHANGE: '修改密码',
    SUCCESSFUL_REGISTER: '成功注册',
    EMAIL_CODE_INVALID: '邮箱验证码错误',
    SEND_EMAIL_CODE: '发送邮箱验证码',
    SSO_CLIENT_LOGIN: 'SSO 登录',
  }
  return behaviourMap[behaviour] || behaviour
}

function getBehaviourType(behaviour: string): string {
  const typeMap: Record<string, string> = {
    SUCCESSFUL_LOGIN: 'success',
    FAILED_LOGIN: 'error',
    PASSWORD_CHANGE: 'warning',
    SUCCESSFUL_REGISTER: 'success',
    EMAIL_CODE_INVALID: 'error',
    SEND_EMAIL_CODE: 'processing',
    SSO_CLIENT_LOGIN: 'processing',
  }
  return typeMap[behaviour] || 'default'
}

function getUserMenuItems(user: User) {
  return [
    { key: 'manage-groups', label: '管理用户组', icon: TeamOutlined, disabled: false },
    { key: 'reset-password', label: '重置密码', icon: KeyOutlined, disabled: false },
    {
      key: 'toggle-ban',
      label: user.banned ? '解封' : '封禁',
      icon: user.banned ? UnlockOutlined : LockOutlined,
      disabled: isCurrentUser(user),
    },
    { key: 'revoke-sessions', label: '注销会话', icon: DesktopOutlined, disabled: false },
    { key: 'delete', label: '删除', icon: DeleteOutlined, danger: true, disabled: isCurrentUser(user) },
  ]
}

function handleUserMenuClick(key: string, user: User) {
  if (key === 'reset-password') {
    resetPassword(user)
  } else if (key === 'manage-groups') {
    viewUserDetail(user, 'groups')
  } else if (key === 'toggle-ban') {
    toggleBan(user)
  } else if (key === 'revoke-sessions') {
    revokeUserSessions(user)
  } else if (key === 'delete') {
    deleteUser(user)
  }
}

function getTableRowClass(record: User) {
  return record.id === currentUserId.value ? 'current-user-row' : ''
}

function getLogRowKey(record: UserLog) {
  return `${record.id || ''}-${record.createTime || ''}-${record.behaviour}`
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="user-management">
    <a-card class="content-card">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">用户管理</span>
      </div>

      <div class="card-header">
        <h3>用户管理</h3>
        <a-button type="primary" @click="openCreateDialog">
          <template #icon><PlusOutlined /></template>
          创建用户
        </a-button>
      </div>

      <div class="search-bar">
        <a-input
          v-model:value="queryParams.keyword"
          placeholder="搜索用户名、邮箱、显示名称"
          allow-clear
          style="width: 250px"
          @press-enter="handleSearch"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="queryParams.roleType"
          placeholder="角色类型"
          allow-clear
          style="width: 150px"
          :options="roleTypeOptions"
        />
        <a-select
          v-model:value="queryParams.banned"
          placeholder="封禁状态"
          allow-clear
          style="width: 120px"
          :options="[
            { label: '正常', value: false },
            { label: '已封禁', value: true },
          ]"
        />
        <a-select
          v-model:value="queryParams.emailVerified"
          placeholder="邮箱验证"
          allow-clear
          style="width: 120px"
          :options="[
            { label: '已验证', value: true },
            { label: '未验证', value: false },
          ]"
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
        :row-key="(record: User) => record.id"
        :row-class-name="getTableRowClass"
        size="middle"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <span class="username-cell">
              {{ record.username }}
              <a-tag v-if="record.id === currentUserId" color="processing">当前用户</a-tag>
            </span>
          </template>
          <template v-else-if="column.key === 'displayName'">
            {{ record.displayName || '-' }}
          </template>
          <template v-else-if="column.key === 'email'">
            <span v-if="record.email">
              {{ record.email }}
              <a-tag
                :color="record.emailVerified ? 'success' : 'warning'"
                size="small"
                style="margin-left: 4px"
              >
                {{ record.emailVerified ? '已验证' : '未验证' }}
              </a-tag>
            </span>
            <span v-else class="text-muted">未绑定</span>
          </template>
          <template v-else-if="column.key === 'roleType'">
            <a-tag :color="getRoleTypeColor(record.roleType)">
              {{ getRoleTypeName(record.roleType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'banned'">
            <a-tag :color="record.banned ? 'error' : 'success'">
              {{ record.banned ? '已封禁' : '正常' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="table-actions">
              <a-button type="link" @click="viewUserDetail(record)">
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
                      v-for="item in getUserMenuItems(record)"
                      :key="item.key"
                      :disabled="item.disabled"
                      :danger="item.danger"
                      @click="handleUserMenuClick(item.key, record)"
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
      title="创建用户"
      :confirm-loading="createLoading"
      ok-text="创建"
      cancel-text="取消"
      @ok="handleCreateSubmit"
    >
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="createForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="显示名称" name="displayName">
          <a-input v-model:value="createForm.displayName" placeholder="请输入显示名称（可选）" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="createForm.email" placeholder="请输入邮箱（可选）" />
        </a-form-item>
        <a-form-item label="角色类型" name="roleType">
          <a-select v-model:value="createForm.roleType" :options="roleTypeOptions" />
        </a-form-item>
        <a-form-item label="发送邮件">
          <div class="switch-row">
            <a-switch v-model:checked="createForm.sendEmail" />
            <span class="form-tip">开启后将发送包含密码的邮件给用户</span>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="editDialogVisible"
      title="编辑用户"
      :confirm-loading="editLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleEditSubmit"
    >
      <a-form ref="editFormRef" :model="editForm" :rules="editRules" layout="vertical">
        <a-form-item label="显示名称" name="displayName">
          <a-input v-model:value="editForm.displayName" placeholder="请输入显示名称" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="editForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="邮箱已验证">
          <a-switch v-model:checked="editForm.emailVerified" />
        </a-form-item>
        <a-form-item label="角色类型" name="roleType">
          <a-select v-model:value="editForm.roleType" :options="roleTypeOptions" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="passwordDialogVisible"
      title="用户密码"
      :footer="null"
      :mask-closable="false"
    >
      <a-alert
        message="请妥善保存以下密码，关闭后将无法再次查看！"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <div class="password-display">
        <div class="password-item">
          <span class="password-label">用户名：</span>
          <span class="password-value">{{ generatedUsername }}</span>
        </div>
        <div class="password-item">
          <span class="password-label">密码：</span>
          <a-input :value="generatedPassword" readonly />
          <a-button @click="copyPassword">
            <template #icon><CopyOutlined /></template>
            复制
          </a-button>
        </div>
      </div>
      <div class="modal-actions">
        <a-button type="primary" @click="passwordDialogVisible = false">我已保存</a-button>
      </div>
    </a-modal>

    <a-modal
      v-model:open="detailDialogVisible"
      title="用户详情"
      :footer="null"
      :width="700"
      :mask-closable="false"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="userDetail">
          <a-tabs v-model:activeKey="detailActiveTab" @change="handleDetailTabChange">
            <a-tab-pane key="info" tab="基本信息">
              <div class="detail-info-list">
                <div class="detail-info-item">
                  <span class="detail-label">用户 ID</span>
                  <span class="detail-value">{{ userDetail.user.id }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">用户名</span>
                  <span class="detail-value">{{ userDetail.user.username }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">显示名称</span>
                  <span class="detail-value">{{ userDetail.user.displayName || '-' }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">邮箱</span>
                  <span class="detail-value">
                    {{ userDetail.user.email || '未绑定' }}
                    <a-tag
                      v-if="userDetail.user.email"
                      :color="userDetail.user.emailVerified ? 'success' : 'warning'"
                    >
                      {{ userDetail.user.emailVerified ? '已验证' : '未验证' }}
                    </a-tag>
                  </span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">角色</span>
                  <span class="detail-value">
                    <a-tag :color="getRoleTypeColor(userDetail.user.roleType)">
                      {{ getRoleTypeName(userDetail.user.roleType) }}
                    </a-tag>
                  </span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">状态</span>
                  <span class="detail-value">
                    <a-tag :color="userDetail.user.banned ? 'error' : 'success'">
                      {{ userDetail.user.banned ? '已封禁' : '正常' }}
                    </a-tag>
                  </span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">用户组</span>
                  <span class="detail-value">
                    <template v-if="userDetail.groups.length">
                      <a-tag
                        v-for="group in userDetail.groups"
                        :key="group.groupId"
                        color="blue"
                        style="margin-right: 6px; margin-bottom: 6px"
                      >
                        {{ group.groupName }}
                      </a-tag>
                    </template>
                    <span v-else>-</span>
                  </span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">活跃会话数</span>
                  <span class="detail-value">{{ userDetail.activeSessionCount }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">最近登录时间</span>
                  <span class="detail-value">{{ formatTime(userDetail.lastLoginTime) }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">最近登录 IP</span>
                  <span class="detail-value">{{ userDetail.lastLoginIp || '-' }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">操作日志数</span>
                  <span class="detail-value">{{ userDetail.totalLogCount }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">创建时间</span>
                  <span class="detail-value">{{ formatTime(userDetail.user.createTime) }}</span>
                </div>
                <div class="detail-info-item">
                  <span class="detail-label">更新时间</span>
                  <span class="detail-value">{{ formatTime(userDetail.user.updateTime) }}</span>
                </div>
              </div>
            </a-tab-pane>

            <a-tab-pane key="groups" tab="用户组">
              <a-spin :spinning="groupLoading">
                <div class="group-panel">
                  <div class="group-section">
                    <div class="group-section-header">
                      <h4>当前所属用户组</h4>
                      <span class="group-count">{{ assignedGroups.length }}</span>
                    </div>
                    <div v-if="assignedGroups.length === 0" class="empty-hint">暂无用户组</div>
                    <div v-else class="group-tag-list">
                      <a-tag
                        v-for="group in assignedGroups"
                        :key="group.groupId"
                        :closable="!groupActionLoading"
                        :color="groupActionLoading ? undefined : 'blue'"
                        @close="removeGroup(group.groupId)"
                      >
                        {{ getUserGroupName(group) }}
                      </a-tag>
                    </div>
                  </div>

                  <div class="group-section">
                    <div class="group-section-header">
                      <h4>添加到用户组</h4>
                    </div>
                    <a-select
                      v-model:value="selectedGroupIds"
                      mode="multiple"
                      placeholder="选择要添加的用户组"
                      style="width: 100%"
                      :options="availableGroupOptions"
                      :loading="groupLoading"
                      :disabled="availableGroupOptions.length === 0"
                      show-search
                      option-filter-prop="label"
                    />
                    <div class="group-actions">
                      <a-button
                        type="primary"
                        :loading="groupActionLoading"
                        :disabled="selectedGroupIds.length === 0"
                        @click="addSelectedGroups"
                      >
                        添加所选用户组
                      </a-button>
                    </div>
                  </div>
                </div>
              </a-spin>
            </a-tab-pane>

            <a-tab-pane key="sessions" tab="会话列表">
              <a-spin :spinning="sessionsLoading">
                <a-empty v-if="userSessions.length === 0" description="暂无活动会话" />
                <div v-else class="sessions-list">
                  <div v-for="session in userSessions" :key="session.id" class="session-item">
                    <div class="session-info">
                      <div class="session-device">{{ formatDeviceInfo(session) }}</div>
                      <div class="session-details">
                        <span>IP: {{ session.ip || '未知' }}</span>
                        <span>登录时间: {{ formatTime(session.createTime) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </a-spin>
            </a-tab-pane>

            <a-tab-pane key="logs" tab="操作日志">
              <a-spin :spinning="logsLoading">
                <a-table
                  :columns="logColumns"
                  :data-source="userLogs"
                  :pagination="false"
                  :row-key="getLogRowKey"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'behaviour'">
                      <a-tag :color="getBehaviourType(record.behaviour)">
                        {{ getBehaviourName(record.behaviour) }}
                      </a-tag>
                    </template>
                    <template v-else-if="column.key === 'message'">
                      {{ record.message || '-' }}
                    </template>
                    <template v-else-if="column.key === 'ip'">
                      {{ record.ip || '-' }}
                    </template>
                    <template v-else-if="column.key === 'createTime'">
                      {{ formatTime(record.createTime) }}
                    </template>
                  </template>
                </a-table>
                <div class="pagination-wrapper" v-if="userLogsTotal > userLogsPageSize">
                  <a-pagination
                    v-model:current="userLogsPage"
                    :page-size="userLogsPageSize"
                    :total="userLogsTotal"
                    size="small"
                    @change="handleLogsPageChange"
                  />
                </div>
              </a-spin>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>

      <div class="modal-actions">
        <a-button @click="detailDialogVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.user-management {
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

.text-muted,
.form-tip {
  color: #8c8c8c;
}

.form-tip {
  font-size: 12px;
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

.username-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.password-display {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.password-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.password-item :deep(.ant-input) {
  flex: 1;
}

.password-label {
  width: 80px;
  font-weight: 500;
  color: #595959;
}

.password-value {
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
  width: 120px;
  font-weight: 500;
  color: #595959;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.group-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-section-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.group-count {
  color: #8c8c8c;
  font-size: 12px;
}

.group-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.group-actions {
  display: flex;
  justify-content: flex-end;
}

.session-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.session-device {
  font-weight: 500;
  color: #262626;
  margin-bottom: 6px;
}

.session-details {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #8c8c8c;
}

:deep(.current-user-row) {
  background-color: #f0f7ff;
}

:deep(.current-user-row:hover > td) {
  background-color: #e6f2ff;
}

@media (max-width: 768px) {
  .password-item,
  .detail-info-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-label,
  .password-label {
    width: auto;
  }

  .search-bar {
    gap: 10px;
    flex-wrap: wrap;
  }

  .search-bar :deep(.ant-input-affix-wrapper),
  .search-bar :deep(.ant-select) {
    width: 100% !important;
  }

  :deep(.ant-table-wrapper) {
    overflow-x: auto;
  }
}
</style>
