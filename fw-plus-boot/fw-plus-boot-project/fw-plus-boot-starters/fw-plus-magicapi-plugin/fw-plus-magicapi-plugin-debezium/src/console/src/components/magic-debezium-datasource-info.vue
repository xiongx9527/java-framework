<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('debezium.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('debezium.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.enabled') }}</label>
      <div style="display:flex;align-items:center;height:var(--magic-input-height)"><magic-checkbox v-model:value="info.enabled" :placeholder="$i('debezium.datasource.form.placeholder.enabled')" /></div>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.connectorClass') }}</label>
      <magic-select v-model:value="info.connectorClass" :placeholder="$i('debezium.datasource.form.placeholder.connectorClass')" :options="connectorOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.hostname') }}</label>
      <magic-input v-model:value="info.hostname" :placeholder="$i('debezium.datasource.form.placeholder.hostname')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.port') }}</label>
      <magic-input v-model:value="info.port" :placeholder="$i('debezium.datasource.form.placeholder.port')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.username') }}</label>
      <magic-input v-model:value="info.username" :placeholder="$i('debezium.datasource.form.placeholder.username')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.password') }}</label>
      <magic-input v-model:value="info.password" type="password" :placeholder="$i('debezium.datasource.form.placeholder.password')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.database') }}</label>
      <magic-input v-model:value="info.database" :placeholder="$i('debezium.datasource.form.placeholder.database')" width="100%" />
    </div>
    <div class="magic-form-row" style="align-items:flex-start">
      <label>{{ $i('debezium.datasource.form.includeTables') }}</label>
      <magic-textarea v-model:value="info.includeTables" :placeholder="$i('debezium.datasource.form.placeholder.includeTables')" style="height:100px;flex:1" />
    </div>
    <div class="magic-form-row" style="align-items:flex-start">
      <label>{{ $i('debezium.datasource.form.excludeTables') }}</label>
      <magic-textarea v-model:value="info.excludeTables" :placeholder="$i('debezium.datasource.form.placeholder.excludeTables')" style="height:100px;flex:1" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.kafka.form.kafkaServers') }}</label>
      <magic-input v-model:value="info.kafkaServers" :placeholder="$i('debezium.kafka.form.placeholder.kafkaServers')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.kafka.form.kafkaTopic') }}</label>
      <magic-input v-model:value="info.kafkaTopic" :placeholder="$i('debezium.kafka.form.placeholder.kafkaTopic')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.serverIdType') }}</label>
      <magic-select v-model:value="info.serverIdType" :placeholder="$i('debezium.datasource.form.placeholder.serverIdType')" :options="serverIdTypeOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('debezium.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('debezium.datasource.form.placeholder.other')" language="json" style="height:150px" />
    </div>
  </div>
</template>

<script setup>
import {inject, onMounted, ref, watch} from 'vue'

const $i = inject('i18n.format')
const service = inject('service', {})['debezium-datasource']

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ other: '{}' })
  }
})

const serverIdTypeOptions = [
  { text: $i('debezium.datasource.form.serverIdTypeOptions.random'), value: 'random' },
  { text: $i('debezium.datasource.form.serverIdTypeOptions.fixed'), value: 'fixed' }
]

const connectorOptions = ref([])

const normalizeConnectorOptions = (options) => {
  if (!Array.isArray(options)) {
    return []
  }
  return options
    .filter((option) => option && option.value)
    .map((option) => ({
      text: option.text || option.value,
      value: option.value
    }))
}

const mergeCurrentConnectorOption = () => {
  const currentValue = props.info && props.info.connectorClass
  if (!currentValue) {
    return
  }
  if (connectorOptions.value.some((option) => option.value === currentValue)) {
    return
  }
  connectorOptions.value = connectorOptions.value.concat({
    text: currentValue,
    value: currentValue
  })
}

const loadConnectorOptions = () => {
  if (!service || !service.loadConnectorOptions) {
    mergeCurrentConnectorOption()
    return
  }
  service.loadConnectorOptions((options) => {
    connectorOptions.value = normalizeConnectorOptions(options)
    mergeCurrentConnectorOption()
  }, () => {
    mergeCurrentConnectorOption()
  })
}

onMounted(() => {
  loadConnectorOptions()
})

watch(() => props.info && props.info.connectorClass, () => {
  mergeCurrentConnectorOption()
})
</script>
