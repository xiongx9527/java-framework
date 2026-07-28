<template>
  <div class="magic-rocketmq-info">
    <form>
      <label>{{ $i('message.enable') }}</label>
      <magic-checkbox v-model:value="info.enabled" />

      <label>{{ $i('rocketmq.subscribe.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('rocketmq.subscribe.form.placeholder.key')" width="200px" />

      <label>{{ $i('rocketmq.subscribe.form.broadcast') }}</label>
      <magic-checkbox v-model:value="info.broadcast" />

      <label>{{ $i('rocketmq.subscribe.form.topic') }}</label>
      <magic-input v-model:value="info.topic" :placeholder="$i('rocketmq.subscribe.form.placeholder.topic')" width="200px" />

      <label>{{ $i('rocketmq.subscribe.form.tags') }}</label>
      <magic-input v-model:value="info.tags" :placeholder="$i('rocketmq.subscribe.form.placeholder.tags')" width="150px" />

      <label>{{ $i('rocketmq.subscribe.form.group') }}</label>
      <magic-input v-model:value="info.group" :placeholder="$i('rocketmq.subscribe.form.placeholder.group')" width="150px" />

      <label>{{ $i('rocketmq.subscribe.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('rocketmq.subscribe.form.placeholder.name')" width="auto" style="flex:1" />
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
import MagicRocketMqSubscribeOther from './MagicRocketMqSubscribeOther.vue'
import MagicRocketMqSubscribeParams from './MagicRocketMqSubscribeParams.vue'

const $i = inject('i18n.format')
const info = inject('info')

const navbars = [{
  title: $i('rocketmq.subscribe.navbars.other'),
  component: MagicRocketMqSubscribeOther
},{
  title: $i('rocketmq.subscribe.navbars.params'),
  component: MagicRocketMqSubscribeParams
}]
</script>

<style scoped>
.magic-rocketmq-info{
  display: flex;
  flex-direction: column;
}
.magic-rocketmq-info form{
  display: flex;
  align-items: center;
  padding: 5px;
}
.magic-rocketmq-info form label{
  display: inline-block;
  width: auto;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-rocketmq-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-rocketmq-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
