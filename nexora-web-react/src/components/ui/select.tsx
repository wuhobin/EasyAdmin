import * as SelectPrimitive from '@radix-ui/react-select'
import { Check, ChevronDown, ChevronUp } from 'lucide-react'
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react'
import { cn } from '@/lib/utils'

export const Select = SelectPrimitive.Root
export const SelectGroup = SelectPrimitive.Group
export const SelectValue = SelectPrimitive.Value

export const SelectTrigger = forwardRef<ElementRef<typeof SelectPrimitive.Trigger>, ComponentPropsWithoutRef<typeof SelectPrimitive.Trigger>>(({ className, children, ...props }, ref) => (
  <SelectPrimitive.Trigger ref={ref} className={cn('flex h-9 w-full items-center justify-between gap-2 rounded-[7px] border border-[var(--nexora-line)] bg-[var(--nexora-panel)] px-3 py-2 text-[13px] text-[var(--nexora-ink)] shadow-[0_1px_2px_rgb(25_23_40_/_2%)] outline-none transition-colors duration-200 data-[placeholder]:text-[var(--nexora-placeholder)] hover:border-[color-mix(in_srgb,var(--nexora-violet)_28%,var(--nexora-line))] focus:border-[var(--nexora-violet)] focus:ring-0 disabled:cursor-not-allowed disabled:bg-[var(--nexora-muted)] disabled:opacity-65 aria-invalid:border-[var(--nexora-danger)] [&>span]:truncate', className)} {...props}>
    {children}<SelectPrimitive.Icon asChild><ChevronDown className="size-4 shrink-0 text-[var(--nexora-placeholder)]" /></SelectPrimitive.Icon>
  </SelectPrimitive.Trigger>
))
SelectTrigger.displayName = SelectPrimitive.Trigger.displayName

export const SelectScrollUpButton = forwardRef<ElementRef<typeof SelectPrimitive.ScrollUpButton>, ComponentPropsWithoutRef<typeof SelectPrimitive.ScrollUpButton>>(({ className, ...props }, ref) => (
  <SelectPrimitive.ScrollUpButton ref={ref} className={cn('flex h-7 cursor-default items-center justify-center', className)} {...props}><ChevronUp className="size-4" /></SelectPrimitive.ScrollUpButton>
))
SelectScrollUpButton.displayName = SelectPrimitive.ScrollUpButton.displayName

export const SelectScrollDownButton = forwardRef<ElementRef<typeof SelectPrimitive.ScrollDownButton>, ComponentPropsWithoutRef<typeof SelectPrimitive.ScrollDownButton>>(({ className, ...props }, ref) => (
  <SelectPrimitive.ScrollDownButton ref={ref} className={cn('flex h-7 cursor-default items-center justify-center', className)} {...props}><ChevronDown className="size-4" /></SelectPrimitive.ScrollDownButton>
))
SelectScrollDownButton.displayName = SelectPrimitive.ScrollDownButton.displayName

export const SelectContent = forwardRef<ElementRef<typeof SelectPrimitive.Content>, ComponentPropsWithoutRef<typeof SelectPrimitive.Content>>(({ className, children, position = 'popper', ...props }, ref) => (
  <SelectPrimitive.Portal>
    <SelectPrimitive.Content ref={ref} position={position} className={cn('nexora-select-content relative z-[1250] max-h-80 min-w-[8rem] overflow-hidden rounded-[8px] border border-[var(--nexora-line)] bg-[var(--nexora-panel)] text-[var(--nexora-ink)] shadow-[0_16px_40px_rgb(0_0_0_/_18%)]', position === 'popper' && 'w-[var(--radix-select-trigger-width)]', className)} {...props}>
      <SelectScrollUpButton /><SelectPrimitive.Viewport className="p-1">{children}</SelectPrimitive.Viewport><SelectScrollDownButton />
    </SelectPrimitive.Content>
  </SelectPrimitive.Portal>
))
SelectContent.displayName = SelectPrimitive.Content.displayName

export const SelectItem = forwardRef<ElementRef<typeof SelectPrimitive.Item>, ComponentPropsWithoutRef<typeof SelectPrimitive.Item>>(({ className, children, ...props }, ref) => (
  <SelectPrimitive.Item ref={ref} className={cn('relative flex min-h-9 w-full cursor-default select-none items-center rounded-md py-2 pl-8 pr-3 text-[13px] outline-none data-[disabled]:pointer-events-none data-[highlighted]:bg-[var(--nexora-muted)] data-[state=checked]:text-[var(--nexora-violet)] data-[disabled]:opacity-45', className)} {...props}>
    <span className="absolute left-2.5 flex size-4 items-center justify-center"><SelectPrimitive.ItemIndicator><Check className="size-4" /></SelectPrimitive.ItemIndicator></span>
    <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
  </SelectPrimitive.Item>
))
SelectItem.displayName = SelectPrimitive.Item.displayName
