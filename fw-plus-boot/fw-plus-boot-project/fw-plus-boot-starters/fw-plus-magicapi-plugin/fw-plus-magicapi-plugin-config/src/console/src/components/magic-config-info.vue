<template>
  <div class="magic-config-info">
    <form>
      <label>{{ $i('config.form.enabled') }}</label>
      <magic-checkbox v-model:value="info.enabled" />
      <label>{{ $i('config.form.env') }}</label>
      <magic-input v-model:value="info.env" :placeholder="$i('config.form.placeholder.env')" width="200px"/>
      <label>{{ $i('config.form.appId') }}</label>
      <magic-input v-model:value="info.appId" :placeholder="$i('config.form.placeholder.appId')" width="200px"/>
      <label>{{ $i('config.form.namespace') }}</label>
      <magic-input v-model:value="info.namespace" :placeholder="$i('config.form.placeholder.namespace')" width="200px"/>
      <label>{{ $i('config.form.externalFlag') }}</label>
      <magic-checkbox v-model:value="info.externalFlag" />
      <label>{{ $i('config.form.commonFlag') }}</label>
      <magic-checkbox v-model:value="info.commonFlag" />
      <label>{{ $i('config.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('config.form.placeholder.name')" width="auto" style="flex:1"/>
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
import MagicConfigDescription from './magic-config-description.vue'

const $i = inject('i18n.format')
const info = inject('info')
const navbars = [{
  title: $i('config.navbars.description'),
  component: MagicConfigDescription
}]
</script>
<style scoped>
.magic-config-info{
  display: flex;
  flex-direction: column;
}
.magic-config-info form{
  display: flex;
  align-items: center;
  padding: 5px;
}
.magic-config-info form label{
  display: inline-block;
  width: auto;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-config-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-config-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
