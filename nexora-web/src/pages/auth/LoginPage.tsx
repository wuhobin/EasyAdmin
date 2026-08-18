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
  const loginModeRef = useRef<'email' | 'wechat'>('email')
  const wechatTransactionRef = useRef<WechatLoginTransaction>()
  const wechatTransactionAttemptRef = useRef(0)
  const wechatTransaction = useMutation({
    mutationFn: async (_attempt: number) => (await createWechatLoginTransactionApi()).data,
    onSuccess: (data, attempt) => {
      if (loginModeRef.current !== 'wechat' || attempt !== wechatTransactionAttemptRef.current) {
        void cancelWechatLoginApi(data)
        return
      }
      wechatTransactionRef.current = data
    }
  })
  const handleWechatSuccess = useCallback(() => navigate(HOME_PATH, { replace: true }), [navigate])

  useEffect(() => () => {
    wechatTransactionAttemptRef.current += 1
    const current = wechatTransactionRef.current
    if (current) void cancelWechatLoginApi(current)
  }, [])

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

  const switchToEmailLogin = () => {
    if (loginModeRef.current === 'email') return
    loginModeRef.current = 'email'
    wechatTransactionAttemptRef.current += 1
    setLoginMode('email')
    const current = wechatTransactionRef.current
    wechatTransactionRef.current = undefined
    if (current) void cancelWechatLoginApi(current)
    wechatTransaction.reset()
  }

  const switchToWechatLogin = () => {
    if (loginModeRef.current === 'wechat') return
    loginModeRef.current = 'wechat'
    setLoginMode('wechat')
    wechatTransaction.reset()
    const attempt = wechatTransactionAttemptRef.current + 1
    wechatTransactionAttemptRef.current = attempt
    wechatTransaction.mutate(attempt)
  }

  return (
    <AuthLayout animationState={animationState}>
      <div className="auth-heading-mark"><span /><span /><span /></div>
      <header className="auth-heading">
        <h1>欢迎回来</h1>
        <p>{flashMessage || '输入账号信息，进入管理工作台'}</p>
      </header>
      {config.wechat.enabled ? <div className="auth-login-tabs" role="tablist" aria-label="登录方式">
        <button type="button" role="tab" aria-selected={loginMode === 'email'} className={loginMode === 'email' ? 'is-active' : ''} onClick={switchToEmailLogin}>邮箱密码</button>
        <button type="button" role="tab" aria-selected={loginMode === 'wechat'} className={loginMode === 'wechat' ? 'is-active' : ''} onClick={switchToWechatLogin}>微信扫码</button>
      </div> : null}
      <div className={`auth-login-stage auth-login-stage-${loginMode}`}>
        {loginMode === 'wechat' && config.wechat.enabled
          ? <WechatLoginPanel
            qrCodeUrl={config.wechat.qrCodeUrl}
            transaction={wechatTransaction.data}
            transactionError={wechatTransaction.isError}
            onSuccess={handleWechatSuccess}
          />
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
      </div>
      {loginMode === 'email'
        ? <p className="auth-switch-copy">还没有账号？ <Link to="/register" className="auth-strong-link">创建账号</Link></p>
        : null}
    </AuthLayout>
  )
}

function WechatLoginPanel({ qrCodeUrl, transaction, transactionError, onSuccess }: {
  qrCodeUrl: string
  transaction?: WechatLoginTransaction
  transactionError: boolean
  onSuccess: () => void
}) {
  const { message } = AntApp.useApp()

  const poll = useQuery({
    queryKey: ['wechat-login', transaction?.transactionId],
    enabled: Boolean(transaction),
    queryFn: async () => (await pollWechatLoginApi(transaction!)).data,
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
  const codeLoading = !transaction && !transactionError
  const codeDigits = transaction?.code.split('') ?? []
  return <div className="auth-wechat-panel">
    <div className="auth-wechat-card-heading"><span aria-hidden="true" />微信安全登录</div>
    {qrCodeUrl
      ? <div className="auth-wechat-qr-frame"><img src={qrCodeUrl} alt="微信公众号二维码" className="auth-wechat-qr" /></div>
      : <div className="auth-error-banner">管理员尚未配置公众号二维码</div>}
    <div className="auth-wechat-content" aria-live="polite" aria-busy={codeLoading}>
      {transactionError
        ? <div className="auth-error-banner">微信登录暂不可用，请稍后重试</div>
        : <>
          <div className="auth-wechat-instruction">
            <strong>扫码后发送登录码</strong>
            <p>关注公众号，将下方 6 位数字发送给公众号</p>
          </div>
          <div className="auth-wechat-code-slot">
            {transaction
              ? <strong className="auth-wechat-code" aria-label={`登录码 ${codeDigits.join(' ')}`}>
                {codeDigits.map((digit, index) => <span key={`${digit}-${index}`}>{digit}</span>)}
              </strong>
              : <span className="auth-wechat-code-placeholder" aria-hidden="true">
                <i /><i /><i /><i /><i /><i />
              </span>}
          </div>
          <small className="auth-wechat-status"><i aria-hidden="true" />网页自动检测登录结果 <span aria-hidden="true">·</span> 登录码 5 分钟内有效</small>
          {codeLoading ? <span className="sr-only">正在生成登录码…</span> : null}
        </>}
    </div>
    {status?.status === 'PENDING_AUDIT' ? <div className="auth-error-banner">{status.message}</div> : null}
    {status && ['FAILED', 'EXPIRED'].includes(status.status) ? <div className="auth-error-banner">{status.message}</div> : null}
  </div>
}
