<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  adminListClawSecurityLevelRolesApi,
  adminSetClawSecurityLevelRoleApi,
  adminRemoveClawSecurityLevelRoleApi,
  adminListClawSecurityLevelGroupsApi,
  adminSetClawSecurityLevelGroupApi,
  adminRemoveClawSecurityLevelGroupApi,
  adminPageUserGroupsApi,
} from '@/api/apiAdmin'
import type { RoleType, UserGroup } from '@/types/admin'
import { ui } from '@/utils/ui'

interface RoleOption {
  roleType: RoleType
  roleCode: number
  label: string
}

// roleCode 与后端 RoleType 枚举值保持一致
const ROLE_OPTIONS: RoleOption[] = [
  { roleType: 'USER', roleCode: 0, label: '普通用户' },
  { roleType: 'ADMIN', roleCode: 1, label: '管理员' },
  { roleType: 'VIP_USER', roleCode: 2, label: 'VIP 用户' },
  { roleType: 'VANILLA', roleCode: 3, label: 'Vanilla' },
  { roleType: 'AWAIT_EMAIL_VERIFICATION', roleCode: 4, label: '待邮箱验证' },
]

const LEVEL_OPTIONS = [
  { value: 2, label: '完全访问' },
  { value: 1, label: '工作区文件' },
  { value: 0, label: '无权限' },
]

const rolesLoading = ref(false)
const roleLevels = ref<Record<number, number>>({})
const roleSavingCode = ref<number | null>(null)

const groupsLoading = ref(false)
const allGroups = ref<UserGroup[]>([])
const groupLevels = ref<Record<string, number>>({})
const groupSavingId = ref('')

const roleColumns = [
  { title: '角色', key: 'roleLabel', dataIndex: 'roleLabel', width: 200 },
  { title: '安全级别', key: 'securityLevel', width: 220 },
]

const groupColumns = [
  { title: '用户组名称', key: 'groupName', dataIndex: 'groupName', width: 200 },
  { title: '描述', key: 'description', dataIndex: 'description' },
  { title: '安全级别', key: 'securityLevel', width: 220 },
]

const roleRows = computed(() =>
  ROLE_OPTIONS.map((role) => ({
    roleCode: role.roleCode,
    roleType: role.roleType,
    roleLabel: role.label,
    securityLevel: roleLevels.value[role.roleCode] ?? 0,
  })),
)

const groupRows = computed(() =>
  allGroups.value.map((group) => ({
    groupId: group.id,
    groupName: group.groupName,
    description: group.description || '-',
    securityLevel: groupLevels.value[group.id] ?? 0,
  })),
)

async function fetchRoleLevels() {
  rolesLoading.value = true
  try {
    const res = await adminListClawSecurityLevelRolesApi()
    const levels: Record<number, number> = {}
    for (const item of res.data.data || []) {
      levels[item.roleCode] = item.securityLevel
    }
    roleLevels.value = levels
  } catch (error) {
    console.error('获取角色 Claw 安全级别失败:', error)
  } finally {
    rolesLoading.value = false
  }
}

async function fetchAllGroups() {
  try {
    const res = await adminPageUserGroupsApi({ pageNum: 1, pageSize: 1000 })
    allGroups.value = res.data.data?.records || []
  } catch (error) {
    console.error('获取用户组列表失败:', error)
  }
}

async function fetchGroupLevels() {
  groupsLoading.value = true
  try {
    const res = await adminListClawSecurityLevelGroupsApi()
    const levels: Record<string, number> = {}
    for (const item of res.data.data || []) {
      levels[item.groupId] = item.securityLevel
    }
    groupLevels.value = levels
  } catch (error) {
    console.error('获取用户组 Claw 安全级别失败:', error)
  } finally {
    groupsLoading.value = false
  }
}

async function handleRoleLevelChange(roleCode: number, securityLevel: number) {
  roleSavingCode.value = roleCode
  try {
    if (securityLevel > 0) {
      await adminSetClawSecurityLevelRoleApi({ roleCode, securityLevel })
    } else {
      await adminRemoveClawSecurityLevelRoleApi({ roleCode })
    }
    ui.success('保存成功')
  } catch (error) {
    console.error('保存角色 Claw 安全级别失败:', error)
  } finally {
    roleSavingCode.value = null
  }
  await fetchRoleLevels()
}

async function handleGroupLevelChange(groupId: string, securityLevel: number) {
  groupSavingId.value = groupId
  try {
    if (securityLevel > 0) {
      await adminSetClawSecurityLevelGroupApi({ groupId, securityLevel })
    } else {
      await adminRemoveClawSecurityLevelGroupApi({ groupId })
    }
    ui.success('保存成功')
  } catch (error) {
    console.error('保存用户组 Claw 安全级别失败:', error)
  } finally {
    groupSavingId.value = ''
  }
  await fetchGroupLevels()
}

onMounted(() => {
  Promise.all([fetchRoleLevels(), fetchAllGroups(), fetchGroupLevels()])
})
</script>

<template>
  <a-card class="content-card">
    <div class="page-path">
      <span class="page-path-root">后台管理</span>
      <span class="page-path-separator"> / </span>
      <span class="page-path-current">Claw 安全级别设置</span>
    </div>

    <div class="card-header">
      <h3>Claw 安全级别设置</h3>
    </div>

    <div class="section-title" style="margin-top: 0">角色安全级别</div>
    <a-table
      row-key="roleCode"
      :columns="roleColumns"
      :data-source="roleRows"
      :loading="rolesLoading"
      :pagination="false"
      :scroll="{ x: 600 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'securityLevel'">
          <a-select
            :value="record.securityLevel"
            :options="LEVEL_OPTIONS"
            :loading="roleSavingCode === record.roleCode"
            style="width: 160px"
            @change="(val: any) => handleRoleLevelChange(record.roleCode, val)"
          />
        </template>
      </template>
    </a-table>

    <div class="section-title">用户组安全级别</div>
    <a-table
      row-key="groupId"
      :columns="groupColumns"
      :data-source="groupRows"
      :loading="groupsLoading"
      :pagination="false"
      :scroll="{ x: 800 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'securityLevel'">
          <a-select
            :value="record.securityLevel"
            :options="LEVEL_OPTIONS"
            :loading="groupSavingId === record.groupId"
            style="width: 160px"
            @change="(val: any) => handleGroupLevelChange(record.groupId, val)"
          />
        </template>
      </template>
    </a-table>
  </a-card>
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

.section-title {
  margin: 24px 0 12px;
  font-size: 15px;
  font-weight: 500;
  color: #262626;
}
</style>
