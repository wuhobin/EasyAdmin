export const MAX_UPLOAD_FILE_SIZE = 50 * 1024 * 1024

export const UPLOAD_ACCEPT = [
  '.jpg',
  '.jpeg',
  '.png',
  '.gif',
  '.webp',
  '.mp4',
  '.pdf',
  '.zip',
  '.txt'
].join(',')

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

export function validateUploadFile(file: Pick<File, 'name' | 'size' | 'type'>): string | null {
  if (!file.name.trim()) {
    return '文件名不能为空'
  }
  if (Array.from(file.name).length > 255) {
    return '文件名不能超过 255 个字符'
  }
  if (file.size === 0) {
    return '上传文件不能为空'
  }

  const extensionSeparator = file.name.lastIndexOf('.')
  const extension = extensionSeparator >= 0
    ? file.name.slice(extensionSeparator + 1).toLowerCase()
    : ''
  const allowedMimeTypes = ALLOWED_UPLOAD_MIME_TYPES[extension]
  if (!allowedMimeTypes) {
    return '仅支持 JPG、JPEG、PNG、GIF、WEBP、MP4、PDF、ZIP 或 TXT 格式'
  }
  if (file.type && !allowedMimeTypes.has(file.type.toLowerCase())) {
    return '文件扩展名与实际类型不一致，请选择正确的文件'
  }
  if (file.size > MAX_UPLOAD_FILE_SIZE) {
    return '文件大小不能超过 50MB'
  }
  return null
}

export function getUploadErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  return '上传失败，请稍后重试'
}
