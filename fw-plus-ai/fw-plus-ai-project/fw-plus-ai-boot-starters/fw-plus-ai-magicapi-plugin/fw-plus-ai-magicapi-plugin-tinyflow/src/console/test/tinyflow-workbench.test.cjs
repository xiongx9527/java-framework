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
				exception(callback) { callback({ message: '保存被拒绝' }); return this; }
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
	const workbench = createWorkbench({
		load: async id => { loaded.push(id); return { id, content: '{"nodes":[]}' }; }
	});
	const binding = createToolbarResourceBinding(info, workbench);

	assert.equal(await binding.sync(), true);
	info.value = { id: 'second' };
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

test('uses the Magic constants base URL for Starter assets', async () => {
	const { tinyflowUiBaseUrl } = await workbenchModule();

	assert.equal(tinyflowUiBaseUrl({ constants: { BASE_URL: '/console' }, request: { baseURL: '/wrong' } }), '/console');
});
