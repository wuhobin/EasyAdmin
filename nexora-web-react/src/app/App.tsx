import { Navigate, Outlet, useRoutes, type RouteObject } from 'react-router-dom'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { AppShell } from '@/layout/AppShell'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { createDynamicRouteObjects } from '@/routes/routeObjects'
import { useRouteStore } from '@/store/routeStore'
import { useAuthBootstrap } from '@/app/useAuthBootstrap'
import { HOME_PATH } from '@/routes/routeAdapter'
import { getToken } from '@/utils/token'
import { isUnauthorizedError } from '@/types/api'

function AppLoading() {
  return <div className="app-loading"><span className="app-loading__spinner" aria-label="正在加载" /></div>
}

function PublicOnly() {
  return getToken() ? <Navigate to={HOME_PATH} replace /> : <Outlet />
}

function AuthGate() {
  const state = useAuthBootstrap()
  if (state.status === 'unauthenticated') return <Navigate to="/login" replace />
  if (state.status === 'loading') return <AppLoading />
  if (state.status === 'error') {
    if (isUnauthorizedError(state.error)) return <Navigate to="/login" replace />
    return <div className="app-loading"><div><strong>工作台初始化失败</strong><p>请刷新页面后重试。</p></div></div>
  }
  return <Outlet />
}

function DynamicRouteFallback() {
  const ready = useRouteStore(state => state.ready)
  return ready ? <NotFoundPage /> : <AppLoading />
}

export function App() {
  const routes = useRouteStore(state => state.routes)
  const dynamicRoutes = createDynamicRouteObjects(routes)
  const routeConfig: RouteObject[] = [
    {
      element: <PublicOnly />,
      children: [
        { path: 'login', element: <LoginPage /> },
        { path: 'register', element: <RegisterPage /> },
        { path: 'forgot-password', element: <ForgotPasswordPage /> }
      ]
    },
    {
      element: <AuthGate />,
      children: [{ element: <AppShell />, children: [...dynamicRoutes, { path: '*', element: <DynamicRouteFallback /> }] }]
    },
    { path: '/', element: <Navigate to={HOME_PATH} replace /> },
    { path: '*', element: <NotFoundPage /> }
  ]
  return useRoutes(routeConfig)
}
