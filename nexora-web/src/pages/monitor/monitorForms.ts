import type { JobPayload, JobRecord } from '@/api/job'
import type { ManagedServer, ManagedServerPayload } from '@/api/server'

export interface ManagedServerFormValues {
  id?: number
  name: string
  host: string
  port: number
  username: string
  password: string
  savePassword: boolean
  clearSavedPassword: boolean
  description: string
  enabled: number
  sort: number
}

export const emptyManagedServerForm: ManagedServerFormValues = {
  name: '',
  host: '',
  port: 22,
  username: 'root',
  password: '',
  savePassword: false,
  clearSavedPassword: false,
  description: '',
  enabled: 1,
  sort: 0
}

export function managedServerToForm(server: ManagedServer): ManagedServerFormValues {
  return {
    id: server.id,
    name: server.name,
    host: server.host,
    port: server.port,
    username: server.username,
    password: '',
    savePassword: false,
    clearSavedPassword: false,
    description: server.description || '',
    enabled: server.enabled,
    sort: server.sort
  }
}

export function managedServerFormToPayload(values: ManagedServerFormValues): ManagedServerPayload {
  const password = values.password || undefined
  const savePassword = Boolean(password && values.savePassword)
  return {
    ...(values.id === undefined ? {} : { id: values.id }),
    name: values.name.trim(),
    host: values.host.trim(),
    port: values.port,
    username: values.username.trim(),
    password,
    savePassword,
    clearSavedPassword: Boolean(values.clearSavedPassword && !savePassword),
    description: values.description.trim(),
    enabled: values.enabled,
    sort: values.sort
  }
}

export interface JobFormValues {
  jobId?: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy: string
  concurrent: string
  status: string
}

export const emptyJobForm: JobFormValues = {
  jobName: '',
  jobGroup: 'DEFAULT',
  invokeTarget: '',
  cronExpression: '0 0 * * * ?',
  misfirePolicy: '1',
  concurrent: '1',
  status: '1'
}

export function jobRecordToForm(record: JobRecord): JobFormValues {
  return { ...record }
}

export function jobFormToPayload(values: JobFormValues): JobPayload {
  return {
    ...(values.jobId === undefined ? {} : { jobId: values.jobId }),
    jobName: values.jobName.trim(),
    jobGroup: values.jobGroup,
    invokeTarget: values.invokeTarget.trim(),
    cronExpression: values.cronExpression.trim(),
    misfirePolicy: values.misfirePolicy,
    concurrent: values.concurrent,
    status: values.status
  }
}
