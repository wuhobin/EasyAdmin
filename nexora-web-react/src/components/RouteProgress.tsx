import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'

export function RouteProgress() {
  const location = useLocation()
  const [progress, setProgress] = useState(0)
  const timers = useRef<number[]>([])

  useEffect(() => {
    timers.current.forEach(window.clearTimeout)
    timers.current = []
    setProgress(16)
    const frame = window.requestAnimationFrame(() => setProgress(68))
    timers.current.push(
      window.setTimeout(() => setProgress(90), 120),
      window.setTimeout(() => setProgress(100), 260),
      window.setTimeout(() => setProgress(0), 440)
    )
    return () => {
      window.cancelAnimationFrame(frame)
      timers.current.forEach(window.clearTimeout)
    }
  }, [location.hash, location.pathname, location.search])

  const progressProps = progress
    ? { role: 'progressbar', 'aria-label': '页面加载进度', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': progress }
    : { 'aria-hidden': true as const }

  return <div className="route-progress-region" role="region" aria-label="页面加载状态"><div className={`route-progress ${progress ? 'is-visible' : ''}`} {...progressProps}><span style={{ width: `${progress}%` }} /></div></div>
}
