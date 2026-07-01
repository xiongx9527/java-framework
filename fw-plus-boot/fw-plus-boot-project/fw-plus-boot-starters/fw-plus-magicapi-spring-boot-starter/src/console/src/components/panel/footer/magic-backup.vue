<template>
	<div class="magic-backup">
		<magic-panel-toolbar :toolbars="toolbars" />
		<div>
			<magic-loading :loading="loading"></magic-loading>
			<magic-table v-if="backupData.length > 0" :data="backupData" :border="true" @contextmenu="onContextmenu" @loadNext="loadNext">
				<magic-table-column :title="$i('message.date')" width="160" v-slot:default="{ row }">{{ formatDate(row.createDate) }}</magic-table-column>
				<magic-table-column :title="$i('message.type')" width="90" v-slot:default="{ row }"><span>{{ displayTitle(row.type) }}</span></magic-table-column>
				<magic-table-column :title="$i('history.operator')" width="100" v-slot:default="{ row }">{{ row.createBy || 'guest' }}</magic-table-column>
				<magic-table-column :title="$i('message.name')" align="left" v-slot:default="{ row }">{{ row.name }}</magic-table-column>
			</magic-table>
			<magic-empty v-else :text="$i('message.empty', $i('history.name'))"/>
		</div>
	</div>
	<magic-dialog v-model:value="dialog" :title="$i('history.name')" width="80%" maxWidth="100%" top="60px" height="80%" className="magic-dialog-diff">
		<div class="magic-backup-diff-container">
			<div class="magic-backup-diff-header">
				<div>{{ currentTime }} by {{ currentSelected.createBy || 'guest' }}</div>
				<div>{{ $i('backup.current') }}({{ formatDate(currentVersion.updateTime || currentVersion.createTime) }} by {{ currentVersion.updateBy || currentVersion.createBy || 'guest' }})</div>
			</div>
			<magic-monaco-diff-editor v-if="!dialogLoading" v-model:value="diffValue" :language="language"/>
			<magic-loading v-else :loading="dialogLoading" />
		</div>
		<magic-button-group align="right">
			<magic-button :value="$i('backup.rollback')" type="active" @click="doRollback"/>
		</magic-button-group>
	</magic-dialog>
