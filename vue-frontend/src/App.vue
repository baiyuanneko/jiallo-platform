<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  LoginOutlined,
  AppstoreOutlined,
  MenuOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMyAvatarApi } from '@/api/apiUser'
import { ui } from '@/utils/ui'

const mobileMenuOpen = ref(false)

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function updateManifest() {
  const isChat = route.path.startsWith('/app/agentic-chat-ui')
  let link = document.querySelector<HTMLLinkElement>('link[rel="manifest"]')
  if (isChat) {
    if (!link) {
      link = document.createElement('link')
      link.rel = 'manifest'
      document.head.appendChild(link)
    }
    link.href = '/manifest-chat.json?v=3'
  } else if (link) {
    link.remove()
  }
}

watch(() => route.path, updateManifest, { immediate: true })

const activeMenu = computed(() => {
  if (route.path.startsWith('/app')) {
    return ['/app']
  }

  return [route.path]
})
const avatarBlobUrl = ref<string>('')

const userInitial = computed(() => {
  return userStore.userInfo?.username?.[0]?.toUpperCase() || 'U'
})

const username = computed(() => {
  return userStore.userInfo?.username || ''
})

const displayName = computed(() => {
  return userStore.userInfo?.displayName || userStore.userInfo?.username || '未登录'
})

const isAdmin = computed(() => {
  return userStore.userInfo?.roleType === 'ADMIN'
})

const isStandalone = computed(() => {
  return !!route.meta.standaloneLayout
})

const isHomePage = computed(() => {
  return route.path === '/'
})

const shouldDisableRouteTransition = computed(() => {
  return route.path.startsWith('/admin') || route.path.startsWith('/app/agentic-chat-ui')
})

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

watch(
  () => userStore.userInfo?.avatarUrl,
  () => {
    loadAvatar()
  },
  { immediate: true },
)

watch(
  () => userStore.avatarVersion,
  () => {
    loadAvatar()
  },
)

function handleLogout() {
  ui.confirm({
    title: '提示',
    content: '确定要登出吗？',
    type: 'warning',
  })
    .then(() => {
      userStore.logout()
      ui.success('已登出')
      router.push('/')
    })
    .catch(() => {})
}

function handleLogoutMobile() {
  mobileMenuOpen.value = false
  handleLogout()
}

function goToLogin() {
  router.push('/login')
}

function goToUserCenter() {
  router.push('/user/center')
}

function goToAdmin() {
  router.push('/admin')
}

function goToHome() {
  router.push('/')
}

function goToAppList() {
  router.push('/app')
}

function goToChat() {
  router.push('/app/agentic-chat-ui')
}

function handleTopMenuClick({ key }: { key: string }) {
  router.push(key)
}
</script>

