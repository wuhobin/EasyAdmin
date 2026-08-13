import type { OssFileRecord } from '@/api/file'

export const MAX_UPLOAD_FILE_SIZE = 50 * 1024 * 1024
export const UPLOAD_ACCEPT = '.jpg,.jpeg,.png,.gif,.webp,.mp4,.pdf,.zip,.txt'

const ALLOWED_UPLOAD_MIME_TYPES: Record<string, ReadonlySet<string>> = {
  jpg: new Set(['image/jpeg']),
  jpeg: new Set(['image/jpeg']),
  png: new Set(['image/png']),
  gif: new Set(['image/gif']),
  webp: new Set(['image/webp']),
  mp4: new Set(['video/mp4']),
  pdf: new Set(['application/pdf']),
  zip: new Set(['application/zip', 'application/x-zip-compressed']),
  txt: new Set(['text/plain'])
}

export type FilePreviewKind = 'image' | 'video' | 'audio' | 'pdf' | 'text' | 'none'

export function validateUploadFile(file: Pick<File, 'name' | 'size' | 'type'>): string | null {
  const name = file.name.trim()
  if (!name) return '文件名不能为空'
  if (Array.from(name).length > 255) return '文件名不能超过 255 个字符'
  if (file.size === 0) return '上传文件不能为空'
  const separator = name.lastIndexOf('.')
  const extension = separator >= 0 ? name.slice(separator + 1).toLowerCase() : ''
  const allowedMimeTypes = ALLOWED_UPLOAD_MIME_TYPES[extension]
  if (!allowedMimeTypes) return '仅支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP 或 TXT 格式'
  if (file.type && !allowedMimeTypes.has(file.type.toLowerCase())) return '文件扩展名与实际类型不一致，请选择正确的文件'
  if (file.size > MAX_UPLOAD_FILE_SIZE) return '文件大小不能超过 50MB'
  return null
}

export function getUploadErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim()) return error.message
  if (typeof error === 'string' && error.trim()) return error
  return '上传失败，请稍后重试'
}

export function displayFileName(file: Pick<OssFileRecord, 'fileName' | 'originalFilename'>) {
  return file.originalFilename || file.fileName
}

export function normalizedFileType(file: Pick<OssFileRecord, 'contentType'>) {
  return (file.contentType || '').split(';')[0].trim().toLowerCase()
}

export function filePreviewKind(file: Pick<OssFileRecord, 'contentType' | 'fileName' | 'originalFilename'>): FilePreviewKind {
  const type = normalizedFileType(file)
  if (type.startsWith('image/')) return 'image'
  if (type.startsWith('video/')) return 'video'
  if (type.startsWith('audio/')) return 'audio'
  if (type === 'application/pdf') return 'pdf'
  if (type.startsWith('text/') || /\.(txt|md|csv|log|java|kt|js|ts|vue|html|css|json|xml|yaml|yml|sql)$/i.test(displayFileName(file))) return 'text'
  return 'none'
}

export function formatFileSize(size?: number) {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1)
  const value = size / Math.pow(1024, index)
  return `${value >= 10 || index === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[index]}`
}

export function validateRenamedFile(originalName: string, nextName: string): string | null {
  const normalized = nextName.trim()
  if (!normalized) return '文件名称不能为空'
  if (Array.from(normalized).length > 255) return '文件名不能超过 255 个字符'
  const originalExtension = originalName.match(/\.[^.]+$/)?.[0]?.toLowerCase() || ''
  const nextExtension = normalized.match(/\.[^.]+$/)?.[0]?.toLowerCase() || ''
  return originalExtension === nextExtension ? null : '不能修改文件扩展名'
}
