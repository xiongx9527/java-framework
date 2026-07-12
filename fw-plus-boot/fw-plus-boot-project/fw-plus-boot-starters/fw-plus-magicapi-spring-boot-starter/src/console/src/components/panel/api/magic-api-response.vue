<template>
    <div class="magic-panel-api-response">
        <magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false" tooltip-direction="bottom">
            <magic-navbar-item v-for="(navbar, key) in navbars" :key="key" :title="navbar.title">
                <keep-alive>
                    <component :is="navbar.component" :opened="opened"/>
                </keep-alive>
            </magic-navbar-item>
        </magic-navbar>
    </div>
</template>
<script setup>
import { resolveDynamicComponent } from 'vue'
import $i from '../../../scripts/i18n.js'
defineProps({
    opened: Object
})
const navbars = [
    { title: $i('message.responseBody'), component: resolveDynamicComponent('magic-api-response-body')},
    { title: $i('message.responseHeader'), component: resolveDynamicComponent('magic-api-response-header')},
    { title: $i('message.responseStructure'), component: resolveDynamicComponent('magic-api-response-structure')}
]
</script>
<style scoped>
.magic-panel-api-response{
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
}
.magic-panel-api-response :deep(.magic-navbar__horizontal > ul){
    border-top: none;
    border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar{
    flex-direction: column;
    width: 100%;
    height: 100%;
}
.magic-panel-api-response :deep(.magic-navbar .magic-navbar-body),
.magic-panel-api-response :deep(.magic-navbar .magic-navbar-item){
    width: 100%;
    height: 100%;
}
.magic-panel-api-response :deep(.magic-navbar .magic-navbar-item){
    display: flex;
    position: relative;
}
</style>
