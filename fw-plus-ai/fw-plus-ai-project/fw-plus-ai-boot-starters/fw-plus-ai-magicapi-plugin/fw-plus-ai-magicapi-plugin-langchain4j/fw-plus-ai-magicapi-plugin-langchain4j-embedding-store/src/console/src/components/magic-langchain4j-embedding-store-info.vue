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
      <label>{{ $i('langchain4j.common.provider') }}</label>
      <magic-select v-model:value="info.provider" inputable :options="providerOptions"
                    :placeholder="$i('langchain4j.hint.required')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.enabled') }}</label>
      <magic-select v-model:value="info.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div v-if="isStore" class="magic-form-row">
      <label>{{ $i('langchain4j.common.embeddingModelKey') }}</label>
      <magic-select v-model:value="info.embeddingModelKey" inputable :options="embeddingModelOptions"
                    :placeholder="$i('langchain4j.hint.embeddingModelKey')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.options') }}</label>
      <span class="magic-langchain4j-form-tip">
        {{ info.provider ? providerLabel(info.provider) : $i('langchain4j.hint.required') }}
      </span>
    </div>
    <template v-if="info.options">
      <div v-for="f in fields" :key="f.name" class="magic-form-row">
        <label>{{ fieldLabel(f) }}<em v-if="f.required">*</em></label>
        <magic-select v-if="f.type === 'switch' || f.type === 'select'" v-model:value="info.options[f.name]"
                      inputable :options="selectOptions(f)" :placeholder="fieldPlaceholder(f)" width="100%"/>
        <magic-input v-else v-model:value="info.options[f.name]" :type="inputType(f)"
                     :placeholder="fieldPlaceholder(f)" width="100%"/>
      </div>
      <div v-if="!fields.length" class="magic-form-row">
        <label></label>
        <span class="magic-langchain4j-form-tip">{{ $i('langchain4j.hint.empty') }}</span>
      </div>
    </template>
  </div>
</template>
<script setup>
import {computed, inject, onMounted} from 'vue'
import {optionsFields, providerNames, isSensitive, SELECT_HINTS} from '../metadata/providers.js'

const isStore = true

const $i = inject('i18n.format')
const injectResources = inject('injectResources', null)

const props = defineProps({
  info: {
    type: Object,
    default: () => ({options: {}})
  }
})

// datasource 表单打开时 options 可能为空，兜底成空对象以便逐字段双向绑定
onMounted(() => {
  if (!props.info.options) {
    props.info.options = {}
  }
})

const fallback = (key, name) => {
  const text = $i(key)
  return text === key ? name : text
}

const providerLabel = (provider) => fallback('langchain4j.provider.' + provider, provider)

const providerOptions = computed(() => providerNames().map(p => ({
  text: providerLabel(p),
  value: p
})))

const enabledOptions = [
  {text: $i('langchain4j.option.yes'), value: true},
  {text: $i('langchain4j.option.no'), value: false}
]

const embeddingModelOptions = computed(() => {
  if (typeof injectResources !== 'function') {
    return []
  }
  try {
    return (injectResources('langchain4j-embedding-model') || [])
      .map(it => ({text: it.name || it.key, value: it.key}))
  } catch (e) {
    return []
  }
})

const fields = computed(() => optionsFields(props.info.provider))

const fieldLabel = (f) => fallback('langchain4j.field.' + f.name, f.name)

const fieldPlaceholder = (f) => {
  if (f.required) {
    return $i('langchain4j.hint.required')
  }
  if (f.type === 'duration') {
    return $i('langchain4j.hint.duration')
  }
  if (f.type === 'tags') {
    return $i('langchain4j.hint.tags')
  }
  if (f.type === 'select') {
    return $i('langchain4j.hint.select')
  }
  return $i('langchain4j.hint.optional')
}

const inputType = (f) => {
  if (isSensitive(f.name)) {
    return 'password'
  }
  return f.type === 'number' ? 'number' : 'text'
}

const selectOptions = (f) => {
  if (f.type === 'switch') {
    return [
      {text: $i('langchain4j.option.true'), value: 'true'},
      {text: $i('langchain4j.option.false'), value: 'false'}
    ]
  }
  return (SELECT_HINTS[f.name] || []).map(v => ({text: v, value: v}))
}
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

.magic-langchain4j-form .magic-form-row label em {
  color: #f56c6c;
  font-style: normal;
  padding-left: 2px;
}

.magic-langchain4j-form-tip {
  color: #909399;
  font-size: 12px;
  padding-left: 5px;
}
</style>
