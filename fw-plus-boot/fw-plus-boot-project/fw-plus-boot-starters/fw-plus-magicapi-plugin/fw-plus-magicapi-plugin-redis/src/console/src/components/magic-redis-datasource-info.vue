<template>
  <div class="magic-redis-form">
    <div v-for="f in fields" :key="f.name" class="magic-form-row">
      <label>{{ fieldLabel(f) }}<em v-if="f.required">*</em></label>
      <magic-monaco-editor v-if="f.type === 'json'" v-model:value="propertiesText"
                           :placeholder="fieldPlaceholder(f)" language="json" style="height:150px" />
      <magic-select v-else-if="f.type === 'select'" v-model:value="info[f.name]" inputable
                    :options="selectOptions(f)" :placeholder="fieldPlaceholder(f)" width="100%" />
      <magic-input v-else v-model:value="info[f.name]" :type="inputType(f)"
                   :placeholder="fieldPlaceholder(f)" width="100%" />
    </div>
  </div>
</template>

<script setup>
import {computed, inject, onMounted} from 'vue'
import {DATASOURCE_FIELDS, isSensitive, SELECT_HINTS} from '../metadata/redis-datasource.js'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ properties: {} })
  }
})

const info = props.info

// datasource 表单打开时 properties 可能为空，兜底成空对象以便双向绑定
onMounted(() => {
  if (!props.info.properties) {
    props.info.properties = {}
  }
})

const fields = DATASOURCE_FIELDS

const fallback = (key, name) => {
  const text = $i(key)
  return text === key ? name : text
}

const fieldLabel = (f) => fallback('redis.datasource.field.' + f.name, f.name)

const fieldPlaceholder = (f) => {
  if (f.required) {
    return $i('redis.datasource.hint.required')
  }
  if (f.type === 'json') {
    return $i('redis.datasource.hint.json')
  }
  if (f.type === 'select') {
    return $i('redis.datasource.hint.select')
  }
  return $i('redis.datasource.hint.optional')
}

const inputType = (f) => {
  if (isSensitive(f.name)) {
    return 'password'
  }
  return f.type === 'number' ? 'number' : 'text'
}

const selectOptions = (f) => (SELECT_HINTS[f.name] || []).map(v => ({
  text: fallback('redis.datasource.extra.' + f.name + '.' + v, v),
  value: v
}))

// properties：表单内以 JSON 文本编辑，原始文本暂存到 info.propertiesText，
// 保存前由 service.validate 解析并写回 info.properties（非法 JSON 阻止保存）；
// 未编辑时回显已保存的 properties，修复「编辑回显为空」
const propertiesText = computed({
  get: () => {
    if (info.propertiesText !== undefined) {
      return info.propertiesText
    }
    return info.properties == null || Object.keys(info.properties).length === 0
      ? ''
      : JSON.stringify(info.properties, null, 2)
  },
  set: (value) => {
    info.propertiesText = value
  }
})
</script>
<style scoped>
.magic-redis-form {
  display: flex;
  flex-direction: column;
  padding: 5px;
}

.magic-redis-form .magic-form-row {
  display: flex;
  align-items: center;
}

.magic-redis-form .magic-form-row label {
  display: inline-block;
  width: 140px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}

.magic-redis-form .magic-form-row label em {
  color: #f56c6c;
  font-style: normal;
  padding-left: 2px;
}
</style>
