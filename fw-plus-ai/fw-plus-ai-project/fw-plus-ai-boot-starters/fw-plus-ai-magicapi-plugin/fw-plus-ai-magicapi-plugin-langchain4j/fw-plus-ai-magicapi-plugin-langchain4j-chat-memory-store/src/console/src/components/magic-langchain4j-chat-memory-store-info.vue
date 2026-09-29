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
      <label>{{ $i('langchain4j.common.getFunction') }}</label>
      <magic-input v-model:value="info.getFunction" :placeholder="$i('langchain4j.hint.function')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.updateFunction') }}</label>
      <magic-input v-model:value="info.updateFunction" :placeholder="$i('langchain4j.hint.function')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('langchain4j.common.deleteFunction') }}</label>
      <magic-input v-model:value="info.deleteFunction" :placeholder="$i('langchain4j.hint.function')" width="100%"/>
    </div>
  </div>
</template>
<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')

defineProps({
  info: {
    type: Object,
    default: () => ({})
  }
})

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
