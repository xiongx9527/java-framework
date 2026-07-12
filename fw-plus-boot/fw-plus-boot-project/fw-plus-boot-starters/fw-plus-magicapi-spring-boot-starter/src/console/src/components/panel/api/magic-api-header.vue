<template>
    <div class="magic-panel-api">
       <magic-panel-common-toolbar v-model:index="currentIndex" :value="info.headers"/>
        <magic-table :data="info.headers" border @clickRow="index => currentIndex = index" align="center">
            <magic-table-column :title="$i('message.required')" width="65" #default="{row}">
                <div class="magic-table-cell-center">
                    <magic-checkbox v-model:value="row.required"/>
                </div>
            </magic-table-column>
            <magic-table-column title="Key" #default="{ row }">
                <div class="magic-table-cell-center">
                    <magic-input v-model:value="row.name" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column title="Value" #default="{ row }">
                <div class="magic-table-cell-center">
                    <magic-input v-model:value="row.value" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column :title="$i('message.parameterType')" #default="{ row }" width="135">
                <div class="magic-table-cell-center">
                    <magic-select :options="$REQUEST_SIMPLE_TYPES" v-model:value="row.dataType" :default-select="$DEFAULT_REQUEST_SIMPLE_TYPE" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column :title="$i('message.defaultValue')"  #default="{ row }">
                <div class="magic-table-cell-center">
                    <magic-input v-model:value="row.defaultValue" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column :title="$i('api.validateType')" #default="{ row }" width="115">
                <div class="magic-table-cell-center">
                    <magic-select :options="$VALIDATE_TYPES" v-model:value="row.validateType" :default-select="$DEFAULT_VALIDATE_TYPE" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column :title="$i('api.expression')" #default="{ row }" width="220">
                <div class="magic-table-cell-center">
                    <magic-input v-model:value="row.expression" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column :title="$i('api.validate')" #default="{ row }" width="165">
                <div class="magic-table-cell-center">
                    <magic-input v-model:value="row.error" :border="false"/>
                </div>
            </magic-table-column>
            <magic-table-column :title="$i('message.description')" #default="{ row }" flex="2">
                <div class="magic-table-cell-center">
                    <magic-input v-model:value="row.description" :border="false"/>
                </div>
            </magic-table-column>
        </magic-table>
    </div>
</template>
<style scoped>
.magic-table-cell-center {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
}

.magic-table-cell-center .magic-input,
.magic-table-cell-center .magic-select,
.magic-table-cell-center .magic-select span {
    text-align: center;
    width: 100%;
}

.magic-table-cell-center .magic-select input {
    text-align: center;
    padding-left: 0;
}

.magic-table-cell-center .magic-checkbox {
    margin: 0 auto;
}
</style>
<script setup>
import { inject, ref } from 'vue'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const currentIndex = ref(-1)
</script>