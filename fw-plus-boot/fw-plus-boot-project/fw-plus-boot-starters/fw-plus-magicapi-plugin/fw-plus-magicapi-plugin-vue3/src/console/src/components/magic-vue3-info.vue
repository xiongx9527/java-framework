<template>
  <div class="magic-vue3-info" style="height: 100vh;">
    <form>
      <label>{{ $i('vue.form.id') }}</label>
      <magic-input v-model:value="info.id" width="200px" readonly />
      <label>{{ $i('vue.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('vue.form.placeholder.name')" width="200px"/>
      <label>{{$i('vue.form.path')}}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('vue.form.placeholder.path')" width="200px"/>
      <label>{{$i('vue.form.description')}}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('vue.form.placeholder.description')" width="auto" style="flex:1" />
    </form>
    <div style="flex:1; padding-top:5px;">
      <magic-textarea v-model:value="info.jsDependences" :placeholder="$i('vue.form.placeholder.jsDependences')" />
    </div>
    <div style="flex:1; padding-top:5px;">
      <magic-textarea v-model:value="info.cssDependences" :placeholder="$i('vue.form.placeholder.cssDependences')" />
    </div>
    <div style="flex:1; padding-top:5px;">
      <magic-textarea v-model:value="info.params" :placeholder="$i('vue.form.placeholder.params')" />
    </div>
  </div>
</template>
<script setup>
import { inject } from 'vue'
const $i = inject('i18n.format')
const info = inject('info')

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
