<template>
    <div class="magic-panel-api">
        <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.paths"/>
        <magic-table :data="info.paths" border @clickRow="index => currentIndex = index" align="center">
            <magic-table-column title="Key" #default="{ row }">
                <magic-input v-model:value="row.name" :border="false"/>
            </magic-table-column>
            <magic-table-column title="Value" #default="{ row }">
                <magic-input v-model:value="row.value" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.parameterType')" #default="{ row }" width="135">
                <magic-select :options="$REQUEST_SIMPLE_TYPES" v-model:value="row.dataType" :default-select="$DEFAULT_REQUEST_SIMPLE_TYPE" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('api.validateType')" #default="{ row }" width="115">
                <magic-select :options="$VALIDATE_TYPES" v-model:value="row.validateType" :default-select="$DEFAULT_VALIDATE_TYPE" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('api.expression')" #default="{ row }" width="220">
                <magic-input v-model:value="row.expression" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('api.validate')" #default="{ row }" width="165">
                <magic-input v-model:value="row.error" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.description')" #default="{ row }" flex="2">
                <magic-input v-model:value="row.description" :border="false"/>
            </magic-table-column>
        </magic-table>
    </div>
</template>
<script setup>
import { inject, ref } from 'vue'
import $i from '../../../scripts/i18n.js'
const currentIndex = ref(-1)
const info = inject('info')
</script>