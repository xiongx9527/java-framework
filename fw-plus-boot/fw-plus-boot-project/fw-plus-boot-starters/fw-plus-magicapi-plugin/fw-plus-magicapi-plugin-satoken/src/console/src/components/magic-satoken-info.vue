<template>
  <div class="magic-satoken-info" style="height: 100vh;">
    <form>
      <label>{{ $i('satoken.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('satoken.form.placeholder.name')" width="200px"/>
      <label>{{ $i('satoken.form.bizType') }}</label>
      <magic-input v-model:value="info.bizType" :placeholder="$i('satoken.form.placeholder.bizType')" width="200px"/>
      <label>{{$i('satoken.form.description')}}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('satoken.form.placeholder.description')" width="auto" style="flex:1"/>
    </form>
    <div style="flex:1; padding-top:5px;">
      <magic-textarea v-model:value="info.params" :placeholder="$i('satoken.form.params')" />
    </div>

  </div>
</template>
<script setup>

import { inject } from 'vue'
const $i = inject('i18n.format')
const info = inject('info')

/**
const navbars = [{
  title: $i('freemarker.form.params'),
  component: resolveDynamicComponent('magic-freemarker-params')
},{
  title: $i('freemarker.result'),
  component: resolveDynamicComponent('magic-freemarker-result')
}]


import { resolveDynamicComponent, inject } from 'vue'
const $i = inject('i18n.format')
const info = inject('info')

const params = info.params || {}
console.log('params:{}', params)

const navbars = [{
  title: $i('freemarker.form.params'),
  component: resolveDynamicComponent('magic-textarea'),
  props: {
    modelValue: info.params, // 绑定到 info.params
    'onUpdate:modelValue': (value) => {
      info.params = value
      console.log('new params:{}', params)
    } // 更新 info.params
  }
}]

 **/



</script>
<style scoped>
 .magic-satoken-info{
   display: flex;
   flex-direction: column;
 }
.magic-satoken-info form{
  display: flex;
  padding: 5px;
}
.magic-satoken-info form label{
  display: inline-block;
  width: 75px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-satoken-info + .magic-navbar{
  flex-direction: column;
  overflow: hidden;
}
.magic-satoken-info + .magic-navbar :deep(.magic-navbar-header){
  border-bottom: 1px solid var(--main-border-color);
}
.magic-satoken-info :deep(.magic-navbar-body),
.magic-satoken-info :deep(.magic-navbar-item){
  width: 100%;
  height: 100%;
}
</style>
