import type { SystemConfig } from '@/api/config'
import type { ThemeMode } from '@/store/settingsStore'

export function formatWatermarkTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function buildWatermarkContent(system: SystemConfig, userName: string, date: Date): string | string[] {
  switch (system.watermarkType) {
    case 'username':
      return userName
    case 'sitename':
      return system.siteName
    case 'custom':
      return system.watermarkCustomText.trim() || system.siteName
    default:
      return [userName, formatWatermarkTime(date)]
  }
}

export function buildWatermarkColor(theme: ThemeMode, opacity: number) {
  const safeOpacity = Math.min(0.5, Math.max(0.01, opacity))
  return theme === 'dark'
    ? `rgba(255, 255, 255, ${safeOpacity})`
    : `rgba(25, 23, 40, ${safeOpacity})`
}
