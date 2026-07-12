<template>
  <div class="magic-mcp-info">
    <form>
      <label>{{ $i('mcp.prompt.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mcp.prompt.form.placeholder.name')" width="220px"/>
      <label>{{ $i('mcp.prompt.form.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('mcp.prompt.form.placeholder.description')" width="auto" style="flex:1"/>
    </form>
    <div class="prompt-json-tabs">
      <div class="tab-header">
        <button type="button" :class="{active: activeTab === 'arguments'}" @click="activeTab = 'arguments'">
          {{ $i('mcp.prompt.form.arguments') }}
        </button>
        <button type="button" :class="{active: activeTab === 'messages'}" @click="activeTab = 'messages'">
          {{ $i('mcp.prompt.form.messages') }}
        </button>
      </div>
      <div v-show="activeTab === 'arguments'" class="tab-panel">
        <magic-monaco-editor v-model:value="argumentsText" :placeholder="$i('mcp.prompt.form.placeholder.arguments')" language="json" class="json-editor"/>
      </div>
      <div v-show="activeTab === 'messages'" class="tab-panel">
        <magic-monaco-editor v-model:value="messagesText" :placeholder="$i('mcp.prompt.form.placeholder.messages')" language="json" class="json-editor"/>
      </div>
    </div>
  </div>
</template>
<script setup>
import {inject, ref, watch} from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
const activeTab = ref('arguments')

const stringify = value => JSON.stringify(value || [], null, 2)
const parse = value => {
  try {
    return JSON.parse(value || '[]')
  } catch (e) {
    return null
  }
}

const argumentsText = ref(stringify(info.arguments))
const messagesText = ref(stringify(info.messages))

watch(() => info.arguments, value => {
  const text = stringify(value)
  if (text !== argumentsText.value) {
    argumentsText.value = text
  }
}, {deep: true})
watch(() => info.messages, value => {
  const text = stringify(value)
  if (text !== messagesText.value) {
    messagesText.value = text
  }
}, {deep: true})
watch(argumentsText, value => {
  const parsed = parse(value)
  if (parsed) {
    info.arguments = parsed
  }
})
watch(messagesText, value => {
  const parsed = parse(value)
  if (parsed) {
    info.messages = parsed
  }
})
</script>
<style scoped>
.magic-mcp-info{
  display: flex;
  flex-direction: column;
  height: 100%;
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
.prompt-json-tabs{
  display: flex;
  flex-direction: column;
  padding: 5px;
  flex: 1;
  min-height: 0;
}
.prompt-json-tabs .tab-header{
  display: flex;
  border-bottom: 1px solid var(--magic-border-color, #ddd);
}
.prompt-json-tabs .tab-header button{
  height: var(--magic-input-height);
  padding: 0 12px;
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  cursor: pointer;
}
.prompt-json-tabs .tab-header button.active{
  color: var(--magic-primary-color, #409eff);
  border-bottom-color: var(--magic-primary-color, #409eff);
}
.prompt-json-tabs .tab-panel{
  position: relative;
  flex: 1;
  min-height: 0;
  padding-top: 8px;
}
.prompt-json-tabs .tab-panel[style*="display: none"]{
  flex: none;
}
.prompt-json-tabs :deep(.json-editor.magic-monaco-editor){
  position: absolute;
  left: 0;
  right: 0;
  top: 8px;
  bottom: 0;
}
</style>
