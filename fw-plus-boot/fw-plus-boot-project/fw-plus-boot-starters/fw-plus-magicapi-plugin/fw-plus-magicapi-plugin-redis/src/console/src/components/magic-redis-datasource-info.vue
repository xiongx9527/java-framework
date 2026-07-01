<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('redis.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('redis.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.hostname') }}</label>
      <magic-input v-model:value="info.hostname" :placeholder="$i('redis.datasource.form.placeholder.hostname')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('redis.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('redis.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" type="password" :placeholder="$i('redis.datasource.form.placeholder.password')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.database') }}</label>
      <magic-input v-model:value="info.database" :placeholder="$i('redis.datasource.form.placeholder.database')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.mode') }}</label>
      <magic-select v-model:value="info.mode" :placeholder="$i('redis.datasource.form.placeholder.mode')" inputable :options="modeOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.clientType') }}</label>
      <magic-select v-model:value="info.clientType" :placeholder="$i('redis.datasource.form.placeholder.clientType')" inputable :options="clientTypeOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('redis.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('redis.datasource.form.placeholder.other')" language="json" style="height:150px" />
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

const info = props.info

const modeOptions = [
  { text: $i('redis.datasource.extra.mode.standalone'), value: 'standalone' },
  { text: $i('redis.datasource.extra.mode.sentinel'), value: 'sentinel' },
  { text: $i('redis.datasource.extra.mode.cluster'), value: 'cluster' }
]

const clientTypeOptions = [
  { text: $i('redis.datasource.extra.clientType.lettuce'), value: 'lettuce' },
  { text: $i('redis.datasource.extra.clientType.jedis'), value: 'jedis' }
]
</script>
