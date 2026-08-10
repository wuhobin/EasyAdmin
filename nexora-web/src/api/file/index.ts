import type {AxiosProgressEvent} from 'axios'
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

export interface PageResult<T> {
    records: T[]
    total: number
    current: number
    size: number
}

// 上传文件
export function uploadApi(
    data: FormData,
    onUploadProgress?: (progressEvent: AxiosProgressEvent) => void,
    groupId?: number
) {
    return request<string>({
        url: '/file/upload',
        method: 'post',
        // 覆盖请求实例的 JSON 默认值，由浏览器为 FormData 生成 multipart boundary。
        headers: {'Content-Type': undefined},
        data,
        timeout: 0,
        onUploadProgress,
        params: groupId ? {groupId} : undefined
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

export function getFileGroupsApi(ownerId?: number) {
    return request<FileGroupList>({
        url: '/file/groups',
        method: 'get',
        params: ownerId ? {ownerId} : undefined
    })
}

export function createFileGroupApi(data: { name: string; ownerId?: number }) {
    return request<FileGroup>({url: '/file/groups', method: 'post', data})
}

export function renameFileGroupApi(id: number, data: { name: string; ownerId?: number }) {
    return request<FileGroup>({url: `/file/groups/${id}`, method: 'put', data})
}

export function deleteFileGroupApi(id: number, ownerId?: number) {
    return request<void>({url: `/file/groups/${id}`, method: 'delete', params: ownerId ? {ownerId} : undefined})
}

export function batchDeleteFilesApi(data: { fileIds: number[]; uploaderId?: number }) {
    return request<void>({url: '/file/batch-delete', method: 'post', data})
}

export function moveFilesApi(data: { fileIds: number[]; groupId?: number; uploaderId?: number }) {
    return request<void>({url: '/file/batch-move', method: 'put', data})
}

export function renameFileApi(id: number, newName: string) {
    return request<void>({url: `/file/${id}/rename`, method: 'put', data: {newName}})
}

export function previewFileApi(id: number) {
    return request<Blob>({url: `/file/${id}/preview`, method: 'get', responseType: 'blob', timeout: 0})
}

export function previewTextFileApi(id: number) {
    return request<string>({url: `/file/${id}/text`, method: 'get', timeout: 0})
}
