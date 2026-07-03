import * as SFCCompiler from '@vue/compiler-sfc'
import { babelParse } from "@vue/compiler-sfc"

// 常量定义
const COMP_IDENTIFIER = `__sfc__`
const VUE_COMPOSITION_FUNCTIONS = ['defineExpose', 'defineProps', 'defineEmits', 'defineSlots', 'defineOptions', 'defineModel']
const SCRIPT_SETUP_REGEX = /<script\s+setup[^>]*>/

// 预编译正则表达式 - 使用缓存
const compositionFunctionPatterns = VUE_COMPOSITION_FUNCTIONS.map(
    fn => new RegExp(`\\b${fn}\\s*[<(]`)
)

// 错误消息常量
const ERROR_MESSAGES = {
    COMPOSITION_FUNCTIONS_REQUIRE_SCRIPT_SETUP: 'defineProps、defineExpose、defineEmits、defineSlots、defineOptions、defineModel需要在<script setup>下使用',
    UNSUPPORTED_PREPROCESSORS: 'lang="x" pre-processors are not supported in the in-browser playground.',
    UNSUPPORTED_STYLE_MODULE: '<style module> is not supported in the playground.',
    JAVASCRIPT_PARSE_ERROR: 'JavaScript解析错误: '
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
        const ast = babelParse(jsCode, { sourceType: 'module' })
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
            }
        }

        replacements.sort((a, b) => b.start - a.start).forEach(replacement => {
            jsCode = replaceCodeSegment(jsCode, replacement.start, replacement.end, replacement.code)
        })

        // 包装成异步函数格式
        jsCode = wrapAsAsyncFunction(jsCode)

        return {
            compileJs: jsCode,
            compiledCss: compiled.css
        }
    } catch (error) {
        const enhancedError = new Error(`${ERROR_MESSAGES.JAVASCRIPT_PARSE_ERROR}${error.message}`)
        enhancedError.originalError = error
        enhancedError.sourceCode = jsCode
        throw enhancedError
    }
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
    ].some(lang => lang && lang !== 'js')

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
 * 编译脚本部分
 */
function compileScriptPart(descriptor, id, compiled) {
    if (!descriptor.script && !descriptor.scriptSetup) {
        return [`\nconst ${COMP_IDENTIFIER} = {}`, undefined]
    }

    try {
        const compiledScript = SFCCompiler.compileScript(descriptor, {
            id,
            refSugar: true,
            inlineTemplate: true
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
        scriptContent = transformImportsToAwaitThen(scriptContent)

        code += `\n${scriptContent}`
        return [code, compiledScript.bindings]

    } catch (error) {
        compiled.errors = [error.message]
        return null
    }
}

/**
 * 转换导入语句为 await import().then() 格式
 */
function transformImportsToAwaitThen(scriptContent) {
    try {
        const ast = babelParse(scriptContent, { sourceType: 'module' })
        let transformedCode = scriptContent

        const importsToReplace = []

        for (let i = ast.program.body.length - 1; i >= 0; i--) {
            const node = ast.program.body[i]
            if (node.type === 'ImportDeclaration') {
                const importStatements = node.specifiers.map(specifier => {
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
        }

        // 应用替换
        importsToReplace.sort((a, b) => b.start - a.start).forEach(replacement => {
            transformedCode = replaceCodeSegment(transformedCode, replacement.start, replacement.end, replacement.code)
        })

        return transformedCode
    } catch (error) {
        console.warn('AST parsing failed, using fallback string replacement:', error.message)
        return fallbackTransformImports(scriptContent)
    }
}

/**
 * 回退的导入转换方法（字符串替换）
 */
function fallbackTransformImports(scriptContent) {
    return scriptContent
        .replace(/import\s+(\w+)\s+from\s+['"]([^'"]+)['"]/g, 'const $1 = await import(\'$2\').then(m => m.default);')
        .replace(/import\s+\*\s+as\s+(\w+)\s+from\s+['"]([^'"]+)['"]/g, 'const $1 = await import(\'$2\');')
        .replace(/import\s+{([^}]+)}\s+from\s+['"]([^'"]+)['"]/g, (match, imports, modulePath) => {
            const importList = imports.split(',').map(imp => {
                const trimmed = imp.trim()
                const parts = trimmed.split(/\s+as\s+/)
                if (parts.length === 2) {
                    return `const ${parts[1]} = await import('${modulePath}').then(m => m.${parts[0]});`
                } else {
                    return `const ${trimmed} = await import('${modulePath}').then(m => m.${trimmed});`
                }
            }).join('\n')
            return importList
        })
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
    transformedCode = transformImportsToAwaitThen(transformedCode)

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
