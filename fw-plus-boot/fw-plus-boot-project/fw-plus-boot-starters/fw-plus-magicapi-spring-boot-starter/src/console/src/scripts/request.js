import axios from 'axios'
import Qs from 'qs'
import constants from './constants.js'
import modal from '../components/common/dialog/magic-modal.js'
import { default as $i, translateCode } from './i18n.js'
import bus from './bus.js'
import Message from './constants/message.js'

// 全局请求计数器
let requestCount = 0
const loadingListeners = new Set()

// 添加loading状态监听
function addLoadingListener(listener) {
    loadingListeners.add(listener)
}

// 移除loading状态监听
function removeLoadingListener(listener) {
    loadingListeners.delete(listener)
}

// 更新loading状态
function updateLoadingState(loading) {
    loadingListeners.forEach(listener => {
        try {
            listener(loading)
        } catch (e) {
            console.error('Loading listener error:', e)
        }
    })
}

// 开始请求
function startRequest() {
    requestCount++
    if (requestCount === 1) {
        updateLoadingState(true)
    }
}

// 结束请求
function endRequest() {
    requestCount = Math.max(0, requestCount - 1)
    if (requestCount === 0) {
        updateLoadingState(false)
    }
}

// 检查是否需要显示loading
function shouldShowLoading(requestConfig) {
    // 特殊情况：不显示loading
    if (requestConfig.showLoading === true) {
        return true
    }
    // 检查请求头中是否包含 Magic-Request-Client-Id 和 Magic-Request-Script-Id（不区分大小写）
    if (requestConfig.headers) {
        let hasBreakpoints = false
        for (let key in requestConfig.headers) {
			if (key === 'Magic-Request-Breakpoints' || key === 'magic-request-breakpoints') {
				hasBreakpoints = true;
				break ;
			}
		}
		if (hasBreakpoints ==  true) {
			return false;
		}
    }
	if(requestConfig.url) {
		// 检查URL是否以/test或/save结尾（去除查询参数部分）
		var url = requestConfig.url.split('?')[0];
		const allowedPaths = ['/test', '/save'];
		if (url && allowedPaths.some(path => url.endsWith(path))) {
			return true;
		}
	}
	// 默认不显示loading
    return false
}
const config = {
	// 请求路径
	baseURL: '',
	// 默认请求方法
	method: 'post',
	// 请求超时时间（毫秒）
	timeout: 0,
	// 是否携带cookie信息
	withCredentials: true,
	// 响应格式,可选项 'arraybuffer', 'blob', 'document', 'json', 'text', 'stream'
	responseType: 'json',
	// 自定义添加头部
	headers: {
		// ;charset=UTF-8
		'Content-Type': 'application/x-www-form-urlencoded'
	},
	// `transformRequest` 允许在向服务器发送前，修改请求数据
	// 只能用在 'PUT', 'POST' 和 'PATCH' 这几个请求方法
	// 后面数组中的函数必须返回一个字符串，或 ArrayBuffer，或 Stream
	transformRequest: [
		function (data) {
			if(data instanceof FormData){
				return data;
			}
			return Qs.stringify(data, {
				// a:[1,2] => a=1&a=2
				arrayFormat: 'repeat',
				// a[b]:1 => a.b=1
				allowDots: true
			})
		}
	],
	paramsSerializer(data) {
		return Qs.stringify(data, {
			// a:[1,2] => a=1&a=2
			arrayFormat: 'repeat',
			// a[b]:1 => a.b=1
			allowDots: true
		})
	}
}
class HttpResponse {
	// 请求成功调用 code == 1的
	successHandle = null
	// 请求异常，包含js异常调用
	errorHandle = null

	endHandle = null

	constructor() {
	}

	// 异常回调，实际上的code != 1的
	exceptionHandle = (code, message) => {
		modal.alert(translateCode(code, message), $i('code.error', code))
	}

	// 调用返回成功需要注入的变量
	success(handle) {
		this.successHandle = handle
		return this
	}

