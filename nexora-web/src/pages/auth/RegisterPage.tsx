import { KeyOutlined, LockOutlined, MailOutlined } from '@ant-design/icons'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import AntApp from 'antd/es/app'
import { AuthButton, AuthInput, PasswordInput } from '@/components/auth/AuthControls'
import { AuthLayout, type AuthAnimationState } from '@/pages/auth/AuthLayout'
import { CaptchaDialog } from '@/pages/auth/CaptchaDialog'
import { registerApi, sendRegisterCodeApi } from '@/api/auth'
import { HOME_PATH } from '@/routes/routeAdapter'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { getToken } from '@/utils/token'
import { isValidEmail, validatePasswordByPolicy, passwordPolicyDescription } from '@/utils/password-policy'

export function RegisterPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const config = usePublicConfigStore(state => state.config)
  const configStatus = usePublicConfigStore(state => state.status)
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [focused, setFocused] = useState<'email' | 'password' | null>(null)
  const [loading, setLoading] = useState(false)
  const [codeSending, setCodeSending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [captchaOpen, setCaptchaOpen] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (configStatus === 'idle') void usePublicConfigStore.getState().load()
  }, [configStatus])

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

  const validateEmail = () => {
    if (!isValidEmail(email.trim())) {
      setError('请输入有效的邮箱地址')
      return false
    }
    return true
  }

  const sendCode = async () => {
    setError('')
    if (!validateEmail() || codeSending || countdown > 0) return
    setCodeSending(true)
    try {
      await sendRegisterCodeApi({ email: email.trim() })
      setCountdown(60)
      message.success('验证码已发送，请查收邮箱')
    } catch {
      setError('验证码发送失败，请稍后重试')
    } finally {
      setCodeSending(false)
    }
  }

  const submitRegistration = async (captchaId?: string) => {
    const passwordError = validatePasswordByPolicy(password, config.password)
    if (!validateEmail() || (config.register.verifyEmail && !/^\d{4,8}$/.test(code)) || passwordError) {
      setError(passwordError || (config.register.verifyEmail ? '请输入 4-8 位邮箱验证码' : '请检查注册信息'))
      return
    }
    setLoading(true)
    try {
      await registerApi({ email: email.trim(), password, code: config.register.verifyEmail ? code : undefined, source: 'ADMIN', captchaId })
      if (config.register.needAudit) {
        message.success('注册申请已提交，请等待管理员审核')
      } else {
        message.success('注册成功，请使用新账号登录')
      }
      navigate('/login', { replace: true, state: { message: '账号创建完成，请登录' } })
    } catch {
      setError('注册失败，请检查信息后重试')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!(await usePublicConfigStore.getState().load())) {
      setError('系统安全配置加载失败，请稍后重试')
      return
    }
    if (config.register.captchaEnabled) {
      setCaptchaOpen(true)
    } else {
      await submitRegistration()
    }
  }

  return (
    <AuthLayout animationState={animationState}>
      <div className="auth-heading-mark auth-heading-mark-register"><span /><span /><span /></div>
      <header className="auth-heading">
        <h1>创建你的账号</h1>
        <p>验证邮箱，建立新的管理工作台账号</p>
      </header>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <AuthInput id="register-email" label="邮箱" type="email" value={email} onChange={event => { setEmail(event.target.value); setError('') }} onFocus={() => setFocused('email')} onBlur={() => setFocused(null)} placeholder="you@example.com" autoComplete="email" leading={<MailOutlined />} />
        {config.register.verifyEmail ? <div className="auth-code-row">
          <AuthInput id="register-code" label="邮箱验证码" value={code} onChange={event => { setCode(event.target.value.replace(/\D/g, '').slice(0, 8)); setError('') }} placeholder="请输入验证码" inputMode="numeric" autoComplete="one-time-code" leading={<KeyOutlined />} />
          <button className="auth-code-button" type="button" disabled={codeSending || countdown > 0} onClick={() => void sendCode()}>{countdown > 0 ? `${countdown} 秒` : codeSending ? '发送中…' : '获取验证码'}</button>
        </div> : null}
        <PasswordInput id="register-password" label="密码" value={password} onChange={event => { setPassword(event.target.value); setError('') }} onFocus={() => setFocused('password')} onBlur={() => setFocused(null)} placeholder={passwordPolicyDescription(config.password)} autoComplete="new-password" leading={<LockOutlined />} visible={showPassword} onToggle={() => setShowPassword(value => !value)} />
        <p className="auth-policy-hint">{passwordPolicyDescription(config.password)}</p>
        {error ? <div className="auth-error-banner" role="alert">{error}</div> : null}
        <AuthButton type="submit" loading={loading}>创建账号</AuthButton>
      </form>
      <p className="auth-switch-copy">已有账号？ <Link to="/login" className="auth-strong-link">返回登录</Link></p>
      <CaptchaDialog open={captchaOpen} onOpenChange={setCaptchaOpen} onSuccess={captchaId => { setCaptchaOpen(false); void submitRegistration(captchaId) }} />
    </AuthLayout>
  )
}
