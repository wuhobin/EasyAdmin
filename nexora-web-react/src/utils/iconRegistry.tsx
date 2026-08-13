import type { ComponentType, CSSProperties } from 'react'
import {
  AccountBookOutlined,
  AppstoreOutlined,
  ApartmentOutlined,
  AuditOutlined,
  BarChartOutlined,
  BellOutlined,
  BookOutlined,
  CalendarOutlined,
  CloudServerOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  FileOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FundOutlined,
  HistoryOutlined,
  IdcardOutlined,
  InboxOutlined,
  KeyOutlined,
  MailOutlined,
  MenuOutlined,
  MonitorOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined
} from '@ant-design/icons'

export type IconComponent = ComponentType<{ className?: string; style?: CSSProperties }>

export const antDesignIcons: Record<string, IconComponent> = {
  AccountBookOutlined,
  AppstoreOutlined,
  ApartmentOutlined,
  AuditOutlined,
  BarChartOutlined,
  BellOutlined,
  BookOutlined,
  CalendarOutlined,
  CloudServerOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  FileOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FundOutlined,
  HistoryOutlined,
  IdcardOutlined,
  InboxOutlined,
  KeyOutlined,
  MailOutlined,
  MenuOutlined,
  MonitorOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  SettingOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined
}

const legacyIconAliases: Record<string, string> = {
  Orange: 'DashboardOutlined',
  Setting: 'SettingOutlined',
  Avatar: 'UserOutlined',
  User: 'UserOutlined',
  UserFilled: 'TeamOutlined',
  Menu: 'MenuOutlined',
  Monitor: 'MonitorOutlined',
  Message: 'MailOutlined',
  MessageBox: 'InboxOutlined',
  Folder: 'FolderOpenOutlined',
  FolderOpened: 'FolderOpenOutlined',
  Document: 'FileOutlined',
  DocumentCopy: 'FileTextOutlined',
  Files: 'FileOutlined',
  Memo: 'DatabaseOutlined',
  AlarmClock: 'ScheduleOutlined',
  Operation: 'AuditOutlined',
  Tickets: 'AccountBookOutlined',
  Tools: 'ToolOutlined',
  CircleCheckFilled: 'HistoryOutlined',
  Platform: 'CloudServerOutlined',
  Bell: 'BellOutlined',
  Lock: 'SafetyOutlined',
  Key: 'KeyOutlined'
}

export function resolveIconName(value?: string | null) {
  if (!value) return 'AppstoreOutlined'
  if (value.startsWith('antd:')) return value.slice(5) || 'AppstoreOutlined'
  return legacyIconAliases[value] || value
}

export function resolveIcon(value?: string | null): IconComponent {
  return antDesignIcons[resolveIconName(value)] || AppstoreOutlined
}

export function resolveRegisteredIcon(value?: string | null): IconComponent | undefined {
  return antDesignIcons[resolveIconName(value)]
}

export function isAntDesignIcon(value?: string | null) {
  return Boolean(value?.startsWith('antd:'))
}
