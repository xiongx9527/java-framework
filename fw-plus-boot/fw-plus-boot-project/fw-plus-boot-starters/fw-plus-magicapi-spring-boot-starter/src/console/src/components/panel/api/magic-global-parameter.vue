<template>
    <div class="magic-panel-global">
        <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.parameters"/>
        <magic-table :data="info.parameters" border @clickRow="index => currentIndex = index">
            <magic-table-column title="Key" #default="{ row }">
                <magic-input v-model:value="row.name" :border="false"/>
            </magic-table-column>
            <magic-table-column title="Value" #default="{ row }">
                <magic-file v-if="row.dataType === 'MultipartFile' " v-model:value="row.value" :border="false"/>
                <magic-file v-else-if="row.dataType === 'MultipartFiles' " v-model:value="row.value" :border="false" multiple/>
                <magic-input v-else v-model:value="row.value" :border="false"/>
            </magic-table-column>
        </magic-table>
    </div>
</template>
<script setup>
import { ref } from 'vue'
import constants from '../../../scripts/constants.js'
const currentIndex = ref(-1)
const info = ref(constants.GLOBAL)
</script>