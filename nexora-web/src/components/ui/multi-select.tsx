import { Check, ChevronsUpDown, Search } from 'lucide-react'
import { useId, useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

interface MultiSelectOption {
  label: string
  value: number
}

interface MultiSelectProps {
  id?: string
  options: MultiSelectOption[]
  value: number[]
  onValueChange: (value: number[]) => void
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  invalid?: boolean
  searchPlaceholder?: string
  loadingText?: string
  emptyText?: string
}

export function MultiSelect({ id, options, value, onValueChange, placeholder = '请选择', disabled, loading, invalid, searchPlaceholder = '搜索角色', loadingText = '正在加载角色', emptyText = '没有匹配的角色' }: MultiSelectProps) {
  const [open, setOpen] = useState(false)
  const [keyword, setKeyword] = useState('')
  const listboxId = useId()
  const filtered = useMemo(() => options.filter(option => option.label.toLowerCase().includes(keyword.trim().toLowerCase())), [keyword, options])
  const selected = options.filter(option => value.includes(option.value))
  const toggle = (optionValue: number) => onValueChange(value.includes(optionValue) ? value.filter(item => item !== optionValue) : [...value, optionValue])

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button id={id} type="button" variant="outline" role="combobox" aria-haspopup="listbox" aria-controls={listboxId} aria-expanded={open} aria-invalid={invalid || undefined} disabled={disabled || loading} className={cn('h-auto min-h-9 w-full justify-between px-3 py-1.5 font-normal', !selected.length && 'text-[var(--nexora-placeholder)]')}>
          <span className="flex min-w-0 flex-wrap gap-1.5 text-left">
            {selected.length ? selected.map(option => <span key={option.value} className="rounded bg-[var(--nexora-muted)] px-1.5 py-0.5 text-xs text-[var(--nexora-ink)]">{option.label}</span>) : (loading ? loadingText : placeholder)}
          </span>
          <ChevronsUpDown className="ml-2 size-4 shrink-0 text-[var(--nexora-placeholder)]" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-[var(--radix-popover-trigger-width)] p-2">
        <div className="relative mb-2">
          <Search aria-hidden="true" className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-[var(--nexora-placeholder)]" />
          <Input name="multi-select-search" aria-label={searchPlaceholder} autoComplete="off" spellCheck={false} value={keyword} onChange={event => setKeyword(event.target.value)} placeholder={`${searchPlaceholder}…`} className="pl-8" />
        </div>
        <div id={listboxId} className="max-h-56 overflow-y-auto" role="listbox" aria-multiselectable="true">
          {filtered.length ? filtered.map(option => {
            const active = value.includes(option.value)
            return <button key={option.value} type="button" role="option" aria-selected={active} className="flex min-h-9 w-full items-center gap-2 rounded-md px-2.5 py-2 text-left text-[13px] hover:bg-[var(--nexora-muted)]" onClick={() => toggle(option.value)}><span className={cn('grid size-4 place-items-center rounded border border-[var(--nexora-line)]', active && 'border-[var(--nexora-violet)] bg-[var(--nexora-violet)] text-white')}>{active ? <Check className="size-3" /> : null}</span><span className="truncate">{option.label}</span></button>
          }) : <p className="m-0 px-2 py-6 text-center text-xs text-[var(--nexora-placeholder)]">{emptyText}</p>}
        </div>
        {value.length ? <div className="mt-2 border-t border-[var(--nexora-line)] pt-2"><Button type="button" variant="ghost" size="sm" className="w-full" onClick={() => onValueChange([])}>清空选择</Button></div> : null}
      </PopoverContent>
    </Popover>
  )
}
