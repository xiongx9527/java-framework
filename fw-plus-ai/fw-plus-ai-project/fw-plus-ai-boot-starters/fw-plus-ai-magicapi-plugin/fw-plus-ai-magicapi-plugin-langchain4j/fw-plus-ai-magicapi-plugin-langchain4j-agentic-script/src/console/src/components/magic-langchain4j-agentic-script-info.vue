<template>
  <div class="magic-langchain4j-form">
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.script-type.label') }}</label>
      <magic-select v-model:value="info.type" :options="typeOptions" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('langchain4j.description-hint')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.input-keys') }}</label>
      <magic-input v-model:value="inputKeysText" :placeholder="$i('langchain4j.input-keys-hint')" width="100%"/>
    </div>
    <div class="magic-form-row" v-if="info.type === 'node'">
      <label>{{ $i('langchain4j.output-key') }}</label>
      <magic-input v-model:value="info.outputKey" :placeholder="$i('langchain4j.output-key-hint')" width="100%"/>
    </div>
    <div class="magic-form-row magic-form-row-top">
      <label>{{ $i('langchain4j.params') }}</label>
      <magic-textarea v-model:value="paramsText" :placeholder="$i('langchain4j.params-hint')" width="100%"/>
    </div>
  </div>
</template>
<script setup>
import { computed, inject } from 'vue'

const $i = inject('i18n.format')

// resource 形态：info 由 magic 控制台注入（当前打开的 .ms 资源），脚本正文走原生编辑器
const info = inject('info')

const typeOptions = [
  {text: $i('langchain4j.script-type.node'), value: 'node'},
  {text: $i('langchain4j.script-type.tool'), value: 'tool'},
  {text: $i('langchain4j.script-type.graph'), value: 'graph'}
]

if (!info.type) {
  info.type = 'node'
}

const splitList = (value) => String(value == null ? '' : value)
  .split(',')
  .map(s => s.trim())
  .filter(s => s.length > 0)

const inputKeysText = computed({
  get: () => (info.inputKeys || []).join(', '),
  set: (value) => { info.inputKeys = splitList(value) }
})

const parseJson = (value) => {
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch (e) {
    return {}
  }
}

const joinJson = (value) => value == null ? '' : JSON.stringify(value)

// 测试参数：表单内以 JSON 文本编辑，落到 info 上为对象
const paramsText = computed({
  get: () => joinJson(info.params),
  set: (value) => { info.params = String(value == null ? '' : value).trim().length === 0 ? null : parseJson(value) }
})
</script>
<style scoped>
.magic-langchain4j-form {
  display: flex;
  flex-direction: column;
  padding: 5px;
}

.magic-langchain4j-form .magic-form-row {
  display: flex;
  align-items: center;
}

.magic-langchain4j-form .magic-form-row-top {
  align-items: flex-start;
}

.magic-langchain4j-form .magic-form-row label {
  display: inline-block;
  width: 180px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}

.magic-langchain4j-form .magic-form-row-top label {
  line-height: 2em;
}
</style>
