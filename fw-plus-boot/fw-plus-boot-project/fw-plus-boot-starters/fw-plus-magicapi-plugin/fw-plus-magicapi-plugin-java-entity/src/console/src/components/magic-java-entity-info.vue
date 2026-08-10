<template>
  <div class="magic-java-entity-info">
    <form>
      <label>{{ $i('javaEntity.form.enabled') }}</label>
      <magic-checkbox v-model:value="info.enabled" />
      <label>{{ $i('javaEntity.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('javaEntity.form.placeholder.name')" width="220px"/>
      <label>{{ $i('javaEntity.form.path') }}</label>
      <magic-input v-model:value="info.path" :placeholder="$i('javaEntity.form.placeholder.path')" width="auto" style="flex:1"/>
    </form>
    <div class="magic-java-entity-description">
      <label>{{ $i('javaEntity.form.description') }}</label>
      <magic-textarea v-model:value="info.description" :placeholder="$i('javaEntity.form.placeholder.description')"/>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>

<style scoped>
.magic-java-entity-info{
  display: flex;
  flex-direction: column;
  height: 100%;
}
.magic-java-entity-info form{
  display: flex;
  padding: 5px;
}
.magic-java-entity-info label{
  display: inline-block;
  width: 75px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
.magic-java-entity-description{
  display: flex;
  flex: 1;
  min-height: 120px;
  padding: 0 5px 5px;
}
.magic-java-entity-description :deep(.magic-textarea){
  flex: 1;
}
</style>
