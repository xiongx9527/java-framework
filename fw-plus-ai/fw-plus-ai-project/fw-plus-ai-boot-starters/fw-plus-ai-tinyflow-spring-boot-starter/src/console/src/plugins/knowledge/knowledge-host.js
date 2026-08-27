const LOCAL_ACTIONS = new Set([
	'create', 'upload', 'import-text', 'import-url', 'rebuild', 'delete',
	'update-chunk', 'retry', 'cancel', 'create-key', 'revoke-key'
]);
const DIFY_ACTIONS = new Set(['save-connection', 'test-connection']);

function providerKey(provider) {
	return provider && provider.key ? provider.key : 'fw-plus-local';
}

export function availableActions(provider) {
	const key = providerKey(provider);
	if (key === 'dify' || (provider && provider.readOnly)) {
		return DIFY_ACTIONS;
	}
	return LOCAL_ACTIONS;
}

export function createKnowledgeMount(createApp, KnowledgeApp) {
	if (typeof createApp !== 'function') {
		throw new Error('Vue createApp is required');
	}
	return function mountKnowledge(options = {}) {
		if (!options.element) {
			throw new Error('Knowledge host element is required');
		}
		const actions = availableActions(options.provider);
		const app = createApp(KnowledgeApp, { options });
		app.mount(options.element);
		let destroyed = false;
		return {
			findAction(id) {
				return actions.has(id) ? { id, enabled: true } : null;
			},
			destroy() {
				if (!destroyed) {
					app.unmount();
					destroyed = true;
				}
			}
		};
	};
}
