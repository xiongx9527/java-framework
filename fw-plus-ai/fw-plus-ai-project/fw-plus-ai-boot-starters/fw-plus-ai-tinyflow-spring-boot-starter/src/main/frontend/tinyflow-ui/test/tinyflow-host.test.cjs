const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const { pathToFileURL } = require('node:url');

function fakeTinyflow() {
	let destroyCalls = 0;
	let constructorOptions;

	class FakeTinyflow {
		constructor(options) {
			constructorOptions = options;
			this.data = options.data;
		}

		getData() {
			return this.data;
		}

		setData(data) {
			this.data = data;
		}

		destroy() {
			destroyCalls += 1;
		}
	}

	return {
		ctor: FakeTinyflow,
		get constructorOptions() {
			return constructorOptions;
		},
		get destroyCalls() {
			return destroyCalls;
		}
	};
}

test('mount forwards data and custom nodes and destroy releases its instance', async () => {
	const { createTinyflowHost } = await import(pathToFileURL(path.join(__dirname, '../src/tinyflow-host.js')).href);
	const fake = fakeTinyflow();
	const element = { id: 'tinyflow-host' };
	const data = { nodes: [{ id: 'source' }], edges: [] };
	const nextData = { nodes: [{ id: 'target' }], edges: [] };
	const customNodes = { task: { title: 'Task' } };

	const host = createTinyflowHost({
		Tinyflow: fake.ctor,
		element,
		data,
		customNodes
	});

	assert.equal(fake.constructorOptions.element, element);
	assert.deepEqual(fake.constructorOptions.data, data);
	assert.equal(fake.constructorOptions.customNodes, customNodes);
	host.setData(nextData);
	assert.deepEqual(host.getData(), nextData);
	host.destroy();
	host.destroy();
	assert.equal(fake.destroyCalls, 1);
});
