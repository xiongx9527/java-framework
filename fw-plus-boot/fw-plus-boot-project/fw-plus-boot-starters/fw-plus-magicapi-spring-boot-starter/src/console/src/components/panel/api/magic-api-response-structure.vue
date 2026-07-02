<template>
    <magic-table v-if="filteredTreeList.length" :data="filteredTreeList" border>
        <magic-table-column :title="$i('api.field')" #default="{ row }">
            <span :style="{paddingLeft: `${(row.level + (row.folder ? 0 : 1)) * 16}px`}" @click="doExpand(row)">
                <magic-icon v-if="row.folder" :icon="row.expand ? 'arrow-bottom' : 'arrow-right'" />
            </span>
            <span>{{ row.name || '-' }}</span>
        </magic-table-column>
        <magic-table-column :title="$i('message.type')" #default="{ row }" width="80">
            <magic-select :options="$BODY_DATA_TYPES" v-model:value="row.node.dataType" :border="false"/>
        </magic-table-column>
        <magic-table-column title="Value" #default="{ row }" flex="3">
            <template v-if="row.folder"><p align="center" style="flex:1">-</p></template>
            <span v-else class="magic-data-type" :class="row.node.dataType?.toLowerCase()">{{ row.node.value }}</span>
        </magic-table-column>
        <magic-table-column :title="$i('message.description')" #default="{ row }">
            <magic-input v-if="row.name" v-model:value="row.node.description" :border="false"/>
            <template v-else><p align="center" style="flex:1">-</p></template>
        </magic-table-column>
    </magic-table>
    <magic-empty v-else :text="$i('message.empty', $i('message.responseBody'))"/>
</template>
<script setup>
import { computed, inject } from 'vue'
import $i from '../../../scripts/i18n.js'
import { definition2TreeList } from '../../../scripts/utils.js'
const opened = inject('opened')
const doExpand = (item) => {
    const start = opened.value.responseBodyTree.findIndex(it => it === item)
    const startLevel = item.level
    item.expand = !item.expand
    for(let i = start + 1, len = opened.value.responseBodyTree.length; i < len; i++){
        const target = opened.value.responseBodyTree[i]
        if(target.level > startLevel){
            target.display  = item.expand
        }else{
            break;
        }
    }
}
const filteredTreeList = computed(() => {
    if(!opened.value.responseBodyTree && opened.value?.item?.responseBodyDefinition){
        opened.value.responseBodyTree = definition2TreeList(opened.value.item.responseBodyDefinition)
    }
    return (opened.value.responseBodyTree || []).filter(it => it.display)
})
</script>
<style scoped>
.magic-table{
    flex: 1
}
.magic-table :deep(.magic-table-column){
    display: flex;
    justify-content: center;
    text-align: center;
}
.magic-table :deep(.magic-table-column:first-child){
    justify-content: unset !important;
    text-align:unset !important;
}
.magic-data-type{
    display: inline-block;
    width: 0;
    padding:0 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
}
</style>