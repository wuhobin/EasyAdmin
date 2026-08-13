import { CheckOutlined, EyeInvisibleOutlined, EyeOutlined } from '@ant-design/icons'
import * as Checkbox from '@radix-ui/react-checkbox'
import type { InputHTMLAttributes, ReactNode } from 'react'

interface AuthInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  leading?: ReactNode
  trailing?: ReactNode
}

export function AuthInput({ label, error, leading, trailing, id, className = '', ...props }: AuthInputProps) {
  return (
    <label className={`auth-field ${error ? 'has-error' : ''} ${className}`} htmlFor={id}>
      <span className="auth-field-label">{label}</span>
      <span className="auth-field-control">
        {leading ? <span className="auth-field-leading" aria-hidden="true">{leading}</span> : null}
        <input id={id} {...props} />
        {trailing ? <span className="auth-field-trailing">{trailing}</span> : null}
      </span>
      <span className="auth-field-message" role={error ? 'alert' : undefined}>{error || ' '}</span>
    </label>
  )
}

interface PasswordInputProps extends Omit<AuthInputProps, 'type' | 'trailing'> {
  visible: boolean
  onToggle: () => void
}

export function PasswordInput({ visible, onToggle, ...props }: PasswordInputProps) {
  return (
    <AuthInput
      {...props}
      type={visible ? 'text' : 'password'}
      trailing={(
        <button
          className="auth-password-toggle"
          type="button"
          onClick={onToggle}
          aria-label={visible ? '隐藏密码' : '显示密码'}
        >
          {visible ? <EyeOutlined /> : <EyeInvisibleOutlined />}
        </button>
      )}
    />
  )
}

interface AuthButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean
  variant?: 'primary' | 'secondary'
}

export function AuthButton({ loading, variant = 'primary', children, className = '', disabled, ...props }: AuthButtonProps) {
  return (
    <button
      {...props}
      className={`auth-button auth-button-${variant} ${className}`}
      disabled={disabled || loading}
    >
      <span className="auth-button-label">{loading ? '正在处理…' : children}</span>
      <span className="auth-button-sheen" aria-hidden="true">{loading ? '处理中' : '继续'} <span>→</span></span>
    </button>
  )
}

interface AuthCheckboxProps {
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  children: ReactNode
}

export function AuthCheckbox({ checked, onCheckedChange, children }: AuthCheckboxProps) {
  return (
    <label className="auth-checkbox-label">
      <Checkbox.Root
        className="auth-checkbox"
        checked={checked}
        onCheckedChange={value => onCheckedChange(value === true)}
      >
        <Checkbox.Indicator><CheckOutlined /></Checkbox.Indicator>
      </Checkbox.Root>
      <span>{children}</span>
    </label>
  )
}
