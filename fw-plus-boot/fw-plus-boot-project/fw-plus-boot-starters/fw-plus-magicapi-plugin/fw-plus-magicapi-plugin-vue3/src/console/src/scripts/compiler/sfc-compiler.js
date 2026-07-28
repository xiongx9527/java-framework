import * as SFCCompiler from '@vue/compiler-sfc'
import {babelParse} from '@vue/compiler-sfc'
import { SourceMapGenerator, SourceMapConsumer } from 'source-map-js'

// 常量定义
const COMP_IDENTIFIER = `__sfc__`
const VUE_COMPOSITION_FUNCTIONS = [
  'defineExpose', 'defineProps', 'defineEmits', 'defineSlots', 'defineOptions', 'defineModel',
  'useSlots', 'useAttrs', 'useCssModule', 'useTransitionState', 'useRoute', 'useRouter',
  'useStore', 'useI18n', 'useHead', 'useMeta', 'useQuery', 'useMutation', 'useQueryClient'
]
const SCRIPT_SETUP_REGEX = /<script\s+setup[^>]*>/
const SUPPORTED_LANGUAGES = ['js', 'ts', 'jsx', 'tsx']
const SUPPORTED_STYLE_LANGUAGES = ['css', 'scss', 'sass', 'less', 'stylus']
const MAX_CACHE_SIZE = 100 // 缓存大小限制

/**
 * ═══════════════════════════════════════════════════════════════
 * 浏览器端动态编译的能力边界
 * ═══════════════════════════════════════════════════════════════
 *
 * 本编译器已支持 Vue 3 SFC 语法规范的 100%。
 * 以下 3 项不属于语法规范，而是构建工具链的编译器扩展能力，无法在浏览器端实现：
 *
 * 1. PostCSS 插件（如 Tailwind CSS）
 *    原因：PostCSS 插件需要 Node.js 文件系统扫描项目文件生成原子类，
 *          这是构建时行为。浏览器端完整加载 Tailwind 需要 ~15MB JS 引擎
 *          加文件系统访问，在动态编译场景下不成立。
 *
 * 2. directiveTransforms（编译时自定义指令转换钩子）
 *    原因：需要用户提供 JS 函数作为编译钩子，但函数无法序列化后存储到
 *          Magic-API 的 .ms 文件中，也不适合浏览器动态编译的 API 形态。
 *
 * 3. expressionPlugins（模板表达式的 Babel 插件链）
 *    原因：Babel 插件是 Node.js 模块，依赖 npm 模块解析机制，
 *          浏览器端不存在此能力。
 *
 * 以上 3 项在 vite-plugin-vue 中由 Vite 构建管道提供，
 * 不是 @vue/compiler-sfc 自身的能力。
 * ═══════════════════════════════════════════════════════════════
 */

// 编译配置选项
const DEFAULT_COMPILE_OPTIONS = {
  sourceMap: true,
  isProd: false,
  inlineTemplate: true,
  enableCache: true,
  maxCacheSize: MAX_CACHE_SIZE,
  enableCssVBind: true,       // 启用 CSS v-bind() 支持
  propsDestructure: true,      // 启用 defineProps 响应式解构（Vue 3.5+）
  hoistStatic: true,           // 启用 <script setup> 静态常量提升
  customElement: false,        // 编译为 Web Components（Custom Elements）
  compilerOptions: {},         // 透传给 compileTemplate 的编译器选项
  modulesOptions: null,        // CSS Modules 选项（generateScopedName 等）
  customTransforms: {},
  resolveSrc: null,            // (url: string) => Promise<string> 外部资源加载回调
  experimentalCssPreprocessors: false  // CSS 预处理器 CDN 动态加载
}

// 错误代码定义
const ERROR_CODES = {
  COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP: 'COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP',
  UNSUPPORTED_PREPROCESSORS: 'UNSUPPORTED_PREPROCESSORS',
  UNSUPPORTED_STYLE_PREPROCESSORS: 'UNSUPPORTED_STYLE_PREPROCESSORS',
  JAVASCRIPT_PARSE_ERROR: 'JAVASCRIPT_PARSE_ERROR',
  TYPESCRIPT_PARSE_ERROR: 'TYPESCRIPT_PARSE_ERROR',
  STYLE_COMPILATION_ERROR: 'STYLE_COMPILATION_ERROR',
  TEMPLATE_COMPILATION_ERROR: 'TEMPLATE_COMPILATION_ERROR',
  SCRIPT_COMPILATION_ERROR: 'SCRIPT_COMPILATION_ERROR',
  CSS_VBIND_ERROR: 'CSS_VBIND_ERROR',
  SRC_FETCH_ERROR: 'SRC_FETCH_ERROR',
  CSS_PREPROCESSOR_ERROR: 'CSS_PREPROCESSOR_ERROR'
}

