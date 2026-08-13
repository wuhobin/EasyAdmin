import { LockOutlined, LogoutOutlined, UnlockOutlined } from '@ant-design/icons'
import AntApp from 'antd/es/app'
import Button from 'antd/es/button'
import Input from 'antd/es/input'
import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { logoutApi } from '@/api/auth'
import { verifyPasswordApi } from '@/api/user'
import { BrandMark } from '@/components/BrandMark'
import { useAuthStore } from '@/store/authStore'
import { useLockStore } from '@/store/lockStore'
import { usePageTabsStore } from '@/store/pageTabsStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { useRouteStore } from '@/store/routeStore'

const timeFormatter = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
const dateFormatter = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

function currentClock() {
  const now = new Date()
  return { time: timeFormatter.format(now), date: dateFormatter.format(now) }
}

export function LockScreen() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const locked = useLockStore(state => state.locked)
  const unlock = useLockStore(state => state.unlock)
  const user = useAuthStore(state => state.user)
  const clearSession = useAuthStore(state => state.clearSession)
  const clearRoutes = useRouteStore(state => state.clearRoutes)
  const resetTabs = usePageTabsStore(state => state.resetTabs)
  const siteLogo = usePublicConfigStore(state => state.config.system.siteLogo)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [shaking, setShaking] = useState(false)
  const [clock, setClock] = useState(currentClock)
  const dialogRef = useRef<HTMLDivElement>(null)
  const passwordRef = useRef<React.ElementRef<typeof Input.Password>>(null)
  const nickname = user.nickname || user.email || '用户'

  useEffect(() => {
    if (!locked) return
    const shell = document.querySelector<HTMLElement>('.app-shell')
    const previousOverflow = document.body.style.overflow
    shell?.setAttribute('inert', '')
    document.body.style.overflow = 'hidden'
    const focusTimer = window.setTimeout(() => passwordRef.current?.focus(), 0)
    return () => {
      window.clearTimeout(focusTimer)
      shell?.removeAttribute('inert')
      document.body.style.overflow = previousOverflow
    }
  }, [locked])

  useEffect(() => {
    if (!locked) return
    setClock(currentClock())
    const timer = window.setInterval(() => setClock(currentClock()), 1000)
    return () => window.clearInterval(timer)
  }, [locked])

  if (!locked) return null

  const trapFocus = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'Tab') return
    const focusable = dialogRef.current?.querySelectorAll<HTMLElement>('input, button, [href], [tabindex]:not([tabindex="-1"])')
    if (!focusable?.length) return
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  const handleUnlock = async () => {
    if (!password) {
      setError('请输入密码')
      passwordRef.current?.focus()
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const response = await verifyPasswordApi(password)
      if (!response.data) {
        setError('密码错误')
        setShaking(true)
        window.setTimeout(() => setShaking(false), 420)
        return
      }
      setPassword('')
      unlock()
      message.success('屏幕已解锁')
    } catch {
      setError('验证失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleLogout = async () => {
    try { await logoutApi() } catch { /* Local cleanup still runs. */ }
    clearSession()
    clearRoutes()
    resetTabs()
    navigate('/login', { replace: true })
  }

  return (
    <div ref={dialogRef} className="lock-screen" role="dialog" aria-modal="true" aria-labelledby="lock-screen-title" onKeyDown={trapFocus}>
      <div className="lock-screen-shade" aria-hidden="true" />
      <header className="lock-screen-brand"><BrandMark size={30} src={siteLogo || undefined} /><strong>NEXORA ADMIN</strong></header>
      <main className="lock-screen-panel">
        <div className="lock-screen-clock" aria-label={`${clock.date} ${clock.time}`}><time>{clock.time}</time><span>{clock.date}</span></div>
        <div className="lock-screen-identity">
          <span className="lock-screen-avatar">{user.avatar ? <img src={user.avatar} alt={`${nickname}的头像`} /> : nickname.slice(0, 1).toUpperCase()}</span>
          <div><h1 id="lock-screen-title">欢迎回来，{nickname}</h1><p>输入当前账户密码以继续</p></div>
        </div>
        <form className={shaking ? 'lock-screen-form is-shaking' : 'lock-screen-form'} onSubmit={event => { event.preventDefault(); void handleUnlock() }}>
          <label htmlFor="lock-password">登录密码</label>
          <div className="lock-screen-input-row"><Input.Password ref={passwordRef} id="lock-password" name="lock-password" value={password} status={error ? 'error' : undefined} autoComplete="current-password" prefix={<LockOutlined />} placeholder="请输入密码" onChange={event => { setPassword(event.target.value); setError('') }} /><Button type="primary" htmlType="submit" icon={<UnlockOutlined />} loading={submitting}>解锁</Button></div>
          <span className="lock-screen-error" role="alert" aria-live="assertive">{error}</span>
        </form>
        <Button type="text" className="lock-screen-logout" icon={<LogoutOutlined />} onClick={() => void handleLogout()}>退出当前账户</Button>
      </main>
    </div>
  )
}
