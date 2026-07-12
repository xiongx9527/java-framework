<template>
    <div class="magic-function-info">
        <form>
            <label>{{ $i('fn.returnValue') }}</label>
            <magic-select width="100px" :options="$FUNCTION_RETURN_TYPES" :default-select="$DEFAULT_FUNCTION_RETURN_TYPE" v-model:value="info.method"/>
            <label>{{ $i('fn.form.name') }}</label>
            <magic-input v-model:value="info.name" :placeholder="$i('fn.form.placeholder.name')" width="200px"/>
            <label>{{ $i('fn.form.path') }}</label>
            <magic-input v-model:value="info.path" :placeholder="$i('fn.form.placeholder.path')" width="auto" style="flex:1"/>
        </form>
    </div>
    <magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false">
        <magic-navbar-item v-for="(navbar, key) in navbars" :key="key" :title="navbar.title">
            <component :is="navbar.component" :info="info"/>
        </magic-navbar-item>
    </magic-navbar>
</template>
<script setup>
import { inject, resolveDynamicComponent } from 'vue'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const navbars = [{
    title: $i('fn.parameter'),
    component: resolveDynamicComponent('magic-function-parameter')
},{
    title: $i('fn.description'),
    component: resolveDynamicComponent('magic-api-description')
}]
</script>
<style scoped>
.magic-function-info{
    display: flex;
    flex-direction: column;
}
.magic-function-info form{
    display: flex;
    padding: 5px;
}
.magic-function-info form label{
    display: inline-block;
    height: 22px;
    line-height: 22px;
    font-weight: 400;
    text-align: right;
    padding: 0 5px;
    padding-left: 20px;
}
.magic-navbar{
    flex-direction: column;
    overflow: hidden;
}
.magic-navbar :deep(.magic-navbar-header){
    border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
    width: 100%;
    height: 100%;
}
</style>