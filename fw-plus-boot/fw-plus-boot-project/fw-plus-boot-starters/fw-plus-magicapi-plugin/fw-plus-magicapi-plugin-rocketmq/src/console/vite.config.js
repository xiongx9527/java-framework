import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import pkg from './package.json';

export default defineConfig({
    plugins: [vue()],
    build: {
        lib: {
            entry: './src/index.js',
            name: 'MagicRocketMq',
            fileName: (format) => `magic-rocketmq.${pkg.version}.${format}.js`,
            formats: ['iife']
        },
        rollupOptions: {
            external: ['vue'],
            output: {
                globals: {
                    vue: 'Vue'
                }
            }
        }
    }
});