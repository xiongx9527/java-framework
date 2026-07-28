<template>
  <div style="height:100%;overflow:auto;">
    <iframe v-if="htmlContent" :srcdoc="htmlContent" style="width:100%;height:100%;border:none;" />
    <div v-else style="color:#999;text-align:center;padding:20px;">
      {{ $i('vue.resultEmpty') }}
    </div>
  </div>
</template>
<script setup>
import {inject, ref, watchEffect} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')

const htmlContent = ref(null)

watchEffect(() => {
  htmlContent.value = info.value?.responseBody || null
})
</script>
