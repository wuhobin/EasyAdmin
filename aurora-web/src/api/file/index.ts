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
  uploaderName?: string
  createTime?: string
}

export interface OssFileQuery {
  pageNum: number
  pageSize: number
  fileName?: string
  contentType?: string
  uploaderName?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// 上传文件
export function uploadApi(data: any) {
  return request({
    url: '/file/upload',
    method: 'post',
    headers: { "Content-Type": "multipart/articles-data" },
    data
  })
}

export function getFileListApi(params: OssFileQuery) {
  return request<PageResult<OssFileRecord>>({
    url: '/file/list',
    method: 'get',
    params
  })
}

export function deleteFileApi(id: number) {
  return request<void>({
    url: `/file/${id}`,
    method: 'delete'
  })
}

