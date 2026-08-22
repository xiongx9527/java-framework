export function createTinyflowHost(options) {
	const { Tinyflow, ...tinyflowOptions } = options;
	let instance = new Tinyflow(tinyflowOptions);

	function requireInstance() {
		if (instance === null) {
			throw new Error('Tinyflow host has been destroyed');
		}
		return instance;
	}

	return {
		getData() {
			return requireInstance().getData();
		},
		setData(data) {
			requireInstance().setData(data);
		},
		destroy() {
			if (instance !== null) {
				instance.destroy();
				instance = null;
			}
		}
	};
}

export function createTinyflowMount(OfficialTinyflow) {
	return function mount(options) {
		return createTinyflowHost({ ...options, Tinyflow: OfficialTinyflow });
	};
}
