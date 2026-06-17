<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  verifyEmailCodeApi,
  changeLoginEmailApi,
  resendEmailCodeWithoutLoginApi,
} from '@/api/apiUser'
import { useUserStore } from '@/stores/user'
import { setAccessToken, setRefreshToken, setTokenExpiry } from '@/utils/auth'
import { ui } from '@/utils/ui'
import type { LoginUserVo } from '@/types/user'

const props = defineProps<{
  visible: boolean
  userId: string
  email: string
  loginInfo?: LoginUserVo
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const userStore = useUserStore()
const verifyCode = ref('')
const loading = ref(false)
const resendLoading = ref(false)
const showChangeEmail = ref(false)
const newEmail = ref('')
const changeEmailLoading = ref(false)
const currentEmail = ref('')
const countdown = ref(0)
let countdownTimer: number | null = null

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const canResend = computed(() => countdown.value === 0 && !resendLoading.value)

function startCountdown(seconds = 60) {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
    }
  }, 1000)
}

watch(
  () => props.email,
  (email) => {
    if (email) {
      currentEmail.value = email
    }
  },
  { immediate: true },
)

watch(
  () => props.visible,
  (val) => {
    if (val) {
      verifyCode.value = ''
      showChangeEmail.value = false
      newEmail.value = ''
      currentEmail.value = props.email
      startCountdown()
    } else if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  },
)

async function handleVerify() {
  if (!verifyCode.value || verifyCode.value.length !== 6) {
    ui.warning('请输入6位验证码')
    return
  }

  loading.value = true
  try {
    const res = await verifyEmailCodeApi({
      userId: props.userId,
      code: verifyCode.value,
    })

    if (res.data.code === 0) {
      const { accessToken, refreshToken, expiresIn } = res.data.data
      userStore.accessToken = accessToken
      userStore.refreshToken = refreshToken
      userStore.tokenExpiresAt = Date.now() + expiresIn * 1000
      setAccessToken(accessToken)
      setRefreshToken(refreshToken)
      setTokenExpiry(userStore.tokenExpiresAt)
      await userStore.fetchUserInfo()
      ui.success('邮箱验证成功')
      dialogVisible.value = false
      emit('success')
    }
  } catch (error) {
    console.error('验证失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleResend() {
  if (!canResend.value) return
  if (!props.loginInfo) {
    ui.error('缺少登录信息，无法重发验证码')
    return
  }

  resendLoading.value = true
  try {
    const res = await resendEmailCodeWithoutLoginApi(props.loginInfo)
    if (res.data.code === 0) {
      ui.success('验证码已发送')
      startCountdown()
    }
  } catch (error) {
    console.error('发送失败:', error)
  } finally {
    resendLoading.value = false
  }
}

async function handleChangeEmail() {
  if (!newEmail.value) {
    ui.warning('请输入新邮箱地址')
    return
  }
  if (!props.loginInfo) {
    ui.error('缺少登录信息，无法修改邮箱')
    return
  }

  changeEmailLoading.value = true
  try {
    const res = await changeLoginEmailApi({
      loginInfo: props.loginInfo,
      newEmail: newEmail.value,
    })

    if (res.data.code === 0) {
      ui.success('新验证码已发送到 ' + newEmail.value)
      currentEmail.value = newEmail.value
      showChangeEmail.value = false
      startCountdown()
    }
  } catch (error) {
    console.error('修改邮箱失败:', error)
  } finally {
    changeEmailLoading.value = false
  }
}

function handleClose() {
  dialogVisible.value = false
}
</script>

<template>
  <a-modal
    v-model:open="dialogVisible"
    title="邮箱验证"
    :width="400"
    :footer="null"
    :mask-closable="false"
    @cancel="handleClose"
  >
    <div class="verification-content">
      <div class="info-text">
        <p>验证码已发送至邮箱：</p>
        <p class="email-address">{{ currentEmail }}</p>
        <p class="hint">请查收邮件并输入6位验证码</p>
      </div>

      <div v-if="!showChangeEmail" class="code-input-section">
        <a-input
          v-model:value="verifyCode"
          placeholder="请输入6位验证码"
          :maxlength="6"
          size="large"
          class="code-input"
          @press-enter="handleVerify"
        />

        <div class="action-buttons">
          <a-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!verifyCode || verifyCode.length !== 6"
            block
            @click="handleVerify"
          >
            验证
          </a-button>
        </div>

        <div class="resend-section">
          <a-button
            type="link"
            :disabled="!canResend"
            :loading="resendLoading"
            @click="handleResend"
          >
            {{ countdown > 0 ? `重新发送 (${countdown}s)` : '重新发送验证码' }}
          </a-button>
          <span class="divider">|</span>
          <a-button type="link" @click="showChangeEmail = true">修改邮箱</a-button>
        </div>
      </div>

      <div v-else class="change-email-section">
        <a-input
          v-model:value="newEmail"
          placeholder="请输入新邮箱地址"
          size="large"
          type="email"
          @press-enter="handleChangeEmail"
        />

        <div class="action-buttons">
          <a-button size="large" @click="showChangeEmail = false">取消</a-button>
          <a-button
            type="primary"
            size="large"
            :loading="changeEmailLoading"
            :disabled="!newEmail"
            @click="handleChangeEmail"
          >
            发送验证码
          </a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.verification-content {
  text-align: center;
}

.info-text {
  margin-bottom: 24px;
}

.info-text p {
  margin: 8px 0;
  color: #595959;
}

.email-address {
  font-size: 16px;
  font-weight: 600;
  color: #262626 !important;
}

.hint {
  font-size: 13px;
  color: #8c8c8c !important;
}

.code-input-section,
.change-email-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.code-input :deep(input) {
  text-align: center;
  font-size: 20px;
  letter-spacing: 8px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-buttons > * {
  flex: 1;
}

.resend-section {
  display: flex;
  align-items: center;
  justify-content: center;
}

.divider {
  color: #bfbfbf;
}
</style>
