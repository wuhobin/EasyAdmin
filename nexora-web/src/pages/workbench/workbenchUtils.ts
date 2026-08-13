import { flattenRoutes, HOME_PATH, PROFILE_PATH, type MenuRoute } from '@/routes/routeAdapter'

export interface WorkbenchShortcut {
  path: string
  title: string
  icon: string
}

const shortcutPriority = [
  '/system/user',
  '/system/role',
  '/system/menu',
  '/system/config',
  '/monitor/job',
  '/monitor/server',
  '/mail/inbox',
  '/file/list',
  '/system/notice'
]

function uniqueByPath(routes: MenuRoute[]) {
  const seen = new Set<string>()
  return routes.filter(route => {
    if (seen.has(route.fullPath)) return false
    seen.add(route.fullPath)
    return true
  })
}

export function getAvailableRoutePath(routes: MenuRoute[], candidates: string[]) {
  return flattenRoutes(routes).find(route => candidates.includes(route.fullPath))?.fullPath
}

export function selectWorkbenchRuntimePaths(routes: MenuRoute[]) {
  return {
    jobs: getAvailableRoutePath(routes, ['/monitor/job']),
    servers: getAvailableRoutePath(routes, ['/monitor/server']),
    files: getAvailableRoutePath(routes, ['/file/list']),
    mail: getAvailableRoutePath(routes, ['/mail/inbox'])
  }
}

export function selectWorkbenchShortcuts(routes: MenuRoute[], limit = 6): WorkbenchShortcut[] {
  const available = uniqueByPath(flattenRoutes(routes).filter(route =>
    !route.meta.hidden
    && !route.meta.isExternal
    && route.fullPath !== HOME_PATH
    && route.fullPath !== PROFILE_PATH
    && route.children.length === 0
  ))
  const byPath = new Map(available.map(route => [route.fullPath, route]))
  const preferred = shortcutPriority.flatMap(path => {
    const route = byPath.get(path)
    return route ? [route] : []
  })
  const preferredPaths = new Set(preferred.map(route => route.fullPath))
  return [...preferred, ...available.filter(route => !preferredPaths.has(route.fullPath))]
    .slice(0, limit)
    .map(route => ({ path: route.fullPath, title: route.meta.title, icon: route.meta.icon }))
}

export function noticePreview(contentFormat: string, preview?: string, content?: string) {
  if (contentFormat === 'html') return 'HTML 内容，点击查看详情'
  return (preview || content || '').replace(/\s+/g, ' ').trim().slice(0, 90)
}

export function formatRelativeTime(value?: string) {
  if (!value) return ''
  const timestamp = new Date(value).getTime()
  if (Number.isNaN(timestamp)) return value
  const seconds = Math.round((timestamp - Date.now()) / 1000)
  const formatter = new Intl.RelativeTimeFormat('zh-CN', { numeric: 'auto' })
  if (Math.abs(seconds) < 60) return formatter.format(seconds, 'second')
  const minutes = Math.round(seconds / 60)
  if (Math.abs(minutes) < 60) return formatter.format(minutes, 'minute')
  const hours = Math.round(minutes / 60)
  if (Math.abs(hours) < 24) return formatter.format(hours, 'hour')
  return formatter.format(Math.round(hours / 24), 'day')
}

export function greetingForHour(hour: number) {
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '夜深了'
}
