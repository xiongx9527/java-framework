<template>
  <div class="magic-word-result">
    <div v-if="!blobUrl" class="empty-state">
      <p>{{ $i('word.result') }}</p>
    </div>
    <div v-else class="result-content">
      <div class="result-info">
        <magic-button @click="doDownload">{{ $i('word.resultDownload') }}</magic-button>
      </div>
      <iframe :src="blobUrl" class="preview-frame" />
    </div>
  </div>
</template>

<script setup>
import { inject, ref, onBeforeUnmount, watchEffect } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
const blobUrl = ref(null)

watchEffect(() => {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
  }
  blobUrl.value = info.value?.responseBody || null
})

function doDownload() {
  if (blobUrl.value) {
    const a = document.createElement('a')
    a.href = blobUrl.value
    a.download = (info.value?.name || 'report') + '.docx'
    a.click()
  }
}

onBeforeUnmount(() => {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
  }
})
</script>

