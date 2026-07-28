<template>
	<div class="magic-main">
    <!-- 左侧导航条 -->
    <div class="left-navbar-container">
      <magic-navbar direction="vertical" :to="$refs.mrl" v-if="nextRender" ref="mnl" :spliter="true">
        <magic-navbar-item v-for="(navbar, index) in leftNavbars" :key="index" v-bind="navbar">
          <magic-resizer :max="750" :min="270" direction='x'>
            <magic-loading :loading="getLoadingState(navbar.type)">
              <magic-resource v-bind="navbar" :data="resources[navbar.type]" @close="$refs.mnl.select(-1)" @onLoad="resourceOnLoad" tooltip-direction="left-bottom"/>
            </magic-loading>
          </magic-resizer>
        </magic-navbar-item>
      </magic-navbar>
    </div>
		<div class="magic-main-body-wrapper">
			<div class="magic-main-body">
				<div ref="mrl" class="magic-navbar magic-navbar__vertical"/>
				<magic-script-editor/>
				<div ref="mrr" class="magic-navbar magic-navbar__vertical reverse"/>
			</div>
			<!-- 底部工具条 -->
			<magic-toolbar />
		</div>
    <!-- 右侧导航条 -->
    <div class="right-navbar-container">
      <magic-navbar :reverse="true" :default-select="-1" direction="vertical" :to="$refs.mrr" :spliter="true" tooltip-direction="left">
        <magic-navbar-item v-for="(navbar, index) in rightNavbars" :key="index" :title="navbar.title" :icon="navbar.icon">
          <magic-resizer :max="420" :min="140" :value="200" direction='x' :reverse="true" v-if="nextRender">
            <magic-loading :loading="getLoadingState(navbar.type)">
              <magic-data-resource :type="navbar.type" :title="navbar.name" :data="resources[navbar.type]"/>
            </magic-loading>
          </magic-resizer>
        </magic-navbar-item>
      </magic-navbar>
    </div>
		<!-- 全局搜索 -->
		<magic-search />
		<!-- 导出 -->
		<magic-export />
		<!-- 上传 -->
		<magic-upload />
		<!-- 推送 -->
		<magic-push />
		<!-- 最近打开 -->
		<magic-recent-opened />
	</div>
</template>
<script setup>
import { ref, nextTick, onMounted, provide } from 'vue'
import request from '../../../scripts/request.js'
import bus from '../../../scripts/bus.js'
import MagicAPI from '../../../scripts/service/magic-api.js'
import MagicFunction from '../../../scripts/service/magic-function.js'
import MagicDatasource from '../../../scripts/service/magic-datasource.js'

