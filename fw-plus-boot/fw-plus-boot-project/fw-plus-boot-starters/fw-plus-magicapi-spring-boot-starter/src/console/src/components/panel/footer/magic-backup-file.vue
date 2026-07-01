<template>
    <magic-loading :loading="loading">
        <div class="magic-backup-file">
            <template v-if="backupData.length > 0">
                <magic-table :data="backupData" :border="true" @clickRow="clickRow">
                    <magic-table-column :title="$i('message.date')" width="160" v-slot:default="{ row }" class="selected">{{ formatDate(row.createDate) }}</magic-table-column>
                    <magic-table-column :title="$i('history.operator')" width="100" v-slot:default="{ row }">{{ row.createBy || 'guest' }}</magic-table-column>
                </magic-table>
                <div class="magic-backup-file-diff-container">
                    <ul>
                        <li>
                            {{ formatDate(current.createDate) }} by {{ current.createBy || 'guest' }}
                            <magic-button :value="$i('backup.rollback')" type="active" @click="doRollback"/>
                        </li>
                        <li>{{ $i('backup.current') }}<template v-if="oldInfo">({{ formatDate(oldInfo.updateTime || oldInfo.createTime) }} by {{ oldInfo.updateBy || oldInfo.createBy || 'guest' }})</template></li>
                    </ul>
                    <magic-monaco-diff-editor v-if="!scriptLoading" v-model:value="diffValue" :language="language"/>
			        <magic-loading v-else :loading="scriptLoading" />
                </div>
            </template>
            <magic-empty v-else :text="$i('message.empty', $i('history.name'))"/>
        </div>
    </magic-loading>
</template>
<script setup>
import { inject, reactive, ref, watch } from 'vue'
import $i from '../../../scripts/i18n.js'
import request from '../../../scripts/request.js'
import { formatDate } from '../../../scripts/utils.js'
import modal from '../../common/dialog/magic-modal.js'
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'
const props = defineProps({
    id: String
})
const service = inject('service')
const oldInfo = ref('')
const loading = ref(true)
const scriptLoading = ref(true)
const backupData = reactive([])
const current = ref({})
const diffValue = ref([])
const language = ref('')
const loadDifference = () => {
    language.value = service[current.value.type].language || 'magicscript'
    const promises = []
    scriptLoading.value = true
    if(!oldInfo.value){
        promises.push(new Promise(r => request.sendGet('/resource/file/' + props.id).success(res => oldInfo.value = res).end(() => r())))
    }
    promises.push(new Promise(r => request.sendGet('/backup', { id: props.id, timestamp: current.value.createDate }).success(res => current.value.script = res).end(() => r())))
    Promise.all(promises).then(() => {
        scriptLoading.value = false
        diffValue.value = [current.value.script, oldInfo.value?.script]
    }).catch(e => {
        console.error(e)
        scriptLoading.value = false
    })
}
const load = () => {
    loading.value = true
    request.sendGet('/backup/' + props.id).success(res => {
        if(res && res.length > 0){
            backupData.length = 0
            backupData.push(...res)
            current.value = backupData[0] || {}
            if(current.value){
                loadDifference()
            }
        }
        loading.value = false
    })
}
load();
watch(() => props.id, load)

const doRollback = () => {
	const msg = `${current.value.name}(${formatDate(current.value.createDate)})`
	request.sendPost('/backup/rollback', { id: props.id, timestamp: current.value.createDate}).success(r => {
		if(!r){
			modal.alert($i('backup.rollbackFailed', msg))
			bus.status('backup.rollbackFailed', false, msg)
		} else {
			modal.alert($i('backup.rollbackSuccess', msg))
			bus.status('backup.rollbackSuccess', true, msg)
            bus.$emit(Message.REFRESH_RESOURCE, props.id)
		}
	})
}
const clickRow = rowIndex => {
    current.value = backupData[rowIndex]
    loadDifference()
}
</script>
<style scoped>
.magic-table{
    width: 270px;
    border-right: 1px solid var(--table-border-color);
}
.magic-backup-file{
    height: 100%;
    position: relative;
    overflow: hidden;
    display: flex;
}
.magic-backup-file-diff-container{
    flex: 1;
    overflow: hidden;
    height: 100%;
    display: flex;
    flex-direction: column;
}
.magic-backup-file-diff-container > ul{
    display: flex;
}
.magic-backup-file-diff-container > ul li{
    flex: 1;
    height: 30px;
    line-height: 30px;
    padding: 0 5px;
}
.magic-backup-file-diff-container > ul li button{
    float: right;
    margin-right: 10px;
    margin-top: 4px;
}
</style>
