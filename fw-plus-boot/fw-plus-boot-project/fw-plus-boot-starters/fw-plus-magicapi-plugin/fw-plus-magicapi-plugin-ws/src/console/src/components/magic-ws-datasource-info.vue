<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('ws.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('ws.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('ws.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('ws.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('ws.datasource.form.secret') }}</label>
      <magic-input v-model:value="info.secret" :placeholder="$i('ws.datasource.form.placeholder.secret')" type="password" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('ws.datasource.form.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('ws.datasource.form.placeholder.description')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('ws.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('ws.datasource.form.placeholder.other')" language="json" style="height:150px" />
    </div>
  </div>
</template>

<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ other: '{}' })
  }
})
</script>
