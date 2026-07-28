<template>
    <magic-textarea v-model:value="info.jsDependences" :placeholder="$i('vue.form.placeholder.jsDependences')"/>
</template>
<script setup>
import { inject } from 'vue'
import $i from '../scripts/i18n.js'

const info = inject('info')
</script>
