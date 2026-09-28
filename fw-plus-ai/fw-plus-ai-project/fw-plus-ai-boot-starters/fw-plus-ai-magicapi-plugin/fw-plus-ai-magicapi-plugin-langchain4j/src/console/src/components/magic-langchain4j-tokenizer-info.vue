<template>
  <magic-langchain4j-form :info="info" short="tokenizer"/>
</template>
<script setup>
import MagicLangchain4jForm from './magic-langchain4j-form.vue'

defineProps({
  info: {
    type: Object,
    default: () => ({ options: {} })
  }
})
</script>
