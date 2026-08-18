import { describe, expect, it } from 'vitest'
import { readUrlQuery, writeUrlQuery, type UrlQuerySchema } from '@/utils/urlQueryState'

interface TestQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: number
  grouped?: boolean
}

const defaults: TestQuery = { pageNum: 1, pageSize: 10 }
const schema: UrlQuerySchema<TestQuery> = { pageNum: 'number', pageSize: 'number', keyword: 'string', status: 'number', grouped: 'boolean' }

describe('urlQueryState', () => {
  it('parses typed URL values and ignores malformed numbers', () => {
    expect(readUrlQuery(new URLSearchParams('pageNum=3&keyword=mail&status=1&grouped=1'), defaults, schema)).toEqual({ pageNum: 3, pageSize: 10, keyword: 'mail', status: 1, grouped: true })
    expect(readUrlQuery(new URLSearchParams('pageNum=bad'), defaults, schema)).toEqual(defaults)
  })

  it('omits defaults and preserves unrelated URL parameters', () => {
    const params = writeUrlQuery(new URLSearchParams('dialog=edit'), { pageNum: 1, pageSize: 20, keyword: 'nexora' }, defaults, schema)
    expect(params.toString()).toBe('dialog=edit&pageSize=20&keyword=nexora')
  })
})
