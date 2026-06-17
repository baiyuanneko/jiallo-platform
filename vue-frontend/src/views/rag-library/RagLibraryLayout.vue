<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  ReadOutlined,
  SearchOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const isSidebarCollapsed = ref(false)

const activeMenu = computed(() => {
  if (route.path === '/rag-library/search') {
    return ['search']
  }
  if (route.path.startsWith('/rag-library')) {
    return ['libraryList']
  }
  return []
})

function handleMenuClick({ key }: { key: string }) {
  if (key === 'libraryList') {
    router.push('/rag-library')
  } else if (key === 'search') {
    router.push('/rag-library/search')
  }
}

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}
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
          <span v-if="!isSidebarCollapsed" class="sidebar-title">RAG 知识库</span>
        </div>
        <a-menu
          mode="inline"
          :inline-collapsed="isSidebarCollapsed"
          :selected-keys="activeMenu"
          class="sidebar-menu"
          @click="handleMenuClick"
        >
          <a-menu-item key="libraryList">
            <ReadOutlined />
            <span>知识库列表</span>
          </a-menu-item>
          <a-menu-item key="search">
            <SearchOutlined />
            <span>检索测试</span>
          </a-menu-item>
        </a-menu>
      </aside>

      <main class="content">
        <RouterView />
      </main>
    </div>
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

.content > :deep(*) {
  flex: 1;
}

@media (max-width: 768px) {
  .user-center {
    padding-right: 0;
  }

  .user-center-container {
    flex-direction: column;
    min-height: auto;
  }

  .sidebar,
  .sidebar--collapsed {
    width: 100%;
    flex-basis: auto;
  }
}
</style>
