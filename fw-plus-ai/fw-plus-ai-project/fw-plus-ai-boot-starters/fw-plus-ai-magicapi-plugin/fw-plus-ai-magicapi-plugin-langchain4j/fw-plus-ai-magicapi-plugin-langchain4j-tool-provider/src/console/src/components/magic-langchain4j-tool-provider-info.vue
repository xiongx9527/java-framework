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
      <magic-group-select v-model:value="functions.includeGroups" type="function" width="100%"
                          :placeholder="$i('langchain4j.hint.groupSelect')" :empty-text="$i('langchain4j.hint.groupEmpty')"/>
    </div>

    <div class="magic-form-section">{{ $i('langchain4j.common.api') }}</div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.enabled') }}</label>
      <magic-select v-model:value="api.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.includeGroups') }}</label>
      <magic-group-select v-model:value="api.includeGroups" type="api" width="100%"
                          :placeholder="$i('langchain4j.hint.groupSelect')" :empty-text="$i('langchain4j.hint.groupEmpty')"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.scope.requireOptIn') }}</label>
      <magic-select v-model:value="api.requireOptIn" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
  </div>
</template>
<script setup>
import {inject} from 'vue'
import MagicGroupSelect from './magic-group-select.vue'

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