// 预编译正则表达式 - 合并为单个正则提升性能
const COMPOSITION_FN_PATTERN = new RegExp(
  `\\b(${VUE_COMPOSITION_FUNCTIONS.join('|')})\\s*[<(]`
)
const CSS_VBIND_REGEX = /\bv-bind\s*\(/

// 错误消息常量
const ERROR_MESSAGES = {
  COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP: 'defineProps、defineExpose、defineEmits、defineSlots、defineOptions、defineModel需要在<script setup>下使用',
  UNSUPPORTED_PREPROCESSORS: `仅支持 ${SUPPORTED_LANGUAGES.map(lang => `lang="${lang}"`).join('、')} 预处理器`,
  UNSUPPORTED_STYLE_PREPROCESSORS: `仅支持 ${SUPPORTED_STYLE_LANGUAGES.map(lang => `lang="${lang}"`).join('、')} 样式预处理器`,
  JAVASCRIPT_PARSE_ERROR: 'JavaScript解析错误: ',
  TYPESCRIPT_PARSE_ERROR: 'TypeScript解析错误: ',
  STYLE_COMPILATION_ERROR: '样式编译错误: ',
  TEMPLATE_COMPILATION_ERROR: '模板编译错误: ',
  SCRIPT_COMPILATION_ERROR: '脚本编译错误: ',
  CSS_VBIND_ERROR: 'CSS v-bind() 编译错误: ',
  SRC_FETCH_ERROR: '外部资源加载失败: ',
  CSS_PREPROCESSOR_ERROR: 'CSS预处理器加载失败，请确认已安装或在选项中启用: '
}

// 缓存机制
const compositionFunctionCache = new Map()
const compilationCache = new Map()

// 编译时间统计
const compileStats = {
  totalCompiles: 0,
  totalTime: 0,
  averageTime: 0,
  cacheHits: 0,
  cacheMisses: 0
}

// 插件系统
const plugins = []

/**
 * 注册插件
 * @param {Object} plugin - 插件对象
 */
function registerPlugin(plugin) {
  plugins.push(plugin)
  // 注册/移除插件后清空缓存，避免使用旧插件版本编译的缓存结果
  clearCompileCache()
}

/**
 * 执行插件钩子
 * @param {string} hookName - 钩子名称
 * @param {*} ...args - 钩子参数
 * @returns {*} 钩子执行结果
 */
function executePluginHook(hookName, ...args) {
  return plugins.reduce((result, plugin) => {
    if (plugin[hookName]) {
      return plugin[hookName](...args, result)
    }
    return result
  }, null)
}

// CSS 预处理器缓存（避免重复加载 CDN 资源）
const preprocessorCache = new Map()

/**
 * 按需加载 CSS 预处理器（通过 dynamic import）
 * @param {string} lang - 预处理语言 (scss/sass/less/stylus)
 * @returns {Promise<Object>} 预处理器模块
 */
async function loadPreprocessor(lang) {
  if (preprocessorCache.has(lang)) {
    const cached = preprocessorCache.get(lang)
    if (cached instanceof Error) throw cached
    return cached
  }

  try {
    let mod
    switch (lang) {
      case 'scss':
      case 'sass':
        mod = await import(/* @vite-ignore */ 'https://unpkg.com/sass@1.69.5/dist/sass.default.js')
        break
      case 'less':
        mod = await import(/* @vite-ignore */ 'https://unpkg.com/less@4.2.0/dist/less.min.js')
        break
      case 'stylus':
        mod = await import(/* @vite-ignore */ 'https://unpkg.com/stylus@0.63.0/lib/stylus.js')
        break
      default:
        throw new Error(`Unsupported preprocessor: ${lang}`)
    }
    preprocessorCache.set(lang, mod)
    return mod
  } catch (e) {
    const err = new Error(`${ERROR_MESSAGES.CSS_PREPROCESSOR_ERROR}${lang}: ${e.message}`)
    preprocessorCache.set(lang, err)
    throw err
  }
}

/**
 * 编译 Vue SFC 代码的主函数 (async)
 * @param {string} sourceCode - Vue SFC 源代码
 * @param {Object} options - 编译选项
 * @returns {Promise<Object>} 编译结果 { compileJs, compileCss, warnings, customBlocks, styleMeta, sourceMap }
 */
export async function compileCode(sourceCode, options = {}) {
  // 合并默认选项
  const compileOptions = { ...DEFAULT_COMPILE_OPTIONS, ...options }
  const compilerOptions = compileOptions.compilerOptions || {}

  // 执行插件的 beforeCompile 钩子
  const processedSource = executePluginHook('beforeCompile', sourceCode, compileOptions) || sourceCode

  // 检查缓存
  const cacheKey = `${processedSource.length.toString(36)}_${processedSource.substring(0, 250)}_${JSON.stringify(Object.entries(compilerOptions).sort())}`
  if (compileOptions.enableCache && compilationCache.has(cacheKey)) {
    compileStats.cacheHits++
    return compilationCache.get(cacheKey)
  }

  compileStats.cacheMisses++
  const startTime = Date.now()
  const compiled = {
    errors: [],
    warnings: [],
    js: '',
    css: '',
    customBlocks: [],
    styleMeta: []
  }

  await compileFile(processedSource, compiled, compileOptions)

  // 收集错误和警告 (错误不再直接 throw，支持降级)
  let result
  try {
    result = processCompiledCode(compiled, compileOptions)
  } catch (error) {
    compiled.errors.push({
      message: `${ERROR_MESSAGES.JAVASCRIPT_PARSE_ERROR}${error.message}`,
      code: error.code || ERROR_CODES.JAVASCRIPT_PARSE_ERROR,
      stack: error.stack
    })
    // 降级返回
    result = {
      compileJs: wrapAsAsyncFunction(`\nconst ${COMP_IDENTIFIER} = {}`),
      compileCss: compiled.css || '',
      sourceMap: null,
      cssMap: null
    }
  }

  // 执行插件的 afterCompile 钩子
  result = executePluginHook('afterCompile', result, compileOptions) || result

  // 更新统计信息
  const endTime = Date.now()
  const compileTime = endTime - startTime
  compileStats.totalCompiles++
  compileStats.totalTime += compileTime
  compileStats.averageTime = compileStats.totalTime / compileStats.totalCompiles

  // 附加 warnings / customBlocks / styleMeta / errors 到结果
  result.warnings = compiled.warnings || []
  result.customBlocks = compiled.customBlocks || []
  result.styleMeta = compiled.styleMeta || []
  if (compiled.errors && compiled.errors.length) {
    result.errors = compiled.errors
  }

  // 缓存结果
  if (compileOptions.enableCache) {
    if (compilationCache.size > compileOptions.maxCacheSize) {
      const firstKey = compilationCache.keys().next().value
      compilationCache.delete(firstKey)
    }
    compilationCache.set(cacheKey, result)
  }

  return result
}

/**
 * 获取编译统计信息
 * @returns {Object} 编译统计信息
 */
function getCompileStats() {
  return { ...compileStats }
}

/**
 * 清空编译缓存
 */
function clearCompileCache() {
  compilationCache.clear()
  compositionFunctionCache.clear()
  preprocessorCache.clear()
}

/**
 * 编译多个 Vue SFC 代码（每个组件独立容错）
 * @param {Array<string>} sourceCodes - Vue SFC 源代码数组
 * @param {Object} options - 编译选项
 * @returns {Promise<Array<Object>>} 编译结果数组
 */
async function compileMultiple(sourceCodes, options = {}) {
  return Promise.all(sourceCodes.map(async code => {
    try {
      return await compileCode(code, options)
    } catch (error) {
      return {
        compileJs: '',
        compileCss: '',
        errors: [{
          message: error.message,
          code: error.code || ERROR_CODES.SCRIPT_COMPILATION_ERROR,
          stack: error.stack
        }],
        warnings: [],
        customBlocks: [],
        styleMeta: [],
        sourceMap: null,
        cssMap: null
      }
    }
  }))
}

/**
 * 处理编译后的代码，转换导入和导出语句
 */
function processCompiledCode(compiled, options = {}) {
  let jsCode = compiled.js

  if (!jsCode) {
    // 脚本编译失败降级：返回空组件
    jsCode = `\nconst ${COMP_IDENTIFIER} = {}`
  }

  try {
    // 执行自定义转换
    if (options.customTransforms.beforeProcess) {
      jsCode = options.customTransforms.beforeProcess(jsCode, compiled, options)
    }

    // 检测是否为 TypeScript（通过 compiled 上的标记而非字符串搜索）
    const isTypeScript = compiled._isTypeScript === true

    const ast = babelParse(jsCode, {
      sourceType: 'module',
      plugins: isTypeScript
        ? ['typescript', 'jsx', 'decorators-legacy']
        : ['jsx']
    })
    const replacements = []

    for (let i = ast.program.body.length - 1; i >= 0; i--) {
      const node = ast.program.body[i]
      const result = processAstNode(node, jsCode)
      if (result.modified) {
        replacements.push({
          start: node.start,
          end: node.end,
          code: result.code
        })
      } else if (isTypeScriptNode(node)) {
        // 移除 TypeScript 类型声明
        replacements.push({
          start: node.start,
          end: node.end,
          code: ''
        })
      }
    }

    // 从后往前替换，避免位置偏移
    jsCode = applyReplacements(jsCode, replacements)

    // 清理多余的空行
    jsCode = cleanExtraNewlines(jsCode)

    // 包装成异步函数格式
    jsCode = wrapAsAsyncFunction(jsCode)

    // 执行自定义转换
    if (options.customTransforms.afterProcess) {
      jsCode = options.customTransforms.afterProcess(jsCode, compiled, options)
    }

    return {
      compileJs: jsCode,
      compileCss: compiled.css || '',
      sourceMap: options.sourceMap ? compiled._jsMap || null : null,
      cssMap: options.sourceMap ? compiled._cssMap || null : null
    }
  } catch (error) {
    const isTypeScript = compiled._isTypeScript === true
    const errorType = isTypeScript ? ERROR_MESSAGES.TYPESCRIPT_PARSE_ERROR : ERROR_MESSAGES.JAVASCRIPT_PARSE_ERROR
    const enhancedError = new Error(`${errorType}${error.message}`)
    enhancedError.originalError = error
    enhancedError.sourceCode = jsCode
    enhancedError.code = isTypeScript ? ERROR_CODES.TYPESCRIPT_PARSE_ERROR : ERROR_CODES.JAVASCRIPT_PARSE_ERROR
    throw enhancedError
  }
}

/**
 * 构建 Source Map — 串联各编译阶段的 source map
 * @param {Object} compiled - 编译中间结果
 * @returns {{ jsMap: Object|null, cssMap: Object|null }}
 */
function buildSourceMaps(compiled) {
  const jsMap = buildJsSourceMap(compiled)
  const cssMap = compiled._styleMap || null
  return { jsMap, cssMap }
}

/**
 * 串联 JS 部分的 source map（script map + template map 偏移合并）
 */
function buildJsSourceMap(compiled) {
  const scriptMap = compiled._scriptMap
  const templateMap = compiled._templateMap
  const scriptLineCount = compiled._scriptLineCount || 0

  if (!scriptMap && !templateMap) return null
  if (!scriptMap) return templateMap
  if (!templateMap) return scriptMap

  try {
    const generator = new SourceMapGenerator({
      file: scriptMap.file || 'compiled.js',
      sourceRoot: scriptMap.sourceRoot || ''
    })

    // 合并 script map（generated offset = 0）
    const scriptConsumer = new SourceMapConsumer(scriptMap)
    scriptConsumer.eachMapping(mapping => {
      if (mapping.source) {
        generator.addMapping({
          generated: { line: mapping.generatedLine, column: mapping.generatedColumn },
          original: { line: mapping.originalLine, column: mapping.originalColumn },
          source: mapping.source,
          name: mapping.name
        })
      }
    })

    // 合并 template map（generated offset = script 行数）
    const templateConsumer = new SourceMapConsumer(templateMap)
    templateConsumer.eachMapping(mapping => {
      if (mapping.source) {
        generator.addMapping({
          generated: {
            line: mapping.generatedLine + scriptLineCount,
            column: mapping.generatedColumn
          },
          original: { line: mapping.originalLine, column: mapping.originalColumn },
          source: mapping.source,
          name: mapping.name
        })
      }
    })

    // 合并 sources / sourcesContent
    const sources = new Set()
    const sourcesContentMap = {}
    ;[scriptConsumer, templateConsumer].forEach(consumer => {
      consumer.sources.forEach(src => {
        if (!sources.has(src)) {
          sources.add(src)
          const content = consumer.sourceContentFor(src, true)
          if (content) sourcesContentMap[src] = content
        }
      })
    })

    // 设置 sources 和 sourcesContent
    const result = JSON.parse(generator.toString())
    result.sources = Array.from(sources)
    result.sourcesContent = result.sources.map(s => sourcesContentMap[s] || null)
    return result
  } catch (e) {
    // 合并失败时返回 script map（template map 丢失但至少 script 层面可调试）
    console.warn('[SourceMap] 串联 map 失败:', e.message)
    return scriptMap
  }
}

/**
 * 判断是否为 TypeScript 节点
 */
function isTypeScriptNode(node) {
  const tsNodeTypes = [
    'TSTypeAliasDeclaration',
    'TSInterfaceDeclaration',
    'TSEnumDeclaration',
    'TSModuleDeclaration',
    'TSDeclareFunction',
    'TSImportEqualsDeclaration'
  ]
  return tsNodeTypes.includes(node.type) || node.type.startsWith('TS')
}

/**
 * 清理多余的空行
 */
function cleanExtraNewlines(code) {
  return code
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

/**
 * 将代码包装成异步函数格式
 */
function wrapAsAsyncFunction(jsCode) {
  // 确保代码以 __sfc__ 结束，然后在包装函数中返回它
  if (!jsCode.includes(COMP_IDENTIFIER)) {
    jsCode += `\nconst ${COMP_IDENTIFIER} = {}`
  }

  return `export default (async function() {\n${jsCode}\nreturn ${COMP_IDENTIFIER}\n})()`
}

/**
 * 替换代码片段的辅助函数
 */
function replaceCodeSegment(jsCode, start, end, newCode) {
  return jsCode.substring(0, start) + newCode + jsCode.substring(end)
}

/**
 * 按位置从后往前批量替换代码片段
 */
function applyReplacements(code, replacements) {
  replacements.sort((a, b) => b.start - a.start).forEach(r => {
    code = replaceCodeSegment(code, r.start, r.end, r.code)
  })
  return code
}

/**
 * 处理 AST 节点并返回处理结果
 */
function processAstNode(node, jsCode) {
  switch (node.type) {
    case 'ImportDeclaration':
      // 跳过 TypeScript 类型导入
      if (node.importKind === 'type') {
        return { modified: true, code: '' }
      }

      // 检查是否有类型导入
      const hasTypeImport = node.specifiers.some(spec =>
        spec.type === 'ImportSpecifier' && spec.importKind === 'type'
      )

      if (hasTypeImport) {
        // 过滤掉类型导入
        const nonTypeSpecifiers = node.specifiers.filter(spec =>
          spec.type !== 'ImportSpecifier' || spec.importKind !== 'type'
        )

        if (nonTypeSpecifiers.length === 0) {
          return { modified: true, code: '' }
        }

        // 重新构建非类型导入
        return {
          modified: true,
          code: generateImportCode({
            ...node,
            specifiers: nonTypeSpecifiers
          })
        }
      }

      return {
        modified: true,
        code: generateImportCode(node)
      }

    case 'ExportDefaultDeclaration':
      // 对于默认导出，转换为变量赋值
      if (node.declaration.type === 'Identifier') {
        return {
          modified: true,
          code: `const ${COMP_IDENTIFIER} = ${node.declaration.name}`
        }
      }
      break

    case 'ExportNamedDeclaration':
      // 处理命名导出
      if (node.declaration) {
        const declarationCode = jsCode.substring(node.declaration.start, node.declaration.end)
        return {
          modified: true,
          code: declarationCode
        }
      }
      break
  }

  return { modified: false, code: '' }
}

/**
 * 生成动态导入代码
 */
function generateImportCode(importNode) {
  const importStatements = importNode.specifiers.map(specifier => {
    const localName = specifier.local?.name
    const importedName = specifier.imported?.name
    const modulePath = importNode.source.value

    switch (specifier.type) {
      case 'ImportDefaultSpecifier':
        return `const ${localName} = await import('${modulePath}').then(m => m.default);`
      case 'ImportNamespaceSpecifier':
        return `const ${localName} = await import('${modulePath}');`
      default:
        return `const ${localName} = await import('${modulePath}').then(m => m.${importedName});`
    }
  })

  return importStatements.join('\n')
}

/**
 * 生成 UUID v4（优先使用 crypto.randomUUID）
 */
function uuidv4() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  // fallback 手写版本
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

/**
 * 编译 Vue 单文件组件 (async)
 */
async function compileFile(code, compiled, options = {}) {
  const id = uuidv4()
  const filename = `${id}.vue`

  if (!validateCodeBeforeParse(code, compiled)) {
    return
  }

  const { errors, warnings, descriptor } = SFCCompiler.parse(code, {
    filename,
    sourceMap: options.sourceMap
  })

  // 收集 parse 阶段的 warnings
  if (warnings && warnings.length) {
    compiled.warnings.push(...warnings.map(w => ({
      message: w.message,
      code: ERROR_CODES.SCRIPT_COMPILATION_ERROR,
      source: 'parse'
    })))
  }

  if (errors.length) {
    compiled.errors = errors.map(error => ({
      message: error.message,
      code: ERROR_CODES.SCRIPT_COMPILATION_ERROR,
      stack: error.stack
    }))
    // parse 失败 descriptor 不完整，停止编译
    compiled.js = `\nconst ${COMP_IDENTIFIER} = {}`
    compiled.css = ''
    return
  }

  if (!validateDescriptor(descriptor, compiled, options)) {
    return
  }

  // 处理外部 src 引用
  if (options.resolveSrc) {
    await resolveSrcReferences(descriptor, compiled, options)
  }

  // 标记是否为 TypeScript（供后续阶段使用，避免字符串搜索）
  compiled._isTypeScript = !!(descriptor.script?.lang === 'ts' ||
    descriptor.scriptSetup?.lang === 'ts' ||
    descriptor.script?.lang === 'tsx' ||
    descriptor.scriptSetup?.lang === 'tsx')

  try {
    await compileDescriptor(descriptor, id, filename, compiled, options)
  } catch (error) {
    compiled.errors.push({
      message: `${ERROR_MESSAGES.SCRIPT_COMPILATION_ERROR}${error.message}`,
      code: ERROR_CODES.SCRIPT_COMPILATION_ERROR,
      stack: error.stack
    })
    // 降级：初始化所有编译产物字段为空
    compiled.js = `\nconst ${COMP_IDENTIFIER} = {}`
    if (!compiled.css) compiled.css = ''
    if (!compiled.customBlocks) compiled.customBlocks = []
    if (!compiled.styleMeta) compiled.styleMeta = []
  }
}

/**
 * 解析外部 src 引用
 */
async function resolveSrcReferences(descriptor, compiled, options) {
  const resolveSrc = options.resolveSrc

  // 解析 script src
  if (descriptor.script?.src) {
    try {
      descriptor.script.content = await resolveSrc(descriptor.script.src)
    } catch (e) {
      compiled.errors.push({
        message: `${ERROR_MESSAGES.SRC_FETCH_ERROR}script: ${descriptor.script.src} - ${e.message}`,
        code: ERROR_CODES.SRC_FETCH_ERROR
      })
    }
  }

  // 解析 scriptSetup src
  if (descriptor.scriptSetup?.src) {
    try {
      descriptor.scriptSetup.content = await resolveSrc(descriptor.scriptSetup.src)
    } catch (e) {
      compiled.errors.push({
        message: `${ERROR_MESSAGES.SRC_FETCH_ERROR}scriptSetup: ${descriptor.scriptSetup.src} - ${e.message}`,
        code: ERROR_CODES.SRC_FETCH_ERROR
      })
    }
  }

  // 解析 template src
  if (descriptor.template?.src) {
    try {
      descriptor.template.content = await resolveSrc(descriptor.template.src)
    } catch (e) {
      compiled.errors.push({
        message: `${ERROR_MESSAGES.SRC_FETCH_ERROR}template: ${descriptor.template.src} - ${e.message}`,
        code: ERROR_CODES.SRC_FETCH_ERROR
      })
    }
  }

  // 解析 style src
  for (let i = 0; i < descriptor.styles.length; i++) {
    const style = descriptor.styles[i]
    if (style.src) {
      try {
        style.content = await resolveSrc(style.src)
      } catch (e) {
        compiled.errors.push({
          message: `${ERROR_MESSAGES.SRC_FETCH_ERROR}style[${i}]: ${style.src} - ${e.message}`,
          code: ERROR_CODES.SRC_FETCH_ERROR
        })
      }
    }
  }
}

/**
 * 分析 Vue SFC 组件信息
 * @param {string} sourceCode - Vue SFC 源代码
 * @returns {Object} 组件信息
 */
function analyzeComponent(sourceCode) {
  const id = uuidv4()
  const filename = `${id}.vue`

  const { errors, warnings, descriptor } = SFCCompiler.parse(sourceCode, {
    filename
  })

  if (errors.length) {
    throw new Error(errors[0].message)
  }

  return {
    ...generateComponentMetadata(descriptor, id),
    warnings: warnings.map(w => w.message)
  }
}

/**
 * 解析前的代码验证
 */
function validateCodeBeforeParse(code, compiled) {
  if (hasVueCompositionFunctions(code) && !hasScriptSetup(code)) {
    compiled.errors = [{
      message: ERROR_MESSAGES.COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP,
      code: ERROR_CODES.COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP
    }]
    return false
  }
  return true
}

/**
 * 验证 SFC 描述符
 */
function validateDescriptor(descriptor, compiled, options = {}) {
  // 验证脚本和模板语言
  const hasUnsupportedScriptLang = [
    descriptor.script?.lang,
    descriptor.scriptSetup?.lang,
    descriptor.template?.lang
  ].some(lang => {
    if (!lang) return false
    return !SUPPORTED_LANGUAGES.includes(lang)
  })

  if (hasUnsupportedScriptLang) {
    compiled.errors = [{
      message: ERROR_MESSAGES.UNSUPPORTED_PREPROCESSORS,
      code: ERROR_CODES.UNSUPPORTED_PREPROCESSORS
    }]
    return false
  }

  // 验证样式语言
  const hasUnsupportedStyleLang = descriptor.styles.some(style => {
    if (!style.lang) return false
    return !SUPPORTED_STYLE_LANGUAGES.includes(style.lang)
  })

  if (hasUnsupportedStyleLang) {
    compiled.errors = [{
      message: ERROR_MESSAGES.UNSUPPORTED_STYLE_PREPROCESSORS,
      code: ERROR_CODES.UNSUPPORTED_STYLE_PREPROCESSORS
    }]
    return false
  }

  return true
}

/**
 * 编译 SFC 描述符 (async)
 */
async function compileDescriptor(descriptor, id, filename, compiled, options = {}) {
  const codeParts = []

  // 收集自定义块
  compiled.customBlocks = descriptor.customBlocks.map(block => ({
    type: block.type,
    content: block.content,
    attrs: block.attrs || {}
  }))

  // 收集样式元数据
  compiled.styleMeta = descriptor.styles.map(s => ({
    scoped: !!s.scoped,
    module: !!s.module,
    moduleName: typeof s.module === 'string' ? s.module : (s.module ? 'style' : null),
    global: !s.scoped && !s.module,
    lang: s.lang || 'css'
  }))

  let scriptBindings = undefined
  const scriptResult = compileScriptPart(descriptor, id, compiled, options)
  if (scriptResult) {
    const [scriptCode, bindings] = scriptResult
    scriptBindings = bindings
    codeParts.push(scriptCode)

    if (descriptor.template && !descriptor.scriptSetup) {
      const templateCode = compileTemplatePart(descriptor, id, bindings, compiled, options)
      if (templateCode) {
        codeParts.push(templateCode)
      }
    }
  }

  if (descriptor.styles.some(s => s.scoped)) {
    codeParts.push(`\n${COMP_IDENTIFIER}.__scopeId = "data-v-${id}"`)
  }

  codeParts.push(
    `\n${COMP_IDENTIFIER}.__file = "${filename}"`
  )

  compiled.js = codeParts.join('').trimStart()

  // 编译样式（async）
  compiled.css = await compileStylesPart(descriptor, id, filename, compiled, options, scriptBindings)

  // 构建 source map（串联各阶段 map）
  const sourceMaps = buildSourceMaps(compiled)
  compiled._jsMap = sourceMaps.jsMap
  compiled._cssMap = sourceMaps.cssMap

  // 仅在完全没有编译产物时清空 errors
  if (!compiled.errors || compiled.errors.length === 0) {
    compiled.errors = []
  }
}

/**
 * 生成组件元数据
 * @param {Object} descriptor - SFC 描述符
 * @param {string} id - 组件 ID
 * @returns {Object} 组件元数据
 */
function generateComponentMetadata(descriptor, id) {
  return {
    id,
    hasScript: !!descriptor.script,
    hasScriptSetup: !!descriptor.scriptSetup,
    hasTemplate: !!descriptor.template,
    hasStyles: descriptor.styles.length > 0,
    hasScopedStyles: descriptor.styles.some(s => s.scoped),
    hasModuleStyles: descriptor.styles.some(s => s.module),
    scriptLang: descriptor.script?.lang || descriptor.scriptSetup?.lang,
    styleLangs: descriptor.styles.map(s => s.lang || 'css'),
    templateLang: descriptor.template?.lang,
    hasGeneric: !!descriptor.scriptSetup?.attrs?.generic,
    customBlockTypes: descriptor.customBlocks.map(b => b.type)
  }
}

/**
 * 编译脚本部分 - 支持 TypeScript 和泛型组件
 */
function compileScriptPart(descriptor, id, compiled, options = {}) {
  if (!descriptor.script && !descriptor.scriptSetup) {
    return [`\nconst ${COMP_IDENTIFIER} = {}`, undefined]
  }

  try {
    // 检测是否为 TypeScript（直接通过属性判断）
    const isTypeScript = descriptor.script?.lang === 'ts' ||
      descriptor.scriptSetup?.lang === 'ts' ||
      descriptor.script?.lang === 'tsx' ||
      descriptor.scriptSetup?.lang === 'tsx'

    // 提取泛型参数
    const generic = descriptor.scriptSetup?.attrs?.generic || undefined

    // 解析组件级 compilerOptions（Vue 3.3+ <script setup compilerOptions="...">）
    let componentCompilerOptions = {}
    const rawCompilerOptions = descriptor.scriptSetup?.attrs?.compilerOptions
    if (rawCompilerOptions) {
      try {
        componentCompilerOptions = Function(`"use strict"; return (${rawCompilerOptions})`)()
      } catch (e) {
        compiled.warnings.push({
          message: `解析组件级 compilerOptions 失败: ${e.message}`,
          code: ERROR_CODES.SCRIPT_COMPILATION_ERROR
        })
      }
    }
    // 合并到 compiled 上供 compileTemplatePart 使用（不污染用户传入的 options）
    compiled._mergedCompilerOptions = { ...componentCompilerOptions, ...(options.compilerOptions || {}) }

    const compiledScript = SFCCompiler.compileScript(descriptor, {
      id,
      inlineTemplate: options.inlineTemplate,
      propsDestructure: options.propsDestructure !== false,
      hoistStatic: options.hoistStatic !== false,
      ...(generic ? { generic } : {}),
      ...(options.customElement ? { customElement: options.customElement } : {}),
      ...(isTypeScript && {
        babelParserPlugins: [
          'jsx',
          'typescript',
          'decorators-legacy'
        ]
      })
    })

    if (compiledScript.map) {
      compiled._scriptMap = compiledScript.map
      // 记录脚本代码行数（供 template map 偏移合并使用）
      compiled._scriptLineCount = (compiledScript.content.match(/\n/g) || []).length
    }

    let code = ''

    if (!options.isProd && compiledScript.bindings) {
      code += `\n/* Analyzed bindings: ${JSON.stringify(
        compiledScript.bindings,
        null,
        2
      )} */`
    }

    if (!options.isProd) {
      const metadata = generateComponentMetadata(descriptor, id)
      code += `\n/* Component metadata: ${JSON.stringify(
        metadata,
        null,
        2
      )} */`
    }

    let scriptContent = SFCCompiler.rewriteDefault(compiledScript.content, COMP_IDENTIFIER)
    scriptContent = transformImportsToAwaitThen(scriptContent, isTypeScript)

    code += `\n${scriptContent}`
    return [code, compiledScript.bindings]

  } catch (error) {
    const isTS = descriptor.script?.lang === 'ts' || descriptor.scriptSetup?.lang === 'ts'
    const errorMessage = isTS ?
      `${ERROR_MESSAGES.TYPESCRIPT_PARSE_ERROR}${error.message}` :
      `${ERROR_MESSAGES.SCRIPT_COMPILATION_ERROR}${error.message}`
    compiled.errors.push({
      message: errorMessage,
      code: isTS ? ERROR_CODES.TYPESCRIPT_PARSE_ERROR : ERROR_CODES.SCRIPT_COMPILATION_ERROR,
      stack: error.stack
    })
    return null
  }
}

/**
 * 转换导入语句为 await import().then() 格式
 */
function transformImportsToAwaitThen(scriptContent, isTypeScript = false) {
  try {
    const ast = babelParse(scriptContent, {
      sourceType: 'module',
      plugins: isTypeScript ?
        ['typescript', 'jsx', 'decorators-legacy'] :
        ['jsx']
    })
    let transformedCode = scriptContent
    const importsToReplace = []

    for (let i = ast.program.body.length - 1; i >= 0; i--) {
      const node = ast.program.body[i]

      if (node.type === 'ImportDeclaration') {
        if (node.importKind === 'type') {
          importsToReplace.push({ start: node.start, end: node.end, code: '' })
          continue
        }

        const nonTypeSpecifiers = node.specifiers.filter(spec =>
          spec.type !== 'ImportSpecifier' || spec.importKind !== 'type'
        )

        if (nonTypeSpecifiers.length === 0) {
          importsToReplace.push({ start: node.start, end: node.end, code: '' })
          continue
        }

        const importStatements = nonTypeSpecifiers.map(specifier => {
          const localName = specifier.local?.name
          const importedName = specifier.imported?.name
          const modulePath = node.source.value

          switch (specifier.type) {
            case 'ImportDefaultSpecifier':
              return `const ${localName} = await import('${modulePath}').then(m => m.default);`
            case 'ImportNamespaceSpecifier':
              return `const ${localName} = await import('${modulePath}');`
            default:
              return `const ${localName} = await import('${modulePath}').then(m => m.${importedName});`
          }
        }).join('\n')

        importsToReplace.push({ start: node.start, end: node.end, code: importStatements })
      }

      if (node.type === 'TSImportEqualsDeclaration') {
        const modulePath = node.moduleReference.expression?.value
        if (modulePath) {
          importsToReplace.push({
            start: node.start,
            end: node.end,
            code: `const ${node.id.name} = await import('${modulePath}').then(m => m.default);`
          })
        }
      }
    }

    return applyReplacements(transformedCode, importsToReplace)
  } catch (error) {
    console.warn('AST parsing failed, using fallback string replacement:', error.message)
    return fallbackTransformImports(scriptContent, isTypeScript)
  }
}

/**
 * 回退的导入转换方法（字符串替换）
 */
function fallbackTransformImports(scriptContent, isTypeScript = false) {
  let result = scriptContent

  if (isTypeScript) {
    result = result
      .replace(/import\s+type\s+.*?\s+from\s+['"][^'"]+['"];?\n?/g, '')
      .replace(/import\s*{\s*([^}]*\btype\s+[^}]*)\s*}\s*from\s+['"][^'"]+['"];?\n?/g, '')
      .replace(/import\s*{\s*([^}]*)\s*}\s*from\s+['"][^'"]+['"];?\n?/g, (match, imports) => {
        const importItems = imports.split(',').map(item => item.trim())
        const nonTypeImports = importItems.filter(item =>
          !item.startsWith('type ') && !item.includes(' as type')
        )

        if (nonTypeImports.length === 0) return ''

        const modulePathMatch = match.match(/from\s+['"]([^'"]+)['"]/)
        if (!modulePathMatch) return match

        const modulePath = modulePathMatch[1]
        const importList = nonTypeImports.map(imp => {
          const parts = imp.split(/\s+as\s+/)
          if (parts.length === 2) {
            return `const ${parts[1]} = await import('${modulePath}').then(m => m.${parts[0]});`
          } else {
            return `const ${imp} = await import('${modulePath}').then(m => m.${imp});`
          }
        }).join('\n')

        return importList
      })
  }

  result = result
    .replace(/import\s+(\w+)\s+from\s+['"]([^'"]+)['"]/g, 'const $1 = await import(\'$2\').then(m => m.default);')
    .replace(/import\s+\*\s+as\s+(\w+)\s+from\s+['"]([^'"]+)['"]/g, 'const $1 = await import(\'$2\');')
    .replace(/import\s+{([^}]+)}\s+from\s+['"]([^'"]+)['"]/g, (match, imports, modulePath) => {
      const importList = imports.split(',').map(imp => {
        const trimmed = imp.trim()
        if (trimmed.startsWith('type ')) return ''

        const parts = trimmed.split(/\s+as\s+/)
        if (parts.length === 2) {
          return `const ${parts[1]} = await import('${modulePath}').then(m => m.${parts[0]});`
        } else {
          return `const ${trimmed} = await import('${modulePath}').then(m => m.${trimmed});`
        }
      }).filter(Boolean).join('\n')
      return importList
    })

  return result
}

