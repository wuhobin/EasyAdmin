import { describe, expect, it } from 'vitest'
import { isValidElement } from 'react'
import { createDynamicRouteObjects } from '@/routes/routeObjects'
import { normalizeRouteTree } from '@/routes/routeAdapter'

describe('createDynamicRouteObjects', () => {
  it('keeps child routes renderable when a parent has a default redirect', () => {
    const routes = normalizeRouteTree([{
      id: 1,
      path: 'system',
      component: 'ParentView',
      redirect: '/system/menu',
      children: [{ id: 2, path: 'menu', component: '/system/menu/index', meta: { title: '菜单管理' } }]
    }])

    const [systemRoute] = createDynamicRouteObjects(routes)
    expect(systemRoute.path).toBe('system')
    expect(systemRoute.children).toHaveLength(2)
    expect(systemRoute.children?.[0].index).toBe(true)
    expect(systemRoute.children?.[1].path).toBe('menu')
    expect(isValidElement(systemRoute.children?.[1].element)).toBe(true)

    const appRoutes = createDynamicRouteObjects(routes)
    expect(appRoutes.some(route => route.path === 'home')).toBe(true)
    expect(appRoutes.some(route => route.path === 'dashboard')).toBe(true)
    expect(appRoutes.some(route => route.path === 'system/profile')).toBe(true)
  })
})
