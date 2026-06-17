<script setup lang="ts">
import { useRouter } from 'vue-router'
import RegisterForm from '@/components/RegisterForm.vue'
import { useBackgroundCarousel } from '@/composables/useBackgroundCarousel'

const router = useRouter()

const { currentBg, imageLoaded } = useBackgroundCarousel()

function handleRegisterSuccess() {
  router.push('/')
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-container">
    <!-- 注册卡片 -->
    <div class="register-wrapper fade-in-up-delay" :class="{ visible: imageLoaded }">
      <div
        class="register-card"
        :class="{ 'register-card--ready': imageLoaded }"
        :style="{ '--register-card-bg': `url(${currentBg})` }"
      >
        <div class="register-card__main">
          <section class="register-panel register-panel--form">
            <h2 class="register-title">用户注册</h2>
            <RegisterForm @success="handleRegisterSuccess" />
          </section>

          <div class="register-divider" />

          <aside class="register-panel register-panel--aside">
            <div class="register-footer">
              <div class="footer-row">
                <span>已有账号？</span>
                <a-button type="link" class="footer-link" @click="goToLogin">立即登录</a-button>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 64px);
  padding: 32px 20px;
  background: #fff;
}

.register-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.register-mascot {
  width: 200px;
  height: auto;
  margin-bottom: -50px;
  z-index: 2;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.register-card {
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

.register-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--register-card-bg);
  background-size: cover;
  background-position: center;
  opacity: 0.4;
  transform: scale(1.04);
  transition: opacity 0.4s ease;
}

.register-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(255, 255, 255, 0.54));
}

.register-card > * {
  position: relative;
  z-index: 1;
}

.register-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1.52fr) 1px minmax(260px, 0.82fr);
  align-items: stretch;
  gap: 40px;
}

.register-panel--form {
  min-width: 0;
}

.register-panel--aside {
  display: flex;
  align-items: center;
}

.register-divider {
  width: 1px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.02) 0%,
    rgba(0, 0, 0, 0.12) 20%,
    rgba(0, 0, 0, 0.12) 80%,
    rgba(0, 0, 0, 0.02) 100%
  );
}

.register-card--ready::before {
  opacity: 0.42;
}

.register-title {
  text-align: left;
  margin: 0 0 30px 0;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.register-footer {
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
  .register-card {
    padding: 34px 24px;
    max-width: 100%;
    border-radius: 22px;
  }

  .register-card__main {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .register-divider {
    width: 100%;
    height: 1px;
  }

  .register-panel--aside {
    align-items: flex-start;
  }

  .register-title {
    font-size: 24px;
  }
}
</style>
