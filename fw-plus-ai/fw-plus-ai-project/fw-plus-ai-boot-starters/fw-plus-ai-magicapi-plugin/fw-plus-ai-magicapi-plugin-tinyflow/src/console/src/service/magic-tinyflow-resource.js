export default function MagicTinyflowResource(bus, constants, $i) {
	return {
		getIcon: () => ['TF', '#2f80ed'],
		name: $i('tinyflow.resource.name'),
		language: 'json',
		defaultScript: '{\n  "nodes": [],\n  "edges": []\n}',
		runnable: false,
		requirePath: true,
		merge: item => item
	};
}
