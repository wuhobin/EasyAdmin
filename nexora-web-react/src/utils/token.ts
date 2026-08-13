import Cookies from 'js-cookie'

export const TOKEN_KEY = 'Nexora-Admin-Token'
export const REMEMBER_ME_DAYS = 3

export function setToken(token: string, rememberMe = false) {
  return Cookies.set(TOKEN_KEY, token, {
    expires: rememberMe ? REMEMBER_ME_DAYS : undefined,
    path: '/',
    sameSite: 'lax',
    secure: window.location.protocol === 'https:'
  })
}

export function getToken() {
  return Cookies.get(TOKEN_KEY)
}

export function removeToken() {
  return Cookies.remove(TOKEN_KEY, { path: '/' })
}
