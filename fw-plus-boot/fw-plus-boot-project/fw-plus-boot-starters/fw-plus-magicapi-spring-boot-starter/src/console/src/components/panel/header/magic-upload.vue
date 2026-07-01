<template>
	<magic-dialog :title="$i('message.upload')" v-model:value="show">
		<magic-file v-model:value="file" accept="application/x-zip-compressed"/>
		<magic-button-group align="right" style="margin-top:5px">
			<magic-button :value="$i('upload.increment')" type="active" @click="doUpload('increment')"/>
			<magic-button :value="$i('upload.full')" @click="doUpload('full')"/>
		</magic-button-group>
	</magic-dialog>
</template>
<script setup>
import { ref } from 'vue'
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
import request from '../../../scripts/request.js'
import $i from '../../../scripts/i18n.js'
import modal from '../../common/dialog/magic-modal.js'
const show = ref(false)
const file = ref(null)
const doUpload = mode => {
	if(file.value){
		const formData = new FormData();
		formData.append('file', file.value, file.value.name);
		formData.append('mode', mode);
		const _upload = () => {
			request.send('/upload', formData, {
				method: 'post',
				headers: {
					'Content-Type': 'multipart/form-data'
				}
			}).success(res => {
				const msg = mode === 'full' ? $i('upload.full') : $i('upload.increment')
				if(res){
					bus.status('upload.success', true, msg)
					show.value = false
					bus.$emit(Message.LOAD_RESOURCES)
				} else {
					bus.status('upload.failed', false, msg)
				}
			})
		}
		if(mode === 'full'){
			modal.confirm($i('message.upload'), $i('message.uploadWarning'), _upload)
		} else {
			_upload()
		}
	}
}
bus.$on(Message.DO_UPLOAD, () => show.value = true)
</script>
