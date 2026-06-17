<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { forgetPasswordApi, resetPasswordApi } from '@/api/apiUser'
import { useBackgroundCarousel } from '@/composables/useBackgroundCarousel'
import { ui } from '@/utils/ui'

const router = useRouter()
const { currentBg, imageLoaded } = useBackgroundCarousel()

const currentStep = ref(1)
const step1FormRef = ref()
const step2FormRef = ref()

const step1Form = ref({
  username: '',
})

const step2Form = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const userId = ref('')
const maskedEmail = ref('')
const resendCountdown = ref(0)
let resendTimer: number | null = null

const step1Rules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
}

const validateConfirmPassword = async (_rule: unknown, value: string) => {
  if (value === '') {
    throw new Error('请再次输入密码')
  }
  if (value !== step2Form.value.newPassword) {
    throw new Error('两次输入密码不一致')
  }
}

const step2Rules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const canResend = computed(() => resendCountdown.value === 0)

function startResendCountdown() {
  resendCountdown.value = 60
  if (resendTimer) {
    clearInterval(resendTimer)
  }
  resendTimer = window.setInterval(() => {
    resendCountdown.value--
    if (resendCountdown.value <= 0) {
      clearInterval(resendTimer!)
      resendTimer = null
    }
  }, 1000)
}

async function handleStep1Submit() {
  try {
    await step1FormRef.value?.validate()
  } catch {
    return
  }

  try {
    const res = await forgetPasswordApi({
      username: step1Form.value.username,
    })

    if (res.data.code === 0) {
      userId.value = res.data.data.userId
      maskedEmail.value = res.data.data.email
      currentStep.value = 2
      startResendCountdown()
      ui.success(`验证码已发送到 ${res.data.data.email}`)
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

async function handleResendCode() {
  if (!canResend.value) return

  try {
    const res = await forgetPasswordApi({
      username: step1Form.value.username,
    })

    if (res.data.code === 0) {
      startResendCountdown()
      ui.success('验证码已重新发送')
    }
  } catch (error) {
    console.error('重新发送验证码失败:', error)
  }
}

async function handleStep2Submit() {
  try {
    await step2FormRef.value?.validate()
  } catch {
    return
  }

  try {
    const res = await resetPasswordApi({
      userId: userId.value,
      code: step2Form.value.code,
      newPassword: step2Form.value.newPassword,
    })

    if (res.data.code === 0) {
      ui.success('密码重置成功，请使用新密码登录')
      router.push('/login')
    }
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

function goToLogin() {
  router.push('/login')
}

function goBackToStep1() {
  currentStep.value = 1
  step2Form.value = {
    code: '',
    newPassword: '',
    confirmPassword: '',
  }
  if (resendTimer) {
    clearInterval(resendTimer)
    resendTimer = null
  }
  resendCountdown.value = 0
}
</script>

<template>
  <div class="forgot-password-container">
    <div class="forgot-password-wrapper fade-in-up-delay" :class="{ visible: imageLoaded }">
      <div
        class="forgot-password-card"
        :class="{ 'forgot-password-card--ready': imageLoaded }"
        :style="{ '--forgot-card-bg': `url(${currentBg})` }"
      >
        <div class="forgot-password-card__main">
          <section class="forgot-password-panel forgot-password-panel--form">
            <h2 class="forgot-password-title">忘记密码</h2>

            <a-form
              v-if="currentStep === 1"
              ref="step1FormRef"
              :model="step1Form"
              :rules="step1Rules"
              layout="vertical"
              class="forgot-form"
            >
              <a-form-item name="username">
                <a-input
                  v-model:value="step1Form.username"
                  placeholder="请输入用户名或邮箱"
                  size="large"
                  allow-clear
                  @press-enter="handleStep1Submit"
                />
              </a-form-item>

              <a-form-item class="forgot-form__submit">
                <a-button type="primary" size="large" block @click="handleStep1Submit">
                  发送验证码
                </a-button>
              </a-form-item>
            </a-form>

            <a-form
              v-else
              ref="step2FormRef"
              :model="step2Form"
              :rules="step2Rules"
              layout="vertical"
              class="forgot-form"
            >
              <div class="email-hint">
                验证码已发送到 <strong>{{ maskedEmail }}</strong>
              </div>

              <a-form-item name="code">
                <div class="verify-row">
                  <a-input
                    v-model:value="step2Form.code"
                    placeholder="请输入6位验证码"
                    size="large"
                    :maxlength="6"
                    allow-clear
                    @press-enter="handleStep2Submit"
                  />
                  <a-button size="large" :disabled="!canResend" @click="handleResendCode">
                    {{ canResend ? '重新发送' : `${resendCountdown}s` }}
                  </a-button>
                </div>
              </a-form-item>

              <a-form-item name="newPassword">
                <a-input-password
                  v-model:value="step2Form.newPassword"
                  placeholder="请输入新密码（至少8位）"
                  size="large"
                  allow-clear
                  @press-enter="handleStep2Submit"
                />
              </a-form-item>

              <a-form-item name="confirmPassword">
                <a-input-password
                  v-model:value="step2Form.confirmPassword"
                  placeholder="请再次输入新密码"
                  size="large"
                  allow-clear
                  @press-enter="handleStep2Submit"
                />
              </a-form-item>

              <a-form-item class="forgot-form__submit">
                <a-space direction="vertical" size="middle" style="width: 100%">
                  <a-button type="primary" size="large" block @click="handleStep2Submit">
                    重置密码
                  </a-button>
                  <a-button size="large" block @click="goBackToStep1">返回上一步</a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </section>

          <div class="forgot-password-divider" />

          <aside class="forgot-password-panel forgot-password-panel--aside">
            <div class="forgot-password-footer">
              <div class="footer-row">
                <span>想起密码了？</span>
                <a-button type="link" class="footer-link" @click="goToLogin">返回登录</a-button>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 64px);
  padding: 32px 20px;
  background: #fff;
}

.forgot-password-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.forgot-password-card {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border-radius: 28px;
  padding: 64px 72px;
  width: 100%;
  max-width: 960px;
  background-color: rgba(255, 255, 255, 0.64);
  border: 1px solid rgba(0, 0, 0, 0.2);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.forgot-password-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--forgot-card-bg);
  background-size: cover;
  background-position: center;
  opacity: 0.4;
  transform: scale(1.04);
  transition: opacity 0.4s ease;
}

.forgot-password-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(255, 255, 255, 0.54));
}

