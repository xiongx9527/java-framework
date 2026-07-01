<template>
  <div class="magic-rabbitmq-info">
    <form>
      <label>{{ $i('message.enable') }}</label>
      <magic-checkbox v-model:value="info.enabled" />

      <label>{{ $i('rabbitmq.subscribe.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('rabbitmq.subscribe.form.placeholder.key')" width="200px" />

      <label>{{ $i('rabbitmq.subscribe.form.broadcast') }}</label>
      <magic-checkbox v-model:value="info.broadcast" />

      <label>{{ $i('rabbitmq.subscribe.form.queueName') }}</label>
      <magic-input v-model:value="info.queueName" :placeholder="$i('rabbitmq.subscribe.form.placeholder.queueName')" width="200px" />

      <label>{{ $i('rabbitmq.subscribe.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('rabbitmq.subscribe.form.placeholder.name')" width="auto" style="flex:1" />
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
import MagicRabbitmqSubscribeOther from './magic-rabbitmq-subscribe-other.vue'
import MagicRabbitmqSubscribeParams from './magic-rabbitmq-subscribe-params.vue'

const $i = inject('i18n.format')
const info = inject('info')

const navbars = [{
  title: $i('rabbitmq.subscribe.navbars.other'),
  component: MagicRabbitmqSubscribeOther
},{
  title: $i('rabbitmq.subscribe.navbars.params'),
  component: MagicRabbitmqSubscribeParams
}]
</script>

<style scoped>
.magic-rabbitmq-info {
  display: flex;
  flex-direction: column;
}
.magic-rabbitmq-info form {
  display: flex;
  align-items: center;
  padding: 5px;
}
.magic-rabbitmq-info form label {
  display: inline-block;
  width: auto;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-rabbitmq-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-rabbitmq-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
