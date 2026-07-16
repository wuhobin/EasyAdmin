import router from '@/router'
import { usePermissionStore } from '@/store/modules/permission'
import { useUserStore, useSettingsStore } from '@/store'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
NProgress.configure({ showSpinner: false })

const whiteList = ['/login'] // 路由白名单
let initializationPromise: Promise<void> | null = null
let initializationToken: string | undefined

function registerAccessRoutes(accessRoutes: any[]) {
  accessRoutes.forEach((route: any) => {
    if (!route || typeof route !== 'object') {
      console.error('Invalid route object:', route)
      return
    }
    if (!route.meta?.isExternal) {
      router.addRoute(route)
    }
  })
}

function initializePermission(token: string) {
  if (!initializationPromise || initializationToken !== token) {
    initializationToken = token
    initializationPromise = (async () => {
      const userStore = useUserStore()
      const permissionStore = usePermissionStore()

      await userStore.getUserInfo()
      const accessRoutes = await permissionStore.generateRoutes()
      if (Array.isArray(accessRoutes)) {
        registerAccessRoutes(accessRoutes)
      }
      userStore.markInitialized()
    })().finally(() => {
      if (initializationToken === token) {
        initializationPromise = null
        initializationToken = undefined
      }
    })
  }

  return initializationPromise
}

export function setupPermission() {
  router.beforeEach(async (to, from, next) => {
    const dynamicTitle = useSettingsStore().dynamicTitle
    if (dynamicTitle && to.meta.title) {
      document.title = to.meta.title as string
    }
    NProgress.start();
    const hasToken = getToken();
    
    if (hasToken) {
      if (to.path === "/login") {
        // 如果已登录，跳转首页
        next({ path: "/" });
        NProgress.done();
      } else {
        const userStore = useUserStore();
        
        // 判断是否已经获取过用户信息
        if (!userStore.initialized) {
          try {
            await initializePermission(hasToken)
            next({ ...to, replace: true });
          } catch (error) {
            console.error('Permission error:', error);
            // 移除 token 并跳转登录页
            userStore.forceLogout();
            next(`/login`);
            NProgress.done();
          }
        } else {
          // 已经有用户信息，直接放行
          next();
        }
      }
    } else {
      // 未登录可以访问白名单页面
      if (whiteList.indexOf(to.path) !== -1) {
        next();
      } else {
        next(`/login`);
        NProgress.done();
      }
    }
  });

  router.afterEach(() => {
    NProgress.done();
    setTimeout(() => {
    }, 300)
  });
}

