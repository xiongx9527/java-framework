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

    <div class="magic-form-section">{{ $i('langchain4j.common.functions') }}</div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.enabled') }}</label>
      <magic-select v-model:value="functions.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.includeGroups') }}</label>
      <magic-input v-model:value="fnIncludeGroups" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.excludeGroups') }}</label>
      <magic-input v-model:value="fnExcludeGroups" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.includePaths') }}</label>
      <magic-input v-model:value="fnIncludePaths" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.excludePaths') }}</label>
      <magic-input v-model:value="fnExcludePaths" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>

    <div class="magic-form-section">{{ $i('langchain4j.common.api') }}</div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.enabled') }}</label>
      <magic-select v-model:value="api.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.methods') }}</label>
      <magic-input v-model:value="apiMethods" :placeholder="$i('langchain4j.hint.methods')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.includeGroups') }}</label>
      <magic-input v-model:value="apiIncludeGroups" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.excludeGroups') }}</label>
      <magic-input v-model:value="apiExcludeGroups" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.includePaths') }}</label>
      <magic-input v-model:value="apiIncludePaths" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.excludePaths') }}</label>
      <magic-input v-model:value="apiExcludePaths" :placeholder="$i('langchain4j.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.requireOptIn') }}</label>
      <magic-select v-model:value="api.requireOptIn" :options="enabledOptions" :default-select="true" width="100%"/>
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

// datasource 表单打开时 tools 可能缺省，兜底成完整结构以便逐字段双向绑定
if (!props.info.tools) {
  props.info.tools = {}
}
if (!props.info.tools.functions) {
  props.info.tools.functions = {}
}
if (!props.info.tools.api) {
  props.info.tools.api = {}
}

const functions = props.info.tools.functions
const api = props.info.tools.api

const enabledOptions = [
  {text: $i('langchain4j.option.yes'), value: true},
  {text: $i('langchain4j.option.no'), value: false}
]

const splitList = (value) => String(value == null ? '' : value)
  .split(',')
  .map(s => s.trim())
  .filter(s => s.length > 0)

// 列表字段：表单内以逗号分隔文本编辑，落到 info 上为字符串数组
const listField = (scope, key) => computed({
  get: () => (scope[key] || []).join(', '),
  set: (value) => { scope[key] = splitList(value) }
})

const fnIncludeGroups = listField(functions, 'includeGroups')
const fnExcludeGroups = listField(functions, 'excludeGroups')
const fnIncludePaths = listField(functions, 'includePaths')
const fnExcludePaths = listField(functions, 'excludePaths')
const apiMethods = listField(api, 'methods')
const apiIncludeGroups = listField(api, 'includeGroups')
const apiExcludeGroups = listField(api, 'excludeGroups')
const apiIncludePaths = listField(api, 'includePaths')
const apiExcludePaths = listField(api, 'excludePaths')
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
