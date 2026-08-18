import { LockOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons'
import AntApp from 'antd/es/app'
import Dropdown from 'antd/es/dropdown'
import { useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { logoutApi } from '@/api/auth'
import { BrandMark } from '@/components/BrandMark'
import { MenuIcon } from '@/components/MenuIcon'
import {
  Sidebar as ShadcnSidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarRail,
  useSidebar
} from '@/components/ui/sidebar'
import { PROFILE_PATH, type MenuRoute } from '@/routes/routeAdapter'
import { useAuthStore } from '@/store/authStore'
import { usePageTabsStore } from '@/store/pageTabsStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { useRouteStore } from '@/store/routeStore'
import { useUiStore } from '@/store/uiStore'
import { useLockStore } from '@/store/lockStore'
import { isExternalPath } from '@/utils/routeNavigation'

const COLLAPSED_GROUPS_KEY = 'nexora-react-collapsed-groups'

function readCollapsedGroups() {
  try {
    const values = JSON.parse(sessionStorage.getItem(COLLAPSED_GROUPS_KEY) || '[]')
    return new Set<string>(Array.isArray(values) ? values : [])
  } catch {
    return new Set<string>()
  }
}

function menuRoutes(routes: MenuRoute[]) {
  return routes.flatMap(route => route.fullPath === '/' ? route.children : [route]).filter(route => !route.meta.hidden)
}

function MenuItem({ route, depth = 0, activePath, collapsed }: { route: MenuRoute; depth?: number; activePath: string; collapsed: boolean }) {
  const [open, setOpen] = useState(() => !readCollapsedGroups().has(route.fullPath))
  const children = route.children.filter(child => !child.meta.hidden)
  const hasChildren = children.length > 0
  // A branch remains open for descendant routes, but only the active leaf gets the selected style.
  const selected = activePath === route.fullPath || (!hasChildren && activePath.startsWith(`${route.fullPath}/`))

  const toggleBranch = () => {
    const next = !open
    setOpen(next)
    const groups = readCollapsedGroups()
    if (next) groups.delete(route.fullPath)
    else groups.add(route.fullPath)
    sessionStorage.setItem(COLLAPSED_GROUPS_KEY, JSON.stringify([...groups]))
  }

  const content = <><MenuIcon value={route.meta.icon} className="nav-item-icon" />{!collapsed ? <span className="nav-item-title">{route.meta.title}</span> : null}{!collapsed && hasChildren ? <span className={`nav-item-chevron ${open ? 'is-open' : ''}`} aria-hidden="true">⌄</span> : null}</>
  const commonProps = { isActive: selected, tooltip: collapsed ? route.meta.title : undefined, title: collapsed ? route.meta.title : undefined, 'aria-current': activePath === route.fullPath ? 'page' as const : undefined }

  return (
    <SidebarMenuItem className={`depth-${depth} ${selected ? 'is-selected' : ''}`}>
      {hasChildren && !collapsed
        ? <SidebarMenuButton {...commonProps} aria-expanded={open} aria-controls={`sidebar-group-${route.id}`} onClick={toggleBranch}>{content}</SidebarMenuButton>
        : <SidebarMenuButton {...commonProps} asChild>{route.meta.isExternal || isExternalPath(route.fullPath) ? <a href={route.fullPath} target="_blank" rel="noreferrer">{content}</a> : <Link to={route.fullPath}>{content}</Link>}</SidebarMenuButton>}
      {hasChildren && open && !collapsed ? <SidebarMenuSub id={`sidebar-group-${route.id}`}>{children.map(child => <MenuItem key={`${child.id}-${child.fullPath}`} route={child} depth={depth + 1} activePath={activePath} collapsed={collapsed} />)}</SidebarMenuSub> : null}
    </SidebarMenuItem>
  )
}

function SidebarUser() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const { state } = useSidebar()
  const user = useAuthStore(store => store.user)
  const clearSession = useAuthStore(store => store.clearSession)
  const clearRoutes = useRouteStore(store => store.clearRoutes)
  const resetTabs = usePageTabsStore(store => store.resetTabs)
  const lock = useLockStore(store => store.lock)
  const nickname = user.nickname || user.email || 'Nexora 用户'
  const collapsed = state === 'collapsed'

  const handleLogout = async () => {
    try { await logoutApi() } catch { /* Local cleanup still runs. */ }
    clearSession()
    clearRoutes()
    resetTabs()
    queryClient.removeQueries({ queryKey: ['notice-unread-count'] })
    queryClient.removeQueries({ queryKey: ['my-notices'] })
    queryClient.removeQueries({ queryKey: ['pending-announcements'] })
    navigate('/login', { replace: true })
    message.success('已安全退出')
  }

  const items = [
    { key: 'profile', icon: <UserOutlined />, label: <Link to={PROFILE_PATH}>个人中心</Link> },
    { key: 'lock', icon: <LockOutlined />, label: '锁定屏幕', onClick: lock },
    { type: 'divider' as const },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: () => void handleLogout() }
  ]

  return (
    <Dropdown
      menu={{ items }}
      placement="topLeft"
      trigger={['click']}
      overlayClassName={`sidebar-account-dropdown ${collapsed ? 'sidebar-account-dropdown-collapsed' : ''}`.trim()}
      popupRender={menu => (
        <div className="sidebar-account-popover">
          <div className="sidebar-account-summary">
            <span className="sidebar-account-avatar">{user.avatar ? <img src={user.avatar} alt="" width={32} height={32} /> : nickname.slice(0, 1).toUpperCase()}</span>
            <span className="sidebar-account-summary-copy"><strong>{nickname}</strong><small>{user.email || '当前登录账户'}</small></span>
          </div>
          <div className="sidebar-account-divider" />
          {menu}
        </div>
      )}
    >
      <button className="sidebar-user" type="button" aria-label={`${nickname}，打开账户菜单`} title={collapsed ? nickname : undefined}>
        <span className="sidebar-user-avatar">{user.avatar ? <img src={user.avatar} alt="" width={34} height={34} /> : nickname.slice(0, 1).toUpperCase()}</span>
        {!collapsed ? <span className="sidebar-user-copy"><strong>{nickname}</strong><small>{user.email || '当前登录账户'}</small></span> : null}
        {!collapsed ? <span className="sidebar-user-chevron">⌄</span> : null}
      </button>
    </Dropdown>
  )
}

export function Sidebar() {
  const routes = useRouteStore(state => state.routes)
  const { state } = useSidebar()
  const collapsed = state === 'collapsed'
  const setMobileOpen = useUiStore(state => state.setMobileSidebarOpen)
  const siteLogo = usePublicConfigStore(state => state.config.system.siteLogo)
  const location = useLocation()
  const visibleRoutes = useMemo(() => menuRoutes(routes), [routes])

  return (
    <ShadcnSidebar side="left" variant="sidebar" collapsible="icon">
      <SidebarHeader className="sidebar-brand">
        <BrandMark size={32} src={siteLogo || undefined} />
        {!collapsed ? <div className="sidebar-wordmark"><strong>NEXORA</strong><span>ADMIN</span></div> : null}
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu aria-label="主导航">
              {visibleRoutes.map(route => <MenuItem key={`${route.id}-${route.fullPath}`} route={route} activePath={location.pathname} collapsed={collapsed} />)}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>
        <SidebarUser />
        <button type="button" className="sidebar-mobile-close" onClick={() => setMobileOpen(false)}>关闭导航</button>
      </SidebarFooter>
      <SidebarRail />
    </ShadcnSidebar>
  )
}