<template>
  <div class="app" :class="{ 'app--home': isHomePage, 'app--standalone': isStandalone }">
    <header v-if="!isStandalone" class="navbar" :class="{ 'navbar--home': isHomePage }">
      <div class="navbar__brand" @click="goToHome">
        <img src="/images/jiallo-platform-logo.png" alt="Jiallo Platform" class="navbar__logo" />
      </div>

      <div class="navbar__right">
        <a-menu
          mode="horizontal"
          :selected-keys="activeMenu"
          class="navbar__links"
          @click="handleTopMenuClick"
        >
          <a-menu-item key="/">关于</a-menu-item>
          <a-menu-item v-if="userStore.isLoggedIn" key="/app">
            <template #icon>
              <AppstoreOutlined />
            </template>
            应用列表
          </a-menu-item>
        </a-menu>

        <a-button
          class="navbar__mobile-toggle"
          type="text"
          @click="mobileMenuOpen = true"
        >
          <MenuOutlined />
        </a-button>

        <a-button
          v-if="userStore.isLoggedIn"
          type="primary"
          size="small"
          class="navbar__chat-btn"
          @click="goToChat"
        >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="arrow-up-right-icon"
          >
            <line x1="7" y1="17" x2="17" y2="7" />
            <polyline points="7 7 17 7 17 17" />
          </svg>
          尝试 Jiallo Chat
        </a-button>

        <div v-if="!userStore.isLoggedIn" class="navbar__user-dropdown">
          <a-dropdown trigger="click">
            <span class="navbar__avatar">
              <a-avatar size="small">{{ userInitial }}</a-avatar>
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item key="login" @click="goToLogin">
                  <LoginOutlined />
                  登录/注册
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>

        <div v-else class="navbar__user-dropdown">
          <a-dropdown trigger="click">
            <span class="navbar__avatar">
              <a-avatar :size="36" :src="avatarBlobUrl || undefined">{{ userInitial }}</a-avatar>
              <span class="navbar__username">{{ displayName }}</span>
            </span>
            <template #overlay>
              <div class="user-dropdown">
                <div class="user-card">
                  <a-avatar :size="50" :src="avatarBlobUrl || undefined">{{ userInitial }}</a-avatar>
                  <div class="user-info">
                    <div class="display-name">{{ displayName }}</div>
                    <div class="username-text">@{{ username }}</div>
                  </div>
                </div>
                <a-menu class="user-menu">
                  <a-menu-item key="user-center" @click="goToUserCenter">
                    <UserOutlined />
                    用户中心
                  </a-menu-item>
                  <a-menu-item v-if="isAdmin" key="admin" @click="goToAdmin">
                    <SettingOutlined />
                    后台管理
                  </a-menu-item>
                  <a-menu-item key="logout" danger @click="handleLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </div>
            </template>
          </a-dropdown>
        </div>
      </div>

      <a-drawer
        v-model:open="mobileMenuOpen"
        placement="right"
        :closable="true"
        :width="280"
        :destroy-on-close="true"
        class="navbar__mobile-drawer"
      >
        <div class="mobile-menu">
          <!-- 未登录：登录入口 -->
          <template v-if="!userStore.isLoggedIn">
            <div class="mobile-menu__item" @click="goToLogin(); mobileMenuOpen = false">
              <LoginOutlined /> 登录/注册
            </div>
          </template>

          <!-- 已登录：用户卡片 -->
          <template v-else>
            <div class="mobile-menu__user-card">
              <a-avatar :size="44" :src="avatarBlobUrl || undefined">{{ userInitial }}</a-avatar>
              <div class="mobile-menu__user-info">
                <div class="mobile-menu__display-name">{{ displayName }}</div>
                <div class="mobile-menu__username">@{{ username }}</div>
              </div>
            </div>
            <div class="mobile-menu__divider"></div>
          </template>

          <div class="mobile-menu__item" @click="goToHome(); mobileMenuOpen = false">
            关于
          </div>
          <div v-if="userStore.isLoggedIn" class="mobile-menu__item" @click="goToAppList(); mobileMenuOpen = false">
            <AppstoreOutlined /> 应用列表
          </div>
          <div v-if="userStore.isLoggedIn" class="mobile-menu__item mobile-menu__item--chat" @click="goToChat(); mobileMenuOpen = false">
            尝试 Jiallo Chat
          </div>

          <!-- 已登录：底部操作 -->
          <template v-if="userStore.isLoggedIn">
            <div class="mobile-menu__divider"></div>
            <div class="mobile-menu__item" @click="goToUserCenter(); mobileMenuOpen = false">
              <UserOutlined /> 用户中心
            </div>
            <div v-if="isAdmin" class="mobile-menu__item" @click="goToAdmin(); mobileMenuOpen = false">
              <SettingOutlined /> 后台管理
            </div>
            <div class="mobile-menu__item mobile-menu__item--danger" @click="handleLogoutMobile">
              <LogoutOutlined /> 退出登录
            </div>
          </template>
        </div>
      </a-drawer>
    </header>

    <main
      class="app-main"
      :class="{ 'app-main--home': isHomePage, 'app-main--standalone': isStandalone }"
    >
      <RouterView v-slot="{ Component, route: viewRoute }">
        <component v-if="shouldDisableRouteTransition" :is="Component" :key="viewRoute.name ?? viewRoute.path" />
        <Transition v-else :name="(viewRoute.meta.transition as string) || 'fade'" mode="out-in">
          <component :is="Component" :key="viewRoute.name ?? viewRoute.path" />
        </Transition>
      </RouterView>
    </main>

    <footer v-if="!isStandalone" class="app-footer">
      <span>The server is running <a href="https://github.com/baiyuanneko/jiallo-platform" target="_blank" rel="noopener noreferrer">Jiallo Platform</a> v0.0.1. <a href="https://github.com/baiyuanneko/jiallo-platform" target="_blank" rel="noopener noreferrer">Jiallo Platform</a> is an open source AI platform that can be freely deployed by everyone and this instance is one deployment of it.</span>
    </footer>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  flex-direction: column;
  position: relative;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}

