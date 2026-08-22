import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

const dirname = path.dirname(fileURLToPath(import.meta.url))

export default {
    base: './',
    build: {
        minify: false,
        cssCodeSplit: true,
        outDir: 'dist',
        lib: {
            target: 'esnext',
            formats: ['iife'],
            entry: path.resolve(dirname, 'src/index.js'),
            name: 'MagicSpringBean',
            fileName: (format) => `magic-spring-bean.1.0.0.${format}.js`
        }
    },
    plugins: [vue()]
}
