import { describe, expect, it } from 'vitest'
import { dictDataFormToPayload, dictFormToPayload, roleFormToPayload, splitRoleMenuSelection, userFormToPayload } from '@/pages/system/managementForms'

describe('management form payloads', () => {
  it('normalizes a newly-created user without dropping required fields', () => {
    expect(userFormToPayload({ nickname: ' 管理员 ', email: 'ADMIN@EXAMPLE.COM ', password: 'Secret1!', mobile: ' 13800138000 ', sex: 0, status: 1, roleIds: [2] }, false)).toEqual({
      nickname: '管理员', email: 'admin@example.com', password: 'Secret1!', mobile: '13800138000', sex: 0, status: 1, roleIds: [2]
    })
  })

  it('does not send immutable email or an empty password while editing a user', () => {
    const payload = userFormToPayload({ id: 7, nickname: 'Nexora', email: 'old@example.com', password: '', mobile: '', sex: 1, status: 2, roleIds: [1, 3] }, true)
    expect(payload).toEqual({ id: 7, nickname: 'Nexora', mobile: '', sex: 1, status: 2, roleIds: [1, 3] })
  })

  it('trims role and dictionary values before submission', () => {
    expect(roleFormToPayload({ id: 2, name: ' 运维 ', code: ' ops ', remarks: ' 说明 ' })).toEqual({ id: 2, name: '运维', code: 'ops', remarks: '说明' })
    expect(dictFormToPayload({ name: ' 性别 ', type: ' sys_sex ', status: 1, remark: ' 基础字典 ' })).toEqual({ name: '性别', type: 'sys_sex', status: 1, remark: '基础字典' })
    expect(dictDataFormToPayload({ dictId: 9, label: ' 男 ', value: ' 1 ', sort: 2, status: 1, remark: '' })).toEqual({ dictId: 9, label: '男', value: '1', sort: 2, status: 1, remark: '' })
  })

  it('keeps granted parent menus half-checked instead of selecting every sibling', () => {
    const records = [{ id: 1, children: [{ id: 14 }, { id: 2 }] }]
    expect(splitRoleMenuSelection(records, [1, 14])).toEqual({ checkedIds: [14], halfCheckedIds: [1] })
  })
})