.forgot-password-card > * {
  position: relative;
  z-index: 1;
}

.forgot-password-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1.52fr) 1px minmax(260px, 0.82fr);
  align-items: stretch;
  gap: 40px;
}

.forgot-password-panel--form {
  min-width: 0;
}

.forgot-password-panel--aside {
  display: flex;
  align-items: center;
}

.forgot-password-divider {
  width: 1px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.02) 0%,
    rgba(0, 0, 0, 0.12) 20%,
    rgba(0, 0, 0, 0.12) 80%,
    rgba(0, 0, 0, 0.02) 100%
  );
}

.forgot-password-card--ready::before {
  opacity: 0.42;
}

.forgot-password-title {
  text-align: left;
  margin: 0 0 30px 0;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.forgot-form :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.email-hint {
  margin-bottom: 20px;
  padding: 12px;
  background-color: rgba(22, 119, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  text-align: left;
}

.email-hint strong {
  color: #1677ff;
}

.verify-row {
  display: flex;
  gap: 10px;
}

.verify-row > :first-child {
  flex: 1;
}

.forgot-password-footer {
  width: 100%;
  font-size: 14px;
  color: #666;
}

.footer-row {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
}

.footer-link {
  padding-inline: 4px;
}

@media (max-width: 768px) {
  .forgot-password-card {
    padding: 34px 24px;
    max-width: 100%;
    border-radius: 22px;
  }

  .forgot-password-card__main {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .forgot-password-divider {
    width: 100%;
    height: 1px;
  }

  .forgot-password-panel--aside {
    align-items: flex-start;
  }

  .forgot-password-title {
    font-size: 24px;
  }

  .verify-row {
    flex-direction: column;
  }
}
</style>
