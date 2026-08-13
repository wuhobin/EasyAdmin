import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(({ className, type, ...props }, ref) => (
  <input
    ref={ref}
    type={type}
    className={cn('flex h-9 w-full rounded-[7px] border border-[var(--nexora-line)] bg-[var(--nexora-panel)] px-3 py-2 text-[13px] text-[var(--nexora-ink)] shadow-[0_1px_2px_rgb(25_23_40_/_2%)] transition-[border-color,background-color] duration-200 file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-[var(--nexora-placeholder)] hover:border-[color-mix(in_srgb,var(--nexora-violet)_28%,var(--nexora-line))] focus-visible:border-[var(--nexora-violet)] focus-visible:outline-none focus-visible:ring-0 disabled:cursor-not-allowed disabled:bg-[var(--nexora-muted)] disabled:opacity-65 aria-invalid:border-[var(--nexora-danger)]', className)}
    {...props}
  />
))
Input.displayName = 'Input'
