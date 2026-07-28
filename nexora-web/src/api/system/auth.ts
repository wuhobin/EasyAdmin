import request, { type ApiResponse } from '@/utils/request'

export interface AuthParams {
  email: string
  password: string
  code: string
  rememberMe: boolean
  source?: string
}

export interface CurrentUserResult {
  id: number
  email: string
  nickname: string | null
  avatar: string | null
  roles: string[] | null
  permissions: string[] | null
}

export interface LoginResult extends CurrentUserResult {
  token: string
}

// 登录接口
export function loginApi(data: AuthParams): Promise<ApiResponse<LoginResult>> {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function sendRegisterCodeApi(data: AuthParams): Promise<ApiResponse<void>> {
  return request<void>({
    url: '/auth/register/sendCode',
    method: 'post',
    data
  })
}

export function registerApi(data: AuthParams): Promise<ApiResponse<void>> {
  return request<void>({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function logoutApi() {
    return request({
      url: '/auth/logout',
      method: 'post',
    })
  }

// 获取用户信息
export function getUserInfoApi(): Promise<ApiResponse<CurrentUserResult>> {
  return request<CurrentUserResult>({
    url: "/auth/info",
    method: "get"
  })
}

export function getRouters() {
  return request({
    url: '/sys/menu/routers',
    method: 'get'
  })
}
