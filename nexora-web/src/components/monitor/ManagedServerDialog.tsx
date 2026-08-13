import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { ManagedServer } from '@/api/server'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { emptyManagedServerForm, managedServerToForm, type ManagedServerFormValues } from '@/pages/monitor/monitorForms'

const schema = z.object({
  id: z.number().optional(),
  name: z.string().trim().min(1, '请输入服务器名称').max(100, '服务器名称不能超过 100 个字符'),
  host: z.string().trim().min(1, '请输入服务器地址').max(255, '服务器地址不能超过 255 个字符'),
  port: z.number().int('SSH 端口必须是整数').min(1, 'SSH 端口范围为 1-65535').max(65535, 'SSH 端口范围为 1-65535'),
  username: z.string().trim().min(1, '请输入 SSH 用户名').max(100, 'SSH 用户名不能超过 100 个字符'),
  password: z.string().max(512, 'SSH 密码不能超过 512 个字符'),
  savePassword: z.boolean(),
  clearSavedPassword: z.boolean(),
  description: z.string().max(500, '描述不能超过 500 个字符'),
  enabled: z.number(),
  sort: z.number().int().min(0, '排序不能小于 0').max(9999, '排序不能超过 9999')
})

export function ManagedServerDialog({ open, server, saving, onOpenChange, onSubmit }: { open: boolean; server?: ManagedServer; saving: boolean; onOpenChange: (open: boolean) => void; onSubmit: (values: ManagedServerFormValues) => void }) {
  const form = useForm<ManagedServerFormValues>({ resolver: zodResolver(schema), defaultValues: emptyManagedServerForm })
  const password = form.watch('password')
  const savePassword = form.watch('savePassword')

  useEffect(() => {
    if (!open) return
    form.reset(server ? managedServerToForm(server) : emptyManagedServerForm)
  }, [form, open, server])

  useEffect(() => {
    if (!password) form.setValue('savePassword', false)
  }, [form, password])

  useEffect(() => {
    if (savePassword) form.setValue('clearSavedPassword', false)
  }, [form, savePassword])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[680px]">
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)}>
            <DialogHeader><DialogTitle>{server ? '编辑服务器' : '添加服务器'}</DialogTitle><DialogDescription>维护 SSH 连接地址、凭据策略和服务器状态。</DialogDescription></DialogHeader>
            <div className="management-dialog-body management-form-grid">
              <FormField control={form.control} name="name" render={({ field }) => <FormItem><FormLabel>服务器名称</FormLabel><FormControl><Input maxLength={100} placeholder="例如：生产应用节点" {...field} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="username" render={({ field }) => <FormItem><FormLabel>SSH 用户名</FormLabel><FormControl><Input maxLength={100} autoComplete="username" placeholder="root" {...field} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="host" render={({ field }) => <FormItem><FormLabel>服务器地址</FormLabel><FormControl><Input maxLength={255} placeholder="IP 地址或域名" {...field} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="port" render={({ field }) => <FormItem><FormLabel>SSH 端口</FormLabel><FormControl><Input type="number" min={1} max={65535} inputMode="numeric" value={field.value} onBlur={field.onBlur} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="password" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>SSH 密码</FormLabel><FormControl><PasswordInput maxLength={512} autoComplete="new-password" placeholder={server ? '留空表示不修改已保存的密码' : '可留空，连接时再临时输入'} {...field} /></FormControl><div className="server-password-options"><FormField control={form.control} name="savePassword" render={({ field: option }) => <label><Checkbox checked={option.value} disabled={!password} onCheckedChange={value => option.onChange(value === true)} />加密保存本次填写的密码</label>} />{server?.hasSavedPassword ? <FormField control={form.control} name="clearSavedPassword" render={({ field: option }) => <label><Checkbox checked={option.value} disabled={savePassword} onCheckedChange={value => option.onChange(value === true)} />清除已保存密码</label>} /> : null}</div><FormMessage /></FormItem>} />
              <FormField control={form.control} name="description" render={({ field }) => <FormItem className="management-form-span-2"><FormLabel>描述</FormLabel><FormControl><Textarea rows={3} maxLength={500} placeholder="记录用途、环境或负责人等信息" {...field} /></FormControl><div className="management-character-count">{field.value.length}/500</div><FormMessage /></FormItem>} />
              <FormField control={form.control} name="sort" render={({ field }) => <FormItem><FormLabel>排序</FormLabel><FormControl><Input type="number" min={0} max={9999} inputMode="numeric" value={field.value} onBlur={field.onBlur} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><FormMessage /></FormItem>} />
              <FormField control={form.control} name="enabled" render={({ field }) => <FormItem><FormLabel>状态</FormLabel><div className="management-switch-field"><span className="management-switch-copy"><span>{field.value === 1 ? '启用' : '停用'}</span><small>停用后不能测试或打开终端</small></span><Switch checked={field.value === 1} onCheckedChange={checked => field.onChange(checked ? 1 : 0)} /></div><FormMessage /></FormItem>} />
            </div>
            <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={saving}>{server ? '保存修改' : '添加服务器'}</Button></DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
