import * as SFCCompiler from '@vue/compiler-sfc'
import { babelParse } from "@vue/compiler-sfc"

// 常量定义
const COMP_IDENTIFIER = `__sfc__`
const VUE_COMPOSITION_FUNCTIONS = ['defineExpose', 'defineProps', 'defineEmits', 'defineSlots', 'defineOptions', 'defineModel']
const SCRIPT_SETUP_REGEX = /<script\s+setup[^>]*>/
const SUPPORTED_LANGUAGES = ['js', 'ts', 'jsx', 'tsx']

// 预编译正则表达式 - 使用缓存
const compositionFunctionPatterns = VUE_COMPOSITION_FUNCTIONS.map(
    fn => new RegExp(`\\b${fn}\\s*[<(]`)
)

// 错误消息常量
const ERROR_MESSAGES = {
    COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP: 'defineProps、defineExpose、defineEmits、defineSlots、defineOptions、defineModel需要在<script setup>下使用',
    UNSUPPORTED_PREPROCESSORS: `仅支持 ${SUPPORTED_LANGUAGES.map(lang => `lang="${lang}"`).join('、')} 预处理器`,
    UNSUPPORTED_STYLE_MODULE: '<style module> is not supported in the playground.',
    JAVASCRIPT_PARSE_ERROR: 'JavaScript解析错误: ',
    TYPESCRIPT_PARSE_ERROR: 'TypeScript解析错误: '
}

// 缓存组合式函数检测结果
const compositionFunctionCache = new Map()

/**
 * 编译 Vue SFC 代码的主函数
 */
export function compileCode(sourceCode) {
    const compiled = {
        errors: [],
        js: '',
        css: ''
    }

    compileFile(sourceCode, compiled)

    if (compiled.errors.length) {
        const error = new Error(compiled.errors[0])
        error.details = compiled.errors
        throw error
    }

    const result = processCompiledCode(compiled)

    return {
        compileJs: result.compileJs,
        compiledCss: result.compiledCss
    }
}

/**
 * 处理编译后的代码，转换导入和导出语句
 */
function processCompiledCode(compiled) {
    let jsCode = compiled.js

    try {
        const ast = babelParse(jsCode, {
            sourceType: 'module',
            plugins: getBabelPlugins(compiled)
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

        replacements.sort((a, b) => b.start - a.start).forEach(replacement => {
            jsCode = replaceCodeSegment(jsCode, replacement.start, replacement.end, replacement.code)
        })

        // 清理多余的空行
        jsCode = cleanExtraNewlines(jsCode)

        // 包装成异步函数格式
        jsCode = wrapAsAsyncFunction(jsCode)

        return {
            compileJs: jsCode,
            compiledCss: compiled.css
        }
    } catch (error) {
        const errorType = compiled.js.includes('lang="ts"') ? ERROR_MESSAGES.TYPESCRIPT_PARSE_ERROR : ERROR_MESSAGES.JAVASCRIPT_PARSE_ERROR
        const enhancedError = new Error(`${errorType}${error.message}`)
        enhancedError.originalError = error
        enhancedError.sourceCode = jsCode
        throw enhancedError
    }
}

/**
 * 获取 Babel 插件配置
 */
function getBabelPlugins(compiled) {
    const plugins = []

    // 检查是否为 TypeScript
    if (compiled.js.includes('lang="ts"') || compiled.js.includes('lang="tsx"')) {
        plugins.push('typescript', 'decorators-legacy')
    }

    // 检查是否为 JSX
    if (compiled.js.includes('lang="jsx"') || compiled.js.includes('lang="tsx"')) {
        plugins.push('jsx')
    }

    return plugins
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

    return `export default (async function() {
${jsCode}
return ${COMP_IDENTIFIER}
})()`
}

/**
 * 替换代码片段的辅助函数
 */
function replaceCodeSegment(jsCode, start, end, newCode) {
    return jsCode.substring(0, start) + newCode + jsCode.substring(end)
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
 * 生成动态导入代码 - 修改为使用 .then() 格式
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
 * 生成 UUID v4
 */
function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0
        const v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
    })
}

/**
 * 编译 Vue 单文件组件
 */
export function compileFile(code, compiled) {
    const id = uuidv4()
    const filename = `${id}.vue`

    if (!validateCodeBeforeParse(code, compiled)) {
        return
    }

    const { errors, descriptor } = SFCCompiler.parse(code, {
        filename,
        sourceMap: true
    })

    if (errors.length) {
        compiled.errors = errors
        return
    }

    if (!validateDescriptor(descriptor, compiled)) {
        return
    }

    try {
        compileDescriptor(descriptor, id, filename, compiled)
    } catch (error) {
        compiled.errors = [error.message]
        if (error.stack) {
            compiled.errors.push(`Stack: ${error.stack}`)
        }
    }
}

/**
 * 解析前的代码验证
 */
function validateCodeBeforeParse(code, compiled) {
    if (hasVueCompositionFunctions(code) && !hasScriptSetup(code)) {
        compiled.errors = [ERROR_MESSAGES.COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP]
        return false
    }
    return true
}

