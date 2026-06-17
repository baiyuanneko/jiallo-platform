<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import type { RegisterUserVo, EmailVerificationRequired } from '@/types/user'
import { isCaptchaError } from '@/types/user'
import EmailVerificationDialog from './EmailVerificationDialog.vue'
import { getRegisterCaptchaApi } from '@/api/apiUser'

const emit = defineEmits<{
  success: []
}>()

const userStore = useUserStore()
const formRef = ref()
const emailFormRef = ref()
const loading = ref(false)

const formData = reactive<RegisterUserVo>({
  username: '',
  password: '',
  email: '',
  verifyCodeId: '',
  verifyCode: '',
})

const captchaLoading = ref(false)
const captchaBase64 = ref('')

const step = ref<'credentials' | 'email'>('credentials')
const hasSubmittedOnce = ref(false)

const showEmailVerification = ref(false)
const emailVerificationData = ref<EmailVerificationRequired | null>(null)

const validatePassword = async (_rule: unknown, value: string) => {
  if (!value) {
    throw new Error('请输入密码')
  }
  if (value.length < 8) {
    throw new Error('密码长度至少为8位')
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  verifyCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

const submitButtonText = computed(() => {
  return step.value === 'credentials' ? '注册' : '发送验证码'
})

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const response = await getRegisterCaptchaApi()
    if (response.data.code === 0) {
      formData.verifyCodeId = response.data.data.captchaId
      captchaBase64.value = response.data.data.captchaBase64
      return
    }
    message.error(response.data.message || '获取验证码失败')
  } catch {
    message.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

async function handleSubmit() {
  try {
    if (step.value === 'credentials') {
      await formRef.value?.validate()
    } else {
      await emailFormRef.value?.validate()
    }
  } catch {
    return
  }

  await doRegister()
}

async function doRegister() {
  loading.value = true
  try {
    const result = await userStore.register(formData)
    if (result === true) {
      emit('success')
    } else if (isCaptchaError(result)) {
      if (hasSubmittedOnce.value) {
        message.error('注册已超时，请重新操作。')
        step.value = 'credentials'
        hasSubmittedOnce.value = false
        formData.email = ''
        formData.verifyCode = ''
        await loadCaptcha()
      } else {
        message.error('验证码错误或已过期')
        formData.verifyCode = ''
        await loadCaptcha()
      }
    } else if (result && typeof result === 'object') {
      if (result.message === 'EMAIL_VERIFICATION_REQUIRED' && !result.email) {
        hasSubmittedOnce.value = true
        step.value = 'email'
      } else {
        emailVerificationData.value = result
        showEmailVerification.value = true
      }
    } else {
      formData.verifyCode = ''
      await loadCaptcha()
    }
  } catch {
    formData.verifyCode = ''
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

function goBackToCredentials() {
  step.value = 'credentials'
  hasSubmittedOnce.value = false
}

function handleVerificationSuccess() {
  emit('success')
}

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <a-form
    v-show="step === 'credentials'"
    ref="formRef"
    :model="formData"
    :rules="rules"
    layout="vertical"
    class="auth-form"
  >
    <a-form-item label="用户名" name="username">
      <a-input
        v-model:value="formData.username"
        placeholder="请输入用户名"
        allow-clear
        size="large"
        @press-enter="handleSubmit"
      />
    </a-form-item>

    <a-form-item label="密码" name="password">
      <a-input-password
        v-model:value="formData.password"
        placeholder="请输入密码（至少8位）"
        allow-clear
        size="large"
        @press-enter="handleSubmit"
      />
    </a-form-item>

    <a-form-item label="邮箱" name="email">
      <a-input
        v-model:value="formData.email"
        placeholder="请输入邮箱（可选）"
        allow-clear
        size="large"
        @press-enter="handleSubmit"
      />
    </a-form-item>

    <a-form-item label="验证码" name="verifyCode">
      <div class="captcha-container">
        <a-input
          v-model:value="formData.verifyCode"
          placeholder="请输入验证码"
          allow-clear
          size="large"
          @press-enter="handleSubmit"
        />
        <button
          type="button"
          class="captcha-image"
          :class="{ loading: captchaLoading }"
          :disabled="captchaLoading"
          title="点击刷新验证码"
          @click="loadCaptcha"
        >
          <img v-if="captchaBase64 && !captchaLoading" :src="captchaBase64" alt="验证码" />
          <span v-else>加载中...</span>
        </button>
      </div>
    </a-form-item>

    <a-form-item class="auth-form__submit">
      <a-button type="primary" size="large" :loading="loading" block @click="handleSubmit">
        {{ submitButtonText }}
      </a-button>
    </a-form-item>
  </a-form>

  <a-form
    v-show="step === 'email'"
    ref="emailFormRef"
    :model="formData"
    :rules="emailRules"
    layout="vertical"
    class="auth-form"
  >
    <div class="email-step-hint">
      <p>系统要求绑定邮箱后才能完成注册</p>
      <p class="hint-sub">验证码将发送至您的邮箱</p>
    </div>

    <a-form-item label="邮箱" name="email">
      <a-input
        v-model:value="formData.email"
        placeholder="请输入邮箱地址"
        allow-clear
        size="large"
        @press-enter="handleSubmit"
      />
    </a-form-item>

    <a-form-item class="auth-form__submit">
      <a-space direction="vertical" size="middle" style="width: 100%">
        <a-button type="primary" size="large" :loading="loading" block @click="handleSubmit">
          {{ submitButtonText }}
        </a-button>
        <a-button size="large" block @click="goBackToCredentials">返回上一步</a-button>
      </a-space>
    </a-form-item>
  </a-form>

  <Teleport to="body">
    <EmailVerificationDialog
      v-if="emailVerificationData"
      v-model:visible="showEmailVerification"
      :user-id="emailVerificationData.userId"
      :email="emailVerificationData.email || ''"
      :login-info="formData"
      @success="handleVerificationSuccess"
    />
  </Teleport>
</template>

<style scoped>
.auth-form :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.captcha-container {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-image {
  flex-shrink: 0;
  width: 120px;
  height: 40px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background-color: #fafafa;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.captcha-image:hover {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.captcha-image.loading {
  cursor: not-allowed;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-image span {
  font-size: 12px;
  color: #8c8c8c;
}

.email-step-hint {
  margin-bottom: 24px;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.1), rgba(9, 109, 217, 0.04));
  color: #262626;
}

.hint-sub {
  margin-top: 6px;
  color: #595959;
  font-size: 13px;
}
</style>