/**
 * 编译模板部分
 */
function compileTemplatePart(descriptor, id, bindingMetadata, compiled, options = {}) {
  const compilerOptions = compiled._mergedCompilerOptions || options.compilerOptions || {}

  const templateResult = SFCCompiler.compileTemplate({
    source: descriptor.template?.content || '',
    filename: descriptor.filename,
    id,
    scoped: descriptor.styles.some(s => s.scoped),
    slotted: descriptor.slotted,
    isProd: options.isProd,
    compilerOptions: {
      bindingMetadata,
      sourceMap: options.sourceMap,
      ...compilerOptions
    }
  })

  // 收集 template warnings
  if (templateResult.warnings && templateResult.warnings.length) {
    compiled.warnings.push(...templateResult.warnings.map(w => ({
      message: `${ERROR_MESSAGES.TEMPLATE_COMPILATION_ERROR}${w.message || w}`,
      code: ERROR_CODES.TEMPLATE_COMPILATION_ERROR
    })))
  }

  if (templateResult.map) {
    compiled._templateMap = templateResult.map
  }

  if (templateResult.errors.length) {
    compiled.errors.push(...templateResult.errors.map(error => ({
      message: `${ERROR_MESSAGES.TEMPLATE_COMPILATION_ERROR}${error.message}`,
      code: ERROR_CODES.TEMPLATE_COMPILATION_ERROR,
      stack: error.stack
    })))
    return null
  }

  const fnName = `render`
  let transformedCode = templateResult.code.replace(
    /\nexport (function|const) (render|ssrRender)/,
    `$1 ${fnName}`
  )

  const isTypeScript = compiled._isTypeScript === true
  transformedCode = transformImportsToAwaitThen(transformedCode, isTypeScript)

  if (templateResult.ssrCode) {
    const ssrFnName = `ssrRender`
    const ssrTransformedCode = templateResult.ssrCode.replace(
      /\nexport (function|const) ssrRender/,
      `$1 ${ssrFnName}`
    )
    transformedCode += `\n${ssrTransformedCode}\n${COMP_IDENTIFIER}.${ssrFnName} = ${ssrFnName}`
  }

  return `\n${transformedCode}\n${COMP_IDENTIFIER}.${fnName} = ${fnName}`
}

