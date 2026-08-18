import { Slot } from '@radix-ui/react-slot'
import { createContext, forwardRef, useContext, useId, type ComponentPropsWithoutRef, type HTMLAttributes } from 'react'
import { Controller, FormProvider, useFormContext, type ControllerProps, type FieldPath, type FieldValues } from 'react-hook-form'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

export const Form = FormProvider

type FormFieldContextValue<TFieldValues extends FieldValues = FieldValues, TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>> = { name: TName }
const FormFieldContext = createContext<FormFieldContextValue>({} as FormFieldContextValue)

export function FormField<TFieldValues extends FieldValues = FieldValues, TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>>(props: ControllerProps<TFieldValues, TName>) {
  return <FormFieldContext.Provider value={{ name: props.name }}><Controller {...props} /></FormFieldContext.Provider>
}

const FormItemContext = createContext<{ id: string }>({ id: '' })

export function useFormField() {
  const fieldContext = useContext(FormFieldContext)
  const itemContext = useContext(FormItemContext)
  const { getFieldState, formState } = useFormContext()
  if (!fieldContext.name) throw new Error('useFormField must be used within <FormField>')
  const fieldState = getFieldState(fieldContext.name, formState)
  return {
    id: itemContext.id,
    name: fieldContext.name,
    formItemId: `${itemContext.id}-form-item`,
    formDescriptionId: `${itemContext.id}-form-item-description`,
    formMessageId: `${itemContext.id}-form-item-message`,
    ...fieldState
  }
}

export const FormItem = forwardRef<HTMLDivElement, HTMLAttributes<HTMLDivElement>>(({ className, ...props }, ref) => {
  const id = useId()
  return <FormItemContext.Provider value={{ id }}><div ref={ref} data-slot="form-item" className={cn('grid gap-2', className)} {...props} /></FormItemContext.Provider>
})
FormItem.displayName = 'FormItem'

export const FormLabel = forwardRef<React.ElementRef<typeof Label>, React.ComponentPropsWithoutRef<typeof Label>>(({ className, ...props }, ref) => {
  const { formItemId } = useFormField()
  return <Label ref={ref} htmlFor={formItemId} className={className} {...props} />
})
FormLabel.displayName = 'FormLabel'

export const FormControl = forwardRef<React.ElementRef<typeof Slot>, ComponentPropsWithoutRef<typeof Slot>>((props, ref) => {
  const { error, formItemId, formDescriptionId, formMessageId } = useFormField()
  return <Slot ref={ref} id={formItemId} aria-describedby={error ? `${formDescriptionId} ${formMessageId}` : formDescriptionId} aria-invalid={Boolean(error)} {...props} />
})
FormControl.displayName = 'FormControl'

export const FormDescription = forwardRef<HTMLParagraphElement, HTMLAttributes<HTMLParagraphElement>>(({ className, ...props }, ref) => {
  const { formDescriptionId } = useFormField()
  return <p ref={ref} id={formDescriptionId} className={cn('text-xs leading-5 text-[var(--nexora-placeholder)]', className)} {...props} />
})
FormDescription.displayName = 'FormDescription'

export const FormMessage = forwardRef<HTMLParagraphElement, HTMLAttributes<HTMLParagraphElement>>(({ className, children, ...props }, ref) => {
  const { error, formMessageId } = useFormField()
  const body = error ? String(error.message ?? '') : children
  if (!body) return null
  return <p ref={ref} id={formMessageId} role="alert" aria-live="polite" className={cn('nexora-form-message text-[11px] font-medium leading-4 text-[var(--nexora-danger)]', className)} {...props}>{body}</p>
})
FormMessage.displayName = 'FormMessage'
