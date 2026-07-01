<template>
	<div class="magic-online">
		<magic-avatar-group :users="users" :max="9"/>
		<span>{{ $i('online.onlines', users.length) }}</span>
	</div>
</template>
<script setup>
import { inject, reactive } from 'vue'
import bus from '../../../scripts/bus'
import constants from '../../../scripts/constants.js'
import $i from '../../../scripts/i18n.js'
import Socket from '../../../scripts/constants/socket.js'
import Message from '../../../scripts/constants/message.js'

const users = reactive([])
const activateUserFiles = inject('activateUserFiles')
const processFiles = (cid, fileId) => {
	const user = users.find(it => it.cid === cid)
	if(user){
		Object.values(activateUserFiles.value).forEach(values => {
			const index = values.findIndex(it => it.cid === cid)
			if(index > -1){
				values.splice(index, 1)
			}
		})
		activateUserFiles.value[fileId] = activateUserFiles.value[fileId] || []
		activateUserFiles.value[fileId].push(user)
	}
}

const addUser = user => {
	if(!users.some(it => it.cid === user.cid)){
		users.push(user)
	}
}
bus.$event(Socket.LOGIN_RESPONSE, ([ret, user]) => {
	activateUserFiles.value = {}
	users.splice(0, users.length)
	if(ret === '1') {
		addUser(user)
	}
})
bus.$event(Socket.PING, () => {
	bus.send(Socket.PONG)
})
bus.$event(Socket.USER_LOGIN, ([user]) => {
	if(constants.CLIENT_ID !== user.cid ){
		bus.$emit(Message.NOTIFY, {
			title: $i('online.login'),
			content: $i('online.loginTips', user.username, user.ip),
			duration: 3000
		})
		bus.status('online.loginTips', true, user.username, user.ip)
	}
	addUser(user)
})
bus.$event(Socket.USER_LOGOUT, ([user]) => {
	if(constants.CLIENT_ID !== user.cid ){
		bus.$emit(Message.NOTIFY, {
			title: $i('online.logout'),
			content: $i('online.logoutTips', user.username, user.ip),
			duration: 3000
		})
		bus.status('online.logoutTips', true, user.username, user.ip)
	}
	const index = users.findIndex(it => it.cid === user.cid)
	processFiles(user.cid, '0')
	if(index > -1){
		users.splice(index, 1)
	}
})
bus.$event(Socket.ONLINE_USERS, ([ rows ]) => {
	rows.forEach(user => {
		if(constants.CLIENT_ID !== user.cid){
			addUser(user)
			user.fileId && processFiles(user.cid, user.fileId)
		}
	})
})
bus.$event(Socket.INTO_FILE_ID, ([cid, fileId]) => processFiles(cid, fileId))
</script>

<style scoped>
.magic-online{
	height: 30px;
	line-height: 30px;
	float: right;
	padding-left: 10px;
	max-width: 500px;
}
.magic-online > span{
	padding-left: 5px;
}
</style>
