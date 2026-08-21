<template>
  <div class="magic-spring-bean-info">
    <form>
      <label>{{ $i('springBean.form.enabled') }}</label>
      <magic-checkbox v-model:value="info.enabled" />
      <label>{{ $i('springBean.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('springBean.form.placeholder.name')" width="220px"/>
      <label>{{ $i('springBean.form.path') }}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('springBean.form.placeholder.path')" width="auto" style="flex:1"/>
    </form>
    <div class="magic-spring-bean-description">
      <label>{{ $i('springBean.form.description') }}</label>
      <magic-textarea v-model:value="info.description" :placeholder="$i('springBean.form.placeholder.description')"/>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>

<style scoped>
.magic-spring-bean-info{
  display: flex;
  flex-direction: column;
  height: 100%;
}
.magic-spring-bean-info form{
  display: flex;
  padding: 5px;
}
.magic-spring-bean-info label{
  display: inline-block;
  width: 75px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-spring-bean-description{
  display: flex;
  flex: 1;
  min-height: 120px;
  padding: 0 5px 5px;
}
.magic-spring-bean-description :deep(.magic-textarea){
  flex: 1;
}
</style>
