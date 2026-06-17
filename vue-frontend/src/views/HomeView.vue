<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function goToLoginOrRegister() {
  router.push('/login')
}

function goToUserCenter() {
  router.push('/user/center')
}

function goToAppList() {
  router.push('/app')
}

function goToChat() {
  router.push('/app/agentic-chat-ui')
}
</script>

<template>
  <section class="page home-page">
    <div class="home-container">
      <div class="home-content">
        <div class="text-section">
          <h1 class="main-title">关于 Jiallo Platform</h1>
          <p class="description">
            Jiallo Platform 是基于 Spring AI Alibaba、Antd Design 等流行技术开发的统一 AI 平台。目前提供了 Agentic Chat UI 和 RAG 功能。同时适合个人部署和作为 SaaS 使用。
          </p>
          <div class="action-group">
            <a-button
              v-if="userStore.isLoggedIn"
              type="primary"
              size="large"
              class="action-button action-button--chat"
              @click="goToChat"
            >
              <svg
                width="18"
                height="18"
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
            <a-button
              v-else
              type="primary"
              size="large"
              class="action-button"
              @click="goToLoginOrRegister"
            >
              登录/注册
            </a-button>
            <a-button
              v-if="userStore.isLoggedIn"
              size="large"
              class="action-button action-button--secondary"
              @click="goToAppList"
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                class="action-icon"
              >
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
              </svg>
              进入应用列表
            </a-button>
            <a-button
              v-if="userStore.isLoggedIn"
              size="large"
              class="action-button action-button--secondary"
              @click="goToUserCenter"
            >
              打开用户中心
            </a-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #1677ff;
}

.page::after {
  content: '';
  position: absolute;
  inset: 0;
  background: none;
  pointer-events: none;
  z-index: 0;
}

.home-page {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 104px 20px 40px;
}

.home-container {
  max-width: 1200px;
  width: 100%;
}

.home-content {
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 28px;
  padding: 60px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.18);
}

.text-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.main-title {
  font-size: 36px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0;
  line-height: 1.3;
}

.description {
  font-size: 16px;
  line-height: 1.8;
  color: #606266;
  margin: 0;
}

.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 32px;
  font-size: 16px;
  line-height: 1;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.action-button--secondary {
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(31, 47, 61, 0.14);
  color: #1f2f3d;
}

.action-button--chat {
  font-weight: 600;
  padding: 0 36px;
}

.arrow-up-right-icon {
  margin-right: 6px;
  flex-shrink: 0;
}

.action-icon {
  margin-right: 6px;
  flex-shrink: 0;
}

.logo-section {
  display: none;
}

.project-logo {
  display: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home-content {
    padding: 40px 30px;
  }

  .main-title {
    font-size: 28px;
  }

  .description {
    font-size: 14px;
  }

  .action-group {
    width: 100%;
  }
}
</style>
