import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import { forwardRef, type ComponentPropsWithoutRef, type ElementRef, type HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export const Dialog = DialogPrimitive.Root
export const DialogTrigger = DialogPrimitive.Trigger
export const DialogClose = DialogPrimitive.Close
export const DialogPortal = DialogPrimitive.Portal

export const DialogOverlay = forwardRef<ElementRef<typeof DialogPrimitive.Overlay>, ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>>(({ className, ...props }, ref) => (
  <DialogPrimitive.Overlay ref={ref} className={cn('nexora-dialog-overlay fixed inset-0 z-[1100] bg-black/48 backdrop-blur-[2px]', className)} {...props} />
))
DialogOverlay.displayName = DialogPrimitive.Overlay.displayName

type DialogContentProps = ComponentPropsWithoutRef<typeof DialogPrimitive.Content> & { hideCloseButton?: boolean }

export const DialogContent = forwardRef<ElementRef<typeof DialogPrimitive.Content>, DialogContentProps>(({ className, children, hideCloseButton = false, ...props }, ref) => (
  <DialogPortal>
    <DialogOverlay />
    <DialogPrimitive.Content ref={ref} className={cn('nexora-dialog-content fixed left-1/2 top-1/2 z-[1110] max-h-[min(88vh,760px)] w-[calc(100%-24px)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-[10px] border border-[var(--nexora-line)] bg-[var(--nexora-panel)] text-[var(--nexora-ink)] shadow-[0_24px_70px_rgb(0_0_0_/_26%)] outline-none', className)} {...props}>
      {children}
      {hideCloseButton ? null : <DialogPrimitive.Close className="absolute right-4 top-4 grid size-8 place-items-center rounded-md text-[var(--nexora-placeholder)] transition-colors hover:bg-[var(--nexora-muted)] hover:text-[var(--nexora-ink)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--nexora-violet)]" aria-label="关闭"><X className="size-4" /></DialogPrimitive.Close>}
    </DialogPrimitive.Content>
  </DialogPortal>
))
DialogContent.displayName = DialogPrimitive.Content.displayName

export function DialogHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('border-b border-[var(--nexora-line)] px-6 pb-4 pt-5 pr-14', className)} {...props} />
}
export function DialogFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('flex flex-col-reverse gap-2 border-t border-[var(--nexora-line)] px-6 pb-5 pt-4 sm:flex-row sm:justify-end', className)} {...props} />
}
export const DialogTitle = forwardRef<ElementRef<typeof DialogPrimitive.Title>, ComponentPropsWithoutRef<typeof DialogPrimitive.Title>>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title ref={ref} className={cn('text-base font-semibold leading-6 text-[var(--nexora-ink)]', className)} {...props} />
))
DialogTitle.displayName = DialogPrimitive.Title.displayName
export const DialogDescription = forwardRef<ElementRef<typeof DialogPrimitive.Description>, ComponentPropsWithoutRef<typeof DialogPrimitive.Description>>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description ref={ref} className={cn('mt-1.5 text-xs leading-5 text-[var(--nexora-placeholder)]', className)} {...props} />
))
DialogDescription.displayName = DialogPrimitive.Description.displayName
