import request from '@/api/client'

export interface SystemConfig {
  siteName: string
  shortTitle: string
  siteDescription: string
  siteLogo: string
  copyright: string
  icp: string
  watermarkEnabled: boolean
  watermarkType: 'username' | 'username_time' | 'sitename' | 'custom'
  watermarkCustomText: string
  watermarkOpacity: number
}

export interface RegisterConfig {
  captchaEnabled: boolean
  verifyEmail: boolean
  defaultRoleCode: string
  needAudit: boolean
}

export interface WechatConfig {
  enabled: boolean
  qrCodeUrl: string
  appId: string
  appSecret: string
  token: string
  aesKey: string
}

export interface LoginConfig {
  maxRetryCount: number
  lockTimeMinutes: number
  rememberMeEnabled: boolean
  sessionTimeoutSeconds: number
  rememberMeTimeoutSeconds: number
  singleLogin: boolean
}

export interface PasswordConfig {
  minLength: number
  maxLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireNumber: boolean
  requireSpecial: boolean
}

export interface EmailConfig {
  enabled: boolean
  host: string
  port: number
  username: string
  password: string
  fromName: string
  ssl: boolean
}

export type ConfigGroupCode = 'system' | 'register' | 'login' | 'password' | 'email' | 'wechat'

export interface ConfigValueByGroup {
  system: SystemConfig
  register: RegisterConfig
  login: LoginConfig
  password: PasswordConfig
  email: EmailConfig
  wechat: WechatConfig
}

export interface ConfigGroupSummary {
  id: number
  groupCode: ConfigGroupCode
  groupName: string
  sort: number
  updateTime: string
}

export interface ConfigGroupDetail<T extends ConfigGroupCode = ConfigGroupCode> {
  id: number
  groupCode: T
  groupName: string
  configValue: ConfigValueByGroup[T]
  sort: number
  createTime: string
  updateTime: string
}

export type PublicRegisterConfig = Omit<RegisterConfig, 'defaultRoleCode'>
export type PublicWechatConfig = Pick<WechatConfig, 'enabled' | 'qrCodeUrl'>
export type PublicLoginConfig = Pick<LoginConfig, 'rememberMeEnabled'>

export interface PublicConfig {
  system: SystemConfig
  register: PublicRegisterConfig
  login: PublicLoginConfig
  password: PasswordConfig
  wechat: PublicWechatConfig
}

export const DEFAULT_PUBLIC_CONFIG: PublicConfig = {
  system: {
    siteName: 'NEXORA ADMIN',
    shortTitle: 'NEXORA ADMIN 后台管理',
    siteDescription: '一个现代化的后台管理系统',
    siteLogo: '',
    copyright: 'Copyright © 2026 Nexora Admin',
    icp: '',
    watermarkEnabled: false,
    watermarkType: 'username_time',
    watermarkCustomText: '',
    watermarkOpacity: 0.15
  },
  register: { captchaEnabled: true, verifyEmail: true, needAudit: false },
  login: { rememberMeEnabled: true },
  password: {
    minLength: 6,
    maxLength: 20,
    requireUppercase: false,
    requireLowercase: false,
    requireNumber: false,
    requireSpecial: false
  },
  wechat: { enabled: false, qrCodeUrl: '' }
}

export const getPublicConfigApi = () => request<PublicConfig>({ url: '/sys/config-group/public', method: 'get' })

export const getConfigGroupListApi = () => request<ConfigGroupSummary[]>({ url: '/sys/config-group/list', method: 'get' })

export const getConfigGroupApi = <T extends ConfigGroupCode>(groupCode: T) => request<ConfigGroupDetail<T>>({
  url: `/sys/config-group/${groupCode}`,
  method: 'get'
})

export const updateConfigGroupApi = <T extends ConfigGroupCode>(groupCode: T, data: ConfigValueByGroup[T]) => request<void>({
  url: `/sys/config-group/${groupCode}`,
  method: 'put',
  data
})

export const refreshConfigGroupCacheApi = () => request<void>({ url: '/sys/config-group/refresh', method: 'post' })

export const testConfigEmailApi = (to: string) => request<void>({
  url: '/sys/config-group/test-email',
  method: 'post',
  params: { to }
})

export const testWechatConnectionApi = () =>
  request<void>({ url: '/auth/wechat/test-connection', method: 'post' })
