import type { SysDictDataPayload, SysDictDataRecord, SysDictPayload, SysDictRecord } from '@/api/dict'
import type { SysRolePayload, SysRoleRecord } from '@/api/role'
import type { SysUserPayload, SysUserRecord } from '@/api/user'

interface MenuSelectionNode {
  id: number
  children?: MenuSelectionNode[]
}

export interface UserFormValues {
  id?: number
  nickname: string
  email: string
  password: string
  mobile: string
  sex: number
  status: number
  roleIds: number[]
}

export const emptyUserForm: UserFormValues = {
  nickname: '',
  email: '',
  password: '',
  mobile: '',
  sex: 0,
  status: 1,
  roleIds: []
}

export function userRecordToForm(record: SysUserRecord): UserFormValues {
  return {
    id: record.id,
    nickname: record.nickname || '',
    email: record.email || '',
    password: '',
    mobile: record.mobile || '',
    sex: record.sex ?? 0,
    status: record.status,
    roleIds: record.roleIds ?? []
  }
}

export function userFormToPayload(values: UserFormValues, editing: boolean): SysUserPayload {
  const common = {
    nickname: values.nickname.trim(),
    mobile: values.mobile.trim(),
    sex: values.sex,
    status: values.status,
    roleIds: values.roleIds
  }
  return editing
    ? { id: values.id, ...common }
    : { ...common, email: values.email.trim().toLowerCase(), password: values.password }
}

export interface RoleFormValues {
  id?: number
  name: string
  code: string
  remarks: string
}

export const emptyRoleForm: RoleFormValues = { name: '', code: '', remarks: '' }

export function roleRecordToForm(record: SysRoleRecord): RoleFormValues {
  return { id: record.id, name: record.name, code: record.code, remarks: record.remarks || '' }
}

export function roleFormToPayload(values: RoleFormValues): SysRolePayload {
  return {
    ...(values.id === undefined ? {} : { id: values.id }),
    name: values.name.trim(),
    code: values.code.trim(),
    remarks: values.remarks.trim()
  }
}

export interface DictFormValues {
  id?: number
  name: string
  type: string
  status: number
  remark: string
}

export const emptyDictForm: DictFormValues = { name: '', type: '', status: 1, remark: '' }

export function dictRecordToForm(record: SysDictRecord): DictFormValues {
  return { id: record.id, name: record.name, type: record.type, status: record.status, remark: record.remark || '' }
}

export function dictFormToPayload(values: DictFormValues): SysDictPayload {
  return {
    ...(values.id === undefined ? {} : { id: values.id }),
    name: values.name.trim(),
    type: values.type.trim(),
    status: values.status,
    remark: values.remark.trim()
  }
}

export interface DictDataFormValues {
  id?: number
  dictId: number
  label: string
  value: string
  sort: number
  status: number
  remark: string
}

export function emptyDictDataForm(dictId: number): DictDataFormValues {
  return { dictId, label: '', value: '', sort: 0, status: 1, remark: '' }
}

export function dictDataRecordToForm(record: SysDictDataRecord): DictDataFormValues {
  return {
    id: record.id,
    dictId: record.dictId,
    label: record.label,
    value: record.value,
    sort: record.sort,
    status: record.status,
    remark: record.remark || ''
  }
}

export function dictDataFormToPayload(values: DictDataFormValues): SysDictDataPayload {
  return {
    ...(values.id === undefined ? {} : { id: values.id }),
    dictId: values.dictId,
    label: values.label.trim(),
    value: values.value.trim(),
    sort: values.sort,
    status: values.status,
    remark: values.remark.trim()
  }
}

export function splitRoleMenuSelection(records: MenuSelectionNode[], menuIds: number[]) {
  const branchIds = new Set<number>()
  const visit = (items: MenuSelectionNode[]) => items.forEach(item => {
    if (item.children?.length) {
      branchIds.add(item.id)
      visit(item.children)
    }
  })
  visit(records)
  return {
    checkedIds: menuIds.filter(id => !branchIds.has(id)),
    halfCheckedIds: menuIds.filter(id => branchIds.has(id))
  }
}
