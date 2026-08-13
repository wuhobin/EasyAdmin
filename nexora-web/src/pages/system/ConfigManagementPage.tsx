import { InfoCircleOutlined, ReloadOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AntApp from 'antd/es/app'
import { AlertCircle, LoaderCircle } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useForm, type UseFormReturn } from 'react-hook-form'
import {
  getConfigGroupApi,
  getConfigGroupListApi,
  refreshConfigGroupCacheApi,
  testConfigEmailApi,
  updateConfigGroupApi,
  type ConfigGroupCode,
  type ConfigGroupSummary,
  type ConfigValueByGroup
} from '@/api/config'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/components/ui/password-input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { configDetailToForm, emptyConfigForm, parseConfigForm, type ConfigFormValues } from '@/pages/system/configForm'
import { useAuthStore } from '@/store/authStore'
import { usePublicConfigStore } from '@/store/publicConfigStore'
import { formatDateTime } from '@/utils/format'
import { useUrlQueryState, type UrlQuerySchema } from '@/utils/urlQueryState'

const groupDefinitions: Array<{ code: ConfigGroupCode; name: string }> = [
  { code: 'system', name: '系统配置' },
  { code: 'register', name: '注册配置' },
  { code: 'login', name: '登录配置' },
  { code: 'password', name: '密码配置' },
  { code: 'email', name: '邮箱配置' }
]

type TextFieldName = 'siteName' | 'shortTitle' | 'siteLogo' | 'copyright' | 'icp' | 'watermarkCustomText' | 'defaultRoleCode' | 'host' | 'username' | 'password' | 'fromName'
type NumberFieldName = 'maxRetryCount' | 'lockTimeMinutes' | 'sessionTimeoutSeconds' | 'rememberMeTimeoutSeconds' | 'minLength' | 'maxLength' | 'port'
type BooleanFieldName = 'enabled' | 'captchaEnabled' | 'verifyEmail' | 'needAudit' | 'rememberMeEnabled' | 'singleLogin' | 'requireUppercase' | 'requireLowercase' | 'requireNumber' | 'requireSpecial' | 'ssl'

function ConfigTextField({ form, name, label, placeholder, maxLength, disabled, password = false }: { form: UseFormReturn<ConfigFormValues>; name: TextFieldName; label: string; placeholder?: string; maxLength?: number; disabled?: boolean; password?: boolean }) {
  const Control = password ? PasswordInput : Input
  return <FormField control={form.control} name={name} render={({ field }) => <FormItem className="config-field"><FormLabel>{label}</FormLabel><FormControl><Control {...field} value={field.value} disabled={disabled} maxLength={maxLength} placeholder={placeholder} /></FormControl><FormMessage /></FormItem>} />
}

function ConfigNumberField({ form, name, label, min, max, step = 1, disabled }: { form: UseFormReturn<ConfigFormValues>; name: NumberFieldName; label: string; min: number; max: number; step?: number; disabled?: boolean }) {
  return <FormField control={form.control} name={name} render={({ field }) => <FormItem className="config-field"><FormLabel>{label}</FormLabel><FormControl><Input type="number" min={min} max={max} step={step} value={Number.isFinite(field.value) ? field.value : ''} disabled={disabled} onBlur={field.onBlur} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><FormMessage /></FormItem>} />
}

function ConfigToggleField({ form, name, label, hint, disabled }: { form: UseFormReturn<ConfigFormValues>; name: BooleanFieldName; label: string; hint?: string; disabled?: boolean }) {
  return <FormField control={form.control} name={name} render={({ field }) => <FormItem className="config-toggle-field"><div className="config-toggle-copy"><FormLabel>{label}</FormLabel>{hint ? <small>{hint}</small> : null}</div><FormControl><Switch checked={field.value} onCheckedChange={field.onChange} disabled={disabled} /></FormControl><FormMessage /></FormItem>} />
}

