<template>
  <Main />
</template>

<script setup>
import { onBeforeUnmount, onMounted, provide, watch } from 'vue'
import { I18nInjectionKey, createI18n } from 'vue-i18n'
import { createPinia } from 'pinia'
import { useBroadcastChannel } from '@vueuse/core'
import { BROADCAST_CHANNEL, I18N_KEY_MAPS } from './constant.js'
import './injectGlobal.js'
import './app.js'
import lowcode from './lowcode.js'
import messages from './locales.js'
import Main from './Main.vue'
import locale from '@opentiny/vue-locale'

const TINY_ENGINE_LOCALE_CHANGE = 'tiny-engine:locale-change'
const LOCALE_STORAGE_KEY = 'tiny-engine-locale'

const normalizeLocale = (value) => (String(value) === 'en_US' ? 'en_US' : 'zh_CN')

const getInitialLocale = () => {
  if (typeof window === 'undefined' || !window.localStorage) {
    return 'zh_CN'
  }

  return normalizeLocale(window.localStorage.getItem(LOCALE_STORAGE_KEY))
}

const persistLocale = (localeValue) => {
  if (typeof window !== 'undefined' && window.localStorage) {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, localeValue)
  }

  if (typeof document !== 'undefined') {
    document.documentElement?.setAttribute?.('lang', localeValue === 'en_US' ? 'en' : 'zh-CN')
  }
}

const customCreateI18n = ({ locale, messages }) => {
  const newMessages = {}
  Object.keys(messages).forEach((key) => {
    const lang = I18N_KEY_MAPS[key] || key
    newMessages[lang] = messages[key]
  })

  return createI18n({
    locale,
    messages: newMessages,
    legacy: false
  })
}

const initialLocale = getInitialLocale()
const i18n = locale.initI18n({
  i18n: { locale: initialLocale },
  createI18n: customCreateI18n,
  messages: {
    zhCN: {
      change_lang: '切换语言'
    },
    enUS: {
      change_lang: 'change lang'
    }
  }
})

if (messages && typeof messages === 'object') {
  Object.entries(messages).forEach(([locale, message]) => {
    i18n.global.mergeLocaleMessage(locale, message)
  })
}

i18n.lowcode = lowcode

window.__app__.use(i18n)
provide(I18nInjectionKey, i18n)

const { data } = useBroadcastChannel({ name: BROADCAST_CHANNEL.PreviewLang })

const setLocale = (value) => {
  if (value) {
    const localeValue = normalizeLocale(value)
    i18n.global.locale.value = localeValue
    persistLocale(localeValue)
  }
}

watch(data, () => {
  setLocale(data.value)
})

const handleLocaleMessage = (event) => {
  const message = event?.data
  if (message?.type !== TINY_ENGINE_LOCALE_CHANGE || message?.channel !== BROADCAST_CHANNEL.PreviewLang) {
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

window.__app__.use(createPinia())
</script>

<style>
body {
  margin: 0;
}
</style>