import Message from '../../../scripts/constants/message.js'
import { replaceURL } from '../../../scripts/utils.js'
import store from '../../../scripts/store.js'
import constants from '../../../scripts/constants.js'
import $i from '../../../scripts/i18n.js'
const nextRender = ref(false)
const resources = ref({})
const loadingStates = ref({})
// 初始化所有资源类型的加载状态
const initLoadingStates = () => {
  const allTypes = [...leftNavbars, ...rightNavbars].map(it => it.type)
  allTypes.forEach(type => {
    loadingStates.value[type] = false
  })
}
// 获取指定类型的加载状态
const getLoadingState = (type) => {
  return loadingStates.value[type] || false
}
// 设置指定类型的加载状态
const setLoadingState = (type, value) => {
  loadingStates.value[type] = value
}
const mnl = ref(null)
const emit = defineEmits(['onLoad'])
const deepFind = (array, itemOrId, stack) => {
	for(let i=0, len = array.length; i< len; i++){
		const item = array[i]
		if(item.id === itemOrId || item === itemOrId){
			stack.push(item)
			return true
		}else if(item.folder && item.children){
			if(deepFind(item.children, itemOrId, stack)) {
				stack.push(item)
				return true
			}
		}
	}
	return false
}
const findResource = id => {
	const entries = Object.entries(resources.value)
	for(let i=0, len = entries.length; i< len; i++){
		const nodes = []
		const entry = entries[i]
		deepFind(entry[1], id, nodes)
		if(nodes.length > 0){
			return {
				item: nodes[0],
				type: entry[0],
				name: replaceURL('/' + nodes.reverse().map(it => it.name).join('/')),
				path: replaceURL('/' + nodes.reverse().map(it => it.path || '').join('/'))
			}
		}
	}
}
provide('findResource', findResource)
const leftNavbars = [
	{ type: 'api', title: $i('api.name'), icon: 'api'},
	{ type: 'function', title: $i('fn.name'), icon: 'function'}
]
const services = {
	api: MagicAPI,
	function: MagicFunction,
	datasource: MagicDatasource
}
constants.PLUGINS.filter(it => it.resource && it.resource.length > 0).map(it => it.resource).forEach(res => res.forEach(it => {
	leftNavbars.push({
		type: it.type,
		icon: it.icon,
		title: $i(it.title),
	})
	if(it.service){
		services[it.type] = it.service
	}
}))
provide('service', services)
leftNavbars.map(it => it.type).forEach(key => resources.value[key] = [])
const rightNavbars = [
	{ type: 'datasource', title: $i('datasource.title'), icon: 'datasource', name: $i('datasource.name')}
]
constants.PLUGINS.filter(it => it.datasources && it.datasources.length > 0).map(it => it.datasources).forEach(res => res.forEach(it => {
	rightNavbars.push({
		type: it.type,
		icon: it.icon,
		title: it.title,
		name: it.name
	})
  if(it.service){
    services[it.type] = it.service
  }
}))
rightNavbars.map(it => it.type).forEach(key => resources.value[key] = [])
provide('resources', () => {
    const navbars = [...leftNavbars, ...rightNavbars]
    const convert = (array) => {
        if(array.length === 1 && array[0].id.endsWith(':0')){
            return array[0].children
        }
        return array
    }
    return navbars.map(navbar => {
        return { key: navbar.type, navbar, tree: convert(JSON.parse(JSON.stringify(resources.value[navbar.type] || []))) }
    })
})
Object.values(services).forEach(service => service.injectResources && service.injectResources(type => resources.value[type]))
nextTick(() => nextRender.value = true)
const processNode = item => {
	return {
		...item.node,
		folder: item.node.parentId !== undefined,
		opened: item.node.parentId !== undefined && constants.DEFAULT_EXPAND === true,
		children: item.children&& item.children.length ? item.children.map(it => processNode(it)) : undefined
	}
}

const loadResource = (type, callback) => {
	setLoadingState(type, true)
	const name = type ? ((leftNavbars.find(it => it.type === type) || rightNavbars.find(it => it.type === type))?.title || '') : $i('message.all')
	bus.status(`message.getResource`, true, name)
	request.send('/' + type + '/resource').success(data => {
		// 只更新指定类型的资源
		resources.value[type] = data[type]?.children?.map(item => processNode(item)) || []
		setLoadingState(type, false)
		bus.status(`message.getResourceFinish`, true, name)
		nextTick(() => callback && callback()) // 确保callback存在时才调用
	})
}

const loadAllResources = (type, callback) => {
	// 如果指定了类型，只设置该类型的加载状态；否则设置所有类型的加载状态
	if (type) {
		setLoadingState(type, true)
	} else {
		[...leftNavbars, ...rightNavbars].forEach(it => setLoadingState(it.type, true))
	}
	const name = type ? ((leftNavbars.find(it => it.type === type) || rightNavbars.find(it => it.type === type))?.title || '') : $i('message.all')
	bus.status(`message.getResource`, true, name)
	request.send('/resource').success(data => {
		[...leftNavbars, ...rightNavbars].filter(it => !type || it.type === type).forEach(it => {
			resources.value[it.type] = data[it.type]?.children?.map(item => processNode(item)) || []
			setLoadingState(it.type, false)
		})
		bus.status(`message.getResourceFinish`, true, name)
		nextTick(() => callback && callback()) // 确保callback存在时才调用
	})
}
bus.$on(Message.RELOAD_RESOURCES, () => {
	// 设置所有资源类型的加载状态
	[...leftNavbars, ...rightNavbars].forEach(it => setLoadingState(it.type, true))
	request.sendGet('/reload').success(() => {
		bus.status('message.reloadResourceSuccess')
		loadAllResources(null, () => bus.$emit(Message.RELOAD_RESOURCES_FINISH))
	}).end(() => {
		// 请求结束后，让loadAllResources函数来控制各类型资源的加载状态
	})
})
bus.$on(Message.RELOAD_RESOURCE, (param) => {
	setLoadingState(param, true)
	request.sendGet('/' + param + '/reloadResource').success(() => {
		bus.status('message.reloadResourceSuccess')
		loadResource(param, () => {})
	}).end(() => {
		// 请求结束后，让loadResource函数来控制该类型资源的加载状态
	})
})

// 初始化所有资源类型的加载状态
initLoadingStates();

