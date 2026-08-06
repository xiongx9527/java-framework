const unwrap = response => {
	if (response && typeof response === 'object' && Object.prototype.hasOwnProperty.call(response, 'code')) {
		if (response.code !== 1) {
			throw new Error(response.message || 'Tinyflow 请求失败');
		}
		return response.data;
	}
	return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response;
};

let runtime = null;

export const setTinyflowRuntime = value => { runtime = value; };
export const getTinyflowRuntime = () => runtime;

const exceptionError = (code, message, response) => {
	if (message instanceof Error) return message;
	if (response instanceof Error) return response;
	if (typeof message === 'string' && message) return new Error(message);
	if (message && message.message) return new Error(message.message);
	if (response && response.message) return new Error(response.message);
	return new Error(code === undefined || code === null ? 'Tinyflow 请求异常' : `Tinyflow 请求异常：${code}`);
};

const sendJson = (request, url, payload) => new Promise((resolve, reject) => {
	if (!request || typeof request.sendJson !== 'function') {
		reject(new Error('Tinyflow 请求服务不可用'));
		return;
	}
	const operation = request.sendJson(url, payload);
	if (operation && typeof operation.then === 'function') {
		operation.then(value => resolve(unwrap(value)), reject);
		return;
	}
	if (!operation || typeof operation.success !== 'function') {
		reject(new Error('Tinyflow 请求服务返回无效结果'));
		return;
	}
	operation.success(value => {
		try {
			resolve(unwrap(value));
		} catch (error) {
			reject(error);
		}
	});
	if (typeof operation.error === 'function') {
		operation.error(error => reject(error instanceof Error ? error : new Error(String(error || 'Tinyflow 请求失败'))));
	}
	if (typeof operation.exception === 'function') {
		operation.exception((code, message, response) => reject(exceptionError(code, message, response)));
	}
});

export function createTinyflowService(request) {
	return {
		get: id => sendJson(request, `/tinyflow/resource/get?id=${encodeURIComponent(id)}`),
		save: resource => sendJson(request, '/tinyflow/resource/save', resource),
		validate: resource => sendJson(request, '/tinyflow/resource/validate', resource),
		run: payload => sendJson(request, '/tinyflow/run', payload),
		nodes: () => sendJson(request, '/tinyflow/nodes'),
		models: () => sendJson(request, '/tinyflow/provider/llm'),
		embeddingModels: () => sendJson(request, '/tinyflow/provider/embedding'),
		vectorStores: () => sendJson(request, '/tinyflow/provider/vector')
	};
}

export const tinyflowUiBaseUrl = value => value && value.constants && value.constants.BASE_URL ? value.constants.BASE_URL : '';

const injectedValue = source => source && typeof source === 'object' && 'value' in source ? source.value : source;

export function createToolbarResourceBinding(info, workbench, opened) {
	let loadedId = null;
	const currentResource = () => injectedValue(info);
	return {
		async sync() {
			if (opened && (!injectedValue(opened) || injectedValue(opened).type !== 'tinyflow')) return false;
			const resource = currentResource();
			const id = resource && resource.id;
			if (!id || id === loadedId) return false;
			const loaded = await workbench.load(id);
			if (loaded) loadedId = id;
			return loaded;
		}
	};
}

export function createWorkbench(options = {}) {
	let resource = null;
	let host = null;
	let data = null;
	let dirty = false;
	let loadGeneration = 0;

	const currentData = () => host && typeof host.getData === 'function' ? host.getData() : data;
	const discardIfNeeded = async () => !dirty || !options.confirmDiscard || await options.confirmDiscard();

	return {
		attach(nextHost) {
			host = nextHost;
			if (data !== null && host && typeof host.setData === 'function') host.setData(data);
		},
		setResource(nextResource) {
			resource = nextResource ? Object.assign({}, nextResource) : null;
		},
		getResource() {
			return resource ? Object.assign({}, resource) : null;
		},
		onDataChange(nextData) {
			data = nextData;
			dirty = true;
		},
		isDirty() {
			return dirty;
		},
		async load(id) {
			const generation = ++loadGeneration;
			if (!await discardIfNeeded() || generation !== loadGeneration) return false;
			if (typeof options.load !== 'function') throw new Error('Tinyflow 资源加载服务不可用');
			let loaded;
			try {
				loaded = await options.load(id);
			} catch (error) {
				if (generation !== loadGeneration) return false;
				throw error;
			}
			if (generation !== loadGeneration) return false;
			if (!loaded || typeof loaded.content !== 'string') throw new Error('Tinyflow 资源内容无效');
			const loadedData = JSON.parse(loaded.content);
			resource = Object.assign({}, loaded);
			data = loadedData;
			if (host && typeof host.setData === 'function') host.setData(loadedData);
			dirty = false;
			return true;
		},
		async save() {
			if (typeof options.save !== 'function') throw new Error('Tinyflow 资源保存服务不可用');
			const payload = Object.assign({}, resource || {}, { content: JSON.stringify(currentData() || {}) });
			await options.save(payload);
			resource = payload;
			dirty = false;
			return payload;
		},
		async run(variables = {}) {
			if (dirty) throw new Error('请先保存 Tinyflow 工作流');
			if (!resource || !resource.id) throw new Error('请先保存 Tinyflow 工作流');
			if (typeof options.run !== 'function') throw new Error('Tinyflow 执行服务不可用');
			return options.run({ resourceId: resource.id, variables });
		},
		async close() {
			if (!await discardIfNeeded()) return false;
			this.destroy();
			return true;
		},
		destroy() {
			loadGeneration += 1;
			if (host && typeof host.destroy === 'function') host.destroy();
			host = null;
		}
	};
}
