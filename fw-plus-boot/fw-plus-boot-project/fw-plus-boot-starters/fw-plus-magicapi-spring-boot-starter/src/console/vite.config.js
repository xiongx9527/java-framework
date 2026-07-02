import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import viteSvgIcons from 'vite-plugin-svg-icons'
import { MonacoEditorNls } from './src/plugins/monaco-editor-nls.js'
import { MonacoExcludeLanguages } from './src/plugins/monaco-exclude-languages.js'
import path from 'path'
const buildIndex = () => {
	return {
		name: 'html-transform',
		transformIndexHtml(html) {
			return html.replace(/@VERSION@/g, require('./package.json').version)
		}
	}
}
// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')
  
  // 环境相关配置
  const isDev = mode === 'development'
  const baseURL = isDev ? 'http://127.0.0.1:3089/magic-api/web' : './'
  const serverURL = isDev ? 'http://127.0.0.1:3089/' : './'
  
  // 定义环境变量
  const envVars = {
    'import.meta.env.MAGIC_API_BASE_URL': JSON.stringify(baseURL),
    'import.meta.env.MAGIC_API_SERVER_URL': JSON.stringify(serverURL),
    'import.meta.env.MAGIC_API_IN_JAR': JSON.stringify(true)
  }
  
  return {
	base: './',
	optimizeDeps: {
		// 强制预构建Vue和Monaco Editor
		include: ['vue', 'monaco-editor/esm/vs/editor/editor.main.js'],
		// 排除Monaco Editor的某些模块，避免重复构建
		exclude: ['monaco-editor/esm/vs/basic-languages', 'monaco-editor/esm/vs/language'],
		esbuildOptions: {
			plugins: [
				MonacoEditorNls(true),
				MonacoExcludeLanguages(true)
			],
			// 优化构建选项
			target: 'es2020',
			platform: 'browser'
		}
	},
	server: {
		host: true,
		port: 3000,
		open: true,
		// 增加超时设置
		hmr: {
			timeout: 30000
		},
		// 优化依赖预构建
		watch: {
			usePolling: true,
			interval: 1000
		}
	},
	build: {
		cssCodeSplit: false,
		rollupOptions: {
			// 屏蔽不需要的语言
			output: {
				minifyInternalExports: true,
				manualChunks: (id, { getModuleInfo }) => {
					if (id.includes('node_modules')) {
						if(id.includes('axios')){
							return 'axios'
						}
						if(id.includes('vue')){
							return 'vue'
						}
						return 'vendor';
					} else if(id.includes('/src/')){
						return 'app'
					}
				}
			},
		}
	},
	plugins: [
		vue(),
		viteSvgIcons({
			iconDirs: [path.resolve(process.cwd(), 'src/icons')],
			symbolId: 'magic-icon-[name]'
		}),
		buildIndex(),
		MonacoEditorNls(),
		MonacoExcludeLanguages()
	],
	// 定义环境变量
	define: envVars
}
})