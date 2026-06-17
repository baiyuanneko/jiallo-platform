<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  UserOutlined,
  ApiOutlined,
  SettingOutlined,
  CloudServerOutlined,
  SafetyCertificateOutlined,
  RobotOutlined,
  TeamOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => [route.path])
const isSidebarCollapsed = ref(false)

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}
</script>

<template>
  <div class="admin-layout user-center">
    <div class="admin-container user-center-container">
      <aside class="admin-sidebar sidebar" :class="{ 'sidebar--collapsed': isSidebarCollapsed }">
        <div class="sidebar-header">
          <button type="button" class="sidebar-toggle" @click="toggleSidebar">
            <MenuUnfoldOutlined v-if="isSidebarCollapsed" />
            <MenuFoldOutlined v-else />
          </button>
          <span v-if="!isSidebarCollapsed" class="sidebar-title">后台管理</span>
        </div>
        <a-menu
          mode="inline"
          :inline-collapsed="isSidebarCollapsed"
          :selected-keys="activeMenu"
          class="admin-menu sidebar-menu"
          @click="handleMenuClick"
        >
          <a-menu-item key="/admin/users">
            <UserOutlined />
            <span>用户管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/sso-clients">
            <ApiOutlined />
            <span>SSO Client 管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/system-config">
            <SettingOutlined />
            <span>系统配置</span>
          </a-menu-item>
          <a-menu-item key="/admin/llm-providers">
            <CloudServerOutlined />
            <span>系统模型供应商管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/model-authorizations">
            <SafetyCertificateOutlined />
            <span>模型与授权</span>
          </a-menu-item>
          <a-menu-item key="/admin/agent-type-authorizations">
            <RobotOutlined />
            <span>智能体授权</span>
          </a-menu-item>
          <a-menu-item key="/admin/feature-module-authorizations">
            <SafetyCertificateOutlined />
            <span>特定功能模块授权</span>
          </a-menu-item>
          <a-menu-item key="/admin/user-groups">
            <TeamOutlined />
            <span>用户组管理</span>
          </a-menu-item>
        </a-menu>
      </aside>

      <main class="admin-content content">
        <div class="content-panel">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0 20px 20px 0;
}

.admin-container {
  display: flex;
  gap: 0;
  width: 100%;
  min-height: calc(100vh - 64px);
}

.admin-sidebar {
  width: 220px;
  flex: 0 0 220px;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  background: #fff;
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

.admin-menu {
  border-inline-end: none;
  flex: 1;
}

.admin-content {
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

.content-panel > :deep(*) {
  flex: 1;
}

@media (max-width: 640px) {
  .admin-container {
    flex-direction: column;
  }

  .admin-sidebar {
    width: 100%;
  }

  .admin-menu {
    display: flex;
    overflow-x: auto;
  }

  .admin-menu :deep(.ant-menu-item) {
    flex: 1;
    justify-content: center;
    min-width: 120px;
  }
}
</style>
