import { usePublicConfigStore } from '@/store/publicConfigStore'

export function AppFooter() {
  const system = usePublicConfigStore(state => state.config.system)
  return <footer className="app-footer"><div className="app-footer-meta"><span>{system.copyright || `Copyright © ${new Date().getFullYear()} ${system.siteName}`}</span>{system.icp ? <span className="app-footer-icp">{system.icp}</span> : null}</div></footer>
}
