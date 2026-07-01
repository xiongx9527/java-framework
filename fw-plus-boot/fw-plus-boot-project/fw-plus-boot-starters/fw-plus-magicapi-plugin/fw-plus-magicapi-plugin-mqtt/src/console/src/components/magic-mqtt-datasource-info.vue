<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mqtt.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('mqtt.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.ip') }}</label>
      <magic-input v-model:value="info.ip" :placeholder="$i('mqtt.datasource.form.placeholder.ip')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('mqtt.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.webPort') }}</label>
      <magic-input v-model:value="info.webPort" :placeholder="$i('mqtt.datasource.form.placeholder.webPort')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('mqtt.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" :placeholder="$i('mqtt.datasource.form.placeholder.password')" type="password" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mqtt.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('mqtt.datasource.form.placeholder.other')" language="json" style="height:150px" />
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
