import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { notifyUnauthorized } from '@/utils/auth-session'
import { readBlobApiError } from '@/utils/binary-response'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export class RequestError extends Error {
  constructor(
    message: string,
    readonly isUnauthorized = false,
    readonly isReported = false
  ) {
    super(message)
  }
}

export function isUnauthorizedError(error: unknown): boolean {
  return error instanceof RequestError && error.isUnauthorized
}

export function isReportedRequestError(error: unknown): boolean {
  return error instanceof RequestError && error.isReported
}

type BinaryRequestConfig = AxiosRequestConfig & {
  responseType: 'blob' | 'arraybuffer'
}

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 5000,
  headers: { "Content-Type": "application/json;charset=utf-8" },
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  async (response) => {
    // 二进制数据则直接返回
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      const errorResponse = await readBlobApiError(response.data)
      if (errorResponse && errorResponse.code !== 200) {
        if (errorResponse.code === 401) {
          void notifyUnauthorized()
        } else {
          ElMessage.error(errorResponse.message || '请求错误')
        }
        return Promise.reject(new RequestError(
          errorResponse.message || '请求错误',
          errorResponse.code === 401,
          errorResponse.code !== 401
        ))
      }
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        void notifyUnauthorized()
      } else {
        ElMessage.error(res.message || '请求错误')
      }
      return Promise.reject(new RequestError(res.message || '请求错误', res.code === 401, res.code !== 401))
    }
    
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      void notifyUnauthorized()
      return Promise.reject(new RequestError(error.message || '请求错误', true))
    } else if (error.response?.status === 500) {
      ElMessage.error('后端接口连接异常')
    } else {
      ElMessage.error('请求错误')
    }
    return Promise.reject(new RequestError(error.message || '请求错误', false, true))
  }
)

export function request(config: BinaryRequestConfig): Promise<Blob | ArrayBuffer>
export function request<T = any>(config: AxiosRequestConfig): Promise<ApiResponse<T>>
export function request<T = any>(config: AxiosRequestConfig) {
  return service.request<ApiResponse<T>, ApiResponse<T> | Blob | ArrayBuffer>(config)
}

export default request
