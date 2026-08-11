import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
	plugins: [vue()],
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
