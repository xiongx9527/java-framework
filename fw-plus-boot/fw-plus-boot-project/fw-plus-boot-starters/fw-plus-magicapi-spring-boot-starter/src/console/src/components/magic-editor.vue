<template>
    <div class="magic-editor" :style="themeStyle" @contextmenu.prevent="" ref="root">
		<!-- 全局请求遮罩层 -->
		<div v-if="globalLoading" class="global-loading-mask">
			<div class="loading-content">
				<div class="loading-spinner"></div>
				<div class="loading-text">{{ $i('message.loading') }}</div>
			</div>
		</div>
		
		<magic-login v-show="showLogin" v-model:value="showLogin" v-model:error="errorMessage"/>
		<!-- 顶部区域 -->
		<magic-header :themeStyle="themeStyle" v-if="loadFinish" :header="config.header" :title="config.title"/>
		<magic-main ref="componentMain" @onLoad="init" v-if="loadFinish"/>
		<!-- 状态条 -->
		<magic-status-bar :config="config" v-if="loadFinish"/>
		
		<!-- 消息通知区域 -->
		<magic-notify v-if="loadFinish"/>
		<div class="magic-mounts"></div>
    </div>
</template>
<script setup>
import request, { addLoadingListener } from '../scripts/request.js'
import constants from '../scripts/constants.js'
import HotKey from '../scripts/hotkey.js'
import defaultTheme from '../scripts/editor/default-theme.js'
import darkTheme from '../scripts/editor/dark-theme.js'
import grayNewTheme from '../scripts/editor/gray-new-theme.js'
import darkNewTheme from '../scripts/editor/dark-new-theme.js'
import { defineTheme } from '../scripts/theme.js'
import bus from '../scripts/bus.js'
import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker"
import JsonWorker from "monaco-editor/esm/vs/language/json/json.worker?worker"
import HtmlWorker from "monaco-editor/esm/vs/language/html/html.worker?worker"
import { getCurrentInstance, nextTick, onMounted, onUnmounted, provide, reactive, ref, onUnmounted as onUnmountedVue } from 'vue'
import { initializeMagicScript } from '../scripts/editor/magic-script.js'
import { initializeVue } from '../scripts/editor/vue.js'
import JavaClass from '../scripts/editor/java-class.js'
import MagicWebSocket from '../scripts/websocket.js'
import modal from './common/dialog/magic-modal.js'
import Socket from '../scripts/constants/socket.js'
import Message from '../scripts/constants/message.js'
import { loadPlugin, randomString, replaceURL } from '../scripts/utils.js'
import store from '../scripts/store.js'
import * as monaco from 'monaco-editor'
import { default as $i, add as i18nAdd} from '../scripts/i18n.js'
initializeMagicScript()
initializeVue()
provide('bus', bus)
self.MonacoEnvironment = {
	getWorker: function(moduleId, label) {
		if (label === 'json') {
			return new JsonWorker()
		}
		if (label === 'html') {
			return new HtmlWorker()
		}
		return new EditorWorker()
	}
}
const props = defineProps({
    config: {
		type: Object,
		required: true
	}
})
props.config.header = props.config.header || {}
const showLogin = ref(false)
const componentMain = ref(null)
const root = ref(null)
const loadFinish = ref(false)
const errorMessage = ref(null)
const globalLoading = ref(false)

// 监听全局loading状态
const handleLoadingChange = (loading) => {
    globalLoading.value = loading
}

// 注册loading监听器
onMounted(() => {
    addLoadingListener(handleLoadingChange)
})

