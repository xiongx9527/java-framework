import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import cssInjectedByJsPlugin from 'vite-plugin-css-injected-by-js'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import pkg from './package.json' with { type: 'json' }

const root = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig({
  base: './',
  resolve: {
    alias: {
      '@': path.resolve(root, 'src/dashboard')
    }
  },
  build: {
    minify: false,
    cssCodeSplit: false,
    outDir: 'dist',
    lib: {
      formats: ['iife'],
      entry: path.resolve(root, 'src/index.js'),
      name: 'MagicMqtt',
      fileName: () => `magic-mqtt.${pkg.version}.iife.js`
    },
    rollupOptions: {
      external: ['vue'],
      output: {
        globals: { vue: 'Vue' }
      }
    }
  },
  plugins: [vue(), cssInjectedByJsPlugin()]
})
