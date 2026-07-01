<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('rabbitmq.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('rabbitmq.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.host') }}</label>
      <magic-input v-model:value="info.host" :placeholder="$i('rabbitmq.datasource.form.placeholder.host')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('rabbitmq.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('rabbitmq.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" type="password" :placeholder="$i('rabbitmq.datasource.form.placeholder.password')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.virtualHost') }}</label>
      <magic-input v-model:value="info.virtualHost" :placeholder="$i('rabbitmq.datasource.form.placeholder.virtualHost')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('rabbitmq.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('rabbitmq.datasource.form.placeholder.other')" language="json" style="height:150px" />
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
