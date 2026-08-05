const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const { pathToFileURL } = require('node:url');

function fakeTinyflow() {
	const instances = [];

	class FakeTinyflow {
		constructor(options) {
			this.data = options.data;
			this.destroyCalls = 0;
			this.options = options;
			instances.push(this);
		}

		getData() {
			return this.data;
		}

		setData(data) {
			this.data = data;
		}

		destroy() {
			this.destroyCalls += 1;
		}
	}

	return {
		ctor: FakeTinyflow,
		instances
	};
}

test('public mount factory always uses the official constructor and isolates host lifecycles', async () => {
	const { createTinyflowMount } = await import(pathToFileURL(path.join(__dirname, '../src/tinyflow-host.js')).href);
	const official = fakeTinyflow();
	const replacement = fakeTinyflow();
	const mount = createTinyflowMount(official.ctor);
	const firstElement = { id: 'first-tinyflow-host' };
	const secondElement = { id: 'second-tinyflow-host' };
	const data = { nodes: [{ id: 'source' }], edges: [] };
	const nextData = { nodes: [{ id: 'target' }], edges: [] };
	const customNodes = { task: { title: 'Task' } };

	const first = mount({
		Tinyflow: replacement.ctor,
		element: firstElement,
		data,
		customNodes
	});
	const second = mount({
		element: secondElement,
		data: nextData,
		customNodes
	});

	assert.equal(official.instances.length, 2);
	assert.equal(replacement.instances.length, 0);
	assert.equal(official.instances[0].options.element, firstElement);
	assert.deepEqual(official.instances[0].options.data, data);
	assert.equal(official.instances[0].options.customNodes, customNodes);
	first.setData(nextData);
	assert.deepEqual(first.getData(), nextData);
	first.destroy();
	first.destroy();
	assert.equal(official.instances[0].destroyCalls, 1);
	assert.equal(official.instances[1].destroyCalls, 0);
	assert.deepEqual(second.getData(), nextData);
});
