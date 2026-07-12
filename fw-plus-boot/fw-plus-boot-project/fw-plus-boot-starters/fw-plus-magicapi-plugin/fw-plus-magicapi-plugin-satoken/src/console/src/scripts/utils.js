import $i from './i18n.js'
import Beautifier from './beautifier/javascript/beautifier.js'
/**
 * 排序数组对象
 * @param {*} arr 排序的数据
 * @param {*} reverse 是否倒序
 * @param {*} field 排序的字段
 * @param {*} childField 子元素排序的字段
 * @returns
 */
export function sortArrayObject(arr, reverse, field, childField) {
	if (arr && arr.length > 0 && field) {
		function sortItem(item1, item2) {
			return item1[field].localeCompare(item2[field], 'zh-CN')
		}
		// 分组
		let folderArr = []
		// 对象
		let fileArr = []
		arr.forEach((element) => {
			if (element.folder === true) {
				if (childField) {
					element[childField] = sortArrayObject(element[childField], reverse, field, childField)
				}
				folderArr.push(element)
			} else {
				fileArr.push(element)
			}
		})
		folderArr.sort(sortItem)
		fileArr.sort(sortItem)
		if (reverse === false) {
			folderArr.reverse()
			fileArr.reverse()
		}
		arr.splice(0, arr.length, ...folderArr.concat(fileArr))
	}
	return arr
}
export function randomString(len) {
	return Array(len).fill(0).map(() => (Math.random() * 16 | 0).toString(16)).join('')
}
export function paddingZero(val, len) {
	len = len || 2
	return (Array(len).join(0) + val).slice(-len)
}
export function formatDate(val) {
	if (typeof val === 'number') {
		if (val.toString().length === 13) {
			val = new Date(val)
		} else {
			val = new Date(val * 1000)
		}
	}
	if (val instanceof Date) {
		var month = val.getMonth() + 1;
		var day = val.getDate();
		var hour = val.getHours();
		var minute = val.getMinutes();
		var seconds = val.getSeconds();
		return val.getFullYear() + '-' + paddingZero(month) + '-' + paddingZero(day) + ' ' + paddingZero(hour) + ':' + paddingZero(minute) + ':' + paddingZero(seconds);
	}
	return '';
}

export function replaceURL(url) {
	return url.replace(/:?\/+/g, e => e.indexOf(':') > -1 ? e : '/');
}
export function convertVariables(variables) {
	const convertObject = data => {
		if (Array.isArray(data)) {
			let len = data.length;
			let chunkSize = 100;
			if (len > chunkSize) {
				let ranges = [];
				for (let index = 0; index < len; index += chunkSize) {
					ranges.push({
						name: `[${index}...${Math.min(index + chunkSize, len) - 1}]`,
						folder: true,
						opened: false,
						dataType: 'array',
						type: 'Array',
						children: data.slice(index, Math.min(index + chunkSize, len)).map((v, i) => convert('' + (index + i), undefined, JSON.stringify(v), v))
					})
				}
				return ranges;
			}
			return data.map((v, index) => convert('' + index, undefined, JSON.stringify(v), v))
		} else {
			return Object.keys(data).map(key => {
				const value = data[key]
				return convert(key, undefined, JSON.stringify(value), value)
			})
		}
	}
	const convertToJson = value => {
		try {
			return JSON.parse(value)
		} catch (e) {
			return value
		}
	}
	const convert = (name, type, value, data) => {
		let displayText = (type || '').startsWith('java.lang')
		let dataType
		let children
		let size
		if (!displayText) {
			data = data || (value && convertToJson(value))
			if (!Array.isArray(data) && typeof data !== 'object') {
				displayText = true
				dataType = typeof data
			} else {
				if (Array.isArray(data)) {
					size = `size = ${data.length}`
					dataType = 'array'
				} else {
					size = `members = ${data && Object.keys(data).length}`
					dataType = 'object'
				}
				type = type || (Array.isArray(data) ? `Array` : typeof data)
				children = data && convertObject(data) || []
			}
		} else {
			dataType = type?.substring(10)?.toLowerCase() || typeof data
			if (['integer', 'double', 'float', 'byte', 'short', 'long'].indexOf(dataType) > -1) {
				dataType = 'number'
			}
			if (dataType === 'class') {
				value = value.substring(1, value.length - 1)
			}
		}
		let isNull = data == null && !displayText
		if (isNull) {
			type = 'null'
			displayText = true
			value = 'null'
		}
		return {
			name,
			value,
			data,
			dataType,
			type,
			size,
			isNull,
			folder: !displayText,
			opened: false,
			children
		}
	}
	return variables && variables.map(it => convert(it.name, it.type, it.value)) || []
}
export function getSizeUnit(dataLen) {
	const unit = ['B', 'KB', 'MB'];
	let index = 0;
	while (index < unit.length && dataLen >= 1024) {
		dataLen = dataLen / 1024
		index++;
	}
	dataLen = dataLen.toFixed(2);
	return `${dataLen} ${unit[index]}`
}

