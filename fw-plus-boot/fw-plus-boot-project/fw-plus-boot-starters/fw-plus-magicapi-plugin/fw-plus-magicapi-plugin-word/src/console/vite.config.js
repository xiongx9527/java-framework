import vue from '@vitejs/plugin-vue'
import viteSvgIcons from 'vite-plugin-svg-icons'
import path from 'path'
import pkg from './package.json'

export default {
    base: './',
    build: {
        minify: 'terser',
        cssCodeSplit: true, // 将组件的 style 打包到 js 文件中
        outDir: 'dist',
        lib: {
            target: 'esnext',
            formats: ['iife'],
            entry: path.resolve(__dirname, 'src/index.js'),
            name: 'MagicWord',
            fileName: (format) => `magic-word.${pkg.version}.${format}.js`
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
        viteSvgIcons({
            iconDirs: [path.resolve(process.cwd(), 'src/icons')],
            symbolId: 'magic-word-[name]'
        }),
    ]
}
