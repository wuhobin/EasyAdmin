import { useCallback, useMemo, type SetStateAction } from 'react'
import { useSearchParams } from 'react-router-dom'

type UrlValueType = 'string' | 'number' | 'boolean'
export type UrlQuerySchema<T extends object> = { [K in keyof T]?: UrlValueType }

function parameterName(key: PropertyKey, prefix?: string) {
  return prefix ? `${prefix}.${String(key)}` : String(key)
}

export function readUrlQuery<T extends object>(params: URLSearchParams, defaults: T, schema: UrlQuerySchema<T>, prefix?: string): T {
  const next = { ...defaults }
  for (const key of Object.keys(schema) as Array<keyof T>) {
    const raw = params.get(parameterName(key, prefix))
    if (raw === null) continue
    const type = schema[key]
    if (type === 'number') {
      const value = Number(raw)
      if (Number.isFinite(value)) next[key] = value as T[keyof T]
    } else if (type === 'boolean') {
      if (raw === '1' || raw === 'true') next[key] = true as T[keyof T]
      else if (raw === '0' || raw === 'false') next[key] = false as T[keyof T]
    } else {
      next[key] = raw as T[keyof T]
    }
  }
  return next
}

export function writeUrlQuery<T extends object>(current: URLSearchParams, value: T, defaults: T, schema: UrlQuerySchema<T>, prefix?: string) {
  const next = new URLSearchParams(current)
  for (const key of Object.keys(schema) as Array<keyof T>) {
    const name = parameterName(key, prefix)
    const fieldValue = value[key]
    if (fieldValue === undefined || fieldValue === null || fieldValue === '' || Object.is(fieldValue, defaults[key])) {
      next.delete(name)
    } else {
      next.set(name, typeof fieldValue === 'boolean' ? fieldValue ? '1' : '0' : String(fieldValue))
    }
  }
  return next
}

export function useUrlQueryState<T extends object>(defaults: T, schema: UrlQuerySchema<T>, prefix?: string) {
  const [searchParams, setSearchParams] = useSearchParams()
  const state = useMemo(() => readUrlQuery(searchParams, defaults, schema, prefix), [defaults, prefix, schema, searchParams])
  const setState = useCallback((value: SetStateAction<T>) => {
    setSearchParams(current => {
      const previous = readUrlQuery(current, defaults, schema, prefix)
      const next = typeof value === 'function' ? (value as (previous: T) => T)(previous) : value
      return writeUrlQuery(current, next, defaults, schema, prefix)
    }, { replace: true })
  }, [defaults, prefix, schema, setSearchParams])
  return [state, setState] as const
}
