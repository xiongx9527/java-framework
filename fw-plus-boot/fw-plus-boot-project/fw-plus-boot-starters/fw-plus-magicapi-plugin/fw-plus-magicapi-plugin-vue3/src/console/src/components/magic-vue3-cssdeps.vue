<template>
    <magic-textarea v-model:value="info.cssDependences" :placeholder="$i('vue.form.placeholder.cssDependences')"/>
</template>
<script setup>
import { inject } from 'vue'
import $i from '../scripts/i18n.js'

const info = inject('info')
</script>
