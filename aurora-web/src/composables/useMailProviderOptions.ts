import { ElMessage } from 'element-plus'
import { getDictDataListApi, getDictListApi } from '@/api/system/dict'
import type { MailProvider } from '@/api/mail'

const MAIL_PROVIDER_DICT_TYPE = 'mail_provider'

export interface MailProviderOption {
  label: string
  value: MailProvider
  isDefault: boolean
}

export function useMailProviderOptions() {
  const providerOptions = ref<MailProviderOption[]>([])
  const providerOptionsLoading = ref(false)

  const loadProviderOptions = async () => {
    providerOptionsLoading.value = true
    try {
      const { data: dictList } = await getDictListApi({ status: 1, pageNum: 1, pageSize: 1000 })
      const dict = dictList.records?.find((item: any) => item.type === MAIL_PROVIDER_DICT_TYPE)
      if (!dict) {
        ElMessage.error('未配置邮箱类型字典 mail_provider')
        providerOptions.value = []
        return
      }

      const { data: dictData } = await getDictDataListApi({ dictId: dict.id, pageNum: 1, pageSize: 100 })
      providerOptions.value = dictData.records
        .filter((item: any) => item.status === 1)
        .map((item: any) => ({
          label: item.label,
          value: item.value as MailProvider,
          isDefault: item.isDefault === '1'
        }))

      if (!providerOptions.value.length) {
        ElMessage.error('邮箱类型字典没有可用数据')
      }
    } catch {
      providerOptions.value = []
    } finally {
      providerOptionsLoading.value = false
    }
  }

  const defaultProvider = () => providerOptions.value.find((item) => item.isDefault)?.value
    || providerOptions.value[0]?.value
    || ('' as MailProvider)

  const providerLabel = (provider: MailProvider) => providerOptions.value
    .find((item) => item.value === provider)?.label || provider

  return {
    providerOptions,
    providerOptionsLoading,
    loadProviderOptions,
    defaultProvider,
    providerLabel
  }
}
