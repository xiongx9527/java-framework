<script setup>
import { inject } from 'vue'

const props = defineProps({
    info: {
        type: Object,
        default: () => ({ properties: {} })
    }
})

const $i = inject('i18n.format')
const info = props.info
const modelType = () => [
    { text: $i('langchain4j.datasource.extra.modelType.EMBEDDING'), value: 'EMBEDDING' },
    { text: $i('langchain4j.datasource.extra.modelType.CHAT'), value: 'CHAT' },
    { text: $i('langchain4j.datasource.extra.modelType.STREAMING_CHAT'), value: 'STREAMING_CHAT' },
    { text: $i('langchain4j.datasource.extra.modelType.LANGUAGE'), value: 'LANGUAGE' },
    { text: $i('langchain4j.datasource.extra.modelType.STREAMING_LANGUAGE'), value: 'STREAMING_LANGUAGE' },
    { text: $i('langchain4j.datasource.extra.modelType.IMAGE'), value: 'IMAGE' }
]
const provider = () => [
    { text: $i('langchain4j.datasource.extra.provider.OPENAI'), value: 'OPENAI' },
    { text: $i('langchain4j.datasource.extra.provider.QWEN'), value: 'QWEN' },
    { text: $i('langchain4j.datasource.extra.provider.HUGGING_FACE'), value: 'HUGGING_FACE' },
    { text: $i('langchain4j.datasource.extra.provider.OLLAMA'), value: 'OLLAMA' },
    { text: $i('langchain4j.datasource.extra.provider.ZHIPU'), value: 'ZHIPU' },
    { text: $i('langchain4j.datasource.extra.provider.QIANFAN'), value: 'QIANFAN' },
    { text: $i('langchain4j.datasource.extra.provider.LOCAL_AI'), value: 'LOCAL_AI' },
    { text: $i('langchain4j.datasource.extra.provider.GEMINI'), value: 'GEMINI' },
    { text: $i('langchain4j.datasource.extra.provider.AZURE_OPENAI'), value: 'AZURE_OPENAI' },
    { text: $i('langchain4j.datasource.extra.provider.ANTHROPIC'), value: 'ANTHROPIC' },
    { text: $i('langchain4j.datasource.extra.provider.ALL_MINI_LM_L6_V2_QUANTIZED'), value: 'ALL_MINI_LM_L6_V2_QUANTIZED' }
]
</script>

<template>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.name') }}</label>
        <magic-input v-model:value="info.name" :placeholder="$i('langchain4j.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.key') }}</label>
        <magic-input v-model:value="info.key" :placeholder="$i('langchain4j.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.baseUrl') }}</label>
        <magic-input v-model:value="info.baseUrl" :placeholder="$i('langchain4j.datasource.form.placeholder.baseUrl')" width="100%" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.apiKey') }}</label>
        <magic-input v-model:value="info.apiKey" :placeholder="$i('langchain4j.datasource.form.placeholder.apiKey')" width="100%" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.provider') }}</label>
        <magic-select v-model:value="info.provider" :placeholder="$i('langchain4j.datasource.form.placeholder.provider')" :options="provider()" :style="{ width: '20px' }" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.modelType') }}</label>
        <magic-select v-model:value="info.modelType" :placeholder="$i('langchain4j.datasource.form.placeholder.modelType')" :options="modelType()" width="100%" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.modelName') }}</label>
        <magic-input v-model:value="info.modelName" :placeholder="$i('langchain4j.datasource.form.placeholder.modelName')" width="100%" />
    </div>
    <div class="magic-form-row">
        <label>{{ $i('langchain4j.datasource.form.properties') }}</label>
        <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('langchain4j.datasource.form.placeholder.properties')" language="json" :style="{ height: '150px' }" />
    </div>
</template>

<style scoped>
.magic-langchain4j-info {
    display: flex;
    flex-direction: column;
    flex: 1;
    padding: 5px;
}
.magic-langchain4j-info form {
    display: flex;
}
.magic-langchain4j-info form label {
    display: inline-block;
    width: 100px;
    height: var(--magic-input-height);
    line-height: var(--magic-input-height);
    font-weight: 400;
    text-align: right;
    padding: 0 5px;
}
.magic-langchain4j-info form :deep(.magic-checkbox) {
    width: 22px;
    height: 22px;
}
.magic-langchain4j-info form :deep(.magic-textarea) {
    margin: 5px;
}
.magic-langchain4j-info form .magic-form-row.align-left label {
    text-align: left;
    width: 100%;
}
.magic-langchain4j-info form .magic-form-row.align-left .magic-checkbox {
    margin-left: 0 !important;
}
</style>
