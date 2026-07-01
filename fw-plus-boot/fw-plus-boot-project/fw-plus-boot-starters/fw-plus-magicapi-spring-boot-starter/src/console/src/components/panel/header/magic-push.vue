<template>
	<magic-dialog :title="$i('message.push')" v-model:value="show" :shade="false" padding="0" width="450px" top="60px" overflow="hidden">
		<magic-resource-choose ref="resource" v-model:value="selected"/>
		<div class="magic-push-form">
			<div>
				<label>{{ $i('message.remote') }}：</label>
				<magic-input v-model:value="remote"/>
			</div>
			<div>
				<label>{{ $i('message.secret') }}：</label>
				<magic-input v-model:value="password" type="password"/>
			</div>
		</div>
		<magic-button-group align="right" style="margin:5px 0; margin-right:5px;">
			<magic-button :value="$i('message.selectAll')" @onClick="$refs.resource.selectAll(true)"></magic-button>
			<magic-button :value="$i('message.deselectAll')" @onClick="$refs.resource.selectAll(false)"></magic-button>
			<magic-button :value="$i('push.increment')" @onClick="doPush('increment')" type="active"></magic-button>
			<magic-button :value="$i('push.full')" @onClick="doPush('full')"></magic-button>
		</magic-button-group>
	</magic-dialog>
</template>
<script setup>
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
import modal from '../../common/dialog/magic-modal.js'
import $i from '../../../scripts/i18n.js'
import { ref } from 'vue'
import request from '../../../scripts/request.js'
const show = ref(false)
const selected = ref([])
const remote = ref('http://host:port/_magic-api-sync')
const password = ref('123456789')
bus.$on(Message.DO_PUSH, () => show.value = true)
const _push = (mode)=> {
	request.sendJson('/push', selected.value, {
		method: 'post',
		headers: {
			'magic-push-target': remote.value,
			'magic-push-secret-key': password.value,
			'magic-push-mode': mode,
			'Content-Type': 'application/json',
		},
		transformRequest: [],
	}).success(() => {
		const msg = mode === 'full' ? $i('push.full') : $i('push.increment') 
		bus.status('push.success', true, msg)
		show.value = false
		bus.$emit(Message.LOAD_RESOURCES)
	})
}
const doPush = (mode) => {
	if(selected.value.length){
		if(mode === 'full'){
			modal.confirm($i('message.push'), $i('message.pushWarning'), () => _push(mode))
		} else {
			_push(mode)
		}
	}else{
		modal.alert($i('message.pushNoneSelect'))
	}
}
</script>
<style scoped>
.magic-dialog :deep(.magic-dialog-body){
	overflow: hidden;
}
.magic-push-form{
	padding:0 10px;
}
.magic-push-form > div{
	display: flex;
	margin-top: 5px;
}
.magic-push-form label {
  width: 80px;
  text-align: right;
  display: inline-block;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
}
.magic-push-form :deep(.magic-input){
	width: auto !important;
	flex: 1;
}
</style>