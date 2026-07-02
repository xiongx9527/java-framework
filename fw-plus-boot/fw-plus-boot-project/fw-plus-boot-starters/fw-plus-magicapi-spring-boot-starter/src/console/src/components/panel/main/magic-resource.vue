<template>
	<div class="magic-resource" @contextmenu.prevent="e => displayBlankContextMenu(e)">
		<div class="magic-resource-header">
			<magic-icon icon="search" size="14px"/>
			<magic-input v-model:value="keyword" :placeholder="$i('message.searchText')" width="100%"/>
			<ul>
				<template v-for="(button, index) in buttons" :key="index" >
					<li v-if="!button.show || button.show()" :data-title="button.name || ''" :data-tooltip-direction="tooltipDirection"
					@click="button.onClick&&button.onClick()" :class="{ separator: button.separator}">
						<magic-icon v-if="!button.separator" :icon="button.icon"/>
					</li>
				</template>
			</ul>
		</div>
		<magic-empty v-if="!data || data.length === 0" :text="$i('message.empty', title)"/>
		<!-- 树形菜单 -->
		<magic-tree ref="treeObj" v-else :data="tree" @itemClick="onItemClick" @contextmenu="onContextMenu" :draggable="true" :sort="true" :descending="descending" :onMove="onMove" :filter="keyword" :filter-text="filterText" :selected="selectedItem">
			<template v-slot:folder="{ item }">
				<magic-icon :icon="item.opened ? 'arrow-bottom': 'arrow-right'" @click.stop="item.opened = !item.opened"/>
				<magic-icon icon="list" />
				<label>{{ item.name }}</label>
				<span v-if="requirePath && item.path">({{ item.path }})</span>
			</template>
			<template v-slot:file="{ item }">
				<magic-text-icon :icon="getIcon(item)"/>
				<label>{{ item.name }}</label>
				<span v-if="requirePath && item.path">({{ item.path }})</span>
				<magic-icon v-if="item.lock === '1'" icon="lock" />
				<magic-avatar-group :users="activateUserFiles[item.id] || []" :max="3" :size="20"/>
			</template>
		</magic-tree>
		<!-- 创建&修改对话框 -->
		<magic-dialog :title="modeText" v-model:value="showGroupDialog" width="350px">
			<!-- 表单项 -->
			<ul class="magic-create-group">
				<li><label>{{ $i('resource.form.groupName') }}：</label><magic-input v-model:value="groupObj.name" :placeholder="$i('resource.form.placeholder.name', title)"/></li>
				<li v-if="requirePath"><label>{{ $i('resource.form.groupPath') }}：</label><magic-input v-model:value="groupObj.path" :placeholder="$i('resource.form.placeholder.path', title)"/></li>
			</ul>
			<magic-button-group align="right" style="padding: 5px 0">
				<magic-button :value="modeText" type="active" @onClick="saveGroup()"/>
				<magic-button :value="$i('message.cancel')" @onClick="showGroupDialog = false"/>
			</magic-button-group>
		</magic-dialog>
		<!-- 复制分组对话框 -->
		<magic-dialog v-model:value="showCopyGroup" :title="$i('resource.copyGroup')" :shade="false" padding="0" width="400px" overflow="hidden">
			<magic-resource-choose ref="chooseGroup" v-model:value="chooseGroupItem" :file="false" :type="type" :single="true" />
			<magic-button-group align="right" style="margin-right:5px;margin-bottom:5px;">
				<magic-button :value="$i('message.expand')" @onClick="$refs.chooseGroup.expand(true)"></magic-button>
				<magic-button :value="$i('message.collapse')" @onClick="$refs.chooseGroup.expand(false)"></magic-button>
				<magic-button type="active" :value="$i('message.copy')" @onClick="doCopyGroup"></magic-button>
			</magic-button-group>
		</magic-dialog>
	</div>
