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
import { computed, ref, onMounted, shallowRef, resolveDynamicComponent } from 'vue'
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
import constants from '../../../scripts/constants.js'
import request from '../../../scripts/request.js'
import store from '../../../scripts/store.js'
import $i from '../../../scripts/i18n.js'
import modal from '../../common/dialog/magic-modal.js'
import {replaceURL} from "../../../scripts/utils";
const props = defineProps({
    config: Object
})
const user = ref(null)
const hasVue3Plugin = ref(false)
const hasLangchain4jPlugin = ref(false)
const hasDtpPlugin = ref(false)
const hasFreemarkerPlugin = ref(false)
const hasConfigPlugin = ref(false)
const hasSatokenPlugin = ref(false)
const hasWsPlugin = ref(false)
const hasMqttPlugin = ref(false)
const hasKafkaPlugin = ref(false)
const hasPulsarPlugin = ref(false)
const hasDebeziumPlugin = ref(false)
const hasDruidPlugin = ref(false)
const hasAdminPlugin = ref(false)
const hasSwaggerPlugin = ref(false)
const hasEurekaPlugin = ref(false)
const pluginStatusBarItems = ref([])
const refreshPluginStatusBarItems = () => {
    pluginStatusBarItems.value = constants.PLUGINS
        .filter(plugin => plugin.statusBarItems && Array.isArray(plugin.statusBarItems))
        .reduce((items, plugin) => items.concat(plugin.statusBarItems), [])
}
const insertPluginStatusBarItems = array => {
    const items = pluginStatusBarItems.value
        .filter(it => props.config.header[it.displayKey] !== false)
    if (items.length === 0) {
        return
    }
    array.splice(0, 0, ...items)
}
const icons = [{
    icon: 'config',
    title: 'Config',
    displayKey: 'config',
    onClick: () => openConfig()
},{
    icon: 'ftl',
    title: 'Freemarker',
    displayKey: 'freemarker',
    onClick: () => openFtl()
},{
    icon: 'dtp',
    title: 'Dynamictp',
    displayKey: 'dtp',
    onClick: () => openDtp()
},{
    icon: 'kafka',
    title: 'Kafka',
    displayKey: 'kafka',
    onClick: () => openKafka()
},{
    icon: 'pulsar',
    title: 'Pulsar',
    displayKey: 'pulsar',
    onClick: () => openPulsar()
},{
    icon: 'debezium',
    title: 'Debezium',
    displayKey: 'debezium',
    onClick: () => openDebezium()
}, {
    icon: 'langchain4j',
    title: 'Langchain4j',
    displayKey: 'langchain4j',
    onClick: () => openLangchain4j()
}, {
    icon: 'ws',
    title: 'Websocket client',
    displayKey: 'ws',
    onClick: () => openWs()
}, {
    icon: 'mqtt',
    title: 'Mqtt client',
    displayKey: 'mqtt',
    onClick: () => openMqtt()
}, {
    icon: 'druid',
    title: 'Druid DataSource',
    displayKey: 'druid',
    onClick: () => openDruid()
}, {
  icon: 'knife4j',
  title: 'Swagger Api Doc',
  displayKey: 'swagger',
  onClick: () => openSwagger()
}, {
    icon: 'vue',
    title: 'Vue SFC',
    displayKey: 'vue3',
    onClick: () => openVue3Compiler()
}, {
  icon: 'satoken',
  title: 'Satoken',
  displayKey: 'satoken',
  onClick: () => openSatoken()
}, {
  icon: 'admin',
  title: 'Spring Boot Admin',
  displayKey: 'admin',
  onClick: () => openSpringBootAdmin()
}, {
  icon: 'eureka',
  title: 'Eureka Dashboard',
  displayKey: 'eureka',
  onClick: () => openEurekaDashboard()
}, {
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
    const array = icons.filter(it => {
        // 根据插件状态控制按钮显示隐藏
        if (it.displayKey === 'freemarker') {
            return hasFreemarkerPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'dtp') {
            return hasDtpPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'langchain4j') {
            return hasLangchain4jPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'vue3') {
            return hasVue3Plugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'config') {
            return hasConfigPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'satoken') {
            return hasSatokenPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'ws') {
            return hasWsPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'mqtt') {
            return hasMqttPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'kafka') {
            return hasKafkaPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'pulsar') {
            return hasPulsarPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'debezium') {
            return hasDebeziumPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'druid') {
            return hasDruidPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'admin') {
            return hasAdminPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'swagger') {
            return hasSwaggerPlugin.value && props.config.header[it.displayKey] !== false
        }
        if (it.displayKey === 'eureka') {
            return hasEurekaPlugin.value && props.config.header[it.displayKey] !== false
        }
        // 其他按钮根据配置显示
        return props.config.header[it.displayKey] !== false
    })
    insertPluginStatusBarItems(array)
    user.value && user.value.id && user.value.username&& array.push({
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
// 监听插件加载完成事件
bus.$on(Message.STATUS, msg => {
    content.value = msg
    // 当插件加载完成时检查插件（检查包含"加载完毕"或"Loaded"的字符串）
    if (msg.includes('加载完毕') || msg.includes('Loaded')) {
        hasVue3Plugin.value = checkPlugin('vue3')
        hasLangchain4jPlugin.value = checkPlugin('langchain4j-datasource')
        hasDtpPlugin.value = checkPlugin('dtp-datasource')
        hasFreemarkerPlugin.value = checkPlugin('freemarker')
        hasConfigPlugin.value = checkPlugin('config')
        hasSatokenPlugin.value = checkPlugin('satoken')
        hasWsPlugin.value = checkPlugin('ws-subscribe')
        hasMqttPlugin.value = checkPlugin('mqtt-subscribe')
        hasKafkaPlugin.value = checkPlugin('kafka-subscribe')
        hasPulsarPlugin.value = checkPlugin('pulsar-subscribe')
        hasDebeziumPlugin.value = checkPlugin('debezium-datasource')
        hasDruidPlugin.value = checkPlugin('druid')
        hasAdminPlugin.value = checkPlugin('admin')
        hasSwaggerPlugin.value = checkPlugin('swagger')
        hasEurekaPlugin.value = checkPlugin('eureka')
        refreshPluginStatusBarItems()
    }
})

const openConfig = () => {
    window.open('https://spring.io/projects/spring-cloud-config/', '_blank')
}

const openFtl = () => {
    window.open('https://freemarker.apache.org/', '_blank')
}

const openDtp = () => {
    window.open('https://dynamictp.cn/', '_blank')
}

const openLangchain4j = () => {
    window.open('https://docs.langchain4j.dev/', '_blank')
}

const openKafka = () => {
  window.open('https://kafka.apache.org/', '_blank')
}

const openPulsar = () => {
  bus.$emit(Message.ADD_FOOTER_TOOLBAR, {
    id: 'pulsar', 
    title: $i('pulsar.title'), 
    icon: 'pulsar', 
    component: shallowRef(resolveDynamicComponent('magic-pulsar')), 
    max: 1000, 
    min: 200, 
    value: 500 
  })
}

const openDebezium = () => {
  window.open('https://debezium.io/', '_blank')
}

const openSatoken = () => {
  window.open('https://sa-token.cc/', '_blank')
}

const openWs = () => {
  /**
  const currentPath = window.location.pathname;
  const cleanedPath = currentPath.replace(/\/index\.html$/, '');
  window.open(cleanedPath + '/ws/index.html', '_blank')
      **/
  let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
  // 使用系统定义的baseURL
  const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
  if (baseURL.startsWith('http')) {
    link = baseURL
  } else if (baseURL.startsWith('./')) {
    link = window.location.pathname.replace(/\/index\.html$/, '');
  }
  const druidUrl = link + '/ws/index.html'
  window.open(druidUrl, '_blank');
}

const openMqtt = () => {
  /**
  const currentPath = window.location.pathname;
  const cleanedPath = currentPath.replace(/\/index\.html$/, '');
  window.open(cleanedPath + '/mqtt/index.html', '_blank')
      **/
  let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
  // 使用系统定义的baseURL
  const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
  if (baseURL.startsWith('http')) {
    link = baseURL
  } else if (baseURL.startsWith('./')) {
    link = window.location.pathname.replace(/\/index\.html$/, '');
  }
  const druidUrl = link + '/mqtt/index.html'
  window.open(druidUrl, '_blank');
}

const openDruid = () => {
  /**
  // 获取当前页面的协议和主机地址（包括IP和端口）
  const protocol = window.location.protocol;
  const host = window.location.host;
  const url = `${protocol}//${host}/druid/login.html`;
  window.open(url, '_blank');
      **/
  let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
  // 使用系统定义的baseURL
  const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
  if (baseURL.startsWith('http')) {
    var url = new URL(baseURL)
    link = url.origin
  } else if (baseURL.startsWith('./')) {
    link = window.location.protocol+"//"+window.location.host
  }
  const druidUrl = link + '/druid/login.html'
  window.open(druidUrl, '_blank');
}

const openSwagger = () => {
  /**
   // 获取当前页面的协议和主机地址（包括IP和端口）
   const protocol = window.location.protocol;
   const host = window.location.host;
   const url = `${protocol}//${host}/druid/login.html`;
   window.open(url, '_blank');
   **/
  let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
  // 使用系统定义的baseURL
  const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
  if (baseURL.startsWith('http')) {
    var url = new URL(baseURL)
    link = url.origin
  } else if (baseURL.startsWith('./')) {
    link = window.location.protocol+"//"+window.location.host
  }
  const druidUrl = link + '/doc.html'
  window.open(druidUrl, '_blank');
}

const openSpringBootAdmin = () => {
  let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
  // 使用系统定义的baseURL
  const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
  if (baseURL.startsWith('http')) {
    var url = new URL(baseURL)
    link = url.origin
  } else if (baseURL.startsWith('./')) {
    link = window.location.protocol+"//"+window.location.host
  }
  let adminUrl = replaceURL(link)
  constants.PLUGINS_DATA.forEach(plugin => {
    if (plugin.name === 'admin' && plugin.globalName && plugin.globalName != '') {
      adminUrl = replaceURL(link + '/' + plugin.globalName)
    }
  })

  window.open(adminUrl, '_blank');
}

const openEurekaDashboard = () => {
  let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
  // 使用系统定义的baseURL
  const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
  if (baseURL.startsWith('http')) {
    var url = new URL(baseURL)
    link = url.origin
  } else if (baseURL.startsWith('./')) {
    link = window.location.protocol+"//"+window.location.host
  }
  let eurekaUrl = replaceURL(link)
  constants.PLUGINS_DATA.forEach(plugin => {
    if (plugin.name === 'eureka' && plugin.globalName && plugin.globalName != '') {
      eurekaUrl = replaceURL(link + '/' + plugin.globalName)
    }
  })

  window.open(eurekaUrl, '_blank');
}

// 打开Vue3编译器
const openVue3Compiler = () => {
    // 在新标签页中打开Vue3编译器
    window.open('https://play.vuejs.org/', '_blank')
}

// 检查是否有指定插件
const checkPlugin = (pluginName) => {
    // 直接从已加载的插件列表中检查是否有指定插件
    let hasPlugin = constants.PLUGINS.some(plugin => {
        if (!plugin) return false
        
        if (plugin.resource && Array.isArray(plugin.resource)) {
            return plugin.resource.some(resource => 
                resource && resource.type && resource.type.includes(pluginName)
            )
        }
        
        if (plugin.toolbars && Array.isArray(plugin.toolbars)) {
            return plugin.toolbars.some(toolbar => 
                toolbar && toolbar.type && toolbar.type.includes(pluginName)
            )
        }

        if (plugin.datasources && Array.isArray(plugin.datasources)) {
            return plugin.datasources.some(datasource => 
                datasource && datasource.type && datasource.type.includes(pluginName)
            )
        }
        
        // 检查插件名称或类型
        return (plugin.name && plugin.name.includes(pluginName)) || 
               (plugin.type && plugin.type.includes(pluginName))
    })

    if (hasPlugin!=true && ('druid' === pluginName || 'admin' === pluginName
        || 'eureka' === pluginName || 'swagger' === pluginName)) {
      constants.PLUGINS_DATA.forEach(plugin => {
        if (plugin.name === pluginName) {
          hasPlugin = true ;
        }
      })
    }
    return hasPlugin
}

// 组件挂载后检查插件状态
onMounted(() => {
    // 立即检查一次
    hasVue3Plugin.value = checkPlugin('vue3')
    hasLangchain4jPlugin.value = checkPlugin('langchain4j-datasource')
    hasDtpPlugin.value = checkPlugin('dtp-datasource')
    hasFreemarkerPlugin.value = checkPlugin('freemarker')
    hasConfigPlugin.value = checkPlugin('config')
    hasSatokenPlugin.value = checkPlugin('satoken')
    hasWsPlugin.value = checkPlugin('ws-subscribe')
    hasMqttPlugin.value = checkPlugin('mqtt-subscribe')
    hasKafkaPlugin.value = checkPlugin('kafka-subscribe')
    hasPulsarPlugin.value = checkPlugin('pulsar-subscribe')
    hasDebeziumPlugin.value = checkPlugin('debezium-datasource')
    hasDruidPlugin.value = checkPlugin('druid')
    hasAdminPlugin.value = checkPlugin('admin')
    hasSwaggerPlugin.value = checkPlugin('swagger')
    hasEurekaPlugin.value = checkPlugin('eureka')
    refreshPluginStatusBarItems()

    // 设置定时器，确保插件加载完成后能够检测到
    const timer = setInterval(() => {
        if (constants.PLUGINS.length > 0) {
            hasVue3Plugin.value = checkPlugin('vue3')
            hasLangchain4jPlugin.value = checkPlugin('langchain4j-datasource')
            hasDtpPlugin.value = checkPlugin('dtp-datasource')
            hasFreemarkerPlugin.value = checkPlugin('freemarker')
            hasConfigPlugin.value = checkPlugin('config')
            hasWsPlugin.value = checkPlugin('ws-subscribe')
            hasMqttPlugin.value = checkPlugin('mqtt-subscribe')
            hasKafkaPlugin.value = checkPlugin('kafka-subscribe')
            hasPulsarPlugin.value = checkPlugin('pulsar-subscribe')
            hasDebeziumPlugin.value = checkPlugin('debezium-datasource')
            hasDruidPlugin.value = checkPlugin('druid')
            hasAdminPlugin.value = checkPlugin('admin')
            hasSwaggerPlugin.value = checkPlugin('swagger')
            hasEurekaPlugin.value = checkPlugin('eureka')
            refreshPluginStatusBarItems()
            clearInterval(timer)
        }
    }, 1000)
    
    // 10秒后清除定时器
    setTimeout(() => {
        clearInterval(timer)
    }, 10000)
})
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
