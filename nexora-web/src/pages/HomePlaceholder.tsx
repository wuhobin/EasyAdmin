import { ArrowRightOutlined, CheckCircleOutlined, ClockCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons'

export function HomePlaceholder() {
  return <section className="home-page">
    <div className="page-heading-row"><div><span className="page-eyebrow">NEXORA / OVERVIEW</span><h1>工作台</h1><p>管理工作台的 React 基础已经准备好。</p></div><span className="page-heading-mark">01</span></div>
    <div className="home-card-grid">
      <article className="home-feature-card home-feature-card-violet"><div className="card-icon"><SafetyCertificateOutlined /></div><span>SESSION</span><strong>认证链路已接通</strong><p>登录、会话恢复与 401 失效处理保持现有后端契约。</p><ArrowRightOutlined className="card-arrow" /></article>
      <article className="home-feature-card"><div className="card-icon"><CheckCircleOutlined /></div><span>ROUTES</span><strong>动态菜单已就绪</strong><p>权限路由将由当前用户的菜单树驱动，并支持迁移占位。</p><ArrowRightOutlined className="card-arrow" /></article>
      <article className="home-feature-card"><div className="card-icon"><ClockCircleOutlined /></div><span>MANAGEMENT</span><strong>权限管理已迁入</strong><p>用户、角色、菜单和字典管理现已使用 React，并支持前端锁屏。</p><ArrowRightOutlined className="card-arrow" /></article>
    </div>
  </section>
}
