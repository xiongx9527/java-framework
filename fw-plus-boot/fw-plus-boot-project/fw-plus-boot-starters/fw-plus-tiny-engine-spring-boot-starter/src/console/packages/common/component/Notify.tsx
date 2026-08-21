import { Notify } from '@opentiny/vue'

const durationMap = {
  info: 5000,
  success: 5000,
  warning: 10000,
  error: 10000
}

let lastErrorNotifyAt = 0

export interface NotifyOptions {
  [key: string]: any
  title?: string
  message: string
  type: keyof typeof durationMap
  customClass?: string
  position?: string
}

const useNotify = (config: NotifyOptions) => {
  const { customClass, title, type = 'info', position = 'top-right', ...otherConfig } = config

  // HTTP 拦截器和业务 catch 可能同时提示同一个错误，避免短时间重复弹窗。
  if (type === 'error') {
    const now = Date.now()
    if (now - lastErrorNotifyAt < 300) return
    lastErrorNotifyAt = now
  }

  Notify({
    duration: durationMap[type],
    ...otherConfig,
    position,
    title,
    type,
    customClass: `${customClass}`,
    verticalOffset: 46
  })
}

export default useNotify
