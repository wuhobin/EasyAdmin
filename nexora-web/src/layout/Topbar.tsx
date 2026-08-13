import { CompressOutlined, ExpandOutlined, MenuOutlined, SettingOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { MenuIcon } from '@/components/MenuIcon'
import { AppearanceSettings } from '@/components/AppearanceSettings'
import { NotificationCenter } from '@/components/notifications/NotificationCenter'
import { SidebarTrigger, useSidebar } from '@/components/ui/sidebar'
import { findRouteTrail, HOME_PATH } from '@/routes/routeAdapter'
import { useRouteStore } from '@/store/routeStore'
import { useUiStore } from '@/store/uiStore'

export function Topbar() {
  const location = useLocation()
  const navigate = useNavigate()
  const routes = useRouteStore(state => state.routes)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [fullscreen, setFullscreen] = useState(Boolean(document.fullscreenElement))
  const setMobileOpen = useUiStore(state => state.setMobileSidebarOpen)
  const { state: sidebarState } = useSidebar()
  useEffect(() => {
    const syncFullscreen = () => setFullscreen(Boolean(document.fullscreenElement))
    document.addEventListener('fullscreenchange', syncFullscreen)
    return () => document.removeEventListener('fullscreenchange', syncFullscreen)
  }, [])

  const toggleFullscreen = async () => {
    try {
      if (document.fullscreenElement) await document.exitFullscreen()
      else await document.documentElement.requestFullscreen()
    } catch {
      // Browsers can reject fullscreen when it is unavailable or restricted.
    }
  }
  const crumbs = useMemo(() => {
    const trail = findRouteTrail(routes, location.pathname).filter(route => route.fullPath !== '/')
    const menuCrumbs = trail
      .filter(route => route.fullPath !== HOME_PATH)
      .map(route => ({ path: route.fullPath, title: route.meta.title, icon: route.meta.icon }))
    const home = { path: HOME_PATH, title: '工作台', icon: 'antd:HomeOutlined' }
    return menuCrumbs.length || location.pathname === HOME_PATH
      ? [home, ...menuCrumbs]
      : [home, { path: location.pathname, title: '页面', icon: '' }]
  }, [location.pathname, routes])

  return (
    <header className="app-topbar">
      <div className="topbar-left">
        <SidebarTrigger className="sidebar-trigger" aria-label={sidebarState === 'collapsed' ? '展开导航' : '收起导航'} title={sidebarState === 'collapsed' ? '展开导航' : '收起导航'} />
        <button className="mobile-menu-trigger" type="button" aria-label="打开导航" onClick={() => setMobileOpen(true)}><MenuOutlined /></button>
        <nav className="breadcrumb" aria-label="面包屑">
          <ol className="breadcrumb-list">
            {crumbs.map((crumb, index) => {
              const isCurrent = index === crumbs.length - 1
              return (
                <li className="breadcrumb-item" key={`${crumb.path}-${crumb.title}`}>
                  {index > 0 ? <i aria-hidden="true">/</i> : null}
                  {isCurrent
                    ? <span className="breadcrumb-current" aria-current="page"><MenuIcon value={crumb.icon} />{crumb.title}</span>
                    : <button className="breadcrumb-link" type="button" onClick={() => navigate(crumb.path)}><MenuIcon value={crumb.icon} />{crumb.title}</button>}
                </li>
              )
            })}
          </ol>
        </nav>
      </div>
      <div className="topbar-actions">
        <NotificationCenter />
        <button className="topbar-icon-button" type="button" onClick={() => setSettingsOpen(true)} aria-label="打开外观设置" title="外观设置"><SettingOutlined /></button>
        <button className="topbar-icon-button topbar-fullscreen-button" type="button" onClick={() => void toggleFullscreen()} aria-label={fullscreen ? '退出全屏' : '进入全屏'} title={fullscreen ? '退出全屏' : '进入全屏'}>{fullscreen ? <CompressOutlined /> : <ExpandOutlined />}</button>
      </div>
      <AppearanceSettings open={settingsOpen} onOpenChange={setSettingsOpen} />
    </header>
  )
}
