<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('influx.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('influx.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.url') }}</label>
      <magic-input v-model:value="info.url" :placeholder="$i('influx.datasource.form.placeholder.url')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('influx.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" type="password" :placeholder="$i('influx.datasource.form.placeholder.password')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.token') }}</label>
      <magic-input v-model:value="info.token" :placeholder="$i('influx.datasource.form.placeholder.token')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.org') }}</label>
      <magic-input v-model:value="info.org" :placeholder="$i('influx.datasource.form.placeholder.org')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.bucket') }}</label>
      <magic-input v-model:value="info.bucket" :placeholder="$i('influx.datasource.form.placeholder.bucket')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('influx.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('influx.datasource.form.placeholder.properties')" language="json" style="height:150px" />
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
