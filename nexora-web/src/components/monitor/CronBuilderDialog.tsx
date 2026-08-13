import { CheckOutlined, CopyOutlined } from '@ant-design/icons'
import cronParser from 'cron-parser'
import { useEffect, useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { buildCronExpression, cronExpressionForParser, parseCronExpression, type CronBuilderState, type CronUnitMode, type CronUnitName } from '@/pages/monitor/cronBuilder'

interface UnitConfig {
  name: CronUnitName
  label: string
  unit: string
  min: number
  max: number
  modes: Array<{ label: string; value: CronUnitMode }>
}

const currentYear = new Date().getFullYear()
const units: UnitConfig[] = [
  { name: 'second', label: '秒', unit: '秒', min: 0, max: 59, modes: commonModes('每秒') },
  { name: 'minute', label: '分钟', unit: '分钟', min: 0, max: 59, modes: commonModes('每分钟') },
  { name: 'hour', label: '小时', unit: '小时', min: 0, max: 23, modes: commonModes('每小时') },
  { name: 'day', label: '日期', unit: '日', min: 1, max: 31, modes: [...commonModes('每日'), { label: '工作日', value: 'workday' }, { label: '不指定', value: 'unspecified' }, { label: '最后一天', value: 'last' }] },
  { name: 'month', label: '月份', unit: '月', min: 1, max: 12, modes: commonModes('每月') },
  { name: 'week', label: '星期', unit: '周', min: 1, max: 7, modes: [...commonModes('每周'), { label: '不指定', value: 'unspecified' }, { label: '最后一周', value: 'last' }] },
  { name: 'year', label: '年份', unit: '年', min: currentYear, max: currentYear + 10, modes: commonModes('每年') }
]

const weekLabels: Record<number, string> = { 1: '周日', 2: '周一', 3: '周二', 4: '周三', 5: '周四', 6: '周五', 7: '周六' }

function commonModes(everyLabel: string): UnitConfig['modes'] {
  return [{ label: everyLabel, value: 'every' }, { label: '周期', value: 'cycle' }, { label: '指定', value: 'specific' }]
}

function range(config: UnitConfig) {
  return Array.from({ length: config.max - config.min + 1 }, (_, index) => config.min + index)
}

function formatDate(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function previewTimes(expression: string) {
  try {
    const interval = cronParser.parseExpression(cronExpressionForParser(expression), { currentDate: new Date() })
    return Array.from({ length: 5 }, () => formatDate(interval.next().toDate()))
  } catch {
    return []
  }
}

export function CronBuilderDialog({ open, value, onOpenChange, onConfirm }: { open: boolean; value: string; onOpenChange: (open: boolean) => void; onConfirm: (value: string) => void }) {
  const [activeUnit, setActiveUnit] = useState<CronUnitName>('second')
  const [state, setState] = useState<CronBuilderState>(() => parseCronExpression(value))

  useEffect(() => {
    if (open) setState(parseCronExpression(value))
  }, [open, value])

  const expression = useMemo(() => buildCronExpression(state), [state])
  const nextTimes = useMemo(() => previewTimes(expression), [expression])
  const config = units.find(unit => unit.name === activeUnit) ?? units[0]
  const selected = state[activeUnit]

  const updateUnit = (patch: Partial<CronBuilderState[CronUnitName]>) => {
    setState(previous => ({ ...previous, [activeUnit]: { ...previous[activeUnit], ...patch } }))
  }

  const toggleValue = (valueToToggle: number, checked: boolean) => {
    const values = checked ? [...selected.values, valueToToggle] : selected.values.filter(value => value !== valueToToggle)
    updateUnit({ values: [...new Set(values)].sort((left, right) => left - right) })
  }

  const copyExpression = async () => {
    try { await navigator.clipboard.writeText(expression) } catch { /* Clipboard access may be unavailable. */ }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="cron-dialog max-w-[760px]">
        <DialogHeader><DialogTitle>Cron 表达式生成器</DialogTitle><DialogDescription>通过执行单位配置 Quartz Cron，并预览最近五次执行时间。</DialogDescription></DialogHeader>
        <div className="cron-builder">
          <div className="cron-unit-tabs" role="tablist" aria-label="Cron 时间单位">
            {units.map(unit => <button type="button" role="tab" aria-selected={activeUnit === unit.name} className={activeUnit === unit.name ? 'is-active' : ''} key={unit.name} onClick={() => setActiveUnit(unit.name)}>{unit.label}</button>)}
          </div>
          <section className="cron-rule-panel" aria-label={`${config.label}规则`}>
            <RadioGroup value={selected.mode} onValueChange={value => updateUnit({ mode: value as CronUnitMode })} className="cron-mode-options">
              {config.modes.map(mode => <label key={mode.value} className="management-radio-option"><RadioGroupItem value={mode.value} />{mode.label}</label>)}
            </RadioGroup>
            {selected.mode === 'cycle' ? <div className="cron-cycle-editor"><span>从</span><Input type="number" min={config.min} max={config.max} value={selected.start} onChange={event => updateUnit({ start: Number(event.target.value) })} /><span>{config.unit}开始，每</span><Input type="number" min={1} max={Math.max(1, config.max)} value={selected.interval} onChange={event => updateUnit({ interval: Number(event.target.value) })} /><span>{config.unit}执行一次</span></div> : null}
            {selected.mode === 'specific' ? <div className="cron-specific-grid">{range(config).map(valueOption => <label key={valueOption}><Checkbox checked={selected.values.includes(valueOption)} onCheckedChange={checked => toggleValue(valueOption, checked === true)} /><span>{config.name === 'week' ? weekLabels[valueOption] : valueOption}</span></label>)}</div> : null}
          </section>
          <section className="cron-result-panel">
            <div className="cron-expression-heading"><span>Cron 表达式</span><Button type="button" variant="ghost" size="icon" aria-label="复制 Cron 表达式" onClick={() => void copyExpression()}><CopyOutlined /></Button></div>
            <code className="cron-expression-value">{expression}</code>
            <p>字段顺序：秒 分 小时 日期 月份 星期 年份</p>
            <div className="cron-preview"><strong>最近五次执行时间</strong>{nextTimes.length ? <ol>{nextTimes.map((time, index) => <li key={time}><span>{index + 1}</span><time>{time}</time>{index === 0 ? <small>下一次</small> : null}</li>)}</ol> : <div className="cron-preview-empty">当前规则无法计算预览时间，请检查表达式。</div>}</div>
          </section>
        </div>
        <DialogFooter><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>取消</Button><Button type="button" disabled={!nextTimes.length} onClick={() => { onConfirm(expression); onOpenChange(false) }}><CheckOutlined />使用此表达式</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
