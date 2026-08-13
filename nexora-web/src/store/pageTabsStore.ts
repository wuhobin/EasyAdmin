import { create } from 'zustand'
import { HOME_PATH, LEGACY_HOME_PATH } from '@/routes/routeAdapter'

export interface PageTab {
  path: string
  search?: string
  hash?: string
  title: string
  icon?: string
  closable: boolean
  revision: number
}

const PAGE_TABS_KEY = 'nexora-react-page-tabs'
const MAX_TABS = 30

export const homeTab: PageTab = {
  path: HOME_PATH,
  title: '工作台',
  icon: 'antd:HomeOutlined',
  closable: false,
  revision: 0
}

function tabKey(tab: Pick<PageTab, 'path' | 'search' | 'hash'>) {
  return tab.path
}

function persistTabs(tabs: PageTab[]) {
  try {
    sessionStorage.setItem(PAGE_TABS_KEY, JSON.stringify(tabs))
  } catch {
    // Storage can be unavailable in private browsing or restricted environments.
  }
}

function readTabs(): PageTab[] {
  try {
    const raw = sessionStorage.getItem(PAGE_TABS_KEY)
    if (!raw) return [homeTab]
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return [homeTab]

    const valid = parsed.filter((value): value is PageTab => {
      if (!value || typeof value !== 'object') return false
      const tab = value as Partial<PageTab>
      return typeof tab.path === 'string' && tab.path.startsWith('/') && typeof tab.title === 'string'
    }).map(tab => ({
      ...tab,
      closable: tab.path !== HOME_PATH,
      revision: 0
    }))

    const unique = valid.filter((tab, index, all) => all.findIndex(item => tabKey(item) === tabKey(tab)) === index)
    return [homeTab, ...unique.filter(tab => tab.path !== HOME_PATH && tab.path !== LEGACY_HOME_PATH)].slice(0, MAX_TABS)
  } catch {
    return [homeTab]
  }
}

function save(tabs: PageTab[]) {
  persistTabs(tabs)
  return tabs
}

interface PageTabsState {
  tabs: PageTab[]
  openTab: (tab: Omit<PageTab, 'revision'> & { revision?: number }) => void
  closeTab: (key: string) => void
  closeOthers: (key: string) => void
  closeLeft: (key: string) => void
  closeRight: (key: string) => void
  closeAll: () => void
  refreshTab: (key: string) => void
  resetTabs: () => void
}

export const usePageTabsStore = create<PageTabsState>((set, get) => ({
  tabs: readTabs(),
  openTab: tab => set(state => {
    const key = tabKey(tab)
    const existing = state.tabs.find(item => tabKey(item) === key)
    if (existing) {
      const next = state.tabs.map(item => item === existing ? {
        ...item,
        search: tab.search,
        hash: tab.hash,
        title: tab.title,
        icon: tab.icon,
        closable: tab.path !== HOME_PATH
      } : item)
      return { tabs: save(next) }
    }

    const nextTab: PageTab = {
      ...tab,
      closable: tab.path !== HOME_PATH,
      revision: tab.revision ?? 0
    }
    const next = [...state.tabs, nextTab]
    if (next.length > MAX_TABS) next.splice(1, next.length - MAX_TABS)
    return { tabs: save(next) }
  }),
  closeTab: key => set(state => ({ tabs: save(state.tabs.filter(tab => tabKey(tab) !== key || !tab.closable)) })),
  closeOthers: key => set(state => ({ tabs: save(state.tabs.filter(tab => !tab.closable || tabKey(tab) === key)) })),
  closeLeft: key => set(state => {
    const index = state.tabs.findIndex(tab => tabKey(tab) === key)
    if (index < 0) return state
    return { tabs: save(state.tabs.filter((tab, tabIndex) => !tab.closable || tabIndex >= index)) }
  }),
  closeRight: key => set(state => {
    const index = state.tabs.findIndex(tab => tabKey(tab) === key)
    if (index < 0) return state
    return { tabs: save(state.tabs.filter((tab, tabIndex) => !tab.closable || tabIndex <= index)) }
  }),
  closeAll: () => set(() => ({ tabs: save([homeTab]) })),
  refreshTab: key => set(state => ({ tabs: save(state.tabs.map(tab => tabKey(tab) === key ? { ...tab, revision: tab.revision + 1 } : tab)) })),
  resetTabs: () => {
    try { sessionStorage.removeItem(PAGE_TABS_KEY) } catch { /* Storage can be unavailable. */ }
    set({ tabs: [homeTab] })
  }
}))

export function getTabKey(tab: Pick<PageTab, 'path' | 'search' | 'hash'>) {
  return tabKey(tab)
}
