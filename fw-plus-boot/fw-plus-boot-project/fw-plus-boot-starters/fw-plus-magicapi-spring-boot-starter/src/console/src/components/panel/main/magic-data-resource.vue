<template>
	<div class="magic-data-resource">
		<div class="magic-data-resource-header">
			<ul>
				<template v-for="(button, index) in buttons" :key="index" >
					<li v-if="!button.show || button.show()" :title="button.name || ''" @click="button.onClick&&button.onClick()" :class="{ separator: button.separator}">
						<magic-icon v-if="!button.separator" :icon="button.icon"/>
					</li>
				</template>
			</ul>
			<magic-input v-model:value="keyword" :placeholder="$i('message.searchText')" width="100%"/>
			<magic-icon icon="search" size="14px"/>
		</div>
		<magic-empty v-if="datasources.length === 0" :text="$i('message.empty', title)"/>
		<ul v-else>
			<li v-for="(item, key) in datasources" :key="key" @contextmenu.prevent="e => onContextMenu(item, e)">
				<magic-icon icon="datasource" />
				<label>{{item.name || $i('datasource.primary')}}</label>
				<span>({{item.key || 'default'}})</span>
				<magic-icon v-if="item.lock === '1'" icon="lock" />
			</li>
		</ul>
	</div>
	<magic-dialog v-model:value="dataResourceDialogVisible" :title="dataResourceDialogTitle" width="550px">
		<magic-loading :loading="loading" style="min-height: 200px;">
			<component :is="componentForm" :info="dataResourceObj"/>
			<magic-button-group align="right" style="padding: 5px 0">
				<magic-button :value="saveButtonText" type="active" @onClick="doSave()"/>
				<magic-button :value="$i('datasource.test')" @onClick="doTest()"/>
				<magic-button :value="$i('message.cancel')" @onClick="dataResourceDialogVisible = false"/>
			</magic-button-group>
		</magic-loading>
	</magic-dialog>
