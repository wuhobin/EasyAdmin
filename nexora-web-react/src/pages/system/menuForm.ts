import type { MenuType, SysMenuPayload, SysMenuRecord } from '@/api/menu'

export interface MenuFormValues {
  parentId: number
  title: string
  sort: number
  type: MenuType
  icon: string
  path: string
  redirect: string
  component: string
  perm: string
  visible: boolean
  external: boolean
}

export const emptyMenuForm: MenuFormValues = {
  parentId: 0,
  title: '',
  sort: 1,
  type: 'CATALOG',
  icon: '',
  path: '',
  redirect: '',
  component: '',
  perm: '',
  visible: true,
  external: false
}

export function normalizeMenuRecords(records: SysMenuRecord[]): SysMenuRecord[] {
  return records.map(record => {
    const { children: rawChildren, ...menu } = record
    const children = rawChildren?.length ? normalizeMenuRecords(rawChildren) : undefined
    return {
      ...menu,
      icon: record.icon || '',
      component: record.component || '',
      perm: record.perm || (record as SysMenuRecord & { perms?: string }).perms || '',
      ...(children ? { children } : {})
    }
  })
}

export function menuRecordToForm(record: SysMenuRecord): MenuFormValues {
  return {
    parentId: record.parentId,
    title: record.title,
    sort: record.sort,
    type: record.type,
    icon: record.icon || '',
    path: record.path || '',
    redirect: record.redirect || '',
    component: record.component || '',
    perm: record.perm || (record as SysMenuRecord & { perms?: string }).perms || '',
    visible: record.hidden !== 1,
    external: record.isExternal === 1
  }
}

export function menuFormToPayload(values: MenuFormValues, id?: number): SysMenuPayload {
  return {
    ...(id === undefined ? {} : { id }),
    parentId: values.parentId,
    title: values.title.trim(),
    sort: values.sort,
    type: values.type,
    icon: values.type === 'BUTTON' ? '' : values.icon,
    path: values.path.trim(),
    redirect: values.redirect.trim(),
    component: values.type === 'CATALOG' ? 'Layout' : values.type === 'MENU' ? values.component.trim() : '',
    perm: values.perm.trim(),
    hidden: values.visible ? 0 : 1,
    isExternal: values.external ? 1 : 0
  }
}
