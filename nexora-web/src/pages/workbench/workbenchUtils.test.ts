import { describe, expect, it } from 'vitest'
import { greetingForHour, noticePreview, selectWorkbenchRuntimePaths, selectWorkbenchShortcuts } from '@/pages/workbench/workbenchUtils'
import type { MenuRoute } from '@/routes/routeAdapter'

function route(path: string, title: string, options: Partial<MenuRoute['meta']> = {}): MenuRoute {
  return {
    id: path.length,
    path,
    fullPath: path,
    name: title,
    component: path,
    sort: 1,
    meta: { title, icon: '', hidden: false, isExternal: false, ...options },
    children: []
  }
}

describe('workbench utilities', () => {
  it('prioritizes known shortcuts and removes hidden routes', () => {
    const result = selectWorkbenchShortcuts([
      route('/file/list', '文件'),
      route('/custom', '自定义'),
      route('/system/user', '用户'),
      route('/hidden', '隐藏', { hidden: true })
    ])

    expect(result.map(item => item.path)).toEqual(['/system/user', '/file/list', '/custom'])
  })

  it('uses renderable leaf routes for runtime overview navigation', () => {
    const fileCatalog = route('/file', '文件管理')
    fileCatalog.redirect = '/file/list'
    fileCatalog.children = [route('/file/list', '文件列表')]
    const mailCatalog = route('/mail', '聚合邮箱')
    mailCatalog.redirect = '/mail/inbox'
    mailCatalog.children = [route('/mail/inbox', '最新邮件')]

    expect(selectWorkbenchRuntimePaths([fileCatalog, mailCatalog])).toMatchObject({
      files: '/file/list',
      mail: '/mail/inbox'
    })
  })

  it('builds safe notice previews', () => {
    expect(noticePreview('text', '  第一行\n 第二行  ')).toBe('第一行 第二行')
    expect(noticePreview('html', '<b>内容</b>')).toBe('HTML 内容，点击查看详情')
  })

  it('uses time-specific greetings', () => {
    expect(greetingForHour(8)).toBe('早上好')
    expect(greetingForHour(15)).toBe('下午好')
    expect(greetingForHour(23)).toBe('夜深了')
  })
})
