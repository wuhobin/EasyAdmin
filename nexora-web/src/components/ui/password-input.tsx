import { Eye, EyeOff } from 'lucide-react'
import { forwardRef, useState, type InputHTMLAttributes } from 'react'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

export const PasswordInput = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(({ className, ...props }, ref) => {
  const [visible, setVisible] = useState(false)
  return (
    <div className="relative">
      <Input ref={ref} type={visible ? 'text' : 'password'} className={cn('pr-10', className)} {...props} />
      <button type="button" className="absolute right-1 top-1 grid size-7 place-items-center rounded-md text-[var(--nexora-placeholder)] transition-colors hover:bg-[var(--nexora-muted)] hover:text-[var(--nexora-ink)]" aria-label={visible ? '隐藏密码' : '显示密码'} onClick={() => setVisible(current => !current)}>
        {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
      </button>
    </div>
  )
})
PasswordInput.displayName = 'PasswordInput'
