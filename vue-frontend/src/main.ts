import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './assets/main.css'

import App from './App.vue'
import router from './router'
import { appConfigKey, getAppConfig } from './config/appConfig'
import { useUserStore } from './stores/user'

const app = createApp(App)
const appConfig = getAppConfig()

// 禁用浏览器历史导航的自动滚动恢复
if ('scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual'
}

app.use(createPinia())

// 初始化用户认证状态（必须在 router 之前，让路由守卫能等待初始化完成）
const userStore = useUserStore()
userStore.initAuth()

app.use(router)
app.use(Antd)
app.provide(appConfigKey, appConfig)

app.mount('#app')
