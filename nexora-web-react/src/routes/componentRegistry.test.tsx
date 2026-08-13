import { describe, expect, it } from 'vitest'
import { MigrationPlaceholder } from '@/pages/MigrationPlaceholder'
import { normalizeRouteTree } from '@/routes/routeAdapter'
import { resolveRouteComponent } from '@/routes/componentRegistry'

describe('component registry', () => {
  it('resolves the migrated configuration management route', () => {
    const [route] = normalizeRouteTree([{ id: 130, path: '/system/config', component: '/system/config/index', meta: { title: '配置管理' } }])
    expect(resolveRouteComponent(route)).not.toBe(MigrationPlaceholder)
  })

  it.each([
    { id: -2, path: '/profile', component: '/profile', title: '个人中心' },
    { id: 33, path: '/system/log/operation', component: '/system/log/operation/index', title: '操作日志' },
    { id: 135, path: '/system/log/job-log', component: '/monitor/job/log', title: '调度日志' },
    { id: 150, path: '/system/notice', component: '/system/notice/index', title: '系统通知' },
    { id: 160, path: '/monitor/server', component: '/monitor/server/index', title: '服务器管理' },
    { id: 161, path: '/monitor/online', component: '/monitor/online/index', title: '在线用户' },
    { id: 162, path: '/monitor/job', component: '/monitor/job/index', title: '定时任务' },
    { id: 118, path: '/mail', component: '/mail/index', title: '最新邮件' },
    { id: 127, path: '/mail/account', component: '/mail/account/index', title: '邮箱列表' },
    { id: 112, path: '/file/list', component: '/file/index', title: '文件列表' }
  ])('resolves the migrated $title route', backendRoute => {
    const [route] = normalizeRouteTree([{ ...backendRoute, meta: { title: backendRoute.title } }])
    expect(resolveRouteComponent(route)).not.toBe(MigrationPlaceholder)
  })
})
