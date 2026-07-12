<template>
    <magic-textarea v-model:value="info.description" :placeholder="$i('config.form.placeholder.description')"/>
</template>
<script setup>
import { inject } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>
