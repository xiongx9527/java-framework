const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const { pathToFileURL } = require('node:url');

const workbenchModule = () => import(pathToFileURL(path.join(__dirname,
	'../src/service/tinyflow-service.js')).href);

test('keeps dirty state when save fails', async () => {
	const { createWorkbench } = await workbenchModule();
	const workbench = createWorkbench({ save: async () => { throw new Error('500'); } });
	workbench.onDataChange({ nodes: [{ id: 'changed' }] });

	await assert.rejects(workbench.save(), /500/);

	assert.equal(workbench.isDirty(), true);
});

test('requires saving before a dirty workflow can run', async () => {
	const { createWorkbench } = await workbenchModule();
	let runCalls = 0;
	const workbench = createWorkbench({ run: async () => { runCalls += 1; } });
	workbench.onDataChange({ nodes: [{ id: 'changed' }] });

	await assert.rejects(workbench.run(), /请先保存/);

	assert.equal(runCalls, 0);
});

test('loads resource data and confirms before replacing or closing dirty work', async () => {
	const { createWorkbench } = await workbenchModule();
	const calls = { destroyed: 0, setData: [], confirmations: 0 };
	const host = {
		setData(data) { calls.setData.push(data); },
		getData() { return calls.setData[calls.setData.length - 1]; },
		destroy() { calls.destroyed += 1; }
	};
	const workbench = createWorkbench({
		load: async (id) => ({ id, content: '{"nodes":[{"id":"start"}]}' }),
		confirmDiscard: async () => { calls.confirmations += 1; return false; }
	});

	workbench.attach(host);
	await workbench.load('saved-workflow');
	assert.deepEqual(calls.setData, [{ nodes: [{ id: 'start' }] }]);
	assert.equal(workbench.isDirty(), false);

	workbench.onDataChange({ nodes: [{ id: 'unsaved' }] });
	assert.equal(await workbench.load('another-workflow'), false);
	assert.equal(await workbench.close(), false);
	assert.equal(calls.confirmations, 2);
	assert.equal(calls.destroyed, 0);
	assert.equal(workbench.getResource().id, 'saved-workflow');
	assert.equal(workbench.isDirty(), true);
});

test('clears dirty only after save and destroys mounted UI on confirmed close', async () => {
	const { createWorkbench } = await workbenchModule();
	const calls = { saved: [], destroyed: 0 };
	const host = {
		getData() { return { nodes: [{ id: 'saved' }] }; },
		setData() {},
		destroy() { calls.destroyed += 1; }
	};
	const workbench = createWorkbench({
		save: async (resource) => { calls.saved.push(resource); },
		confirmDiscard: async () => true
	});

	workbench.attach(host);
	workbench.setResource({ id: 'saved-workflow', path: 'orders/saved' });
	workbench.onDataChange({ nodes: [{ id: 'edited' }] });
	await workbench.save();
	assert.equal(workbench.isDirty(), false);
	assert.deepEqual(calls.saved[0], {
		id: 'saved-workflow', path: 'orders/saved', content: '{"nodes":[{"id":"saved"}]}'
	});

	assert.equal(await workbench.close(), true);
	assert.equal(calls.destroyed, 1);
});

test('closes a clean workbench directly and requires confirmation only for dirty work', async () => {
	const { createWorkbench } = await workbenchModule();
	let confirmations = 0;
	let destroyed = 0;
	const workbench = createWorkbench({ confirmDiscard: async () => { confirmations += 1; return true; } });
	workbench.attach({ destroy() { destroyed += 1; } });

	assert.equal(await workbench.close(), true);
	assert.equal(confirmations, 0);
	assert.equal(destroyed, 1);

	const dirtyWorkbench = createWorkbench({ confirmDiscard: async () => { confirmations += 1; return true; } });
	dirtyWorkbench.attach({ destroy() { destroyed += 1; } });
	dirtyWorkbench.onDataChange({ nodes: [{ id: 'dirty' }] });
	assert.equal(await dirtyWorkbench.close(), true);
	assert.equal(confirmations, 1);
	assert.equal(destroyed, 2);
});

