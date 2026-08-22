<template>
  <toolbar-base
    :content="toolbarTitle"
    :icon="options.icon.default || options.icon"
    :options="options"
    @click-api="changeLang"
  >
  </toolbar-base>
</template>

<script lang="ts">
/* metaService: engine.toolbars.lang.Main */
import { computed, ref, watch } from 'vue'
import { useBroadcastChannel } from '@vueuse/core'
import { ToolbarBase } from '@opentiny/tiny-engine-common'
import { applyLocaleToI18n, getInitialLocale } from '@opentiny/tiny-engine-common/js/i18n'

import { constants } from '@opentiny/tiny-engine-utils'

const { BROADCAST_CHANNEL } = constants
const TINY_ENGINE_LOCALE_CHANGE = 'tiny-engine:locale-change'

const postLocaleMessage = (channel, value) => {
  if (typeof window === 'undefined') {
    return
  }

  const message = {
    type: TINY_ENGINE_LOCALE_CHANGE,
    channel,
    value
  }

  window.postMessage(message, window.location.origin)

  if (window.parent && window.parent !== window) {
    window.parent.postMessage(message, window.location.origin)
  }

  Array.from(document.querySelectorAll('iframe')).forEach((iframe) => {
    iframe.contentWindow?.postMessage(message, '*')
  })
}

export default {
  components: {
    ToolbarBase
  },
  props: {
    langChannel: {
      type: String,
      default: BROADCAST_CHANNEL.CanvasLang
    },
    options: {
      type: Object,
      default: () => ({})
    }
  },
  setup(props) {
    const langVal = ref(getInitialLocale())
    const { post, data } = useBroadcastChannel({ name: props.langChannel })

    const applyLang = (value) => {
      langVal.value = applyLocaleToI18n(value)
      return langVal.value
    }

    watch(data, () => {
      applyLang(data.value)
    })

    const langOptions = [
      {
        value: 'zh_CN',
        label: '中文'
      },
      {
        value: 'en_US',
        label: 'English'
      }
    ]
    const toolbarTitle = computed(() => (langVal.value === langOptions[0].value ? '中英文切换' : 'Switch language'))

    const changeLang = () => {
      const nextLocale = langVal.value === langOptions[0].value ? langOptions[1].value : langOptions[0].value
      applyLang(nextLocale)
      post(langVal.value)
      postLocaleMessage(props.langChannel, langVal.value)
    }
    return {
      langOptions,
      langVal,
      toolbarTitle,
      changeLang
    }
  }
}
</script>
<style scoped lang="less">
.change-lang-box {
  svg {
    color: var(--te-toolbars-lang-text-color);
    width: 18px;
    height: 18px;
    &:hover {
      opacity: 0.75;
    }
  }
}
.icon {
  color: var(--te-toolbars-lang-icon-color);
}
</style>
