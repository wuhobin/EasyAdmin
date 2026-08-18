import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CloseCircleOutlined,
  CloseOutlined,
  DeleteOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import Dropdown from 'antd/es/dropdown'
import type { MenuProps } from 'antd/es/menu'
import { useEffect, useMemo, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { flattenRoutes, isPageTabRoute } from '@/routes/routeAdapter'
import { usePageTabsStore, getTabKey, homeTab, type PageTab } from '@/store/pageTabsStore'
import { useRouteStore } from '@/store/routeStore'
import { MenuIcon } from '@/components/MenuIcon'

function tabHref(tab: PageTab) {
  return `${tab.path}${tab.search || ''}${tab.hash || ''}`
}

function isSameTab(tab: PageTab, key: string) {
  return getTabKey(tab) === key
}

export function TabsBar() {
  const location = useLocation()
  const navigate = useNavigate()
  const routes = useRouteStore(state => state.routes)
  const tabs = usePageTabsStore(state => state.tabs)
  const openTab = usePageTabsStore(state => state.openTab)
  const closeTab = usePageTabsStore(state => state.closeTab)
  const closeOthers = usePageTabsStore(state => state.closeOthers)
  const closeLeft = usePageTabsStore(state => state.closeLeft)
  const closeRight = usePageTabsStore(state => state.closeRight)
  const closeAll = usePageTabsStore(state => state.closeAll)
  const refreshTab = usePageTabsStore(state => state.refreshTab)
  const scrollRef = useRef<HTMLDivElement>(null)
  const currentKey = getTabKey({ path: location.pathname, search: location.search, hash: location.hash })
  const routeByPath = useMemo(() => new Map(flattenRoutes(routes).map(route => [route.fullPath, route])), [routes])
  const currentRoute = routeByPath.get(location.pathname)

  useEffect(() => {
    if (location.pathname === homeTab.path) {
      openTab({ ...homeTab })
      return
    }
    if (!isPageTabRoute(currentRoute)) return
    openTab({
      path: location.pathname,
      search: location.search,
      hash: location.hash,
      title: currentRoute.meta.title,
      icon: currentRoute.meta.icon,
      closable: true
    })
  }, [currentRoute, location.hash, location.pathname, location.search, openTab])

  useEffect(() => {
    for (const tab of tabs) {
      const route = routeByPath.get(tab.path)
      if (tab.closable && route && !isPageTabRoute(route)) closeTab(getTabKey(tab))
    }
  }, [closeTab, routeByPath, tabs])

  useEffect(() => {
    const active = Array.from(scrollRef.current?.querySelectorAll<HTMLElement>('[data-tab-key]') || [])
      .find(element => element.dataset.tabKey === currentKey)
    active?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'nearest', inline: 'nearest' })
  }, [currentKey, tabs])

  const tabContextItems = (tab: PageTab): MenuProps['items'] => {
    const index = tabs.findIndex(item => isSameTab(item, getTabKey(tab)))
    const hasLeftTabs = tabs.slice(0, index).some(item => item.closable)
    const hasRightTabs = tabs.slice(index + 1).some(item => item.closable)
    const hasOtherTabs = tabs.some(item => item.closable && !isSameTab(item, getTabKey(tab)))
    const canClose = tab.closable
    return [
      { key: 'refresh', icon: <ReloadOutlined />, label: '刷新页面' },
      { key: 'close', icon: <CloseOutlined />, label: '关闭当前', disabled: !canClose },
      { type: 'divider' as const },
      { key: 'close-left', icon: <ArrowLeftOutlined />, label: '关闭左侧', disabled: !hasLeftTabs },
      { key: 'close-right', icon: <ArrowRightOutlined />, label: '关闭右侧', disabled: !hasRightTabs },
      { key: 'close-others', icon: <CloseCircleOutlined />, label: '关闭其他', disabled: !hasOtherTabs },
      { key: 'close-all', icon: <DeleteOutlined />, label: '关闭全部', disabled: !tabs.some(item => item.closable) }
    ]
  }

  const goAfterClosing = (closedTab: PageTab) => {
    const index = tabs.findIndex(item => isSameTab(item, getTabKey(closedTab)))
    const remaining = tabs.filter(item => !isSameTab(item, getTabKey(closedTab)))
    const target = remaining[Math.min(Math.max(index, 0), remaining.length - 1)] || homeTab
    navigate(tabHref(target))
  }

  const runAction = (action: string, tab: PageTab) => {
    const key = getTabKey(tab)
    if (action === 'refresh') {
      refreshTab(key)
      if (currentKey !== key) navigate(tabHref(tab))
    } else if (action === 'close') {
      if (!tab.closable) return
      closeTab(key)
      if (currentKey === key) goAfterClosing(tab)
    } else if (action === 'close-left') {
      closeLeft(key)
      if (currentKey !== key) navigate(tabHref(tab))
    } else if (action === 'close-right') {
      closeRight(key)
      if (currentKey !== key) navigate(tabHref(tab))
    } else if (action === 'close-others') {
      closeOthers(key)
      if (currentKey !== key) navigate(tabHref(tab))
    } else if (action === 'close-all') {
      closeAll()
      if (currentKey !== homeTab.path) navigate(homeTab.path)
    }
  }

  return (
    <nav className="app-tabs" aria-label="页面标签">
      <div className="app-tabs-scroll" ref={scrollRef} onWheel={event => {
        if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
          event.currentTarget.scrollLeft += event.deltaY
          event.preventDefault()
        }
      }}>
        {tabs.map(tab => {
          const key = getTabKey(tab)
          const active = key === currentKey
          return (
            <Dropdown key={`${key}:${tab.revision}`} trigger={['contextMenu']} menu={{ items: tabContextItems(tab), onClick: info => runAction(info.key, tab) }}>
              <div className={`app-tab ${active ? 'is-active' : ''}`} data-tab-key={key}>
                <Link className="app-tab-main" to={tabHref(tab)} aria-current={active ? 'page' : undefined}>
                  <MenuIcon value={tab.icon} />
                  <span className="app-tab-label" title={tab.title}>{tab.title}</span>
                </Link>
                {tab.closable ? <button className="app-tab-close" type="button" aria-label={`关闭${tab.title}`} onClick={() => {
                  closeTab(key)
                  if (active) goAfterClosing(tab)
                }}><CloseOutlined /></button> : null}
              </div>
            </Dropdown>
          )
        })}
      </div>
    </nav>
  )
}
