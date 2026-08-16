import {
  CameraOutlined,
  CheckCircleFilled,
  KeyOutlined,
  LockOutlined,
  MailOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UserOutlined
} from '@ant-design/icons'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react'
import { useForm } from 'react-hook-form'
import { getUserInfoApi } from '@/api/auth'
import { uploadFileApi } from '@/api/file'
import {
  changeEmailApi,
  bindEmailApi,
  getUserProfileApi,
  sendChangeEmailCodeApi,
  updateUserPasswordApi,
  updateUserProfileApi
} from '@/api/user'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import {
  createPasswordFormSchema,
  emailFormSchema,
  normalizeEmailForm,
  profileFormSchema,
  profileFormToPayload,
  profileRecordToForm,
  type EmailFormValues,
  type PasswordFormValues,
  type ProfileFormValues
} from '@/pages/profile/profileForms'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { passwordPolicyDescription } from '@/utils/password-policy'
import { validatePasswordByPolicy } from '@/utils/password-policy'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

type ProfileSection = 'details' | 'security'

interface ProfilePageState { section: ProfileSection }
const initialPageState: ProfilePageState = { section: 'details' }
const pageStateSchema: UrlQuerySchema<ProfilePageState> = { section: 'string' }

const EMPTY_PROFILE_FORM: ProfileFormValues = { nickname: '', mobile: '', sex: 0 }
const EMPTY_PASSWORD_FORM: PasswordFormValues = { oldPassword: '', newPassword: '', confirmPassword: '' }
const EMPTY_EMAIL_FORM: EmailFormValues = { email: '', code: '' }
const AVATAR_TYPES = new Set(['image/jpeg', 'image/png'])
const MAX_AVATAR_SIZE = 2 * 1024 * 1024

function initials(value: string) {
  return Array.from(value.trim())[0]?.toUpperCase() || 'N'
}

function ProfileLoading() {
  return (
    <section className="profile-page" aria-label="正在加载个人中心" aria-busy="true">
      <div className="profile-workspace profile-skeleton">
        <div className="profile-skeleton-avatar" />
        <div className="profile-skeleton-copy"><i /><i /><i /></div>
        <div className="profile-skeleton-tabs"><i /><i /></div>
        <div className="profile-skeleton-form">{Array.from({ length: 4 }, (_, index) => <i key={index} />)}</div>
      </div>
    </section>
  )
}

