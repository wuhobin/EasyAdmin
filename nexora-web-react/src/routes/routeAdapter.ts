import type { BackendRoute } from '@/api/auth'
import { isExternalPath } from '@/utils/routeNavigation'

export interface RouteMeta {
  title: string
  icon: string
  hidden: boolean
  isExternal: boolean
  activeMenu?: string
}

export interface MenuRoute {
  id: number
  path: string
  fullPath: string
  name: string
  component: string
  redirect?: string
  sort: number
  meta: RouteMeta
  children: MenuRoute[]
}

export const HOME_PATH = '/home'
export const LEGACY_HOME_PATH = '/dashboard'
export const PROFILE_PATH = '/profile'
export const LEGACY_PROFILE_PATH = '/system/profile'

export const homeRoute: MenuRoute = {
  id: -1,
  path: HOME_PATH,
  fullPath: HOME_PATH,
  name: 'Home',
  component: HOME_PATH,
  sort: -1,
  meta: {
    title: '工作台',
    icon: 'antd:HomeOutlined',
    hidden: false,
    isExternal: false,
    activeMenu: HOME_PATH
  },
  children: []
}

export const profileRoute: MenuRoute = {
  id: -2,
  path: PROFILE_PATH,
  fullPath: PROFILE_PATH,
  name: 'Profile',
  component: PROFILE_PATH,
  sort: -1,
  meta: {
    title: '个人中心',
    icon: 'antd:UserOutlined',
    hidden: true,
    isExternal: false,
    activeMenu: PROFILE_PATH
  },
  children: []
}

const cleanSegment = (path: string) => path.replace(/^\/+|\/+$/g, '')

function joinPath(parent: string, child: string) {
  if (isExternalPath(child)) return child
  const segment = cleanSegment(child)
  if (!segment) return parent || '/'
  if (child.startsWith('/')) return `/${segment}`
  return `${parent === '/' ? '' : parent}/${segment}` || '/'
}

function normalizeRoute(route: BackendRoute, parentPath: string): MenuRoute | null {
  if (!route || typeof route.path !== 'string') return null
  const fullPath = joinPath(parentPath || '/', route.path)
  const children = (route.children ?? [])
    .map(child => normalizeRoute(child, fullPath))
    .filter((child): child is MenuRoute => Boolean(child))
    .sort((a, b) => a.sort - b.sort)
  const meta = route.meta ?? {}
  return {
    id: route.id,
    path: route.path,
    fullPath,
    name: route.name || fullPath,
    component: route.component || (children.length ? 'ParentView' : ''),
    redirect: route.redirect || undefined,
    sort: route.sort ?? 0,
    meta: {
      title: meta.title || route.name || fullPath,
      icon: meta.icon || '',
      hidden: Boolean(meta.hidden),
      isExternal: Boolean(meta.isExternal),
      activeMenu: fullPath
    },
    children
  }
}

export function normalizeRouteTree(routes: BackendRoute[]): MenuRoute[] {
  return routes
    .map(route => normalizeRoute(route, '/'))
    .filter((route): route is MenuRoute => Boolean(route))
    .sort((a, b) => a.sort - b.sort)
}

function removeStaticRouteDuplicates(routes: MenuRoute[]): MenuRoute[] {
  return routes.flatMap(route => {
    if ([HOME_PATH, LEGACY_HOME_PATH, PROFILE_PATH, LEGACY_PROFILE_PATH].includes(route.fullPath)) return []
    return [{ ...route, children: removeStaticRouteDuplicates(route.children) }]
  })
}

export function createAppRouteTree(routes: BackendRoute[]): MenuRoute[] {
  return [homeRoute, profileRoute, ...removeStaticRouteDuplicates(normalizeRouteTree(routes))]
}

export function flattenRoutes(routes: MenuRoute[]): MenuRoute[] {
  return routes.flatMap(route => [route, ...flattenRoutes(route.children)])
}

export function findRouteByPath(routes: MenuRoute[], path: string): MenuRoute | undefined {
  return flattenRoutes(routes).find(route => route.fullPath === path)
}

export function findRouteTrail(routes: MenuRoute[], path: string): MenuRoute[] {
  for (const route of routes) {
    if (route.fullPath === path) return [route]
    const childTrail = findRouteTrail(route.children, path)
    if (childTrail.length) return [route, ...childTrail]
  }
  return []
}
