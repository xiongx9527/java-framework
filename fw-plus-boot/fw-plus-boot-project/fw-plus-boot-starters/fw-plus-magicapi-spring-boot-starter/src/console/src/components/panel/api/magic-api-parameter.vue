<template>
    <div class="magic-panel-api">
        <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.parameters"/>
        <magic-table :data="info.parameters" border @clickRow="index => currentIndex = index" align="center">
            <magic-table-column :title="$i('message.required')" width="65" #default="{row}">
                <magic-checkbox v-model:value="row.required"/>
            </magic-table-column>
            <magic-table-column title="Key" #default="{ row }">
                <magic-input v-model:value="row.name" :border="false"/>
            </magic-table-column>
            <magic-table-column title="Value" #default="{ row }">
                <magic-file v-if="row.dataType === 'MultipartFile' " v-model:value="row.value" :border="false"/>
                <magic-file v-else-if="row.dataType === 'MultipartFiles' " v-model:value="row.value" :border="false" multiple/>
                <magic-input v-else v-model:value="row.value" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.parameterType')" #default="{ row }" width="135">
                <magic-select :options="$REQUEST_PARAMETER_TYPES" v-model:value="row.dataType" :default-select="$DEFAULT_REQUEST_PARAMETER_TYPE" :border="false"/>
            </magic-table-column>
            <magic-table-column :title="$i('message.defaultValue')"  #default="{ row }" >
                <magic-input v-model:value="row.defaultValue" :border="false"/>
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