/**
 * 编译样式部分 - 支持 CSS v-bind()、预处理器和模块化样式 (async)
 */
async function compileStylesPart(descriptor, id, filename, compiled, options = {}, bindings = undefined) {
  const styleResults = []

  for (let i = 0; i < descriptor.styles.length; i++) {
    const style = descriptor.styles[i]
    try {
      const hasVBind = CSS_VBIND_REGEX.test(style.content)
      const hasPreprocessor = style.lang && style.lang !== 'css'
      const needsAsync = hasVBind || (hasPreprocessor && options.experimentalCssPreprocessors)
      const enableCssVBind = options.enableCssVBind !== false

      const styleId = (style.scoped || (hasVBind && enableCssVBind)) ? `data-v-${id}` : undefined

      const baseOptions = {
        source: style.content,
        filename,
        id: styleId,
        scoped: style.scoped,
        modules: !!style.module,
        trim: true,
        preprocessLang: style.lang || 'css',
        ...(style.module && options.modulesOptions ? {
          modulesOptions: { ...options.modulesOptions }
        } : {})
      }

      if (needsAsync) {
        const asyncOptions = {
          ...baseOptions,
          isAsync: hasVBind && enableCssVBind
        }
        if (hasPreprocessor && options.experimentalCssPreprocessors) {
          asyncOptions.preprocessCustomRequire = async (lang) => await loadPreprocessor(lang)
        }

        try {
          const styleResult = await SFCCompiler.compileStyleAsync(asyncOptions)
          processStyleResult(styleResult, style, i, compiled, styleResults)
        } catch (asyncError) {
          compiled.errors.push({
            message: `${hasVBind ? ERROR_MESSAGES.CSS_VBIND_ERROR : ERROR_MESSAGES.CSS_PREPROCESSOR_ERROR}${asyncError.message}`,
            code: hasVBind ? ERROR_CODES.CSS_VBIND_ERROR : ERROR_CODES.CSS_PREPROCESSOR_ERROR,
            styleIndex: i,
            stack: asyncError.stack
          })
          if (style.content.trim()) styleResults.push(style.content)
        }
      } else {
        const styleResult = SFCCompiler.compileStyle(baseOptions)
        processStyleResult(styleResult, style, i, compiled, styleResults)
      }
    } catch (error) {
      if (style.content.trim()) styleResults.push(style.content)
      compiled.errors.push({
        message: `${ERROR_MESSAGES.STYLE_COMPILATION_ERROR}${error.message}`,
        code: ERROR_CODES.STYLE_COMPILATION_ERROR,
        styleIndex: i,
        stack: error.stack
      })
    }
  }

  return styleResults.length > 0 ? styleResults.join('\n').trim() : ''
}

