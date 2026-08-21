/**
 * Copyright (c) 2023 - present TinyEngine Authors.
 * Copyright (c) 2023 - present Huawei Cloud Computing Technologies Co., Ltd.
 *
 * Use of this source code is governed by an MIT-style license.
 *
 * THE OPEN SOURCE SOFTWARE IN THIS PRODUCT IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE. SEE THE APPLICABLE LICENSES FOR MORE DETAILS.
 *
 */

import { createI18n, I18nInjectionKey } from 'vue-i18n'
import i18n, { defineCustomI18n } from '@opentiny/tiny-engine-i18n-host'
import tinyLocale from '@opentiny/vue-locale'
import { i18nKeyMaps } from './constants'
import enUs from '../i18n/en-us.json'
import zhCn from '../i18n/zh-cn.json'

export const LOCALE_STORAGE_KEY = 'tiny-engine-locale'

// 类型定义
type LocaleKeyMap = keyof typeof i18nKeyMaps
type LocaleMessages = Record<string, any>

interface CreateI18nParams {
  locale: string
  messages: Record<string, LocaleMessages>
}

export const normalizeLocale = (value?: unknown) =>
  String(value) === i18nKeyMaps.enUS ? i18nKeyMaps.enUS : i18nKeyMaps.zhCN

const getLocaleStorage = () => {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage
}

const setDocumentLocale = (locale: string) => {
  if (typeof document === 'undefined') {
    return
  }

  document.documentElement?.setAttribute?.('lang', locale === i18nKeyMaps.enUS ? 'en' : 'zh-CN')
}

export const getInitialLocale = () => normalizeLocale(getLocaleStorage()?.getItem(LOCALE_STORAGE_KEY))

// 此处处理TinyVue组件库的国际化zhCN --> zh_CN
const customCreateI18n = ({ locale, messages }: CreateI18nParams) => {
  const newMessages: Record<string, LocaleMessages> = {}
  Object.keys(messages).forEach((key) => {
    const lang = i18nKeyMaps[key as LocaleKeyMap]
    newMessages[lang] = messages[key]
  })

  return createI18n({
    locale,
    messages: newMessages,
    legacy: false
  })
}

// 初始化 TinyVue 组件库的 i18n 实例
// 注意：@opentiny/vue-locale 可能缺少完整的类型定义，此处暂时使用 any 类型
const initialLocale = getInitialLocale()
const customI18n: any = tinyLocale.initI18n({
  i18n: { locale: initialLocale },
  createI18n: customCreateI18n,
  messages: {}
})

// 合并组件库的i18n配置
defineCustomI18n(customI18n)

const { mergeLocaleMessage } = i18n.global

mergeLocaleMessage(i18nKeyMaps.enUS, enUs)
mergeLocaleMessage(i18nKeyMaps.zhCN, zhCn)

export const applyLocaleToI18n = (value?: unknown) => {
  const locale = normalizeLocale(value)

  i18n.global.locale.value = locale
  getLocaleStorage()?.setItem(LOCALE_STORAGE_KEY, locale)
  setDocumentLocale(locale)

  return locale
}

setDocumentLocale(initialLocale)

// 导出类型
export type { LocaleKeyMap, LocaleMessages, CreateI18nParams }

export { I18nInjectionKey, i18nKeyMaps }

// i18n对象可以多处使用。模板中直接使用$t，setup环境或普通环境中可以引入后使用i18n.global.t
export default i18n
