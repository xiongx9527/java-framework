<template>
    <magic-textarea v-model:value="info.description"/>
</template>
<script setup>
import { inject } from 'vue'

const info = inject('info')
</script>
