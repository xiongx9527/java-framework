const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const { pathToFileURL } = require('node:url');

const repoRoot = path.resolve(__dirname, '../../../../../../../../.');
const toolbarCloseModule = () => import(pathToFileURL(path.join(repoRoot,
	'fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar-close.js')).href);

test('Magic toolbar keeps a dirty plugin open when its exposed beforeClose rejects', async () => {
	const { requestToolbarClose } = await toolbarCloseModule();
	let removed = 0;
	const toolbar = { beforeClose: instance => instance.beforeClose() };

	const closed = await requestToolbarClose(toolbar, { beforeClose: async () => false }, () => { removed += 1; });

	assert.equal(closed, false);
	assert.equal(removed, 0);
});

test('Magic toolbar removes a clean plugin after its exposed beforeClose accepts', async () => {
	const { requestToolbarClose } = await toolbarCloseModule();
	let removed = 0;
	const toolbar = { beforeClose: instance => instance.beforeClose() };

	const closed = await requestToolbarClose(toolbar, { beforeClose: async () => true }, () => { removed += 1; });

	assert.equal(closed, true);
	assert.equal(removed, 1);
});

test('Magic toolbar preserves existing direct-close behavior when no close hook exists', async () => {
	const { requestToolbarClose } = await toolbarCloseModule();
	let removed = 0;

	assert.equal(await requestToolbarClose({}, null, () => { removed += 1; }), true);
	assert.equal(removed, 1);
});

test('Magic toolbar rejects a throwing hook and suppresses repeated close clicks', async () => {
	const { requestToolbarClose } = await toolbarCloseModule();
	let removed = 0;
	let release;
	const pending = new Promise(resolve => { release = resolve; });
	const toolbar = { beforeClose: async () => pending };

	const first = requestToolbarClose(toolbar, {}, () => { removed += 1; });
	const second = requestToolbarClose(toolbar, {}, () => { removed += 1; });
	release(true);

	assert.equal(await first, true);
	assert.equal(await second, false);
	assert.equal(removed, 1);
	assert.equal(await requestToolbarClose({ beforeClose: () => { throw new Error('拒绝关闭'); } }, {}, () => { removed += 1; }), false);
	assert.equal(removed, 1);
});

test('Magic toolbar removes the intended tab after asynchronous close and reordering', async () => {
	const { removeToolbarByIdentity, requestToolbarClose } = await toolbarCloseModule();
	let releaseFirst;
	let releaseSecond;
	const first = { beforeClose: () => new Promise(resolve => { releaseFirst = resolve; }) };
	const second = { beforeClose: () => new Promise(resolve => { releaseSecond = resolve; }) };
	const toolbars = [first, second];

	const closeFirst = requestToolbarClose(first, {}, () => removeToolbarByIdentity(toolbars, first));
	const closeSecond = requestToolbarClose(second, {}, () => removeToolbarByIdentity(toolbars, second));
	toolbars.reverse();
	removeToolbarByIdentity(toolbars, second);
	releaseSecond(true);
	releaseFirst(true);

	assert.equal(await closeSecond, false);
	assert.equal(await closeFirst, true);
	assert.deepEqual(toolbars, []);
});

test('built Tinyflow plugin exposes the current-resource workflow toolbar and close hook', () => {
	const bundlePath = path.join(__dirname, '../dist/magic-tinyflow.1.0.0.iife.js');
	const inserted = [];
	const document = {
		getElementById: () => null,
		createElementNS: () => ({ style: {}, setAttribute() {} }),
		body: { firstChild: null, insertBefore: node => inserted.push(node) }
	};
	const context = {
		document,
		window: {},
		Vue: { defineComponent: value => value, computed: value => value, inject: () => null, onBeforeUnmount() {}, onMounted() {}, ref: value => ({ value }), watch() {} }
	};
	vm.runInNewContext(fs.readFileSync(bundlePath, 'utf8'), context);
	const plugin = context.MagicTinyflow({
		bus: {}, constants: { BASE_URL: '/console' }, Message: {}, request: {},
		i18n: { add() {}, format: key => key }
	});

	assert.equal(plugin.resource[0].type, 'tinyflow');
	assert.equal(plugin.toolbars[0].type, 'tinyflow');
	assert.equal(plugin.toolbars[0].allowClose, true);
	assert.equal(typeof plugin.toolbars[0].beforeClose, 'function');
	assert.equal(plugin.toolbars[1].id, 'tinyflow-knowledge');
	assert.equal(inserted.length, 1);
});
