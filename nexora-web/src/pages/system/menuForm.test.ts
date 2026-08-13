import { describe, expect, it } from 'vitest'
import type { SysMenuRecord } from '@/api/menu'
import { menuFormToPayload, menuRecordToForm, normalizeMenuRecords } from '@/pages/system/menuForm'

const record: SysMenuRecord = {
  id: 7,
  parentId: 2,
  path: 'menu',
  component: '/system/menu/index',
  title: '菜单管理',
  sort: 3,
  icon: 'Setting',
  type: 'MENU',
  redirect: '',
  name: 'menu',
  hidden: 0,
  isExternal: 0,
  perm: '',
  children: []
}

describe('menu form conversion', () => {
  it('normalizes legacy nullable fields and maps records to form values', () => {
    const [normalized] = normalizeMenuRecords([{ ...record, icon: '', component: '', perm: '', children: undefined }])
    expect(normalized).not.toHaveProperty('children')
    expect(menuRecordToForm({ ...record, perm: '', ...( { perms: 'sys:menu:view' } as object) }).perm).toBe('sys:menu:view')
  })

  it('builds backend payloads with stable component and visibility values', () => {
    const payload = menuFormToPayload({ ...menuRecordToForm(record), title: '  菜单管理  ', type: 'CATALOG', component: '/ignored' }, 7)
    expect(payload).toMatchObject({ id: 7, title: '菜单管理', component: 'Layout', hidden: 0, isExternal: 0 })

    const buttonPayload = menuFormToPayload({ ...menuRecordToForm(record), type: 'BUTTON', icon: 'antd:DeleteOutlined', perm: '  sys:menu:delete ', external: true }, undefined)
    expect(buttonPayload).toMatchObject({ component: '', icon: '', perm: 'sys:menu:delete', isExternal: 1 })
    expect(buttonPayload).not.toHaveProperty('id', undefined)
  })
})
