import { describe, expect, it } from 'vitest'
import { isValidEmail, validatePasswordByPolicy } from '@/utils/password-policy'

const strictPolicy = { minLength: 8, maxLength: 16, requireUppercase: true, requireLowercase: true, requireNumber: true, requireSpecial: true }

describe('password policy', () => {
  it('reports the first unmet requirement', () => {
    expect(validatePasswordByPolicy('', strictPolicy)).toBe('请输入密码')
    expect(validatePasswordByPolicy('short', strictPolicy)).toContain('长度')
    expect(validatePasswordByPolicy('lowercase1!', strictPolicy)).toBe('密码必须包含大写字母')
    expect(validatePasswordByPolicy('Lowercase!', strictPolicy)).toBe('密码必须包含数字')
    expect(validatePasswordByPolicy('Lowercase1', strictPolicy)).toBe('密码必须包含特殊字符')
    expect(validatePasswordByPolicy('Lowercase1!', strictPolicy)).toBeNull()
  })

  it('measures unicode characters and UTF-8 byte limits', () => {
    const unicodePolicy = { ...strictPolicy, minLength: 1, maxLength: 100, requireUppercase: false, requireLowercase: false, requireNumber: false, requireSpecial: false }
    expect(validatePasswordByPolicy('密码', unicodePolicy)).toBeNull()
    expect(validatePasswordByPolicy('😀'.repeat(19), unicodePolicy)).toBe('密码 UTF-8 编码不能超过 72 字节')
  })
})

describe('email validation', () => {
  it('accepts ordinary addresses and rejects malformed input', () => {
    expect(isValidEmail('admin@nexora.dev')).toBe(true)
    expect(isValidEmail('admin@nexora')).toBe(false)
    expect(isValidEmail('admin nexora.dev')).toBe(false)
  })
})