</template>
<script setup>
import { getCurrentInstance, inject, onMounted, ref, resolveDynamicComponent, shallowRef } from 'vue'
import bus from '../../../scripts/bus.js'
import constants from '../../../scripts/constants.js'
import Message from '../../../scripts/constants/message.js'
import request from '../../../scripts/request.js'
import $i from '../../../scripts/i18n.js'
import { formatDate } from '../../../scripts/utils.js'
import modal from '../../common/dialog/magic-modal.js'
const magicBackupFileComponent = resolveDynamicComponent('magic-backup-file')
const loading = ref(true)
const toolbars = [{
	icon: 'refresh',
	title: $i('message.refresh'),
	onClick() {
		load()
	}
}, {
	icon: 'copy',
	title: $i('backup.full'),
	onClick() {
		request.sendPost('/backup/full').success(() => {
			bus.status('backup.backupSuccess')
			load()
		})
	}
}]
const diffValue = ref([])
const backupData = ref([])
const dialog = ref(false)
const dialogLoading = ref(true)
const currentTime = ref('')
const currentSelected = ref({})
const language = ref('magicscript')
let loadingNext = false
let hasNext = false
let nextTimestamp = undefined
const load = (timestamp) => {
	loading.value = true
	request.sendGet('/backups', { timestamp }).success(res => {
		if(timestamp){
			backupData.value.push(...res)
		} else {
			backupData.value = res
		}
		if(hasNext = res.length >= 100){
			 nextTimestamp = res[res.length - 1].createDate
		}
		loading.value = false
	})
}
const service = inject('service')
const currentVersion = ref({})
const displayTitle = type => {
	if(type.endsWith('-group')){
		return $i('message.group', service[type.replace('-group', '')]?.name || 'Unknown')
	}
	if(type === 'full'){
		return $i('backup.full')
	}
	return service[type]?.name || type
}
const displayTime = timestamp => formatDate(timestamp)
let inited = false
bus.$on(Message.LOAD_RESOURCES_FINISH, (type) => type || load())
const loadNext = () => {
	if(hasNext && !loadingNext) {
		load(nextTimestamp)
	}
}
onMounted(() => {
	if(!inited && constants.LOGINED){
		load()
	}
	inited = true
})
const { proxy } = getCurrentInstance()
const onContextmenu = (event, row) => {
	const menus = []
	const rowService = service[row.type]
	if(row.id !== 'full' && !(row.type&&row.type.endsWith('-group')) && !(rowService && rowService.requireScript === false)){
		menus.push({
			icon: 'difference',
			label: $i('backup.difference'),
			onClick() {
				currentTime.value = formatDate(row.createDate)
				dialog.value = true
				dialogLoading.value = true
				language.value = service[row.type].language || 'magicscript'
				Promise.all([
					new Promise(r => request.sendGet('/backup', { id: row.id, timestamp: row.createDate}).success(script => r(script))),
					new Promise(r => request.sendGet('/resource/file/' + row.id).success(res => r(res)))
				]).then(values => {
					currentVersion.value = values[1]
					currentSelected.value = row
					dialogLoading.value = false
					diffValue.value = [values[0], values[1]?.script]
				}).catch(e => {
					console.error(e)
					dialog.value = false
				})
			}
		})
		menus.push({
			label: $i('toolbars.history'),
			icon: 'history',
			onClick: () => {
				bus.$emit(Message.ADD_FOOTER_TOOLBAR, {
					component: shallowRef(magicBackupFileComponent),
					id: 'backup-file',
					icon: 'history',
					title: $i('toolbars.viewHistory', row.name),
					allowClose: true,
					data: {
						id: row.id
					}
				})
			}
		})
	}
	if(row.id === 'full' || !row.type.endsWith('-group')){
		menus.push({
			icon: 'rollback',
			label: $i('backup.rollback'),
			onClick(){
				const msg = `${row.name}(${formatDate(row.createDate)})`
				modal.confirm($i('backup.rollback'), $i('backup.rollbackConfirm'), () => {
					request.sendPost('/backup/rollback', { id: row.id, timestamp: row.createDate}).success(r => {
						if(!r){
							modal.alert($i('backup.rollbackFailed', msg))
							bus.status('backup.rollbackFailed', false, msg)
						} else {
							bus.status(`backup.rollbackSuccess`, true, msg)
							bus.$emit(Message.LOAD_RESOURCES)
						}
					})
				})
			}
		})
	}
	if(menus.length > 0){
		proxy.$contextmenu({ event, menus })
	}
}
const doRollback = () => {
	dialog.value = false
	const msg = `${currentSelected.value.name}(${formatDate(currentSelected.value.createDate)})`
	request.sendPost('/backup/rollback', { id: currentSelected.value.id, timestamp: currentSelected.value.createDate}).success(r => {
		if(!r){
			modal.alert($i('backup.rollbackFailed', msg))
			bus.status('backup.rollbackFailed', false, msg)
		} else {
			modal.alert($i('backup.rollbackSuccess', msg))
			bus.status('backup.rollbackSuccess', true, msg)
			bus.$emit(Message.REFRESH_RESOURCE, currentSelected.value.id)
		}
	})
}
</script>

<style scoped>
.magic-backup{
	display: flex;
	flex: 1;
	height: 100%;
}
.magic-backup > div{
	background-color: var(--navbar-body-background-color);
	flex: 1;
	overflow: hidden;
	position: relative;
	height: 100%;
}
.magic-backup .magic-table{
	height: 100%;
	position: absolute;
	width: 100%;
}
.magic-backup :deep(.magic-table-row .magic-table-column:last-child),
.magic-backup :deep(.magic-table-header .magic-table-column:last-child){
	padding-left: 5px;
}
.magic-backup-diff-container{
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
}
.magic-backup-diff-container .magic-backup-diff-header{
	height: 30px;
	line-height: 30px;
	display: flex;
}
.magic-backup-diff-container .magic-backup-diff-header > div{
	flex: 1;
}
.magic-backup-diff-container .magic-backup-diff-header > div:last-child{
	text-align: right;
}
</style>
<style>
.magic-dialog-diff .magic-dialog-main{
	display: inline-flex;
	flex-direction: column;
}
.magic-dialog-diff .magic-dialog-main .magic-dialog-body{
	flex: 1;
	display: flex;
    flex-direction: column;
}
</style>
