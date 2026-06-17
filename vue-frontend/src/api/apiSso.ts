// SSO 登录相关 API 接口

import request from '@/utils/request'
import type { GetSsoClientInfoVo, ConfirmLoginVo, RSsoClient, RString } from '@/types/sso'
import { getAppConfig } from '@/config/appConfig'

/**
 * 获取 SSO 客户端信息
 * 无需登录，用于展示授权页面信息
 */
export function getSsoClientInfoApi(data: GetSsoClientInfoVo) {
  return request.post<RSsoClient>('/sso/getSsoClientInfo', data)
}

/**
 * 确认 SSO 登录授权
 * 需要登录状态，返回用于第三方网站认证的 token
 */
export function confirmSsoLoginApi(data: ConfirmLoginVo) {
  return request.post<RString>('/sso/confirmLogin', data)
}

/**
 * 获取 SSO 客户端图标 URL
 * 公开接口，可直接用于 <img> 标签的 src 属性
 * @param clientUniqueName - SSO 客户端唯一名称
 * @param cacheBust - 可选的缓存破坏参数（如时间戳）
 */
export function getSsoClientIconUrl(clientUniqueName: string, cacheBust?: string | number): string {
  const { apiBaseUrl } = getAppConfig()
  const url = `${apiBaseUrl}/sso/icon/${clientUniqueName}`
  return cacheBust ? `${url}?v=${cacheBust}` : url
}
