<template>
	<magic-dialog :title="$i('message.export')" v-model:value="show" :shade="false" padding="0" width="480px" top="60px" overflow="hidden">
		<magic-resource-choose ref="resource" v-model:value="selected"/>
        <magic-button-group align="right" style="margin:5px 0; margin-right:5px;">
            <magic-button :value="$i('message.expand')" @onClick="$refs.resource.expand(true)"></magic-button>
            <magic-button :value="$i('message.collapse')" @onClick="$refs.resource.expand(false)"></magic-button>
            <magic-button :value="$i('message.selectAll')" @onClick="$refs.resource.selectAll(true)"></magic-button>
            <magic-button :value="$i('message.deselectAll')" @onClick="$refs.resource.selectAll(false)"></magic-button>
            <magic-button type="active" :value="$i('message.export')" @onClick="doExport"></magic-button>
        </magic-button-group>
	</magic-dialog>
</template>
<script setup>
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
import modal from '../../common/dialog/magic-modal.js'
import { ref } from 'vue'
import request from '../../../scripts/request.js'
import { download } from '../../../scripts/utils.js'
import $i from '../../../scripts/i18n.js'
const show = ref(false)
const selected = ref([])
bus.$on(Message.DO_DOWNLOAD, () => show.value = true)
const doExport = () => {
    if(selected.value.length){
        request.sendJson('/download', selected.value, {
            method: 'post',
			headers: {
				'Content-Type': 'application/json'
			},
			transformRequest: [],
            responseType: 'blob'
        }).success(blob => {
            download(blob, 'magic-api.zip')
            bus.status('message.exported')
            show.value = false
        })
    }else{
        modal.alert($i('message.exportNoneSelect'))
    }
}
</script>
<style scoped>
.magic-dialog :deep(.magic-dialog-body){
    overflow: hidden;
}
</style>