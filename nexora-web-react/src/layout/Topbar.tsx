import { MenuOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons'
import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { MenuIcon } from '@/components/MenuIcon'
import { NotificationCenter } from '@/components/notifications/NotificationCenter'
import { SidebarTrigger, useSidebar } from '@/components/ui/sidebar'
import { findRouteTrail, HOME_PATH } from '@/routes/routeAdapter'
import { useRouteStore } from '@/store/routeStore'
import { useSettingsStore } from '@/store/settingsStore'
import { useUiStore } from '@/store/uiStore'

export function Topbar() {
  const location = useLocation()
  const navigate = useNavigate()
  const routes = useRouteStore(state => state.routes)
  const theme = useSettingsStore(state => state.theme)
  const toggleTheme = useSettingsStore(state => state.toggleTheme)
  const setMobileOpen = useUiStore(state => state.setMobileSidebarOpen)
  const { state: sidebarState } = useSidebar()
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
        <button className="topbar-icon-button" type="button" onClick={toggleTheme} aria-label={theme === 'dark' ? '切换浅色模式' : '切换深色模式'}>{theme === 'dark' ? <SunOutlined /> : <MoonOutlined />}</button>
      </div>
    </header>
  )
}
