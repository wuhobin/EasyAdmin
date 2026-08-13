import { z } from 'zod'
import type { PasswordConfig } from '@/api/config'
import type { UserProfilePayload, UserProfileRecord } from '@/api/user'
import { validatePasswordByPolicy } from '@/utils/password-policy'

export interface ProfileFormValues {
  nickname: string
  mobile: string
  sex: number
}

export interface PasswordFormValues {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface EmailFormValues {
  email: string
  code: string
}

export const profileFormSchema = z.object({
  nickname: z.string().trim().min(1, '请输入昵称').max(30, '昵称不能超过 30 个字符'),
  mobile: z.string().trim().refine(value => !value || /^1[3-9]\d{9}$/.test(value), '请输入正确的手机号'),
  sex: z.number().int().min(0).max(2)
})

export const emailFormSchema = z.object({
  email: z.string().trim().min(1, '请输入新邮箱').email('请输入正确的邮箱地址'),
  code: z.string().trim().regex(/^\d{4,8}$/, '请输入 4-8 位数字验证码')
})

export function createPasswordFormSchema(policy: PasswordConfig) {
  return z.object({
    oldPassword: z.string().min(1, '请输入当前密码'),
    newPassword: z.string(),
    confirmPassword: z.string().min(1, '请再次输入新密码')
  }).superRefine((values, context) => {
    const passwordError = validatePasswordByPolicy(values.newPassword, policy)
    if (passwordError) context.addIssue({ code: 'custom', path: ['newPassword'], message: passwordError })
    if (values.newPassword !== values.confirmPassword) {
      context.addIssue({ code: 'custom', path: ['confirmPassword'], message: '两次输入的密码不一致' })
    }
    if (values.oldPassword && values.oldPassword === values.newPassword) {
      context.addIssue({ code: 'custom', path: ['newPassword'], message: '新密码不能与当前密码相同' })
    }
  })
}

export function profileRecordToForm(profile: UserProfileRecord): ProfileFormValues {
  return {
    nickname: profile.nickname || '',
    mobile: profile.mobile || '',
    sex: profile.sex ?? 0
  }
}

export function profileFormToPayload(values: ProfileFormValues, avatar?: string): UserProfilePayload {
  return {
    nickname: values.nickname.trim(),
    mobile: values.mobile.trim(),
    sex: values.sex,
    avatar: avatar || undefined
  }
}

export function normalizeEmailForm(values: EmailFormValues): EmailFormValues {
  return { email: values.email.trim().toLowerCase(), code: values.code.trim() }
}
