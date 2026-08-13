import { KeyOutlined, LockOutlined, MailOutlined } from '@ant-design/icons'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import AntApp from 'antd/es/app'
import { AuthButton, AuthInput, PasswordInput } from '@/components/auth/AuthControls'
import { AuthLayout, type AuthAnimationState } from '@/pages/auth/AuthLayout'
import { resetPasswordApi, sendResetPasswordCodeApi } from '@/api/auth'
import { HOME_PATH } from '@/routes/routeAdapter'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { getToken } from '@/utils/token'
import { isValidEmail, validatePasswordByPolicy, passwordPolicyDescription } from '@/utils/password-policy'

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const config = usePublicConfigStore(state => state.config)
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [focused, setFocused] = useState<'email' | 'password' | null>(null)
  const [loading, setLoading] = useState(false)
  const [codeSending, setCodeSending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [error, setError] = useState('')

  useEffect(() => { void usePublicConfigStore.getState().load() }, [])
  useEffect(() => {
    if (countdown <= 0) return undefined
    const timer = window.setInterval(() => setCountdown(value => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [countdown])

  if (getToken()) return <Navigate to={HOME_PATH} replace />

  const animationState: AuthAnimationState = {
    isTyping: focused === 'email',
    isPasswordFocused: focused === 'password',
    showPassword,
    passwordLength: password.length,
    isSubmitting: loading,
    hasError: Boolean(error)
  }

  const sendCode = async () => {
    setError('')
    if (!isValidEmail(email.trim())) { setError('请输入有效的邮箱地址'); return }
    if (codeSending || countdown > 0) return
    setCodeSending(true)
    try {
      await sendResetPasswordCodeApi({ email: email.trim() })
      setCountdown(60)
      message.success('验证码已发送，请查收邮箱')
    } catch {
      setError('验证码发送失败，请稍后重试')
    } finally {
      setCodeSending(false)
    }
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!isValidEmail(email.trim())) { setError('请输入有效的邮箱地址'); return }
    if (!/^\d{4,8}$/.test(code)) { setError('请输入 4-8 位邮箱验证码'); return }
    const passwordError = validatePasswordByPolicy(password, config.password)
    if (passwordError) { setError(passwordError); return }
    if (password !== confirmPassword) { setError('两次输入的密码不一致'); return }
    setLoading(true)
    try {
      await resetPasswordApi({ email: email.trim(), code, password })
      message.success('密码重置成功，请使用新密码登录')
      navigate('/login', { replace: true, state: { message: '密码已更新，请重新登录' } })
    } catch {
      setError('密码重置失败，请检查验证码后重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout animationState={animationState}>
      <div className="auth-heading-mark auth-heading-mark-reset"><span /><span /><span /></div>
      <header className="auth-heading">
        <h2>重置登录密码</h2>
        <p>验证当前绑定邮箱后设置新密码</p>
      </header>
      <form className="auth-form" onSubmit={submit} noValidate>
        <AuthInput id="reset-email" label="绑定邮箱" type="email" value={email} onChange={event => { setEmail(event.target.value); setError('') }} onFocus={() => setFocused('email')} onBlur={() => setFocused(null)} placeholder="you@example.com" autoComplete="email" leading={<MailOutlined />} />
        <div className="auth-code-row">
          <AuthInput id="reset-code" label="邮箱验证码" value={code} onChange={event => { setCode(event.target.value.replace(/\D/g, '').slice(0, 8)); setError('') }} placeholder="请输入验证码" inputMode="numeric" autoComplete="one-time-code" leading={<KeyOutlined />} />
          <button className="auth-code-button" type="button" disabled={codeSending || countdown > 0} onClick={() => void sendCode()}>{countdown > 0 ? `${countdown} 秒` : codeSending ? '发送中…' : '获取验证码'}</button>
        </div>
        <PasswordInput id="reset-password" label="新密码" value={password} onChange={event => { setPassword(event.target.value); setError('') }} onFocus={() => setFocused('password')} onBlur={() => setFocused(null)} placeholder={passwordPolicyDescription(config.password)} autoComplete="new-password" leading={<LockOutlined />} visible={showPassword} onToggle={() => setShowPassword(value => !value)} />
        <PasswordInput id="reset-confirm-password" label="确认新密码" value={confirmPassword} onChange={event => { setConfirmPassword(event.target.value); setError('') }} placeholder="再次输入新密码" autoComplete="new-password" leading={<LockOutlined />} visible={showPassword} onToggle={() => setShowPassword(value => !value)} />
        {error ? <div className="auth-error-banner" role="alert">{error}</div> : null}
        <AuthButton type="submit" loading={loading}>保存新密码</AuthButton>
      </form>
      <p className="auth-switch-copy">想起密码了？ <Link to="/login" className="auth-strong-link">返回登录</Link></p>
    </AuthLayout>
  )
}
