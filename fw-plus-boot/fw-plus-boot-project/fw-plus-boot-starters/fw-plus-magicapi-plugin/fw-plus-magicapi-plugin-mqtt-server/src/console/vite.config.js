import vue from '@vitejs/plugin-vue'
import path from 'path'
import pkg from './package.json'
import cssInjectedByJsPlugin from 'vite-plugin-css-injected-by-js'

export default {
    base: './',
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src/dashboard')
        }
    },
    build: {
        minify: false,
        cssCodeSplit: false,
        outDir: 'dist',
        lib: {
            target: 'esnext',
            formats: ['iife'],
            entry: path.resolve(__dirname, 'src/index.js'),
            name: 'MagicMqtt',
            fileName: (format) => `magic-mqtt.${pkg.version}.${format}.js`
        },
        rollupOptions: {
            external: ['vue'],
            output: {
                globals: {
                    vue: 'Vue'
                }
            }
        }
    },
    plugins: [
        vue(),
        cssInjectedByJsPlugin(),
    ]
}