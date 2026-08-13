import { beforeEach, describe, expect, it } from 'vitest'
import { useLockStore } from '@/store/lockStore'

class MemoryStorage {
  private values = new Map<string, string>()

  getItem(key: string) { return this.values.get(key) ?? null }
  setItem(key: string, value: string) { this.values.set(key, value) }
  removeItem(key: string) { this.values.delete(key) }
  clear() { this.values.clear() }
}

Object.defineProperty(globalThis, 'sessionStorage', { value: new MemoryStorage(), configurable: true })

describe('lock store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    useLockStore.setState({ locked: false })
  })

  it('persists the locked state for page refreshes', () => {
    useLockStore.getState().lock()
    expect(useLockStore.getState().locked).toBe(true)
    expect(sessionStorage.getItem('nexora-react-locked')).toBe('true')
  })

  it('clears persisted state after unlocking', () => {
    useLockStore.getState().lock()
    useLockStore.getState().unlock()
    expect(useLockStore.getState().locked).toBe(false)
    expect(sessionStorage.getItem('nexora-react-locked')).toBeNull()
  })
})
