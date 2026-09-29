import vue from '@vitejs/plugin-vue'
import path from 'path'
import pkg from './package.json'

export default {
    base: './',
    build: {
        minify: false,
        cssCodeSplit: true,
        outDir: 'dist',
        lib: {
            target: 'esnext',
            formats: ['iife'],
            entry: path.resolve(__dirname, 'src/index.js'),
            name: 'MagicLangchain4jSystemMessage',
            fileName: (format) => `magic-langchain4j-system-message.${pkg.version}.${format}.js`
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
    ]
}
