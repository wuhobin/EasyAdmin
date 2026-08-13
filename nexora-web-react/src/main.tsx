import { StrictMode, useEffect, type ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import ConfigProvider from 'antd/es/config-provider'
import antTheme from 'antd/es/theme'
import zhCN from 'antd/locale/zh_CN'
import { BrowserRouter } from 'react-router-dom'
import { App } from '@/app/App'
import { registerUnauthorizedHandler } from '@/utils/auth-session'
import { useAuthStore } from '@/store/authStore'
import { useRouteStore } from '@/store/routeStore'
import { usePageTabsStore } from '@/store/pageTabsStore'
import { useSettingsStore } from '@/store/settingsStore'
import '@/styles/global.css'
import '@/styles/shadcn.css'
import '@/styles/auth.css'
import '@/styles/shell.css'
import '@/styles/menu.css'
import '@/styles/management.css'
import '@/styles/config.css'
import '@/styles/lock.css'
import '@/styles/notice.css'
import '@/styles/monitor.css'
import '@/styles/mail.css'
import '@/styles/file.css'
import '@/styles/profile.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { refetchOnWindowFocus: false }
  }
})

function AuthSessionBridge() {
  const { modal } = AntApp.useApp()
  useEffect(() => {
    registerUnauthorizedHandler(async () => {
      useAuthStore.getState().clearSession()
      useRouteStore.getState().clearRoutes()
      usePageTabsStore.getState().resetTabs()
      queryClient.removeQueries({ queryKey: ['notice-unread-count'] })
      queryClient.removeQueries({ queryKey: ['my-notices'] })
      queryClient.removeQueries({ queryKey: ['pending-announcements'] })
      await modal.warning({ title: '登录状态已失效', content: '请重新登录后继续使用工作台。', okText: '重新登录' })
      window.location.assign('/login')
    })
  }, [modal])
  return null
}

function NexoraConfigProvider({ children }: { children: ReactNode }) {
  const themeMode = useSettingsStore(state => state.theme)
  const dark = themeMode === 'dark'
  const palette = dark
    ? {
        primary: '#9d83ff',
        surface: '#15131e',
        panel: '#201d2b',
        muted: '#292535',
        text: '#f8f7fb',
        textSecondary: '#b8b2c5',
        placeholder: '#817a91',
        border: '#3a3449'
      }
    : {
        primary: '#6c3ff5',
        surface: '#fcfbfe',
        panel: '#ffffff',
        muted: '#f2eff6',
        text: '#191728',
        textSecondary: '#4e4a5d',
        placeholder: '#b5b0be',
        border: '#e6e2ec'
      }

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: dark ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
        token: {
          colorPrimary: palette.primary,
          colorBgBase: palette.surface,
          colorBgContainer: palette.panel,
          colorBgElevated: palette.panel,
          colorFillSecondary: palette.muted,
          colorText: palette.text,
          colorTextSecondary: palette.textSecondary,
          colorTextPlaceholder: palette.placeholder,
          colorBorder: palette.border,
          colorBorderSecondary: palette.border,
          borderRadius: 10,
          fontFamily: 'Inter, "Noto Sans SC", "PingFang SC", sans-serif'
        },
        components: {
          Modal: {
            headerBg: palette.panel,
            contentBg: palette.panel,
            footerBg: palette.panel,
            titleColor: palette.text
          }
        }
      }}
    >
      {children}
    </ConfigProvider>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <NexoraConfigProvider>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
            <AuthSessionBridge />
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </AntApp>
    </NexoraConfigProvider>
  </StrictMode>
)
