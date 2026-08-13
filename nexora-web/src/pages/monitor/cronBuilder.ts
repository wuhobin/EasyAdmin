export type CronUnitName = 'second' | 'minute' | 'hour' | 'day' | 'month' | 'week' | 'year'
export type CronUnitMode = 'every' | 'cycle' | 'specific' | 'workday' | 'unspecified' | 'last'

export interface CronUnitState {
  mode: CronUnitMode
  start: number
  interval: number
  values: number[]
}

export type CronBuilderState = Record<CronUnitName, CronUnitState>

const currentYear = new Date().getFullYear()

export function createCronBuilderState(): CronBuilderState {
  return {
    second: { mode: 'specific', start: 0, interval: 1, values: [0] },
    minute: { mode: 'specific', start: 0, interval: 1, values: [0] },
    hour: { mode: 'every', start: 0, interval: 1, values: [] },
    day: { mode: 'every', start: 1, interval: 1, values: [] },
    month: { mode: 'every', start: 1, interval: 1, values: [] },
    week: { mode: 'unspecified', start: 1, interval: 1, values: [] },
    year: { mode: 'every', start: currentYear, interval: 1, values: [] }
  }
}

function field(unit: CronUnitState, fallback = '*') {
  if (unit.mode === 'every') return '*'
  if (unit.mode === 'cycle') return `${unit.start}/${unit.interval}`
  if (unit.mode === 'specific') return [...unit.values].sort((a, b) => a - b).join(',') || fallback
  if (unit.mode === 'workday') return 'W'
  if (unit.mode === 'unspecified') return '?'
  return 'L'
}

export function buildCronExpression(state: CronBuilderState) {
  return [field(state.second), field(state.minute), field(state.hour), field(state.day), field(state.month), field(state.week), field(state.year)].join(' ')
}

function parseField(value: string, unit: CronUnitState) {
  if (value === '*') return { ...unit, mode: 'every' as const }
  if (value === '?') return { ...unit, mode: 'unspecified' as const }
  if (value === 'W') return { ...unit, mode: 'workday' as const }
  if (value === 'L') return { ...unit, mode: 'last' as const }
  if (value.includes('/')) {
    const [start, interval] = value.split('/').map(Number)
    return { ...unit, mode: 'cycle' as const, start: Number.isFinite(start) ? start : unit.start, interval: Number.isFinite(interval) ? interval : unit.interval }
  }
  const values = value.split(',').map(Number).filter(Number.isFinite)
  return { ...unit, mode: 'specific' as const, values }
}

export function parseCronExpression(expression: string): CronBuilderState {
  const state = createCronBuilderState()
  const parts = expression.trim().split(/\s+/)
  if (parts.length < 6 || parts.length > 7) return state
  const names: CronUnitName[] = ['second', 'minute', 'hour', 'day', 'month', 'week', 'year']
  names.forEach((name, index) => {
    if (parts[index]) state[name] = parseField(parts[index], state[name])
  })
  return state
}

export function cronExpressionForParser(expression: string) {
  const parts = expression.trim().split(/\s+/)
  const sixFields = (parts.length === 7 ? parts.slice(0, 6) : parts).map(part => part.replace(/[?LW]/gi, '*'))
  return sixFields.join(' ')
}