/**
 * 验证 SFC 描述符
 */
function validateDescriptor(descriptor, compiled) {
    const hasUnsupportedLang = [
        descriptor.script?.lang,
        descriptor.scriptSetup?.lang,
        descriptor.template?.lang
    ].some(lang => {
        // 允许 js、ts、jsx、tsx
        if (!lang) return false
        return !SUPPORTED_LANGUAGES.includes(lang)
    })

    if (hasUnsupportedLang) {
        compiled.errors = [ERROR_MESSAGES.UNSUPPORTED_PREPROCESSORS]
        return false
    }

    if (descriptor.styles.some(style => style.module)) {
        compiled.errors = [ERROR_MESSAGES.UNSUPPORTED_STYLE_MODULE]
        return false
    }

    return true
}

/**
 * 编译 SFC 描述符
 */
function compileDescriptor(descriptor, id, filename, compiled) {
    const codeParts = []

    const scriptResult = compileScriptPart(descriptor, id, compiled)
    if (!scriptResult) return

    const [scriptCode, bindings] = scriptResult
    codeParts.push(scriptCode)

    if (descriptor.template && !descriptor.scriptSetup) {
        const templateCode = compileTemplatePart(descriptor, id, bindings, compiled)
        if (!templateCode) return
        codeParts.push(templateCode)
    }

    // 修复：确保作用域 ID 被正确设置
    if (descriptor.styles.some(s => s.scoped)) {
        codeParts.push(`\n${COMP_IDENTIFIER}.__scopeId = "data-v-${id}"`)
    }

    codeParts.push(
        `\n${COMP_IDENTIFIER}.__file = "${filename}"`
    )

    compiled.js = codeParts.join('').trimStart()

    // 修复：确保样式被正确编译
    compiled.css = compileStylesPart(descriptor, id, filename, compiled)
    compiled.errors = []
}

/**
 * 编译脚本部分 - 支持 TypeScript
 */
function compileScriptPart(descriptor, id, compiled) {
    if (!descriptor.script && !descriptor.scriptSetup) {
        return [`\nconst ${COMP_IDENTIFIER} = {}`, undefined]
    }

    try {
        // 检测是否为 TypeScript
        const isTypeScript = descriptor.script?.lang === 'ts' ||
            descriptor.scriptSetup?.lang === 'ts' ||
            descriptor.script?.lang === 'tsx' ||
            descriptor.scriptSetup?.lang === 'tsx'

        const compiledScript = SFCCompiler.compileScript(descriptor, {
            id,
            refSugar: true,
            inlineTemplate: true,
            // 添加 TypeScript 支持
            ...(isTypeScript && {
                babelParserPlugins: [
                    'jsx',
                    'typescript',
                    'decorators-legacy'
                ]
            })
        })

        let code = ''

        // 添加绑定信息注释（仅在开发模式下）
        if (process.env.NODE_ENV !== 'production' && compiledScript.bindings) {
            code += `\n/* Analyzed bindings: ${JSON.stringify(
                compiledScript.bindings,
                null,
                2
            )} */`
        }

        // 重写默认导出
        let scriptContent = SFCCompiler.rewriteDefault(compiledScript.content, COMP_IDENTIFIER)

        // 转换导入语句为 await import().then() 格式
        scriptContent = transformImportsToAwaitThen(scriptContent, isTypeScript)

        code += `\n${scriptContent}`
        return [code, compiledScript.bindings]

    } catch (error) {
        // 增强 TypeScript 错误信息
        const isTS = descriptor.script?.lang === 'ts' || descriptor.scriptSetup?.lang === 'ts'
        const errorMessage = isTS ?
            `${ERROR_MESSAGES.TYPESCRIPT_PARSE_ERROR}${error.message}` :
            error.message
        compiled.errors = [errorMessage]
        return null
    }
}

/**
 * 转换导入语句为 await import().then() 格式 - 支持 TypeScript
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

            // 处理导入声明
            if (node.type === 'ImportDeclaration') {
                // 跳过类型导入
                if (node.importKind === 'type') {
                    importsToReplace.push({
                        start: node.start,
                        end: node.end,
                        code: ''
                    })
                    continue
                }

                // 过滤掉类型导入的 specifiers
                const nonTypeSpecifiers = node.specifiers.filter(spec =>
                    spec.type !== 'ImportSpecifier' || spec.importKind !== 'type'
                )

                if (nonTypeSpecifiers.length === 0) {
                    importsToReplace.push({
                        start: node.start,
                        end: node.end,
                        code: ''
                    })
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

                importsToReplace.push({
                    start: node.start,
                    end: node.end,
                    code: importStatements
                })
            }

            // 处理 TypeScript 类型导入
            if (node.type === 'TSImportEqualsDeclaration') {
                // TypeScript 的 import = require 语法
                const modulePath = node.moduleReference.expression?.value
                if (modulePath) {
                    const importCode = `const ${node.id.name} = await import('${modulePath}').then(m => m.default);`
                    importsToReplace.push({
                        start: node.start,
                        end: node.end,
                        code: importCode
                    })
                }
            }
        }

        // 应用替换
        importsToReplace.sort((a, b) => b.start - a.start).forEach(replacement => {
            transformedCode = replaceCodeSegment(transformedCode, replacement.start, replacement.end, replacement.code)
        })

        return transformedCode
    } catch (error) {
        console.warn('AST parsing failed, using fallback string replacement:', error.message)
        return fallbackTransformImports(scriptContent, isTypeScript)
    }
}

/**
 * 回退的导入转换方法（字符串替换）- 支持 TypeScript
 */
