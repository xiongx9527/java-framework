<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('pulsar.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('pulsar.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.servers') }}</label>
      <magic-input v-model:value="info.servers" :placeholder="$i('pulsar.datasource.form.placeholder.servers')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.serviceUrls') }}</label>
      <magic-input v-model:value="info.serviceUrls" :placeholder="$i('pulsar.datasource.form.placeholder.serviceUrls')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.tenant') }}</label>
      <magic-input v-model:value="info.tenant" :placeholder="$i('pulsar.datasource.form.placeholder.tenant')" :options="extraOptions" inputable width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.namespace') }}</label>
      <magic-input v-model:value="info.namespace" :placeholder="$i('pulsar.datasource.form.placeholder.namespace')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('pulsar.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('pulsar.datasource.form.placeholder.other')" language="json" style="height:150px" />
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

const extraOptions = [
  { text: $i('datasource.extra.producer'), value: 'producer' },
  { text: $i('datasource.extra.consumer'), value: 'consumer' }
]
</script>
