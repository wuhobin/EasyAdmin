import { create } from 'zustand'
import type { BackendRoute } from '@/api/auth'
import { createAppRouteTree, type MenuRoute } from '@/routes/routeAdapter'

interface RouteState {
  routes: MenuRoute[]
  ready: boolean
  setRoutes: (routes: BackendRoute[]) => void
  clearRoutes: () => void
}

export const useRouteStore = create<RouteState>(set => ({
  routes: [],
  ready: false,
  setRoutes: backendRoutes => set({ routes: createAppRouteTree(backendRoutes), ready: true }),
  clearRoutes: () => set({ routes: [], ready: false })
}))
