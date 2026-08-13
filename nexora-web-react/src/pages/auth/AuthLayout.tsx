import { MoonOutlined, SunOutlined } from '@ant-design/icons'
import { useEffect, type ReactNode } from 'react'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { useSettingsStore } from '@/store/settingsStore'
import { BrandMark } from '@/components/BrandMark'
import { AnimatedCharacters } from '@/pages/auth/AnimatedCharacters'

export interface AuthAnimationState {
  isTyping?: boolean
  isPasswordFocused?: boolean
  showPassword?: boolean
  passwordLength?: number
  isSubmitting?: boolean
  hasError?: boolean
}

interface AuthLayoutProps {
  children: ReactNode
  animationState?: AuthAnimationState
}

export function AuthLayout({ children, animationState }: AuthLayoutProps) {
  const config = usePublicConfigStore(state => state.config)
  const loadStatus = usePublicConfigStore(state => state.status)
  const toggleTheme = useSettingsStore(state => state.toggleTheme)
  const theme = useSettingsStore(state => state.theme)

  useEffect(() => {
    if (loadStatus === 'idle') void usePublicConfigStore.getState().load()
  }, [loadStatus])

  return (
    <div className="auth-page">
      <aside className="auth-visual-panel">
        <div className="auth-brand-lockup">
          <BrandMark size={30} src={config.system.siteLogo || undefined} />
          <span>{config.system.siteName}</span>
        </div>
        <div className="auth-character-wrap">
          <AnimatedCharacters {...animationState} />
        </div>
      </aside>

      <main className="auth-form-panel">
        <div className="auth-topbar">
          <div className="auth-mobile-brand"><BrandMark size={26} src={config.system.siteLogo || undefined} /><span>{config.system.siteName}</span></div>
          <button type="button" className="theme-toggle" onClick={toggleTheme} aria-label={theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'}>
            {theme === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          </button>
        </div>
        <div className="auth-form-card">{children}</div>
        <p className="auth-copyright">{config.system.copyright}</p>
      </main>
    </div>
  )
}
