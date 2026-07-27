import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicModelDataSource from './service/magic-model-datasource.js'
import MagicModelDatasourceInfo from './components/magic-model-datasource-info.vue'

if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 24 24" id="magic-model-model"><path d="M12 3a7 7 0 0 0-7 7v4a3 3 0 0 0 3 3h1v-6H7v-1a5 5 0 0 1 10 0v1h-2v6h1a3 3 0 0 0 3-3v-4a7 7 0 0 0-7-7Zm-3 16h6v2H9v-2Z"/></symbol>'
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
        name: 'model',
        statusBarItems: [{
            icon: '#magic-model-model',
            title: 'Model',
            displayKey: 'model'
        }],
        datasources: [{
            type: 'model-datasource',
            icon: '#magic-model-model',
            title: i18n.format('model.datasource.title'),
            name: i18n.format('model.datasource.name'),
            service: MagicModelDataSource(opt.request, i18n.format, opt.modal),
            component: MagicModelDatasourceInfo
        }]
    }
}
