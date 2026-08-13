import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { Sidebar } from '@/layout/Sidebar'
import { Topbar } from '@/layout/Topbar'
import { TabsBar } from '@/layout/TabsBar'
import { CachedOutlet } from '@/layout/CachedOutlet'
import { AppFooter } from '@/layout/AppFooter'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ContentWatermark } from '@/components/ContentWatermark'
import { LockScreen } from '@/components/LockScreen'
import { AnnouncementPrompt } from '@/components/notifications/AnnouncementPrompt'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { useRouteStore } from '@/store/routeStore'
import { flattenRoutes } from '@/routes/routeAdapter'
import { useSettingsStore } from '@/store/settingsStore'
import { useUiStore } from '@/store/uiStore'

export function AppShell() {
  const location = useLocation()
  const collapsed = useUiStore(state => state.sidebarCollapsed)
  const mobileOpen = useUiStore(state => state.mobileSidebarOpen)
  const setSidebarCollapsed = useUiStore(state => state.setSidebarCollapsed)
  const setMobileOpen = useUiStore(state => state.setMobileSidebarOpen)
  const routes = useRouteStore(state => state.routes)
  const shortTitle = usePublicConfigStore(state => state.config.system.shortTitle)
  const siteName = usePublicConfigStore(state => state.config.system.siteName)
  const showTags = useSettingsStore(state => state.showTags)
  const showFooter = useSettingsStore(state => state.showFooter)
  const showLogo = useSettingsStore(state => state.showLogo)
  const dynamicTitle = useSettingsStore(state => state.dynamicTitle)
  const tagsStyle = useSettingsStore(state => state.tagsStyle)
  const pageAnimation = useSettingsStore(state => state.pageAnimation)

  useEffect(() => setMobileOpen(false), [location.pathname, setMobileOpen])

  useEffect(() => {
    const baseTitle = shortTitle || siteName || 'Nexora Admin'
    if (!dynamicTitle) { document.title = baseTitle; return }
    const current = flattenRoutes(routes).find(route => route.fullPath === location.pathname)
    document.title = current?.meta.title ? `${current.meta.title} - ${baseTitle}` : baseTitle
  }, [dynamicTitle, location.pathname, routes, shortTitle, siteName])

  return (
    <SidebarProvider open={!collapsed} onOpenChange={open => setSidebarCollapsed(!open)}>
      <a className="skip-link" href="#main-content">跳到主要内容</a>
      <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : ''} ${mobileOpen ? 'mobile-sidebar-open' : ''} ${showLogo ? '' : 'sidebar-logo-hidden'} tags-style-${tagsStyle} page-animation-${pageAnimation}`}>
        <Sidebar />
        <button className="mobile-sidebar-scrim" type="button" aria-label="关闭导航" onClick={() => setMobileOpen(false)} />
        <div className="app-main">
          <Topbar />
          <div className={showTags ? '' : 'app-tabs-hidden'}><TabsBar /></div>
          <main id="main-content" className="app-content" tabIndex={-1}><ContentWatermark><CachedOutlet /></ContentWatermark></main>
          {showFooter ? <AppFooter /> : null}
        </div>
      </div>
      <AnnouncementPrompt />
      <LockScreen />
    </SidebarProvider>
  )
}
