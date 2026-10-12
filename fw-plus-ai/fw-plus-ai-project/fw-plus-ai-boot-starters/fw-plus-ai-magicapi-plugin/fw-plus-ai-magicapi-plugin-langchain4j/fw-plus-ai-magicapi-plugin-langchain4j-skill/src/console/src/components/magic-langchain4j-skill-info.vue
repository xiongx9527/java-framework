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
      <label>{{ $i('langchain4j.description') }}</label>
      <magic-input v-model:value="info.description" :placeholder="$i('langchain4j.description-hint')" width="100%"/>
    </div>
    <div class="magic-form-row magic-form-row-top">
      <label>{{ $i('langchain4j.content') }}</label>
      <magic-textarea v-model:value="info.content" :placeholder="$i('langchain4j.content-hint')" width="100%"/>
    </div>
    <div class="magic-form-row magic-form-row-top">
      <label>{{ $i('langchain4j.resources') }}</label>
      <magic-textarea v-model:value="resourcesText" :placeholder="$i('langchain4j.resources-hint')" width="100%"/>
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

const info = props.info

const enabledOptions = [
  {text: $i('langchain4j.option.yes'), value: true},
  {text: $i('langchain4j.option.no'), value: false}
]

const parseResources = (value) => {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : null
  } catch (e) {
    return null
  }
}

// resources：表单内以 JSON 数组文本编辑，落到 info 上为 [{path, content}] 数组
const resourcesText = computed({
  get: () => (info.resources == null ? '' : JSON.stringify(info.resources, null, 2)),
  set: (value) => {
    const text = String(value == null ? '' : value).trim()
    info.resources = text.length === 0 ? null : parseResources(text)
  }
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