function fallbackTransformImports(scriptContent, isTypeScript = false) {
    let result = scriptContent

    if (isTypeScript) {
        // 移除 TypeScript 类型导入
        result = result
            .replace(/import\s+type\s+.*?\s+from\s+['"][^'"]+['"];?\n?/g, '')
            .replace(/import\s*{\s*([^}]*\btype\s+[^}]*)\s*}\s*from\s+['"][^'"]+['"];?\n?/g, '')
            .replace(/import\s*{\s*([^}]*)\s*}\s*from\s+['"][^'"]+['"];?\n?/g, (match, imports) => {
                // 检查是否包含类型导入
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

    // 转换常规导入
    result = result
        .replace(/import\s+(\w+)\s+from\s+['"]([^'"]+)['"]/g, 'const $1 = await import(\'$2\').then(m => m.default);')
        .replace(/import\s+\*\s+as\s+(\w+)\s+from\s+['"]([^'"]+)['"]/g, 'const $1 = await import(\'$2\');')
        .replace(/import\s+{([^}]+)}\s+from\s+['"]([^'"]+)['"]/g, (match, imports, modulePath) => {
            const importList = imports.split(',').map(imp => {
                const trimmed = imp.trim()
                // 跳过类型导入
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
function compileTemplatePart(descriptor, id, bindingMetadata, compiled) {
    const templateResult = SFCCompiler.compileTemplate({
        source: descriptor.template?.content || '',
        filename: descriptor.filename,
        id,
        scoped: descriptor.styles.some(s => s.scoped),
        slotted: descriptor.slotted,
        isProd: false,
        compilerOptions: {
            bindingMetadata
        }
    })

    if (templateResult.errors.length) {
        compiled.errors = templateResult.errors
        return null
    }

    const fnName = `render`
    let transformedCode = templateResult.code.replace(
        /\nexport (function|const) (render|ssrRender)/,
        `$1 ${fnName}`
    )

    // 转换模板中的导入语句
    const isTypeScript = descriptor.script?.lang === 'ts' || descriptor.scriptSetup?.lang === 'ts'
    transformedCode = transformImportsToAwaitThen(transformedCode, isTypeScript)

    return `\n${transformedCode}\n${COMP_IDENTIFIER}.${fnName} = ${fnName}`
}

/**
 * 编译样式部分 - 修复作用域 CSS
 */
function compileStylesPart(descriptor, id, filename, compiled) {
    const styleResults = []

    for (const style of descriptor.styles) {
        try {
            // 修复作用域 CSS：使用正确的参数
            const styleResult = SFCCompiler.compileStyle({
                source: style.content,
                filename,
                id: style.scoped ? `data-v-${id}` : undefined, // 只在作用域样式时传递 id
                scoped: style.scoped,
                modules: !!style.module, // 处理模块化样式
                trim: true
            })

            if (styleResult.errors && styleResult.errors.length > 0) {
                const filteredErrors = styleResult.errors.filter(error =>
                    !error.message.includes('pathToFileURL') &&
                    !error.message.includes('PathToFileURL')
                )
                if (filteredErrors.length > 0) {
                    compiled.errors.push(...filteredErrors.map(e => e.message))
                }
            }

            // 确保代码被正确添加
            if (styleResult.code) {
                styleResults.push(styleResult.code)
            } else if (style.content.trim()) {
                // 如果编译失败但有内容，使用原始内容作为回退
                styleResults.push(style.content)
            }
        } catch (error) {
            // 编译失败时使用原始样式内容作为回退
            if (style.content.trim()) {
                styleResults.push(style.content)
            }
            compiled.errors.push(`Style compilation error: ${error.message}`)
        }
    }

    return styleResults.length > 0
        ? styleResults.join('\n').trim()
        : ''
}

/**
 * 检查代码中是否包含 Vue 组合式函数（带缓存）
 */
function hasVueCompositionFunctions(content) {
    const cacheKey = content.substring(0, 100)
    if (compositionFunctionCache.has(cacheKey)) {
        return compositionFunctionCache.get(cacheKey)
    }

    const hasFunctions = compositionFunctionPatterns.some(pattern => pattern.test(content))
    compositionFunctionCache.set(cacheKey, hasFunctions)

    if (compositionFunctionCache.size > 100) {
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
    hasScriptSetup
}