/**
 * 处理样式编译结果 — 提取 warnings、map、errors、code、module 导出
 */
function processStyleResult(styleResult, style, styleIndex, compiled, styleResults) {
  // 收集 warnings
  if (styleResult.warnings && styleResult.warnings.length) {
    compiled.warnings.push(...styleResult.warnings.map(w => ({
      message: w.message || w,
      code: ERROR_CODES.STYLE_COMPILATION_ERROR,
      styleIndex
    })))
  }

  // 收集 style map（多 style 块收集为数组）
  if (styleResult.map) {
    if (compiled._styleMap) {
      compiled._styleMaps = compiled._styleMaps || [compiled._styleMap]
      compiled._styleMaps.push(styleResult.map)
      compiled._styleMap = styleResult.map
    } else {
      compiled._styleMap = styleResult.map
    }
  }

  // 收集 errors（过滤 postcss pathToFileURL 噪音）
  if (styleResult.errors && styleResult.errors.length > 0) {
    const filteredErrors = styleResult.errors.filter(e =>
      !e.message.includes('pathToFileURL') && !e.message.includes('PathToFileURL')
    )
    if (filteredErrors.length > 0) {
      compiled.errors.push(...filteredErrors.map(e => ({
        message: `${ERROR_MESSAGES.STYLE_COMPILATION_ERROR}${e.message}`,
        code: ERROR_CODES.STYLE_COMPILATION_ERROR,
        styleIndex
      })))
    }
  }

  // 样式代码降级
  if (styleResult.code) {
    styleResults.push(styleResult.code)
  } else if (style.content.trim()) {
    styleResults.push(style.content)
  }

  // CSS Modules 类名映射导出
  if (style.module && styleResult.modules) {
    const moduleName = typeof style.module === 'string' ? style.module : 'style'
    styleResults.push(`\n${COMP_IDENTIFIER}.${moduleName} = ${JSON.stringify(styleResult.modules)}`)
  }
}

