import { Navigate, Outlet, type RouteObject } from 'react-router-dom'
import { Suspense } from 'react'
import { ForbiddenPage, NotFoundPage } from '@/pages/NotFoundPage'
import { HomePlaceholder } from '@/pages/HomePlaceholder'
import { resolveRouteComponent } from '@/routes/componentRegistry'
import { flattenRoutes, HOME_PATH, LEGACY_HOME_PATH, LEGACY_PROFILE_PATH, PROFILE_PATH, type MenuRoute } from '@/routes/routeAdapter'

function segment(path: string) {
  return path.replace(/^\/+|\/+$/g, '')
}

function redirectTarget(route: MenuRoute) {
  const target = route.redirect?.trim()
  if (!target) return undefined
  if (target.startsWith('/')) return target
  const base = route.fullPath === '/' ? '' : route.fullPath
  return `${base}/${segment(target)}` || '/'
}

function routeObject(route: MenuRoute): RouteObject {
  const Page = resolveRouteComponent(route)
  const target = redirectTarget(route)
  const hasChildren = route.children.length > 0
  const element = hasChildren
    ? <Outlet />
    : target
      ? <Navigate to={target} replace />
      : <Suspense fallback={<div className="app-loading"><span className="app-loading__spinner" aria-label="正在加载页面" /></div>}><Page title={route.meta.title} /></Suspense>
  const children = hasChildren
    ? [
        ...(target ? [{ index: true, element: <Navigate to={target} replace /> } satisfies RouteObject] : []),
        ...route.children.map(routeObject)
      ]
    : undefined
  return {
    path: segment(route.path) || undefined,
    element,
    children
  }
}

export function createDynamicRouteObjects(routes: MenuRoute[]): RouteObject[] {
  const result: RouteObject[] = []
  for (const route of routes) {
    if (route.path === '/' || route.fullPath === '/') {
      result.push(...route.children.map(routeObject))
    } else {
      result.push(routeObject(route))
    }
  }
  const hasHome = flattenRoutes(routes).some(route => route.fullPath === HOME_PATH)
  if (!hasHome) result.push({ path: segment(HOME_PATH), element: <HomePlaceholder /> })
  result.push({ path: segment(LEGACY_HOME_PATH), element: <Navigate to={HOME_PATH} replace /> })
  result.push({ path: segment(LEGACY_PROFILE_PATH), element: <Navigate to={PROFILE_PATH} replace /> })
  result.push({ path: '403', element: <ForbiddenPage /> })
  result.push({ path: '404', element: <NotFoundPage /> })
  return result
}
