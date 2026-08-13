import { forwardRef, type TextareaHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(({ className, ...props }, ref) => (
  <textarea
    ref={ref}
    className={cn('flex min-h-24 w-full resize-y rounded-[7px] border border-[var(--nexora-line)] bg-[var(--nexora-panel)] px-3 py-2.5 text-[13px] leading-5 text-[var(--nexora-ink)] shadow-[0_1px_2px_rgb(25_23_40_/_2%)] transition-colors duration-200 placeholder:text-[var(--nexora-placeholder)] hover:border-[color-mix(in_srgb,var(--nexora-violet)_28%,var(--nexora-line))] focus-visible:border-[var(--nexora-violet)] focus-visible:outline-none focus-visible:ring-0 disabled:cursor-not-allowed disabled:bg-[var(--nexora-muted)] disabled:opacity-65 aria-invalid:border-[var(--nexora-danger)]', className)}
    {...props}
  />
))
Textarea.displayName = 'Textarea'
