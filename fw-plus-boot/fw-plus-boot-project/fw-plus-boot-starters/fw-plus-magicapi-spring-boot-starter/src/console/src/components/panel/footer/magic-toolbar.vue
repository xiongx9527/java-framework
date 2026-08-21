<template>
    <div class="magic-toolbar">
        <magic-navbar direction="horizontal" ref="navbar" tooltip-direction="bottom">
            <magic-navbar-item v-for="(toolbar, key) in toolbars" :key="key" v-bind="toolbar">
                <magic-resizer direction="y" :max="700" :min="150"  :value="250">
                    <div class="magic-toolbar-header">
                        <label>{{ toolbar.title }}</label>
                        <div class="magic-toolbar-header-buttons">
                            <magic-icon v-if="toolbar.fullscreen" :icon="isFullscreen ? 'fullscreen-exit' : 'fullscreen'" size="14px" :title="isFullscreen ? $i('message.exitFullscreen') : $i('message.fullscreen')" @click="toggleFullscreen"/>
                            <magic-icon v-if="!isFullscreen" icon="minimize" size="14px" :title="$i('message.hide')" @click="navbar.select(-1)"/>
                            <magic-icon icon="close" :title="$i('editor.tab.close')" v-if="toolbar.allowClose" @click="closeTab(toolbar)"/>
                        </div>
                    </div>
                    <component :is="toolbar.component" v-bind="toolbar.data" :ref="component => setToolbarComponent(toolbar, component)"/>
                </magic-resizer>
            </magic-navbar-item>
        </magic-navbar>
    </div>
</template>
<script setup>
import {onMounted, onUnmounted, provide, reactive, ref, resolveDynamicComponent, shallowRef} from 'vue'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import Message from '../../../scripts/constants/message.js'
import constants from '../../../scripts/constants.js'
import { stableSortByOrder } from '../../../scripts/plugin-extension.js'
import { removeToolbarByIdentity, requestToolbarClose } from './magic-toolbar-close.js'

const navbar = ref(null)
const isFullscreen = ref(false)

function toggleFullscreen(event) {
    const panel = event.target.closest('.magic-resizer-y')
    if (!panel) return
    if (isFullscreen.value) {
        document.exitFullscreen()
    } else {
        panel.requestFullscreen()
    }
}

function onFullscreenChange() {
    isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
    document.addEventListener('fullscreenchange', onFullscreenChange)
})

onUnmounted(() => {
    document.removeEventListener('fullscreenchange', onFullscreenChange)
})
const toolbars = reactive([
    { type: 'api', title: $i('api.title'), icon: 'parameter', component: shallowRef(resolveDynamicComponent('magic-api-info')) },

    { id: 'response', type: 'api', title: $i('toolbars.response'), icon: 'run', component: shallowRef(resolveDynamicComponent('magic-api-response')) },

    // { type: 'task', title: $i('task.title'), icon: 'parameter', component: shallowRef(resolveDynamicComponent('magic-task-info')) },

    { type: 'function', title: $i('fn.title'), icon: 'parameter', component: shallowRef(resolveDynamicComponent('magic-function-info')) },

    { type: 'group-api', title: $i('message.group', $i('api.name')), icon: 'parameter', component: shallowRef(resolveDynamicComponent('magic-api-group')) },

    { id: 'debug', type: ['api', 'task'], title: $i('toolbars.debug'), icon: 'debug-info', component: shallowRef(resolveDynamicComponent('magic-debug')) },

    { id: 'log', title: $i('toolbars.log'), icon: 'log', component: shallowRef(resolveDynamicComponent('magic-log')) },

    { type: 'api', title: $i('toolbars.global'), icon: 'settings', component: shallowRef(resolveDynamicComponent('magic-global')) },

    { id: 'todo', title: 'TODO', icon: 'todo', component: shallowRef(resolveDynamicComponent('magic-todo')) },

    { id: 'history', title: $i('toolbars.history'), icon: 'history', component: shallowRef(resolveDynamicComponent('magic-backup')) },

    { id: 'event', title: $i('toolbars.event'), icon: 'event', component: shallowRef(resolveDynamicComponent('magic-event')), style: { float: 'right'} }
])

