import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ButtonHTMLAttributes, type CSSProperties, type HTMLAttributes, type LiHTMLAttributes } from 'react'

interface SidebarContextValue {
  state: 'expanded' | 'collapsed'
  open: boolean
  setOpen: (value: boolean) => void
  isMobile: boolean
  toggleSidebar: () => void
}

const SidebarContext = createContext<SidebarContextValue | null>(null)

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(false)
  useEffect(() => {
    const media = window.matchMedia('(max-width: 900px)')
    const update = () => setIsMobile(media.matches)
    update()
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])
  return isMobile
}

export function useSidebar() {
  const context = useContext(SidebarContext)
  if (!context) throw new Error('useSidebar must be used within a SidebarProvider.')
  return context
}

export function SidebarProvider({ children, defaultOpen = true, open: openProp, onOpenChange, className = '', style, ...props }: HTMLAttributes<HTMLDivElement> & { defaultOpen?: boolean; open?: boolean; onOpenChange?: (open: boolean) => void }) {
  const isMobile = useIsMobile()
  const [internalOpen, setInternalOpen] = useState(defaultOpen)
  const open = openProp ?? internalOpen
  const setOpen = useCallback((next: boolean) => {
    if (openProp === undefined) setInternalOpen(next)
    onOpenChange?.(next)
    try { document.cookie = `sidebar_state=${next}; path=/; max-age=604800` } catch { /* Cookie persistence is optional. */ }
  }, [onOpenChange, openProp])
  const toggleSidebar = useCallback(() => setOpen(!open), [open, setOpen])
  const contextValue = useMemo(() => ({ state: open ? 'expanded' as const : 'collapsed' as const, open, setOpen, isMobile, toggleSidebar }), [isMobile, open, setOpen, toggleSidebar])

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === 'b' && (event.ctrlKey || event.metaKey)) {
        event.preventDefault()
        toggleSidebar()
      }
    }
    window.addEventListener('keydown', handleShortcut)
    return () => window.removeEventListener('keydown', handleShortcut)
  }, [toggleSidebar])

  return <SidebarContext.Provider value={contextValue}><div data-slot="sidebar-wrapper" className={`sidebar-provider ${className}`.trim()} style={style as CSSProperties} {...props}>{children}</div></SidebarContext.Provider>
}

interface SidebarProps extends HTMLAttributes<HTMLElement> {
  side?: 'left' | 'right'
  variant?: 'sidebar' | 'floating' | 'inset'
  collapsible?: 'offcanvas' | 'icon' | 'none'
}

export function Sidebar({ side = 'left', variant = 'sidebar', collapsible = 'offcanvas', className = '', children, ...props }: SidebarProps) {
  const { state } = useSidebar()
  return <aside data-slot="sidebar" data-side={side} data-state={state} data-collapsible={state === 'collapsed' ? collapsible : ''} data-variant={variant} className={`app-sidebar ${className}`.trim()} {...props}>{children}</aside>
}

export function SidebarHeader({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="sidebar-header" className={`sidebar-header ${className}`.trim()} {...props} />
}

export function SidebarContent({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="sidebar-content" className={`sidebar-content sidebar-scroll ${className}`.trim()} {...props} />
}

export function SidebarGroup({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="sidebar-group" className={`sidebar-group ${className}`.trim()} {...props} />
}

export function SidebarGroupLabel({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="sidebar-group-label" className={`sidebar-section-label ${className}`.trim()} {...props} />
}

export function SidebarGroupContent({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="sidebar-group-content" className={`sidebar-group-content ${className}`.trim()} {...props} />
}

export function SidebarMenu({ className = '', ...props }: HTMLAttributes<HTMLUListElement>) {
  return <ul data-slot="sidebar-menu" className={`sidebar-nav ${className}`.trim()} {...props} />
}

export function SidebarMenuItem({ className = '', ...props }: LiHTMLAttributes<HTMLLIElement>) {
  return <li data-slot="sidebar-menu-item" className={`nav-node ${className}`.trim()} {...props} />
}

export function SidebarMenuButton({ className = '', isActive = false, size = 'default', tooltip: _tooltip, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { isActive?: boolean; size?: 'default' | 'sm' | 'lg'; tooltip?: string }) {
  return <button data-slot="sidebar-menu-button" data-active={isActive || undefined} data-size={size} className={`nav-item nav-item-${size} ${isActive ? 'is-active' : ''} ${className}`.trim()} type="button" {...props} />
}

export function SidebarMenuSub({ className = '', ...props }: HTMLAttributes<HTMLUListElement>) {
  return <ul data-slot="sidebar-menu-sub" className={`nav-children ${className}`.trim()} {...props} />
}

export function SidebarMenuSubItem({ className = '', ...props }: LiHTMLAttributes<HTMLLIElement>) {
  return <li data-slot="sidebar-menu-sub-item" className={`nav-node ${className}`.trim()} {...props} />
}

export function SidebarMenuSubButton({ className = '', isActive = false, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { isActive?: boolean }) {
  return <button data-slot="sidebar-menu-sub-button" data-active={isActive || undefined} className={`nav-item nav-sub-item ${isActive ? 'is-active' : ''} ${className}`.trim()} type="button" {...props} />
}

export function SidebarFooter({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="sidebar-footer" className={`sidebar-footer ${className}`.trim()} {...props} />
}

export function SidebarTrigger({ className = '', onClick, children, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  const { toggleSidebar } = useSidebar()
  return (
    <button data-slot="sidebar-trigger" data-sidebar="trigger" className={`sidebar-trigger-button ${className}`.trim()} type="button" onClick={event => { onClick?.(event); toggleSidebar() }} {...props}>
      {children ?? (
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect width="18" height="18" x="3" y="3" rx="2" />
          <path d="M9 3v18" />
        </svg>
      )}
    </button>
  )
}

export function SidebarRail({ className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  const { toggleSidebar } = useSidebar()
  return <button data-slot="sidebar-rail" className={`sidebar-rail ${className}`.trim()} type="button" aria-label="切换侧边栏" tabIndex={-1} onClick={toggleSidebar} {...props} />
}

export type { SidebarContextValue }
