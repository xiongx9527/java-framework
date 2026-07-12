<template>
  <div class="magic-pulsar-info">
    <form>
      <label>{{ $i('message.enable') }}</label>
      <magic-checkbox v-model:value="info.enabled" />

      <label>{{ $i('pulsar.subscribe.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('pulsar.subscribe.form.placeholder.key')" width="200px" />

      <label>{{ $i('pulsar.subscribe.form.broadcast') }}</label>
      <magic-checkbox v-model:value="info.broadcast" />

      <label>{{ $i('pulsar.subscribe.form.topic') }}</label>
      <magic-input v-model:value="info.topic" :placeholder="$i('pulsar.subscribe.form.placeholder.topic')" width="200px" />

      <label>{{ $i('pulsar.subscribe.form.batchIndexAcknowledgmentEnabled') }}</label>
      <magic-checkbox v-model:value="info.batchIndexAcknowledgmentEnabled" />

      <label>{{ $i('pulsar.subscribe.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('pulsar.subscribe.form.placeholder.name')" width="auto" style="flex:1" />
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
import MagicPulsarSubscribeOther from './magic-pulsar-subscribe-other.vue'
import MagicPulsarSubscribeParams from './magic-pulsar-subscribe-params.vue'

const $i = inject('i18n.format')
const info = inject('info')

const navbars = [{
  title: $i('pulsar.subscribe.navbars.other'),
  component: MagicPulsarSubscribeOther
},{
  title: $i('pulsar.subscribe.navbars.params'),
  component: MagicPulsarSubscribeParams
}]
</script>

<style scoped>
.magic-pulsar-info {
  display: flex;
  flex-direction: column;
}
.magic-pulsar-info form {
  display: flex;
  align-items: center;
  padding: 5px;
}
.magic-pulsar-info form label {
  display: inline-block;
  width: auto;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-pulsar-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-pulsar-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
