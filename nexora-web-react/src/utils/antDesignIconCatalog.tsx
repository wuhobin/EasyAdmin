import * as AntDesignIcons from '@ant-design/icons'
import type { IconComponent } from '@/utils/iconRegistry'

const entries = Object.entries(AntDesignIcons).filter(([name, icon]) =>
  /(?:Outlined|Filled|TwoTone)$/.test(name) && icon !== null && icon !== undefined
) as Array<[string, IconComponent]>

export const antDesignIconCatalog: Record<string, IconComponent> = Object.fromEntries(entries)

export const antDesignIconNames = Object.keys(antDesignIconCatalog).sort((a, b) => a.localeCompare(b))
