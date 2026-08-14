import { LockOutlined, MailOutlined } from '@ant-design/icons'
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Navigate, Link, useLocation, useNavigate } from 'react-router-dom'
import AntApp from 'antd/es/app'
import { AuthButton, AuthCheckbox, AuthInput, PasswordInput } from '@/components/auth/AuthControls'
import { AuthLayout, type AuthAnimationState } from '@/pages/auth/AuthLayout'
import { HOME_PATH } from '@/routes/routeAdapter'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { getToken, setToken } from '@/utils/token'
import {
  cancelWechatLoginApi,
  createWechatLoginTransactionApi,
  pollWechatLoginApi,
  type WechatLoginTransaction
} from '@/api/auth'
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
  const [loginMode, setLoginMode] = useState<'email' | 'wechat'>('email')
  const handleWechatSuccess = useCallback(() => navigate(HOME_PATH, { replace: true }), [navigate])

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
        <h1>欢迎回来</h1>
        <p>{flashMessage || '输入账号信息，进入管理工作台'}</p>
      </header>
      {config.wechat.enabled ? <div className="auth-login-tabs" role="tablist" aria-label="登录方式">
        <button type="button" role="tab" aria-selected={loginMode === 'email'} className={loginMode === 'email' ? 'is-active' : ''} onClick={() => setLoginMode('email')}>邮箱密码</button>
        <button type="button" role="tab" aria-selected={loginMode === 'wechat'} className={loginMode === 'wechat' ? 'is-active' : ''} onClick={() => setLoginMode('wechat')}>微信扫码</button>
      </div> : null}
      {loginMode === 'wechat' && config.wechat.enabled
        ? <WechatLoginPanel qrCodeUrl={config.wechat.qrCodeUrl} onSuccess={handleWechatSuccess} />
        : <form className="auth-form" onSubmit={handleSubmit} noValidate>
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
      </form>}
      <p className="auth-switch-copy">还没有账号？ <Link to="/register" className="auth-strong-link">创建账号</Link></p>
    </AuthLayout>
  )
}

function WechatLoginPanel({ qrCodeUrl, onSuccess }: { qrCodeUrl: string; onSuccess: () => void }) {
  const { message } = AntApp.useApp()
  const transactionRef = useRef<WechatLoginTransaction>()
  const transaction = useMutation({
    mutationFn: async () => (await createWechatLoginTransactionApi()).data,
    onSuccess: data => {
      transactionRef.current = data
    }
  })

  useEffect(() => {
    transaction.mutate()
    return () => {
      const current = transactionRef.current
      if (current) void cancelWechatLoginApi(current)
    }
    // A new transaction is intentionally tied to this panel mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const poll = useQuery({
    queryKey: ['wechat-login', transaction.data?.transactionId],
    enabled: Boolean(transaction.data),
    queryFn: async () => (await pollWechatLoginApi(transaction.data!)).data,
    refetchInterval: query => query.state.data?.status === 'PENDING' || !query.state.data ? 3000 : false,
    retry: false
  })

  useEffect(() => {
    const result = poll.data
    if (result?.status === 'SUCCESS' && result.user) {
      setToken(result.user.token, false)
      message.success('微信登录成功')
      onSuccess()
    }
  }, [message, onSuccess, poll.data])

  const status = poll.data
  return <div className="auth-wechat-panel">
    {qrCodeUrl ? <img src={qrCodeUrl} alt="微信公众号二维码" className="auth-wechat-qr" /> : <div className="auth-error-banner">管理员尚未配置公众号二维码</div>}
    {transaction.isPending ? <p>正在生成登录码…</p> : transaction.isError ? <div className="auth-error-banner">微信登录暂不可用，请稍后重试</div> : null}
    {transaction.data ? <>
      <p>微信扫码关注公众号后，向公众号发送下面的 6 位数字</p>
      <strong className="auth-wechat-code">{transaction.data.code}</strong>
      <small>登录码 5 分钟内有效，网页会自动检测登录结果</small>
    </> : null}
    {status?.status === 'PENDING_AUDIT' ? <div className="auth-error-banner">{status.message}</div> : null}
    {status && ['FAILED', 'EXPIRED'].includes(status.status) ? <div className="auth-error-banner">{status.message}</div> : null}
  </div>
}