	exception(handle) {
		this.exceptionHandle = handle
		return this
	}

	// 调用异常需要注入的变量
	error(handle) {
		this.errorHandle = handle
		return this
	}

	end(handle) {
		this.endHandle = handle;
	}
}

class HttpRequest {
	_axios = null

	constructor() {
		this._axios = axios.create(config)
	}

	// 返回初始化过后的axios
	getAxios() {
		return this._axios
	}

	setBaseURL(baseURL) {
		config.baseURL = baseURL
	}

	execute(requestConfig) {
		let _config = {
			baseURL: config.baseURL,
			...requestConfig
		}
		_config.headers = _config.headers || {};
		_config.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE
		
		// 动态控制loading显示
		const showLoading = shouldShowLoading(requestConfig)
		if (showLoading) {
			startRequest()
		}
		
		return this._axios.request(_config).finally(() => {
			if (showLoading) {
				endRequest()
			}
		});
	}

	processError(error) {
		if (error.response) {
			modal.alert(JSON.stringify(error.response?.data || '') || $i('code.invalid', error.response?.status), $i('code.invalid', error.response?.status))
		} else {
			modal.alert(error.message, $i('code.httpError'))
		}
		console.error(error)
	}
	sendJson(url, json, config) {
		return this.send(url, JSON.stringify(json), config || {
			method: 'post',
			headers: {
				'Content-Type': 'application/json'
			},
			transformRequest: []
		})
	}
	sendGet(url, params, newConfig) {
		newConfig = newConfig || {}
		newConfig.method = 'get'
		return this.send(url, params, newConfig)
	}
	sendPost(url, data, newConfig) {
		newConfig = newConfig || {}
		newConfig.method = 'post'
		return this.send(url, data, newConfig)
	}
	// 发送默认请求
	send(url, params, newConfig) {
		let requestConfig = {
			...config,
			...newConfig,
			url: url
		}
		if ((requestConfig.method || '').toLowerCase() === 'post') {
			requestConfig.data = params
		} else {
			requestConfig.params = params
		}
		requestConfig.baseURL = config.baseURL
		let httpResponse = new HttpResponse()
		let successed = false;
		let processResult = (data, response) => {
			if(data instanceof Blob){
				successed = true
				httpResponse.successHandle && httpResponse.successHandle(data, response)
			} else if (data.code === 1) {
				successed = true
				httpResponse.successHandle && httpResponse.successHandle(data.data, response)
			} else {
				if (data.code === 401) {
					bus.$emit(Message.SHOW_LOGIN)
				}
				httpResponse.exceptionHandle && httpResponse.exceptionHandle(data.code, data.message, response)
			}
		}
		
		// 动态控制loading显示
		const showLoading = shouldShowLoading(requestConfig)
		if (showLoading) {
			startRequest()
		}
		
		this.execute(requestConfig)
			.then(response => {
				let data = response.data
				let isJson = response.headers['content-type'] && response.headers['content-type'].startsWith('application/json')
				if(data instanceof Blob && isJson){
					let reader = new FileReader()
					reader.readAsText(data)
					reader.onload = function() {
						try{
							data = JSON.parse(this.result)
							processResult(data, response)
						}catch(e){
							console.error(e);
							processResult(data, response)
						}
					}
					return;
				}
				processResult(data, response)
			})
			.catch((error) => {
				if (typeof httpResponse.errorHandle === 'function') {
					httpResponse.errorHandle(error.response?.data, error.response, error)
				} else {
					this.processError(error)

				}
			})
			.finally(() => {
				if (showLoading) {
					// 结束请求，隐藏loading
				    endRequest()
				}
				if (typeof httpResponse.endHandle === 'function') {
					httpResponse.endHandle(successed)
				}
			})
		return httpResponse
	}
}

export default new HttpRequest()

// 导出loading相关函数
export { addLoadingListener, removeLoadingListener }
