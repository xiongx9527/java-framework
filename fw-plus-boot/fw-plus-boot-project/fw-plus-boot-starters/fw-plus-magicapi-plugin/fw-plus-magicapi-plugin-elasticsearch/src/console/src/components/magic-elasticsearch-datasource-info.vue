<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('elasticsearch.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('elasticsearch.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.scheme') }}</label>
      <magic-input v-model:value="info.scheme" :placeholder="$i('elasticsearch.datasource.form.placeholder.scheme')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.hostname') }}</label>
      <magic-input v-model:value="info.hostname" :placeholder="$i('elasticsearch.datasource.form.placeholder.hostname')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('elasticsearch.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('elasticsearch.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" type="password" :placeholder="$i('elasticsearch.datasource.form.placeholder.password')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('elasticsearch.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('elasticsearch.datasource.form.placeholder.properties')" language="json" style="height:150px" />
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