</template>
<script setup>
import { ref, toRaw, computed, getCurrentInstance, reactive, inject, nextTick, onMounted, resolveDynamicComponent, shallowRef } from 'vue'
import bus from '../../../scripts/bus.js'
import constants from '../../../scripts/constants.js'
import request from '../../../scripts/request.js'
import $i from '../../../scripts/i18n.js'
import Message from '../../../scripts/constants/message.js'
import { replaceURL, processTree, download, copyToClipboard } from '../../../scripts/utils.js'
const magicBackupFileComponent = resolveDynamicComponent('magic-backup-file')
const props = defineProps({
	/**
	 * 类型，api、function、resource、cron、websocket
	 */
	type: String,
	/**
	 * 名称，接口、函数、资源、定时任务、WebSocket
	 */
	title: String,
	data: Array,
	tooltipDirection: String
})
const config = inject('service')[props.type]
const requirePath = config.requirePath
const getIcon = config.getIcon
// 搜索关键字
const keyword = ref('')
// 是否是创建分组
const mode = ref(true)
// 是否显示复制分组对话框
const showCopyGroup = ref(false)
// 当前分组ID
const srcGroupId = ref('')
// 选择分组对话框中的分组
const chooseGroupItem = ref(null)
const activateUserFiles = inject('activateUserFiles')
const modeText = computed(() => mode.value ? $i('resource.createGroup') : $i('resource.updateGroup'))
// 排序
const descending = ref(true)
// 当前打开的
const selectedItem = ref({})
const treeObj = ref({})
// 是否显示创建分组的dialog
const showGroupDialog = ref(false)
const groupObj = ref({
	type: props.type
})
const tree = computed(() => props.data)
const emits = defineEmits(['close', 'onLoad'])
// 搜索条的按钮
const buttons = ref([
	{
		name: $i('resource.createGroup'),
		icon: 'group-add',
		onClick:() => {
			groupObj.value = {
				type: props.type,
				parentId: '0'
			}
			mode.value = true
			showGroupDialog.value = true
		}
	},
	{ name: $i('resource.header.expand'), icon: 'expand-all', onClick: () => processTree(tree.value, it => it.opened = true) },
	{ name: $i('resource.header.collapse'), icon: 'collapse-all', onClick: () => processTree(tree.value, it => it.opened = false) },
	{ name: $i('resource.header.desc'), icon: 'descending',  show: () => descending.value, onClick: () => descending.value = false },
	{ name: $i('resource.header.asc'), icon: 'ascending',  show: () => !descending.value, onClick: () => descending.value = true },
	{ separator: true },
	{ name: $i('message.refresh'), icon: 'refresh', onClick:() => {
			bus.$emit(Message.RELOAD_RESOURCE, props.type)
		} },
	{ name: $i('resource.header.position'), icon: 'position', onClick: () => {
		if(treeObj.value && selectedItem.value){
			bus.$emit(Message.SELECT_NAVBAR_BY_ITEM, selectedItem.value)
			treeObj.value.scrollIntoView(selectedItem.value)
		}
	} },
	{ name: $i('message.hide'), icon: 'minimize', onClick:() => emits('close')},

])
const deepFind = (itemOrId, array, nameStack, pathStack, folderStack) => {
	folderStack = folderStack || []
	array = array || []
	let find = array.find(it => it === itemOrId || it.id === itemOrId)
	if(find){
		nameStack.push(find.name)
		folderStack.push(find)
		config.requirePath && find.path && pathStack.push(find.path)
		return find;
	}else {
		for(let i=0, len = array.length; i< len; i++){
			const it = array[i]
			if(it.folder && it.children && (find = deepFind(itemOrId, it.children, nameStack, pathStack))){
				nameStack.unshift(it.name)
				folderStack.unshift(it)
				config.requirePath && it.path && pathStack.unshift(it.path)
				return find
			}
		}
	}
}
const getFullPath = (item, onlyPath) => {
	const nameStack = []
	const pathStack = []
	deepFind(toRaw(item), toRaw(tree.value), nameStack, pathStack)
	const path = pathStack.length > 0 ?  replaceURL(`/${pathStack.join('/')}`) : ''
	return onlyPath ? path : `/${nameStack.join('/')}${config.requirePath ?  `(${path})` : ''}`
}
const filterText = (item) => getFullPath(item)
const cachedItems = []
const onItemClick = (item, force) => {
	if(item) {
		if(item.folder){
			bus.$emit(Message.OPEN_GROUP, item)
		}else {
			let opened = cachedItems.find(it => it.item == item)
			if(!opened){
				opened = reactive({
					type: props.type,
					title: props.title,
					language: config.language || 'magicscript',
					pageType: config.pageType,
					component: config.component,
					item: config.merge(item),
					path: () => getFullPath(item),
					requestPath: () => {
						const pathStack = []
						deepFind(toRaw(item), toRaw(tree.value), [], pathStack)
						return replaceURL(`/${pathStack.join('/')}`)
					},
					getGroups: () => {
						const groupStack = []
						deepFind(toRaw(item), toRaw(tree.value), [], [], groupStack)
						return groupStack
					},
					getIcon: config.getIcon,
					runnable: config.runnable,
					doTest: config.doTest,
					processSave: config.processSave || (e => e)
				})
				cachedItems.push(opened)
			}
			bus.$emit(Message.OPEN, opened, force)
		}
	}
}
bus.$on(Message.OPEN, opened => selectedItem.value = opened.item)
bus.$on(Message.OPEN_EMPTY, () => selectedItem.value = null)
const { proxy } = getCurrentInstance()
const saveGroup = () => {
	const group = {...groupObj.value}
	delete group.children
	delete group.opened
	delete group.folder
	request.sendJson('/resource/folder/save', group).success(id => {
		if(id){
			const newGroup = {...toRaw(groupObj.value), folder: true, id}
			newGroup.options = newGroup.options || []
			newGroup.paths = newGroup.paths || []
			updateNode(newGroup)
			bus.status('resource.saveGroupSuccess', true, props.title, getFullPath(id))
			showGroupDialog.value = false
			bus.loading(2)
		}else{
			const path = getFullPath(groupObj.value)
			bus.status('resource.saveGroupFailed', false, props.title, path)
			proxy.$alert($i('resource.saveGroupFailed', props.title, path))
		}
	})
}
const onMove = (src, target) => new Promise(reslove => {
	proxy.$confirm($i('message.move'), $i('resource.move', getFullPath(src)), ()=> {
		request.send('/resource/move', { src: src.id, groupId: target.groupId || target.id }).success(r => {
			const msgId = src.folder ? 'resource.moveGroup' : 'resource.moveResource'
			const path = getFullPath(src)
			if(r){
				if(src.folder){
					bus.status(msgId + 'Success', true, props.title, path)
				} else {
					bus.status(msgId + 'Success', true, path)
				}
				src[src.folder ? 'parentId' : 'groupId'] = target.groupId || target.id

			}else{
				if(src.folder){
					bus.status(msgId + 'Failed', false, props.title, path)
					proxy.$alert($i(msgId + 'Failed', props.title, path))
				} else {
					bus.status(msgId + 'Failed', false, path)
					proxy.$alert($i(msgId + 'Failed', path))
				}
			}
			reslove(r)
		})
	})
})
const deleteNode = (node, data) => {
	data = data || tree.value
	const index = data.findIndex(it => it === node || it.id === node.id)
	if(index > -1) {
		data.splice(index, 1)
	} else {
		data.forEach(it => deleteNode(node, it.children || []))
	}
}
const updateNode = (node, data) => {
	data = data || tree.value || []
	const find = data.find(it =>it.id === node.id || node.groupId === it.id)
	if (find) {
		find.children = find.children || []
		find.opened = true  // 展开
		if(find.id === node.id){
			find.name = node.name
			find.path = node.path
			find.parentId = node.parentId
		}else{
			find.children.push(node)
		}
		return true
	} else if(node.parentId === '0'){
		data.push(node)
		return true
	} else {
		if(data.some(it => updateNode(node, it.children || []))){
			return true
		}
		const parentNode = node.parentId !== undefined && data.find(it => it.id === node.parentId)
		if(parentNode){
			parentNode.children = parentNode.children || []
			parentNode.children.push(node)
			return true
		}
	}
	return false
}
bus.$on(Message.OPEN_ITEM, item => {
	const array = toRaw(tree.value)
	if(array){
		onItemClick(deepFind(toRaw(item), array, [], []), true)
	}
})
const onContextMenu = (item, event) => {
	if(item && event) {
		const menus = []
		if(item.folder) {
			menus.push.apply(menus, [{
				label: $i('resource.contextmenu.newFile', props.title),
				icon: 'plus',
				onClick(){
					const info = {
						groupId: item.id,
						name: $i('message.untitled'),
						script: config.defaultScript || `return 'Hello magic-api'`,
						path: config.requirePath ? '' : undefined
					}
					updateNode(info)
					onItemClick(info, true)
				}
			},{
				label: $i('resource.createGroup'),
				icon: 'group-add',
				onClick(){
					mode.value = true
					groupObj.value = {
						parentId: item.id,
						type: props.type
					}
					showGroupDialog.value = true
				}
			},{
				label: $i('resource.updateGroup'),
				icon: 'update',
				onClick(){
					mode.value = false
					groupObj.value = {
						...item
					}
					showGroupDialog.value = true
				}
			},{
				label: $i('resource.copyGroup'),
				icon: 'copy',
				onClick(){
					srcGroupId.value = item.id
					showCopyGroup.value = true
				}
			},{
				label: $i('resource.contextmenu.deleteGroup'),
				icon: 'delete',
				onClick() {
					proxy.$confirm($i('resource.contextmenu.deleteGroup'), $i('resource.deleteGroupConfirm', props.title, getFullPath(item)), ()=> {
						if(item.id) {
							request.send('/resource/delete', { id: item.id }).success(ret => {
								if(!ret) {
									proxy.$alert('resource.deleteGroupFailed', props.title, getFullPath(item))
									bus.status('resource.deleteGroupFailed', false, props.title, getFullPath(item))
								}else{
									bus.status('resource.deleteGroupSuccess', true, props.title, getFullPath(item))
									deleteNode(item)
								}
							})
						}else{
							deleteNode(item)
						}
					})
				}
			},{
				label: $i('resource.contextmenu.exportGroup'),
				icon: 'download',
				onClick() {
					request.send(`/download?groupId=${item.id}`, null, { headers: { 'Content-Type': 'application/json' }, responseType: 'blob' }).success(blob => {
						download(blob, `${item.name}.zip`)
						bus.status('resource.groupExport', true, item.name, props.title)
					})
				}
			}])
			if(item.parentId !== '0'){
				menus.push({
					label: $i('resource.contextmenu.moveToRoot'),
					icon: 'move',
					onClick() {
						proxy.$confirm($i('resource.moveGroup'), $i('resource.moveRootGroupConfirm', getFullPath(item)), ()=> {
							request.send('/resource/move', { src: item.id, groupId: '0' }).success(ret => {
								if(!ret) {
									proxy.$alert($i('resource.moveRootFailed', props.title, getFullPath(item)))
									bus.status('resource.moveRootFailed', false, props.title, getFullPath(item))
								}else{
									bus.status('resource.moveRootSuccess', true, props.title, getFullPath(item))
									item.parentId = '0'
									deleteNode(item)
									updateNode(item)
								}
							})
						})
					}
				})
			}
		} else {
			menus.push.apply(menus, [{
				label: $i('resource.contextmenu.copy', props.title),
				icon: 'copy',
				divided: true,
				onClick: () => {
					request.sendGet(`/resource/file/${item.id}`).success(res => {
						res.id = null;
						updateNode(res)
						onItemClick(res, true)
						res.name = res.name + `(${$i('message.copy')})`
						if(config.requirePath){
							res.path = res.path + '_copy'
						}
					})
				}
			}])
			if(config.requirePath){
				if(props.type === 'api'){
					menus.push({
						label: $i('resource.contextmenu.copyWithPath'),
						icon: 'copy',
						onClick: () => {
							let path = getFullPath(item, true)
							if(path){
								path = replaceURL(constants.SERVER_URL + '/' + path)
								if(copyToClipboard(path)){
									bus.status('resource.copyPathSuccess', true, props.title, path)
								}else{
									bus.status('resource.copyPathFailed', false, props.title, path)
								}
							}
						}
					})
				}
				menus.push.apply(menus, [{
					label: $i('resource.contextmenu.copyRelativePath'),
					icon: 'copy',
					divided: true,
					onClick: () => {
						const path = getFullPath(item, true)
						if(path){
							if(copyToClipboard(path)){
								bus.status('resource.copyRelativePathSuccess', true, props.title, path)
							}else{
								bus.status('resource.copyRelativePathFailed', false, props.title, path)
							}
						}
					}
				}])
				if(item.lock === constants.LOCKED){
					menus.push({
						label: $i('resource.contextmenu.unlock'),
						icon: 'unlock',
						onClick: () => request.sendPost('/resource/unlock', { id: item.id}).success(ret => {
							bus.status(ret ? 'message.unlockSuccess' : 'message.unlockFailed', ret, getFullPath(item))
							if(ret) {
								item.lock = constants.UNLOCK
							}
						})
					})
				} else {
					menus.push({
						label: $i('resource.contextmenu.lock'),
						icon: 'lock',
						onClick: () => request.sendPost('/resource/lock', { id: item.id}).success(ret => {
							bus.status(ret ? 'message.lockSuccess' : 'message.lockFailed', ret, getFullPath(item))
							if(ret) {
								item.lock = constants.LOCKED
							}
						})
					})
				}
			}
			if(item.id) {
				menus.push({
					label: $i('toolbars.history'),
					icon: 'history',
					onClick: () => {
						bus.$emit(Message.ADD_FOOTER_TOOLBAR, {
							component: shallowRef(magicBackupFileComponent),
							id: 'backup-file',
							icon: 'history',
							title: $i('toolbars.viewHistory', getFullPath(item)),
							allowClose: true,
							data: {
								id: item.id
							}
						})
					}
				})
			}
			menus.push.apply(menus, [{
				label: $i('message.refresh'),
				icon: 'refresh',
				onClick: () => {
					bus.$emit(Message.REFRESH_RESOURCE, item.id)
				}
			},{
				label: $i('resource.contextmenu.delete'),
				icon: 'delete',
				onClick: () => {
					const msg = `${props.title}「${getFullPath(item)}」`
					proxy.$confirm($i('message.deleteTips', props.title), $i('message.deleteConfirm', msg), ()=> {
						if(item.id){
							request.send('/resource/delete', { id: item.id }).success(ret => {
								if(!ret) {
									bus.status('message.deleteFailed', false, msg)
									proxy.$alert($i('message.deleteFailed', msg))
								}else{
									bus.status('message.deleteSuccess', true, msg)
									deleteNode(item)
									bus.$emit(Message.DELETE_FILE, item)
								}
							})
						}else{
							bus.status('message.deleteSuccess', true, msg)
							deleteNode(item)
							bus.$emit(Message.DELETE_FILE, item)
						}
					})
				}
			}])
		}
		constants.PLUGINS.forEach(it => {
			if(it.contextmenu && typeof it.contextmenu === 'function'){
				const pMenus = it.contextmenu({
					...item,
					menuType: 'resource'
				})
				pMenus && pMenus.length && pMenus.forEach(m => menus.push(m))
			}
		})
		proxy.$contextmenu({ menus, event })
	}
}

