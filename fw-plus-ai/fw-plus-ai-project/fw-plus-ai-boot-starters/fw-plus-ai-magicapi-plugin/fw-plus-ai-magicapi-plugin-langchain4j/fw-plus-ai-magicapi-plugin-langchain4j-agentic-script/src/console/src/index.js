import iconDocument from './icons/langchain4j.svg?raw'
// icons/langchain4j.svg 是完整 SVG 文档（可直接预览）；注入时取其中的 <symbol> 片段
const iconSymbol = iconDocument.replace(/^[\s\S]*?(<symbol[\s\S]*<\/symbol>)[\s\S]*$/, '$1')
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import createService from './service/magic-langchain4j-agentic-script.js'
import MagicLangchain4jAgenticScriptInfo from './components/magic-langchain4j-agentic-script-info.vue'

const ICON_ID = 'magic-langchain4j'

// 手动注入 SVG icon（与其它 langchain4j 插件共用图标集）
if (typeof window !== 'undefined') {
    const svgDom = document.getElementById('__svg__icons__dom__')
    if (svgDom) {
        svgDom.innerHTML += iconSymbol
    } else {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
        svg.style.position = 'absolute'
        svg.style.width = '0'
        svg.style.height = '0'
        svg.id = '__svg__icons__dom__'
        svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
        svg.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        svg.innerHTML = iconSymbol
        document.body.insertBefore(svg, document.body.firstChild)
    }
}

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        // 左侧资源：路径型 .ms 资源，与后端 Langchain4jAgenticScriptMagicResourceStorage.folder() 一致
        resource: [{
            type: 'langchain4j-agentic-script',
            icon: '#' + ICON_ID,
            title: i18n.format('langchain4j.type.agentic-script'),
            service: createService(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        // 底部工具条：形态/描述/输入变量/输出 key/测试参数编辑（脚本走 magic 原生 .ms 编辑器）
        toolbars: [{
            type: 'langchain4j-agentic-script',
            title: i18n.format('langchain4j.form.title'),
            icon: '#' + ICON_ID,
            order: 315,
            component: MagicLangchain4jAgenticScriptInfo
        }]
    }
}