// 清理监听器
onUnmountedVue(() => {
    removeLoadingListener(handleLoadingChange)
})
provide('ELEMENT_ROOT', root)
provide('activateUserFiles', ref({}))
let websocket = null
const config = props.config
// 请求URL设置
constants.BASE_URL = config.baseURL || ''
constants.SERVER_URL = config.serverURL || ''
let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '');
if (constants.BASE_URL.startsWith('http')) { // http开头
	link = constants.BASE_URL
} else if (constants.BASE_URL.startsWith('/')) { // / 开头的
	link = `${location.protocol}/${location.host}${constants.BASE_URL}`
} else {
	link = link + '/' + constants.BASE_URL
}
if(config.blockClose !== false) {
	window.onbeforeunload = () => '系统可能不会保存您所做的更改。'
}
const requestConfig = config.request || {
	beforeSend: config => config,
	onError: err => Promise.reject(err)
}
request.getAxios().interceptors.request.use(
	cfg => requestConfig.beforeSend && requestConfig.beforeSend(cfg) || cfg,
	err => requestConfig.onError && requestConfig.onError(err) || Promise.reject(err)
)
const responseConfig = config.response || {
	onSuccess: resp => resp,
	onError: err => Promise.reject(err)
}
request.getAxios().interceptors.response.use(
	resp => responseConfig.onSuccess && responseConfig.onSuccess(resp) || resp,
	err => responseConfig.onError && responseConfig.onError(err) || Promise.reject(err)
)
request.setBaseURL(constants.BASE_URL)
constants.AUTO_SAVE = config.autoSave !== false
bus.status('message.loadClass')
provide('i18n.format', $i)
const installPlugin = () => {
	return new Promise(resolve => {
    request.sendGet('/plugins').success(plugins => {
      constants.PLUGINS_DATA = plugins || []
      Promise.all((constants.PLUGINS_DATA).filter(it => it.javascriptFilename).map(plugin => new Promise(r => {
        bus.status('plugin.loading', true, plugin.name)
        loadPlugin(replaceURL(link + '/plugins/' + plugin.javascriptFilename)).then(() => {
          constants.PLUGINS.push(window[plugin.globalName]({
            'i18n': { add: i18nAdd, format: $i },
            request,
            constants,
            Message,
            bus,
            modal,
            JavaClass,
            monaco
          }))
          bus.status('plugin.loaded', true, plugin.name)
          r();
        }).catch(e => {
          bus.status('plugin.loadFailed', false, plugin.name)
          r()
        })
      }))).then(() => resolve())
    }).error(() => resolve())

    /**
		request.sendGet('/plugins').success(plugins => Promise.all((plugins || []).filter(it => it.javascriptFilename).map(plugin => new Promise(r => {
			bus.status('plugin.loading', true, plugin.name)
			loadPlugin(replaceURL(link + '/plugins/' + plugin.javascriptFilename)).then(() => {
				constants.PLUGINS.push(window[plugin.globalName]({
					'i18n': { add: i18nAdd, format: $i },
					request,
					constants,
					Message,
					bus,
					modal,
					JavaClass,
					monaco
				}))
				bus.status('plugin.loaded', true, plugin.name)
				r();
			}).catch(e => {
				bus.status('plugin.loadFailed', false, plugin.name)
				r()
			})
		}))).then(() => resolve())).error(() => resolve())
     **/
	})
}
const app = getCurrentInstance().appContext.app
Promise.all([JavaClass.initClasses(), JavaClass.initImportClass(), installPlugin()]).then(()=> bus.status('message.loadClassFinish')).catch((e)=>{
	bus.status('message.loadClassError')
}).finally(() => {
	constants.PLUGINS.forEach(it => {
		if(it.datasources){
			it.datasources.filter(it => it.component).forEach(ds => {
				app.component(`magic-datasource-${ds.type}`, ds.component)
			})
		}
	})
	loadFinish.value = true
})
const options = props.config.options || []
provide('options', options)
//request.sendGet('/options').success(res => {
//	res.forEach(it => options.push(it))
//})
request.sendGet('/optionList').success(res => {
	res.forEach(it => options.push(it))
})
const onLogin = () => {
	constants.LOGINED = true
	websocket = new MagicWebSocket(replaceURL(link.replace(/^http/, 'ws') + '/console'))
	nextTick(() => componentMain.value.loadResources())
}
bus.$on(Message.LOGINED, onLogin)
const checkUpdate = () => {
	fetch('https://console.ssssssss.org.cn/latest?group=org.ssssssss&artifactId=magic-api&from=' + constants.MAGIC_API_VERSION_TEXT)
		.then(response => {
			if(props.config.checkUpdate !== false && response.status === 200){
				response.json().then(json => {
					if (json.version && json.version !== 'unknown' && constants.config.version !== json.version) {
						bus.status('message.newVersionRelease', true, json.version)
						if (json.version !== store.get(constants.IGNORE_VERSION)) {
							bus.$emit(Message.NOTIFY, {
								title: $i('message.tips'),
								icon: 'warning',
								content: $i('message.versionUpdate', json.version),
								buttons: [{
									title: $i('message.changelog'),
									onClick: () => {
										window.open('http://www.ssssssss.org/magic-api/changelog.html')
									}
								}, {
									title: $i('message.ignore'),
									onClick: () => {
										store.set(constants.IGNORE_VERSION, json.version)
									}
								}]
							})

						}
					} else {
						bus.status('message.versionLastest')
					}
				})
			}
		})
}

