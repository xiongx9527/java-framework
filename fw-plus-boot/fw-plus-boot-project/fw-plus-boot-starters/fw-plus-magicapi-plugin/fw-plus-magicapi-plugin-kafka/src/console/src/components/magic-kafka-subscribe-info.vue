<template>
  <div class="magic-kafka-info">
    <form>
      <label>{{ $i('message.enable') }}</label>
      <magic-checkbox v-model:value="info.enabled" />

      <label>{{ $i('kafka.subscribe.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('kafka.subscribe.form.placeholder.key')" width="200px" />

      <label>{{ $i('kafka.subscribe.form.broadcast') }}</label>
      <magic-checkbox v-model:value="info.broadcast" />

      <label>{{ $i('kafka.subscribe.form.topic') }}</label>
      <magic-input v-model:value="info.topic" :placeholder="$i('kafka.subscribe.form.placeholder.topic')" width="200px" />

      <label>{{ $i('kafka.subscribe.form.ackMode') }}</label>
      <magic-select v-model:value="info.ackMode" :placeholder="$i('kafka.subscribe.form.placeholder.ackMode')" :options="ackModeOptions" width="150px" />

      <label>{{ $i('kafka.subscribe.form.concurrency') }}</label>
      <magic-input v-model:value="info.concurrency" width="50px" />

      <label>{{ $i('kafka.subscribe.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('kafka.subscribe.form.placeholder.name')" width="auto" style="flex:1" />
    </form>
  </div>
  <magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false" tooltip-direction="bottom">
    <magic-navbar-item v-for="(navbar, key) in navbars" :key="key" v-bind="navbar">
      <component :is="navbar.component"/>
    </magic-navbar-item>
  </magic-navbar>
</template>

<script setup>
import {inject} from 'vue'
import MagicKafkaSubscribeOther from './magic-kafka-subscribe-other.vue'
import MagicKafkaSubscribeParams from './magic-kafka-subscribe-params.vue'

const $i = inject('i18n.format')
const info = inject('info')

const ackModeOptions = [
  { text: 'RECORD', value: 'RECORD' },
  { text: 'BATCH', value: 'BATCH' },
  { text: 'TIME', value: 'TIME' },
  { text: 'COUNT', value: 'COUNT' },
  { text: 'COUNT_TIME', value: 'COUNT_TIME' },
  { text: 'MANUAL', value: 'MANUAL' },
  { text: 'MANUAL_IMMEDIATE', value: 'MANUAL_IMMEDIATE' }
]

const navbars = [{
  title: $i('kafka.subscribe.navbars.other'),
  component: MagicKafkaSubscribeOther
},{
  title: $i('kafka.subscribe.navbars.params'),
  component: MagicKafkaSubscribeParams
}]
</script>

<style scoped>
.magic-kafka-info {
  display: flex;
  flex-direction: column;
}
.magic-kafka-info form {
  display: flex;
  align-items: center;
  padding: 5px;
}
.magic-kafka-info form label {
  display: inline-block;
  width: auto;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-kafka-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-kafka-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
