<template>
    <magic-textarea v-model:value="info.other" :placeholder="$i('rocketmq.subscribe.form.placeholder.other')" />
</template>

<script setup>
import { inject } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
</script>
