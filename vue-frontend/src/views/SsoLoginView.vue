<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  LoadingOutlined,
  CloseCircleOutlined,
  SafetyCertificateOutlined,
  CheckCircleFilled,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'
import { useBackgroundCarousel } from '@/composables/useBackgroundCarousel'
import { useUserStore } from '@/stores/user'
import { getSsoClientInfoApi, confirmSsoLoginApi, getSsoClientIconUrl } from '@/api/apiSso'
import { ui } from '@/utils/ui'
import { PERMISSION_DESCRIPTIONS } from '@/types/sso'
import type { SsoClient } from '@/types/sso'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { currentBg, imageLoaded } = useBackgroundCarousel()

const loading = ref(true)
const confirming = ref(false)
const error = ref<string | null>(null)
const clientInfo = ref<SsoClient | null>(null)
const clientUniqueName = computed(() => route.query.client as string)
const iconError = ref(false)

const permissionList = computed(() => {
  if (!clientInfo.value) return []
  return PERMISSION_DESCRIPTIONS[clientInfo.value.clientPermissionType] || []
})

const clientIconUrl = computed(() => {
  if (!clientInfo.value) return ''
  return getSsoClientIconUrl(clientInfo.value.clientUniqueName)
})

function handleIconError() {
  iconError.value = true
}

async function fetchClientInfo() {
  if (!clientUniqueName.value) {
    error.value = '缺少客户端参数'
    loading.value = false
    return
  }

  try {
    const res = await getSsoClientInfoApi({ clientUniqueName: clientUniqueName.value })
    if (res.data.code === 0) {
      clientInfo.value = res.data.data
      if (clientInfo.value.silentRedirect) {
        await handleConfirm()
      }
    } else {
      error.value = res.data.message || '获取客户端信息失败'
    }
  } catch (err) {
    error.value = '获取客户端信息失败，请稍后重试'
    console.error('获取客户端信息失败:', err)
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  if (!clientInfo.value) return

  confirming.value = true
  try {
    const res = await confirmSsoLoginApi({ clientUniqueName: clientUniqueName.value })
    if (res.data.code === 0) {
      const token = res.data.data
      const redirectUri = clientInfo.value.clientRedirectUri
      const separator = redirectUri.includes('?') ? '&' : '?'
      window.location.href = `${redirectUri}${separator}token=${encodeURIComponent(token)}`
    } else {
      ui.error(res.data.message || '授权失败')
      confirming.value = false
    }
  } catch (err) {
    ui.error('授权失败，请稍后重试')
    console.error('授权失败:', err)
    confirming.value = false
  }
}

function handleCancel() {
  router.push('/')
}

async function handleSwitchAccount() {
  await userStore.logout()
  router.push({
    path: '/login',
    query: clientUniqueName.value ? { redirect_sso: clientUniqueName.value } : {},
  })
}

function goHome() {
  router.push('/')
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.replace({
      path: '/login',
      query: { redirect_sso: clientUniqueName.value },
    })
    return
  }

  await fetchClientInfo()
})
</script>

