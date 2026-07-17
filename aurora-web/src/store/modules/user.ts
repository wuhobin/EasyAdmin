import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getUserInfoApi,
  loginApi,
  logoutApi,
  type CurrentUserResult,
  type LoginParams,
} from '@/api/system/auth'
import { resetRouter } from '@/router'
import { store } from '@/store'
import { removeToken, setToken } from '@/utils/auth'

export interface CurrentUser {
  id: number | null
  username: string
  nickname: string | null
  avatar: string | null
  roles: string[]
  permissions: string[]
}

function createEmptyUser(): CurrentUser {
  return {
    id: null,
    username: '',
    nickname: null,
    avatar: null,
    roles: [],
    permissions: []
  }
}

function toCurrentUser(data: CurrentUserResult): CurrentUser {
  return {
    id: data.id,
    username: data.username,
    nickname: data.nickname,
    avatar: data.avatar,
    roles: data.roles ?? [],
    permissions: data.permissions ?? []
  }
}

export const useUserStore = defineStore("user", () => {
  const user = ref<CurrentUser>(createEmptyUser())
  const initialized = ref(false)

  /**
   * 登录
   *
   * @param {LoginData}
   * @returns
   */
  async function login(loginData: LoginParams) {
    const { data } = await loginApi(loginData)
    setToken(data.token, loginData.rememberMe)
  }

  // 获取信息(用户昵称、头像、角色集合、权限集合)
  async function getUserInfo() {
    const { data } = await getUserInfoApi()
    if (!data) {
      throw new Error('Verification failed, please login again.')
    }
    user.value = toCurrentUser(data)
    return data
  }

  function markInitialized() {
    initialized.value = true
  }

  function clearSession() {
    removeToken()
    user.value = createEmptyUser()
    initialized.value = false
    resetRouter()
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearSession()
      location.reload()
    }
  }

  function forceLogout() {
    clearSession()
  }

  return {
    user,
    initialized,
    login,
    getUserInfo,
    markInitialized,
    logout,
    clearSession,
    forceLogout
  }
})

// 非setup
export function useUserStoreHook() {
  return useUserStore(store)
}
