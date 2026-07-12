<template>
	<div class="magic-api-group">
		<form>
			<label>{{ $i('resource.form.groupName') }}</label>
			<magic-input v-model:value="info.name" :placeholder="$i('resource.form.placeholder.name', $i('api.name'))" width="250px"/>
			<label>{{ $i('resource.form.groupPath') }}</label>
			<magic-input v-model:value="info.path" :placeholder="$i('resource.form.placeholder.path', $i('api.name'))" width="auto" style="flex:1"/>
            <magic-button :value="$i('message.save')" @onClick="onClick"/>
		</form>
	</div>
	<magic-navbar direction="horizontal" ref="navbar" style="flex:1" :allow-close="false">
		<magic-navbar-item v-for="(navbar, key) in navbars" :key="key" v-bind="navbar">
			<component :is="navbar.component"/>
		</magic-navbar-item>
	</magic-navbar>
</template>
<script setup>
import { resolveDynamicComponent, inject } from 'vue'
import request from '../../../scripts/request.js'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const navbars = [{
	title: $i('api.navbars.path'),
	component: resolveDynamicComponent('magic-api-path')
},{
	title: $i('api.navbars.groupOption'),
	component: resolveDynamicComponent('magic-api-option')
}]
const onClick = () => {
    const group = info.value
    request.sendJson('/resource/folder/save', {
        id: group.id,
        name: group.name,
        parentId: group.parentId,
        type: group.type,
        path: group.path,
        paths: group.paths,
        options: group.options
    })
}
</script>
<style scoped>
.magic-api-group{
	display: flex;
	flex-direction: column;
}
.magic-api-group form{
	display: flex;
	padding: 5px;
}
.magic-api-group form label{
	display: inline-block;
	width: 85px;
	height: 22px;
	line-height: 22px;
	font-weight: 400;
	text-align: right;
	padding: 0 5px;
}
.magic-api-group + .magic-navbar{
	flex-direction: column;
	overflow: hidden;
}
.magic-api-group + .magic-navbar :deep(.magic-navbar-header){
	border-bottom: 1px solid var(--main-border-color);
}
.magic-navbar :deep(.magic-navbar-body),
.magic-navbar :deep(.magic-navbar-item){
	width: 100%;
	height: 100%;
}
.magic-api-group > form .magic-button{
    margin-left: 10px;
}
</style>