import { readFileSync } from 'fs'
import path from 'path'
import { createRequire } from 'module'
import { fileURLToPath } from 'url'
import { defineConfig, loadEnv } from 'vite'

const directory = path.dirname(fileURLToPath(import.meta.url))
const pkg = createRequire(import.meta.url)('./package.json')

const createSymbol = () => {
    const svg = readFileSync(path.resolve(directory, 'src/assets/knife4j.svg'), 'utf8')
    const openingTag = svg.match(/<svg\b[^>]*>/)[0]
    const viewBox = openingTag.match(/\bviewBox=["']([^"']+)["']/)[1]
    const xlinkNamespace = openingTag.match(/\s+xmlns:xlink=["'][^"']+["']/)?.[0] || ''
    const body = svg.slice(svg.indexOf('>', svg.indexOf('<svg')) + 1, svg.lastIndexOf('</svg>'))
    return `<symbol viewBox="${viewBox}"${xlinkNamespace} id="magic-swagger-knife4j">${body}</symbol>`
}

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    return {
        base: './',
        define: {
            __MAGIC_API_BASE_URL__: JSON.stringify(env.MAGIC_API_BASE_URL || './'),
            __MAGIC_SWAGGER_SYMBOL__: JSON.stringify(createSymbol())
        },
        build: {
            minify: false,
            outDir: 'dist',
            lib: {
                entry: path.resolve(directory, 'src/index.js'),
                name: 'MagicSwagger',
                formats: ['iife'],
                fileName: format => `magic-swagger.${pkg.version}.${format}.js`
            }
        }
    }
})
