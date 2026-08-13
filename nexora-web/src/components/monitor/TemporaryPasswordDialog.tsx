import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { ManagedServer } from '@/api/server'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { PasswordInput } from '@/components/ui/password-input'

interface Values { password: string }
const schema = z.object({ password: z.string().min(1, '请输入 SSH 密码').max(512, 'SSH 密码不能超过 512 个字符') })

export function TemporaryPasswordDialog({ server, action, onCancel, onSubmit }: { server?: ManagedServer; action: string; onCancel: () => void; onSubmit: (password: string) => void }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { password: '' } })
  useEffect(() => { if (server) form.reset({ password: '' }) }, [form, server])
  return <Dialog open={Boolean(server)} onOpenChange={open => { if (!open) onCancel() }}><DialogContent className="max-w-[480px]"><Form {...form}><form onSubmit={form.handleSubmit(values => onSubmit(values.password))}><DialogHeader><DialogTitle>临时 SSH 密码</DialogTitle><DialogDescription>“{server?.name}”没有保存密码，请输入本次{action}使用的密码。密码不会保存。</DialogDescription></DialogHeader><div className="management-dialog-body"><FormField control={form.control} name="password" render={({ field }) => <FormItem><FormLabel>SSH 密码</FormLabel><FormControl><PasswordInput autoFocus maxLength={512} autoComplete="off" placeholder="密码仅用于本次连接" {...field} /></FormControl><FormMessage /></FormItem>} /></div><DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit">继续</Button></DialogFooter></form></Form></DialogContent></Dialog>
}
