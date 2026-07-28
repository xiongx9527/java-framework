<template>
    <magic-textarea v-model:value="info.params" :placeholder="$i('pulsar.subscribe.form.placeholder.params')"/>
</template>
<script setup>
import { inject } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>
