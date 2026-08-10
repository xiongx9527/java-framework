const trim = value => (value || '').replace(/\/+$/, '');
const query = values => Object.entries(values || {})
	.filter(([, value]) => value !== undefined && value !== null && value !== '')
	.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
	.join('&');

const unwrap = response => {
	if (response && typeof response === 'object' && Object.prototype.hasOwnProperty.call(response, 'code')) {
		if (response.code !== 1 && response.code !== 200) {
			throw new Error(response.message || '知识库请求失败');
		}
		return response.data;
	}
	return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response;
};

const operationError = (code, message, response) => {
	if (message instanceof Error) return message;
	if (response instanceof Error) return response;
	return new Error((message && (message.message || message)) || `知识库请求异常：${code || 'unknown'}`);
};

const magicRequest = (request, url, payload) => new Promise((resolve, reject) => {
	const operation = request.sendJson(url, payload || {});
	if (operation && typeof operation.then === 'function') {
		operation.then(value => resolve(unwrap(value)), reject);
		return;
	}
	if (!operation || typeof operation.success !== 'function') {
		reject(new Error('知识库请求服务返回无效结果'));
		return;
	}
	operation.success(value => {
		try { resolve(unwrap(value)); } catch (error) { reject(error); }
	});
	if (typeof operation.error === 'function') operation.error(error => reject(operationError(null, error)));
	if (typeof operation.exception === 'function') operation.exception((code, message, response) => reject(operationError(code, message, response)));
});

export function createKnowledgeApi(options = {}) {
	const baseUrl = trim(options.baseUrl);
	const request = options.request;
	const call = async (path, config = {}) => {
		const suffix = query(config.query);
		const url = `${baseUrl}${path}${suffix ? `?${suffix}` : ''}`;
		if (request && typeof request.sendJson === 'function') return magicRequest(request, path + (suffix ? `?${suffix}` : ''), config.body);
		const response = await (options.fetch || fetch)(url, {
			method: config.method || (config.body ? 'POST' : 'GET'),
			credentials: 'same-origin',
			headers: config.body ? { 'Content-Type': 'application/json' } : undefined,
			body: config.body ? JSON.stringify(config.body) : undefined
		});
		const payload = await response.json().catch(() => null);
		if (!response.ok) throw new Error((payload && payload.message) || `知识库请求失败：HTTP ${response.status}`);
		return unwrap(payload);
	};
	return {
		listKnowledge: (page = 1, size = 20) => call('/tinyflow/knowledge/list', { query: { page, size } }),
		createKnowledge: body => call('/tinyflow/knowledge/create', { method: 'POST', body }),
		updateKnowledge: (knowledgeId, body) => call('/tinyflow/knowledge/update', { method: 'POST', query: { knowledgeId }, body }),
		deleteKnowledge: (knowledgeId, expectedVersion) => call('/tinyflow/knowledge/delete', { method: 'POST', query: { knowledgeId, expectedVersion }, body: {} }),
		importText: body => call('/tinyflow/knowledge/import/text', { method: 'POST', body }),
		importFile: body => call('/tinyflow/knowledge/import/file', { method: 'POST', body }),
		importUrl: body => call('/tinyflow/knowledge/import/url', { method: 'POST', body }),
		listChunks: (knowledgeId, documentId, page = 1, size = 20) => call('/tinyflow/knowledge/chunk/list', { query: { knowledgeId, documentId, page, size } }),
		updateChunk: body => call('/tinyflow/knowledge/chunk/update', { method: 'POST', body }),
		listTasks: (knowledgeId, page = 1, size = 20) => call('/tinyflow/knowledge/task/list', { query: { knowledgeId, page, size } }),
		retryTask: (knowledgeId, taskId) => call('/tinyflow/knowledge/task/retry', { method: 'POST', query: { knowledgeId, taskId }, body: {} }),
		cancelTask: (knowledgeId, taskId) => call('/tinyflow/knowledge/task/cancel', { method: 'POST', query: { knowledgeId, taskId }, body: {} }),
		recall: body => call('/tinyflow/knowledge/recall', { method: 'POST', body }),
		models: () => call('/tinyflow/knowledge/options/model', { query: { limit: 100 } }),
		vectors: () => call('/tinyflow/knowledge/options/vector', { query: { limit: 100 } }),
		s3: () => call('/tinyflow/knowledge/options/s3', { query: { limit: 100 } }),
		createKey: knowledgeId => call('/tinyflow/knowledge/key/create', { method: 'POST', body: { knowledgeId } }),
		listKeys: knowledgeId => call('/tinyflow/knowledge/key/list', { query: { knowledgeId } }),
		revokeKey: (knowledgeId, accessKeyId) => call('/tinyflow/knowledge/key/revoke', { method: 'POST', query: { knowledgeId, accessKeyId }, body: {} }),
		saveDify: body => call('/tinyflow/knowledge/dify/save', { method: 'POST', body }),
		listDify: knowledgeId => call('/tinyflow/knowledge/dify/list', { query: { knowledgeId } }),
		deleteDify: connectionId => call('/tinyflow/knowledge/dify/delete', { method: 'POST', query: { connectionId }, body: {} }),
		testDify: connectionId => call('/tinyflow/knowledge/dify/test', { method: 'POST', query: { connectionId }, body: {} })
	};
}
