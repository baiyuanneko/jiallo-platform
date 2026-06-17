<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
  UserAddOutlined,
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import {
  adminPageUserGroupsApi,
  adminCreateUserGroupApi,
  adminUpdateUserGroupApi,
  adminDeleteUserGroupApi,
  adminGetGroupMembersApi,
  adminAddMembersToGroupApi,
  adminRemoveMembersFromGroupApi,
  adminListUsersApi,
} from '@/api/apiAdmin'
import type { GroupMemberUser, UserGroup } from '@/types/admin'
import type { RoleType } from '@/types/admin'
import { ui } from '@/utils/ui'

// ==================== 用户组列表 ====================

const tableLoading = ref(false)
const userGroups = ref<UserGroup[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')

const columns = [
  { title: '用户组名称', key: 'groupName', dataIndex: 'groupName', width: 200 },
  { title: '描述', key: 'description', dataIndex: 'description' },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 260 },
]

async function fetchUserGroups() {
  tableLoading.value = true
  try {
    const res = await adminPageUserGroupsApi({
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    userGroups.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } catch (error) {
    console.error('获取用户组列表失败:', error)
  } finally {
    tableLoading.value = false
  }
}

function handlePageChange(page: number, size: number) {
  pageNum.value = page
  pageSize.value = size
  fetchUserGroups()
}

function handleSearch() {
  pageNum.value = 1
  fetchUserGroups()
}

// ==================== 创建/编辑用户组 ====================

const editModalVisible = ref(false)
const editModalLoading = ref(false)
const editMode = ref<'create' | 'update'>('create')
const editForm = ref({
  groupId: '',
  groupName: '',
  description: '',
})

function openCreateModal() {
  editMode.value = 'create'
  editForm.value = { groupId: '', groupName: '', description: '' }
  editModalVisible.value = true
}

function openEditModal(group: UserGroup) {
  editMode.value = 'update'
  editForm.value = {
    groupId: group.id,
    groupName: group.groupName,
    description: group.description || '',
  }
  editModalVisible.value = true
}

async function handleEditSubmit() {
  if (!editForm.value.groupName.trim()) {
    ui.error('用户组名称不能为空')
    return
  }
  editModalLoading.value = true
  try {
    if (editMode.value === 'create') {
      await adminCreateUserGroupApi({
        groupName: editForm.value.groupName.trim(),
        description: editForm.value.description.trim() || undefined,
      })
      ui.success('用户组创建成功')
    } else {
      await adminUpdateUserGroupApi({
        groupId: editForm.value.groupId,
        groupName: editForm.value.groupName.trim(),
        description: editForm.value.description.trim() || undefined,
      })
      ui.success('用户组更新成功')
    }
    editModalVisible.value = false
    await fetchUserGroups()
  } catch (error) {
    console.error('保存用户组失败:', error)
  } finally {
    editModalLoading.value = false
  }
}

// ==================== 删除用户组 ====================

async function handleDelete(group: UserGroup) {
  try {
    await adminDeleteUserGroupApi({ groupId: group.id })
    ui.success(`用户组 "${group.groupName}" 已删除`)
    await fetchUserGroups()
  } catch (error) {
    console.error('删除用户组失败:', error)
  }
}

// ==================== 管理成员 ====================

const memberModalVisible = ref(false)
const memberLoading = ref(false)
const currentGroup = ref<UserGroup | null>(null)
const memberUsers = ref<GroupMemberUser[]>([])

// 添加成员相关
const addUserSearchKeyword = ref('')
const userSearchResults = ref<Array<{ id: string; username: string; displayName: string }>>([])
const userSearchLoading = ref(false)
const addUserLoading = ref(false)
const removeUserLoading = ref('')

async function openMemberModal(group: UserGroup) {
  currentGroup.value = group
  memberUsers.value = []
  addUserSearchKeyword.value = ''
  userSearchResults.value = []
  memberModalVisible.value = true
  await fetchMembers()
}

async function fetchMembers() {
  if (!currentGroup.value) return
  memberLoading.value = true
  try {
    const res = await adminGetGroupMembersApi(currentGroup.value.id)
    memberUsers.value = res.data.data || []
  } catch (error) {
    console.error('获取成员列表失败:', error)
  } finally {
    memberLoading.value = false
  }
}

async function searchUsers() {
  if (!addUserSearchKeyword.value.trim()) return
  userSearchLoading.value = true
  try {
    const res = await adminListUsersApi({
      keyword: addUserSearchKeyword.value.trim(),
      pageNum: 1,
      pageSize: 20,
    })
    const users = res.data.data?.records || []
    // 过滤掉已在组中的用户
    userSearchResults.value = users
      .filter((u: any) => !memberUsers.value.some((member) => member.userId === u.id))
      .map((u: any) => ({
        id: u.id,
        username: u.username || '',
        displayName: u.displayName || '',
      }))
  } catch (error) {
    console.error('搜索用户失败:', error)
  } finally {
    userSearchLoading.value = false
  }
}

async function addUserToGroup(userId: string) {
  if (!currentGroup.value) return
  addUserLoading.value = true
  try {
    await adminAddMembersToGroupApi({
      groupId: currentGroup.value.id,
      userIds: [userId],
    })
    ui.success('成员添加成功')
    await fetchMembers()
    // 从搜索结果中移除
    userSearchResults.value = userSearchResults.value.filter((u) => u.id !== userId)
  } catch (error) {
    console.error('添加成员失败:', error)
  } finally {
    addUserLoading.value = false
  }
}

async function removeUserFromGroup(userId: string) {
  if (!currentGroup.value) return
  removeUserLoading.value = userId
  try {
    await adminRemoveMembersFromGroupApi({
      groupId: currentGroup.value.id,
      userIds: [userId],
    })
    ui.success('成员移除成功')
    await fetchMembers()
  } catch (error) {
    console.error('移除成员失败:', error)
  } finally {
    removeUserLoading.value = ''
  }
}

function closeMemberModal() {
  memberModalVisible.value = false
  currentGroup.value = null
  memberUsers.value = []
}

onMounted(() => {
  fetchUserGroups()
})
</script>

<template>
  <div class="user-group-management">
    <a-card class="content-card">
      <div class="page-path">
        <span class="page-path-root">后台管理</span>
        <span class="page-path-separator"> / </span>
        <span class="page-path-current">用户组管理</span>
      </div>

      <div class="card-header">
        <div>
          <h3>用户组管理</h3>
          <p class="card-subtitle">创建和管理用户组，分配用户到组，实现按用户组授权。</p>
        </div>
        <div class="header-actions">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索用户组名称"
            style="width: 200px"
            @search="handleSearch"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input-search>
          <a-button @click="fetchUserGroups">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button type="primary" @click="openCreateModal">
            <template #icon><PlusOutlined /></template>
            创建用户组
          </a-button>
        </div>
      </div>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="userGroups"
        :loading="tableLoading"
        :pagination="{
          current: pageNum,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handlePageChange,
        }"
        :scroll="{ x: 700 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'description'">
            <span>{{ record.description || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="primary" ghost @click="openEditModal(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button size="small" @click="openMemberModal(record)">
                <template #icon><TeamOutlined /></template>
                管理成员
              </a-button>
              <a-popconfirm
                title="确定要删除该用户组吗？删除后组成员和授权关系将同步清除。"
                @confirm="handleDelete(record)"
              >
                <a-button size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑用户组 Modal -->
    <a-modal
      v-model:open="editModalVisible"
      :title="editMode === 'create' ? '创建用户组' : '编辑用户组'"
      :confirm-loading="editModalLoading"
      @ok="handleEditSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="用户组名称" required>
          <a-input v-model:value="editForm.groupName" placeholder="请输入用户组名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="editForm.description"
            placeholder="请输入用户组描述（可选）"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 管理成员 Modal -->
    <a-modal
      v-model:open="memberModalVisible"
      :title="currentGroup ? `管理成员 - ${currentGroup.groupName}` : '管理成员'"
      width="800px"
      ok-text="关闭"
      :cancel-button-props="{ style: { display: 'none' } }"
      @ok="closeMemberModal"
      @cancel="closeMemberModal"
    >
      <template v-if="currentGroup">
        <!-- 当前成员列表 -->
        <div class="member-section">
          <h4>当前成员（{{ memberUsers.length }}）</h4>
          <a-spin :spinning="memberLoading">
            <div v-if="memberUsers.length === 0" class="empty-hint">暂无成员</div>
            <div v-else class="member-list">
              <a-tag
                v-for="member in memberUsers"
                :key="member.userId"
                closable
                :color="removeUserLoading === member.userId ? undefined : 'blue'"
                @close="removeUserFromGroup(member.userId)"
              >
                {{ member.displayName || member.username || member.userId }}
                <span class="member-tag-id">({{ member.userId }})</span>
              </a-tag>
            </div>
          </a-spin>
        </div>

        <!-- 搜索添加用户 -->
        <div class="member-section">
          <h4>添加成员</h4>
          <div class="add-user-row">
            <a-input
              v-model:value="addUserSearchKeyword"
              placeholder="输入用户名或 ID 搜索用户"
              @press-enter="searchUsers"
              style="flex: 1"
            />
            <a-button
              type="primary"
              :loading="userSearchLoading"
              @click="searchUsers"
            >
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
          </div>
          <div v-if="userSearchResults.length > 0" class="search-results">
            <div
              v-for="user in userSearchResults"
              :key="user.id"
              class="search-result-item"
            >
              <span class="user-info">
                <span class="user-name">{{ user.displayName || user.username }}</span>
                <span class="user-id">（ID: {{ user.id }}）</span>
              </span>
              <a-button
                size="small"
                type="primary"
                :loading="addUserLoading"
                @click="addUserToGroup(user.id)"
              >
                <template #icon><UserAddOutlined /></template>
                添加
              </a-button>
            </div>
          </div>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<style scoped>
.user-group-management {
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

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.member-section {
  margin-bottom: 24px;
}

.member-section h4 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.empty-hint {
  color: #8c8c8c;
  font-size: 13px;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-tag-id {
  margin-left: 4px;
  font-size: 12px;
  color: inherit;
  opacity: 0.75;
}

.add-user-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}

.user-info {
  display: flex;
  gap: 4px;
  align-items: baseline;
}

.user-name {
  font-weight: 500;
}

.user-id {
  font-size: 12px;
  color: #8c8c8c;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }
}
</style>
