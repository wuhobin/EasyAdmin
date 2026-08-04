import request from '@/utils/request'

export type ConfigGroupCode = 'system' | 'register' | 'login' | 'password' | 'email'

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
  enabled: boolean
  captchaEnabled: boolean
  verifyEmail: boolean
  defaultRoleCode: string
  needAudit: boolean
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

export interface ConfigValueByGroup {
  system: SystemConfig
  register: RegisterConfig
  login: LoginConfig
  password: PasswordConfig
  email: EmailConfig
}

export interface SysConfigGroupSummary {
  id: number
  groupCode: ConfigGroupCode
  groupName: string
  sort: number
  updateTime: string
}

export interface SysConfigGroupDetail<T extends ConfigGroupCode = ConfigGroupCode> {
  id: number
  groupCode: T
  groupName: string
  configValue: ConfigValueByGroup[T]
  sort: number
  createTime: string
  updateTime: string
}

export interface PublicRegisterConfig {
  enabled: boolean
  captchaEnabled: boolean
  verifyEmail: boolean
  needAudit: boolean
}

export interface PublicLoginConfig {
  rememberMeEnabled: boolean
}

export interface SysConfigPublic {
  system: SystemConfig
  register: PublicRegisterConfig
  login: PublicLoginConfig
  password: PasswordConfig
}

export function getConfigGroupListApi() {
  return request<SysConfigGroupSummary[]>({
    url: '/sys/config-group/list',
    method: 'get'
  })
}

export function getPublicConfigApi() {
  return request<SysConfigPublic>({
    url: '/sys/config-group/public',
    method: 'get'
  })
}

export function getConfigGroupApi<T extends ConfigGroupCode>(groupCode: T) {
  return request<SysConfigGroupDetail<T>>({
    url: `/sys/config-group/${groupCode}`,
    method: 'get'
  })
}

export function updateConfigGroupApi<T extends ConfigGroupCode>(
  groupCode: T,
  data: ConfigValueByGroup[T]
) {
  return request<void>({
    url: `/sys/config-group/${groupCode}`,
    method: 'put',
    data
  })
}

export function refreshConfigGroupCacheApi() {
  return request<void>({
    url: '/sys/config-group/refresh',
    method: 'post'
  })
}

export function testConfigEmailApi(to: string) {
  return request<void>({
    url: '/sys/config-group/test-email',
    method: 'post',
    params: { to }
  })
}
