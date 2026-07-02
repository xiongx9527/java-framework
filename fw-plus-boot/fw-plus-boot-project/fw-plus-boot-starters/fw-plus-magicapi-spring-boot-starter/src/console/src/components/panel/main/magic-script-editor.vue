<template>
	<div class="magic-script-editor">
		<div class="magic-empty-container" v-if="openedScripts.length === 0">
			<div class="magic-hot-key">
				<p>
				{{ $i('message.save') }}<em>Ctrl + S</em><br/>
				{{ $i('message.run') }}<em>Ctrl + Q</em><br/>
				{{ $i('editor.tooltip.complection')}}<em>Alt + /</em><br/>
				{{ $i('editor.tooltip.resume')}}<em>F8</em><br/>
				{{ $i('editor.tooltip.stepInto')}}<em>F6</em><br/>
				{{ $i('editor.tooltip.format')}}<em>Ctrl + Alt + L</em><br/>
				{{ $i('editor.tooltip.recent')}}<em>Ctrl + E</em>
				</p>
			</div>
		</div>
		<template v-else>
			<magic-tab v-model:value="selectTab" :tabs="openedScripts" className="magic-script-tab"  ref="tab"
				:allow-close="true" @close="onClose" @change="tab => bus.$emit(Message.OPEN, tab)"
				@before-change="beforeChange" @item-contextmenu="onContextMenu">
				<template v-slot="{ tab }">
					<magic-text-icon :icon="tab.getIcon(tab.item)"/>{{ tab.item.name }}<span v-if="isUpdated(tab)">*</span>
					<magic-icon v-if="tab.item.lock === $LOCKED" icon="lock"/>
					<magic-avatar-group :users="activateUserFiles[tab.item.id] || []" :max="3" :size="20"/>
				</template>
			</magic-tab>
			<magic-loading :loading="loading">
				<div class="magic-monaco-editor-wrapper">
					<component :is="selectTab.component" :Message="Message" :bus="bus" :request="request" :selectTab="selectTab" v-if="selectTab && selectTab.pageType=='component'" />
					<magic-monaco-editor ref="editor" v-else v-model:value="selectTab.item.script" v-model:decorations="selectTab.decorations" :language="selectTab.language" :support-breakpoint="true"/>
				</div>
			</magic-loading>
		</template>
	</div>
