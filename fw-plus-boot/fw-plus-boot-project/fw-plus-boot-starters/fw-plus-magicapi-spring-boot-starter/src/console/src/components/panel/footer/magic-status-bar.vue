<template>
    <div class="magic-status-bar">
        <div class="message" v-html="content"></div>
        <magic-online />
        <ul>
            <li v-for="(item, key) in navs" :data-title="item.title" data-tooltip-direction="left-top" :key="key" @click.stop="item.onClick">
                <magic-icon :icon="item.icon"/>
            </li>
        </ul>
    </div>
</template>
<script setup>
import { computed, ref, onMounted } from 'vue'
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
import constants from '../../../scripts/constants.js'
import request from '../../../scripts/request.js'
import store from '../../../scripts/store.js'
import $i from '../../../scripts/i18n.js'
import modal from '../../common/dialog/magic-modal.js'
import { createPluginStatusBarItemsController } from '../../../scripts/plugin-extension.js'

const props = defineProps({
    config: Object
})
const user = ref(null)
const pluginStatusBarItems = ref([])
const { refresh: refreshPluginStatusBarItems } = createPluginStatusBarItemsController({
    getPlugins: () => constants.PLUGINS,
    getHeader: () => props.config.header,
    setItems: items => pluginStatusBarItems.value = items,
    subscribe: listener => bus.$on(Message.PLUGINS_CHANGED, listener)
})
const insertPluginStatusBarItems = array => {
    const items = pluginStatusBarItems.value
    if (items.length === 0) {
        return
    }
    array.splice(0, 0, ...items)
}
const icons = [{
    icon: 'gitee',
    title: 'Gitee',
    displayKey: 'repo',
    onClick: () => window.open('https://gitee.com/ssssssss-team/magic-api')
}, {
    icon: 'git',
    title: 'Github',
    displayKey: 'repo',
    onClick: () => window.open('https://github.com/ssssssss-team/magic-api')
}, {
    icon: 'qq',
    title: $i('message.joinGroup'),
    displayKey: 'qqGroup',
    onClick: () => window.open('https://www.ssssssss.org/magic-api/pages/group/')
}, {
    icon: 'help',
    title: $i('message.document'),
    displayKey: 'document',
    onClick: () => window.open('https://ssssssss.org/magic-api')
}]
const navs = computed(() => {
    const array = icons.filter(it => props.config.header[it.displayKey] !== false)
    insertPluginStatusBarItems(array)
    user.value && user.value.id && user.value.username && array.push({
        icon: 'logout',
        title: user.value.username,
        onClick: () => modal.confirm($i('message.logout'), $i('message.logoutConfirm', user.value.username), () => request.sendPost('/logout').success(() => {
            user.value = null
            constants.HEADER_MAGIC_TOKEN_VALUE = 'unauthorization'
            constants.LOGINED = false
            store.remove(constants.STORE.token)
            bus.$emit(Message.LOGOUT)
            bus.status('message.logoutSuccess')
        }))
    })
    return array
})
const content = ref('')
bus.$on(Message.LOGINED, () => {
    bus.status('message.getCurrentLoginUser')
    request.send('/user').success(value => user.value = value)
})
bus.$on(Message.STATUS, msg => content.value = msg)
onMounted(() => refreshPluginStatusBarItems())
</script>
<style scoped>
/* 底部状态条 */
.magic-status-bar{
    height: 30px;
    line-height: 30px;
    display: flex;
    background-color: var(--main-background-color);
    padding-left: 24px;
}
.magic-status-bar .message{
    flex: 1;
}
.magic-status-bar ul {
    float: right;
    display: inline-flex;
    align-items: center;
    padding-right: 2px;
}
.magic-status-bar ul li{
    cursor: pointer;
    padding:0 4px;
    height: 20px;
    line-height: 20px;
    border-radius: 3px;
}
.magic-status-bar ul li .magic-icon{
    height: 20px;
    line-height: 20px;
}
.magic-status-bar ul li:hover{
    background-color: var(--main-hover-icon-background-color);
}
</style>
