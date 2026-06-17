<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  DashboardOutlined,
  UserOutlined,
  LockOutlined,
  DesktopOutlined,
  EditOutlined,
  CheckOutlined,
  CloseOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  WarningOutlined,
  UnlockOutlined,
  FileTextOutlined,
  UploadOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  editUserInfoApi,
  changePasswordApi,
  bindEmailApi,
  bindEmailVerifyApi,
  unbindEmailApi,
  resendEmailCodeApi,
  viewUserLogApi,
  uploadAvatarApi,
  getMyAvatarApi,
  listActiveSessionsApi,
  listMyGroupsApi,
  revokeSessionByIdApi,
  revokeAllSessionsApi,
} from '@/api/apiUser'
import type { MyUserGroup, UserLog, UserSession } from '@/types/user'
import VuePictureCropper, { cropper } from 'vue-picture-cropper'
import { UAParser } from 'ua-parser-js'
import { ui } from '@/utils/ui'

const router = useRouter()
const userStore = useUserStore()

const activeMenu = ref('dashboard')
const isSidebarCollapsed = ref(false)
const activeLoginMethodTab = ref('password')
const passwordDialogVisible = ref(false)
const bindEmailDialogVisible = ref(false)
const verifyEmailDialogVisible = ref(false)
const avatarDialogVisible = ref(false)

const isEditingDisplayName = ref(false)
const editingDisplayName = ref('')

const passwordFormRef = ref()
const bindEmailFormRef = ref()
const verifyEmailFormRef = ref()

const passwordForm = ref({
  newPassword: '',
  confirmPassword: '',
})

const bindEmailForm = ref({
  email: '',
})

const verifyEmailForm = ref({
  emailCode: '',
})

const resendCountdown = ref(0)
let resendTimer: number | null = null

const userLogs = ref<UserLog[]>([])
const logTotal = ref(0)
const logCurrentPage = ref(1)
const logPageSize = ref(10)
const logLoading = ref(false)
const myGroups = ref<MyUserGroup[]>([])
const myGroupsLoading = ref(false)

const cropperImg = ref('')
const uploading = ref(false)
const avatarBlobUrl = ref<string>('')

const sessions = ref<UserSession[]>([])
const sessionsLoading = ref(false)
const currentSessionId = ref<string>('')

const validateConfirmPassword = async (_rule: unknown, value: string) => {
  if (value !== passwordForm.value.newPassword) {
    throw new Error('两次输入的密码不一致')
  }
}

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少为8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const bindEmailRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
}

const verifyEmailRules = {
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}

const userInitial = computed(() => {
  const username = userStore.userInfo?.username
  return username && username.length > 0 ? username.charAt(0).toUpperCase() : 'U'
})

const pageTitleMap: Record<string, string> = {
  dashboard: '仪表盘',
  'user-info': '用户信息',
  'login-method': '登录方式管理',
  'session-management': '会话管理',
  'security-logs': '敏感操作日志',
}

const currentPageTitle = computed(() => pageTitleMap[activeMenu.value] || '用户中心')

const dashboardServices = [
  { key: 'user-info', title: '用户信息', icon: UserOutlined },
  { key: 'login-method', title: '登录方式管理', icon: LockOutlined },
  { key: 'session-management', title: '会话管理', icon: UnlockOutlined },
  { key: 'security-logs', title: '敏感操作日志', icon: FileTextOutlined },
]

const userBasicInfo = computed(() => {
  const user = userStore.userInfo
  if (!user) return []

  return [
    { label: '用户名', value: user.username },
    { label: '显示名称', value: user.displayName || '未设置' },
    {
      label: '角色',
      value: getRoleTypeName(user.roleType),
      type: getRoleTypeColor(user.roleType),
      isRole: true,
    },
    {
      label: '账号状态',
      value: user.banned ? '已封禁' : '未封禁',
      type: user.banned ? 'danger' : 'success',
      isBanned: user.banned,
      isStatus: true,
    },
    {
      label: '注册时间',
      value: user.createTime ? new Date(user.createTime).toLocaleString('zh-CN') : '-',
    },
    {
      label: '更新时间',
      value: user.updateTime ? new Date(user.updateTime).toLocaleString('zh-CN') : '-',
    },
  ]
})