</template>
<script setup>
import { getCurrentInstance, inject, nextTick, onMounted, reactive, ref, toRaw, watch } from 'vue'
import bus from '../../../scripts/bus.js'
import request from '../../../scripts/request.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import { Range } from 'monaco-editor'
import { convertVariables } from '../../../scripts/utils.js'
import RequestParameter from '../../../scripts/editor/request-parameter.js'
import Socket from '../../../scripts/constants/socket.js'
import store from '../../../scripts/store.js'
import $i from '../../../scripts/i18n.js'
import { encrypt } from '../../../scripts/rot13.js'
const { proxy } = getCurrentInstance()
const openedScripts = reactive([])
const selectTab = ref({})
const loading = ref(true)
const editor = ref(null)
const tab = ref(null)
const activateUserFiles = inject('activateUserFiles')
const javaTypes = {
	'String': 'java.lang.String',
	'Integer': 'java.lang.Integer',
	'Double': 'java.lang.Double',
	'Long': 'java.lang.Long',
	'Byte': 'java.lang.Byte',
	'Short': 'java.lang.Short',
	'Float': 'java.lang.Float',
	'MultipartFile': 'org.springframework.web.multipart.MultipartFile',
	'MultipartFiles': 'java.util.List',
}
RequestParameter.setEnvironment(() => {
	const env = {}
	const item = selectTab.value?.item
	const append = array => array && Array.isArray(array) && array.forEach(it => {
		if(it && typeof it.name === 'string' && it.dataType){
			env[it.name] = javaTypes[it.dataType] || 'java.lang.Object'
		}
	})
	if(item){
		append(item?.parameters)
		append(item?.paths)
	}
	return env
})
// 关闭tab
const onClose = tab => {
	let index = openedScripts.findIndex(it => it === tab)
	openedScripts.splice(index, 1)
	if(tab === selectTab.value){
		let len = openedScripts.length
		if(index < len){
			bus.$emit(Message.OPEN, openedScripts[index])
		} else if(len > 0) {
			bus.$emit(Message.OPEN, openedScripts[index - 1])
		}
	}
	bus.$emit(Message.CLOSE, tab.item)
	// 没有打开的文件时
	if(openedScripts.length === 0){
		// 设置标题为空
		bus.$emit(Message.OPEN_EMPTY)
		selectTab.value = {}
	}
}
watch(openedScripts, (newVal) => {
	store.set(constants.RECENT_OPENED_TAB, newVal.filter(it => it.item?.id).map(it => it.item.id))
})
// 执行保存
const doSave = (flag) => {
	const opened = selectTab.value
	if(opened && opened.item){
		const item = selectTab.value.processSave(opened.item)
		Object.keys(item).forEach(key => opened.item[key] = item[key])
		return request.sendJson(`/resource/file/${selectTab.value.type}/save?auto=${flag ? 0 : 1}`, item).success((id) => {
			const msg = `${opened.title}「${opened.path()}」`
			if(id) {
				bus.status('message.saveSuccess', true, msg)
				opened.tmpObject = JSON.parse(JSON.stringify(item))
				if(opened.item.id !== id){
					bus.loading(1)
				}
				opened.item.id = id
			}else{
				bus.status('message.saveFailed', false, msg)
				proxy.$alert($i('message.saveFailed', msg))
			}
		})
	}
}
// 执行测试
const doTest = () => selectTab.value.doTest(selectTab.value)
const doContinue = step => {
    if(selectTab.value.debuging){
        editor.value.removedDecorations(selectTab.value.debugDecorations)
        selectTab.value.debuging = false
        selectTab.value.variables = null
        const breakpoints = (selectTab.value.decorations || []).filter(it => it.options.linesDecorationsClassName === 'breakpoints').map(it => it.range.startLineNumber).join('|')
        bus.send(Socket.RESUME_BREAKPOINT, [selectTab.value.item.id, (step === true ? '1' : '0'), breakpoints].join(','))
    }
}
// tab 右键菜单事件
const onContextMenu = (event, item, index) => {
	const menus = [{
		label: $i('editor.tab.close'),
		divided: true,
		onClick(){
			onClose(item)
		}
	},{
		label: $i('editor.tab.closeOther'),
		divided: true,
		onClick(){
			[...openedScripts].forEach((it, i) => i != index && onClose(it))
		}
	},{
		label: $i('editor.tab.closeLeft'),
		onClick(){
			[...openedScripts].forEach((it, i) => i < index && onClose(it))
		}
	},{
		label: $i('editor.tab.closeRight'),
		divided: true,
		onClick(){
			[...openedScripts].forEach((it, i) => i > index && onClose(it))
		}
	},{
		label: $i('editor.tab.closeAll'),
		onClick(){
			[...openedScripts].forEach(it => onClose(it))
		}
	}]
	constants.PLUGINS.forEach(it => {
		if(it.contextmenu && typeof it.contextmenu === 'function'){
			const pMenus = it.contextmenu({
				...item,
				menuType: 'editorTab'
			})
			pMenus && pMenus.length && pMenus.forEach(m => menus.push(m))
		}
	})
	proxy.$contextmenu({ menus, event})
}
// 判断是否有变动
const isUpdated = (tab) => Object.keys(tab.tmpObject || {}).some(k => {
	const v1 = tab.tmpObject[k]
	const v2 = tab.item[k]
	if(v1 === v2 || k === 'properties' || k === 'responseBody' || k === 'responseBodyDefinition'){
		return false
	}
	return (typeof v1 === 'object' || typeof v2 === 'object') ? JSON.stringify(v1) !== JSON.stringify(v2) : v1 !== v2
})
// 退出登录
bus.$on(Message.LOGOUT, () => [...openedScripts].forEach(tab => onClose(tab)))
// 执行删除时
bus.$on(Message.DELETE_FILE, item => {
	const index = openedScripts.findIndex(it => it.item === item)
	if(index > -1){
		onClose(openedScripts[index])
	}
})
// 重新加载资源完毕时
bus.$on(Message.RELOAD_RESOURCES_FINISH, ()=> [...openedScripts].forEach(it => onClose(it)))
// 登录响应
bus.$event(Socket.LOGIN_RESPONSE, () => {
	if(selectTab.value){
		bus.send(Socket.SET_FILE_ID, selectTab.value.item?.id || '0')
	}
})
const beforeChange = tab => {
	if(tab && editor.value){
		tab.scrollTop = editor.value.getScrollTop()
	}
}
// 打开文件
bus.$on(Message.OPEN, opened => {
	let find = openedScripts.find(it => it.item === opened.item || (it.item.id && it.item.id === opened.item.id))
	bus.send(Socket.SET_FILE_ID, opened.item.id || '0')
	if(find){
		selectTab.value = find
		loading.value = false
		nextTick(() => editor.value.setScrollTop(find.scrollTop || 0))
	} else {
		openedScripts.push(opened)
		selectTab.value = opened
		if(opened.item.id && !opened.item.script){
			loading.value = true
			request.sendGet(`/resource/file/${opened.item.id}`).success(data => {
				bus.status('message.getDetail', true, `${opened.title}「${opened.path()}」`)
				Object.keys(data).forEach(key => opened.item[key] = data[key])
				opened.tmpObject = JSON.parse(JSON.stringify(opened.processSave(data)))
				loading.value = false
				nextTick(() => editor.value.setScrollTop(0))
			})
		} else {
			opened.tmpObject = JSON.parse(JSON.stringify(opened.processSave(opened.item)))
			loading.value = false
			nextTick(() => editor.value.setScrollTop(0))
		}
	}
	if(selectTab.value.decorations && selectTab.value.decorations.length > 0){
		nextTick(() => {
			const decorations = toRaw(selectTab.value.decorations)
			selectTab.value.debugDecorations = editor.value.appendDecoration(decorations)
												.map((it, index) => decorations[index].options?.className === 'debug-line' ? it : null)
												.filter(it => it !== null) || []
		})
	}
	nextTick(() => tab.value && tab.value.scrollIntoView(opened))
})
// 保存事件
bus.$on(Message.DO_SAVE, doSave)
// 测试事件
bus.$on(Message.DO_TEST, () => {
	const opened = selectTab.value
	if(opened && opened.item && opened.runnable && opened.doTest && opened.running !== true){
		if(constants.AUTO_SAVE && opened.item.lock !== '1'){
			doSave().end(successed => successed && doTest())
		} else {
			doTest()
		}
	}
})
// 进入断点
bus.$event(Socket.BREAKPOINT, ([ scriptId, { range, variables } ]) => {
    // 如果切换或已关闭
    if(selectTab.value?.item?.id !== scriptId){
        const opened = openedScripts.find(it => it.item.id === scriptId)
        if(opened){
            // 切换tab
            bus.$emit(Message.OPEN, opened)
        }else{
            // TODO  重新打开
        }
    }
    nextTick(() => {
        selectTab.value.variables = convertVariables(variables)
        selectTab.value.debuging = true
        selectTab.value.debugDecorations = [editor.value.appendDecoration([{
            range: new Range(range[0], 1, range[0], 1),
            options: {
                isWholeLine: true,
                inlineClassName: 'debug-line',
                className: 'debug-line'
            }
        }])]
        bus.$emit(Message.SWITCH_TOOLBAR, 'debug')
    })
})
// 恢复断点
bus.$on(Message.DEBUG_CONTINUE, doContinue)
// 断点单步运行
bus.$on(Message.DEBUG_SETPINTO, () => doContinue(true))
// 执行出现异常
bus.$event(Socket.EXCEPTION, ([ [scriptId, message, line] ]) => {
	if(selectTab.value?.item?.id === scriptId){
		const range = new Range(line[0], line[2], line[1], line[3] + 1)
		const instance = editor.value.getInstance()
		const decorations = instance.deltaDecorations([], [{
			range,
			options: {
				hoverMessage: {
					value: message
				},
				inlineClassName: 'squiggly-error'
			}
		}])
		instance.revealRangeInCenter(range)
		instance.focus()
		if(constants.DECORATION_TIMEOUT >= 0){
			setTimeout(() => instance.deltaDecorations(decorations, []), constants.DECORATION_TIMEOUT)
		}
	}
})
const emit = defineEmits(['onLoad'])
onMounted(() => emit('onLoad'))
</script>
<style scoped>
.magic-script-editor{
	flex: 1;
	overflow: hidden;
	position: relative;
}
.magic-script-editor .magic-monaco-editor-wrapper{
    position: absolute;
	top: 30px;
	left: 0;
	right: 0;
	bottom: 0;
}
.magic-empty-container{
	flex: 1;
	position: relative;
	width: 100%;
	height: 100%;
	background: var(--empty-background-color);
}
.magic-hot-key{
	position: absolute;
	top: 50%;
	margin-top: -105px;
	text-align: center;
	color: var(--empty-color);
	font-size: 16px;
	width: 100%;
}
.magic-hot-key p{
	display: inline-block;
	text-align: left;
	line-height: 30px;
}
.magic-hot-key p em{
	margin-left: 15px;
	font-style: normal;
	color: var(--empty-key-color);
}
.magic-monaco-editor{
	position: absolute;
	top: 0;
	bottom: 0;
	left:0;
	right: 0;
	overflow: visible !important;
}
.magic-script-editor :deep(.magic-avatar-group){
	margin-left: 10px;
}
</style>
