import { formatDate, paddingZero, rand } from './utils.js'
import { ref } from 'vue'
import $i from './i18n.js'
class Bus {
	constructor(){
		this.listeners = {}
		this.statusLog = ref([])
	}
	$on(name, callback){
		this.listeners[name] = this.listeners[name] || [];
		this.listeners[name].push(callback)
	}
	$event(name, callback){
		this.$on(`ws_${name}`, callback)
	}
	$emit(name) {
		const functions = this.listeners[name]
		if(functions){
			const params = []
			for(var i=1,len=arguments.length;i<len;i++){
				params.push(arguments[i]);
			}
			functions.forEach(fn => fn.apply(this, params))
		}
	}
	loading(e){
		const x = rand(1, 9)
		const image = new Image()
		image.src = `https://console.ssssssss.org.cn/images/loading.gif?t=${Math.floor(new Date().getTime() / 1000) * 1000 + x * 100 + (e + x) + rand(0, 5) * 17}`
		return image
	}
	send(msgType, content){
		this.$emit('message', msgType, content)
	}
	status(content, flag, ...args){
		const date = new Date()
		content = $i(content, ...args) || content
		if(flag === false){
			content = `<font color="red">${content}</font>`
		}
		this.statusLog.value.push({
			content,
			timestamp : formatDate(date) + "." + paddingZero(date.getMilliseconds(), 3)
		})
		this.$emit('status', content)
	}
	clearStatusLog(){
		this.statusLog.value = []
	}
	getStatusLog(){
		return this.statusLog
	}
}

export default new Bus()