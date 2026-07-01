<template>
    <button @click.stop="$emit('onClick')" class="magic-button" :class="type" type="button">{{ value }}</button>
</template>
<script setup>
const props = defineProps({
    value: String,
    type: String
})
</script>
<style scoped>
.magic-button{
    height: var(--button-height);
    line-height: var(--button-height);
    background-color: var(--button-background-color);
    text-align: center;
    padding: 0 15px;
    border: 1px solid var(--button-border-color);
    outline: 0;
    cursor: pointer;
    color: var(--main-color);
}
.magic-button.link{
    background-color: transparent;
    color: var(--log-color-link);
    border:none;
    padding: 0 5px;
}
.magic-button.active,
.magic-button:hover:not(.link){
    background-color: var(--button-hover-background-color);
    border-color: var(--button-hover-border-color);
    color: var(--button-hover-color);
    border-color: var(--button-border-hover-color)
}
</style>