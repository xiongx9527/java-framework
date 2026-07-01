<template>
    <div class="magic-panel-global">
       <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.headers"/>
        <magic-table :data="info.headers" border @clickRow="index => currentIndex = index">
            <magic-table-column title="Key" #default="{ row }">
                <magic-input v-model:value="row.name" :border="false"/>
            </magic-table-column>
            <magic-table-column title="Value" #default="{ row }">
                <magic-input v-model:value="row.value" :border="false"/>
            </magic-table-column>
        </magic-table>
    </div>
</template>
<script setup>
import { ref } from 'vue'
import constants from '../../../scripts/constants.js'
const info = ref(constants.GLOBAL)
const currentIndex = ref(-1)
</script>