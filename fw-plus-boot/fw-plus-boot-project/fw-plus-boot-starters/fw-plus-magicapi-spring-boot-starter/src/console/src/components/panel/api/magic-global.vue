<template>
	<div class="magic-global">
		<magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false">
			<magic-navbar-item v-for="(navbar, key) in navbars" :key="key" v-bind="navbar">
				<component :is="navbar.component"/>
			</magic-navbar-item>
		</magic-navbar>
	</div>
</template>
<script setup>
import { resolveDynamicComponent } from 'vue'
const navbars = [{
	title: '全局请求参数',
	component: resolveDynamicComponent('magic-global-parameter')
},{
	title: '全局请求Header',
	component: resolveDynamicComponent('magic-global-header')
}]
</script>
<style scoped>
.magic-global{
	display: flex;
	flex-direction: column;
	flex: 1;
	width: 100%;
	height: 100%;
}
.magic-global > .magic-navbar{
	flex-direction: column;
	overflow: hidden;
}
.magic-global > .magic-navbar :deep(.magic-navbar-header){
	border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
	width: 100%;
	height: 100%;
}
.magic-global .magic-panel-global{
	display: flex;
	width: 100%;
    height: 100%;
}
.magic-global :deep(.magic-panel-global > div){
	flex: 1;
}
</style>