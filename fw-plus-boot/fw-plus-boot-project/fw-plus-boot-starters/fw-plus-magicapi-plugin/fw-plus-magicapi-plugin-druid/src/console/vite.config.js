import { readFileSync } from 'fs'
import path from 'path'
import { createRequire } from 'module'
import { fileURLToPath } from 'url'
import { defineConfig, loadEnv } from 'vite'

const directory = path.dirname(fileURLToPath(import.meta.url))
const pkg = createRequire(import.meta.url)('./package.json')

const createSymbol = () => {
    const svg = readFileSync(path.resolve(directory, 'src/assets/druid.svg'), 'utf8')
    const openingTag = svg.match(/<svg\b[^>]*>/)[0]
    const viewBox = openingTag.match(/\bviewBox=["']([^"']+)["']/)[1]
    const body = svg.slice(svg.indexOf('>', svg.indexOf('<svg')) + 1, svg.lastIndexOf('</svg>'))
    return `<symbol viewBox="${viewBox}" id="magic-druid-druid">${body}</symbol>`
}

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    return {
        base: './',
        define: {
            __MAGIC_API_BASE_URL__: JSON.stringify(env.MAGIC_API_BASE_URL || './'),
            __MAGIC_DRUID_SYMBOL__: JSON.stringify(createSymbol())
        },
        build: {
            minify: false,
            outDir: 'dist',
            lib: {
                entry: path.resolve(directory, 'src/index.js'),
                name: 'MagicDruid',
                formats: ['iife'],
                fileName: format => `magic-druid.${pkg.version}.${format}.js`
            }
        }
    }
})
