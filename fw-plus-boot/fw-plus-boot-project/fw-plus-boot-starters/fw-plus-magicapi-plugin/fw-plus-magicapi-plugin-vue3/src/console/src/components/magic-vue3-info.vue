<template>
  <div class="magic-vue3-info">
    <form>
      <label>{{ $i('vue.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('vue.form.placeholder.name')" width="200px"/>
      <label>{{$i('vue.form.path')}}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('vue.form.placeholder.path')" width="auto" style="flex:1" />
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
import $i from '../scripts/i18n.js'
import MagicVue3Jsdeps from './magic-vue3-jsdeps.vue'
import MagicVue3Cssdeps from './magic-vue3-cssdeps.vue'
import MagicVue3Params from './magic-vue3-params.vue'
import MagicVue3Description from './magic-vue3-description.vue'

const info = inject('info')
const navbars = [{
  title: $i('vue.navbars.jsDependences'),
  component: MagicVue3Jsdeps
},{
  title: $i('vue.navbars.cssDependences'),
  component: MagicVue3Cssdeps
},{
  title: $i('vue.navbars.params'),
  component: MagicVue3Params
},{
  title: $i('vue.form.description'),
  component: MagicVue3Description
}]
</script>
<style scoped>
.magic-vue3-info{
  display: flex;
  flex-direction: column;
}
.magic-vue3-info form{
  display: flex;
  padding: 5px;
}
.magic-vue3-info form label{
  display: inline-block;
  width: 75px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-vue3-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-vue3-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
