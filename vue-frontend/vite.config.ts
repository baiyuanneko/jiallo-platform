import { fileURLToPath, URL } from 'node:url'
import process from 'node:process'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// Dev-server proxy target: the compose service name inside Docker,
// localhost when running natively on the host.
const backendTarget = process.env.VITE_BACKEND_URL || 'http://localhost:8233'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // Mirrors the nginx rule in production: /jiallo-backend/** -> backend:8233/**
      '/jiallo-backend': {
        target: backendTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/jiallo-backend/, ''),
      },
    },
  },
})
