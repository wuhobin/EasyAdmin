import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { Sidebar } from '@/layout/Sidebar'
import { Topbar } from '@/layout/Topbar'
import { TabsBar } from '@/layout/TabsBar'
import { CachedOutlet } from '@/layout/CachedOutlet'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ContentWatermark } from '@/components/ContentWatermark'
import { LockScreen } from '@/components/LockScreen'
import { AnnouncementPrompt } from '@/components/notifications/AnnouncementPrompt'
import { useUiStore } from '@/store/uiStore'

export function AppShell() {
  const location = useLocation()
  const collapsed = useUiStore(state => state.sidebarCollapsed)
  const mobileOpen = useUiStore(state => state.mobileSidebarOpen)
  const setSidebarCollapsed = useUiStore(state => state.setSidebarCollapsed)
  const setMobileOpen = useUiStore(state => state.setMobileSidebarOpen)

  useEffect(() => setMobileOpen(false), [location.pathname, setMobileOpen])

  return (
    <SidebarProvider open={!collapsed} onOpenChange={open => setSidebarCollapsed(!open)}>
      <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : ''} ${mobileOpen ? 'mobile-sidebar-open' : ''}`}>
        <Sidebar />
        <button className="mobile-sidebar-scrim" type="button" aria-label="关闭导航" onClick={() => setMobileOpen(false)} />
        <div className="app-main">
          <Topbar />
          <TabsBar />
          <main className="app-content"><ContentWatermark><CachedOutlet /></ContentWatermark></main>
        </div>
      </div>
      <AnnouncementPrompt />
      <LockScreen />
    </SidebarProvider>
  )
}
