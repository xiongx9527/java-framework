const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const { pathToFileURL } = require('node:url');

const loaderModule = () => import(pathToFileURL(path.join(__dirname,
	'../src/service/tinyflow-ui-loader.js')).href);

const documentWith = (onAppend) => ({
	head: {
		appendChild(node) {
			onAppend(node);
			return node;
		}
	},
	createElement(tagName) {
		return { tagName, dataset: {} };
	}
});

test('loads the Starter UI with context-path-aware versioned CSS and JavaScript URLs', async () => {
	const appended = [];
	const browserWindow = {};
	const document = documentWith((node) => {
		appended.push(node);
		if (node.tagName === 'link') node.onload();
		if (node.tagName === 'script') {
			browserWindow.FwPlusTinyflowUi = { version: '1.3.6', mount: () => ({}), mountKnowledge: () => ({}) };
			node.onload();
		}
	});
	const { loadTinyflowUi } = await loaderModule();

	const ui = await loadTinyflowUi({ window: browserWindow, document, baseUrl: '/console' });

	assert.equal(ui, browserWindow.FwPlusTinyflowUi);
	assert.equal(appended[0].href, '/console/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.css');
	assert.equal(appended[1].src, '/console/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.js');
});

test('reports missing global constructor instead of blank canvas', async () => {
	const { loadTinyflowUi } = await loaderModule();

	await assert.rejects(loadTinyflowUi({ window: {}, baseUrl: '/ctx' }), /FwPlusTinyflowUi/);
});

test('reports missing Starter static resource and incompatible UI version', async () => {
	const { loadTinyflowUi } = await loaderModule();
	const missingWindow = {};
	await assert.rejects(loadTinyflowUi({
		window: missingWindow,
		document: documentWith((node) => {
			if (node.tagName === 'link') node.onload();
			if (node.tagName === 'script') node.onerror();
		}),
		baseUrl: '/ctx'
	}), /加载 Tinyflow UI 失败/);

	await assert.rejects(loadTinyflowUi({
		window: { FwPlusTinyflowUi: { version: '1.3.5', mount: () => ({}), mountKnowledge: () => ({}) } },
		baseUrl: '/ctx'
	}), /版本不匹配/);
});

test('reports a missing Starter stylesheet before mounting a blank canvas', async () => {
	const { loadTinyflowUi } = await loaderModule();

	await assert.rejects(loadTinyflowUi({
		window: {},
		document: documentWith((node) => { if (node.tagName === 'link') node.onerror(); }),
		baseUrl: '/console'
	}), /加载 Tinyflow UI 样式失败/);
});

test('rejects a bundled UI without the knowledge mount contract', async () => {
	const { loadTinyflowUi } = await loaderModule();

	await assert.rejects(loadTinyflowUi({
		window: { FwPlusTinyflowUi: { version: '1.3.6', mount: () => ({}) } },
		baseUrl: '/ctx'
	}), /mountKnowledge/);
});
