<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('kafka.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('kafka.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('kafka.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('kafka.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('kafka.datasource.form.servers') }}</label>
      <magic-input v-model:value="info.servers" :placeholder="$i('kafka.datasource.form.placeholder.servers')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('kafka.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('kafka.datasource.form.placeholder.properties')" language="json" style="height:150px" />
    </div>
  </div>
</template>

<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ properties: {} })
  }
})
</script>
