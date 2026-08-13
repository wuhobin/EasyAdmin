import * as SheetPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef, type HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export const Sheet = SheetPrimitive.Root
export const SheetClose = SheetPrimitive.Close
export const SheetTitle = SheetPrimitive.Title
export const SheetDescription = SheetPrimitive.Description

export const SheetContent = forwardRef<ElementRef<typeof SheetPrimitive.Content>, ComponentPropsWithoutRef<typeof SheetPrimitive.Content>>(({ className, children, ...props }, ref) => (
  <SheetPrimitive.Portal>
    <SheetPrimitive.Overlay className="nexora-dialog-overlay fixed inset-0 z-[1100] bg-black/48 backdrop-blur-[2px]" />
    <SheetPrimitive.Content ref={ref} className={cn('nexora-sheet-content fixed inset-y-0 right-0 z-[1110] w-[min(980px,96vw)] overflow-y-auto border-l border-[var(--nexora-line)] bg-[var(--nexora-panel)] text-[var(--nexora-ink)] shadow-[-20px_0_60px_rgb(0_0_0_/_20%)] outline-none', className)} {...props}>
      {children}
      <SheetPrimitive.Close className="absolute right-4 top-4 grid size-8 place-items-center rounded-md text-[var(--nexora-placeholder)] transition-colors hover:bg-[var(--nexora-muted)] hover:text-[var(--nexora-ink)]" aria-label="关闭"><X className="size-4" /></SheetPrimitive.Close>
    </SheetPrimitive.Content>
  </SheetPrimitive.Portal>
))
SheetContent.displayName = SheetPrimitive.Content.displayName

export function SheetHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('border-b border-[var(--nexora-line)] px-5 py-5 pr-14', className)} {...props} />
}
export function SheetBody({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('px-5 pb-5', className)} {...props} />
}
