import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { forwardRef, type ComponentProps } from 'react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

const attachmentVariants = cva(
  'group/attachment relative flex w-fit max-w-full min-w-0 shrink-0 flex-wrap border border-[var(--nexora-line)] bg-[var(--nexora-panel)] text-[var(--nexora-ink)] transition-colors focus-within:ring-1 focus-within:ring-[color-mix(in_srgb,var(--nexora-violet)_45%,transparent)] data-[state=error]:border-[color-mix(in_srgb,var(--nexora-danger)_35%,var(--nexora-line))] data-[state=idle]:border-dashed',
  {
    variants: {
      size: {
        default: 'gap-2 text-sm',
        sm: 'gap-2 text-xs',
        xs: 'gap-1.5 text-xs'
      },
      orientation: {
        horizontal: 'min-w-40 items-center rounded-[8px] p-2',
        vertical: 'w-24 flex-col rounded-[8px] p-2'
      }
    },
    defaultVariants: { size: 'default', orientation: 'horizontal' }
  }
)

export type AttachmentState = 'idle' | 'uploading' | 'processing' | 'error' | 'done'

export function Attachment({ className, state = 'done', size = 'default', orientation = 'horizontal', ...props }: ComponentProps<'div'> & VariantProps<typeof attachmentVariants> & { state?: AttachmentState }) {
  return <div data-slot="attachment" data-state={state} data-size={size} data-orientation={orientation} className={cn(attachmentVariants({ size, orientation }), className)} {...props} />
}

const mediaVariants = cva(
  'relative flex aspect-square w-10 shrink-0 items-center justify-center overflow-hidden rounded-[7px] bg-[var(--nexora-muted)] text-[var(--nexora-ink-soft)] group-data-[orientation=vertical]/attachment:w-full group-data-[size=sm]/attachment:w-8 group-data-[size=xs]/attachment:w-7 [&_svg]:pointer-events-none [&_svg]:size-4 group-data-[orientation=vertical]/attachment:[&_svg]:size-6',
  {
    variants: {
      variant: {
        icon: '',
        image: 'opacity-65 group-data-[state=done]/attachment:opacity-100 group-data-[state=idle]/attachment:opacity-100 [&_img]:aspect-square [&_img]:h-full [&_img]:w-full [&_img]:object-cover'
      }
    },
    defaultVariants: { variant: 'icon' }
  }
)

export function AttachmentMedia({ className, variant = 'icon', ...props }: ComponentProps<'div'> & VariantProps<typeof mediaVariants>) {
  return <div data-slot="attachment-media" data-variant={variant} className={cn(mediaVariants({ variant }), className)} {...props} />
}

export function AttachmentContent({ className, ...props }: ComponentProps<'div'>) {
  return <div data-slot="attachment-content" className={cn('max-w-full min-w-0 flex-1 leading-tight', className)} {...props} />
}

export function AttachmentTitle({ className, ...props }: ComponentProps<'span'>) {
  return <span data-slot="attachment-title" className={cn('block max-w-full min-w-0 truncate font-medium', className)} {...props} />
}

export function AttachmentDescription({ className, ...props }: ComponentProps<'span'>) {
  return <span data-slot="attachment-description" className={cn('mt-0.5 block max-w-full min-w-0 truncate text-xs text-[var(--nexora-placeholder)] group-data-[state=error]/attachment:text-[var(--nexora-danger)]', className)} {...props} />
}

export function AttachmentActions({ className, ...props }: ComponentProps<'div'>) {
  return <div data-slot="attachment-actions" className={cn('relative z-20 flex shrink-0 items-center', className)} {...props} />
}

export const AttachmentAction = forwardRef<HTMLButtonElement, ComponentProps<typeof Button>>(({ className, variant = 'ghost', size = 'icon', ...props }, ref) => (
  <Button ref={ref} data-slot="attachment-action" variant={variant} size={size} className={cn(className)} {...props} />
))
AttachmentAction.displayName = 'AttachmentAction'

export function AttachmentTrigger({ className, asChild = false, type, ...props }: ComponentProps<'button'> & { asChild?: boolean }) {
  const Component = asChild ? Slot : 'button'
  return <Component data-slot="attachment-trigger" type={asChild ? undefined : type ?? 'button'} className={cn('absolute inset-0 z-10 rounded-[inherit] outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[var(--nexora-violet)]', className)} {...props} />
}

export function AttachmentGroup({ className, ...props }: ComponentProps<'div'>) {
  return <div data-slot="attachment-group" className={cn('flex min-w-0 snap-x snap-mandatory gap-3 overflow-x-auto overscroll-x-contain py-1 [&>[data-slot=attachment]]:flex-none [&>[data-slot=attachment]]:snap-start', className)} {...props} />
}