<template>
  <div class="sso-page">
    <div class="sso-banner">
      <InfoCircleOutlined class="sso-banner__icon" />
      <span>
        您正在试图使用 Bynidentity 统一通行证的信息登录到其他外部应用，请确保你信任对应应用，并仔细确认，防止隐私泄露。
      </span>
    </div>

    <div class="sso-container">
      <div class="sso-wrapper fade-in-up-delay" :class="{ visible: imageLoaded }">
      <div
        class="sso-card"
        :class="{ 'sso-card--ready': imageLoaded }"
        :style="{ '--sso-card-bg': `url(${currentBg})` }"
      >
        <template v-if="loading">
          <div class="sso-loading">
            <a-spin size="large">
              <template #indicator>
                <LoadingOutlined spin style="font-size: 32px" />
              </template>
            </a-spin>
            <p>正在加载...</p>
          </div>
        </template>

        <template v-else-if="error">
          <div class="sso-error">
            <CloseCircleOutlined class="error-icon" />
            <h3>出错了</h3>
            <p>{{ error }}</p>
            <a-button type="primary" @click="goHome">返回首页</a-button>
          </div>
        </template>

        <template v-else-if="clientInfo && !clientInfo.silentRedirect">
          <div class="sso-card__main">
            <section class="sso-panel sso-panel--content">
              <h2 class="sso-title">授权确认</h2>
              <p class="sso-question">
                你要使用账户
                <strong>{{ userStore.userInfo?.displayName || userStore.userInfo?.username }}</strong>
                登录
                <strong>{{ clientInfo.clientName }}</strong>
                吗？
              </p>

              <div class="permission-section">
                <p class="permission-title">该应用将获取你的以下信息</p>
                <ul class="permission-list">
                  <li v-for="(perm, index) in permissionList" :key="index" class="permission-item">
                    <CheckCircleFilled class="permission-item__icon" />
                    <span class="permission-item__text">{{ perm }}</span>
                  </li>
                </ul>
              </div>

              <div class="sso-actions">
                <a-button :disabled="confirming" @click="handleCancel">取消</a-button>
                <a-button type="primary" :loading="confirming" @click="handleConfirm">
                  {{ confirming ? '授权中...' : '授权登录' }}
                </a-button>
              </div>
            </section>

            <div class="sso-divider" />

            <aside class="sso-panel sso-panel--aside">
              <div class="sso-side">
                <div v-if="userStore.userInfo" class="current-user">
                  <span class="current-user__label">当前账户：</span>
                  <div class="current-user__main">
                    <strong class="current-user__value">
                      {{ userStore.userInfo.displayName || userStore.userInfo.username }}
                    </strong>
                    <a-button type="link" class="current-user__switch" @click="handleSwitchAccount">
                      切换账户
                    </a-button>
                  </div>
                </div>

                <div class="client-info-title">应用信息</div>
                <div class="client-info">
                  <div class="client-header">
                    <img
                      v-if="!iconError"
                      :src="clientIconUrl"
                      :alt="clientInfo.clientName"
                      class="client-icon"
                      @error="handleIconError"
                    />
                    <div class="client-icon-placeholder" v-else>
                      <SafetyCertificateOutlined />
                    </div>
                    <div class="client-text">
                      <h3 class="client-name">{{ clientInfo.clientName }}</h3>
                      <p v-if="clientInfo.clientAuthorName" class="client-author">
                        {{ clientInfo.clientAuthorName }}
                      </p>
                    </div>
                  </div>

                  <details
                    v-if="clientInfo.clientDesc || clientInfo.clientWebsite"
                    class="client-details"
                  >
                    <summary class="client-details__summary">显示详细信息</summary>
                    <div class="client-details__content">
                      <div v-if="clientInfo.clientDesc" class="client-detail-row">
                        <span class="client-detail-row__label">描述</span>
                        <span class="client-detail-row__value">{{ clientInfo.clientDesc }}</span>
                      </div>
                      <div v-if="clientInfo.clientWebsite" class="client-detail-row">
                        <span class="client-detail-row__label">官网</span>
                        <span class="client-detail-row__value client-detail-row__value--link">
                          <a :href="clientInfo.clientWebsite" target="_blank" rel="noopener noreferrer">
                            {{ clientInfo.clientWebsite }}
                          </a>
                        </span>
                      </div>
                    </div>
                  </details>
                </div>

                <p class="client-security-tip">
                  请注意安全风险。你的信息将被发送给此开发者。
                </p>
              </div>
            </aside>
          </div>
        </template>

        <template v-else-if="clientInfo && clientInfo.silentRedirect">
          <div class="sso-loading">
            <a-spin size="large">
              <template #indicator>
                <LoadingOutlined spin style="font-size: 32px" />
              </template>
            </a-spin>
            <p>正在跳转...</p>
          </div>
        </template>
      </div>
    </div>
    </div>
  </div>
</template>

<style scoped>
.sso-page {
  min-height: calc(100vh - 64px);
}

.sso-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 24px;
  background: #eaf4ff;
  border-bottom: 1px solid rgba(22, 119, 255, 0.14);
  color: #1554ad;
  font-size: 14px;
  line-height: 1.7;
}

.sso-banner__icon {
  flex: none;
  font-size: 16px;
}

.sso-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 64px - 50px);
  padding: 32px 20px;
  background: #fff;
}

.sso-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 960px;
}

.sso-card {
  position: relative;
  z-index: 1;
  overflow: hidden;
  background-color: rgba(255, 255, 255, 0.64);
  border-radius: 28px;
  padding: 56px 64px;
  width: 100%;
  max-width: 960px;
  min-width: 320px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.sso-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--sso-card-bg);
  background-size: cover;
  background-position: center;
  opacity: 0.4;
  transform: scale(1.04);
  transition: opacity 0.4s ease;
}

.sso-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(255, 255, 255, 0.54));
}

