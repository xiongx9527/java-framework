<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('sse.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('sse.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('sse.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('sse.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('sse.datasource.form.secret') }}</label>
      <magic-input v-model:value="info.secret" :placeholder="$i('sse.datasource.form.placeholder.secret')" type="password" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('sse.datasource.form.mcp') }}</label>
      <div style="display:flex;align-items:center;height:var(--magic-input-height)"><magic-checkbox v-model:value="info.mcp" /></div>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('sse.datasource.form.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('sse.datasource.form.placeholder.description')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('sse.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('sse.datasource.form.placeholder.other')" language="json" style="height:150px" />
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
