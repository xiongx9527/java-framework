<template>
  <div class="magic-mcp-info">
    <form>
      <label>{{ $i('mcp.resource.form.uri') }}</label>
      <magic-input v-model:value="info.uri" :placeholder="$i('mcp.resource.form.placeholder.uri')" width="260px"/>
      <label>{{ $i('mcp.resource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mcp.resource.form.placeholder.name')" width="180px"/>
      <label>{{ $i('mcp.resource.form.mimeType') }}</label>
      <magic-input v-model:value="info.mimeType" :placeholder="$i('mcp.resource.form.placeholder.mimeType')" width="220px"/>
      <label>{{ $i('mcp.resource.form.template') }}</label>
      <magic-checkbox v-model:value="info.template" />
      <label>{{ $i('mcp.resource.form.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('mcp.resource.form.placeholder.description')" width="auto" style="flex:1"/>
    </form>
  </div>
</template>
<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>
<style scoped>
.magic-mcp-info{
  display: flex;
  flex-direction: column;
}
.magic-mcp-info form{
  display: flex;
  align-items: center;
  padding: 5px;
}
.magic-mcp-info form label{
  display: inline-block;
  width: auto;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}
</style>
