import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { MailAccount, MailProvider, MailProviderConfig } from '@/api/mail'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { buildMailAddress, defaultMailProvider, emptyMailAccountForm, isMailAddressForDomain, mailAccountToForm, mailAddressAccount, providerDomain, replaceMailProviderDomain, type MailAccountFormValues } from '@/pages/mail/mailForms'

const schema = z.object({
  id: z.number().optional(),
  accountName: z.string().trim().min(1, '请输入账户名称').max(100, '账户名称不能超过 100 个字符'),
  provider: z.union([z.enum(['QQ', 'NETEASE_163', 'NETEASE_126', 'YEAH']), z.literal('')]).refine(Boolean, '请选择邮箱类型'),
  email: z.string().trim().min(1, '请输入邮箱地址').email('邮箱格式不正确'),
  authCode: z.string().max(255, '邮箱授权码不能超过 255 个字符'),
  enabled: z.number(),
  sort: z.number().int('排序必须是整数').min(0, '排序不能小于 0').max(999, '排序不能超过 999')
}).superRefine((values, context) => {
  if (values.id === undefined && !values.authCode) context.addIssue({ code: 'custom', path: ['authCode'], message: '请输入邮箱授权码' })
})

export function MailAccountDialog({ open, account, providers, providersLoading, saving, onOpenChange, onSubmit }: {
  open: boolean
  account?: MailAccount
  providers: MailProviderConfig[]
  providersLoading?: boolean
  saving: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (values: MailAccountFormValues) => void
}) {
  const form = useForm<MailAccountFormValues>({ resolver: zodResolver(schema), defaultValues: emptyMailAccountForm })

  useEffect(() => {
    if (!open) return
    form.reset(account ? mailAccountToForm(account) : { ...emptyMailAccountForm, provider: defaultMailProvider(providers) })
  }, [account, form, open, providers])

  useEffect(() => {
    if (!open || form.getValues('provider') || !providers.length) return
    form.setValue('provider', defaultMailProvider(providers))
  }, [form, open, providers])

  const submit = form.handleSubmit(values => {
    const domain = providerDomain(providers, values.provider)
    if (!isMailAddressForDomain(values.email, domain)) {
      form.setError('email', { message: `当前邮箱类型要求地址以 @${domain} 结尾` })
      return
    }
    onSubmit(values)
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[620px]">
        <Form {...form}>
          <form onSubmit={submit}>
            <DialogHeader>
              <DialogTitle>{account ? '编辑邮箱账户' : '添加邮箱账户'}</DialogTitle>
              <DialogDescription className="mail-dialog-description">配置邮箱服务信息，用于收取和发送邮件。</DialogDescription>
            </DialogHeader>
            <div className="management-dialog-body management-form-grid mail-account-form">
              <FormField control={form.control} name="accountName" render={({ field }) => <FormItem><FormLabel>账户名称</FormLabel><FormControl><Input maxLength={100} placeholder="例如：工作 QQ 邮箱" {...field} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="provider" render={({ field }) => <FormItem><FormLabel>邮箱类型</FormLabel><Select disabled={providersLoading} value={field.value} onValueChange={value => {
                const previousDomain = providerDomain(providers, field.value)
                const nextProvider = value as MailProvider
                field.onChange(nextProvider)
                form.setValue('email', replaceMailProviderDomain(form.getValues('email'), previousDomain, providerDomain(providers, nextProvider)), { shouldValidate: Boolean(form.getValues('email')) })
              }}><FormControl><SelectTrigger><SelectValue placeholder={providersLoading ? '正在加载邮箱类型' : '请选择邮箱类型'} /></SelectTrigger></FormControl><SelectContent>{providers.map(provider => <SelectItem key={provider.value} value={provider.value}>{provider.label}</SelectItem>)}</SelectContent></Select><FormMessage /></FormItem>} />
              <FormField control={form.control} name="email" render={({ field }) => {
                const domain = providerDomain(providers, form.watch('provider'))
                return <FormItem className="management-form-span-2"><FormLabel>邮箱地址</FormLabel><div className="mail-address-control"><FormControl><Input value={mailAddressAccount(field.value)} onBlur={field.onBlur} onChange={event => field.onChange(buildMailAddress(event.target.value, domain))} placeholder="请输入邮箱账号或手机号" autoComplete="email" /></FormControl><span>@{domain || '邮箱域名'}</span></div><FormMessage /></FormItem>
              }} />
              <FormField control={form.control} name="authCode" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>邮箱授权码</FormLabel><FormControl><PasswordInput maxLength={255} autoComplete="new-password" placeholder={account ? '留空表示不修改' : '不是邮箱登录密码，请填写 IMAP 授权码'} {...field} /></FormControl><FormDescription>请先在邮箱设置中开启 IMAP/SMTP 服务并生成授权码。</FormDescription><FormMessage /></FormItem>} />
              <FormField control={form.control} name="sort" render={({ field }) => <FormItem><FormLabel>排序</FormLabel><FormControl><Input type="number" min={0} max={999} inputMode="numeric" value={field.value} onBlur={field.onBlur} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="enabled" render={({ field }) => <FormItem><FormLabel>状态</FormLabel><div className="management-switch-field"><span className="management-switch-copy"><span>{field.value === 1 ? '启用' : '停用'}</span><small>停用后不再读取该邮箱</small></span><Switch aria-label="邮箱账户状态" checked={field.value === 1} onCheckedChange={checked => field.onChange(checked ? 1 : 0)} /></div><FormMessage /></FormItem>} />
            </div>
            <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button className="mail-account-submit" type="submit" loading={saving}>{account ? '保存修改' : '添加邮箱'}</Button></DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
