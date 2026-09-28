import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import createDatasourceService from './service/magic-langchain4j-datasource.js'
import MagicLangchain4jInfo from './components/magic-langchain4j-tokenizer-info.vue'

const ICON_ID = 'magic-langchain4j'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 1024 1024" id="magic-langchain4j">'
        + '<path d="M512 64a96 96 0 0 0-96 96v96h-96a192 192 0 0 0-192 192v96h128v-96a64 64 0 0 1 64-64h288a64 64 0 0 1 64 64v96h128v-96a192 192 0 0 0-192-192h-96v-96a96 96 0 0 0 0-192z" fill="#3fb950"/>'
        + '<path d="M160 576a96 96 0 0 0-96 96v160a96 96 0 0 0 96 96h96a96 96 0 0 0 96-96v-160a96 96 0 0 0-96-96h-96z m608 0a96 96 0 0 0-96 96v160a96 96 0 0 0 96 96h96a96 96 0 0 0 96-96v-160a96 96 0 0 0-96-96h-96z" fill="#3fb950"/>'
        + '</symbol>'
    const svgDom = document.getElementById('__svg__icons__dom__')
    if (svgDom) {
        svgDom.innerHTML += svgBody
    } else {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
        svg.style.position = 'absolute'
        svg.style.width = '0'
        svg.style.height = '0'
        svg.id = '__svg__icons__dom__'
        svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
        svg.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        svg.innerHTML = svgBody
        document.body.insertBefore(svg, document.body.firstChild)
    }
}

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        datasources: [{
            // 完整 datasource type，与后端 Langchain4jModelTypes.TOKENIZER 一致
            type: 'langchain4j-tokenizer',
            icon: '#' + ICON_ID,
            title: i18n.format('langchain4j.type.tokenizer'),
            name: i18n.format('langchain4j.type.tokenizer'),
            service: createDatasourceService(opt.request, i18n.format, opt.modal, 'tokenizer'),
            component: MagicLangchain4jInfo
        }]
    }
}
