import request from '@/utils/request'

export interface SysUserForm {
  id?: number
  nickname?: string
  email?: string
  password?: string
  oldPassword?: string
  newPassword?: string
  code?: string
  mobile?: string
  avatar?: string
  sex?: number
  status?: number
  roleIds?: number[]
}

/**
 * 获取用户列表
 */
export function getUserListApi(params?: any) {
  return request({
    url: '/sys/user',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 */
export function getUserDetailApi(id: string) {
  return request({
    url: `/sys/user/${id}`,
    method: 'get'
  })
}

/**
 * 新增用户
 */
export function createUserApi(data: SysUserForm) {
  return request({
    url: '/sys/user',
    method: 'post',
    data
  })
}

/**
 * 修改用户
 */
export function updateUserApi(data: SysUserForm) {
  return request({
    url: `/sys/user`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUserApi(ids: number[] | number) {
  return request({
    url: `/sys/user/delete/${ids}`,
    method: 'delete'
  })
}

/**
 * 重置密码
 */
export function resetPasswordApi(data: SysUserForm) {
  return request({
    url: '/sys/user/reset',
    method: 'put',
    data
  })
}

export function auditUserApi(id: number) {
  return request<void>({
    url: `/sys/user/audit/${id}`,
    method: 'put'
  })
}


// 获取用户个人信息
export function getUserProfileApi() {
  return request({
    url: '/sys/user/profile',
    method: 'get'
  })
}

// 修改用户个人信息
export function updateUserProfileApi(data: SysUserForm) {
  return request({
    url: '/sys/user/updProfile',
    method: 'put',
    data: data
  })
}

// 用户密码重置
export function updateUserPwdApi(oldPassword: string, newPassword: string) {
  const data: SysUserForm = {
    oldPassword,
    newPassword
  }
  return request({
    url: '/sys/user/updatePwd',
    method: 'put',
    data
  })
}

// 用户头像上传
export function uploadAvatar(data: any) {
  return request({
    url: '/system/user/profile/avatar',
    method: 'post',
    data: data
  })
}

export function verifyPassword(password: string) {
  return request({
    url: `/sys/user/verifyPassword/${password}`,
    method: 'get'
  })
}

export function sendChangeEmailCodeApi(email: string) {
  const data: SysUserForm = { email }
  return request({
    url: '/sys/user/profile/email/sendCode',
    method: 'post',
    data
  })
}

export function changeEmailApi(email: string, code: string) {
  const data: SysUserForm = { email, code }
  return request({
    url: '/sys/user/profile/changeEmail',
    method: 'put',
    data
  })
}
