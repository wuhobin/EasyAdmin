import Watermark from 'antd/es/watermark'
import { useEffect, useState, type ReactNode } from 'react'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { useSettingsStore } from '@/store/settingsStore'
import { buildWatermarkColor, buildWatermarkContent } from '@/utils/watermark'

export function ContentWatermark({ children }: { children: ReactNode }) {
  const system = usePublicConfigStore(state => state.config.system)
  const configStatus = usePublicConfigStore(state => state.status)
  const userName = useAuthStore(state => state.user.nickname || state.user.email || 'Nexora User')
  const theme = useSettingsStore(state => state.theme)
  const [currentTime, setCurrentTime] = useState(() => new Date())

  useEffect(() => {
    if (configStatus === 'idle') void usePublicConfigStore.getState().load()
  }, [configStatus])

  useEffect(() => {
    if (!system.watermarkEnabled || system.watermarkType !== 'username_time') return
    setCurrentTime(new Date())
    const timer = window.setInterval(() => setCurrentTime(new Date()), 60_000)
    return () => window.clearInterval(timer)
  }, [system.watermarkEnabled, system.watermarkType])

  if (!system.watermarkEnabled) return children

  const content = buildWatermarkContent(system, userName, currentTime)
  return (
    <Watermark
      className="app-watermark"
      content={content}
      rotate={-15}
      width={200}
      height={Array.isArray(content) ? 48 : 24}
      gap={[160, 142]}
      zIndex={2}
      font={{
        color: buildWatermarkColor(theme, system.watermarkOpacity),
        fontSize: 13,
        fontWeight: 500,
        fontFamily: 'Inter, "Noto Sans SC", "PingFang SC", sans-serif'
      }}
    >
      {children}
    </Watermark>
  )
}
