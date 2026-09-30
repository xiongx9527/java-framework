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
      <label>{{ $i('langchain4j.transport.label') }}</label>
      <magic-select v-model:value="info.transport" :options="transportOptions" width="100%"/>
    </div>
    <div class="magic-form-row" v-if="info.transport !== 'stdio'">
      <label>{{ $i('langchain4j.url') }}</label>
      <magic-input v-model:value="info.url" :placeholder="$i('langchain4j.url-hint')" width="100%"/>
    </div>
    <div class="magic-form-row" v-if="info.transport === 'stdio'">
      <label>{{ $i('langchain4j.command') }}</label>
      <magic-input v-model:value="commandText" :placeholder="$i('langchain4j.command-hint')" width="100%"/>
    </div>
    <div class="magic-form-row" v-if="info.transport === 'stdio'">
      <label>{{ $i('langchain4j.environment') }}</label>
      <magic-input v-model:value="environmentText" :placeholder="$i('langchain4j.environment-hint')" width="100%"/>
    </div>
    <div class="magic-form-row" v-if="info.transport !== 'stdio'">
      <label>{{ $i('langchain4j.headers') }}</label>
      <magic-input v-model:value="headersText" :placeholder="$i('langchain4j.headers-hint')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.filter-tool-names') }}</label>
      <magic-input v-model:value="filterToolNamesText" :placeholder="$i('langchain4j.filter-tool-names-hint')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.auth-retry-count') }}</label>
      <magic-input v-model:value="info.authRetryCount" :placeholder="$i('langchain4j.auth-retry-count-hint')" width="100%"/>
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
if (!props.info.transport) {
  props.info.transport = 'http-sse'
}

const info = props.info
const enabledOptions = [
  {text: $i('langchain4j.option.yes'), value: true},
  {text: $i('langchain4j.option.no'), value: false}
]

const transportOptions = [
  {text: $i('langchain4j.transport.http-sse'), value: 'http-sse'},
  {text: $i('langchain4j.transport.streamable-http'), value: 'streamable-http'},
  {text: $i('langchain4j.transport.stdio'), value: 'stdio'}
]

const splitList = (value) => String(value == null ? '' : value)
  .split(',')
  .map(s => s.trim())
  .filter(s => s.length > 0)

const parseJson = (value) => {
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch (e) {
    return {}
  }
}

const joinJson = (value) => value == null ? '' : JSON.stringify(value)

// 列表字段：表单内以逗号分隔文本编辑，落到 info 上为字符串数组
const listField = (key) => computed({
  get: () => (info[key] || []).join(', '),
  set: (value) => { info[key] = splitList(value) }
})

// JSON 对象字段（请求头/环境变量）：表单内以 JSON 文本编辑，落到 info 上为对象
const jsonField = (key) => computed({
  get: () => joinJson(info[key]),
  set: (value) => { info[key] = String(value == null ? '' : value).trim().isEmpty() ? null : parseJson(value) }
})



const commandText = listField('command')
const environmentText = jsonField('environment')
const headersText = jsonField('customHeaders')
const filterToolNamesText = listField('filterToolNames')
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

.magic-langchain4j-form .magic-form-row label {
  display: inline-block;
  width: 140px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}

.magic-langchain4j-form .magic-form-section {
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 600;
  padding-left: 150px;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  margin: 5px 0;
}
</style>