.sso-card > * {
  position: relative;
  z-index: 1;
}

.sso-card--ready::before {
  opacity: 0.42;
}

.sso-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1.52fr) 1px minmax(220px, 0.82fr);
  align-items: stretch;
  gap: 40px;
}

.sso-panel--content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sso-panel--aside {
  display: flex;
  align-items: flex-start;
}

.sso-side {
  width: 100%;
}

.sso-divider {
  width: 1px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.02) 0%,
    rgba(0, 0, 0, 0.12) 20%,
    rgba(0, 0, 0, 0.12) 80%,
    rgba(0, 0, 0, 0.02) 100%
  );
}

.sso-title {
  text-align: left;
  margin: 0 0 18px 0;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.sso-question {
  margin: 0 0 28px;
  font-size: 16px;
  line-height: 1.6;
  color: #262626;
}

.sso-question strong {
  color: #262626;
  font-weight: 600;
}

.sso-loading,
.sso-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  text-align: center;
}

.sso-loading p,
.sso-error p {
  margin-top: 16px;
  color: #595959;
}

.sso-error h3 {
  margin: 16px 0 8px 0;
  color: #262626;
}

.error-icon {
  font-size: 48px;
  color: #ff4d4f;
}

.client-info {
  margin-top: 20px;
  padding: 18px;
  background-color: rgba(245, 247, 250, 0.88);
  border-radius: 12px;
  border: 1px solid rgba(22, 119, 255, 0.28);
}

.client-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.client-icon,
.client-icon-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  object-fit: cover;
  background-color: #f0f5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #1677ff;
}

.client-name {
  margin: 0;
  font-size: 18px;
  color: #262626;
}

.client-author,
.client-desc,
.client-website {
  margin: 6px 0 0;
  color: #595959;
}

.client-details {
  margin-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding-top: 10px;
}

.client-details__summary {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding-right: 16px;
  font-size: 13px;
  line-height: 1.5;
  color: #8c8c8c;
  cursor: pointer;
  list-style: none;
  user-select: none;
}

.client-details__summary::-webkit-details-marker {
  display: none;
}

.client-details__summary::after {
  content: '';
  width: 7px;
  height: 7px;
  border-right: 1.5px solid currentColor;
  border-bottom: 1.5px solid currentColor;
  transform: rotate(45deg) translateY(-1px);
  transform-origin: center;
  transition: transform 0.2s ease;
}

.client-details[open] .client-details__summary::after {
  transform: rotate(225deg) translateY(-1px);
}

.client-details__content {
  padding-top: 8px;
}

.client-detail-row {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  line-height: 1.6;
}

.client-detail-row__label {
  color: #8c8c8c;
}

.client-detail-row__value {
  min-width: 0;
  color: #595959;
  word-break: break-word;
}

.client-website a {
  color: #595959;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.permission-section {
  margin-bottom: 20px;
}

.permission-title {
  margin-bottom: 14px;
  font-weight: 600;
  color: #262626;
}

.permission-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 6px;
}

.permission-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-radius: 0;
  background: transparent;
  border: 1px solid rgba(22, 119, 255, 0.1);
  border-width: 0 0 1px;
}

.permission-item__icon {
  flex: none;
  margin-top: 3px;
  font-size: 15px;
  color: #1677ff;
}

.permission-item__text {
  color: #434343;
  line-height: 1.7;
}

.permission-item:last-child {
  border-bottom: none;
}

.current-user {
  width: 100%;
  color: #262626;
}

.current-user__main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-user__label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.current-user__switch {
  padding: 0;
  height: auto;
  font-size: 13px;
  line-height: 1;
}

.current-user__value {
  font-size: 20px;
  line-height: 1.4;
}

.client-info-title {
  display: block;
  margin: 18px 0 8px;
  font-size: 14px;
  color: #666;
}

.client-security-tip {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #8c8c8c;
}

.sso-actions {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
}

@media (max-width: 768px) {
  .sso-banner {
    padding: 10px 16px;
    align-items: flex-start;
  }

  .sso-card {
    min-width: 0;
    width: calc(100% - 32px);
    padding: 34px 24px;
    border-radius: 22px;
  }

  .client-header {
    align-items: flex-start;
  }

  .sso-card__main {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .sso-divider {
    width: 100%;
    height: 1px;
  }

  .sso-panel--aside {
    align-items: flex-start;
  }

  .sso-question {
    font-size: 16px;
  }

  .sso-actions {
    flex-direction: column-reverse;
  }
}
</style>
