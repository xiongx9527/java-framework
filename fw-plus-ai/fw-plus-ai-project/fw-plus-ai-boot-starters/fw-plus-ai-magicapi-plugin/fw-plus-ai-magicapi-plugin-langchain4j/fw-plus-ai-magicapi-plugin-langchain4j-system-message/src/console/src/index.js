import iconDocument from './icons/langchain4j.svg?raw'
// icons/langchain4j.svg 是完整 SVG 文档（可直接预览）；注入时取其中的 <symbol> 片段
const iconSymbol = iconDocument.replace(/^[\s\S]*?(<symbol[\s\S]*<\/symbol>)[\s\S]*$/, '$1')
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import createService from './service/magic-langchain4j-system-message.js'
import MagicLangchain4jSystemMessageInfo from './components/magic-langchain4j-system-message-info.vue'

const ICON_ID = 'magic-langchain4j'

// 手动注入 SVG icon，规避 vite-plugin-svg-icons 在 IIFE 库模式下失效的问题
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
        // 左侧资源：路径型 .ms 资源，与后端 Langchain4jSystemMessageMagicResourceStorage.folder() 一致
        resource: [{
            type: 'langchain4j-system-message',
            icon: '#' + ICON_ID,
            title: i18n.format('langchain4j.type.system-message'),
            service: createService(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        // 底部工具条：名称/路径编辑
        toolbars: [{
            type: 'langchain4j-system-message',
            title: i18n.format('langchain4j.form.title'),
            icon: '#' + ICON_ID,
            order: 310,
            component: MagicLangchain4jSystemMessageInfo
        }]
    }
}
