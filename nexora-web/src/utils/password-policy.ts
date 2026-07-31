import type { PasswordConfig } from '@/api/system/config'

const SPECIAL_CHARACTER_PATTERN = /[^\p{L}\p{N}]/u
const UPPERCASE_PATTERN = /\p{Lu}/u
const LOWERCASE_PATTERN = /\p{Ll}/u
const NUMBER_PATTERN = /\p{Nd}/u

export function passwordPolicyDescription(policy: PasswordConfig): string {
  const requirements = [
    `${policy.minLength}-${policy.maxLength} 个字符`,
    policy.requireUppercase ? '大写字母' : '',
    policy.requireLowercase ? '小写字母' : '',
    policy.requireNumber ? '数字' : '',
    policy.requireSpecial ? '特殊字符' : ''
  ].filter(Boolean)
  return `密码需包含：${requirements.join('、')}，且 UTF-8 编码不超过 72 字节`
}

export function validatePasswordByPolicy(
  password: string,
  policy: PasswordConfig
): string | null {
  if (!password) return '请输入密码'
  const characterLength = Array.from(password).length
  if (characterLength < policy.minLength || characterLength > policy.maxLength) {
    return `密码长度必须在 ${policy.minLength} 到 ${policy.maxLength} 个字符之间`
  }
  if (new TextEncoder().encode(password).length > 72) {
    return '密码 UTF-8 编码不能超过 72 字节'
  }
  if (policy.requireUppercase && !UPPERCASE_PATTERN.test(password)) return '密码必须包含大写字母'
  if (policy.requireLowercase && !LOWERCASE_PATTERN.test(password)) return '密码必须包含小写字母'
  if (policy.requireNumber && !NUMBER_PATTERN.test(password)) return '密码必须包含数字'
  if (policy.requireSpecial && !SPECIAL_CHARACTER_PATTERN.test(password)) {
    return '密码必须包含特殊字符'
  }
  return null
}