const passwordInfo = computed(() => {
  const user = userStore.userInfo
  if (!user) return []

  return [
    { label: '登录密码', value: '已设置' },
    {
      label: '密码更新时间',
      value: user.passwordUpdatedAt
        ? new Date(user.passwordUpdatedAt).toLocaleString('zh-CN')
        : '-',
    },
  ]
})

const accountBindingInfo = computed(() => {
  const user = userStore.userInfo
  if (!user) return []

  return [
    {
      label: '邮箱',
      value: user.email || '未绑定',
      status: user.email ? (user.emailVerified ? '已验证' : '未验证') : null,
      statusType: user.emailVerified ? 'success' : 'warning',
    },
    {
      label: '手机号',
      value: user.mobile || '未绑定',
      status: user.mobile ? (user.mobileVerified ? '已验证' : '未验证') : null,
      statusType: user.mobileVerified ? 'success' : 'warning',
    },
  ]
})

const logColumns = [
  { title: '操作类型', key: 'behaviour', dataIndex: 'behaviour', width: 140 },
  { title: '描述', key: 'message', dataIndex: 'message' },
  { title: 'IP 地址', key: 'ip', dataIndex: 'ip', width: 140 },
  { title: '操作时间', key: 'createTime', dataIndex: 'createTime', width: 180 },
]

async function loadAvatar() {
  if (!userStore.userInfo?.avatarUrl) {
    if (avatarBlobUrl.value) {
      URL.revokeObjectURL(avatarBlobUrl.value)
      avatarBlobUrl.value = ''
    }
    return
  }

  try {
    const blob = await getMyAvatarApi(userStore.avatarVersion)
    if (avatarBlobUrl.value) {
      URL.revokeObjectURL(avatarBlobUrl.value)
    }
    avatarBlobUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('加载头像失败:', error)
  }
}

async function fetchMyGroups() {
  myGroupsLoading.value = true
  try {
    const res = await listMyGroupsApi()
    if (res.data.code === 0) {
      myGroups.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取用户组失败:', error)
    ui.error('获取用户组失败')
  } finally {
    myGroupsLoading.value = false
  }
}

function getRoleTypeName(roleType: string) {
  const roleMap: Record<string, string> = {
    USER: '普通用户',
    ADMIN: '管理员',
    VIP_USER: 'VIP 用户',
    VANILLA: '香子兰用户组成员',
  }
  return roleMap[roleType] || roleType
}

function getRoleTypeColor(roleType: string): 'primary' | 'success' | 'info' | 'warning' | 'danger' {
  const colorMap: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
    USER: 'info',
    ADMIN: 'danger',
    VIP_USER: 'warning',
    VANILLA: 'primary',
  }
  return colorMap[roleType] || 'info'
}

function getTagColor(type?: string) {
  const colorMap: Record<string, string> = {
    primary: 'processing',
    success: 'success',
    info: 'default',
    warning: 'warning',
    danger: 'error',
  }
  return type ? colorMap[type] || 'default' : 'default'
}

function startEditDisplayName() {
  editingDisplayName.value = userStore.userInfo?.displayName || ''
  isEditingDisplayName.value = true
}

function cancelEditDisplayName() {
  isEditingDisplayName.value = false
  editingDisplayName.value = ''
}

async function saveDisplayName() {
  const newDisplayName = editingDisplayName.value.trim()
  if (!newDisplayName) {
    ui.warning('显示名称不能为空')
    return
  }

  try {
    const res = await editUserInfoApi({ displayName: newDisplayName })
    if (res.data.code === 0) {
      userStore.userInfo = res.data.data
      ui.success('修改成功')
      isEditingDisplayName.value = false
    }
  } catch (error) {
    console.error('修改用户信息失败:', error)
  }
}

function openPasswordDialog() {
  passwordForm.value.newPassword = ''
  passwordForm.value.confirmPassword = ''
  passwordDialogVisible.value = true
}

async function handlePasswordSubmit() {
  try {
    await passwordFormRef.value?.validate()
  } catch {
    return
  }

  try {
    const res = await changePasswordApi({ newPassword: passwordForm.value.newPassword })
    if (res.data.code === 0) {
      ui.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      setTimeout(() => {
        userStore.logout()
        window.location.href = '/login'
      }, 1500)
    }
  } catch (error) {
    console.error('修改密码失败:', error)
  }
}

function openBindEmailDialog() {
  bindEmailForm.value.email = ''
  bindEmailDialogVisible.value = true
}

