// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest'

const SETTINGS_KEY = 'nexora-react-appearance-v1'

describe('settingsStore', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.className = ''
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.removeAttribute('data-density')
    document.documentElement.style.cssText = ''
    vi.resetModules()
  })

  it('migrates the legacy theme preference into appearance settings', async () => {
    localStorage.setItem('nexora-react-theme', 'dark')

    const { useSettingsStore } = await import('@/store/settingsStore')

    expect(useSettingsStore.getState().theme).toBe('dark')
    expect(JSON.parse(localStorage.getItem(SETTINGS_KEY) ?? '{}').theme).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('persists changes and applies the selected density and accent color', async () => {
    const { useSettingsStore } = await import('@/store/settingsStore')

    useSettingsStore.getState().updateSettings({ accentColor: '#2563eb', density: 'small' })

    expect(JSON.parse(localStorage.getItem(SETTINGS_KEY) ?? '{}')).toMatchObject({ accentColor: '#2563eb', density: 'small' })
    expect(document.documentElement.dataset.density).toBe('small')
    expect(document.documentElement.style.getPropertyValue('--nexora-violet')).toBe('#2563eb')
  })
})