const getMagicTokenValue = () => (config.getMagicTokenValue && config.getMagicTokenValue()) || store.get(constants.STORE.token) || constants.HEADER_MAGIC_TOKEN_VALUE
const autoLogin = () => {
	return new Promise(resolve => {
		constants.HEADER_MAGIC_TOKEN_VALUE = getMagicTokenValue()
		bus.status('message.tryAutoLogin')
		request.sendPost('/login').success(isLogin => {
			showLogin.value = !isLogin
			if(isLogin){
				nextTick(() => {
					bus.status('message.autoLoginSuccess')
					bus.$emit(Message.LOGINED)
				})
			}
		}).end(resolve)
	})
}
const hideLoadingElement = () => {
	if(typeof hideMaLoading === 'function'){
		hideMaLoading()
	}
}
const init = () => {
	request.execute({ url: '/config.json', method: 'get'}).then(res => {
		constants.config = res.data
		// 如果在jar中引用，需要处理一下SERVER_URL
		if(props.config.inJar && location.href.indexOf(res.data.web) > -1){
			let host = location.href.substring(0, location.href.indexOf(res.data.web))
			constants.SERVER_URL = replaceURL(host + '/' + (res.data.prefix || ''))
		}
		if(constants.config.version && constants.config.version !== constants.MAGIC_API_VERSION_TEXT){
			bus.status('message.versionConflict', false, constants.MAGIC_API_VERSION_TEXT, constants.config.version)
			bus.$emit(Message.NOTIFY, {
				icon: 'error',
				title: $i('message.versionCheck'),
				content: $i('message.versionConflict', constants.MAGIC_API_VERSION_TEXT, constants.config.version)
			})
		}
		autoLogin().then(() => {
			hideLoadingElement()
			checkUpdate()
		})
	}).catch(e => {
		console.error(e)
		hideLoadingElement()
		errorMessage.value = $i('message.loadConfigError')
	})
}
// 快捷键处理
const bindKey = () =>{
	const element = document.body
	// Ctrl + S 执行保存
	HotKey.bind(element, HotKey.Ctrl | HotKey.S, ()=> bus.$emit(Message.DO_SAVE, true))
	// Ctrl + Q 执行测试
	HotKey.bind(element, HotKey.Ctrl | HotKey.Q, ()=> bus.$emit(Message.DO_TEST))
	// Ctrl + E 最近打开
	HotKey.bind(element, HotKey.Ctrl | HotKey.E, ()=> bus.$emit(Message.DO_RECENT))
	// F8 DEBUG 到下一个断点
	HotKey.bind(element, HotKey.F8, ()=> bus.$emit(Message.DEBUG_CONTINUE))
	// F6 DEBUG 执行单步
	HotKey.bind(element, HotKey.F6, ()=> bus.$emit(Message.DEBUG_SETPINTO))
	// Ctrl + Shift + F 全局搜索
	HotKey.bind(element, HotKey.Ctrl | HotKey.Shift | HotKey.F, ()=> bus.$emit(Message.DO_SEARCH))
}
onMounted(() => {
	bindKey()
})
onUnmounted(() => HotKey.unbind())

// 皮肤相关
const themeStyle = reactive({})
defineTheme('default', defaultTheme)
defineTheme('dark', darkTheme)
defineTheme('gray-new', grayNewTheme)
defineTheme('dark-new', darkNewTheme)
Object.keys(config.themes || {}).forEach(themeKey => {
	defineTheme(themeKey, config.themes[themeKey])
})
constants.THEME = config.theme || 'default'
constants.DEFAULT_EXPAND = config.defaultExpand !== false
constants.JDBC_DRIVERS = config.jdbcDrivers || constants.JDBC_DRIVERS
constants.DATASOURCE_TYPES = config.datasourceTypes || constants.DATASOURCE_TYPES
if(config.editorFontFamily !== undefined){
	constants.EDITOR_FONT_FAMILY = config.editorFontFamily
}
if(config.editorFontSize !== undefined){
	constants.EDITOR_FONT_SIZE = config.editorFontSize
}
constants.FONT_LIGATURES = config.fontLigatures !== false
if(config.logMaxRows !== undefined){
	constants.LOG_MAX_ROWS = Math.max(config.logMaxRows, 10)
}
if(config.decorationTimeout !== undefined) {
	constants.DECORATION_TIMEOUT = config.decorationTimeout
}
bus.$on(Message.MESSAGE, (msgType, content) => {
	if(websocket){
		if(content){
			websocket.send(`${msgType},${content}`)
		}else{
			websocket.send(msgType)
		}
	}
})
bus.$event(Socket.OPEN, () => {
	constants.CLIENT_ID = randomString(16)
	nextTick(() => bus.send(Socket.LOGIN, [getMagicTokenValue(), constants.CLIENT_ID].join(',')))
})
bus.$event(Socket.LOGIN_RESPONSE, ([ret, user]) => {
	if(ret === '1'){
		constants.user = user
	} else {
		// 登录失败
	}
})
bus.$event(Socket.REFRESH_TOKEN, ([newToken]) => {
	constants.HEADER_MAGIC_TOKEN_VALUE = newToken
	store.set(constants.STORE.token, constants.HEADER_MAGIC_TOKEN_VALUE)
})
const showLoginFunc = () => {
	showLogin.value = true
	websocket && websocket.close()
	websocket = null
}
bus.$on(Message.LOGOUT, showLoginFunc)
bus.$on(Message.SHOW_LOGIN, showLoginFunc)
</script>

<style scoped>
.global-loading-mask {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    z-index: 9999999999999;
    display: flex;
    align-items: center;
    justify-content: center;
}

.loading-content {
    background: white;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    text-align: center;
    min-width: 120px;
}

.loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #f3f3f3;
    border-top: 3px solid #007acc;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 12px;
    transform-origin: center center;
    will-change: transform;
}

.loading-text {
    color: #333;
    font-size: 14px;
    font-weight: 500;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}
</style>
