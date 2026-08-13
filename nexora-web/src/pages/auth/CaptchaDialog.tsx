import { ReloadOutlined } from '@ant-design/icons'
import * as Dialog from '@radix-ui/react-dialog'
import { useEffect, useRef, useState } from 'react'
import { generateImageCaptchaApi, matchImageCaptchaApi, type ImageCaptchaResult, type ImageCaptchaTrack, type ImageCaptchaTrackPoint } from '@/api/auth'

interface CaptchaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess: (captchaId: string) => void
}

type CaptchaStatus = 'idle' | 'loading' | 'dragging' | 'matching' | 'success' | 'error'

export function CaptchaDialog({ open, onOpenChange, onSuccess }: CaptchaDialogProps) {
  const [captcha, setCaptcha] = useState<ImageCaptchaResult>()
  const [status, setStatus] = useState<CaptchaStatus>('idle')
  const [error, setError] = useState('')
  const [imagesReady, setImagesReady] = useState(false)
  const [left, setLeft] = useState(0)
  const [dragLimit, setDragLimit] = useState(0)
  const bgRef = useRef<HTMLImageElement>(null)
  const pieceRef = useRef<HTMLImageElement>(null)
  const trackRef = useRef<HTMLDivElement>(null)
  const thumbRef = useRef<HTMLButtonElement>(null)
  const pointerId = useRef<number>()
  const dragStartX = useRef(0)
  const dragStartY = useRef(0)
  const dragStartTime = useRef(0)
  const trackList = useRef<ImageCaptchaTrackPoint[]>([])
  const renderedDimensions = useRef<Pick<ImageCaptchaTrack, 'bgImageWidth' | 'bgImageHeight' | 'templateImageWidth' | 'templateImageHeight'>>()

  const loadChallenge = async () => {
    setStatus('loading')
    setError('')
    setImagesReady(false)
    setLeft(0)
    try {
      const { data } = await generateImageCaptchaApi()
      if (data.type.toUpperCase() !== 'SLIDER') throw new Error('unsupported')
      setCaptcha(data)
      setStatus('idle')
    } catch {
      setError('验证图片加载失败，请刷新后重试')
      setStatus('error')
    }
  }

  useEffect(() => {
    if (open) void loadChallenge()
    else {
      setCaptcha(undefined)
      setError('')
      setStatus('idle')
      setImagesReady(false)
      setLeft(0)
    }
  }, [open])

  const measure = () => {
    const bg = bgRef.current
    const piece = pieceRef.current
    const track = trackRef.current
    const thumb = thumbRef.current
    if (!bg || !piece || !track || !thumb || !bg.complete || !piece.complete || bg.naturalWidth === 0 || piece.naturalWidth === 0) return
    const bgRect = bg.getBoundingClientRect()
    const pieceRect = piece.getBoundingClientRect()
    const trackRect = track.getBoundingClientRect()
    const thumbRect = thumb.getBoundingClientRect()
    renderedDimensions.current = {
      bgImageWidth: Math.round(bgRect.width),
      bgImageHeight: Math.round(bgRect.height),
      templateImageWidth: Math.round(pieceRect.width),
      templateImageHeight: Math.round(pieceRect.height)
    }
    setDragLimit(Math.max(0, Math.min(bgRect.width - pieceRect.width + 5, trackRect.width - thumbRect.width)))
    setImagesReady(true)
  }

  const appendPoint = (event: React.PointerEvent, type: ImageCaptchaTrackPoint['type']) => {
    trackList.current.push({ x: type === 'DOWN' ? 0 : left, y: event.pageY - dragStartY.current, t: Math.max(0, Date.now() - dragStartTime.current), type })
  }

  const handleDown = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (!imagesReady || status !== 'idle' || !renderedDimensions.current || !event.isPrimary) return
    pointerId.current = event.pointerId
    dragStartX.current = event.pageX
    dragStartY.current = event.pageY
    dragStartTime.current = Date.now()
    trackList.current = []
    event.currentTarget.setPointerCapture(event.pointerId)
    appendPoint(event, 'DOWN')
    setStatus('dragging')
  }

  const handleMove = (event: React.PointerEvent<HTMLButtonElement>) => {
    if (pointerId.current !== event.pointerId || status !== 'dragging') return
    setLeft(Math.min(Math.max(0, event.pageX - dragStartX.current), dragLimit))
    appendPoint(event, 'MOVE')
  }

  const handleUp = async (event: React.PointerEvent<HTMLButtonElement>) => {
    if (pointerId.current !== event.pointerId || status !== 'dragging' || !captcha || !renderedDimensions.current) return
    pointerId.current = undefined
    const stopTime = Date.now()
    const finalLeft = Math.min(Math.max(0, event.pageX - dragStartX.current), dragLimit)
    setLeft(finalLeft)
    appendPoint(event, 'UP')
    if (stopTime - dragStartTime.current < 300 || trackList.current.length < 10) {
      setStatus('error')
      window.setTimeout(() => { if (open) void loadChallenge() }, 700)
      return
    }
    setStatus('matching')
    try {
      const { data: matched } = await matchImageCaptchaApi(captcha.id, {
        ...renderedDimensions.current,
        startTime: dragStartTime.current,
        stopTime,
        left: Math.round(finalLeft),
        top: 0,
        trackList: [...trackList.current],
        data: captcha.data
      })
      if (matched === true) {
        setStatus('success')
        onSuccess(captcha.id)
        return
      }
    } catch {
      // The challenge is refreshed below so the user can retry.
    }
    setStatus('error')
    window.setTimeout(() => { if (open) void loadChallenge() }, 700)
  }

  const statusLabel = status === 'success' ? '验证通过' : status === 'matching' ? '正在核验轨迹' : status === 'error' ? '位置不正确，正在刷新' : imagesReady ? '按住滑块向右拖动' : '正在准备验证图片'

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="captcha-overlay" />
        <Dialog.Content className="captcha-dialog" onOpenAutoFocus={event => event.preventDefault()}>
          <Dialog.Title className="captcha-title">安全验证</Dialog.Title>
          <Dialog.Description className="captcha-description">拖动拼图，确认本次注册由你发起</Dialog.Description>
          <button className="captcha-close" type="button" aria-label="关闭验证" onClick={() => onOpenChange(false)}>×</button>
          <div className="captcha-toolbar"><span>{statusLabel}</span><button type="button" aria-label="刷新验证码" onClick={() => void loadChallenge()} disabled={status === 'loading' || status === 'matching'}><ReloadOutlined /></button></div>
          <div className="captcha-stage">
            {captcha ? <>
              <img ref={bgRef} src={captcha.backgroundImage} alt="" width={captcha.backgroundImageWidth} height={captcha.backgroundImageHeight} draggable="false" onLoad={measure} />
              <div className="captcha-piece" style={{ transform: `translate3d(${left}px, 0, 0)` }}><img ref={pieceRef} src={captcha.templateImage} alt="" width={captcha.templateImageWidth} height={captcha.templateImageHeight} draggable="false" onLoad={measure} /></div>
            </> : null}
            {status === 'loading' || !imagesReady ? <div className="captcha-loading">加载验证图片…</div> : null}
          </div>
          <div ref={trackRef} className={`captcha-track ${status}`}>
            <div className="captcha-track-fill" style={{ width: `${left + 28}px` }} />
            <span>{statusLabel}</span>
            <button ref={thumbRef} className="captcha-thumb" type="button" aria-label="按住并向右拖动滑块完成拼图" disabled={!imagesReady || status !== 'idle' && status !== 'dragging'} style={{ transform: `translate3d(${left}px,0,0)` }} onPointerDown={handleDown} onPointerMove={handleMove} onPointerUp={handleUp} onPointerCancel={() => { pointerId.current = undefined; setLeft(0); setStatus('idle') }}><span>{status === 'success' ? '✓' : '››'}</span></button>
          </div>
          <p className={`captcha-status ${status}`} role="status">{error || (status === 'success' ? '图片验证已通过' : status === 'matching' ? '正在核验你的操作轨迹' : '')}</p>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
