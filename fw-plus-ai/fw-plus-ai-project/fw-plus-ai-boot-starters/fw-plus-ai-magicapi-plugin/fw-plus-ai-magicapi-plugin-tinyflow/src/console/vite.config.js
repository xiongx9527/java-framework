import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'node:path';
import pkg from './package.json';

export default defineConfig({
	plugins: [vue()],
	build: {
		minify: false,
		outDir: 'dist',
		lib: {
			entry: path.resolve('src/index.js'),
			formats: ['iife'],
			name: 'MagicTinyflow',
			fileName: () => `magic-tinyflow.${pkg.version}.iife.js`
		},
		rollupOptions: {
			external: ['vue'],
			output: { globals: { vue: 'Vue' } }
		}
	}
});