function SystemConfigFields({ form, disabled }: { form: UseFormReturn<ConfigFormValues>; disabled: boolean }) {
  const watermarkType = form.watch('watermarkType')
  const siteLogo = form.watch('siteLogo')
  const opacity = form.watch('watermarkOpacity')
  return <>
    <section className="config-section"><div className="config-section-heading"><h2>站点信息</h2><p>设置后台的名称、说明与品牌标识。</p></div><div className="config-field-grid">
      <ConfigTextField form={form} name="siteName" label="站点名称" maxLength={100} disabled={disabled} />
      <ConfigTextField form={form} name="shortTitle" label="后台短标题" maxLength={100} disabled={disabled} />
      <FormField control={form.control} name="siteDescription" render={({ field }) => <FormItem className="config-field config-span-2"><FormLabel>站点描述</FormLabel><FormControl><Textarea {...field} rows={3} maxLength={500} disabled={disabled} /></FormControl><div className="config-character-count">{field.value.length}/500</div><FormMessage /></FormItem>} />
      <FormField control={form.control} name="siteLogo" render={({ field }) => <FormItem className="config-field config-span-2"><FormLabel>Logo 地址</FormLabel><div className="config-logo-field"><FormControl><Input {...field} maxLength={1024} disabled={disabled} placeholder="留空时使用 Nexora 默认 Logo" /></FormControl><div className="config-logo-preview">{siteLogo ? <img src={siteLogo} alt="Logo 预览" width={36} height={36} /> : <span>默认</span>}</div></div><FormMessage /></FormItem>} />
    </div></section>
    <section className="config-section"><div className="config-section-heading"><h2>备案信息</h2><p>维护页面底部展示的版权和备案内容。</p></div><div className="config-field-grid">
      <ConfigTextField form={form} name="copyright" label="版权信息" maxLength={255} disabled={disabled} />
      <ConfigTextField form={form} name="icp" label="ICP备案号" placeholder="可留空" maxLength={100} disabled={disabled} />
    </div></section>
    <section className="config-section"><div className="config-section-heading"><h2>页面水印</h2><p>控制后台页面水印的内容与可见程度。</p></div><div className="config-field-grid config-watermark-grid">
      <FormField control={form.control} name="watermarkEnabled" render={({ field }) => <FormItem className="config-toggle-field"><div className="config-toggle-copy"><FormLabel>强制开启水印</FormLabel><small>开启后在后台内容区域显示水印。</small></div><FormControl><Switch checked={field.value} onCheckedChange={field.onChange} disabled={disabled} /></FormControl><FormMessage /></FormItem>} />
      <FormField control={form.control} name="watermarkOpacity" render={({ field }) => <FormItem className="config-field"><FormLabel>水印透明度</FormLabel><div className="config-range-row"><FormControl><input className="config-range" type="range" min={0.01} max={0.5} step={0.01} value={field.value} disabled={disabled} onChange={event => field.onChange(event.target.valueAsNumber)} /></FormControl><output>{Math.round(opacity * 100)}%</output></div><FormMessage /></FormItem>} />
      <FormField control={form.control} name="watermarkType" render={({ field }) => <FormItem className="config-field"><FormLabel>水印内容</FormLabel><Select value={field.value} onValueChange={field.onChange} disabled={disabled}><FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl><SelectContent><SelectItem value="username">用户名</SelectItem><SelectItem value="username_time">用户名 + 时间</SelectItem><SelectItem value="sitename">站点名称</SelectItem><SelectItem value="custom">自定义文本</SelectItem></SelectContent></Select><FormMessage /></FormItem>} />
      <ConfigTextField form={form} name="watermarkCustomText" label="自定义水印文本" placeholder="选择自定义文本时使用" maxLength={100} disabled={disabled || watermarkType !== 'custom'} />
    </div></section>
  </>
}

function RegisterConfigFields({ form, disabled }: { form: UseFormReturn<ConfigFormValues>; disabled: boolean }) {
  return <section className="config-section"><div className="config-section-heading"><h2>注册策略</h2><p>控制注册入口、验证方式和新用户初始权限。</p></div><div className="config-field-grid">
    <ConfigToggleField form={form} name="enabled" label="开放用户注册" hint="关闭后隐藏注册入口并拒绝注册请求。" disabled={disabled} />
    <ConfigToggleField form={form} name="captchaEnabled" label="注册滑块验证" hint="创建账号前必须完成滑块验证。" disabled={disabled} />
    <ConfigToggleField form={form} name="verifyEmail" label="验证邮箱" hint="注册时要求完成邮箱验证码校验。" disabled={disabled} />
    <ConfigToggleField form={form} name="needAudit" label="注册后审核" hint="新用户需管理员审核后才能使用。" disabled={disabled} />
    <ConfigTextField form={form} name="defaultRoleCode" label="默认角色编码" placeholder="例如 user" maxLength={50} disabled={disabled} />
  </div></section>
}

