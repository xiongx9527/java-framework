<template>
    <magic-textarea v-model:value="info.description" :placeholder="$i('vue.form.placeholder.description')"/>
</template>
<script setup>
import { inject } from 'vue'
import $i from '../scripts/i18n.js'

const info = inject('info')
</script>
