import iconDocument from './icons/langchain4j.svg?raw'
// icons/langchain4j.svg 是完整 SVG 文档（可直接预览）；注入时取其中的 <symbol> 片段
const iconSymbol = iconDocument.replace(/^[\s\S]*?(<symbol[\s\S]*<\/symbol>)[\s\S]*$/, '$1')
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import createDatasourceService from './service/magic-langchain4j-datasource.js'
import MagicLangchain4jInfo from './components/magic-langchain4j-chat-model-info.vue'

const ICON_ID = 'magic-langchain4j'

// 手动注入 SVG icon：icons/langchain4j.svg 是 LangChain4j 官方 logo（docs.langchain4j.dev/img/logo.svg）
// 的像素化 <symbol>（32x32 / 16 色 / 约 15KB）。vite 的 ?raw 在构建期内联为字符串，
// 规避 vite-plugin-svg-icons 在 IIFE 库模式下失效的问题
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
        datasources: [{
            // 完整 datasource type，与后端 Langchain4jModelTypes.CHAT_MODEL 一致
            type: 'langchain4j-chat-model',
            icon: '#' + ICON_ID,
            title: i18n.format('langchain4j.type.chat-model'),
            name: i18n.format('langchain4j.type.chat-model'),
            service: createDatasourceService(opt.request, i18n.format, opt.modal, 'chat-model'),
            component: MagicLangchain4jInfo
        }]
    }
}
