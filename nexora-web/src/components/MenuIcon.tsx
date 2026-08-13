import { lazy, Suspense, type LazyExoticComponent } from 'react'
import { resolveIcon, resolveIconName, resolveRegisteredIcon, type IconComponent } from '@/utils/iconRegistry'

interface MenuIconProps {
  value?: string | null
  className?: string
}

const lazyIcons = new Map<string, LazyExoticComponent<IconComponent>>()

function getLazyIcon(name: string) {
  const cached = lazyIcons.get(name)
  if (cached) return cached
  const Icon = lazy(async () => {
    const { antDesignIconCatalog } = await import('@/utils/antDesignIconCatalog')
    return { default: antDesignIconCatalog[name] || antDesignIconCatalog.AppstoreOutlined }
  })
  lazyIcons.set(name, Icon)
  return Icon
}

export function MenuIcon({ value, className = '' }: MenuIconProps) {
  const registeredIcon = resolveRegisteredIcon(value)
  if (registeredIcon) {
    const Icon = registeredIcon
    return <Icon className={className} aria-hidden="true" />
  }

  const name = resolveIconName(value)
  if (!/(?:Outlined|Filled|TwoTone)$/.test(name)) {
    const Icon = resolveIcon(value)
    return <Icon className={className} aria-hidden="true" />
  }

  const LazyIcon = getLazyIcon(name)
  return <Suspense fallback={<span className={`anticon ${className}`.trim()} aria-hidden="true" />}><LazyIcon className={className} aria-hidden="true" /></Suspense>
}
