<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { AppstoreOutlined, ReadOutlined } from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const chatStore = useChatStore()

const apps = [
  {
    id: 'agentic-chat-ui',
    title: 'Agentic Chat UI',
  },
  {
    path: '/rag-library',
    title: 'RAG 知识库',
    requiresAuth: true,
  },
]

onMounted(() => {
  chatStore.loadRagModuleAuth()
})

function handleAppClick(app: { id?: string; path?: string }) {
  if (app.path) {
    router.push(app.path)
  } else if (app.id) {
    router.push(`/app/${app.id}`)
  }
}
</script>

<template>
  <a-card class="content-card">
    <div class="page-path">
      <span class="page-path-root">应用中心</span>
      <span class="page-path-separator"> / </span>
      <span class="page-path-current">应用列表</span>
    </div>
    <div class="card-header">
      <h3>仪表盘</h3>
    </div>

    <section class="dashboard-section">
      <h4 class="section-title">相关服务</h4>
      <div class="service-grid">
        <template v-for="app in apps" :key="app.id || app.path">
          <button
            v-if="!app.requiresAuth || chatStore.ragModuleAuthorized"
            type="button"
            class="service-entry"
            @click="handleAppClick(app)"
          >
          <ReadOutlined v-if="app.path" class="service-entry__icon" />
          <AppstoreOutlined v-else class="service-entry__icon" />
            <span class="service-entry__label">{{ app.title }}</span>
          </button>
        </template>
      </div>
    </section>
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
  margin-bottom: 8px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: #262626;
}

.dashboard-section {
  padding-top: 8px;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
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

@media (max-width: 768px) {
  .content-card {
    min-height: auto;
  }
}
</style>
