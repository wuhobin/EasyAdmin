import { SearchOutlined } from '@ant-design/icons'
import { useMemo, useState } from 'react'
import { MenuIcon } from '@/components/MenuIcon'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { antDesignIconNames } from '@/utils/antDesignIconCatalog'

interface IconPickerProps {
  value?: string
  onChange?: (value: string) => void
}

export function IconPicker({ value = '', onChange }: IconPickerProps) {
  const [open, setOpen] = useState(false)
  const [keyword, setKeyword] = useState('')
  const entries = useMemo(() => antDesignIconNames.filter(name => name.toLowerCase().includes(keyword.toLowerCase())), [keyword])
  const selectedName = value.startsWith('antd:') ? value.slice(5) : value

  return (
    <>
      <button className="icon-picker-trigger" type="button" onClick={() => setOpen(true)}>
        <MenuIcon value={value} /> <span>{selectedName || '选择 Ant Design 图标'}</span>
      </button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-[720px]">
          <DialogHeader><DialogTitle>选择 Ant Design 图标</DialogTitle><DialogDescription>图标保存为 `antd:&lt;IconExportName&gt;`，旧图标值仍可兼容显示。</DialogDescription></DialogHeader>
          <div className="management-dialog-body">
            <div className="relative"><SearchOutlined className="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 text-[var(--nexora-placeholder)]" /><Input placeholder="搜索图标，例如 UserOutlined" value={keyword} onChange={event => setKeyword(event.target.value)} className="pl-9" /></div>
            <div className="icon-picker-grid">
              {entries.length ? entries.map(name => <button type="button" className={`icon-picker-item ${selectedName === name ? 'is-selected' : ''}`} key={name} onClick={() => { onChange?.(`antd:${name}`); setOpen(false) }}><MenuIcon value={`antd:${name}`} /><span>{name}</span></button>) : <p className="icon-picker-empty">没有找到匹配的图标</p>}
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}
