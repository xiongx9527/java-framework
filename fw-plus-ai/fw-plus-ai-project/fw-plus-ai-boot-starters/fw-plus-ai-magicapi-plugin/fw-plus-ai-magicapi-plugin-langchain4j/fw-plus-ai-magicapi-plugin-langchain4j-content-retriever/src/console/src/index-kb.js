import iconDocument from './icons/langchain4j.svg?raw'
// icons/langchain4j.svg 是完整 SVG 文档（可直接预览）；注入时取其中的 <symbol> 片段
const iconSymbol = iconDocument.replace(/^[\s\S]*?(<symbol[\s\S]*<\/symbol>)[\s\S]*$/, '$1')
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import { bindKbRequest } from './service/kb-service.js'
import MagicKbBrowser from './components/magic-kb-browser.vue'

const ICON_ID = 'magic-langchain4j'

// 手动注入 SVG icon：与主产物共用同一 icon id（两份产物由后端按装配条件选择性加载，不会重复注入）
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

// 知识库管理页插件入口：仅在后端 KnowledgeConfiguration 装配（SqlSessionFactory + 开关）时
// 才会被控制台加载并执行——toolbars 入口随知识库启动而激活（不启动，不激活）
export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    bindKbRequest(opt.request)
    return {
        toolbars: [{
            // 顶部工具栏知识库管理页（数据集/文档/分段/召回测试四视图）
            id: 'kb-browser',
            title: i18n.format('kb.dataset.title'),
            icon: '#' + ICON_ID,
            order: 210,
            component: MagicKbBrowser,
            adminOnly: true,
            allowClose: true,
            fullscreen: true
        }]
    }
}
