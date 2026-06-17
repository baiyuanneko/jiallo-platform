<script setup lang="ts">
import { useRouter } from 'vue-router'
import LoginForm from '@/components/LoginForm.vue'
import { useBackgroundCarousel } from '@/composables/useBackgroundCarousel'

const router = useRouter()

const { currentBg, imageLoaded } = useBackgroundCarousel()

function handleLoginSuccess() {
  router.push('/')
}

function goToRegister() {
  router.push('/register')
}

function goToForgotPassword() {
  router.push('/forgot-password')
}
</script>

<template>
  <div class="login-container">
    <div class="login-wrapper fade-in-up-delay" :class="{ visible: imageLoaded }">
      <div
        class="login-card"
        :class="{ 'login-card--ready': imageLoaded }"
        :style="{ '--login-card-bg': `url(${currentBg})` }"
      >
        <div class="login-card__main">
          <section class="login-panel login-panel--form">
            <h2 class="login-title">用户登录</h2>
            <LoginForm @success="handleLoginSuccess" />
          </section>

          <div class="login-divider" />

          <aside class="login-panel login-panel--aside">
            <div class="login-footer">
              <div class="footer-row">
                <span>还没有账号？</span>
                <a-button type="link" class="footer-link" @click="goToRegister">立即注册</a-button>
              </div>
              <div class="footer-row">
                <a-button type="link" class="footer-link" @click="goToForgotPassword">
                  忘记密码？
                </a-button>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 64px);
  padding: 32px 20px;
  background: #fff;
}

.login-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-card {
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

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--login-card-bg);
  background-size: cover;
  background-position: center;
  opacity: 0.4;
  transform: scale(1.04);
  transition: opacity 0.4s ease;
}

.login-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(255, 255, 255, 0.54));
}

.login-card > * {
  position: relative;
  z-index: 1;
}

.login-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1.52fr) 1px minmax(260px, 0.82fr);
  align-items: stretch;
  gap: 40px;
}

.login-panel--form {
  min-width: 0;
}

.login-panel--aside {
  display: flex;
  align-items: center;
}

.login-divider {
  width: 1px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.02) 0%,
    rgba(0, 0, 0, 0.12) 20%,
    rgba(0, 0, 0, 0.12) 80%,
    rgba(0, 0, 0, 0.02) 100%
  );
}

.login-card--ready::before {
  opacity: 0.42;
}

.login-title {
  text-align: left;
  margin: 0 0 30px 0;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.login-footer {
  width: 100%;
  font-size: 14px;
  color: #666;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  .login-card {
    padding: 34px 24px;
    max-width: 100%;
    border-radius: 22px;
  }

  .login-card__main {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .login-divider {
    width: 100%;
    height: 1px;
  }

  .login-panel--aside {
    align-items: flex-start;
  }

  .login-title {
    font-size: 24px;
  }
}
</style>
