import Cookies from 'js-cookie'

const TokenKey = 'Aurora-Admin-Token'
const REMEMBER_ME_DAYS = 3

export function setToken(token: string, rememberMe = false) {
  return Cookies.set(TokenKey, token, {
    expires: rememberMe ? REMEMBER_ME_DAYS : undefined,
    path: '/',
    sameSite: 'lax',
    secure: window.location.protocol === 'https:'
  })
}

export function getToken() {
  return Cookies.get(TokenKey)
}

export function removeToken() {
  return Cookies.remove(TokenKey, { path: '/' })
}