test('destroys the mounted UI when the workbench unmounts', async () => {
	const { createWorkbench } = await workbenchModule();
	let destroyed = 0;
	const workbench = createWorkbench();
	workbench.attach({ destroy() { destroyed += 1; } });

	workbench.destroy();

	assert.equal(destroyed, 1);
});

test('settles a Magic HttpResponse exception and keeps the workflow dirty', async () => {
	const { createTinyflowService, createWorkbench } = await workbenchModule();
	const service = createTinyflowService({
		sendJson() {
			return {
				success() { return this; },
				error() { return this; },
				exception(callback) { callback(-1, '保存被拒绝', { message: '不应覆盖真实消息' }); return this; }
			};
		}
	});
	const workbench = createWorkbench({ save: resource => service.save(resource) });
	workbench.onDataChange({ nodes: [{ id: 'changed' }] });

	await assert.rejects(workbench.save(), /保存被拒绝/);

	assert.equal(workbench.isDirty(), true);
});

test('reads the current host resource on first open and tree switch without a watcher loop', async () => {
	const { createToolbarResourceBinding, createWorkbench } = await workbenchModule();
	const loaded = [];
	const info = { value: { id: 'first' } };
	const opened = { value: { type: 'tinyflow', item: { id: 'first' } } };
	const workbench = createWorkbench({
		load: async id => { loaded.push(id); return { id, content: '{"nodes":[]}' }; }
	});
	const binding = createToolbarResourceBinding(info, workbench, opened);

	assert.equal(await binding.sync(), true);
	info.value = { id: 'second' };
	opened.value = { type: 'tinyflow', item: { id: 'second' } };
	assert.equal(await binding.sync(), true);
	assert.equal(await binding.sync(), false);

	assert.deepEqual(loaded, ['first', 'second']);
	assert.equal(workbench.getResource().id, 'second');
});

test('reads a Vue-style injected ref whose value getter is on its prototype', async () => {
	const { createToolbarResourceBinding, createWorkbench } = await workbenchModule();
	const refPrototype = {};
	Object.defineProperty(refPrototype, 'value', { get() { return this.current; } });
	const info = Object.create(refPrototype);
	info.current = { id: 'vue-ref-resource' };
	const workbench = createWorkbench({ load: async id => ({ id, content: '{"nodes":[]}' }) });

	assert.equal(await createToolbarResourceBinding(info, workbench).sync(), true);
	assert.equal(workbench.getResource().id, 'vue-ref-resource');
});

test('does not load or confirm dirty work until the host opens a Tinyflow resource', async () => {
	const { createToolbarResourceBinding, createWorkbench } = await workbenchModule();
	const loaded = [];
	let confirmations = 0;
	const info = { value: { id: 'api-resource' } };
	const opened = { value: { type: 'api' } };
	const workbench = createWorkbench({
		load: async id => { loaded.push(id); return { id, content: '{"nodes":[]}' }; },
		confirmDiscard: async () => { confirmations += 1; return false; }
	});
	workbench.onDataChange({ nodes: [{ id: 'dirty' }] });
	const binding = createToolbarResourceBinding(info, workbench, opened);

	assert.equal(await binding.sync(), false);
	opened.value = { type: 'function' };
	assert.equal(await binding.sync(), false);
	assert.deepEqual(loaded, []);
	assert.equal(confirmations, 0);

	opened.value = { type: 'tinyflow', item: { id: 'api-resource' } };
	assert.equal(await binding.sync(), false);
	assert.equal(confirmations, 1);
});

test('ignores OPEN_GROUP info updates while opened still identifies the previous Tinyflow file', async () => {
	const { createToolbarResourceBinding, createWorkbench } = await workbenchModule();
	const loaded = [];
	let confirmations = 0;
	const info = { value: { id: 'workflow-file' } };
	const opened = { value: { type: 'tinyflow', item: { id: 'workflow-file' } } };
	const workbench = createWorkbench({
		load: async id => { loaded.push(id); return { id, content: '{"nodes":[]}' }; },
		confirmDiscard: async () => { confirmations += 1; return false; }
	});
	const binding = createToolbarResourceBinding(info, workbench, opened);

	assert.equal(await binding.sync(), true);
	workbench.onDataChange({ nodes: [{ id: 'dirty' }] });
	info.value = { id: 'tinyflow-group' };
	assert.equal(await binding.sync(), false);
	info.value = { id: 'api-group' };
	assert.equal(await binding.sync(), false);
	info.value = { id: 'other-group' };
	assert.equal(await binding.sync(), false);

	assert.deepEqual(loaded, ['workflow-file']);
	assert.equal(confirmations, 0);
	assert.equal(workbench.getResource().id, 'workflow-file');
});

