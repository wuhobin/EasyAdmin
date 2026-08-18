import { create } from 'zustand'
import { loginApi, type AuthParams, type CurrentUserResult } from '@/api/auth'
import { removeToken, setToken } from '@/utils/token'
import { useLockStore } from '@/store/lockStore'

export interface CurrentUser {
  id: number | null
  email: string
  nickname: string | null
  avatar: string | null
  roles: string[]
  permissions: string[]
}

const emptyUser = (): CurrentUser => ({
  id: null,
  email: '',
  nickname: null,
  avatar: null,
  roles: [],
  permissions: []
})

function toCurrentUser(data: CurrentUserResult): CurrentUser {
  return {
    id: data.id,
    email: data.email ?? '',
    nickname: data.nickname,
    avatar: data.avatar,
    roles: data.roles ?? [],
    permissions: data.permissions ?? []
  }
}

interface AuthState {
  user: CurrentUser
  initialized: boolean
  setUser: (data: CurrentUserResult) => void
  markInitialized: () => void
  login: (data: AuthParams) => Promise<void>
  clearSession: () => void
  hasPermission: (permission: string) => boolean
  hasAnyPermission: (permissions: string[]) => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: emptyUser(),
  initialized: false,
  setUser: data => set({ user: toCurrentUser(data) }),
  markInitialized: () => set({ initialized: true }),
  login: async data => {
    const { data: result } = await loginApi(data)
    setToken(result.token, data.rememberMe)
  },
  clearSession: () => {
    removeToken()
    useLockStore.getState().unlock()
    set({ user: emptyUser(), initialized: false })
  },
  hasPermission: permission => get().user.permissions.includes(permission),
  hasAnyPermission: permissions => permissions.some(permission => get().user.permissions.includes(permission))
}))
