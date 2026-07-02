<template>
	<div class="magic-resource-choose">
		<magic-tree :data="tree" :sort="false" @itemClick="onItemClick">
			<template v-slot:folder="{ item }">
				<magic-checkbox v-model:value="item.selected" :checked-half="item.checkedHalf" @change="doSelected(item)"/>
				<magic-icon  :icon="item.opened ? 'arrow-bottom': 'arrow-right'" @click.stop="item.opened = !item.opened"/>
				<magic-icon :icon="item.icon" :class="item.iconClass"/>
				<label>{{ item.name }}</label>
				<span v-if="item.path">({{item.path}})</span>
			</template>
			<template v-slot:file="{ item }">
				<magic-checkbox v-model:value="item.selected" :checked-half="item.checkedHalf" @change="doSelected(item)"/>
				<magic-text-icon v-if="item.textIcon" :icon="item.icon"/>
				<magic-icon v-else :icon="item.icon"/>
				<label>{{ item.name }}</label>
				<span v-if="item.path">({{item.path}})</span>
			</template>
		</magic-tree>
	</div>
</template>
<script setup>
import { inject, ref } from 'vue'
import { processTree } from '../../../scripts/utils'
const props = defineProps({
    value: [Array, Object],
	/* api、function、null*/
	type: String,
	single: {
		type: Boolean,
		default: () => false
	},
	file: {
		type: Boolean,
		default: () => true
	}
})
const resources = inject('resources')
const service = inject('service')
const tree = ref([])
const emit = defineEmits(['update:value'])
const updateSelected = () => {
	let array = []
	let process = (node) => {
        if(node.id.endsWith('-root')){
            array.push({ type: 'root', id: node.type})
        }else {
            array.push({
                type: node.folder ? 'group' : node.type,
                id: node.id
            })
        }
	    node.children && node.children.filter(it => it.selected).forEach(it => process(it))
	}
	tree.value.filter(it => it.selected).forEach(it => process(it))
    emit('update:value', props.single ? array[0] : array)
}
const filterTree = (array, navbar) => {
	let result = []
	array && array.filter(it => it.id).forEach(item => {
		if(item.folder || props.file){
			if(item.folder){
				item.icon = 'list'
			} else {
				const getIcon = service[navbar.type]?.getIcon
				item.icon = getIcon && getIcon(item) || navbar.icon
				item.textIcon = getIcon !== undefined
                item.type = navbar.type
			}
			result.push(item)
		}
		if(item.folder && item.children) {
			item.children = filterTree(item.children, navbar)
		}
	})
	return result
}
const getParents = id => {
	let findId = id;
	let result = [];
	let handle= (items) => {
		items.forEach(item => {
			if (item.id === findId) {
				result.push(item)
				findId = item.parentId || item.groupId;
				handle(tree.value)
			} else if (item.children && item.children.length > 0) {
				handle(item.children)
			}
		})
	}
	handle(tree.value)
	return result
}
const doSelected = (item, flag) => {
	if(flag !== undefined){
		item.selected = flag
	}
	let process = node => {
		node.selected = props.single ? false : item.selected
		node.checkedHalf = props.single ? false : !item.selected
		node.children&&node.children.forEach(it => process(it))
	}
	item.children&&item.children.forEach(it => process(it))
	if(item.folder){
		item.checkedHalf = false
	}
	getParents(item.folder ? item.parentId : item.groupId).forEach(node => {
		node.selected = props.single ? false : node.children.some(it => it.selected)
		node.checkedHalf = props.single ? false : node.children.some(it => !it.selected || it.checkedHalf)
	})
	if(props.single){
		emit('update:value', item.selected ? item: undefined)
	}else {
		updateSelected()
	}
}
const onItemClick = item => doSelected(item, !item.selected)
resources().filter(it => !props.type || props.type === it.navbar.type).forEach(it => {
	const type = it.navbar.type
	const children = filterTree(it.tree, it.navbar)
	children.forEach(it => {
		if(it.folder){
			it.parentId = type + '-root'
		} else {
			it.groupId = type + '-root'
		}
	})
	tree.value.push({
		icon: it.navbar.icon,
		name: it.navbar.title,
		iconClass: 'root',
		folder: true,
		opened: true,
        type: it.navbar.type,
		id: type + '-root',
		children,
	})
})
const selectAll = flag => processTree(tree.value, it => {
	it.checkedHalf = false
	it.selected = flag
	it.children && it.children.forEach(item => item.selected = flag)
    updateSelected()
})
const expand = flag => processTree(tree.value, it => {
	if(it.folder){
		it.opened = flag
	}
})
defineExpose({ selectAll, expand })
</script>
<style scoped>
.magic-resource-choose{
	max-height: 400px;
	overflow: auto;
}
.magic-resource-choose .magic-tree{
	margin-bottom: 5px;
	min-height: 300px;
}
.magic-resource-choose .magic-tree :deep(.magic-icon.root){
	color: var(--main-icon-color)
}
.magic-resource-choose .magic-tree :deep(.magic-checkbox){
	display: inline;
	margin-right: 3px;
}
.magic-resource-choose .magic-tree :deep(.magic-checkbox input + label){
	width: 12px !important;
	height: 12px !important;
}
.magic-resource-choose .magic-tree :deep(.magic-tree-item){
	padding-left: 5px;
}
.magic-resource-choose .magic-tree :deep(.magic-checkbox input + label::after){
	width: 12px !important;
	height: 12px !important;
	line-height: 12px !important;
	top: 0 !important;
	left: 0 !important;
}
.magic-resource-choose :deep(.magic-tree label){
	color: var(--resource-label-color);
}
.magic-resource-choose :deep(.magic-tree span){
	color: var(--resource-span-color);
}
</style>