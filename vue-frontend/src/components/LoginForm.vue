<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import type { LoginUserVo, EmailVerificationRequired } from '@/types/user'
import { isCaptchaError } from '@/types/user'
import EmailVerificationDialog from './EmailVerificationDialog.vue'
import { getLoginCaptchaApi } from '@/api/apiUser'

const emit = defineEmits<{
  success: []
}>()

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const formData = reactive<LoginUserVo>({
  username: '',
  password: '',
  verifyCodeId: '',
  verifyCode: '',
})

const captchaLoading = ref(false)
const captchaBase64 = ref('')

const showEmailVerification = ref(false)
const emailVerificationData = ref<EmailVerificationRequired | null>(null)

const rules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  verifyCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const response = await getLoginCaptchaApi()
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
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const result = await userStore.login(formData)
    if (result === true) {
      emit('success')
    } else if (isCaptchaError(result)) {
      message.error('验证码错误或已过期')
      formData.verifyCode = ''
      await loadCaptcha()
    } else if (result && typeof result === 'object') {
      emailVerificationData.value = result
      showEmailVerification.value = true
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

function handleVerificationSuccess() {
  emit('success')
}

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical" class="auth-form">
    <a-form-item label="用户名" name="username">
      <a-input
        v-model:value="formData.username"
        placeholder="请输入用户名或邮箱"
        allow-clear
        size="large"
        @press-enter="handleSubmit"
      />
    </a-form-item>

    <a-form-item label="密码" name="password">
      <a-input-password
        v-model:value="formData.password"
        placeholder="请输入密码"
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
        登录
      </a-button>
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
</style>
