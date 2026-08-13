import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import { useEffect, useState } from 'react'
import { acknowledgeAnnouncementsApi, getPendingAnnouncementsApi, type NoticeRecord } from '@/api/notice'
import { NoticeContent } from '@/components/notifications/NoticeContent'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useAuthStore } from '@/store/authStore'

function announcementId(item: NoticeRecord) {
  return item.noticeId ?? item.id
}

export function AnnouncementPrompt() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const userId = useAuthStore(state => state.user.id)
  const [expandedIds, setExpandedIds] = useState<number[]>([])
  const query = useQuery({ queryKey: ['pending-announcements', userId], queryFn: async () => (await getPendingAnnouncementsApi()).data, enabled: userId !== null, staleTime: Number.POSITIVE_INFINITY, retry: false })
  const announcements = query.data ?? []

  useEffect(() => {
    if (announcements.length && !expandedIds.length) setExpandedIds([announcementId(announcements[0])])
  }, [announcements, expandedIds.length])

  const acknowledgeMutation = useMutation({
    mutationFn: () => acknowledgeAnnouncementsApi(announcements.map(announcementId)),
    onSuccess: async () => {
      queryClient.setQueryData<NoticeRecord[]>(['pending-announcements', userId], [])
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['notice-unread-count'] }),
        queryClient.invalidateQueries({ queryKey: ['my-notices'] })
      ])
      window.dispatchEvent(new Event('nexora:notice-read-changed'))
    },
    onError: () => message.error('公告确认失败，请稍后重试')
  })

  return (
    <Dialog open={announcements.length > 0}>
      <DialogContent className="max-w-[720px]" hideCloseButton onEscapeKeyDown={event => event.preventDefault()} onPointerDownOutside={event => event.preventDefault()} onInteractOutside={event => event.preventDefault()}>
        <DialogHeader><DialogTitle>系统公告</DialogTitle><DialogDescription className="announcement-description">以下公告需要确认后关闭。</DialogDescription></DialogHeader>
        <div className="announcement-list">
          {announcements.map(item => {
            const id = announcementId(item)
            const expanded = expandedIds.includes(id)
            return <section className="announcement-item" key={id}><button type="button" aria-expanded={expanded} aria-controls={`announcement-${id}`} onClick={() => setExpandedIds(current => expanded ? current.filter(value => value !== id) : [...current, id])}><span>{item.title}</span><time>{item.publishTime || ''}</time><i aria-hidden="true">{expanded ? '−' : '+'}</i></button>{expanded ? <div id={`announcement-${id}`} className="announcement-content"><NoticeContent notice={item} /></div> : null}</section>
          })}
        </div>
        <DialogFooter><Button className="announcement-confirm-button" type="button" loading={acknowledgeMutation.isPending} onClick={() => acknowledgeMutation.mutate()}>我知道了</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
