import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { getUserInfoApi } from '@/api/auth'
import { bindEmailApi, sendChangeEmailCodeApi } from '@/api/user'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { emailFormSchema, normalizeEmailForm, type EmailFormValues } from '@/pages/profile/profileForms'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { passwordPolicyDescription, validatePasswordByPolicy } from '@/utils/password-policy'

const EMPTY_EMAIL_FORM: EmailFormValues = { email: '', code: '' }

interface BindEmailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function BindEmailDialog({ open, onOpenChange }: BindEmailDialogProps) {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const currentUser = useAuthStore(state => state.user)
  const setUser = useAuthStore(state => state.setUser)
  const passwordPolicy = usePublicConfigStore(state => state.config.password)
  const [codeCountdown, setCodeCountdown] = useState(0)
  const [password, setPassword] = useState('')
  const form = useForm<EmailFormValues>({ resolver: zodResolver(emailFormSchema), defaultValues: EMPTY_EMAIL_FORM })

  useEffect(() => {
    if (codeCountdown <= 0) return
    const timer = window.setInterval(() => setCodeCountdown(value => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [codeCountdown])

  const reset = () => {
    form.reset(EMPTY_EMAIL_FORM)
    setCodeCountdown(0)
    setPassword('')
  }

  const handleOpenChange = (nextOpen: boolean) => {
    onOpenChange(nextOpen)
    if (!nextOpen) reset()
  }

  const sendCodeMutation = useMutation({
    mutationFn: sendChangeEmailCodeApi,
    onSuccess: () => { setCodeCountdown(60); message.success('验证码已发送') },
    onError: error => message.error(error instanceof Error ? error.message : '验证码发送失败')
  })

  const bindMutation = useMutation({
    mutationFn: async (values: EmailFormValues) => {
      const normalized = normalizeEmailForm(values)
      const passwordError = validatePasswordByPolicy(password, passwordPolicy)
      if (passwordError) throw new Error(passwordError)
      await bindEmailApi(normalized.email, normalized.code, password)
      return normalized.email
    },
    onSuccess: async email => {
      try {
        const { data: user } = await getUserInfoApi()
        setUser(user)
      } catch {
        if (currentUser.id !== null) setUser({ ...currentUser, id: currentUser.id, email })
      }
      await queryClient.invalidateQueries({ queryKey: ['user-profile'] })
      handleOpenChange(false)
      message.success('邮箱和登录密码已绑定')
    },
    onError: error => message.error(error instanceof Error ? error.message : '邮箱绑定失败')
  })

  const sendEmailCode = async () => {
    if (!await form.trigger('email')) return
    sendCodeMutation.mutate(form.getValues('email').trim().toLowerCase())
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="profile-email-dialog max-w-[500px]">
        <Form {...form}>
          <form onSubmit={form.handleSubmit(values => bindMutation.mutate(values))}>
            <DialogHeader>
              <DialogTitle>绑定登录邮箱</DialogTitle>
              <DialogDescription className="profile-email-dialog-description">验证邮箱并设置密码后，该账号也可以使用邮箱密码登录。</DialogDescription>
            </DialogHeader>
            <div className="management-dialog-body profile-email-dialog-form">
              <FormField control={form.control} name="email" render={({ field }) => <FormItem><FormLabel>登录邮箱</FormLabel><div className="profile-field-control"><FormControl><Input type="email" autoComplete="email" placeholder="请输入登录邮箱" {...field} /></FormControl><FormDescription className="sr-only">输入接收验证码并用于后续登录的邮箱</FormDescription><FormMessage /></div></FormItem>} />
              <FormField control={form.control} name="code" render={({ field }) => <FormItem><FormLabel>验证码</FormLabel><div className="profile-code-field"><FormControl><Input inputMode="numeric" maxLength={8} placeholder="4-8 位数字" {...field} /></FormControl><Button type="button" variant="outline" loading={sendCodeMutation.isPending} disabled={codeCountdown > 0} onClick={() => void sendEmailCode()}>{codeCountdown > 0 ? `${codeCountdown} 秒` : '发送验证码'}</Button><FormMessage /></div><FormDescription className="sr-only">请输入邮箱收到的验证码</FormDescription></FormItem>} />
              <div data-slot="form-item"><label htmlFor="bind-email-password">登录密码</label><div className="profile-field-control"><PasswordInput id="bind-email-password" value={password} onChange={event => setPassword(event.target.value)} autoComplete="new-password" placeholder={passwordPolicyDescription(passwordPolicy)} /></div></div>
            </div>
            <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={bindMutation.isPending}>完成绑定</Button></DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
