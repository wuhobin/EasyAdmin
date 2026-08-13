import { describe, expect, it } from 'vitest'
import type { SystemConfig } from '@/api/config'
import { buildWatermarkColor, buildWatermarkContent, formatWatermarkTime } from '@/utils/watermark'

const systemConfig: SystemConfig = {
  siteName: 'Nexora Admin',
  shortTitle: 'Nexora',
  siteDescription: '',
  siteLogo: '',
  copyright: '',
  icp: '',
  watermarkEnabled: true,
  watermarkType: 'username_time',
  watermarkCustomText: '',
  watermarkOpacity: 0.15
}

describe('watermark settings', () => {
  it('builds all supported watermark contents', () => {
    const date = new Date(2026, 7, 12, 9, 5)
    expect(formatWatermarkTime(date)).toBe('2026-08-12 09:05')
    expect(buildWatermarkContent(systemConfig, 'Admin', date)).toEqual(['Admin', '2026-08-12 09:05'])
    expect(buildWatermarkContent({ ...systemConfig, watermarkType: 'username' }, 'Admin', date)).toBe('Admin')
    expect(buildWatermarkContent({ ...systemConfig, watermarkType: 'sitename' }, 'Admin', date)).toBe('Nexora Admin')
    expect(buildWatermarkContent({ ...systemConfig, watermarkType: 'custom', watermarkCustomText: 'Internal' }, 'Admin', date)).toBe('Internal')
    expect(buildWatermarkContent({ ...systemConfig, watermarkType: 'custom' }, 'Admin', date)).toBe('Nexora Admin')
  })

  it('uses the configured opacity with theme-aware text colors', () => {
    expect(buildWatermarkColor('light', 0.15)).toBe('rgba(25, 23, 40, 0.15)')
    expect(buildWatermarkColor('dark', 0.15)).toBe('rgba(255, 255, 255, 0.15)')
    expect(buildWatermarkColor('light', 0.9)).toBe('rgba(25, 23, 40, 0.5)')
  })
})
