import path from 'path'
import { createRequire } from 'module'
import { fileURLToPath } from 'url'
import { defineConfig, loadEnv } from 'vite'

const directory = path.dirname(fileURLToPath(import.meta.url))
const pkg = createRequire(import.meta.url)('./package.json')

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    return {
        base: './',
        define: {
            __MAGIC_API_BASE_URL__: JSON.stringify(env.MAGIC_API_BASE_URL || './')
        },
        build: {
            minify: false,
            outDir: 'dist',
            lib: {
                entry: path.resolve(directory, 'src/index.js'),
                name: 'MagicTinyEngine',
                formats: ['iife'],
                fileName: format => `magic-tiny-engine.${pkg.version}.${format}.js`
            }
        }
    }
})
