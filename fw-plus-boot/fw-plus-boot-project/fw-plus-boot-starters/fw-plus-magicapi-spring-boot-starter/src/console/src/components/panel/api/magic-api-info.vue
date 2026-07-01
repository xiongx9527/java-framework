<template>
	<div class="magic-api-info">
		<form>
			<label>{{$i('api.form.method')}}</label>
			<magic-select width="100px" :options="$REQUEST_METHODS" :default-select="$DEFAULT_REQUEST_METHOD" v-model:value="info.method"/>
			<label>{{$i('api.form.name')}}</label>
			<magic-input v-model:value="info.name" :placeholder="$i('api.form.placeholder.name')" width="200px"/>
			<label>{{$i('api.form.path')}}</label>
			<magic-input v-model:value="info.path" :placeholder="$i('api.form.placeholder.path')" width="auto" style="flex:1"/>
		</form>
	</div>
	<magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false" tooltip-direction="bottom">
		<magic-navbar-item v-for="(navbar, key) in navbars" :key="key" v-bind="navbar">
			<component :is="navbar.component"/>
		</magic-navbar-item>
	</magic-navbar>
</template>
<script setup>
import { resolveDynamicComponent, inject } from 'vue'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const navbars = [{
	title: $i('api.navbars.parameter'),
	component: resolveDynamicComponent('magic-api-parameter')
},{
	title: $i('api.navbars.header'),
	component: resolveDynamicComponent('magic-api-header')
},{
	title: $i('api.navbars.path'),
	component: resolveDynamicComponent('magic-api-path')
},{
	title: $i('api.navbars.body'),
	component: resolveDynamicComponent('magic-api-body')
},{
	title: $i('api.navbars.option'),
	component: resolveDynamicComponent('magic-api-option')
},{
	title: $i('api.navbars.description'),
	component: resolveDynamicComponent('magic-api-description')
}]
</script>
<style scoped>
.magic-api-info{
	display: flex;
	flex-direction: column;
}
.magic-api-info form{
	display: flex;
	padding: 5px;
}
.magic-api-info form label{
	display: inline-block;
	width: 75px;
	height: var(--magic-input-height);
  	line-height: var(--magic-input-height);
	font-weight: 400;
	text-align: right;
	padding: 0 5px;
}
.magic-api-info + .magic-navbar{
	flex-direction: column;
	overflow: hidden;
}
.magic-api-info + .magic-navbar :deep(.magic-navbar-header){
	border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
	width: 100%;
	height: 100%;
}
</style>