const isPluginToolbarVisible = (toolbar, pluginData) => !toolbar.adminOnly || (pluginData && pluginData.adminEnabled === true)
const pluginToolbars = constants.PLUGINS.filter(it => it.toolbars && it.toolbars.length > 0).reduce((items, plugin) => {
    const pluginData = constants.PLUGINS_DATA.find(item => item.name === plugin.name)
    return items.concat(plugin.toolbars.filter(toolbar => isPluginToolbarVisible(toolbar, pluginData)))
}, [])
stableSortByOrder(pluginToolbars).forEach(it => {
	toolbars.push({
        id: it.id,
			type: it.type,
			icon: it.icon,
		title: $i(it.title),
        component: shallowRef(it.component),
        fullscreen: it.fullscreen,
		allowClose: it.allowClose,
		beforeClose: it.beforeClose,
		componentInstance: null
	})
})
const opened = ref({})
const info = ref({})
provide('opened', opened)
provide('info', info)
const setOpenedToolbars = (type, force) => {
    toolbars.forEach(it => it.show = it.type === undefined || (Array.isArray(it.type) ? it.type.find(x => x.type === type) : it.type === type))
    if(navbar.value && force){
        navbar.value.select(toolbars.findIndex(it => it.show))
    }
}
setOpenedToolbars('')
const setToolbarComponent = (toolbar, component) => {
	toolbar.componentInstance = component
}
const closeTab = async toolbar => {
  const openedToolbar = toolbars.find(it => it === toolbar || (toolbar.id && it.id === toolbar.id))
  if(!openedToolbar) return false
  const closed = await requestToolbarClose(openedToolbar, openedToolbar.componentInstance, () => removeToolbarByIdentity(toolbars, openedToolbar))
  if (closed && navbar.value) navbar.value.select(-1)
  return closed
}
bus.$on(Message.ADD_FOOTER_TOOLBAR, async (toolbar) => {
  const index = toolbar.id && toolbars.findIndex(it => it.id === toolbar.id)
  if(index > -1 && !await closeTab(toolbar)) {
    return
  }
  navbar.value.select(toolbars.push(toolbar) - 1)
})
bus.$on(Message.OPEN_EMPTY, () => setOpenedToolbars(''))
bus.$on(Message.OPEN, (event, force) => {
    event.responseBlobValue = null
    opened.value = event
    info.value = event.item
    setOpenedToolbars(event.type, force)
})
bus.$on(Message.OPEN_GROUP, (item) => {
    setOpenedToolbars('group-' + item.type, true)
    info.value = item
})
bus.$on(Message.SWITCH_TOOLBAR, id => {
    const index = toolbars.findIndex(it => it.id === id)
    if(index > -1){
        navbar.value.select(index)
    }
})
</script>
<style scoped>
.magic-toolbar .magic-navbar{
    flex-direction: column-reverse;
}
.magic-toolbar-header{
    height: 26px;
    line-height: 26px;
    border-bottom: 1px solid var(--main-border-color);
    border-top: 1px solid var(--main-border-color);
    background-color: var(--main-background-color);
}
.magic-toolbar-header + div{
    z-index: 99999;
}
.magic-toolbar-header label{
    display: inline-block;
    padding-left: 10px;
}
.magic-toolbar .magic-resizer-y{
    flex-direction: column;
}
.magic-toolbar-header-buttons{
    float: right;
    margin-right: 5px;
    vertical-align: middle;
    display: flex;
    align-items: center;
    height: 26px;
}
.magic-toolbar-header-buttons .magic-icon{
    display: inline-block;
    cursor: pointer;
    padding:0 2px;
    height: 18px;
    line-height: 18px;
    border-radius: 3px;
    z-index: 99999;
}
.magic-toolbar-header-buttons .magic-icon:hover{
    background-color: var(--main-hover-icon-background-color);
}
.magic-toolbar .magic-navbar :deep(.magic-navbar-body){
    background-color: var(--main-background-color) ;
}
</style>
<style>
.magic-panel-api,
.magic-panel-function,
.magic-event{
    display: flex;
    width: 100%;
    height: 100%;
}
.magic-panel-api > div,
.magic-panel-function > div,
.magic-event > div{
    flex: 1;
}
</style>
