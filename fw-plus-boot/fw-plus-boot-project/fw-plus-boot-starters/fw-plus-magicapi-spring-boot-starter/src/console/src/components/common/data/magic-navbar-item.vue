<template>
    <div class="magic-navbar-item">
        <slot/>
    </div>
</template>
<script setup>
const props = defineProps({
    title: String,
    style: Object,
    show: {
        type: Boolean,
        default: true
    },
    to: HTMLElement
})
</script>
<style scoped>
.magic-navbar-item{
    height: 100%;
    width: 100%;
}
</style>