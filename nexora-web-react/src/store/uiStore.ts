import { create } from 'zustand'

interface UiState {
  sidebarCollapsed: boolean
  mobileSidebarOpen: boolean
  setSidebarCollapsed: (value: boolean) => void
  toggleSidebar: () => void
  setMobileSidebarOpen: (value: boolean) => void
}

export const useUiStore = create<UiState>(set => ({
  sidebarCollapsed: false,
  mobileSidebarOpen: false,
  setSidebarCollapsed: value => set({ sidebarCollapsed: value }),
  toggleSidebar: () => set(state => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  setMobileSidebarOpen: value => set({ mobileSidebarOpen: value })
}))
