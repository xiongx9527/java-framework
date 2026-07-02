<template>
    <magic-table :data="filteredTreeList" border>
        <magic-table-column :title="$i('api.field')" #default="{ row }">
            <span :style="{paddingLeft: `${(row.level + (row.folder ? 0 : 1)) * 16}px`}" @click="doExpand(row)">
                <magic-icon v-if="row.folder" :icon="row.expand ? 'arrow-bottom' : 'arrow-right'" />
            </span>
            <span :title="row.name || '-'">{{ row.name || '-' }}</span>
        </magic-table-column>
        <magic-table-column :title="$i('message.required')" #default="{ row }" width="65">
            <magic-checkbox v-model:value="row.node.required"/>
        </magic-table-column>
        <magic-table-column :title="$i('message.type')" #default="{ row }" width="80">
            <magic-select :options="$BODY_DATA_TYPES" v-model:value="row.node.dataType" :border="false"/>
        </magic-table-column>
        <magic-table-column title="Value" #default="{ row }" flex="2">
            <template v-if="row.folder"><p align="center" style="flex:1">-</p></template>
            <span v-else class="magic-data-type" :class="row.node.dataType?.toLowerCase()">{{ row.node.value }}</span>
        </magic-table-column>
        <magic-table-column :title="$i('message.defaultValue')" #default="{ row }">
            <magic-input v-model:value="row.node.defaultValue" :border="false"/>
        </magic-table-column>
        <magic-table-column :title="$i('api.validateType')" #default="{ row }" width="115">
            <magic-select :options="$VALIDATE_TYPES" v-model:value="row.node.validateType" :default-select="$DEFAULT_VALIDATE_TYPE" :border="false"/>
        </magic-table-column>
        <magic-table-column :title="$i('api.expression')" #default="{ row }">
            <magic-input v-model:value="row.node.expression" :border="false"/>
        </magic-table-column>
        <magic-table-column :title="$i('api.validate')" #default="{ row }">
            <magic-input v-model:value="row.node.error" :border="false"/>
        </magic-table-column>
        <magic-table-column :title="$i('message.description')" #default="{ row }">
            <magic-input v-if="row.name" v-model:value="row.node.description" :border="false"/>
            <template v-else><p align="center" style="flex:1">-</p></template>
        </magic-table-column>
    </magic-table>
</template>
<script setup>
import { computed, inject} from 'vue'
import $i from '../../../scripts/i18n.js'
import { definition2TreeList } from '../../../scripts/utils.js'
const opened = inject('opened')
const filteredTreeList = computed(() => {
    if(!opened.value.requestBodyTree && opened.value?.item?.requestBodyDefinition){
        opened.value.requestBodyTree = definition2TreeList(opened.value.item.requestBodyDefinition)
    }
    return (opened.value.requestBodyTree || []).filter(it => it.display)
})
const doExpand = (item) => {
    const start = opened.value.requestBodyTree.findIndex(it => it === item)
    const startLevel = item.level
    item.expand = !item.expand
    for(let i = start + 1, len = opened.value.requestBodyTree.length; i < len; i++){
        const target = opened.value.requestBodyTree[i]
        if(target.level > startLevel){
            target.display  = item.expand
        }else{
            break;
        }
    }
}
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
    text-align: center;
}
</style>