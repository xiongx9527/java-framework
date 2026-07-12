<template>
	<div class="magic-todo">
		<magic-panel-toolbar :toolbars="toolbars"/>
		<div>
			<magic-loading :loading="loading"></magic-loading>
			<magic-tree v-if="tree.length > 0" :data="tree" @itemClick="onItemClick">
				<template v-slot:folder="{ item }">
					<magic-icon :icon="item.opened ? 'arrow-bottom': 'arrow-right'" @click.stop="item.opened = !item.opened"/>
					<magic-icon v-if="item.root" :icon="item.icon"></magic-icon>
					<magic-text-icon v-else-if="item.icon" :icon="item.icon"/>
					<magic-icon v-else icon="list"></magic-icon>
					<label>{{ item.name }}</label>
					<span v-if="item.path">({{item.path}})</span>
				</template>
				<template v-slot:file="{ item }">
					<magic-icon></magic-icon>
					<magic-icon icon="todo"></magic-icon>
					<label class="todo">{{ item.text }}</label>
				</template>
			</magic-tree>
			<magic-empty v-else :text="$i('message.empty', 'TODO')"/>
		</div>
	</div>
</template>
<script setup>
import { inject, onMounted, ref } from 'vue'
import bus from '../../../scripts/bus.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import request from '../../../scripts/request.js'
import $i from '../../../scripts/i18n.js'
import { processTree } from '../../../scripts/utils.js'
const loading = ref(true)
const toolbars = [{
	icon: 'refresh',
	title: $i('message.refresh'),
	onClick() {
		load()
	}
}, {
	icon: 'expand-all',
	title: $i('message.expand'),
	onClick: () => processTree(tree.value, it => it.opened = true)
}, {
	icon: 'collapse-all',
	title: $i('message.collapse'),
	onClick: () => processTree(tree.value, it => it.opened = false)
}]
const tree = ref([])
const resources = inject('resources')
const navbars = {}
const onItemClick = item => {
	if(item.groupId || item.line){
		bus.$emit(Message.OPEN_WITH_ID, item.id)
	}
}
const deepFind = (array, id) => {
	let find = array.find(it => it.id === id)
	if(!find){
		for (let index = 0; index < array.length; index++) {
			const item = array[index];
			if(item.folder && (find = deepFind(item.children || [], id))){
				find._type = item.type
				return find
			}
		}
	}
	return find
}
const filterTree = array => {
	let result = []
	array.forEach(item => {
		if(item.line > 0){
			result.push(item)
		} else if(item.folder && item.children) {
			item.children = filterTree(item.children)
			if(item.children.length){
				result.push(item)
			}
		}
	})
	return result
}
const service = inject('service')
const load = () => {
	tree.value = []
	loading.value = true
	request.sendGet('/todo').success(res => {
		const all = resources()
		all.forEach(it=> navbars[it.navbar.type] = it.navbar)
		let hasData = false
		res.forEach(it => {
			for (let index = 0; index < all.length; index++) {
				const elements = all[index];
				const item = deepFind(elements.tree, it.id)
				if(item) {
					item.folder = true
					item.children = item.children || []
					item.icon = service[item._type].getIcon(item)
					item.children.push({ ...it })
					elements.display = true
					hasData = true
					break;
				}
			}
		})
		if(hasData){
			tree.value = filterTree(all.filter(it => it.display).map(it => {
				return {
					folder: true,
					icon: it.navbar.icon,
					name: it.navbar.title,
					children: it.tree,
					root: true
				}
			}))
			processTree(tree.value, it => it.opened = true)
		}
		loading.value = false
	})
}
let inited = false
bus.$on(Message.LOAD_RESOURCES_FINISH, load)
onMounted(() => {
	if(!inited && constants.LOGINED){
		load()
	}
	inited = true
})
</script>
<style scoped>
.magic-todo{
	display: flex;
	flex: 1;
}
.magic-todo div{
	background-color: var(--navbar-body-background-color);
	flex: 1;
	overflow: hidden;
	position: relative;
}
.magic-todo div :deep(.magic-tree){
	width: 100%;
	height: 100%;
	position: absolute;
	overflow: auto;
}
.magic-todo div :deep(.magic-tree label){
	color: var(--resource-label-color);
}
.magic-todo div :deep(.magic-tree label.todo){
	font-style: italic;
	color: var(--todo-color);
}
.magic-todo div :deep(.magic-tree span){
	color: var(--resource-span-color);
}
</style>