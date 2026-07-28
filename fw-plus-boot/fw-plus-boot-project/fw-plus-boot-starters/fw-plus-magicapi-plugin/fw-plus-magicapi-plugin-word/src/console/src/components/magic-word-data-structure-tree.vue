<template>
    <magic-table :data="filteredTreeList" border>
        <magic-table-column :title="$i('api.field')" #default="{ row }">
            <span :style="{paddingLeft: `${(row.level + (row.folder ? 0 : 1)) * 16}px`}" @click="doExpand(row)">
                <magic-icon v-if="row.folder" :icon="row.expand ? 'arrow-bottom' : 'arrow-right'" />
            </span>
            <span :title="row.name || '-'"> {{ row.name || '-' }}</span>
        </magic-table-column>
        <magic-table-column :title="$i('message.type')" #default="{ row }" width="120">
          <magic-select :options="typeOptions" v-model:value="row.node.dataType" :border="false"/>
        </magic-table-column>
        <magic-table-column title="Value" #default="{ row }" width="120">
            <template v-if="row.folder"><span class="magic-dash">-</span></template>
            <span v-else class="magic-data-type" :class="(row.node.dataType||'').toLowerCase()">{{ row.node.value }}</span>
        </magic-table-column>
        <magic-table-column :title="$i('message.description')" #default="{ row }" flex="2">
          <magic-input v-if="row.name" v-model:value="row.node.description" :border="false"/>
          <template v-else><span class="magic-dash">-</span></template>
        </magic-table-column>
    </magic-table>
</template>
<script setup>
import { computed, inject, reactive } from 'vue'

const $i = inject('i18n.format')
const info = inject('info')
const opened = inject('opened')

const typeOptions = [
    {value:'String',text:'String'},{value:'Integer',text:'Integer'},
    {value:'Double',text:'Double'},{value:'Long',text:'Long'},
    {value:'Short',text:'Short'},{value:'Float',text:'Float'},
    {value:'Byte',text:'Byte'},{value:'Boolean',text:'Boolean'},
    {value:'Date',text:'Date'},{value:'Object',text:'Object'},
    {value:'Array',text:'Array'},{value:'Any',text:'Any'}
]
const state = reactive({ treeList: [] })

const buildTreeList = (def, level) => {
    if (!def || !Array.isArray(def)) return
    def.forEach(node => {
        const folder = !!(node.children && node.children.length > 0)
        state.treeList.push({ node, level, folder, expand: true, display: true, name: node.name })
        if (folder) buildTreeList(node.children, level + 1)
    })
}

const filteredTreeList = computed(() => {
    if (!opened.value.dataStructureTree) {
        const def = info.value?.dataStructureTree
        state.treeList = []
        if (def && Array.isArray(def)) buildTreeList(def, 0)
        opened.value.dataStructureTree = state.treeList
    }
    return (opened.value.dataStructureTree || []).filter(it => it.display)
})

const doExpand = (item) => {
    if (!item.folder) return
    const tree = opened.value.dataStructureTree
    const idx = tree.findIndex(it => it === item)
    if (idx < 0) return
    item.expand = !item.expand
    const startLevel = item.level
    for (let i = idx + 1; i < tree.length; i++) {
        const t = tree[i]
        if (t.level > startLevel) t.display = item.expand
        else break
    }
}
</script>
<style scoped>
.magic-table {
    flex: 1
}
.magic-table :deep(.magic-table-column) {
    display: flex;
    justify-content: center;
    text-align: center;
}
.magic-table :deep(.magic-table-column:first-child) {
    justify-content: unset !important;
    text-align: unset !important;
}
.magic-table :deep(.magic-table-column:first-child) span {
    display: inline-flex;
    align-items: center;
}
.magic-table :deep(.magic-icon) {
    vertical-align: middle;
}
.magic-dash {
    display: block;
    width: 100%;
    text-align: center;
}
.magic-data-type {
    display: inline-block;
    width: 0;
    padding: 0 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    text-align: center;
}
</style>
