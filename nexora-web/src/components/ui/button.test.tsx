import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { Button } from '@/components/ui/button'

describe('Button', () => {
  it('renders a link through asChild without violating the Slot child contract', () => {
    const markup = renderToStaticMarkup(
      <Button variant="outline" asChild>
        <a href="/system/log/job-log">
          <span>调度日志</span>
        </a>
      </Button>
    )

    expect(markup).toContain('href="/system/log/job-log"')
    expect(markup).toContain('调度日志')
  })

  it('keeps the loading indicator when asChild is enabled', () => {
    const markup = renderToStaticMarkup(
      <Button loading asChild>
        <a href="/monitor/job">定时任务</a>
      </Button>
    )

    expect(markup).toContain('aria-busy="true"')
    expect(markup).toContain('animate-spin')
    expect(markup).toContain('定时任务')
  })
})