const displayBlankContextMenu = event => {
	proxy.$contextmenu({
		event,
		menus: buttons.value.slice(0, 5).filter(it => !it.show || it.show()).map(it => {
			return {
				icon: it.icon,
				label: it.name,
				onClick: it.onClick
			}
		})
	})
}
let selectedNewId = null
const doCopyGroup = () => {
	if(chooseGroupItem.value){
		request.sendPost('/resource/folder/copy', { src: srcGroupId.value, target: chooseGroupItem.value.id.endsWith('-root') ? '0' : chooseGroupItem.value.id}).success(newId => {
			selectedNewId = newId
			showCopyGroup.value = false
			bus.$emit(Message.LOAD_RESOURCES)
		})
	}
}
const findResource = inject('findResource')
bus.$on(Message.LOAD_RESOURCES_FINISH, () => {
	if(selectedNewId){
		nextTick(() => {
			const find = findResource(selectedNewId)
			bus.$emit(Message.SELECT_NAVBAR_BY_ITEM, find.item)
			bus.$emit(Message.OPEN_GROUP, find.item)
		})
	}
})
onMounted(() => emits('onLoad'))
</script>
<style scoped>
.magic-resource{
	display: flex;
	flex-direction: column;
	flex: 1;
	width: 100%;
}
.magic-resource .magic-resource-header{
	height: 24px;
	line-height: 24px;
	display: flex;
	background-color: var(--main-background-color);
	border-bottom: 1px solid var(--main-border-color);
}
.magic-resource .magic-resource-header input{
	flex: 1;
	border:none;
	background: none;
	height: 24px;
	line-height: 24px;
}
.magic-resource .magic-resource-header ul {
	display: var(--magic-resource-header-icon-display);
	align-items: center;

}
.magic-resource:hover .magic-resource-header ul{
	display: inline-flex;
}
.magic-resource .magic-resource-header ul li{
	display: inline-block;
	cursor: pointer;
	padding:0 3px;
	height: 20px;
	line-height: 20px;
	border-radius: 3px;
}
.magic-resource .magic-resource-header svg{
	height: 100%;
}
.magic-resource .magic-resource-header ul li.separator{
	background-color: var(--main-border-color);
	width:1px;
	border-radius: 0;
	padding:0;
	margin:0 4px;
}
.magic-resource .magic-resource-header ul li:hover{
	background: var(--main-hover-icon-background-color);
}

.magic-resource .magic-tree label{
	color: var(--resource-label-color)
}
.magic-resource .magic-tree span{
	color: var(--resource-span-color)
}
.magic-resource .magic-tree .magic-icon-lock{
	fill: var(--resource-label-color);
    margin-left: 5px;
}
.magic-resource > .magic-tree {
	flex: 1;
	overflow: auto;
}
.magic-create-group li{
	display: flex;
	height: 26px;
	line-height: 26px;
}
.magic-create-group li:not(:first-child){
	margin-top: 4px;
}	
.magic-create-group li input{
	flex: 1
}
.magic-resource :deep(.magic-avatar-group){
	margin-left: 10px;
}
</style>
