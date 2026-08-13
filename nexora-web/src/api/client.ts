import axios, { type AxiosRequestConfig } from 'axios'
import { notifyUnauthorized } from '@/utils/auth-session'
import { getToken } from '@/utils/token'
import { RequestError, type ApiResponse } from '@/types/api'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }

    const result = response.data as ApiResponse
    if (result.code !== 200) {
      if (result.code === 401) void notifyUnauthorized()
      throw new RequestError(result.message || '请求失败', result.code === 401, result.code !== 401)
    }
    return result
  },
  error => {
    if (axios.isCancel(error)) return Promise.reject(error)
    if (error.response?.status === 401) {
      void notifyUnauthorized()
      return Promise.reject(new RequestError(error.message || '登录状态已失效', true))
    }
    return Promise.reject(new RequestError(error.message || '请求失败', false, true))
  }
)

export function request<T>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.request<ApiResponse<T>, ApiResponse<T>>(config)
}

export default request
