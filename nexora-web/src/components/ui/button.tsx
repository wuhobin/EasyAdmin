import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { LoaderCircle } from 'lucide-react'
import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export const buttonVariants = cva(
  'inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-[7px] text-[13px] font-medium transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color-mix(in_srgb,var(--nexora-violet)_50%,transparent)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--nexora-panel)] disabled:pointer-events-none disabled:opacity-45 [&_svg]:pointer-events-none [&_svg]:size-4',
  {
    variants: {
      variant: {
        default: 'bg-[var(--nexora-violet)] text-white shadow-sm hover:bg-[var(--nexora-violet-dark)]',
        destructive: 'bg-[var(--nexora-danger)] text-white shadow-sm hover:bg-[color-mix(in_srgb,var(--nexora-danger)_84%,black)]',
        outline: 'border border-[var(--nexora-line)] bg-[var(--nexora-panel)] text-[var(--nexora-ink)] hover:border-[color-mix(in_srgb,var(--nexora-violet)_34%,var(--nexora-line))] hover:bg-[var(--nexora-muted)]',
        secondary: 'bg-[var(--nexora-muted)] text-[var(--nexora-ink)] hover:bg-[color-mix(in_srgb,var(--nexora-muted)_78%,var(--nexora-line))]',
        ghost: 'text-[var(--nexora-ink-soft)] hover:bg-[var(--nexora-muted)] hover:text-[var(--nexora-ink)]',
        link: 'h-auto text-[var(--nexora-violet)] underline-offset-4 hover:underline'
      },
      size: {
        default: 'h-9 px-3.5 py-2',
        sm: 'h-8 rounded-md px-3 text-xs',
        lg: 'h-10 px-5',
        icon: 'size-9 p-0'
      }
    },
    defaultVariants: { variant: 'default', size: 'default' }
  }
)

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
  asChild?: boolean
  loading?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(({ asChild = false, className, variant, size, loading = false, disabled, children, ...props }, ref) => {
  const Component = asChild ? Slot : 'button'
  return (
    <Component ref={ref} className={cn(buttonVariants({ variant, size }), className)} disabled={disabled || loading} aria-busy={loading || undefined} {...props}>
      {loading ? <LoaderCircle className="animate-spin" aria-hidden="true" /> : null}
      {children}
    </Component>
  )
})
Button.displayName = 'Button'
