<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('dtp.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('dtp.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.corePoolSize') }}</label>
      <magic-input v-model:value="info.corePoolSize" type="number" :placeholder="$i('dtp.datasource.form.placeholder.corePoolSize')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.maximumPoolSize') }}</label>
      <magic-input v-model:value="info.maximumPoolSize" type="number" :placeholder="$i('dtp.datasource.form.placeholder.maximumPoolSize')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.queueType') }}</label>
      <magic-select v-model:value="info.queueType" :placeholder="$i('dtp.datasource.form.placeholder.queueType')" :options="queueTypeOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.queueCapacity') }}</label>
      <magic-input v-model:value="info.queueCapacity" type="number" :placeholder="$i('dtp.datasource.form.placeholder.queueCapacity')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.threadFactory') }}</label>
      <magic-input v-model:value="info.threadFactory" :placeholder="$i('dtp.datasource.form.placeholder.threadFactory')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.rejectedPolicy') }}</label>
      <magic-input v-model:value="info.rejectedPolicy" :placeholder="$i('dtp.datasource.form.placeholder.rejectedPolicy')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('dtp.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('dtp.datasource.form.placeholder.other')" language="json" style="height:150px" />
    </div>
  </div>
</template>

<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')

const queueTypeOptions = [
  { label: 'LinkedBlockingQueue', value: 'linked' },
  { label: 'ArrayBlockingQueue', value: 'array' },
  { label: 'SynchronousQueue', value: 'synchronous' },
  { label: 'PriorityBlockingQueue', value: 'priority' },
  { label: 'DelayQueue', value: 'delayed' },
  { label: 'LinkedTransferQueue', value: 'linkedtransferqueue' },
  { label: 'LinkedBlockingDeque', value: 'linkedblockingdeque' }
]

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ other: '{}' })
  }
})
</script>
