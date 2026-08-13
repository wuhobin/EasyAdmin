import { describe, expect, it } from 'vitest'
import { createAppRouteTree, findRouteByPath, findRouteTrail, flattenRoutes, isPageTabRoute, normalizeRouteTree } from '@/routes/routeAdapter'

describe('normalizeRouteTree', () => {
  it('joins nested paths, keeps external paths, and sorts siblings', () => {
    const routes = normalizeRouteTree([
      { id: 2, path: 'reports', name: 'reports', sort: 20, component: '/reports', meta: { title: '报表' } },
      {
        id: 1,
        path: '/',
        name: 'root',
        sort: 1,
        component: 'Layout',
        children: [
          { id: 4, path: 'system', name: 'system', sort: 2, component: 'ParentView', meta: { title: '系统' }, children: [
            { id: 5, path: 'menu', name: 'menu', sort: 1, component: '/system/menu/index', meta: { title: '菜单管理' } }
          ] },
          { id: 3, path: 'docs', name: 'docs', sort: 1, component: 'ParentView', meta: { title: '文档', isExternal: true } }
        ]
      }
    ])

    expect(routes.map(route => route.fullPath)).toEqual(['/','/reports'])
    expect(routes[0].children.map(route => route.fullPath)).toEqual(['/docs', '/system'])
    expect(findRouteByPath(routes, '/system/menu')?.meta.title).toBe('菜单管理')
    expect(findRouteTrail(routes, '/system/menu').map(route => route.fullPath)).toEqual(['/', '/system', '/system/menu'])
    expect(flattenRoutes(routes)).toHaveLength(5)

    const external = normalizeRouteTree([{ id: 8, path: 'https://docs.nexora.dev', component: '', meta: { title: '文档', isExternal: true } }])
    expect(external[0].fullPath).toBe('https://docs.nexora.dev')
  })

  it('drops malformed entries without stopping the rest of the tree', () => {
    const routes = normalizeRouteTree([
      { id: 1, path: '', name: 'empty', component: '' },
      null as never,
      { id: 2, path: 'valid', name: 'valid', component: '' }
    ])
    expect(routes.map(route => route.fullPath)).toEqual(['/','/valid'])
  })

  it('prepends the static workbench and replaces legacy home routes', () => {
    const routes = createAppRouteTree([
      { id: 1, path: '/dashboard', component: '/dashboard', meta: { title: '仪表盘' } },
      { id: 2, path: '/system', component: 'ParentView', meta: { title: '系统管理' }, children: [
        { id: 3, path: 'profile', component: '/system/user/profile/index', meta: { title: '个人中心', hidden: true } }
      ] }
    ])

    expect(routes.map(route => route.fullPath)).toEqual(['/home', '/profile', '/system'])
    expect(routes[2].children).toEqual([])
    expect(routes[0].meta.title).toBe('工作台')
    expect(routes[1].meta.hidden).toBe(true)
  })

  it('only treats renderable leaf routes as cacheable page tabs', () => {
    const [catalog, redirect] = normalizeRouteTree([
      { id: 1, path: '/file', component: 'ParentView', redirect: '/file/list', children: [
        { id: 2, path: 'list', component: '/file/list/index', meta: { title: '文件列表' } }
      ] },
      { id: 3, path: '/legacy', component: '/legacy', redirect: '/home' }
    ])

    expect(isPageTabRoute(catalog)).toBe(false)
    expect(isPageTabRoute(catalog.children[0])).toBe(true)
    expect(isPageTabRoute(redirect)).toBe(false)
  })
})
