<template>
    <div class="magic-panel-function">
        <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.parameters"/>
        <magic-table :data="info.parameters" border @clickRow="index => currentIndex = index">
            <magic-table-column :title="$i('message.name')" #default="{ row }" width="20%">
                <magic-input v-model:value="row.name" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.parameterType')" #default="{ row }" width="20%">
                <magic-select :options="$FUNCTION_RETURN_TYPES" v-model:value="row.type" :default-select="$DEFAULT_FUNCTION_RETURN_TYPE" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.description')" #default="{ row }" flex="1">
                <magic-input v-model:value="row.description" :border="false"/>
            </magic-table-column>
        </magic-table>
    </div>
</template>
<script setup>
import { inject, ref } from 'vue'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const currentIndex = ref(-1)
</script>