import { Link } from 'react-router-dom'
import { HOME_PATH } from '@/routes/routeAdapter'

export function NotFoundPage() {
  return <section className="empty-state-page"><span className="page-eyebrow">NEXORA / 404</span><h1>页面不存在</h1><p>这个地址没有对应的工作台页面。</p><Link className="soft-action-button" to={HOME_PATH}>返回工作台</Link></section>
}

export function ForbiddenPage() {
  return <section className="empty-state-page"><span className="page-eyebrow">NEXORA / 403</span><h1>没有访问权限</h1><p>当前账号没有访问这个页面的权限，请联系管理员。</p><Link className="soft-action-button" to={HOME_PATH}>返回工作台</Link></section>
}