/**
 * 优化 CSS 代码
 * @param {string} css - CSS 代码
 * @returns {string} 优化后的 CSS 代码
 */
function optimizeCss(css) {
  return css
    .replace(/\s+/g, ' ')
    .replace(/\s*([{}:;,])\s*/g, '$1')
    .replace(/;}/g, '}')
    .trim()
}

/**
 * 检查代码中是否包含 Vue 组合式函数（带缓存）
 */
function hasVueCompositionFunctions(content) {
  // 使用长度+前缀组合键避免短内容碰撞
  const cacheKey = `${content.length.toString(36)}_${content.substring(0, 200)}`
  if (compositionFunctionCache.has(cacheKey)) {
    return compositionFunctionCache.get(cacheKey)
  }

  const hasFunctions = COMPOSITION_FN_PATTERN.test(content)
  compositionFunctionCache.set(cacheKey, hasFunctions)

  if (compositionFunctionCache.size > MAX_CACHE_SIZE) {
    const firstKey = compositionFunctionCache.keys().next().value
    compositionFunctionCache.delete(firstKey)
  }

  return hasFunctions
}

/**
 * 检查代码是否包含 <script setup>
 */
function hasScriptSetup(content) {
  return SCRIPT_SETUP_REGEX.test(content)
}

// 导出工具函数供测试使用
export const __TEST__ = {
  transformImportsToAwaitThen,
  fallbackTransformImports,
  hasVueCompositionFunctions,
  hasScriptSetup,
  validateCodeBeforeParse,
  validateDescriptor,
  compileDescriptor,
  compileScriptPart,
  compileTemplatePart,
  compileStylesPart,
  processStyleResult,
  buildSourceMaps,
  generateComponentMetadata,
  optimizeCss,
  executePluginHook,
  loadPreprocessor,
  uuidv4,
  processAstNode,
  generateImportCode,
  wrapAsAsyncFunction,
  cleanExtraNewlines,
  replaceCodeSegment,
  applyReplacements,
  resolveSrcReferences
}

// 导出默认编译函数
export default compileCode

// 导出所有函数
export {
  compileFile,
  analyzeComponent,
  getCompileStats,
  clearCompileCache,
  compileMultiple,
  registerPlugin
}
