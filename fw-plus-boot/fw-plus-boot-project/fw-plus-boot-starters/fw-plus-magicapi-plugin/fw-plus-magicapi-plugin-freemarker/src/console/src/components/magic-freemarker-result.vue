<template>
  <div style="height:100%;overflow:auto;">
    <iframe v-if="blobUrl" :src="blobUrl" style="width:100%;height:100%;border:none;" />
    <div v-else style="color:#999;text-align:center;padding:20px;">
      {{ $i('freemarker.resultEmpty') }}
    </div>
  </div>
</template>
<script setup>
import {inject, onBeforeUnmount, ref, watchEffect} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')

const blobUrl = ref(null)

function revoke() {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = null
  }
}

watchEffect(() => {
  const html = info.value?.responseBody
  revoke()
  if (html) {
    blobUrl.value = URL.createObjectURL(new Blob([html], { type: 'text/html' }))
  }
})

onBeforeUnmount(() => revoke())
</script>
