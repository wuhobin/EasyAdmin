import { getMailProvidersApi, type MailProvider, type MailProviderConfig } from '@/api/mail'

export function useMailProviderOptions() {
  const providerOptions = ref<MailProviderConfig[]>([])
  const providerOptionsLoading = ref(false)

  const loadProviderOptions = async () => {
    providerOptionsLoading.value = true
    try {
      const { data } = await getMailProvidersApi()
      providerOptions.value = data
    } catch {
      providerOptions.value = []
    } finally {
      providerOptionsLoading.value = false
    }
  }

  const defaultProvider = () => providerOptions.value.find((item) => item.defaultProvider)?.value
    || providerOptions.value[0]?.value
    || ('' as MailProvider)

  const providerLabel = (provider: MailProvider) => providerOptions.value
    .find((item) => item.value === provider)?.label || provider

  const providerDomain = (provider: MailProvider) => providerOptions.value
    .find((item) => item.value === provider)?.domain || ''

  return {
    providerOptions,
    providerOptionsLoading,
    loadProviderOptions,
    defaultProvider,
    providerLabel,
    providerDomain
  }
}
