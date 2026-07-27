<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('model.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('model.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.provider') }}</label>
      <magic-input v-model:value="info.provider" :placeholder="$i('model.datasource.form.placeholder.provider')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.modelType') }}</label>
      <magic-select v-model:value="info.modelType" :options="modelTypeOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.enabled') }}</label>
      <magic-checkbox v-model:value="info.enabled" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.baseUrl') }}</label>
      <magic-input v-model:value="info.baseUrl" :placeholder="$i('model.datasource.form.placeholder.baseUrl')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.apiKey') }}</label>
      <magic-input v-model:value="info.apiKey" type="password" :placeholder="$i('model.datasource.form.placeholder.apiKey')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.modelName') }}</label>
      <magic-input v-model:value="info.modelName" :placeholder="$i('model.datasource.form.placeholder.modelName')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('model.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="propertiesText" :placeholder="$i('model.datasource.form.placeholder.properties')" language="json" style="height:220px" />
    </div>
  </div>
</template>

<script setup>
import {inject, onMounted, ref, watch} from 'vue'

const $i = inject('i18n.format')
const props = defineProps({
  info: {
    type: Object,
    default: () => ({ enabled: true, modelType: 'CHAT', properties: {} })
  }
})

const modelTypeOptions = [
  { text: 'CHAT', value: 'CHAT' },
  { text: 'STREAMING_CHAT', value: 'STREAMING_CHAT' },
  { text: 'EMBEDDING', value: 'EMBEDDING' },
  { text: 'IMAGE', value: 'IMAGE' },
  { text: 'LANGUAGE', value: 'LANGUAGE' },
  { text: 'STREAMING_LANGUAGE', value: 'STREAMING_LANGUAGE' },
  { text: 'MODERATION', value: 'MODERATION' },
  { text: 'SCORING', value: 'SCORING' },
  { text: 'AUDIO', value: 'AUDIO' },
  { text: 'VIDEO', value: 'VIDEO' },
  { text: 'PDF', value: 'PDF' }
]
const propertiesText = ref('{}')

const formatJson = (value) => {
  if (!value) {
    return '{}'
  }
  return typeof value === 'string' ? value : JSON.stringify(value, null, 2)
}

onMounted(() => {
  if (!props.info.propertiesText) {
    props.info.propertiesText = formatJson(props.info.properties)
  }
  if (props.info.enabled === undefined) {
    props.info.enabled = true
  }
  if (!props.info.modelType) {
    props.info.modelType = 'CHAT'
  }
  propertiesText.value = props.info.propertiesText
})

watch(propertiesText, (value) => {
  props.info.propertiesText = value
})
</script>
