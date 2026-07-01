<template>
    <div class="magic-form-row">
        <label>{{$i('message.name')}}</label>
        <magic-input v-model:value="info.name" :placeholder="$i('datasource.form.placeholder.name')"/>
    </div>
    <div class="magic-form-row">
        <label>Key</label>
        <magic-input v-model:value="info.key" :placeholder="$i('datasource.form.placeholder.key')"/>
    </div>
    <div class="magic-form-row">
        <label>URL</label>
        <magic-input v-model:value="info.url" :placeholder="$i('datasource.form.placeholder.url')"/>
    </div>
    <div class="magic-form-row">
        <label>{{$i('message.username')}}</label>
        <magic-input v-model:value="info.username" :placeholder="$i('datasource.form.placeholder.username')"/>
    </div>
    <div class="magic-form-row">
        <label>{{$i('message.password')}}</label>
        <magic-input v-model:value="info.password" type="password" :placeholder="$i('datasource.form.placeholder.password')"/>
    </div>
    <div class="magic-form-row">
        <label>{{$i('datasource.form.driver')}}</label>
        <magic-select inputable v-model:value="info.driverClassName" width="100%" :options="constants.JDBC_DRIVERS.map(it => { return {text: it, value: it} })" :placeholder="$i('datasource.form.placeholder.driver')"/>
    </div>
    <div class="magic-form-row">
        <label>{{$i('datasource.form.type')}}</label>
        <magic-select inputable v-model:value="info.type" width="100%" :options="constants.DATASOURCE_TYPES.map(it => { return {text: it, value: it} })" :placeholder="$i('datasource.form.placeholder.type')"/>
    </div>
    <div class="magic-form-row">
        <label>maxRows</label>
        <magic-input v-model:value="info.maxRows" :placeholder="$i('datasource.form.placeholder.maxRows')" :default-value="-1" type="number"/>
    </div>
    <div class="magic-form-row">
        <label>{{$i('datasource.form.other')}}</label>
        <magic-monaco-editor language="json" v-model:value="properties" style="height:150px"/>
    </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import $i from '../../../scripts/i18n.js'
import constants from '../../../scripts/constants.js'
const { info } = defineProps({
    info: Object
})
const properties = ref(JSON.stringify(info.properties || {}))
watch(properties, (val) => {
    try{
        info.properties = JSON.parse(val)
    }catch(e){
        info.properties = {}
    }
})
</script>