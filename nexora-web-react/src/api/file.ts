import type { AxiosProgressEvent } from 'axios'
import request from '@/api/client'
import type { PageResult } from '@/types/api'

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
  groupId?: number
  groupName?: string
  createTime?: string
}

export interface OssFileQuery {
  pageNum: number
  pageSize: number
  fileName?: string
  contentType?: string
  uploaderId?: number
  groupId?: number
  ungrouped?: boolean
}

export interface FileGroup {
  id: number
  ownerId: number
  name: string
  fileCount: number
}

export interface FileGroupList {
  groups: FileGroup[]
  ungroupedCount: number
  scopeRequired: boolean
}

export const uploadFileApi = (data: FormData, onUploadProgress?: (event: AxiosProgressEvent) => void) =>
  request<string>({
    url: '/file/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': undefined },
    timeout: 0,
    onUploadProgress
  })

export const getFileListApi = (params: OssFileQuery) =>
  request<PageResult<OssFileRecord>>({ url: '/file/list', method: 'get', params })

export const downloadFileApi = (id: number) =>
  request<Blob>({ url: `/file/${id}/download`, method: 'get', responseType: 'blob', timeout: 0 }) as unknown as Promise<Blob>

export const deleteFileApi = (id: number) =>
  request<void>({ url: `/file/${id}`, method: 'delete' })

export const getFileGroupsApi = (ownerId?: number) =>
  request<FileGroupList>({ url: '/file/groups', method: 'get', params: ownerId ? { ownerId } : undefined })

export const createFileGroupApi = (data: { name: string; ownerId?: number }) =>
  request<FileGroup>({ url: '/file/groups', method: 'post', data })

export const renameFileGroupApi = (id: number, data: { name: string; ownerId?: number }) =>
  request<FileGroup>({ url: `/file/groups/${id}`, method: 'put', data })

export const deleteFileGroupApi = (id: number, ownerId?: number) =>
  request<void>({ url: `/file/groups/${id}`, method: 'delete', params: ownerId ? { ownerId } : undefined })

export const batchDeleteFilesApi = (data: { fileIds: number[]; uploaderId?: number }) =>
  request<void>({ url: '/file/batch-delete', method: 'post', data })

export const moveFilesApi = (data: { fileIds: number[]; groupId?: number; uploaderId?: number }) =>
  request<void>({ url: '/file/batch-move', method: 'put', data })

export const renameFileApi = (id: number, newName: string) =>
  request<void>({ url: `/file/${id}/rename`, method: 'put', data: { newName } })

export const previewFileApi = (id: number) =>
  request<Blob>({ url: `/file/${id}/preview`, method: 'get', responseType: 'blob', timeout: 0 }) as unknown as Promise<Blob>

export const previewTextFileApi = (id: number) =>
  request<string>({ url: `/file/${id}/text`, method: 'get', timeout: 0 })
