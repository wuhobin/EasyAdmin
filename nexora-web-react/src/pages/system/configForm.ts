import { z } from 'zod'
import type { ConfigGroupCode, ConfigValueByGroup, PasswordConfig, SystemConfig } from '@/api/config'

export interface ConfigFormValues extends SystemConfig, PasswordConfig {
  enabled: boolean
  captchaEnabled: boolean
  verifyEmail: boolean
  defaultRoleCode: string
  needAudit: boolean
  maxRetryCount: number
  lockTimeMinutes: number
  rememberMeEnabled: boolean
  sessionTimeoutSeconds: number
  rememberMeTimeoutSeconds: number
  singleLogin: boolean
  host: string
  port: number
  username: string
  password: string
  fromName: string
  ssl: boolean
}

export const emptyConfigForm: ConfigFormValues = {
  siteName: '',
  shortTitle: '',
  siteDescription: '',
  siteLogo: '',
  copyright: '',
  icp: '',
  watermarkEnabled: false,
  watermarkType: 'username_time',
  watermarkCustomText: '',
  watermarkOpacity: 0.15,
  enabled: true,
  captchaEnabled: true,
  verifyEmail: true,
  defaultRoleCode: 'user',
  needAudit: false,
  maxRetryCount: 5,
  lockTimeMinutes: 30,
  rememberMeEnabled: true,
  sessionTimeoutSeconds: 3600,
  rememberMeTimeoutSeconds: 259200,
  singleLogin: false,
  minLength: 6,
  maxLength: 20,
  requireUppercase: false,
  requireLowercase: false,
  requireNumber: false,
  requireSpecial: false,
  host: '',
  port: 465,
  username: '',
  password: '',
  fromName: 'Nexora Admin',
  ssl: true
}

const systemSchema = z.object({
  siteName: z.string().trim().min(1, '请输入站点名称').max(100, '站点名称不能超过 100 个字符'),
  shortTitle: z.string().trim().min(1, '请输入后台短标题').max(100, '后台短标题不能超过 100 个字符'),
  siteDescription: z.string().trim().max(500, '站点描述不能超过 500 个字符'),
  siteLogo: z.string().trim().max(1024, 'Logo 地址不能超过 1024 个字符'),
  copyright: z.string().trim().max(255, '版权信息不能超过 255 个字符'),
  icp: z.string().trim().max(100, 'ICP备案信息不能超过 100 个字符'),
  watermarkEnabled: z.boolean(),
  watermarkType: z.enum(['username', 'username_time', 'sitename', 'custom']),
  watermarkCustomText: z.string().trim().max(100, '水印文本不能超过 100 个字符'),
  watermarkOpacity: z.number().min(0.01, '水印透明度不能小于 1%').max(0.5, '水印透明度不能大于 50%')
}).superRefine((values, context) => {
  if (values.watermarkType === 'custom' && !values.watermarkCustomText) {
    context.addIssue({ code: 'custom', path: ['watermarkCustomText'], message: '请输入自定义水印文本' })
  }
})

const registerSchema = z.object({
  enabled: z.boolean(),
  captchaEnabled: z.boolean(),
  verifyEmail: z.boolean(),
  defaultRoleCode: z.string().trim().min(1, '请输入默认角色编码').max(50, '角色编码不能超过 50 个字符'),
  needAudit: z.boolean()
})

const loginSchema = z.object({
  maxRetryCount: z.number().int().min(1, '不能小于 1').max(20, '不能大于 20'),
  lockTimeMinutes: z.number().int().min(1, '不能小于 1 分钟').max(1440, '不能大于 1440 分钟'),
  rememberMeEnabled: z.boolean(),
  sessionTimeoutSeconds: z.number().int().min(300, '不能小于 300 秒').max(86400, '不能大于 86400 秒'),
  rememberMeTimeoutSeconds: z.number().int().min(3600, '不能小于 3600 秒').max(31536000, '不能大于 31536000 秒'),
  singleLogin: z.boolean()
}).refine(values => values.rememberMeTimeoutSeconds >= values.sessionTimeoutSeconds, {
  path: ['rememberMeTimeoutSeconds'],
  message: '记住我会话时长不能小于普通会话时长'
})

const passwordSchema = z.object({
  minLength: z.number().int().min(6, '最小长度不能小于 6').max(32, '最小长度不能大于 32'),
  maxLength: z.number().int().min(6, '最大长度不能小于 6').max(64, '最大长度不能大于 64'),
  requireUppercase: z.boolean(),
  requireLowercase: z.boolean(),
  requireNumber: z.boolean(),
  requireSpecial: z.boolean()
}).refine(values => values.maxLength >= values.minLength, {
  path: ['maxLength'],
  message: '密码最大长度不能小于最小长度'
})

const emailSchema = z.object({
  enabled: z.boolean(),
  host: z.string().trim().max(255, 'SMTP 服务器不能超过 255 个字符'),
  port: z.number().int().min(1, '端口不能小于 1').max(65535, '端口不能大于 65535'),
  username: z.string().trim().max(255, '用户名不能超过 255 个字符'),
  password: z.string().max(255, '密码不能超过 255 个字符'),
  fromName: z.string().trim().max(100, '发件人名称不能超过 100 个字符'),
  ssl: z.boolean()
}).superRefine((values, context) => {
  if (!values.enabled) return
  if (!values.host) context.addIssue({ code: 'custom', path: ['host'], message: '请输入 SMTP 服务器' })
  if (!values.username) context.addIssue({ code: 'custom', path: ['username'], message: '请输入 SMTP 用户名' })
  if (!values.password.trim()) context.addIssue({ code: 'custom', path: ['password'], message: '请输入 SMTP 密码或授权码' })
})

const schemas = { system: systemSchema, register: registerSchema, login: loginSchema, password: passwordSchema, email: emailSchema }

export function configDetailToForm<T extends ConfigGroupCode>(value: ConfigValueByGroup[T]): ConfigFormValues {
  return { ...emptyConfigForm, ...value }
}

export function parseConfigForm(groupCode: ConfigGroupCode, values: ConfigFormValues) {
  return schemas[groupCode].safeParse(values) as z.ZodSafeParseResult<ConfigValueByGroup[ConfigGroupCode]>
}
