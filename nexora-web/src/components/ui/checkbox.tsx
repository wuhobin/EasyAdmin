import * as CheckboxPrimitive from '@radix-ui/react-checkbox'
import { Check } from 'lucide-react'
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react'
import { cn } from '@/lib/utils'

export const Checkbox = forwardRef<ElementRef<typeof CheckboxPrimitive.Root>, ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root>>(({ className, ...props }, ref) => (
  <CheckboxPrimitive.Root ref={ref} className={cn('peer grid size-4 shrink-0 place-items-center rounded-[4px] border border-[var(--nexora-line)] bg-[var(--nexora-panel)] text-white shadow-sm outline-none transition-colors data-[state=checked]:border-[var(--nexora-violet)] data-[state=checked]:bg-[var(--nexora-violet)] focus-visible:ring-2 focus-visible:ring-[color-mix(in_srgb,var(--nexora-violet)_35%,transparent)] disabled:cursor-not-allowed disabled:opacity-45', className)} {...props}>
    <CheckboxPrimitive.Indicator><Check className="size-3" strokeWidth={3} /></CheckboxPrimitive.Indicator>
  </CheckboxPrimitive.Root>
))
Checkbox.displayName = CheckboxPrimitive.Root.displayName