export function processTree(tree, fn) {
	const process = list => list.filter(it => it.folder).forEach(it => {
		process(it.children || []);
		fn(it)
	})
	process(tree || [])
}


export function download(blob, filename) {
	let element = document.createElement('a')
	let href = window.URL.createObjectURL(blob);
	element.href = href;
	element.download = filename;
	document.body.appendChild(element);
	element.click();
	document.body.removeChild(element);
	window.URL.revokeObjectURL(href)
}

export function copyToClipboard(value) {
	try {
		var copyText = document.createElement('textarea')
		copyText.style = 'position:absolute;left:-99999999px'
		document.body.appendChild(copyText)
		copyText.innerHTML = value
		copyText.readOnly = false
		copyText.select()
		document.execCommand('copy')
		return true
	} catch (e) {
		return false
	}
}

export function hash(str) {
	let h = 0;
	for (let i = 0, offset = 0, len = str.length; i < len; i++) {
		h = 31 * h + str.charCodeAt(offset++);
	}
	return h;
}

export function definition2TreeList(definition) {
	let treeList = []
	const appendNode = (node, level) => {
		const item = {
			level,
			node,
			name: level === 0 ? $i('message.root') : node.name,
			folder: node?.dataType === 'Object' || node?.dataType === 'Array',
			display: true
		}
		if (item.folder) {
			item.expand = true
		}
		treeList.push(item)
	}
	const appendChild = (items, level) => {
		(items || []).forEach(node => {
			appendNode(node, level)
			appendChild(node.children, level + 1)
		})
	}
	appendNode(definition || {}, 0)
	appendChild(definition?.children || [], 1)
	return treeList
}

export function loadPlugin(url) {
	return new Promise((resolve, reject) => {
		const head = document.getElementsByTagName('head')[0];
		const element = document.createElement('script');
		element.setAttribute('type', 'text/javascript');
		element.src = url;
		head.appendChild(element);
		if (element.readyState) {
			element.onreadystatechange = () => {
				if (element.readyState === 'loaded' || element.readyState === 'complete') {
					element.onreadystatechange = null;
					resolve();
				}
			};
		} else {
			element.onload = function(){
				resolve()
			}
			element.onerror = function(){
				reject()
			}
		}
	})
}
export function rand(n, m){
	return Math.floor(Math.random()*(m-n+1)+n)
}

export function formatJson(val, defaultVal) {
    if (val) {
        if (typeof val == 'string') {
            return new Beautifier(val).beautify()
        }
        if (val) {
            return JSON.stringify(val, null, 4);
        }
    }
    return defaultVal || ''
};

export function structureHasChanged (obj, newObj){
    if ((!obj && newObj) || (!newObj && obj)) {
        return true
    }
    if (obj.dataType !== newObj.dataType || obj.name !== newObj.name || obj?.children.length !== newObj.children.length) {
        return true
    }
    for(let i = 0, len = obj.children.length; i < len; i++){
        if (structureHasChanged(obj.children[i], newObj.children[i])) {
            return true
        }
    }
    return false
}