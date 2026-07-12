<template>
  <div class="magic-mqttclient-info">
    <form>
      <label>{{ $i('message.enable') }}</label>
      <magic-checkbox v-model:value="info.enabled" />

      <label>{{ $i('mqttclient.subscribe.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('mqttclient.subscribe.form.placeholder.key')" width="300px" />

      <label>{{ $i('mqttclient.subscribe.form.topic') }}</label>
      <magic-input v-model:value="info.topic" :placeholder="$i('mqttclient.subscribe.form.placeholder.topic')" width="300px" />

      <label>{{ $i('mqttclient.subscribe.form.qos') }}</label>
      <magic-select v-model:value="info.qos" :placeholder="$i('mqttclient.subscribe.form.placeholder.qos')" :options="qosOptions" width="150px" />

      <label>{{ $i('mqttclient.subscribe.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mqttclient.subscribe.form.placeholder.name')" width="auto" style="flex:1" />
    </form>
    <div style="flex:1;padding-top:5px">
      <magic-textarea v-model:value="info.description" :placeholder="$i('mqttclient.subscribe.form.placeholder.description')" />
    </div>
    <div style="flex:1">
      <magic-textarea v-model:value="info.params" :placeholder="$i('mqttclient.subscribe.form.placeholder.params')" />
    </div>
    <div style="flex:1">
      <magic-textarea v-model:value="info.other" :placeholder="$i('mqttclient.subscribe.form.placeholder.other')" />
    </div>
  </div>
</template>

<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')

const qosOptions = [
  { text: 'QoS 0', value: '0' },
  { text: 'QoS 1', value: '1' },
  { text: 'QoS 2', value: '2' }
]
</script>

<style scoped>
.magic-mqttclient-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 5px;
}
.magic-mqttclient-info form {
  display: flex;
}
.magic-mqttclient-info form label {
  display: inline-block;
  width: 100px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-mqttclient-info form .magic-checkbox {
  width: 22px;
  height: 22px;
}
.magic-mqttclient-info form .magic-textarea {
  margin: 5px;
}
</style>
