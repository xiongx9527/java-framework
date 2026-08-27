<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mqttclient.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('mqttclient.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.ip') }}</label>
      <magic-input v-model:value="info.ip" :placeholder="$i('mqttclient.datasource.form.placeholder.ip')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('mqttclient.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.clientId') }}</label>
      <magic-input v-model:value="info.clientId" :placeholder="$i('mqttclient.datasource.form.placeholder.clientId')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('mqttclient.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" :placeholder="$i('mqttclient.datasource.form.placeholder.password')" type="password" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqttclient.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('mqttclient.datasource.form.placeholder.properties')" language="json" style="height:150px" />
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
