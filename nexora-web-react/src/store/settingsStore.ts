import { create } from 'zustand'

export type ThemeMode = 'light' | 'dark'
const THEME_KEY = 'nexora-react-theme'

interface SettingsState {
  theme: ThemeMode
  setTheme: (theme: ThemeMode) => void
  toggleTheme: () => void
}

function readTheme(): ThemeMode {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(theme: ThemeMode) {
  document.documentElement.dataset.theme = theme
  document.documentElement.classList.toggle('dark', theme === 'dark')
}

const initialTheme = readTheme()
applyTheme(initialTheme)

export const useSettingsStore = create<SettingsState>(set => ({
  theme: initialTheme,
  setTheme: theme => {
    localStorage.setItem(THEME_KEY, theme)
    applyTheme(theme)
    set({ theme })
  },
  toggleTheme: () => {
    const next = useSettingsStore.getState().theme === 'dark' ? 'light' : 'dark'
    useSettingsStore.getState().setTheme(next)
  }
}))
