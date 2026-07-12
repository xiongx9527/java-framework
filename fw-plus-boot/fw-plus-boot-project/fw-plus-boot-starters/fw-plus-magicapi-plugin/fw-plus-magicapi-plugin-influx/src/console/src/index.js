import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicInfluxDataSource from './service/magic-influx-datasource.js'
import MagicInfluxDatasourceInfo from './components/magic-influx-datasource-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 128 128" id="magic-influx-influx"><path d="M76.32 16.27H51.68c-1.29 0-2.33 1.05-2.33 2.33v99.96c0 1.29 1.04 2.33 2.33 2.33h24.64c1.29 0 2.33-1.04 2.33-2.33V18.6c0-1.28-1.04-2.33-2.33-2.33z" fill="#40C0E7"></path></symbol>'
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
            type: 'influx-datasource',
            icon: '#magic-influx-influx',
            title: i18n.format('influx.datasource.title'),
            name: i18n.format('influx.datasource.name'),
            service: MagicInfluxDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicInfluxDatasourceInfo
        }]
    }
}