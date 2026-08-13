import { describe, expect, it } from 'vitest'
import { filePreviewKind, formatFileSize, validateRenamedFile, validateUploadFile } from '@/pages/file/fileUtils'

describe('file utilities', () => {
  it('validates upload extensions, mime types and size', () => {
    expect(validateUploadFile({ name: 'photo.jpg', size: 1024, type: 'image/jpeg' })).toBeNull()
    expect(validateUploadFile({ name: 'photo.jpg', size: 1024, type: 'image/png' })).toContain('实际类型')
    expect(validateUploadFile({ name: 'script.exe', size: 1024, type: '' })).toContain('仅支持')
    expect(validateUploadFile({ name: 'empty.txt', size: 0, type: 'text/plain' })).toContain('不能为空')
  })

  it('detects preview kinds from mime type and filename', () => {
    expect(filePreviewKind({ fileName: 'image.bin', contentType: 'image/png' })).toBe('image')
    expect(filePreviewKind({ fileName: 'readme.md', contentType: 'application/octet-stream' })).toBe('text')
    expect(filePreviewKind({ fileName: 'archive.zip', contentType: 'application/zip' })).toBe('none')
  })

  it('formats sizes and prevents extension changes', () => {
    expect(formatFileSize(1536)).toBe('1.5 KB')
    expect(validateRenamedFile('report.pdf', 'quarterly.pdf')).toBeNull()
    expect(validateRenamedFile('report.pdf', 'quarterly.txt')).toBe('不能修改文件扩展名')
  })
})
