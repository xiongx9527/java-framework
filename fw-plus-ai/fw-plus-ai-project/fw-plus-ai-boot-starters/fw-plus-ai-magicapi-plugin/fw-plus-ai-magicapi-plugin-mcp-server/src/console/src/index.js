import iconDocument from './icons/server.svg?raw'
// icons/server.svg 是完整 SVG 文档（可直接预览）；注入时取其中的 <symbol> 片段
const iconSymbol = iconDocument.replace(/^[\s\S]*?(<symbol[\s\S]*<\/symbol>)[\s\S]*$/, '$1')
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import createDatasourceService from './service/magic-mcp-server.js'
import MagicMcpServerInfo from './components/magic-mcp-server-info.vue'

const ICON_ID = 'magic-mcp-server'

// 手动注入 SVG icon：icons/server.svg 的 <symbol id="magic-mcp-server"> 与 datasource
// icon: '#' + ICON_ID 对应。vite 的 ?raw 在构建期内联为字符串，规避
// vite-plugin-svg-icons 在 IIFE 库模式下失效的问题（与 langchain4j 各类型一致）
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
        // 完整 datasource type，与后端 MagicMcpServerInfoMagicResourceStorage.folder() 一致
        datasources: [{
            type: 'mcp-server',
            icon: '#' + ICON_ID,
            title: i18n.format('mcp.server.name'),
            name: i18n.format('mcp.server.name'),
            service: createDatasourceService(opt.request, i18n.format, opt.modal),
            component: MagicMcpServerInfo
        }]
    }
}