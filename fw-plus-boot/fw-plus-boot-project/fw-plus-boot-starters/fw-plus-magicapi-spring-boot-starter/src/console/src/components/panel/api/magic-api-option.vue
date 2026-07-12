<template>
    <div class="magic-panel-api">
       <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.options"/>
       <magic-table :data="info.options" border @clickRow="index => currentIndex = index">
            <magic-table-column title="Key" #default="{ row }" width="20%">
                <magic-select :options="options" v-model:value="row.name" :default-select="$DEFAULT_REQUEST_SIMPLE_TYPE" :border="false" inputable @select="v => onSelect(v, row)"/>
            </magic-table-column>
            <magic-table-column title="Value" #default="{ row }" width="60%">
                <magic-input v-model:value="row.value" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.description')" #default="{ row }" width="20%">
                <magic-input v-model:value="row.description" :border="false"/>
            </magic-table-column>
        </magic-table>
    </div>
</template>
<script setup>
import { computed, inject, ref } from 'vue'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const defaultOptions = inject('options') || []
const options = computed(() => (defaultOptions).map(e => { return { text: e[0], value: e[0], description: e[1], defaultValue: e[2]} }))
const currentIndex = ref(-1)
const onSelect = (value, row) => {
    const item = defaultOptions.find(it => it[0] === value)
    if(item){
        row.description = item[1] || ''
        row.value = item[2] || ''
    }
}
</script>