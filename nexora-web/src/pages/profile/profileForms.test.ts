import { describe, expect, it } from 'vitest'
import { createPasswordFormSchema, emailFormSchema, normalizeEmailForm, profileFormSchema, profileFormToPayload, profileRecordToForm } from '@/pages/profile/profileForms'

const passwordPolicy = {
  minLength: 8,
  maxLength: 20,
  requireUppercase: true,
  requireLowercase: true,
  requireNumber: true,
  requireSpecial: false
}

describe('profile forms', () => {
  it('normalizes profile values without changing the existing avatar', () => {
    const form = profileRecordToForm({ id: 7, status: 1, nickname: 'Nexora', email: 'user@example.com', mobile: undefined, sex: 0 })
    expect(form).toEqual({ nickname: 'Nexora', mobile: '', sex: 0 })
    expect(profileFormToPayload({ nickname: ' 新昵称 ', mobile: ' 13800138000 ', sex: 2 }, '/avatar.png')).toEqual({
      nickname: '新昵称', mobile: '13800138000', sex: 2, avatar: '/avatar.png'
    })
    expect(profileFormToPayload({ nickname: '新昵称', mobile: ' ', sex: 0 })).toEqual({
      nickname: '新昵称', mobile: '', sex: 0, avatar: undefined
    })
  })

  it('validates profile and email formats', () => {
    expect(profileFormSchema.safeParse({ nickname: '', mobile: '123', sex: 4 }).success).toBe(false)
    expect(emailFormSchema.safeParse({ email: 'bad-email', code: '12a4' }).success).toBe(false)
    expect(normalizeEmailForm({ email: ' USER@EXAMPLE.COM ', code: ' 123456 ' })).toEqual({ email: 'user@example.com', code: '123456' })
  })

  it('applies the configured password policy and cross-field checks', () => {
    const schema = createPasswordFormSchema(passwordPolicy)
    expect(schema.safeParse({ oldPassword: 'OldPass1', newPassword: 'weak', confirmPassword: 'weak' }).success).toBe(false)
    expect(schema.safeParse({ oldPassword: 'NewPass1', newPassword: 'NewPass1', confirmPassword: 'NewPass1' }).success).toBe(false)
    expect(schema.safeParse({ oldPassword: 'OldPass1', newPassword: 'NewPass1', confirmPassword: 'NewPass1' }).success).toBe(true)
  })
})
