import { defineConfig } from 'vite';

export default defineConfig({
	build: {
		lib: {
			entry: 'src/index.js',
			formats: ['iife'],
			name: 'FwPlusTinyflowUi',
			fileName: () => 'tinyflow-ui.js'
		},
		rollupOptions: {
			output: {
				assetFileNames: (asset) => asset.name && asset.name.endsWith('.css') ? 'tinyflow-ui.css' : '[name][extname]'
			}
		}
	}
});