export function ProfilePage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const setUser = useAuthStore(state => state.setUser)
  const passwordPolicy = usePublicConfigStore(state => state.config.password)
  const avatarInputRef = useRef<HTMLInputElement>(null)
  const [pageState, setPageState] = useUrlQueryState(initialPageState, pageStateSchema)
  const activeSection: ProfileSection = pageState.section === 'security' ? 'security' : 'details'
  const setActiveSection = (section: ProfileSection) => setPageState({ section })
  const moveSection = (event: React.KeyboardEvent<HTMLButtonElement>, section: ProfileSection) => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
    event.preventDefault()
    const next: ProfileSection = event.key === 'ArrowLeft' || event.key === 'Home' ? 'details' : 'security'
    setActiveSection(next)
    window.requestAnimationFrame(() => document.getElementById(`profile-tab-${next}`)?.focus())
  }
  const [emailDialogOpen, setEmailDialogOpen] = useState(false)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [codeCountdown, setCodeCountdown] = useState(0)
  const [bindPassword, setBindPassword] = useState('')
  const passwordSchema = useMemo(() => createPasswordFormSchema(passwordPolicy), [passwordPolicy])
  const profileForm = useForm<ProfileFormValues>({ resolver: zodResolver(profileFormSchema), defaultValues: EMPTY_PROFILE_FORM })
  const passwordForm = useForm<PasswordFormValues>({ resolver: zodResolver(passwordSchema), defaultValues: EMPTY_PASSWORD_FORM })
  const emailForm = useForm<EmailFormValues>({ resolver: zodResolver(emailFormSchema), defaultValues: EMPTY_EMAIL_FORM })
  const profileQuery = useQuery({ queryKey: ['user-profile'], queryFn: async () => (await getUserProfileApi()).data })
  const profile = profileQuery.data?.sysUser
  const roles = profileQuery.data?.roles ?? []
  const displayName = profile?.nickname || profile?.email || 'Nexora 用户'

  useEffect(() => {
    if (profile) profileForm.reset(profileRecordToForm(profile))
  }, [profile, profileForm])

  useEffect(() => {
    if (profile && !profile.email) setEmailDialogOpen(true)
  }, [profile])

  useEffect(() => {
    if (codeCountdown <= 0) return
    const timer = window.setInterval(() => setCodeCountdown(value => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [codeCountdown])

  const refreshIdentity = async () => {
    const [profileResponse, userResponse] = await Promise.all([getUserProfileApi(), getUserInfoApi()])
    queryClient.setQueryData(['user-profile'], profileResponse.data)
    setUser(userResponse.data)
    return profileResponse.data
  }

  const profileMutation = useMutation({
    mutationFn: (values: ProfileFormValues) => updateUserProfileApi(profileFormToPayload(values, profile?.avatar)),
    onSuccess: async () => { await refreshIdentity(); message.success('个人资料已更新') },
    onError: error => message.error(error instanceof Error ? error.message : '个人资料更新失败')
  })

  const passwordMutation = useMutation({
    mutationFn: (values: PasswordFormValues) => updateUserPasswordApi(values.oldPassword, values.newPassword),
    onSuccess: () => { passwordForm.reset(EMPTY_PASSWORD_FORM); message.success('登录密码已更新') },
    onError: error => message.error(error instanceof Error ? error.message : '登录密码更新失败')
  })

  const sendCodeMutation = useMutation({
    mutationFn: sendChangeEmailCodeApi,
    onSuccess: () => { setCodeCountdown(60); message.success('验证码已发送') },
    onError: error => message.error(error instanceof Error ? error.message : '验证码发送失败')
  })

  const emailMutation = useMutation({
    mutationFn: (values: EmailFormValues) => {
      const normalized = normalizeEmailForm(values)
      if (!profile?.email) {
        const passwordError = validatePasswordByPolicy(bindPassword, passwordPolicy)
        if (passwordError) throw new Error(passwordError)
        return bindEmailApi(normalized.email, normalized.code, bindPassword)
      }
      return changeEmailApi(normalized.email, normalized.code)
    },
    onSuccess: async () => {
      await refreshIdentity()
      setEmailDialogOpen(false)
      emailForm.reset(EMPTY_EMAIL_FORM)
      setCodeCountdown(0)
      setBindPassword('')
      message.success(profile?.email ? '登录邮箱已更新' : '邮箱和登录密码已绑定')
    },
    onError: error => message.error(error instanceof Error ? error.message : '登录邮箱更新失败')
  })

  const sendEmailCode = async () => {
    if (!await emailForm.trigger('email')) return
    const email = emailForm.getValues('email').trim().toLowerCase()
    sendCodeMutation.mutate(email)
  }

  const chooseAvatar = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !profile) return
    if (!AVATAR_TYPES.has(file.type)) { message.error('头像仅支持 JPG 或 PNG 格式'); return }
    if (file.size >= MAX_AVATAR_SIZE) { message.error('头像大小不能超过 2MB'); return }

    setAvatarUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const { data: avatar } = await uploadFileApi(formData)
      await updateUserProfileApi(profileFormToPayload(profileRecordToForm(profile), avatar))
      await refreshIdentity()
      message.success('头像已更新')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '头像上传失败')
    } finally {
      setAvatarUploading(false)
    }
  }

  if (profileQuery.isLoading) return <ProfileLoading />

  if (profileQuery.isError || !profile) {
    return (
      <section className="profile-page">
        <div className="profile-workspace profile-error-state">
          <span><UserOutlined /></span>
          <strong>个人资料加载失败</strong>
          <p>请检查网络连接后重新尝试。</p>
          <Button type="button" loading={profileQuery.isFetching} onClick={() => void profileQuery.refetch()}><ReloadOutlined />重新加载</Button>
        </div>
      </section>
    )
  }

  return (
    <section className="profile-page">
      <div className="profile-workspace">
        <header className="profile-account">
          <div className="profile-avatar-editor">
            <input ref={avatarInputRef} type="file" accept="image/jpeg,image/png" aria-label="选择新头像" onChange={event => void chooseAvatar(event)} />
            <button type="button" disabled={avatarUploading} aria-label="更换头像" onClick={() => avatarInputRef.current?.click()}>
              <span className="profile-avatar">{profile.avatar ? <img src={profile.avatar} alt={`${displayName}的头像`} width={84} height={84} /> : initials(displayName)}</span>
              <span className="profile-avatar-action" aria-hidden="true">{avatarUploading ? <i /> : <CameraOutlined />}</span>
            </button>
            <small>JPG / PNG · 2MB 内</small>
          </div>

          <div className="profile-account-copy">
            <div className="profile-account-title"><h1>{displayName}</h1><span><CheckCircleFilled />账户正常</span></div>
            <p>{profile.email || '尚未绑定邮箱'}</p>
            <div className="profile-role-list">{(roles.length ? roles : ['普通用户']).map(role => <span key={role}>{role}</span>)}</div>
          </div>

          <dl className="profile-account-meta">
            <div><dt>手机号</dt><dd>{profile.mobile || '未设置'}</dd></div>
            <div><dt>加入时间</dt><dd>{formatDateTime(profile.createTime)}</dd></div>
            <div><dt>最近登录</dt><dd>{formatDateTime(profile.lastLoginTime)}</dd></div>
          </dl>
        </header>

        <div className="profile-tabs" role="tablist" aria-label="个人中心设置">
          <button id="profile-tab-details" type="button" role="tab" tabIndex={activeSection === 'details' ? 0 : -1} aria-selected={activeSection === 'details'} aria-controls="profile-panel-details" className={activeSection === 'details' ? 'active' : ''} onClick={() => setActiveSection('details')} onKeyDown={event => moveSection(event, 'details')}><UserOutlined /><span>个人资料</span></button>
          <button id="profile-tab-security" type="button" role="tab" tabIndex={activeSection === 'security' ? 0 : -1} aria-selected={activeSection === 'security'} aria-controls="profile-panel-security" className={activeSection === 'security' ? 'active' : ''} onClick={() => setActiveSection('security')} onKeyDown={event => moveSection(event, 'security')}><SafetyCertificateOutlined /><span>登录安全</span></button>
        </div>

        {activeSection === 'details' ? (
          <div id="profile-panel-details" className="profile-panel" role="tabpanel" aria-labelledby="profile-tab-details">
            <div className="profile-panel-heading"><div><h2>个人资料</h2><p>用于系统内的身份展示与必要联系。</p></div><UserOutlined /></div>
            <Form {...profileForm}>
              <form className="profile-settings-form" onSubmit={profileForm.handleSubmit(values => profileMutation.mutate(values))}>
                <FormField control={profileForm.control} name="nickname" render={({ field }) => <FormItem><FormLabel>昵称</FormLabel><div className="profile-field-control"><FormControl><Input maxLength={30} autoComplete="name" placeholder="请输入昵称" {...field} /></FormControl><FormDescription className="sr-only">最多输入 30 个字符</FormDescription><FormMessage /></div></FormItem>} />
                <FormField control={profileForm.control} name="mobile" render={({ field }) => <FormItem><FormLabel>手机号</FormLabel><div className="profile-field-control"><FormControl><Input maxLength={11} inputMode="numeric" autoComplete="tel" placeholder="请输入手机号" {...field} /></FormControl><FormDescription className="sr-only">请输入 11 位中国大陆手机号，可以留空</FormDescription><FormMessage /></div></FormItem>} />
                <div data-slot="form-item" className="profile-readonly-field"><label htmlFor="profile-current-email">登录邮箱</label><div className="profile-email-setting"><Input id="profile-current-email" value={profile.email || ''} placeholder="尚未绑定邮箱" disabled /><Button type="button" variant="outline" onClick={() => { emailForm.reset(EMPTY_EMAIL_FORM); setEmailDialogOpen(true) }}><MailOutlined />{profile.email ? '更换邮箱' : '绑定邮箱'}</Button></div></div>
                <FormField control={profileForm.control} name="sex" render={({ field }) => <FormItem><FormLabel>性别</FormLabel><div className="profile-field-control"><FormControl><RadioGroup value={String(field.value)} onValueChange={value => field.onChange(Number(value))} className="profile-radio-options">{[{ label: '保密', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }].map(option => <label key={option.value} htmlFor={`profile-sex-${option.value}`}><RadioGroupItem id={`profile-sex-${option.value}`} value={String(option.value)} />{option.label}</label>)}</RadioGroup></FormControl><FormDescription className="sr-only">选择在系统内展示的性别信息</FormDescription><FormMessage /></div></FormItem>} />
                <div className="profile-form-actions"><span>保存后会同步更新侧栏中的账户信息。</span><Button type="submit" loading={profileMutation.isPending}>保存更改</Button></div>
              </form>
            </Form>
          </div>
        ) : (
          <div id="profile-panel-security" className="profile-panel" role="tabpanel" aria-labelledby="profile-tab-security">
            <div className="profile-panel-heading"><div><h2>登录安全</h2><p>{profile.email ? '更新当前账户的登录密码。' : '绑定邮箱后即可设置和修改登录密码。'}</p></div><KeyOutlined /></div>
            <div className="profile-security-note"><LockOutlined /><div><strong>密码要求</strong><p>{passwordPolicyDescription(passwordPolicy)}</p></div></div>
            {profile.email ? <Form {...passwordForm}>
              <form className="profile-settings-form profile-password-form" onSubmit={passwordForm.handleSubmit(values => passwordMutation.mutate(values))}>
                <FormField control={passwordForm.control} name="oldPassword" render={({ field }) => <FormItem><FormLabel>当前密码</FormLabel><div className="profile-field-control"><FormControl><PasswordInput autoComplete="current-password" placeholder="请输入当前密码" {...field} /></FormControl><FormDescription className="sr-only">输入当前正在使用的登录密码</FormDescription><FormMessage /></div></FormItem>} />
                <FormField control={passwordForm.control} name="newPassword" render={({ field }) => <FormItem><FormLabel>新密码</FormLabel><div className="profile-field-control"><FormControl><PasswordInput autoComplete="new-password" placeholder="请输入新密码" {...field} /></FormControl><FormDescription className="sr-only">新密码需要符合上方密码要求</FormDescription><FormMessage /></div></FormItem>} />
                <FormField control={passwordForm.control} name="confirmPassword" render={({ field }) => <FormItem><FormLabel>确认新密码</FormLabel><div className="profile-field-control"><FormControl><PasswordInput autoComplete="new-password" placeholder="请再次输入新密码" {...field} /></FormControl><FormDescription className="sr-only">再次输入相同的新密码</FormDescription><FormMessage /></div></FormItem>} />
                <div className="profile-form-actions"><span>更新后，下一次登录请使用新密码。</span><Button type="submit" loading={passwordMutation.isPending}>更新密码</Button></div>
              </form>
            </Form> : <Button type="button" onClick={() => setEmailDialogOpen(true)}><MailOutlined />绑定邮箱并设置密码</Button>}
          </div>
        )}
      </div>

      <Dialog open={emailDialogOpen} onOpenChange={open => { setEmailDialogOpen(open); if (!open) { emailForm.reset(EMPTY_EMAIL_FORM); setCodeCountdown(0); setBindPassword('') } }}>
        <DialogContent className="profile-email-dialog max-w-[500px]">
          <Form {...emailForm}>
            <form onSubmit={emailForm.handleSubmit(values => emailMutation.mutate(values))}>
              <DialogHeader><DialogTitle>{profile.email ? '更换登录邮箱' : '绑定登录邮箱'}</DialogTitle><DialogDescription className="profile-email-dialog-description">{profile.email ? '验证码将发送到新邮箱，验证通过后用于后续登录。' : '验证邮箱并设置密码后，该账号也可以使用邮箱密码登录。'}</DialogDescription></DialogHeader>
              <div className="management-dialog-body profile-email-dialog-form">
                <FormField control={emailForm.control} name="email" render={({ field }) => <FormItem><FormLabel>新邮箱</FormLabel><div className="profile-field-control"><FormControl><Input type="email" autoComplete="email" placeholder="请输入新邮箱" {...field} /></FormControl><FormDescription className="sr-only">输入接收验证码并用于后续登录的新邮箱</FormDescription><FormMessage /></div></FormItem>} />
                <FormField control={emailForm.control} name="code" render={({ field }) => <FormItem><FormLabel>验证码</FormLabel><div className="profile-code-field"><FormControl><Input inputMode="numeric" maxLength={8} placeholder="4-8 位数字" {...field} /></FormControl><Button type="button" variant="outline" loading={sendCodeMutation.isPending} disabled={codeCountdown > 0} onClick={() => void sendEmailCode()}>{codeCountdown > 0 ? `${codeCountdown} 秒` : '发送验证码'}</Button><FormMessage /></div><FormDescription className="sr-only">请输入新邮箱收到的验证码</FormDescription></FormItem>} />
                {!profile.email ? <div data-slot="form-item"><label htmlFor="profile-bind-email-password">登录密码</label><div className="profile-field-control"><PasswordInput id="profile-bind-email-password" value={bindPassword} onChange={event => setBindPassword(event.target.value)} autoComplete="new-password" placeholder={passwordPolicyDescription(passwordPolicy)} /></div></div> : null}
              </div>
              <DialogFooter><DialogClose asChild><Button type="button" variant="outline">取消</Button></DialogClose><Button type="submit" loading={emailMutation.isPending}>{profile.email ? '确认更换' : '完成绑定'}</Button></DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </section>
  )
}
