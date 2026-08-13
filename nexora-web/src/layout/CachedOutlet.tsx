import ConfigProvider from 'antd/es/config-provider'
import { useRef, type ReactNode } from 'react'
import { useLocation, useOutlet } from 'react-router-dom'
import { getTabKey, usePageTabsStore } from '@/store/pageTabsStore'

interface CachedPage {
  revision: number
  element: ReactNode
}

function CachedPageSlot({ page, active }: { page: CachedPage; active: boolean }) {
  const containerRef = useRef<HTMLDivElement>(null)
  return (
    <div ref={containerRef} className="app-page-cache" hidden={!active} aria-hidden={!active}>
      <ConfigProvider getPopupContainer={() => containerRef.current || document.body}>
        {page.element}
      </ConfigProvider>
    </div>
  )
}

export function CachedOutlet() {
  const location = useLocation()
  const outlet = useOutlet()
  const tabs = usePageTabsStore(state => state.tabs)
  const cacheRef = useRef(new Map<string, CachedPage>())
  const currentKey = getTabKey({ path: location.pathname, search: location.search, hash: location.hash })
  const currentTab = tabs.find(tab => getTabKey(tab) === currentKey)
  const revision = currentTab?.revision ?? 0

  if (outlet) {
    const cached = cacheRef.current.get(currentKey)
    if (!cached || cached.revision !== revision) {
      cacheRef.current.set(currentKey, { revision, element: outlet })
    }
  }

  const activeKeys = new Set(tabs.map(getTabKey))
  for (const key of cacheRef.current.keys()) {
    if (!activeKeys.has(key) && key !== currentKey) cacheRef.current.delete(key)
  }

  return (
    <>
      {Array.from(cacheRef.current.entries()).map(([key, cached]) => (
        <CachedPageSlot key={`${key}:${cached.revision}`} page={cached} active={key === currentKey} />
      ))}
    </>
  )
}
