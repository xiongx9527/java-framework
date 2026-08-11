import { inject, onBeforeUnmount, onMounted, watch, type WritableComputedRef } from 'vue'
import { I18nInjectionKey } from 'vue-i18n'
import { useBroadcastChannel } from '@vueuse/core'
import { constants } from '@opentiny/tiny-engine-utils'

const { BROADCAST_CHANNEL } = constants
const TINY_ENGINE_LOCALE_CHANGE = 'tiny-engine:locale-change'
const LOCALE_STORAGE_KEY = 'tiny-engine-locale'

const normalizeLocale = (value: unknown) => (String(value) === 'en_US' ? 'en_US' : 'zh_CN')

const persistLocale = (localeValue: string) => {
  if (typeof window !== 'undefined' && window.localStorage) {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, localeValue)
  }

  if (typeof document !== 'undefined') {
    document.documentElement?.setAttribute?.('lang', localeValue === 'en_US' ? 'en' : 'zh-CN')
  }
}

export function useLocale() {
  const { locale } = inject(I18nInjectionKey).global
  const { data } = useBroadcastChannel({ name: BROADCAST_CHANNEL.CanvasLang })

  const setLocale = (value: unknown) => {
    if (value) {
      const localeValue = normalizeLocale(value)
      ;(locale as WritableComputedRef<unknown>).value = localeValue
      persistLocale(localeValue)
    }
  }

  watch(data, () => {
    setLocale(data.value)
  })

  const handleLocaleMessage = (event: MessageEvent) => {
    const message = event?.data
    if (message?.type !== TINY_ENGINE_LOCALE_CHANGE || message?.channel !== BROADCAST_CHANNEL.CanvasLang) {
      return
    }

    setLocale(message.value)
  }

  onMounted(() => {
    window.addEventListener('message', handleLocaleMessage)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('message', handleLocaleMessage)
  })
}
