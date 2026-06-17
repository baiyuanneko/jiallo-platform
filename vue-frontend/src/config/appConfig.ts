import type { InjectionKey } from 'vue'

export interface AppConfig {
  apiBaseUrl: string
  clientUniqueName: string
}

declare global {
  interface Window {
    __APP_CONFIG__?: AppConfig
  }
}

export const appConfigKey: InjectionKey<AppConfig> = Symbol('AppConfig')

export const getAppConfig = (): AppConfig => {
  if (!window.__APP_CONFIG__) {
    throw new Error(
      'App configuration not found. Make sure /config/appConfig.js is loaded before the app starts.',
    )
  }

  return window.__APP_CONFIG__
}

export const setAppConfig = (config: AppConfig): AppConfig => {
  window.__APP_CONFIG__ = config
  return config
}
