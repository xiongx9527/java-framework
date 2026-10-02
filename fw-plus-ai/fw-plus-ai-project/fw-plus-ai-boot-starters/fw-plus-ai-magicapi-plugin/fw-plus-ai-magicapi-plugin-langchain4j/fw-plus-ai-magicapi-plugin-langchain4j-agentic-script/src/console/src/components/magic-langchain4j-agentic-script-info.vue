<template>
  <div class="magic-langchain4j-form">
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('langchain4j.hint.optional')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('langchain4j.hint.required')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.enabled') }}</label>
      <magic-select v-model:value="info.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
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
      <label>{{ $i('langchain4j.script') }}</label>
      <magic-textarea v-model:value="info.script" :placeholder="$i('langchain4j.script-hint')" width="100%"/>
    </div>
  </div>
</template>
<script setup>
import {computed, inject} from 'vue'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({})
  }
})

// datasource 表单打开时字段可能缺省，兜底成完整结构以便逐字段双向绑定
if (!props.info.type) {
  props.info.type = 'node'
}

const info = props.info

const enabledOptions = [
  {text: $i('langchain4j.option.yes'), value: true},
  {text: $i('langchain4j.option.no'), value: false}
]

const typeOptions = [
  {text: $i('langchain4j.script-type.node'), value: 'node'},
  {text: $i('langchain4j.script-type.tool'), value: 'tool'}
]

const splitList = (value) => String(value == null ? '' : value)
  .split(',')
  .map(s => s.trim())
  .filter(s => s.length > 0)

// 列表字段：表单内以逗号分隔文本编辑，落到 info 上为字符串数组
const inputKeysText = computed({
  get: () => (info.inputKeys || []).join(', '),
  set: (value) => { info.inputKeys = splitList(value) }
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
