import { ElMessageBox } from 'element-plus'
import router from '@/router'
import { useUserStoreHook } from '@/store/modules/user'
import { registerUnauthorizedHandler } from '@/utils/auth-session'

export function setupAuthSession() {
  registerUnauthorizedHandler(async () => {
    const userStore = useUserStoreHook()
    userStore.forceLogout()

    try {
      await ElMessageBox.alert('登录状态已失效，请重新登录', '提示', {
        confirmButtonText: '确定',
        type: 'warning'
      })
    } catch {
      // 关闭提示后仍需返回登录页
    }

    await router.replace('/login')
  })
}
