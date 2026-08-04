import request, { type ApiResponse } from '@/utils/request'

export interface AuthParams {
  email: string
  password: string
  code?: string
  rememberMe: boolean
  source?: string
}

export type RegisterParams = Pick<AuthParams, 'email' | 'password' | 'code' | 'source'> & {
  captchaId?: string
}

export type ResetPasswordParams = Pick<AuthParams, 'email' | 'password' | 'code'>

export interface ImageCaptchaResult {
  id: string
  type: string
  backgroundImage: string
  templateImage: string
  backgroundImageTag?: string
  templateImageTag?: string
  backgroundImageWidth: number
  backgroundImageHeight: number
  templateImageWidth: number
  templateImageHeight: number
  data?: unknown
}

export interface ImageCaptchaTrackPoint {
  x: number
  y: number
  t: number
  type: 'DOWN' | 'MOVE' | 'UP'
}

export interface ImageCaptchaTrack {
  bgImageWidth: number
  bgImageHeight: number
  templateImageWidth: number
  templateImageHeight: number
  startTime: number
  stopTime: number
  left: number
  top: number
  trackList: ImageCaptchaTrackPoint[]
  data?: unknown
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

export function generateImageCaptchaApi(): Promise<ApiResponse<ImageCaptchaResult>> {
  return request<ImageCaptchaResult>({
    url: '/auth/image',
    method: 'post'
  })
}

export function matchImageCaptchaApi(
  captchaId: string,
  track: ImageCaptchaTrack
): Promise<ApiResponse<boolean>> {
  return request<boolean>({
    url: `/auth/image/${encodeURIComponent(captchaId)}/match`,
    method: 'post',
    data: track
  })
}

export function registerApi(data: RegisterParams): Promise<ApiResponse<void>> {
  return request<void>({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function sendResetPasswordCodeApi(data: Pick<ResetPasswordParams, 'email'>): Promise<ApiResponse<void>> {
  return request<void>({
    url: '/auth/password/reset/sendCode',
    method: 'post',
    data
  })
}

export function resetPasswordApi(data: ResetPasswordParams): Promise<ApiResponse<void>> {
  return request<void>({
    url: '/auth/password/reset',
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