async function handleBindEmailSubmit() {
  try {
    await bindEmailFormRef.value?.validate()
  } catch {
    return
  }

  try {
    const res = await bindEmailApi({ email: bindEmailForm.value.email })
    if (res.data.code === 0) {
      ui.success('验证码已发送到邮箱')
      bindEmailDialogVisible.value = false
      verifyEmailForm.value.emailCode = ''
      verifyEmailDialogVisible.value = true
      startResendCountdown()
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

async function handleVerifyEmailSubmit() {
  try {
    await verifyEmailFormRef.value?.validate()
  } catch {
    return
  }

  try {
    const res = await bindEmailVerifyApi({ emailCode: verifyEmailForm.value.emailCode })
    if (res.data.code === 0) {
      ui.success('邮箱绑定成功')
      verifyEmailDialogVisible.value = false
      stopResendCountdown()
      await userStore.fetchUserInfo()
    }
  } catch (error) {
    console.error('验证失败:', error)
  }
}

async function handleResendCode() {
  if (resendCountdown.value > 0) return

  try {
    const res = await resendEmailCodeApi()
    if (res.data.code === 0) {
      ui.success('验证码已重新发送')
      startResendCountdown()
    }
  } catch (error) {
    console.error('重新发送验证码失败:', error)
  }
}

function startResendCountdown() {
  resendCountdown.value = 60
  resendTimer = window.setInterval(() => {
    resendCountdown.value--
    if (resendCountdown.value <= 0) {
      stopResendCountdown()
    }
  }, 1000)
}

function stopResendCountdown() {
  if (resendTimer) {
    clearInterval(resendTimer)
    resendTimer = null
  }
  resendCountdown.value = 0
}

async function handleUnbindEmail() {
  try {
    await ui.confirm({
      title: '确认解绑',
      content: '解绑后将无法通过邮箱找回密码，确定要解绑邮箱吗？',
      type: 'warning',
    })

    const res = await unbindEmailApi()
    if (res.data.code === 0) {
      ui.success('邮箱解绑成功')
      await userStore.fetchUserInfo()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解绑邮箱失败:', error)
    }
  }
}

function handleVerifyDialogClose() {
  stopResendCountdown()
  verifyEmailDialogVisible.value = false
}

function getCurrentSessionId(): string {
  const currentSession = sessions.value.find((session) => session.isCurrentSession)
  return currentSession?.id || ''
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

function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function isExpiringSoon(session: UserSession): boolean {
  if (!session.refreshTokenExpireTime) return false
  const expireTime = new Date(session.refreshTokenExpireTime).getTime()
  const now = Date.now()
  return expireTime - now < 24 * 60 * 60 * 1000 && expireTime > now
}

function isExpired(session: UserSession): boolean {
  if (!session.refreshTokenExpireTime) return false
  return new Date(session.refreshTokenExpireTime).getTime() < Date.now()
}

function getSessionStatus(session: UserSession) {
  if (isExpired(session)) return { text: '已过期', type: 'danger' }
  if (isExpiringSoon(session)) return { text: '即将过期', type: 'warning' }
  if (session.id === currentSessionId.value) return { text: '当前会话', type: 'success' }
  return { text: '正常', type: 'info' }
}

async function fetchSessions() {
  sessionsLoading.value = true
  try {
    const res = await listActiveSessionsApi()
    if (res.data.code === 0) {
      sessions.value = res.data.data || []
      if (sessions.value.length > 0) {
        currentSessionId.value = getCurrentSessionId()
      }
    }
  } catch (error) {
    console.error('获取会话列表失败:', error)
    ui.error('获取会话列表失败')
  } finally {
    sessionsLoading.value = false
  }
}

async function handleRevokeSession(session: UserSession) {
  const isCurrent = session.id === currentSessionId.value

  try {
    await ui.confirm({
      title: '确认注销',
      content: isCurrent
        ? '您正在注销当前会话，注销后将自动退出登录。确定要继续吗？'
        : '确定要注销此会话吗？',
      type: 'warning',
    })

    const res = await revokeSessionByIdApi({ sessionId: session.id })
    if (res.data.code === 0) {
      ui.success('会话已注销')
      if (isCurrent) {
        userStore.logout()
        router.push('/login')
      } else {
        await fetchSessions()
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销会话失败:', error)
    }
  }
}

async function handleRevokeAllSessions() {
  try {
    await ui.confirm({
      title: '确认注销所有会话',
      content: '注销所有会话后，您将在所有设备上退出登录。确定要继续吗？',
      type: 'warning',
    })

    const res = await revokeAllSessionsApi()
    if (res.data.code === 0) {
      ui.success('所有会话已注销')
      userStore.logout()
      router.push('/login')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('注销所有会话失败:', error)
    }
  }
}

async function fetchUserLogs() {
  logLoading.value = true
  try {
    const res = await viewUserLogApi({
      pageNum: logCurrentPage.value,
      pageSize: logPageSize.value,
    })
    if (res.data.code === 0) {
      userLogs.value = res.data.data.records
      logTotal.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取用户日志失败:', error)
  } finally {
    logLoading.value = false
  }
}

function handlePageChange(page: number, pageSize?: number) {
  logCurrentPage.value = page
  if (pageSize) {
    logPageSize.value = pageSize
  }
  fetchUserLogs()
}

function getBehaviourName(behaviour: string): string {
  const behaviourMap: Record<string, string> = {
    SUCCESSFUL_LOGIN: '成功登录',
    FAILED_LOGIN: '登录失败',
    PASSWORD_CHANGE: '修改密码',
    SUCCESSFUL_REGISTER: '成功注册',
    EMAIL_CODE_INVALID: '邮箱验证码错误',
    SEND_EMAIL_CODE: '发送邮箱验证码',
  }
  return behaviourMap[behaviour] || behaviour
}

function getBehaviourType(behaviour: string): string {
  const typeMap: Record<string, string> = {
    SUCCESSFUL_LOGIN: 'success',
    FAILED_LOGIN: 'danger',
    PASSWORD_CHANGE: 'warning',
    SUCCESSFUL_REGISTER: 'success',
    EMAIL_CODE_INVALID: 'danger',
    SEND_EMAIL_CODE: 'info',
  }
  return typeMap[behaviour] || 'default'
}

function openAvatarDialog() {
  cropperImg.value = ''
  avatarDialogVisible.value = true
}

function handleSelectedFile(file: File) {
  const maxSize = 2 * 1024 * 1024
  if (file.size > maxSize) {
    ui.error('图片大小不能超过 2MB')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    cropperImg.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

function handleBeforeUpload(file: File) {
  handleSelectedFile(file)
  return false
}

async function handleUploadAvatar() {
  if (!cropperImg.value) {
    ui.warning('请先选择图片')
    return
  }

  uploading.value = true
  try {
    if (!cropper) {
      ui.error('裁剪器未初始化')
      uploading.value = false
      return
    }

    cropper
      .getCroppedCanvas({
        width: 200,
        height: 200,
      })
      .toBlob(async (blob) => {
        if (!blob) {
          ui.error('裁剪失败')
          uploading.value = false
          return
        }

        const formData = new FormData()
        formData.append('file', blob, 'avatar.jpg')

        const res = await uploadAvatarApi(formData)
        if (res.data.code === 0) {
          ui.success('头像上传成功')
          avatarDialogVisible.value = false
          cropperImg.value = ''
          userStore.incrementAvatarVersion()
          await userStore.fetchUserInfo()
          await loadAvatar()
        }
        uploading.value = false
      }, 'image/jpeg')
  } catch (error) {
    console.error('上传头像失败:', error)
    uploading.value = false
  }
}

function openMenuPage(key: string) {
  activeMenu.value = key
  if (key === 'session-management') {
    fetchSessions()
  }
  if (key === 'security-logs') {
    fetchUserLogs()
  }
}

function handleSidebarClick({ key }: { key: string }) {
  openMenuPage(key)
}

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

function getLogRowKey(record: UserLog) {
  return `${record.createTime || 'time'}-${record.behaviour}-${record.ip || 'ip'}-${record.message || ''}`
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  await loadAvatar()
  await fetchMyGroups()
})
</script>

<template>
  <div class="user-center">
    <div class="user-center-container">
      <aside class="sidebar" :class="{ 'sidebar--collapsed': isSidebarCollapsed }">
        <div class="sidebar-header">
          <button type="button" class="sidebar-toggle" @click="toggleSidebar">
            <MenuUnfoldOutlined v-if="isSidebarCollapsed" />
            <MenuFoldOutlined v-else />
          </button>
          <span v-if="!isSidebarCollapsed" class="sidebar-title">用户中心</span>
        </div>
        <a-menu
          mode="inline"
          :inline-collapsed="isSidebarCollapsed"
          :selected-keys="[activeMenu]"
          class="sidebar-menu"
          @click="handleSidebarClick"
        >
          <a-menu-item key="dashboard">
            <DashboardOutlined />
            <span>仪表盘</span>
          </a-menu-item>
          <a-menu-item key="user-info">
            <UserOutlined />
            <span>用户信息</span>
          </a-menu-item>
          <a-menu-item key="login-method">
            <LockOutlined />
            <span>登录方式管理</span>
          </a-menu-item>
          <a-menu-item key="session-management">
            <UnlockOutlined />
            <span>会话管理</span>
          </a-menu-item>
          <a-menu-item key="security-logs">
            <FileTextOutlined />
            <span>敏感操作日志</span>
          </a-menu-item>
        </a-menu>
      </aside>

      <main class="content">
        <a-card v-show="activeMenu === 'dashboard'" class="content-card">
          <div class="page-path">
            <span class="page-path-root">用户中心</span>
            <span class="page-path-separator"> / </span>
            <span class="page-path-current">{{ currentPageTitle }}</span>
          </div>
          <div class="card-header">
            <h3>仪表盘</h3>
          </div>

          <section class="dashboard-section">
            <h4 class="section-title">相关服务</h4>
            <div class="service-grid">
              <button
                v-for="service in dashboardServices"
                :key="service.key"
                type="button"
                class="service-entry"
                @click="openMenuPage(service.key)"
              >
                <component :is="service.icon" class="service-entry__icon" />
                <span class="service-entry__label">{{ service.title }}</span>
              </button>
            </div>
          </section>
        </a-card>

        <a-card v-show="activeMenu === 'user-info'" class="content-card">
          <div class="page-path">
            <span class="page-path-root">用户中心</span>
            <span class="page-path-separator"> / </span>
            <span class="page-path-current">{{ currentPageTitle }}</span>
          </div>
          <div class="card-header">
            <h3>用户信息</h3>
          </div>

          <div class="info-item avatar-info-item">
            <span class="info-label">头像</span>
            <div class="info-value">
              <div class="avatar-container avatar-container--editable" @click="openAvatarDialog">
                <a-avatar :size="100" :src="avatarBlobUrl || undefined">
                  {{ userInitial }}
                </a-avatar>
                <div class="avatar-overlay">
                  <UploadOutlined />
                  <span>更换头像</span>
                </div>
              </div>
            </div>
          </div>

          <div class="info-list">
            <div v-for="item in userBasicInfo" :key="item.label" class="info-item">
              <span class="info-label">{{ item.label }}</span>
              <span class="info-value">
                <template v-if="item.label === '显示名称'">
                  <div v-if="!isEditingDisplayName" class="editable-value">
                    <span>{{ item.value }}</span>
                    <a-button type="text" size="small" @click="startEditDisplayName">
                      <template #icon><EditOutlined /></template>
                    </a-button>
                  </div>
                  <div v-else class="edit-input-wrapper">
                    <a-input
                      v-model:value="editingDisplayName"
                      placeholder="请输入显示名称"
                      size="small"
                      style="width: 200px"
                      @press-enter="saveDisplayName"
                    />
                    <a-button type="text" size="small" @click="saveDisplayName">
                      <template #icon><CheckOutlined /></template>
                    </a-button>
                    <a-button type="text" size="small" @click="cancelEditDisplayName">
                      <template #icon><CloseOutlined /></template>
                    </a-button>
                  </div>
                </template>

                <template v-else-if="item.isRole">
                  <a-tag :color="getTagColor(item.type)">{{ item.value }}</a-tag>
                </template>

                <template v-else-if="item.isStatus">
                  <div class="status-wrapper">
                    <CheckCircleOutlined
                      v-if="!item.isBanned"
                      class="status-icon success-icon"
                    />
                    <CloseCircleOutlined v-else class="status-icon danger-icon" />
                    <span>{{ item.value }}</span>
                  </div>
                </template>

                <template v-else>
                  <a-tag v-if="item.type" :color="getTagColor(item.type)">{{ item.value }}</a-tag>
                  <span v-else>{{ item.value }}</span>
                </template>
              </span>
            </div>
          </div>

          <div class="group-section">
            <h4 class="section-title">所属用户组</h4>
            <a-spin :spinning="myGroupsLoading">
              <div v-if="myGroups.length === 0" class="empty-group-hint">
                暂无所属用户组
              </div>
              <div v-else class="group-tag-list">
                <a-tag v-for="group in myGroups" :key="group.groupId" color="blue">
                  {{ group.groupName }}
                </a-tag>
              </div>
            </a-spin>
          </div>
        </a-card>

        <a-card v-show="activeMenu === 'login-method'" class="content-card">
          <div class="page-path">
            <span class="page-path-root">用户中心</span>
            <span class="page-path-separator"> / </span>
            <span class="page-path-current">{{ currentPageTitle }}</span>
          </div>
          <div class="card-header">
            <h3>登录方式管理</h3>
          </div>

          <a-tabs v-model:activeKey="activeLoginMethodTab" class="login-method-tabs">
            <a-tab-pane key="password" tab="密码管理">
              <div class="section section--tabbed">
                <div class="info-list">
                  <div v-for="item in passwordInfo" :key="item.label" class="info-item">
                    <span class="info-label">{{ item.label }}</span>
                    <span class="info-value">{{ item.value }}</span>
                  </div>
                </div>
                <div class="action-buttons">
                  <a-button type="primary" @click="openPasswordDialog">修改密码</a-button>
                </div>
              </div>
            </a-tab-pane>

            <a-tab-pane key="verification" tab="辅助验证管理">
              <div class="section section--tabbed">
                <div class="info-list">
                  <div v-for="item in accountBindingInfo" :key="item.label" class="info-item">
                    <span class="info-label">{{ item.label }}</span>
                    <span class="info-value">
                      <span>{{ item.value }}</span>
                      <a-tag
                        v-if="item.status"
                        :color="getTagColor(item.statusType)"
                        style="margin-left: 8px"
                      >
                        {{ item.status }}
                      </a-tag>
                    </span>
                  </div>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>

        <div v-show="activeMenu === 'session-management'" class="content-panel">
          <a-card class="content-card">
            <div class="page-path">
              <span class="page-path-root">用户中心</span>
              <span class="page-path-separator"> / </span>
              <span class="page-path-current">{{ currentPageTitle }}</span>
            </div>
            <div class="card-header card-header--stacked">
              <h3>会话管理</h3>
              <p class="card-description">
                您可以在这里查看当前账号的所有活动会话，并在发现异常时及时进行处理。
              </p>
              <div class="session-page-actions">
                <a-button
                  type="primary"
                  danger
                  :disabled="sessions.length === 0"
                  @click="handleRevokeAllSessions"
                >
                  <template #icon><WarningOutlined /></template>
                  注销所有会话
                </a-button>
              </div>
            </div>

            <a-spin :spinning="sessionsLoading">
              <div class="sessions-list">
                <a-empty
                  v-if="sessions.length === 0 && !sessionsLoading"
                  description="暂无活动会话"
                />

                <div
                  v-for="session in sessions"
                  :key="session.id"
                  class="session-item"
                  :class="{ 'current-session': session.id === currentSessionId }"
                >
                  <div class="session-icon">
                    <DesktopOutlined />
                  </div>

                  <div class="session-info">
                    <div class="session-device">
                      <span class="device-name">{{ formatDeviceInfo(session) }}</span>
                      <a-tag :color="getTagColor(getSessionStatus(session).type)" style="margin-left: 8px">
                        {{ getSessionStatus(session).text }}
                      </a-tag>
                    </div>

                    <div class="session-details">
                      <div class="detail-item">
                        <span class="detail-label">IP 地址:</span>
                        <span class="detail-value">{{ session.ip || '未知' }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-label">登录时间:</span>
                        <span class="detail-value">{{ formatTime(session.createTime) }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-label">过期时间:</span>
                        <span class="detail-value">{{ formatTime(session.refreshTokenExpireTime) }}</span>
                      </div>
                    </div>

                    <div class="session-actions">
                      <a-button type="primary" danger @click="handleRevokeSession(session)">
                        <template #icon><DeleteOutlined /></template>
                        注销此会话
                      </a-button>
                    </div>
                  </div>
                </div>
              </div>
            </a-spin>
          </a-card>
        </div>

        <a-card v-show="activeMenu === 'security-logs'" class="content-card">
          <div class="page-path">
            <span class="page-path-root">用户中心</span>
            <span class="page-path-separator"> / </span>
            <span class="page-path-current">{{ currentPageTitle }}</span>
          </div>
          <div class="card-header">
            <h3>敏感操作日志</h3>
          </div>

          <a-table
            :columns="logColumns"
            :data-source="userLogs"
            :loading="logLoading"
            :pagination="false"
            :row-key="getLogRowKey"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'behaviour'">
                <a-tag :color="getTagColor(getBehaviourType(record.behaviour))">
                  {{ getBehaviourName(record.behaviour) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'message'">
                <span>{{ record.message || '-' }}</span>
              </template>
              <template v-else-if="column.key === 'ip'">
                <span>{{ record.ip || '-' }}</span>
              </template>
              <template v-else-if="column.key === 'createTime'">
                <span>{{ record.createTime ? new Date(record.createTime).toLocaleString('zh-CN') : '-' }}</span>
              </template>
            </template>
          </a-table>

          <div class="pagination-wrapper">
            <a-pagination
              v-model:current="logCurrentPage"
              v-model:page-size="logPageSize"
              :page-size-options="['10', '20', '50', '100']"
              :total="logTotal"
              show-size-changer
              show-quick-jumper
              show-less-items
              @change="handlePageChange"
            />
          </div>
        </a-card>
      </main>
    </div>

    <a-modal
      v-model:open="passwordDialogVisible"
      title="修改密码"
      :footer="null"
      :width="400"
    >
      <a-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" layout="vertical">
        <a-form-item label="新密码" name="newPassword">
          <a-input-password
            v-model:value="passwordForm.newPassword"
            placeholder="请输入新密码（至少8位）"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="确认新密码" name="confirmPassword">
          <a-input-password
            v-model:value="passwordForm.confirmPassword"
            placeholder="请再次输入新密码"
            allow-clear
          />
        </a-form-item>
      </a-form>
      <div class="modal-actions">
        <a-button @click="passwordDialogVisible = false">取消</a-button>
        <a-button type="primary" @click="handlePasswordSubmit">确定</a-button>
      </div>
    </a-modal>

    <a-modal
      v-model:open="bindEmailDialogVisible"
      title="绑定邮箱"
      :footer="null"
      :width="400"
    >
      <a-form ref="bindEmailFormRef" :model="bindEmailForm" :rules="bindEmailRules" layout="vertical">
        <a-form-item label="邮箱地址" name="email">
          <a-input v-model:value="bindEmailForm.email" placeholder="请输入邮箱地址" allow-clear />
        </a-form-item>
      </a-form>
      <div class="modal-actions">
        <a-button @click="bindEmailDialogVisible = false">取消</a-button>
        <a-button type="primary" @click="handleBindEmailSubmit">发送验证码</a-button>
      </div>
    </a-modal>

    <a-modal
      v-model:open="verifyEmailDialogVisible"
      title="验证邮箱"
      :footer="null"
      :width="400"
      @cancel="handleVerifyDialogClose"
    >
      <a-alert message="验证码已发送到您的邮箱" type="info" show-icon style="margin-bottom: 20px" />
      <a-form
        ref="verifyEmailFormRef"
        :model="verifyEmailForm"
        :rules="verifyEmailRules"
        layout="vertical"
      >
        <a-form-item label="验证码" name="emailCode">
          <div class="verify-code-row">
            <a-input
              v-model:value="verifyEmailForm.emailCode"
              placeholder="请输入6位验证码"
              :maxlength="6"
              allow-clear
            />
            <a-button :disabled="resendCountdown > 0" @click="handleResendCode">
              {{ resendCountdown > 0 ? `${resendCountdown}秒后重发` : '重新发送' }}
            </a-button>
          </div>
        </a-form-item>
      </a-form>
      <div class="modal-actions">
        <a-button @click="handleVerifyDialogClose">取消</a-button>
        <a-button type="primary" @click="handleVerifyEmailSubmit">确定</a-button>
      </div>
    </a-modal>

    <a-modal
      v-model:open="avatarDialogVisible"
      title="上传头像"
      :footer="null"
      :width="600"
    >
      <a-upload
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        accept="image/jpeg,image/png,image/gif,image/webp"
      >
        <a-button type="primary">
          <template #icon><UploadOutlined /></template>
          选择图片
        </a-button>
      </a-upload>
      <div class="upload-tip">支持 jpg/png/gif/webp 格式，大小不超过 2MB</div>

      <vue-picture-cropper
        v-if="cropperImg"
        :boxStyle="{
          width: '100%',
          height: '400px',
          marginTop: '20px',
          backgroundColor: '#f5f5f5',
          borderRadius: '8px',
        }"
        :img="cropperImg"
        :options="{
          aspectRatio: 1,
          viewMode: 1,
          autoCropArea: 0.8,
        }"
      />

      <div class="modal-actions">
        <a-button @click="avatarDialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="uploading" :disabled="!cropperImg" @click="handleUploadAvatar">
          上传
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.user-center {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0 20px 20px 0;
}

.user-center-container {
  display: flex;
  gap: 0;
  width: 100%;
  min-height: calc(100vh - 64px);
}

.sidebar {
  width: 220px;
  flex: 0 0 220px;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  background: white;
  border-right: 1px solid #d9d9d9;
  border-radius: 0;
  overflow: hidden;
  transition:
    width 0.2s ease,
    flex-basis 0.2s ease;
}

.sidebar--collapsed {
  width: 56px;
  flex-basis: 56px;
}

.sidebar-header {
  height: 48px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-toggle {
  border: none;
  background: transparent;
  padding: 0;
  color: #595959;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.sidebar-menu {
  border-inline-end: none;
  flex: 1;
}

.content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-self: stretch;
}

.content-panel {
  flex: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.content-card {
  flex: 1;
  min-height: 100%;
  border-radius: 0;
  box-shadow: none;
}

.login-method-tabs {
  margin-top: 4px;
}

.dashboard-section {
  padding-top: 8px;
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
  margin-bottom: 8px;
}

.card-header--stacked {
  display: block;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.card-description {
  margin: 8px 0 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #595959;
}

.avatar-info-item {
  align-items: center;
  padding: 24px 0;
}

.avatar-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-container--editable {
  cursor: pointer;
  position: relative;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-container--editable:hover .avatar-overlay {
  opacity: 1;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-item {
  display: flex;
  align-items: flex-start;
  padding: 18px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  width: 120px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  color: #262626;
}

.editable-value,
.edit-input-wrapper,
.status-wrapper,
.action-buttons,
.verify-code-row,
.modal-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-icon,
.action-icon {
  cursor: pointer;
  color: #8c8c8c;
}

.confirm-icon,
.success-icon {
  color: #52c41a;
}

.cancel-icon,
.danger-icon {
  color: #ff4d4f;
}

.section {
  padding: 20px 0;
}

.section--tabbed {
  padding-top: 4px;
}

.section:not(:last-child) {
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.group-section {
  padding: 20px 0 0;
  border-top: 1px solid #f0f0f0;
}

.group-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-group-hint {
  color: #8c8c8c;
  font-size: 13px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.service-entry {
  min-height: 56px;
  padding: 0 18px;
  border: 1px solid #e5e5e5;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #262626;
  cursor: pointer;
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease,
    background-color 0.2s ease;
}

.service-entry:hover {
  background: #f7f7f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.service-entry__icon {
  font-size: 18px;
  color: #595959;
}

.service-entry__label {
  font-size: 14px;
  font-weight: 500;
}

.action-buttons {
  margin-top: 24px;
  flex-wrap: wrap;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.session-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fafafa;
}

.current-session {
  border-color: #91caff;
  background: #f0f8ff;
}

.session-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.session-info {
  flex: 1;
}

.session-device {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.device-name {
  font-weight: 500;
  color: #262626;
}

.session-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-page-actions,
.session-actions {
  display: flex;
  justify-content: flex-start;
}

.session-actions {
  margin-top: 16px;
}

.detail-item {
  display: flex;
  gap: 8px;
  color: #595959;
}

.detail-label {
  color: #8c8c8c;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.upload-tip {
  margin-top: 10px;
  color: #8c8c8c;
  font-size: 13px;
}

@media (max-width: 768px) {
  .user-center {
    padding: 0 16px 16px;
  }

  .user-center-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-basis: auto;
  }

  .content {
    display: block;
  }

  .content-panel {
    min-height: auto;
  }

  .content-card {
    min-height: auto;
  }

  .sidebar-menu {
    display: flex;
    overflow-x: auto;
  }

  .sidebar-menu :deep(.ant-menu-item) {
    flex: 1;
    justify-content: center;
    min-width: 120px;
  }

  .info-item,
  .session-item {
    flex-direction: column;
  }

  .info-label {
    width: auto;
  }

  .session-actions,
  .pagination-wrapper,
  .modal-actions {
    justify-content: stretch;
  }

  .modal-actions > *,
  .session-actions > * {
    width: 100%;
  }
}
</style>
