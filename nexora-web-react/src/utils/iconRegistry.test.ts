import { describe, expect, it } from 'vitest'
import { antDesignIconNames } from '@/utils/antDesignIconCatalog'
import { isAntDesignIcon, resolveIconName, resolveIcon } from '@/utils/iconRegistry'

describe('icon registry', () => {
  it('reads the new antd namespace and legacy values', () => {
    expect(resolveIconName('antd:DashboardOutlined')).toBe('DashboardOutlined')
    expect(resolveIconName('Setting')).toBe('SettingOutlined')
    expect(resolveIconName('AlarmClock')).toBe('ScheduleOutlined')
    expect(resolveIconName('DocumentCopy')).toBe('FileTextOutlined')
    expect(resolveIconName('MessageBox')).toBe('InboxOutlined')
    expect(resolveIconName('Platform')).toBe('CloudServerOutlined')
    expect(resolveIconName('')).toBe('AppstoreOutlined')
    expect(isAntDesignIcon('antd:MenuOutlined')).toBe(true)
    expect(isAntDesignIcon('Menu')).toBe(false)
  })

  it('falls back to the app icon for unknown values', () => {
    expect(resolveIcon('not-registered')).toBe(resolveIcon(''))
  })

  it('exposes the complete Ant Design icon family for the picker', () => {
    expect(antDesignIconNames.length).toBeGreaterThan(100)
  })
})