test('keeps only the newest resource when overlapping loads finish out of order', async () => {
	const { createWorkbench } = await workbenchModule();
	let resolveA;
	let resolveB;
	const painted = [];
	const workbench = createWorkbench({
		load: id => new Promise(resolve => {
			if (id === 'A') resolveA = resolve;
			else resolveB = resolve;
		})
	});
	workbench.attach({ setData: data => painted.push(data) });
	const loadingA = workbench.load('A');
	await Promise.resolve();
	const loadingB = workbench.load('B');
	await Promise.resolve();
	resolveB({ id: 'B', content: '{"nodes":[{"id":"B"}]}' });
	assert.equal(await loadingB, true);
	resolveA({ id: 'A', content: '{"nodes":[{"id":"A"}]}' });
	assert.equal(await loadingA, false);

	assert.equal(workbench.getResource().id, 'B');
	assert.deepEqual(painted, [{ nodes: [{ id: 'B' }] }]);
	assert.equal(workbench.isDirty(), false);
});

test('discards a stale load failure after a newer resource succeeds', async () => {
	const { createWorkbench } = await workbenchModule();
	let rejectA;
	let resolveB;
	const workbench = createWorkbench({
		load: id => new Promise((resolve, reject) => {
			if (id === 'A') rejectA = reject;
			else resolveB = resolve;
		})
	});
	const loadingA = workbench.load('A');
	await Promise.resolve();
	const loadingB = workbench.load('B');
	await Promise.resolve();
	resolveB({ id: 'B', content: '{"nodes":[]}' });
	assert.equal(await loadingB, true);
	rejectA(new Error('A 已过期'));
	assert.equal(await loadingA, false);
	assert.equal(workbench.getResource().id, 'B');
});

test('does not mount or report loader completion after disposal', async () => {
	const { createWorkbench, createWorkbenchMountLifecycle } = await workbenchModule();
	let resolveLoader;
	let rejectLoader;
	let mountCalls = 0;
	let optionCalls = 0;
	const workbench = createWorkbench();
	const lifecycle = createWorkbenchMountLifecycle(workbench);
	const pendingMount = lifecycle.mount(() => new Promise(resolve => { resolveLoader = resolve; }), () => { optionCalls += 1; return { element: 'canvas' }; });
	lifecycle.dispose();
	resolveLoader({ mount() { mountCalls += 1; return { destroy() {} }; } });
	assert.equal(await pendingMount, false);
	assert.equal(optionCalls, 0);
	assert.equal(mountCalls, 0);

	const rejectedLifecycle = createWorkbenchMountLifecycle(createWorkbench());
	const pendingReject = rejectedLifecycle.mount(() => new Promise((resolve, reject) => { rejectLoader = reject; }), () => ({ element: 'canvas' }));
	rejectedLifecycle.dispose();
	rejectLoader(new Error('加载失败'));
	assert.equal(await pendingReject, false);
});

test('destroys a host created while disposal begins before attachment', async () => {
	const { createWorkbench, createWorkbenchMountLifecycle } = await workbenchModule();
	let destroyed = 0;
	const workbench = createWorkbench();
	const lifecycle = createWorkbenchMountLifecycle(workbench);
	const mounted = await lifecycle.mount(async () => ({
		mount() {
			lifecycle.dispose();
			return { destroy() { destroyed += 1; } };
		}
	}), () => ({ element: 'canvas' }));

	assert.equal(mounted, false);
	assert.equal(destroyed, 1);
	assert.equal(workbench.getResource(), null);
});

test('uses the Magic constants base URL for Starter assets', async () => {
	const { tinyflowUiBaseUrl } = await workbenchModule();

	assert.equal(tinyflowUiBaseUrl({ constants: { BASE_URL: '/console' }, request: { baseURL: '/wrong' } }), '/console');
});
