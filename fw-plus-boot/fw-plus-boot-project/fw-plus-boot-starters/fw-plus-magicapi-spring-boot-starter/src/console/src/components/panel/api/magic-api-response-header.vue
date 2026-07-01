<template>
    <magic-table :data="data" border>
        <magic-table-column title="Key" #default="{ row }">
            <span :title="row.key">{{ row.key }}</span>
        </magic-table-column>
        <magic-table-column title="Value" #default="{ row }">
            <span :title="row.value">{{ row.value }}</span>
        </magic-table-column>
    </magic-table>
</template>
<script setup>
import { computed, inject } from 'vue'
const opened = inject('opened')
const data = computed(() => Object.keys(opened.value.responseHeaders || {}).map(key => { return { key, value: opened.value.responseHeaders[key]} }))
</script>
<style scoped>
.magic-table{
    flex: 1
}
.magic-table span{
    padding: 0 5px
}
</style>