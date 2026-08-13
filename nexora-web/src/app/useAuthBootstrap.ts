import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getRoutersApi, getUserInfoApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useRouteStore } from '@/store/routeStore'
import { getToken } from '@/utils/token'

export function useAuthBootstrap() {
  const token = getToken()
  const initialized = useAuthStore(state => state.initialized)
  const setUser = useAuthStore(state => state.setUser)
  const markInitialized = useAuthStore(state => state.markInitialized)
  const setRoutes = useRouteStore(state => state.setRoutes)
  const routesReady = useRouteStore(state => state.ready)
  const query = useQuery({
    queryKey: ['auth-bootstrap', token],
    enabled: Boolean(token) && !initialized,
    staleTime: Number.POSITIVE_INFINITY,
    retry: false,
    queryFn: async () => {
      const [userResponse, routeResponse] = await Promise.all([getUserInfoApi(), getRoutersApi()])
      return { user: userResponse.data, routes: routeResponse.data }
    }
  })

  useEffect(() => {
    if (!query.data) return
    setUser(query.data.user)
    setRoutes(query.data.routes)
    markInitialized()
  }, [markInitialized, query.data, setRoutes, setUser])

  if (!token) return { status: 'unauthenticated' as const, error: undefined }
  if (initialized && routesReady) return { status: 'ready' as const, error: undefined }
  if (query.isError) return { status: 'error' as const, error: query.error }
  return { status: 'loading' as const, error: undefined }
}
