// aurora-web 登录 token 工具 —— localStorage 实现
// 兼容老用户：cookie 中残留的 Aurora-Admin-Token 会在 getToken 首次调用时一次性迁移

const TokenKey = 'aurora_admin_token'

// 设置 token
export function setToken(token: string) {
  try {
    localStorage.setItem(TokenKey, token)
  } catch (_) {
    // localStorage 在隐私模式或 quota 满时可能抛错；登录流程继续，刷新即掉
  }
}

// 读取 token；首次访问时把老 cookie 里的 Aurora-Admin-Token 搬到 localStorage
export function getToken(): string | undefined {
  try {
    const legacy = readCookie('Aurora-Admin-Token')
    if (legacy) {
      localStorage.setItem(TokenKey, legacy)
      eraseCookie('Aurora-Admin-Token')
    }
  } catch (_) {
    // 吞掉；新用户流程不受影响
  }
  return localStorage.getItem(TokenKey) ?? undefined
}

// 删除 token
export function removeToken() {
  localStorage.removeItem(TokenKey)
}

// ---- 内部：仅用于迁移期读 / 删老 cookie ----
function readCookie(name: string): string | null {
  const m = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'))
  return m ? decodeURIComponent(m[2]) : null
}

function eraseCookie(name: string) {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
}
