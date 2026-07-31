import type { AxiosProgressEvent } from 'axios'
import request from '@/utils/request'

export interface OssFileRecord {
  id: number
  fileId: string
  fileUrl: string
  fileName: string
  originalFilename?: string
  contentType?: string
  fileSize: number
  platform?: string
  thumbnailUrl?: string
  uploaderId?: number
  createTime?: string
}

export interface OssFileQuery {
  pageNum: number
  pageSize: number
  fileName?: string
  contentType?: string
  uploaderId?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// 上传文件
export function uploadApi(
  data: FormData,
  onUploadProgress?: (progressEvent: AxiosProgressEvent) => void
) {
  return request<string>({
    url: '/file/upload',
    method: 'post',
    // 覆盖请求实例的 JSON 默认值，由浏览器为 FormData 生成 multipart boundary。
    headers: { 'Content-Type': undefined },
    data,
    timeout: 0,
    onUploadProgress
  })
}

export function getFileListApi(params: OssFileQuery) {
  return request<PageResult<OssFileRecord>>({
    url: '/file/list',
    method: 'get',
    params
  })
}

export function downloadFileApi(id: number) {
  return request({
    url: `/file/${id}/download`,
    method: 'get',
    responseType: 'blob',
    timeout: 0
  })
}

export function deleteFileApi(id: number) {
  return request<void>({
    url: `/file/${id}`,
    method: 'delete'
  })
}
