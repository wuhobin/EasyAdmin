import { create } from 'zustand'
import { DEFAULT_PUBLIC_CONFIG, getPublicConfigApi, type PublicConfig } from '@/api/config'

type LoadStatus = 'idle' | 'loading' | 'success' | 'error'

interface PublicConfigState {
  config: PublicConfig
  status: LoadStatus
  load: (force?: boolean) => Promise<boolean>
}

let pendingLoad: Promise<boolean> | undefined

export const usePublicConfigStore = create<PublicConfigState>((set, get) => ({
  config: structuredClone(DEFAULT_PUBLIC_CONFIG),
  status: 'idle',
  load: async (force = false) => {
    if (get().status === 'success' && !force) return true
    if (pendingLoad) return pendingLoad
    pendingLoad = (async () => {
      set({ status: 'loading' })
      try {
        const { data } = await getPublicConfigApi()
        set({ config: data, status: 'success' })
        document.title = data.system.shortTitle || data.system.siteName
        return true
      } catch {
        set({ status: 'error' })
        document.title = get().config.system.shortTitle || get().config.system.siteName
        return false
      } finally {
        pendingLoad = undefined
      }
    })()
    return pendingLoad
  }
}))
