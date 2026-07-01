import bus from './bus.js'
import ReconnectingWebSocket from './reconnecting-websocket.js'
let openedSocket = {}
function MagicWebSocket(url) {
	if(openedSocket[url]) {
		try{
			openedSocket[url].close()
		} catch(e){

		}
	}
	this.listeners = {};
	this.future = new Promise(r => {
		this.resolve = r
		this.socket = new ReconnectingWebSocket(url, null, {
			timeoutInterval: 10000
		});
		openedSocket[url] = this
		this.socket.onmessage = this.messageReceived;
		this.socket.onconnecting = () => {
			this.future = new Promise(resolve => this.resolve = resolve)
			bus.status('message.connectDebugServer')
		}
		this.socket.onopen = () => {
			bus.status('message.connectDebugServerSuccess')
			bus.$emit('ws_open')
			this.resolve()
		}
		this.socket.onclose = () => {
			bus.status('message.debugServerClose')
			bus.$emit('ws_close')
		}
	})
}

MagicWebSocket.prototype.on = function (msgType, callback) {
	this.listeners[msgType] = this.listeners[msgType] || []
	this.listeners[msgType].push(callback)
}

MagicWebSocket.prototype.messageReceived = function (e) {
	let payload = e.data
	let index = payload.indexOf(",")
	let msgType = index === -1 ? payload : payload.substring(0, index)
	let args = []
	while (index > -1) {
		payload = payload.substring(index + 1)
		if (payload.startsWith('[') || payload.startsWith('{')) {
			args.push(JSON.parse(payload))
			break;
		}
		index = payload.indexOf(",")
		args.push(index === -1 ? payload : payload.substring(0, index))
	}
	bus.$emit('ws_' + msgType, args)
}
MagicWebSocket.prototype.send = function(msg){
	this.future.then(() => {
		this.socket.send(msg)
	})

}
MagicWebSocket.prototype.close = function () {
	this.socket.close()
}

export default MagicWebSocket
