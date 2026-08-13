import { ToolOutlined, ArrowLeftOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import { HOME_PATH } from '@/routes/routeAdapter'

interface MigrationPlaceholderProps { title?: string }

export function MigrationPlaceholder({ title = '页面' }: MigrationPlaceholderProps) {
  return <section className="empty-state-page"><div className="empty-state-icon"><ToolOutlined /></div><span className="page-eyebrow">NEXORA / IN PROGRESS</span><h1>{title}正在迁移</h1><p>这个页面已经接入权限与路由，内容将在后续批次加入 React 工作台。</p><Link className="soft-action-button" to={HOME_PATH}><ArrowLeftOutlined /> 返回工作台</Link></section>
}