function LoginConfigFields({ form, disabled }: { form: UseFormReturn<ConfigFormValues>; disabled: boolean }) {
  return <>
    <section className="config-section"><div className="config-section-heading"><h2>会话策略</h2><p>设置记住登录状态和同账号会话规则。</p></div><div className="config-field-grid">
      <ConfigToggleField form={form} name="rememberMeEnabled" label="允许记住我" hint="关闭后统一使用普通会话时长。" disabled={disabled} />
      <ConfigToggleField form={form} name="singleLogin" label="单点登录" hint="建立新会话前踢出该账号其他会话。" disabled={disabled} />
    </div></section>
    <section className="config-section"><div className="config-section-heading"><h2>安全限制</h2><p>控制密码重试、账号锁定和会话有效期。</p></div><div className="config-field-grid">
      <ConfigNumberField form={form} name="maxRetryCount" label="最大密码重试次数" min={1} max={20} disabled={disabled} />
      <ConfigNumberField form={form} name="lockTimeMinutes" label="锁定时间（分钟）" min={1} max={1440} disabled={disabled} />
      <ConfigNumberField form={form} name="sessionTimeoutSeconds" label="普通会话时长（秒）" min={300} max={86400} step={300} disabled={disabled} />
      <ConfigNumberField form={form} name="rememberMeTimeoutSeconds" label="记住我会话时长（秒）" min={3600} max={31536000} step={3600} disabled={disabled} />
    </div></section>
  </>
}

function PasswordConfigFields({ form, disabled }: { form: UseFormReturn<ConfigFormValues>; disabled: boolean }) {
  return <>
    <section className="config-section"><div className="config-section-heading"><h2>长度限制</h2><p>设置所有账户密码可接受的字符长度范围。</p></div><div className="config-field-grid">
      <ConfigNumberField form={form} name="minLength" label="最小长度" min={6} max={32} disabled={disabled} />
      <ConfigNumberField form={form} name="maxLength" label="最大长度" min={6} max={64} disabled={disabled} />
      <div className="config-notice config-span-2"><InfoCircleOutlined /><span>BCrypt 仅接受不超过 72 字节的 UTF-8 输入，前后端会同时校验此限制。</span></div>
    </div></section>
    <section className="config-section"><div className="config-section-heading"><h2>复杂度要求</h2><p>按需组合密码必须包含的字符类型。</p></div><div className="config-field-grid">
      <ConfigToggleField form={form} name="requireUppercase" label="必须包含大写字母" hint="A-Z" disabled={disabled} />
      <ConfigToggleField form={form} name="requireLowercase" label="必须包含小写字母" hint="a-z" disabled={disabled} />
      <ConfigToggleField form={form} name="requireNumber" label="必须包含数字" hint="0-9" disabled={disabled} />
      <ConfigToggleField form={form} name="requireSpecial" label="必须包含特殊字符" hint="非字母、非数字字符" disabled={disabled} />
    </div></section>
  </>
}

function EmailConfigFields({ form, disabled, testing, onTest }: { form: UseFormReturn<ConfigFormValues>; disabled: boolean; testing: boolean; onTest: (address: string) => void }) {
  const enabled = form.watch('enabled')
  const [testAddress, setTestAddress] = useState('')
  return <>
    <section className="config-section"><div className="config-section-heading"><h2>SMTP 连接</h2><p>配置系统验证邮件与通知邮件的发送通道。</p></div><div className="config-field-grid">
      <ConfigToggleField form={form} name="enabled" label="启用邮件" hint="启用后系统可发送验证和通知邮件。" disabled={disabled} />
      <ConfigToggleField form={form} name="ssl" label="SSL 加密" hint="根据邮件服务商要求启用加密连接。" disabled={disabled} />
      <ConfigTextField form={form} name="host" label="SMTP 服务器" placeholder="如 smtp.qq.com" maxLength={255} disabled={disabled} />
      <ConfigNumberField form={form} name="port" label="端口" min={1} max={65535} disabled={disabled} />
      <ConfigTextField form={form} name="username" label="用户名" placeholder="SMTP 登录用户名" maxLength={255} disabled={disabled} />
      <ConfigTextField form={form} name="password" label="密码 / 授权码" placeholder="邮箱密码或授权码" maxLength={255} disabled={disabled} password />
      <ConfigTextField form={form} name="fromName" label="发件人名称" placeholder="显示的发件人名称" maxLength={100} disabled={disabled} />
    </div></section>
    <section className="config-section"><div className="config-section-heading"><h2>发送测试</h2><p>保存配置后，向指定地址发送一封连通性测试邮件。</p></div><div className="config-test-email"><label className="sr-only" htmlFor="config-test-email">测试收件人邮箱</label><Input id="config-test-email" name="config-test-email" type="email" autoComplete="off" spellCheck={false} value={testAddress} maxLength={254} onChange={event => setTestAddress(event.target.value)} placeholder="输入测试收件人邮箱…" disabled={disabled || !enabled} /><Button type="button" loading={testing} disabled={disabled || !enabled} onClick={() => onTest(testAddress.trim())}><SendOutlined />发送测试邮件</Button></div></section>
  </>
}

