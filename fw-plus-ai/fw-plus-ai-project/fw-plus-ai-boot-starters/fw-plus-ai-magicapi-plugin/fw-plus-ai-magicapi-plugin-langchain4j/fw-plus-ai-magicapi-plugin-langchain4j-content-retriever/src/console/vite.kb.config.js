import vue from '@vitejs/plugin-vue'
import path from 'path'
import pkg from './package.json'

// 知识库管理页独立产物：后端在知识库装配成功时才声明该插件（magic-kb-browser.1.0.0.iife.js），
// 未装配时控制台不加载本 js，toolbars 入口不出现（不启动，不激活）。
export default {
    base: './',
    build: {
        minify: false,
        cssCodeSplit: true,
        outDir: 'dist',
        // 与主产物（content-retriever iife.js）共存于同一 dist 目录，二次构建不清空
        emptyOutDir: false,
        lib: {
            target: 'esnext',
            formats: ['iife'],
            entry: path.resolve(__dirname, 'src/index-kb.js'),
            name: 'MagicKbBrowser',
            fileName: (format) => `magic-kb-browser.${pkg.version}.${format}.js`
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
