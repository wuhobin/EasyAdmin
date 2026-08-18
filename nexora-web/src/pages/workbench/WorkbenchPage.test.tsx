/* @vitest-environment jsdom */

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WorkbenchPage } from '@/pages/workbench/WorkbenchPage'
import { useAuthStore } from '@/store/authStore'
import { useRouteStore } from '@/store/routeStore'

;(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

vi.mock('@/api/notice', () => ({
  getMyNoticeDetailApi: vi.fn(),
  getMyNoticeListApi: vi.fn(async () => ({ data: { records: [] } })),
  getUnreadNoticeCountApi: vi.fn(async () => ({ data: { unreadCount: 0 } })),
  markNoticeReadApi: vi.fn()
}))
vi.mock('@/api/workbench', () => ({
  getWorkbenchSummaryApi: vi.fn(async () => ({ data: { administrator: false, accessibleFeatureCount: 0, roleCount: 0, permissionCount: 0 } }))
}))

describe('WorkbenchPage', () => {
  let container: HTMLDivElement
  let root: Root

  beforeEach(() => {
    container = document.createElement('div')
    document.body.appendChild(container)
    root = createRoot(container)
    useAuthStore.setState({
      user: { id: 1, email: '', nickname: '微信用户', avatar: null, roles: [], permissions: [] },
      initialized: true
    })
    useRouteStore.setState({ routes: [], ready: true })
  })

  afterEach(async () => {
    await act(async () => root.unmount())
    container.remove()
    vi.clearAllMocks()
  })

  it('opens the email binding dialog on the workbench without navigating away', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    await act(async () => {
      root.render(
        <MemoryRouter initialEntries={['/']} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <AntApp>
            <QueryClientProvider client={queryClient}>
              <WorkbenchPage />
            </QueryClientProvider>
          </AntApp>
        </MemoryRouter>
      )
    })

    const bindButton = Array.from(document.querySelectorAll('button')).find(button => button.textContent === '去绑定')
    expect(bindButton).toBeDefined()

    await act(async () => bindButton?.click())
    await act(async () => {
      await vi.dynamicImportSettled()
      await new Promise(resolve => window.setTimeout(resolve, 0))
    })

    expect(document.body.textContent).toContain('绑定登录邮箱')
    expect(document.body.textContent).toContain('发送验证码')
    expect(document.body.textContent).toContain('完成绑定')
    expect(window.location.pathname).toBe('/')
  })
})
