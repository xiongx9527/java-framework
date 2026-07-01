<template>
	<magic-dialog v-model:value="show" :title="$i('editor.tooltip.recent')" padding="0" :shade="false">
		<ul class="magic-recent-opened">
			<li v-for="(row, key) in data" :key="key" @click.stop="onClick(row.item.id)">
				<magic-text-icon v-if="services[row.type] && services[row.type].getIcon" :icon="services[row.type].getIcon(row.item)"/>
				<label>{{ row.name }}</label>
				<span v-if="services[row.type].requirePath && row.path">({{ row.path }})</span>
			</li>
		</ul>
		<div v-if="data.length === 0" style="width: 100%; height: 100px">
			<magic-empty text="empty."/>
		</div>
	</magic-dialog>
</template>
<script setup>
import { computed, inject, reactive, ref, toRaw } from 'vue'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import store from '../../../scripts/store.js'
const recentOpened = reactive([])
const show = ref(false)
const findResource = inject('findResource')
const services = inject('service')
try{
	JSON.parse(store.get(constants.RECENT_OPENED) || '[]').forEach(it => recentOpened.push(it))
} catch(ignored){

}
const data = computed(() => recentOpened.map(it => findResource(it)).filter(it => it && services[it.type]))
const onClick = id => {
	bus.$emit(Message.OPEN_WITH_ID, id)
	show.value = false
}
bus.$on(Message.DO_RECENT, () => show.value = !show.value)
bus.$on(Message.CLOSE, item => {
	if(item.id){
		const index = recentOpened.findIndex(it => it === item.id)
		if(index > -1){
			recentOpened.splice(index, 1)
		}
		recentOpened.unshift(item.id)
		if(recentOpened.length > 20){
			recentOpened.splice(recentOpened.length - 1, 1)
		}
		store.set(constants.RECENT_OPENED, toRaw(recentOpened))
	}
})
</script>
<style scoped>
ul{
	max-width: 400px;
	max-height: 600px;
	overflow: auto;
}
ul li:hover{
	background-color: var(--tree-hover-background-color);
}
ul li{
	height: 20px;
	line-height: 20px;
	transition: 0.3s;
	word-break: break-all;
	white-space: nowrap;
}
ul li label{
	color: var(--resource-label-color);
}
ul li span {
	color: var(--resource-span-color);
}
.magic-dialog-main :deep(.magic-dialog-body){
	padding:0;
}
</style>