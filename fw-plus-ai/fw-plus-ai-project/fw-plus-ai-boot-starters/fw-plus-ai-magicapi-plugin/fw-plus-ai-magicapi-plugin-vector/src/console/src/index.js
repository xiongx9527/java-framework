import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicVectorDataSource from './service/magic-vector-datasource.js'
import MagicVectorDatasourceInfo from './components/magic-vector-datasource-info.vue'

if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 24 24" id="magic-vector-vector"><path d="M5 5a2 2 0 1 1 2.83 1.82l2.35 3.52a2.03 2.03 0 0 1 1.64 0l2.35-3.52A2 2 0 1 1 16 7l-2.35 3.52a2 2 0 0 1 0 2.96L16 17a2 2 0 1 1-1.83 1.18l-2.35-3.52a2.03 2.03 0 0 1-1.64 0l-2.35 3.52A2 2 0 1 1 6 17l2.35-3.52a2 2 0 0 1 0-2.96L6 7a2 2 0 0 1-1-2Z"/></symbol>'
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
        name: 'vector',
        statusBarItems: [{
            icon: '#magic-vector-vector',
            title: 'LangChain4j Vector',
            displayKey: 'vector'
        }],
        datasources: [{
            type: 'vector-datasource',
            icon: '#magic-vector-vector',
            title: i18n.format('vector.datasource.title'),
            name: i18n.format('vector.datasource.name'),
            service: MagicVectorDataSource(opt.request, i18n.format, opt.modal),
            component: MagicVectorDatasourceInfo
        }]
    }
}
