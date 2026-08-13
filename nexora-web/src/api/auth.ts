import request from '@/api/client'

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

export const loginApi = (data: AuthParams) =>
  request<LoginResult>({ url: '/auth/login', method: 'post', data })

export const registerApi = (data: RegisterParams) =>
  request<void>({ url: '/auth/register', method: 'post', data })

export const sendRegisterCodeApi = (data: Pick<AuthParams, 'email'>) =>
  request<void>({ url: '/auth/register/sendCode', method: 'post', data })

export const generateImageCaptchaApi = () =>
  request<ImageCaptchaResult>({ url: '/auth/image', method: 'post' })

export const matchImageCaptchaApi = (captchaId: string, track: ImageCaptchaTrack) =>
  request<boolean>({
    url: `/auth/image/${encodeURIComponent(captchaId)}/match`,
    method: 'post',
    data: track
  })

export const sendResetPasswordCodeApi = (data: Pick<ResetPasswordParams, 'email'>) =>
  request<void>({ url: '/auth/password/reset/sendCode', method: 'post', data })

export const resetPasswordApi = (data: ResetPasswordParams) =>
  request<void>({ url: '/auth/password/reset', method: 'post', data })

export const logoutApi = () => request<void>({ url: '/auth/logout', method: 'post' })

export const getUserInfoApi = () => request<CurrentUserResult>({ url: '/auth/info', method: 'get' })

export const getRoutersApi = () => request<BackendRoute[]>({ url: '/sys/menu/routers', method: 'get' })

export interface BackendRoute {
  id: number
  component?: string | null
  path: string
  name?: string | null
  redirect?: string | null
  sort?: number | null
  meta?: {
    title?: string | null
    icon?: string | null
    hidden?: boolean | null
    isExternal?: boolean | null
  } | null
  children?: BackendRoute[] | null
}
