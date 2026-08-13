import { BgColorsOutlined, CheckOutlined, MoonOutlined, SunOutlined, UndoOutlined } from '@ant-design/icons'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Sheet, SheetBody, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Switch } from '@/components/ui/switch'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { defaultAppearanceSettings, useSettingsStore, type InterfaceDensity, type PageAnimation, type TagsStyle, type ThemeMode } from '@/store/settingsStore'
import { useUiStore } from '@/store/uiStore'

const ACCENT_COLORS = [
  { label: '鸢尾紫', value: '#6c3ff5' },
  { label: '钴蓝', value: '#2563eb' },
  { label: '青绿', value: '#0f8b8d' },
  { label: '松绿', value: '#25855a' },
  { label: '莓红', value: '#d14e72' },
  { label: '石墨', value: '#475569' }
]

function SettingSwitch({ label, description, checked, disabled, onCheckedChange }: {
  label: string
  description: string
  checked: boolean
  disabled?: boolean
  onCheckedChange: (checked: boolean) => void
}) {
  return <label className={`appearance-setting-row ${disabled ? 'is-disabled' : ''}`}><span><strong>{label}</strong><small>{description}</small></span><Switch checked={checked} disabled={disabled} onCheckedChange={onCheckedChange} /></label>
}

function SegmentedControl<T extends string>({ value, options, onChange, label }: {
  value: T
  options: { value: T; label: string; icon?: React.ReactNode }[]
  onChange: (value: T) => void
  label: string
}) {
  return <div className="appearance-segmented" role="group" aria-label={label}>{options.map(option => <button key={option.value} type="button" className={value === option.value ? 'is-active' : ''} aria-pressed={value === option.value} onClick={() => onChange(option.value)}>{option.icon}{option.label}{value === option.value ? <CheckOutlined className="appearance-segmented-check" /> : null}</button>)}</div>
}

export function AppearanceSettings({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const settings = useSettingsStore()
  const updateSettings = useSettingsStore(state => state.updateSettings)
  const resetSettings = useSettingsStore(state => state.resetSettings)
  const collapsed = useUiStore(state => state.sidebarCollapsed)
  const setSidebarCollapsed = useUiStore(state => state.setSidebarCollapsed)
  const globalWatermark = usePublicConfigStore(state => state.config.system.watermarkEnabled)

  const reset = () => {
    resetSettings()
    setSidebarCollapsed(false)
  }

  return <Sheet open={open} onOpenChange={onOpenChange}>
    <SheetContent className="appearance-settings-sheet w-[min(410px,calc(100vw-12px))]">
      <SheetHeader className="appearance-settings-header">
        <span className="appearance-settings-icon"><BgColorsOutlined /></span>
        <div><SheetTitle>外观设置</SheetTitle><SheetDescription>调整当前浏览器中的工作台显示方式。</SheetDescription></div>
      </SheetHeader>
      <SheetBody className="appearance-settings-body">
        <section className="appearance-settings-section">
          <header><span>主题</span><small>颜色与显示模式</small></header>
          <div className="appearance-setting-block"><span className="appearance-setting-label">主题模式</span><SegmentedControl<ThemeMode> label="主题模式" value={settings.theme} onChange={theme => updateSettings({ theme })} options={[{ value: 'light', label: '浅色', icon: <SunOutlined /> }, { value: 'dark', label: '深色', icon: <MoonOutlined /> }]} /></div>
          <div className="appearance-setting-block"><span className="appearance-setting-label">强调色</span><div className="appearance-color-grid">{ACCENT_COLORS.map(color => <button key={color.value} type="button" className={settings.accentColor === color.value ? 'is-active' : ''} aria-label={color.label} aria-pressed={settings.accentColor === color.value} title={color.label} style={{ '--swatch-color': color.value } as React.CSSProperties} onClick={() => updateSettings({ accentColor: color.value })}><i />{settings.accentColor === color.value ? <CheckOutlined /> : null}</button>)}<label className="appearance-custom-color" title="自定义颜色"><input type="color" aria-label="选择自定义强调色" value={settings.accentColor} onChange={event => updateSettings({ accentColor: event.target.value })} /><BgColorsOutlined /></label></div></div>
          <SettingSwitch label="灰度模式" description="将工作台内容转换为灰度显示" checked={settings.greyMode} onCheckedChange={greyMode => updateSettings({ greyMode })} />
        </section>

        <section className="appearance-settings-section">
          <header><span>布局</span><small>导航与内容密度</small></header>
          <div className="appearance-setting-block"><span className="appearance-setting-label">界面密度</span><SegmentedControl<InterfaceDensity> label="界面密度" value={settings.density} onChange={density => updateSettings({ density })} options={[{ value: 'small', label: '紧凑' }, { value: 'default', label: '标准' }, { value: 'large', label: '宽松' }]} /></div>
          <SettingSwitch label="显示 Logo" description="保留侧栏顶部的品牌区域" checked={settings.showLogo} onCheckedChange={showLogo => updateSettings({ showLogo })} />
          <SettingSwitch label="折叠侧栏" description="仅显示导航图标" checked={collapsed} onCheckedChange={setSidebarCollapsed} />
          <SettingSwitch label="显示标签页" description="保留页面缓存和批量关闭入口" checked={settings.showTags} onCheckedChange={showTags => updateSettings({ showTags })} />
          <SettingSwitch label="显示页脚" description="显示版权和备案信息" checked={settings.showFooter} onCheckedChange={showFooter => updateSettings({ showFooter })} />
        </section>

        <section className="appearance-settings-section">
          <header><span>页面</span><small>标题、水印与切换效果</small></header>
          <SettingSwitch label="动态标题" description="浏览器标题显示当前页面名称" checked={settings.dynamicTitle} onCheckedChange={dynamicTitle => updateSettings({ dynamicTitle })} />
          <SettingSwitch label="开启水印" description={globalWatermark ? '后台配置已强制开启水印' : '在内容区域显示当前账户水印'} checked={globalWatermark || settings.watermark} disabled={globalWatermark} onCheckedChange={watermark => updateSettings({ watermark })} />
          <div className="appearance-setting-select"><span><strong>页面动画</strong><small>切换缓存页面时的过渡方式</small></span><Select value={settings.pageAnimation} onValueChange={(pageAnimation: PageAnimation) => updateSettings({ pageAnimation })}><SelectTrigger className="appearance-animation-select" aria-label="页面动画"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="slide">轻微滑动</SelectItem><SelectItem value="fade">淡入</SelectItem><SelectItem value="none">无动画</SelectItem></SelectContent></Select></div>
          <div className="appearance-setting-block"><span className="appearance-setting-label">标签样式</span><div className="appearance-tab-style-grid">{([{ value: 'border', label: '边框' }, { value: 'card', label: '卡片' }, { value: 'modern', label: '现代' }] as { value: TagsStyle; label: string }[]).map(option => <button key={option.value} type="button" className={settings.tagsStyle === option.value ? 'is-active' : ''} aria-pressed={settings.tagsStyle === option.value} onClick={() => updateSettings({ tagsStyle: option.value })}><span className={`appearance-tab-preview ${option.value}`}><i /><i className="active" /><i /></span><small>{option.label}</small></button>)}</div></div>
        </section>
      </SheetBody>
      <footer className="appearance-settings-footer"><span>设置仅保存在当前浏览器</span><Button type="button" variant="outline" onClick={reset}><UndoOutlined />恢复默认</Button></footer>
    </SheetContent>
  </Sheet>
}

export { defaultAppearanceSettings }
