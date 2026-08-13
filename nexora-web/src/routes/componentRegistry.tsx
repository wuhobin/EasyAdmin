import type { ComponentType } from 'react'
import { lazy } from 'react'
import { HomePlaceholder } from '@/pages/HomePlaceholder'
import { MigrationPlaceholder } from '@/pages/MigrationPlaceholder'
import { HOME_PATH, PROFILE_PATH, type MenuRoute } from '@/routes/routeAdapter'

const MenuManagementPage = lazy(async () => {
  const module = await import('@/pages/system/MenuManagementPage')
  return { default: module.MenuManagementPage }
})

const UserManagementPage = lazy(async () => {
  const module = await import('@/pages/system/UserManagementPage')
  return { default: module.UserManagementPage }
})

const RoleManagementPage = lazy(async () => {
  const module = await import('@/pages/system/RoleManagementPage')
  return { default: module.RoleManagementPage }
})

const DictManagementPage = lazy(async () => {
  const module = await import('@/pages/system/DictManagementPage')
  return { default: module.DictManagementPage }
})

const ConfigManagementPage = lazy(async () => {
  const module = await import('@/pages/system/ConfigManagementPage')
  return { default: module.ConfigManagementPage }
})

const OperationLogPage = lazy(async () => {
  const module = await import('@/pages/system/OperationLogPage')
  return { default: module.OperationLogPage }
})

const JobLogPage = lazy(async () => {
  const module = await import('@/pages/monitor/JobLogPage')
  return { default: module.JobLogPage }
})

const ServerManagementPage = lazy(async () => {
  const module = await import('@/pages/monitor/ServerManagementPage')
  return { default: module.ServerManagementPage }
})

const OnlineUsersPage = lazy(async () => {
  const module = await import('@/pages/monitor/OnlineUsersPage')
  return { default: module.OnlineUsersPage }
})

const ScheduledJobsPage = lazy(async () => {
  const module = await import('@/pages/monitor/ScheduledJobsPage')
  return { default: module.ScheduledJobsPage }
})

const NoticeManagementPage = lazy(async () => {
  const module = await import('@/pages/system/NoticeManagementPage')
  return { default: module.NoticeManagementPage }
})

const LatestMailPage = lazy(async () => {
  const module = await import('@/pages/mail/LatestMailPage')
  return { default: module.LatestMailPage }
})

const MailAccountsPage = lazy(async () => {
  const module = await import('@/pages/mail/MailAccountsPage')
  return { default: module.MailAccountsPage }
})

const FileListPage = lazy(async () => {
  const module = await import('@/pages/file/FileListPage')
  return { default: module.FileListPage }
})

const ProfilePage = lazy(async () => {
  const module = await import('@/pages/profile/ProfilePage')
  return { default: module.ProfilePage }
})

const registry: Record<string, ComponentType<{ title?: string }>> = {
  '/system/menu/index': MenuManagementPage,
  '/system/menu': MenuManagementPage,
  '/system/user/index': UserManagementPage,
  '/system/user': UserManagementPage,
  '/system/role/index': RoleManagementPage,
  '/system/role': RoleManagementPage,
  '/system/dict/index': DictManagementPage,
  '/system/dict': DictManagementPage,
  '/system/config/index': ConfigManagementPage,
  '/system/config': ConfigManagementPage,
  '/system/log/operation/index': OperationLogPage,
  '/system/log/operation': OperationLogPage,
  '/monitor/job/log': JobLogPage,
  '/system/log/job-log': JobLogPage,
  '/monitor/server/index': ServerManagementPage,
  '/monitor/server': ServerManagementPage,
  '/monitor/online/index': OnlineUsersPage,
  '/monitor/online': OnlineUsersPage,
  '/monitor/job/index': ScheduledJobsPage,
  '/monitor/job': ScheduledJobsPage,
  '/system/notice/index': NoticeManagementPage,
  '/system/notice': NoticeManagementPage,
  '/mail/index': LatestMailPage,
  '/mail': LatestMailPage,
  '/mail/account/index': MailAccountsPage,
  '/mail/account': MailAccountsPage,
  '/file/index': FileListPage,
  '/file': FileListPage,
  '/system/user/profile/index': ProfilePage,
  '/system/profile': ProfilePage,
  [PROFILE_PATH]: ProfilePage,
  [HOME_PATH]: HomePlaceholder,
  home: HomePlaceholder
}

export function resolveRouteComponent(route: MenuRoute): ComponentType<{ title?: string }> {
  return registry[route.component] || registry[route.fullPath] || (route.fullPath === HOME_PATH ? HomePlaceholder : MigrationPlaceholder)
}

export function registerRouteComponent(key: string, component: ComponentType<{ title?: string }>) {
  registry[key] = component
}