interface ConfigPageState { group: ConfigGroupCode }
const initialPageState: ConfigPageState = { group: 'system' }
const pageStateSchema: UrlQuerySchema<ConfigPageState> = { group: 'string' }

export function ConfigManagementPage() {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const permissions = useAuthStore(state => state.user.permissions)
  const canUpdate = permissions.includes('sys:config:update')
  const [pageState, setPageState] = useUrlQueryState(initialPageState, pageStateSchema)
  const activeGroup = groupDefinitions.some(definition => definition.code === pageState.group) ? pageState.group : 'system'
  const activeTabRef = useRef<HTMLButtonElement>(null)
  const form = useForm<ConfigFormValues>({ defaultValues: emptyConfigForm })

  const summariesQuery = useQuery({ queryKey: ['config-groups'], queryFn: async () => (await getConfigGroupListApi()).data })
  const detailQuery = useQuery({ queryKey: ['config-group', activeGroup], queryFn: async () => (await getConfigGroupApi(activeGroup)).data })
  const activeSummary = useMemo(() => summariesQuery.data?.find(item => item.groupCode === activeGroup), [activeGroup, summariesQuery.data])
  const tabs = useMemo(() => groupDefinitions.map(definition => summariesQuery.data?.find(item => item.groupCode === definition.code) ?? { id: 0, groupCode: definition.code, groupName: definition.name, sort: 0, updateTime: '' } satisfies ConfigGroupSummary), [summariesQuery.data])
  const detailReady = detailQuery.data?.groupCode === activeGroup && !detailQuery.isFetching && !detailQuery.isError

  useEffect(() => {
    if (detailQuery.data) form.reset(configDetailToForm(detailQuery.data.configValue))
  }, [detailQuery.data, form])

  useEffect(() => {
    activeTabRef.current?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  }, [activeGroup])

  const saveMutation = useMutation({
    mutationFn: ({ groupCode, data }: { groupCode: ConfigGroupCode; data: ConfigValueByGroup[ConfigGroupCode] }) => updateConfigGroupApi(groupCode, data),
    onSuccess: async (_, variables) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['config-groups'] }),
        queryClient.invalidateQueries({ queryKey: ['config-group', variables.groupCode] })
      ])
      const savedGroup = tabs.find(tab => tab.groupCode === variables.groupCode)
      message.success(`${savedGroup?.groupName ?? '配置分组'}已保存`)
      if (!(await usePublicConfigStore.getState().load(true))) message.warning('配置已保存，但公共配置刷新失败，请稍后重试')
    },
    onError: () => message.error('配置保存失败，请检查填写内容')
  })

  const refreshMutation = useMutation({
    mutationFn: refreshConfigGroupCacheApi,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['config-groups'] }),
        queryClient.invalidateQueries({ queryKey: ['config-group'] }),
        usePublicConfigStore.getState().load(true)
      ])
      message.success('全部配置缓存已刷新')
    },
    onError: () => message.error('配置缓存刷新失败')
  })

  const testEmailMutation = useMutation({
    mutationFn: testConfigEmailApi,
    onSuccess: () => message.success('测试邮件发送成功，请查收'),
    onError: () => message.error('测试邮件发送失败，请检查 SMTP 配置')
  })

  const saveCurrentGroup = () => {
    if (!canUpdate || !detailReady || saveMutation.isPending) return
    form.clearErrors()
    const parsed = parseConfigForm(activeGroup, form.getValues())
    if (!parsed.success) {
      parsed.error.issues.forEach(issue => {
        const name = issue.path[0]
        if (typeof name === 'string') form.setError(name as keyof ConfigFormValues, { type: 'manual', message: issue.message })
      })
      const firstField = parsed.error.issues[0]?.path[0]
      if (typeof firstField === 'string') form.setFocus(firstField as keyof ConfigFormValues)
      return
    }
    saveMutation.mutate({ groupCode: activeGroup, data: parsed.data })
  }

  const sendTestEmail = (address: string) => {
    if (address.length > 254) {
      message.warning('测试收件人邮箱不能超过 254 个字符')
      return
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(address)) {
      message.warning('请输入正确的测试收件人邮箱')
      return
    }
    testEmailMutation.mutate(address)
  }

  const changeGroup = (groupCode: ConfigGroupCode) => {
    if (groupCode === activeGroup) return
    form.clearErrors()
    form.reset(emptyConfigForm)
    setPageState({ group: groupCode })
  }

  const handleTabKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    const direction = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0
    const nextIndex = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : direction ? (index + direction + tabs.length) % tabs.length : -1
    if (nextIndex < 0) return
    event.preventDefault()
    const groupCode = tabs[nextIndex].groupCode
    changeGroup(groupCode)
    window.requestAnimationFrame(() => document.getElementById(`config-tab-${groupCode}`)?.focus())
  }

  return <section className="config-management-page"><div className="config-management-canvas">
    <header className="config-toolbar"><div className="config-tabs" role="tablist" aria-label="配置分组">{tabs.map((tab, index) => <button ref={activeGroup === tab.groupCode ? activeTabRef : undefined} id={`config-tab-${tab.groupCode}`} key={tab.groupCode} type="button" role="tab" tabIndex={activeGroup === tab.groupCode ? 0 : -1} aria-selected={activeGroup === tab.groupCode} aria-controls="config-panel" className={`config-tab ${activeGroup === tab.groupCode ? 'is-active' : ''}`} onClick={() => changeGroup(tab.groupCode)} onKeyDown={event => handleTabKeyDown(event, index)}>{tab.groupName}</button>)}</div><div className="config-toolbar-meta">{activeSummary?.updateTime ? <span>最近更新 {formatDateTime(activeSummary.updateTime)}</span> : null}{canUpdate ? <div className="config-actions"><Button type="button" variant="outline" loading={refreshMutation.isPending} onClick={() => refreshMutation.mutate()}><ReloadOutlined />刷新缓存</Button><Button type="button" loading={saveMutation.isPending} disabled={!detailReady} onClick={saveCurrentGroup}><SaveOutlined />保存当前分组</Button></div> : null}</div></header>
    <Form {...form}><form id="config-panel" role="tabpanel" aria-labelledby={`config-tab-${activeGroup}`} className="config-form" onSubmit={event => { event.preventDefault(); saveCurrentGroup() }}>
      {detailQuery.isFetching ? <div className="config-loading" role="status"><LoaderCircle className="animate-spin" /><span>正在加载配置…</span></div> : null}
      {detailQuery.isError ? <div className="config-error" role="alert"><AlertCircle /><div><strong>配置加载失败</strong><span>当前分组暂时无法读取，请重试。</span></div><Button type="button" variant="outline" onClick={() => void detailQuery.refetch()}><ReloadOutlined />重新加载</Button></div> : <fieldset className="config-fieldset" disabled={!canUpdate || !detailReady}>
        {activeGroup === 'system' ? <SystemConfigFields form={form} disabled={!canUpdate} /> : null}
        {activeGroup === 'register' ? <RegisterConfigFields form={form} disabled={!canUpdate} /> : null}
        {activeGroup === 'login' ? <LoginConfigFields form={form} disabled={!canUpdate} /> : null}
        {activeGroup === 'password' ? <PasswordConfigFields form={form} disabled={!canUpdate} /> : null}
        {activeGroup === 'email' ? <EmailConfigFields form={form} disabled={!canUpdate} testing={testEmailMutation.isPending} onTest={sendTestEmail} /> : null}
      </fieldset>}
    </form></Form>
  </div></section>
}
