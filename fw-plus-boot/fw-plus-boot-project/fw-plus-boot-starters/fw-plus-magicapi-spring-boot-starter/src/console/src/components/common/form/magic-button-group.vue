<template>
    <div class="magic-button-group" :class="align">
        <slot />
    </div>
</template>
<script setup>
const props = defineProps({
    align: {
        type: String,
        default: 'left'
    }
})
</script>
<style scoped>
.magic-button-group :deep(.magic-button:not(:last-child)){
    margin-right: 10px;
}
.magic-button-group.right{
    text-align: right;
}
.magic-button-group.left{
    text-align: left;
}
.magic-button-group.center{
    text-align: center;
}
</style>