<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mongo.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('mongo.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.hostname') }}</label>
      <magic-input v-model:value="info.hostname" :placeholder="$i('mongo.datasource.form.placeholder.hostname')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('mongo.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('mongo.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" type="password" :placeholder="$i('mongo.datasource.form.placeholder.password')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.database') }}</label>
      <magic-input v-model:value="info.database" :placeholder="$i('mongo.datasource.form.placeholder.database')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.mode') }}</label>
      <magic-select v-model:value="info.mode" :placeholder="$i('mongo.datasource.form.placeholder.mode')" inputable :options="modeOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mongo.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('mongo.datasource.form.placeholder.other')" language="json" style="height:150px" />
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

const modeOptions = [
  { text: $i('mongo.datasource.extra.mode.standalone'), value: 'standalone' },
  { text: $i('mongo.datasource.extra.mode.replicaSet'), value: 'replicaSet' },
  { text: $i('mongo.datasource.extra.mode.cluster'), value: 'cluster' }
]
</script>
