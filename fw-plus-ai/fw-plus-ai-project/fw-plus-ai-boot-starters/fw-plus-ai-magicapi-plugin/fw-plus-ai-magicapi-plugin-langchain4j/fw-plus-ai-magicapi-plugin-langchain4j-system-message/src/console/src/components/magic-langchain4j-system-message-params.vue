<template>
    <div class="magic-langchain4j-system-message-params">
        <magic-monaco-editor v-model:value="params" language="json"/>
    </div>
</template>
<script setup>
import { computed, inject } from 'vue'

const info = inject('info')
const params = computed({
    get: () => info.value.params || '',
    set: (v) => { info.value.params = v }
})
</script>
<style scoped>
.magic-langchain4j-system-message-params {
    position: relative;
    width: 100%;
    height: 100%;
}
.magic-langchain4j-system-message-params :deep(.magic-monaco-editor) {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    bottom: 0;
}
</style>
