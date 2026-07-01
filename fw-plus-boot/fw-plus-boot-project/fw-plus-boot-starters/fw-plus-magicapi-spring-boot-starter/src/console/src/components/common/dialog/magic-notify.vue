<template>
	<div class="magic-notify">
		<div class="magic-notify-item" v-for="(item, key) in messages" :key="key">
			<div class="close" @click="removeMessage(item)">
				<magic-icon icon="close"/>
			</div>
			<h3><svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" v-html="item.icon"></svg>{{item.title}}</h3>
			<p v-html="item.content"></p>
			<magic-button-group v-if="item.buttons">
				<magic-button v-for="(button, key) in item.buttons" :key="key" :value="button.title" type="link" @click="() => removeMessage(item) && button.onClick()"></magic-button>
			</magic-button-group>
		</div>
	</div>
</template>
<script setup>
import { reactive } from 'vue'
import bus from '../../../scripts/bus'
import Message from '../../../scripts/constants/message.js'

const icons = {
	'error' : `<path fill="#E05555" fill-opacity=".7" fill-rule="evenodd" d="M8,14 C4.6862915,14 2,11.3137085 2,8 C2,4.6862915 4.6862915,2 8,2 C11.3137085,2 14,4.6862915 14,8 C14,11.3137085 11.3137085,14 8,14 Z M7,4 L7,8 L9,8 L9,4 L7,4 Z M7,10 L7,12 L9,12 L9,10 L7,10 Z"/>`,
	'warning' : `<path fill="#F4AF3D" fill-rule="evenodd" d="M8,2 L15,14 L1,14 L8,2 Z M9,13 L9,11 L7,11 L7,13 L9,13 Z M9,10 L9,6 L7,6 L7,10 L9,10 Z"/>`,
	'info': `<path fill="#40B6E0" fill-opacity=".7" fill-rule="evenodd" d="M8,14 C4.6862915,14 2,11.3137085 2,8 C2,4.6862915 4.6862915,2 8,2 C11.3137085,2 14,4.6862915 14,8 C14,11.3137085 11.3137085,14 8,14 Z M7,4 L7,8 L9,8 L9,4 L7,4 Z M7,10 L7,12 L9,12 L9,10 L7,10 Z" transform="rotate(-180 8 8)"/>`
}

const messages = reactive([])
const removeMessage = message => {
	const index = messages.findIndex(it => message.id === it.id)
	if(index > -1){
		messages.splice(index, 1)
	}
	return true
}
const appendMessage = message => {
	message.icon = icons[message.icon] || icons.info
	message.id = message.id || parseInt(Math.random() * 10000000)
	removeMessage(message)
	messages.push(message)
	if(message.duration){
		setTimeout(() => removeMessage(message), parseInt(message.duration))
	}
}
bus.$on(Message.NOTIFY, appendMessage)
</script>

<style scoped>
.magic-notify{
	position: absolute;
	right: 40px;
	bottom: 65px;
	transition: 0.5s;
	z-index: 999999;
}
.magic-notify .magic-notify-item{
	background-color: var(--main-background-color);
	padding: 5px;
	position: relative;
	border-radius: 2px;
	border: 1px solid var(--main-border-color);
	max-width: 280px;
	box-shadow: 0 0 8px var(--dialog-shadow-color);
}
.magic-notify .magic-notify-item:not(:first-child){
	margin-top: 5px;
}
.magic-notify .magic-notify-item .magic-icon-close{
	font-size: 12px;
}
.magic-notify .magic-notify-item .close{
	position: absolute;
	top: 2px;
	right: 2px;
	display: none;
}
.magic-notify .magic-notify-item h3{
	font-size: 12px;
	height: 20px;
	line-height: 20px;
	display: flex;
	align-items: center;
}
.magic-notify .magic-notify-item h3 svg{
	vertical-align: middle;
	margin-right: 2px;
}
.magic-notify .magic-notify-item:hover .close{
	display: block;
}
.magic-notify .magic-notify-item p{
	padding: 5px;
	padding-left: 16px;
}
.magic-notify .magic-notify-item .magic-button-group{
	padding-left: 11px;
}
</style>
