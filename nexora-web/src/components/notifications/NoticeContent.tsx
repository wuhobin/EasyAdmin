import type { NoticeRecord } from '@/api/notice'

export function NoticeContent({ notice, className = '' }: { notice: Pick<NoticeRecord, 'content' | 'contentFormat' | 'title'>; className?: string }) {
  if (notice.contentFormat === 'html') {
    return <iframe className={`notice-html-frame ${className}`.trim()} sandbox="allow-popups" srcDoc={notice.content || ''} title={`${notice.title} HTML 内容`} />
  }
  return <pre className={`notice-text-content ${className}`.trim()}>{notice.content || ''}</pre>
}
