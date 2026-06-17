<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { externalLoginApi } from '@/api/apiUser'
import { useUserStore } from '@/stores/user'
import { ui } from '@/utils/ui'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const statusText = ref('正在校验外部登录凭证...')

async function handleExternalLogin() {
  const token = route.query.token

  if (typeof token !== 'string' || !token.trim()) {
    loading.value = false
    statusText.value = '缺少外部登录凭证'
    ui.error('缺少外部登录凭证')
    router.replace('/login')
    return
  }

  try {
    const res = await externalLoginApi({ ssoToken: token })

    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '外部登录失败')
    }

    await userStore.applyTokenResponse(res.data.data)
    ui.success('登录成功')
    router.replace('/')
  } catch (error) {
    console.error('外部登录失败:', error)
    loading.value = false
    statusText.value = '外部登录失败，正在返回登录页...'
    router.replace('/login')
  }
}

onMounted(() => {
  handleExternalLogin()
})
</script>

<template>
  <div class="external-login-view">
    <a-card class="external-login-card" :bordered="false">
      <a-spin :spinning="loading" size="large">
        <div class="external-login-content">
          <h2 class="external-login-title">正在登录</h2>
          <p class="external-login-text">{{ statusText }}</p>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.external-login-view {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f5f7fa;
}

.external-login-card {
  width: 100%;
  max-width: 480px;
  border-radius: 24px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
}

.external-login-content {
  padding: 32px 16px;
  text-align: center;
}

.external-login-title {
  margin: 0 0 12px;
  font-size: 28px;
  color: #1f2937;
}

.external-login-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #6b7280;
}
</style>
