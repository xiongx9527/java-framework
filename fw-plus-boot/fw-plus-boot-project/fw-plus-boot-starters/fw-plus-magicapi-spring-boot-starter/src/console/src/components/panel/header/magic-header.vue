<template>
    <div class="magic-header">
        <div class="magic-logo">
            <label :title="title">{{ title }}</label>
            <label :title="version">{{ version }}</label>
        </div>
        <div class="magic-header-title">{{ pageTitle }}</div>
        <ul>
            <li v-for="(button, index) in buttons" :key="index" :data-title="button.name" data-tooltip-direction="left-bottom"
                @click.stop="button.disabled !== true && button.onClick()" 
                :class="{ disabled: button.disabled && button.disabled() === true }">
                <magic-icon :icon="button.icon"></magic-icon>
            </li>
        </ul>
    </div>
    <div v-show="skinVisible" class="magic-skin-selector">
      <ul>
        <li v-for="theme in Object.keys(Themes)" :key="'theme_' + theme" @click="switchTheme(theme)">{{ theme }}</li>
      </ul>
    </div>
     <div v-show="localeVisible" class="magic-locale-selector">
      <ul>
        <li v-for="item in locales" :key="'locale_' + item.id" @click="switchLocale(item)">{{ item.name }}</li>
      </ul>
    </div>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import bus from '../../../scripts/bus.js'
import { Themes } from '../../../scripts/theme.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import * as monaco from 'monaco-editor'
import store from '../../../scripts/store.js'
import $i from '../../../scripts/i18n.js'
import modal from '../../common/dialog/magic-modal.js'
const localeScripts = import.meta.glob('../../../scripts/i18n/*.js')
const locales = reactive([])
for(let k in localeScripts){
    localeScripts[k]().then(v => {
        locales.push({
            id: k.replace(/(.*?i18n\/)(.*)(\.js)/g, '$2'),
            name: v.default.name
        })
    })
}
const props = defineProps({
    title: {
        type: String,
        default: 'magic-api'
    },
    themeStyle: Object,
    header: Object
})
const version = constants.MAGIC_API_VERSION_TEXT
const skinVisible = ref(false)
const localeVisible = ref(false)
const opened = ref({})
const pageTitle = ref('')
bus.$on(Message.OPEN_EMPTY, () => {
    pageTitle.value = ''
    opened.value = {}
})
bus.$on(Message.OPEN, item => {
    opened.value = item
    pageTitle.value = item.path()
    watch(item, () => {
        if(opened.value === item){
            pageTitle.value = item.path()
        }
    })
})
const buttons = computed(() => [
    {name: `${$i('message.run')}(Ctrl + Q)`, icon: 'run', disabled: () => opened.value.runnable !== true || opened.value.running === true, onClick: ()=> bus.$emit(Message.DO_TEST)},
    {name: `${$i('message.save')}(Ctrl + S)`, icon: 'save', onClick: () => bus.$emit(Message.DO_SAVE, true)},
    {name: `${$i('message.search')}(Ctrl + Shift + F)`, icon: 'search', onClick: () => bus.$emit(Message.DO_SEARCH)},
    {name: $i('message.upload'), icon: 'upload', onClick: () => bus.$emit(Message.DO_UPLOAD)},
    {name: $i('message.export'), icon: 'download', onClick: () => bus.$emit(Message.DO_DOWNLOAD)},
    {name: $i('message.push'), icon: 'push', onClick: () => bus.$emit(Message.DO_PUSH)},
    {name: $i('message.skin'), displayKey: 'skin', icon: 'skin', onClick: () => {skinVisible.value = !skinVisible.value; localeVisible.value = false}},
    {name: $i('message.i18n'), icon: 'i18n', onClick: () => {localeVisible.value = !localeVisible.value; skinVisible.value = false}},
    {name: $i('message.reload'), icon: 'refresh', onClick: () => bus.$emit(Message.RELOAD_RESOURCES)}
].filter(it => props.header[it.displayKey] !== false))
const switchTheme = $theme => {
    constants.THEME = $theme
    bus.$emit(Message.SWITCH_THEME, $theme)
    bus.status('message.switchSkin',true, $theme)
    monaco.editor.setTheme($theme)
    Object.keys(props.themeStyle).forEach(key => props.themeStyle[key] = undefined)
    let theme = Themes[$theme]
    store.set(constants.STORE.theme, $theme)
    Object.keys(theme).forEach((key) => props.themeStyle[`--${key}`] = theme[key])
    skinVisible.value = false
}
const storeTheme = store.get(constants.STORE.theme)
if(Themes[storeTheme]){
    switchTheme(storeTheme)
} else {
    switchTheme(constants.THEME)
}
const switchLocale = ({ id, name }) => {
    localeVisible.value = false
    store.set('locale', id)
    modal.confirm($i('message.tips'), $i('message.switchLocale', name), () => {
        location.reload()
    })
}
// switchTheme('dark')
</script>
<style scoped>
.magic-header{
    height: var(--magic-header-height);
    line-height: var(--magic-header-height);;
    background-color: var(--main-background-color);
    border-bottom: 1px solid var(--main-border-color);
    display: flex;
    color: var(--header-default-color);
}
.magic-header .magic-logo{
    float: left;
    font-weight: 700;
    font-size: 0;
    letter-spacing: 0;
    background-repeat: no-repeat;
    background-position: var(--magic-header-logo-background-position);
    background-size: var(--magic-header-logo-background-size);
    padding-left: var(--magic-header-logo-padding);
    color: var(--header-title-color);
}
.magic-header .magic-logo label {
    font-size: 16px;
    display: inline-block;
    margin-right: 5px;
    text-align: left;
    font-weight: 400;
}
.magic-header .magic-logo label:last-child {
    font-size: 12px
}
.magic-header ul{
    float: right;
}
.magic-header ul li{
    cursor: pointer;
    height: var(--magic-header-height);
    text-align: center;
    display: inline-flex;
    align-items: center;
}
.magic-header ul li svg{
    padding: 0 4px;
    height: var(--magic-header-icon-size);
    width: var(--magic-header-icon-size);
    border-radius: 4px;
}
.magic-header ul li.disabled svg{
    fill: var(--button-disabled-color);
}
.magic-header ul li:not(.disabled) svg:hover{
    background-color: var(--main-hover-icon-background-color);
    
}
.magic-header ul li:not(.disabled) :deep(.magic-icon-run),
.magic-header ul li :deep(.magic-icon-push){
    fill: #59a869;
}
.magic-header-title{
    flex:1;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.magic-skin-selector,.magic-locale-selector {
    position: absolute;
    top: var(--magic-header-height);
    right: 24px;
    z-index: 20;
    background-color: var(--main-background-color);
    border: 1px solid var(--main-border-color);
    border-top: none;
}
.magic-skin-selector{
    right: 48px;
}
.magic-skin-selector ul li,
.magic-locale-selector ul li{
    height: 24px;
    line-height: 24px;
    text-align: center;
    cursor: pointer;
    padding: 2px 5px;
}
.magic-skin-selector ul li:not(:last-child),
.magic-locale-selector ul li:not(:last-child) {
    border-bottom: 1px solid var(--main-border-color);
}
.magic-skin-selector ul li:hover,
.magic-locale-selector ul li:hover{
    background-color: var(--select-option-hover-background-color);
    color: var(--select-option-hover-color);
}
</style>