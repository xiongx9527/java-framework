<template>
    <textarea class="magic-textarea" :placeholder="placeholder" :value="value" @input="(e) => $emit('update:value', e.target.value)"></textarea>
</template>
<script setup>
defineProps({
    value: String,
    placeholder: String
})
</script>
<style scoped>
.magic-textarea {
  outline: 0;
  line-height: 22px;
  border-radius: 0;
  outline: 0;
  border: 1px solid var(--input-border-color);
  resize: none;
  background: var(--input-background-color);
  color: var(--input-color);
  width: 100%;
  height: 100%;
}

.magic-textarea:focus {
  border-color: var(--input-focus-color);
}
</style>