.app--home {
  background-color: #fff;
}

.navbar {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
  z-index: 100;
}

.navbar--home {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background-color: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  border-bottom-color: rgba(240, 240, 240, 0.72);
}

.navbar__brand {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.navbar__logo {
  height: 40px;
  width: auto;
  object-fit: contain;
}

.navbar__title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2f3d;
  white-space: nowrap;
}

.navbar__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.navbar__links {
  border-bottom: none;
  background: transparent;
}

.navbar__links :deep(.ant-menu-overflow) {
  justify-content: flex-end;
}

.navbar__chat-btn {
  font-weight: 500;
  border-radius: 20px;
  padding: 0 18px;
  height: 34px;
  line-height: 34px;
}

.navbar__chat-btn .arrow-up-right-icon {
  margin-right: 4px;
  flex-shrink: 0;
}

.navbar__avatar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.navbar__mobile-toggle {
  display: none;
}

.navbar__username {
  font-size: 14px;
  color: #595959;
}

.user-dropdown {
  width: 220px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.12);
}

.user-card {
  padding: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.user-info {
  flex: 1;
}

.display-name {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
}

.username-text {
  font-size: 12px;
  color: #8c8c8c;
}

.user-menu {
  border-inline-end: none;
}

.user-menu :deep(.ant-menu-item) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mobile-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
}

.mobile-menu__user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px 16px;
}

.mobile-menu__user-info {
  flex: 1;
  min-width: 0;
}

.mobile-menu__display-name {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
  line-height: 1.4;
}

.mobile-menu__username {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.4;
}

.mobile-menu__divider {
  height: 1px;
  background-color: #f0f0f0;
  margin: 4px 0;
}

.mobile-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  font-size: 16px;
  color: #1f2f3d;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.mobile-menu__item:hover {
  background-color: #f5f5f5;
}

.mobile-menu__item--chat {
  color: var(--ant-primary-color, #1677ff);
  font-weight: 600;
}

.mobile-menu__item--danger {
  color: #ff4d4f;
}

.app-main {
  flex: 1;
  overflow-x: hidden;
}

.app-footer {
  padding: 14px 20px 18px;
  text-align: center;
  font-size: 12px;
  line-height: 1.6;
  color: #999;
}

.app-footer a {
  color: #888;
  text-decoration: none;
}

.app-footer a:hover {
  color: #666;
}

.app--standalone {
  padding: 0;
}

.app-main--standalone {
  padding: 0;
  height: 100vh;
  overflow: hidden;
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
    width: 100%;
    max-width: 100%;
    overflow: hidden;
  }

  .navbar__right {
    gap: 8px;
    max-width: 100%;
    overflow: hidden;
  }

  .navbar__right > * {
    display: none;
  }

  .navbar__right > .navbar__mobile-toggle {
    display: inline-flex;
  }

  .navbar__title {
    font-size: 15px;
  }

  .navbar__brand {
    max-width: calc(100% - 80px);
  }

  .navbar__logo {
    max-width: 36px;
    max-height: 36px;
    width: auto;
    height: auto;
  }
}
</style>
