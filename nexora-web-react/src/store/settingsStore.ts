import { create } from 'zustand'

export type ThemeMode = 'light' | 'dark'
export type InterfaceDensity = 'small' | 'default' | 'large'
export type PageAnimation = 'slide' | 'fade' | 'none'
export type TagsStyle = 'border' | 'card' | 'modern'

export interface AppearanceSettings {
  theme: ThemeMode
  accentColor: string
  greyMode: boolean
  density: InterfaceDensity
  showLogo: boolean
  showTags: boolean
  dynamicTitle: boolean
  watermark: boolean
  showFooter: boolean
  pageAnimation: PageAnimation
  tagsStyle: TagsStyle
}

const SETTINGS_KEY = 'nexora-react-appearance-v1'
const LEGACY_THEME_KEY = 'nexora-react-theme'

export const defaultAppearanceSettings: AppearanceSettings = {
  theme: 'light',
  accentColor: '#6c3ff5',
  greyMode: false,
  density: 'default',
  showLogo: true,
  showTags: true,
  dynamicTitle: false,
  watermark: false,
  showFooter: true,
  pageAnimation: 'fade',
  tagsStyle: 'border'
}

function preferredTheme(): ThemeMode {
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function isHexColor(value: unknown): value is string {
  return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value)
}

function readSettings(): AppearanceSettings {
  let saved: Partial<AppearanceSettings> = {}
  let migrateLegacyTheme = false
  try {
    const value = localStorage.getItem(SETTINGS_KEY)
    if (value) saved = JSON.parse(value) as Partial<AppearanceSettings>
    else {
      const legacyTheme = localStorage.getItem(LEGACY_THEME_KEY)
      if (legacyTheme === 'light' || legacyTheme === 'dark') {
        saved.theme = legacyTheme
        migrateLegacyTheme = true
      }
    }
  } catch {
    // Storage can be unavailable in private browsing or restricted environments.
  }
  const theme = saved.theme === 'light' || saved.theme === 'dark' ? saved.theme : preferredTheme()
  const settings = {
    ...defaultAppearanceSettings,
    ...saved,
    theme,
    accentColor: isHexColor(saved.accentColor) ? saved.accentColor : defaultAppearanceSettings.accentColor
  }
  if (migrateLegacyTheme) persistSettings(settings)
  return settings
}

function mixWithWhite(hex: string, amount: number) {
  const value = Number.parseInt(hex.slice(1), 16)
  const mix = (channel: number) => Math.round(channel + (255 - channel) * amount)
  const red = mix((value >> 16) & 255)
  const green = mix((value >> 8) & 255)
  const blue = mix(value & 255)
  return `#${[red, green, blue].map(channel => channel.toString(16).padStart(2, '0')).join('')}`
}

export function getEffectiveAccentColor(theme: ThemeMode, accentColor: string) {
  return theme === 'dark' ? mixWithWhite(accentColor, 0.24) : accentColor
}

function mixWithBlack(hex: string, amount: number) {
  const value = Number.parseInt(hex.slice(1), 16)
  const mix = (channel: number) => Math.round(channel * (1 - amount))
  const red = mix((value >> 16) & 255)
  const green = mix((value >> 8) & 255)
  const blue = mix(value & 255)
  return `#${[red, green, blue].map(channel => channel.toString(16).padStart(2, '0')).join('')}`
}

function applySettings(settings: AppearanceSettings) {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const accent = getEffectiveAccentColor(settings.theme, settings.accentColor)
  root.dataset.theme = settings.theme
  root.dataset.density = settings.density
  root.dataset.pageAnimation = settings.pageAnimation
  root.classList.toggle('dark', settings.theme === 'dark')
  root.classList.toggle('grey-mode', settings.greyMode)
  root.style.setProperty('--nexora-violet', accent)
  root.style.setProperty('--nexora-violet-dark', mixWithBlack(accent, settings.theme === 'dark' ? 0.1 : 0.2))
  document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
    ?.setAttribute('content', settings.theme === 'dark' ? '#15131e' : '#fcfbfe')
}

function persistSettings(settings: AppearanceSettings) {
  try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings)) } catch { /* Persistence is optional. */ }
}

interface SettingsState extends AppearanceSettings {
  updateSettings: (settings: Partial<AppearanceSettings>) => void
  setTheme: (theme: ThemeMode) => void
  toggleTheme: () => void
  resetSettings: () => void
}

const initialSettings = readSettings()
applySettings(initialSettings)

export const useSettingsStore = create<SettingsState>((set, get) => ({
  ...initialSettings,
  updateSettings: updates => {
    const next = { ...get(), ...updates } as SettingsState
    const appearance = Object.fromEntries(Object.keys(defaultAppearanceSettings).map(key => [key, next[key as keyof AppearanceSettings]])) as unknown as AppearanceSettings
    persistSettings(appearance)
    applySettings(appearance)
    set(updates)
  },
  setTheme: theme => get().updateSettings({ theme }),
  toggleTheme: () => get().setTheme(get().theme === 'dark' ? 'light' : 'dark'),
  resetSettings: () => {
    const next = { ...defaultAppearanceSettings }
    persistSettings(next)
    applySettings(next)
    set(next)
  }
}))
