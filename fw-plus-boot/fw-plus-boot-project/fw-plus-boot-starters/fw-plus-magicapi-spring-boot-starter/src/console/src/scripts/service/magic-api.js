import constants from '../constants.js'
import Message from '../constants/message.js'
import request from '../request.js'
import bus from '../bus.js'
import $i from '../i18n.js'
import modal from '../../components/common/dialog/magic-modal.js'
import { definition2TreeList, getSizeUnit, structureHasChanged } from '../utils.js'
import { parseJson } from '../parsing/parser.js'
import { nextTick } from 'vue'
function sendApiTestRequest(opened, requestConfig) {
	bus.$emit(Message.SWITCH_TOOLBAR, 'log')
	requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID
	requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id
	requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE
	// 设置断点
	requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || []).filter(it => it.options.linesDecorationsClassName === 'breakpoints').map(it => it.range.startLineNumber).join(',')
	requestConfig.responseType = 'blob'
	requestConfig.validateStatus = () => true
	let dataLen = 0
	requestConfig.transformResponse = [function (data, headers) {
		dataLen = data.size
		if (headers['content-disposition']) {
			return new Promise(resolve => resolve(data));
		}
		return new Promise(resolve => {
			const reader = new FileReader()
			reader.readAsText(data)
			reader.onload = function () {
				try {
					resolve(JSON.stringify(JSON.parse(this.result), null, 4))
				} catch (e) {
					resolve(data)
				}
			}
		})
	}]
	const fullName = opened.path()
	bus.status('api.test.begin', true, fullName)
	const start = new Date().getTime()
	request.execute(requestConfig).then(res => res.data.then(data => {
		bus.status('api.test.success', true, fullName, res.status, getSizeUnit(dataLen), new Date().getTime() - start)
		opened.running = false
		if((opened.responseBlob = data instanceof Blob)){
			opened.responseBlobValue = data
			opened.item.responseBody = null
			opened.item.responseBodyDefinition = null
			opened.responseBodyTree = null
		} else {
			opened.item.responseBody = data
			let definition = parseJson(opened.item.responseBody, opened.item.responseBodyDefinition)
			if(structureHasChanged(opened.item.responseBodyDefinition, definition)){
				bus.$emit(Message.NOTIFY, {
					title: $i('message.tips'),
					id: 'responseBodyStructure',
					icon: 'info',
					content: $i('api.structure.content', 'ResponseBody'),
					buttons: [{
						title: $i('api.structure.ok'),
						onClick: () => {
							opened.item.responseBodyDefinition = definition
							opened.responseBodyTree = definition2TreeList(definition)
						}
					}, {
						title: $i('message.cancel'),
						onClick: () => {}
					}]
				})
				
			}
		}
		opened.responseHeaders = res.headers
		nextTick(() => bus.$emit(Message.SWITCH_TOOLBAR, 'response'))
	})).catch(error => {
		bus.status('api.test.requestError', false, fullName)
		opened.running = false
		request.processError(error)
	})

}
function doApiTest(opened) {
	opened.running = true
	const info = opened.item
	const requestConfig = {
		baseURL: constants.SERVER_URL,
		url: opened.requestPath(),
		method: info.method,
		headers: {},
		responseType: 'json',
		withCredentials: true
	}
	const setPaths = paths => paths.filter(it => it.value && it.value.trim()).forEach(it => requestConfig.url = requestConfig.url.replace(new RegExp(`\\{${it.name}\}`, "g"), it.value.trim()))
	opened.getGroups().filter(it => it.paths && it.paths.length > 0).map(it => it.paths).forEach(paths => setPaths(paths))
	setPaths(info.paths || [])
	if (requestConfig.url.indexOf('{') > -1) {
		modal.alert($i('api.test.missPath'))
		opened.running = false
		return;
	}
	constants.GLOBAL.headers.filter(it => it.name).forEach(it => requestConfig.headers[it.name] = it.value)
	info.headers.filter(it => it.name).forEach(it => requestConfig.headers[it.name] = it.value)
	const params = {}
	constants.GLOBAL.parameters.filter(it => it.name).forEach(it => params[it.name] = it.value)
	info.parameters.filter(it => it.name).forEach(it => params[it.name] = it.value)
	if (Object.values(params).some(it => it instanceof FileList || it instanceof File)) {
		requestConfig.headers['Content-Type'] = 'multipart/form-data';
		const formData = new FormData()
		Object.keys(params).forEach(key => {
			let value = params[key];
			if (value instanceof FileList) {
				Array.from(value).forEach(file => formData.append(key, file, file.name))
			} else if (value instanceof File) {
				formData.append(key, value, value.name)
			} else {
				formData.append(key, value);
			}
		});
		requestConfig.data = formData;
	} else {
		requestConfig.headers['Content-Type'] = 'application/x-www-form-urlencoded'
		if (requestConfig.method !== 'POST' || info.requestBody) {
			requestConfig.params = params
		} else {
			requestConfig.data = params
		}
		try {
			if (info.requestBody) {
				const json = JSON.parse(info.requestBody)
				if ((Array.isArray(json) && json.length > 0) || (typeof json === 'object' && Object.keys(json).length > 0)) {
					requestConfig.params = params
					requestConfig.data = info.requestBody
					requestConfig.headers['Content-Type'] = 'application/json'
					requestConfig.transformRequest = []
				}
			}
		} catch (e) {
			opened.running = false
			modal.alert($i('api.test.requestBodyError'))
		}

	}
	sendApiTestRequest(opened, requestConfig)
}

export default {
	doTest: doApiTest,
	getIcon: item => item.method || 'GET',
	runnable: true,
	requirePath: true,
	name: $i('api.name'),
	merge: item => {
		item.method = item.method || constants.DEFAULT_REQUEST_METHOD
		item.parameters = item.parameters || []
		item.headers = item.headers || []
		item.paths = item.paths || []
		item.options = item.options || []
		return item
	},
	processSave: item => {
		const persistenceResponseBody = constants.config.persistenceResponseBody !== false
		return {
			id: item.id,
			name: item.name,
			path: item.path,
			groupId: item.groupId,
			lock: item.lock,
			method: item.method,
			description: item.description,
			createBy: item.createBy,
			createTime: item.createTime,
			properties: item.properties,
			script: item.script,
			responseBody: persistenceResponseBody && item.responseBody || undefined,
			responseBodyDefinition: persistenceResponseBody && item.responseBodyDefinition || undefined,
			requestBody: item.requestBody,
			requestBodyDefinition: item.requestBodyDefinition,
			parameters: item.parameters.filter(it => it.name),
			headers: item.headers.filter(it => it.name),
			paths: item.paths.filter(it => it.name),
			options: item.options.filter(it => it.name)
		}
	}
}
