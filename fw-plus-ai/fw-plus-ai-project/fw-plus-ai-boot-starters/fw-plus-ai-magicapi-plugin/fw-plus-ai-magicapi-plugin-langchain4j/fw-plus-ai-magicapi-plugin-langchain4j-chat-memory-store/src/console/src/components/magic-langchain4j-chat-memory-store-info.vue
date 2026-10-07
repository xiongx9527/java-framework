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
      <label>{{ $i('langchain4j.common.type') }}</label>
      <magic-select v-model:value="info.type" :options="typeOptions" :default-select="true" width="100%"/>
    </div>
    <div v-if="info.type !== 'in-memory'" class="magic-form-row">
      <label>{{ $i('langchain4j.common.datasourceKey') }}</label>
      <magic-input v-model:value="info.datasourceKey" :placeholder="$i('langchain4j.hint.datasourceKey')" width="100%"/>
    </div>
    <div v-if="info.type === 'jdbc'" class="magic-form-row">
      <label>{{ $i('langchain4j.params.tableName') }}</label>
      <magic-input v-model:value="info.params.tableName" :placeholder="$i('langchain4j.hint.params')" width="100%"/>
    </div>
    <template v-if="info.type === 'redis'">
      <div class="magic-form-row">
        <label>{{ $i('langchain4j.params.keyPrefix') }}</label>
        <magic-input v-model:value="info.params.keyPrefix" :placeholder="$i('langchain4j.hint.params')" width="100%"/>
      </div>
      <div class="magic-form-row">
        <label>{{ $i('langchain4j.params.ttl') }}</label>
        <magic-input v-model:value="info.params.ttl" :placeholder="$i('langchain4j.hint.ttl')" width="100%"/>
      </div>
    </template>
    <div v-if="info.type === 'mongodb'" class="magic-form-row">
      <label>{{ $i('langchain4j.params.collectionName') }}</label>
      <magic-input v-model:value="info.params.collectionName" :placeholder="$i('langchain4j.hint.params')" width="100%"/>
    </div>
    <div v-if="info.type === 'elasticsearch'" class="magic-form-row">
      <label>{{ $i('langchain4j.params.indexName') }}</label>
      <magic-input v-model:value="info.params.indexName" :placeholder="$i('langchain4j.hint.params')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.enabled') }}</label>
      <magic-select v-model:value="info.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
  </div>
</template>
<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({})
  }
})

// 默认提供商：内存；参数对象始终存在，便于直接双向绑定
if (!props.info.type) {
  props.info.type = 'in-memory'
}
if (!props.info.params) {
  props.info.params = {}
}

const typeOptions = [
  {text: $i('langchain4j.provider.inMemory'), value: 'in-memory'},
  {text: $i('langchain4j.provider.jdbc'), value: 'jdbc'},
  {text: $i('langchain4j.provider.redis'), value: 'redis'},
  {text: $i('langchain4j.provider.mongodb'), value: 'mongodb'},
  {text: $i('langchain4j.provider.elasticsearch'), value: 'elasticsearch'}
]

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
</style>
