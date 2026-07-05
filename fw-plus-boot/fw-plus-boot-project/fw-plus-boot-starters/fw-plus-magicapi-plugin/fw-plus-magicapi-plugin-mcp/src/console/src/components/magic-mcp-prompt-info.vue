<template>
  <div class="magic-mcp-info">
    <form>
      <label>{{ $i('mcp.prompt.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mcp.prompt.form.placeholder.name')" width="220px"/>
      <label>{{ $i('mcp.prompt.form.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('mcp.prompt.form.placeholder.description')" width="auto" style="flex:1"/>
    </form>
    <form>
      <label>{{ $i('mcp.prompt.form.arguments') }}</label>
      <magic-textarea v-model:value="argumentsText" :placeholder="$i('mcp.prompt.form.placeholder.arguments')" style="flex:1"/>
      <label>{{ $i('mcp.prompt.form.messages') }}</label>
      <magic-textarea v-model:value="messagesText" :placeholder="$i('mcp.prompt.form.placeholder.messages')" style="flex:1"/>
    </form>
  </div>
</template>
<script setup>
import {computed, inject} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')

const stringify = value => JSON.stringify(value || [], null, 2)
const parse = value => {
  try {
    return JSON.parse(value || '[]')
  } catch (e) {
    return []
  }
}

const argumentsText = computed({
  get: () => stringify(info.arguments),
  set: value => info.arguments = parse(value)
})
const messagesText = computed({
  get: () => stringify(info.messages),
  set: value => info.messages = parse(value)
})
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
.magic-mcp-info form + form{
  align-items: stretch;
}
</style>
