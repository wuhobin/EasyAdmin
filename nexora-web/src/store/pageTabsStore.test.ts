import { beforeEach, describe, expect, it } from 'vitest'
import { getTabKey, homeTab, usePageTabsStore } from '@/store/pageTabsStore'

function open(path: string, title: string) {
  usePageTabsStore.getState().openTab({ path, title, closable: true })
}

describe('pageTabsStore', () => {
  beforeEach(() => usePageTabsStore.getState().resetTabs())

  it('keeps home fixed, deduplicates tabs, and refreshes by revision', () => {
    open('/system/menu', '菜单管理')
    open('/system/menu', '菜单配置')

    const key = getTabKey({ path: '/system/menu' })
    expect(usePageTabsStore.getState().tabs.map(tab => tab.title)).toEqual(['工作台', '菜单配置'])

    usePageTabsStore.getState().refreshTab(key)
    expect(usePageTabsStore.getState().tabs[1].revision).toBe(1)

    usePageTabsStore.getState().closeTab(getTabKey(homeTab))
    expect(usePageTabsStore.getState().tabs[0].path).toBe('/home')
  })

  it('supports closing tabs on either side, other tabs, and all tabs', () => {
    open('/a', 'A')
    open('/b', 'B')
    open('/c', 'C')

    usePageTabsStore.getState().closeLeft('/b')
    expect(usePageTabsStore.getState().tabs.map(tab => tab.path)).toEqual(['/home', '/b', '/c'])

    usePageTabsStore.getState().closeRight('/b')
    expect(usePageTabsStore.getState().tabs.map(tab => tab.path)).toEqual(['/home', '/b'])

    open('/c', 'C')
    usePageTabsStore.getState().closeOthers('/c')
    expect(usePageTabsStore.getState().tabs.map(tab => tab.path)).toEqual(['/home', '/c'])

    usePageTabsStore.getState().closeAll()
    expect(usePageTabsStore.getState().tabs).toEqual([homeTab])
  })
})
