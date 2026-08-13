import { LockOutlined, MailOutlined } from '@ant-design/icons'
import { useState, type FormEvent } from 'react'
import { Navigate, Link, useLocation, useNavigate } from 'react-router-dom'
import AntApp from 'antd/es/app'
import { AuthButton, AuthCheckbox, AuthInput, PasswordInput } from '@/components/auth/AuthControls'
import { AuthLayout, type AuthAnimationState } from '@/pages/auth/AuthLayout'
import { HOME_PATH } from '@/routes/routeAdapter'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { getToken } from '@/utils/token'
import { isValidEmail } from '@/utils/password-policy'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = AntApp.useApp()
  const login = useAuthStore(state => state.login)
  const config = usePublicConfigStore(state => state.config)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [focused, setFocused] = useState<'email' | 'password' | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (getToken()) return <Navigate to={HOME_PATH} replace />

  const animationState: AuthAnimationState = {
    isTyping: focused === 'email',
    isPasswordFocused: focused === 'password',
    showPassword,
    passwordLength: password.length,
    isSubmitting: loading,
    hasError: Boolean(error)
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    const cleanEmail = email.trim()
    if (!isValidEmail(cleanEmail)) {
      setError('请输入有效的邮箱地址')
      return
    }
    if (!password) {
      setError('请输入密码')
      return
    }
    if (!(await usePublicConfigStore.getState().load())) {
      setError('系统安全配置加载失败，请稍后重试')
      return
    }
    setLoading(true)
    try {
      await login({ email: cleanEmail, password, rememberMe, source: 'ADMIN' })
      message.success('登录成功')
      navigate(HOME_PATH, { replace: true })
    } catch {
      setError('邮箱或密码不正确，请重试')
    } finally {
      setLoading(false)
    }
  }

  const flashMessage = (location.state as { message?: string } | null)?.message

  return (
    <AuthLayout animationState={animationState}>
      <div className="auth-heading-mark"><span /><span /><span /></div>
      <header className="auth-heading">
        <h2>欢迎回来</h2>
        <p>{flashMessage || '输入账号信息，进入管理工作台'}</p>
      </header>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <AuthInput
          id="login-email"
          label="邮箱"
          type="email"
          value={email}
          onChange={event => { setEmail(event.target.value); setError('') }}
          onFocus={() => setFocused('email')}
          onBlur={() => setFocused(null)}
          placeholder="you@example.com"
          autoComplete="email"
          leading={<MailOutlined />}
          aria-invalid={Boolean(error)}
        />
        <PasswordInput
          id="login-password"
          label="密码"
          value={password}
          onChange={event => { setPassword(event.target.value); setError('') }}
          onFocus={() => setFocused('password')}
          onBlur={() => setFocused(null)}
          placeholder="请输入密码"
          autoComplete="current-password"
          leading={<LockOutlined />}
          visible={showPassword}
          onToggle={() => setShowPassword(value => !value)}
        />
        <div className="auth-form-options">
          {config.login.rememberMeEnabled ? <AuthCheckbox checked={rememberMe} onCheckedChange={setRememberMe}>记住我 3 天</AuthCheckbox> : <span />}
          <Link to="/forgot-password" className="auth-inline-link">忘记密码？</Link>
        </div>
        {error ? <div className="auth-error-banner" role="alert">{error}</div> : null}
        <AuthButton type="submit" loading={loading}>进入工作台</AuthButton>
      </form>
      <p className="auth-switch-copy">还没有账号？ <Link to="/register" className="auth-strong-link">创建账号</Link></p>
    </AuthLayout>
  )
}
