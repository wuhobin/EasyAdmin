import { describe, expect, it } from 'vitest'
import { configDetailToForm, parseConfigForm } from '@/pages/system/configForm'

describe('config form', () => {
  it('validates cross-field login and password limits', () => {
    const login = configDetailToForm({ maxRetryCount: 5, lockTimeMinutes: 30, rememberMeEnabled: true, sessionTimeoutSeconds: 7200, rememberMeTimeoutSeconds: 3600, singleLogin: false })
    expect(parseConfigForm('login', login).success).toBe(false)

    const password = configDetailToForm({ minLength: 20, maxLength: 12, requireUppercase: false, requireLowercase: false, requireNumber: false, requireSpecial: false })
    expect(parseConfigForm('password', password).success).toBe(false)
  })

  it('requires SMTP credentials only when email is enabled', () => {
    const disabled = configDetailToForm({ enabled: false, host: '', port: 465, username: '', password: '', fromName: '', ssl: true })
    expect(parseConfigForm('email', disabled).success).toBe(true)

    const enabled = { ...disabled, enabled: true }
    expect(parseConfigForm('email', enabled).success).toBe(false)

    const whitespacePassword = { ...enabled, host: 'smtp.example.com', username: 'admin@example.com', password: '   ' }
    expect(parseConfigForm('email', whitespacePassword).success).toBe(false)
  })

  it('requires custom watermark text only for the custom type', () => {
    const system = configDetailToForm({ siteName: 'Nexora', shortTitle: 'Nexora Admin', siteDescription: '', siteLogo: '', copyright: '', icp: '', watermarkEnabled: true, watermarkType: 'custom', watermarkCustomText: '', watermarkOpacity: 0.15 })
    expect(parseConfigForm('system', system).success).toBe(false)
  })
})