// 初始加载时，设置所有资源类型的加载状态
[...leftNavbars, ...rightNavbars].forEach(it => setLoadingState(it.type, true));

// 加载资源函数
const loadResources = type => loadAllResources(type, () => bus.$emit(Message.LOAD_RESOURCES_FINISH, type));

// 监听资源加载和重载事件
bus.$on(Message.LOAD_RESOURCES, loadResources);
bus.$on(Message.RELOAD_RESOURCES_FINISH, loadResources);
//bus.$on(Message.RELOAD_RESOURCE_FINISH, (type) => loadResource(type));

const selectNavbarByItem = itemOrId => {
	let item;
	Object.values(resources.value).some(array => {
		const stack = []
		if(deepFind(array, itemOrId, stack)){
			item = stack[0]
			const index = leftNavbars.findIndex(it => it.type === stack[stack.length - 1].type)
			mnl.value&&mnl.value.select(index) // 选中
			return true
		}
		return false
	})
	return item
}
bus.$on(Message.SELECT_NAVBAR_BY_ITEM, selectNavbarByItem)
bus.$on(Message.OPEN_WITH_ID, id => {
	const item = selectNavbarByItem(id)
	if(item){
		bus.$emit(Message.OPEN_ITEM, item)
	}
})
bus.$on(Message.LOGOUT, () => resources.value = {})
defineExpose({ loadResources })
onMounted(() => emit('onLoad'))
let resource_load_idnex = 0
const resourceOnLoad = () => {
	if(++resource_load_idnex % leftNavbars.length === 0){
		try{
			JSON.parse(store.get(constants.RECENT_OPENED_TAB) || '[]').forEach(it => {
				bus.$emit(Message.OPEN_WITH_ID, it)
			})
		}catch(e){
			console.error(e)
		}
	}
}
bus.$on(Message.REFRESH_RESOURCE, id => {
	const resource = findResource(id)
	resource && request.sendGet(`/resource/file/${id}`).success(data => {
		Object.keys(data).forEach(key => resource.item[key] = data[key])
	})
})
	bus.$on(Message.RELOAD_ITEM, ({type, id}) => {
		const resource = findResource(id)
		resource && request.sendGet(`/${type}/reload/${id}`).success(() => {
			request.sendGet(`/resource/file/${id}`).success(data => {
				Object.keys(data).forEach(key => resource.item[key] = data[key])
			})
		})
	})
</script>
<style scoped>
.magic-main {
  flex: 1;
  display: flex;
  border-bottom: 1px solid var(--main-border-color);
  height: 0;
  width: 100%;
  overflow: hidden; /* 防止主容器出现滚动条 */
}

/*
 * 左侧导航栏容器样式配置
 * 用于设置导航栏的滚动行为和最大高度限制
 */
.left-navbar-container {
  overflow-y: auto;
  max-height: 100%;
}

/* 可选：美化滚动条样式 */
/*
 * Webkit浏览器滚动条宽度设置
 * 控制滚动条的显示宽度为6像素
 */
.left-navbar-container::-webkit-scrollbar {
  width: 6px;
}

/*
 * Webkit浏览器滚动条轨道样式
 * 设置滚动条轨道背景为透明
 */
.left-navbar-container::-webkit-scrollbar-track {
  background: transparent;
}

/*
 * Webkit浏览器滚动条滑块样式
 * 设置滚动条滑块颜色为半透明黑色，圆角为3像素
 */
.left-navbar-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

/*
 * 右侧导航栏容器样式配置
 * 设置容器的滚动行为和最大高度限制
 */
.right-navbar-container {
  overflow-y: auto;
  max-height: 100%;
}

/* 可选：美化滚动条样式 */
/*
 * 自定义Webkit内核浏览器的滚动条宽度
 * 控制滚动条的显示宽度为6像素
 */
.right-navbar-container::-webkit-scrollbar {
  width: 6px;
}

/*
 * 自定义滚动条轨道样式
 * 设置滚动条轨道背景为透明
 */
.right-navbar-container::-webkit-scrollbar-track {
  background: transparent;
}

/*
 * 自定义滚动条滑块样式
 * 设置滚动条滑块颜色为半透明黑色，圆角为3像素
 */
.right-navbar-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}


.magic-main-body-wrapper{
	display:flex;
	flex:1;
	flex-direction: column;
}
.magic-main-body{
	display:flex;
	flex:1;
	overflow: hidden;
}
.magic-toolbar :deep(.magic-resizer-y .magic-resizer-event){
	top: auto;
}
</style>
