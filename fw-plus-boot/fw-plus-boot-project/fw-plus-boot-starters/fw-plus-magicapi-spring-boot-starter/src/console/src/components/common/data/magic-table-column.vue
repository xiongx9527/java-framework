<template>
    <div class="magic-table-row">
        <slot :row="row" ></slot>
    </div>
</template>
<script setup>
const props = defineProps({
    title: String,
    row: Object,
    flex: String,
    width: String,
    align: String
})
</script>