import { defineStore } from 'pinia'
import {
  getPublicConfigApi,
  type SysConfigPublic
} from '@/api/system/config'

export const DEFAULT_PUBLIC_CONFIG: SysConfigPublic = {
  system: {
    siteName: 'NEXORA ADMIN',
    shortTitle: 'NEXORA ADMIN 后台管理',
    siteDescription: '一个现代化的后台管理系统',
    siteLogo: '',
    copyright: 'Copyright © 2026 Nexora Admin',
    icp: '',
    watermarkEnabled: false,
    watermarkType: 'username_time',
    watermarkCustomText: '',
    watermarkOpacity: 0.15
  },
  register: {
    enabled: true,
    captchaEnabled: true,
    verifyEmail: true,
    needAudit: false
  },
  login: {
    rememberMeEnabled: true
  },
  password: {
    minLength: 6,
    maxLength: 20,
    requireUppercase: false,
    requireLowercase: false,
    requireNumber: false,
    requireSpecial: false
  }
}

function cloneDefaults(): SysConfigPublic {
  return structuredClone(DEFAULT_PUBLIC_CONFIG)
}

export type PublicConfigLoadStatus = 'idle' | 'loading' | 'success' | 'error'

let pendingLoad: Promise<boolean> | undefined

export const usePublicConfigStore = defineStore('publicConfig', {
  state: () => ({
    config: cloneDefaults(),
    status: 'idle' as PublicConfigLoadStatus
  }),

  getters: {
    loaded: state => state.status === 'success',
    loading: state => state.status === 'loading',
    system: state => state.config.system,
    register: state => state.config.register,
    login: state => state.config.login,
    password: state => state.config.password
  },

  actions: {
    async load(force = false): Promise<boolean> {
      if (this.loaded && !force) return true
      if (pendingLoad) return pendingLoad

      pendingLoad = this.fetchConfig()
      try {
        return await pendingLoad
      } finally {
        pendingLoad = undefined
      }
    },

    async fetchConfig(): Promise<boolean> {
      this.status = 'loading'
      try {
        const { data } = await getPublicConfigApi()
        this.config = data
        this.status = 'success'
        this.applyDocumentTitle()
        return true
      } catch (error) {
        this.status = 'error'
        this.applyDocumentTitle()
        console.warn('加载公共系统配置失败', error)
        return false
      }
    },

    applyDocumentTitle() {
      document.title = this.config.system.shortTitle || this.config.system.siteName
    }
  }
})