</template>
<script setup>
import { ref, resolveDynamicComponent, getCurrentInstance, computed, inject } from 'vue'
import request from '../../../scripts/request.js'
import bus from '../../../scripts/bus.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import $i from '../../../scripts/i18n.js'
const props = defineProps({
	/**
	 * 类型，datasource、redis、mongo
	 */
	type: String,
	/**
	 * 名称，Datasource，Redis，Mongo
	 */
	title: String,
	data: Array
})
const componentForm = resolveDynamicComponent(`magic-datasource-${props.type}`)
const dataResourceDialogVisible = ref(false)
const dataResourceDialogTitle = ref('')
const dataResourceObj = ref({})
const keyword = ref('')
const saveButtonText = ref('')
const loading = ref(true)
const datasources = computed(() => {
	return props.data && props.data.length > 0 && props.data[0].children && props.data[0].children.filter(it => it.name.indexOf(keyword.value) > -1 || it.key.indexOf(keyword.value) > -1) || []
})
const service = inject('service')[props.type]
// 搜索条的按钮
const buttons = [
	{
		name: $i('message.createDataSource', props.title),
		icon: 'plus',
		onClick:() => {
			dataResourceDialogTitle.value = $i('message.createDataSource', props.title)
			saveButtonText.value = $i('message.create')
			dataResourceObj.value = {}
			dataResourceDialogVisible.value = true
			loading.value = false
		}
	},
	{ name: $i('message.refresh'), icon: 'refresh', onClick: () => {
			bus.$emit(Message.RELOAD_RESOURCE, props.type)
		} },
]
const doTest = () => {
	service.doTest(dataResourceObj.value)
}
const { proxy } = getCurrentInstance()
const doSaveObj = (saveObj, msgId, msg) => {
	saveObj.groupId = `${props.type}:0`
	request.sendJson(`/resource/file/${props.type}/save`, saveObj).success((id) => {
		if(id) {
			if(saveObj.id !== id){
				bus.loading(3)
			}
			saveObj.id = id
			bus.status(msgId + 'Success',true, msg)
			props.data[0].children = props.data[0].children || []
			const target = props.data[0].children.find(it => it.id === saveObj.id)
			if(target) {
				Object.keys(saveObj).forEach(key => target[key] = saveObj[key])
			}else{
				props.data[0].children.push(saveObj)
			}
			dataResourceDialogVisible.value = false
		}else{
			bus.status(msgId + 'Failed', false, msg)
			proxy.$alert($i(msgId + 'Failed', msg))
		}
	})
}
const doSave = () => {
	const saveObj = { ...dataResourceObj.value }
	doSaveObj(saveObj, 'message.save',`${props.title}「${getFullPath(saveObj)}」`)
}
const getFullPath = item => `${item.name}(${item.key})`
const deleteNode = item => {
	const index = props.data[0].children.findIndex(it => it === item)
	if(index > -1) {
		props.data[0].children.splice(index, 1)
	}
}
const onContextMenu = (item, event) => {
	const menus = []
	if(item.id){
		[{
			label: $i('message.update'),
			icon: 'update',
			divided: true,
			onClick: () => {
				loading.value = true
				dataResourceDialogTitle.value = $i('message.updateTips', props.title)
				saveButtonText.value = $i('message.update')
				dataResourceDialogVisible.value = true
				bus.status('message.getDetail', `${props.title}「${getFullPath(item)}」`)
				request.sendGet(`/resource/file/${item.id}`).success(data => dataResourceObj.value = data).end(() => {
					loading.value = false
				})
			}
		},{
			label: $i('resource.contextmenu.delete'),
			icon: 'delete',
			onClick: () => {
				const msg = `${props.title}「${getFullPath(item)}」`
				proxy.$confirm($i('message.deleteTips', props.title), $i('message.deleteConfirm', msg), () => {
					request.send('/resource/delete', { id: item.id }).success(ret => {
						bus.status(ret ? 'message.deleteSuccess': 'message.deleteFailed', ret, msg)
						if(!ret) {
							proxy.$alert(ret ? 'message.deleteSuccess': 'message.deleteFailed', msg)
						}else{
							deleteNode(item)
						}
					})
				})
			}
		}, {
			label: $i('message.copy'),
			icon: 'copy',
			divided: true,
			onClick: () => {
				request.sendGet(`/resource/file/${item.id}`).success(res => {
					res.id = undefined
					res.name = res.name + `(${$i('message.copy')})`
					res.key = res.key + '_copy'
					doSaveObj(res, 'datasource.copy',`${props.title}「${getFullPath(res)}」`)
				})
			}
		}].forEach(m => menus.push(m))
		if(item.lock === constants.LOCKED){
			menus.push({
				label: $i('resource.contextmenu.unlock'),
				icon: 'unlock',
				onClick: () => request.sendPost('/resource/unlock', { id: item.id}).success(ret => {
					bus.status(ret ? 'message.unlockSuccess': 'message.unlockFailed', ret, `${props.title}「${getFullPath(item)}」`)
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
					bus.status(ret ? 'message.lockSuccess': 'message.lockFailed', ret, `${props.title}「${getFullPath(item)}」`)
					if(ret) {
						item.lock = constants.LOCKED
					}
				})
			})
		}
	}
	constants.PLUGINS.forEach(it => {
		if(it.contextmenu && typeof it.contextmenu === 'function'){
			const pMenus = it.contextmenu({
				...item,
				menuType: 'datasource'
			})
			pMenus && pMenus.length && pMenus.forEach(m => menus.push(m))
		}
	})
	menus.length && proxy.$contextmenu({ menus, event})
}
</script>
<style scoped>
.magic-data-resource{
	display: flex;
	flex-direction: column;
	flex: 1;
	width: 100%;
}
.magic-data-resource .magic-data-resource-header{
	height: 24px;
	line-height: 24px;
	display: flex;
	background-color: var(--main-background-color);
	border-bottom: 1px solid var(--main-border-color);
	padding-left: 3px;
}
.magic-data-resource .magic-data-resource-header input{
	flex: 1;
	border:none;
	background: none;
	height: 24px;
	line-height: 24px;
}
.magic-data-resource .magic-data-resource-header ul {
	display: inline-flex;
	align-items: center;

}
.magic-data-resource .magic-data-resource-header ul li{
	display: inline-block;
	cursor: pointer;
	padding:0 3px;
	height: 20px;
	line-height: 20px;
	border-radius: 3px;
}
.magic-data-resource .magic-data-resource-header ul li.separator{
	background-color: var(--main-border-color);
	width:1px;
	border-radius: 0;
	padding:0;
	margin:0 4px;
}
.magic-data-resource .magic-data-resource-header ul li:hover{
	background: var(--main-hover-icon-background-color);
}
.magic-data-resource .magic-data-resource-header svg{
	height: 100%;
}
.magic-data-resource > ul{
	height: 100%;
	overflow: auto;
}
.magic-data-resource > ul li{
	line-height: 22px;
	padding-left: 10px;
	white-space: nowrap;
}
.magic-data-resource > ul li:hover{
	background-color: var(--tree-hover-background-color);
}
.magic-data-resource > ul li .magic-icon{
	padding-right: 2px;
}
.magic-data-resource > ul li label{
	color: var(--resource-label-color);
}
.magic-data-resource > ul li span{
	color: var(--resource-span-color);
	display: inline-block;
	height: 22px;
	line-height: 22px;
}
.magic-data-resource > ul li :deep(.magic-icon-datasource){
	fill: #089910
}
</style>
<style>
.magic-editor .magic-form-row{
	display: flex;
	margin-bottom: 5px;
}
.magic-editor .magic-form-row label{
	margin-right: 5px;
	display: inline-block;
	width: 70px;
	text-align: right;
	height: var(--magic-input-height);
	line-height: var(--magic-input-height);
}
.magic-editor .magic-form-row > input,
.magic-editor .magic-form-row > .magic-select{
	flex: 1;
	width: auto;
}
</style>
