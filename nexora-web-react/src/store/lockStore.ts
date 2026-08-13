import { create } from 'zustand'

const LOCKED_KEY = 'nexora-react-locked'

function readLocked() {
  try {
    return sessionStorage.getItem(LOCKED_KEY) === 'true'
  } catch {
    return false
  }
}

function persistLocked(locked: boolean) {
  try {
    if (locked) sessionStorage.setItem(LOCKED_KEY, 'true')
    else sessionStorage.removeItem(LOCKED_KEY)
  } catch {
    // Storage can be unavailable in private browsing or restricted environments.
  }
}

interface LockState {
  locked: boolean
  lock: () => void
  unlock: () => void
}

export const useLockStore = create<LockState>(set => ({
  locked: readLocked(),
  lock: () => { persistLocked(true); set({ locked: true }) },
  unlock: () => { persistLocked(false); set({ locked: false }) }
}))
