import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    // 不自动滚动，由各组件内部控制（避免 router.replace 对流式聊天的副作用）
    return false
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { hideForAuth: true }, // 已登录用户访问时重定向到首页
    },
    {
      path: '/externalLogin',
      name: 'externalLogin',
      component: () => import('../views/ExternalLoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { hideForAuth: true }, // 已登录用户访问时重定向到首页
    },
    {
      path: '/forgot-password',
      name: 'forgotPassword',
      component: () => import('../views/ForgotPasswordView.vue'),
      meta: { hideForAuth: true }, // 已登录用户访问时重定向到首页
    },
    {
      path: '/sso/login',
      name: 'ssoLogin',
      component: () => import('../views/SsoLoginView.vue'),
      // 不设置 requiresAuth，由组件内部处理登录检查和跳转
    },
    {
      path: '/user/center',
      name: 'userCenter',
      component: () => import('../views/UserCenter.vue'),
      meta: { requiresAuth: true }, // 需要登录才能访问
    },
    {
      path: '/app',
      component: () => import('../views/app/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'appList',
          component: () => import('../views/app/AppListView.vue'),
        },
      ],
    },
    {
      path: '/app/agentic-chat-ui/:sessionId?',
      name: 'agenticChat',
      component: () => import('../views/agentic-chat/AgenticChatView.vue'),
      meta: { requiresAuth: true, standaloneLayout: true, title: 'Jiallo Chat' },
    },
    {
      path: '/app/agentic-chat-ui/shared-sessions',
      name: 'sharedSessions',
      component: () => import('../views/agentic-chat/SharedSessionsView.vue'),
      meta: { requiresAuth: true, standaloneLayout: true, title: 'Jiallo Chat' },
    },
    {
      path: '/app/agentic-chat-ui/shared/:sharedSessionId',
      name: 'sharedSession',
      component: () => import('../views/agentic-chat/SharedSessionView.vue'),
      meta: { requiresAuth: false, title: 'Jiallo Chat' },
    },
    {
      path: '/rag-library',
      component: () => import('../views/rag-library/RagLibraryLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'ragLibrary',
          component: () => import('../views/rag-library/RagLibraryView.vue'),
          meta: { title: 'RAG 知识库' },
        },
        {
          path: ':libraryId/docs',
          name: 'ragLibraryDocs',
          component: () => import('../views/rag-library/DocManagementView.vue'),
          meta: { title: '知识库文档' },
        },
        {
          path: 'search',
          name: 'ragLibrarySearch',
          component: () => import('../views/rag-library/SearchTestView.vue'),
          meta: { title: '检索测试' },
        },
      ],
    },
    // 后台管理路由
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      redirect: '/admin/users',
      children: [
        {
          path: 'users',
          name: 'adminUsers',
          component: () => import('../views/admin/UserManagement.vue'),
        },
        {
          path: 'sso-clients',
          name: 'adminSsoClients',
          component: () => import('../views/admin/SsoClientManagement.vue'),
        },
        {
          path: 'system-config',
          name: 'adminSystemConfig',
          component: () => import('../views/admin/SystemConfig.vue'),
        },
        {
          path: 'llm-providers',
          name: 'adminLlmProviders',
          component: () => import('../views/admin/LlmProviderManagement.vue'),
        },
        {
          path: 'model-authorizations',
          name: 'adminModelAuthorizations',
          component: () => import('../views/admin/ModelAuthorizationManagement.vue'),
        },
        {
          path: 'agent-type-authorizations',
          name: 'adminAgentTypeAuthorizations',
          component: () => import('../views/admin/AgentTypeAuthorization.vue'),
        },
        {
          path: 'feature-module-authorizations',
          name: 'adminFeatureModuleAuthorizations',
          component: () => import('../views/admin/FeatureModuleAuthorization.vue'),
        },
        {
          path: 'user-groups',
          name: 'adminUserGroups',
          component: () => import('../views/admin/UserGroupManagement.vue'),
        },
      ],
    },
  ],
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 等待认证初始化完成
  await userStore.waitForAuthInit()

  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 未登录，重定向到登录页
    next('/login')
    return
  }

  // 需要管理员权限的页面
  if (to.meta.requiresAdmin && userStore.userInfo?.roleType !== 'ADMIN') {
    // 非管理员，重定向到首页
    next('/')
    return
  }

  // 已登录用户访问登录/注册页
  if (to.meta.hideForAuth && userStore.isLoggedIn) {
    // 已登录，重定向到首页
    next('/')
    return
  }

  // 其他情况正常放行
  document.title = (to.meta.title as string) || 'Jiallo'
  next()
})

export default router
