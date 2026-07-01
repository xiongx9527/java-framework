import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicMongoDataSource from './service/magic-mongo-datasource.js'
import MagicMongoDatasourceInfo from './components/magic-mongo-datasource-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 120 240" id="magic-mongo-mongo"><path d="M82.3229 28.6444C71.5367 15.8469 62.2485 2.84945 60.351 0.149971C60.1512 -0.0499903 59.8515 -0.0499903 59.6518 0.149971C57.7542 2.84945 48.4661 15.8469 37.6798 28.6444C-54.9019 146.721 52.2613 226.406 52.2613 226.406L53.1601 227.006C53.959 239.303 55.9565 257 55.9565 257H59.9514H63.9463C63.9463 257 65.9438 239.403 66.7428 227.006L67.6416 226.306C67.7414 226.406 174.905 146.721 82.3229 28.6444ZM59.9514 224.606C59.9514 224.606 55.1576 220.507 53.8592 218.408V218.207L59.6518 89.6326C59.6518 89.2326 60.2511 89.2326 60.2511 89.6326L66.0436 218.207V218.408C64.7453 220.507 59.9514 224.606 59.9514 224.606Z"/></symbol>'
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
            type: 'mongo-datasource',
            icon: '#magic-mongo-mongo',
            title: i18n.format('mongo.datasource.title'),
            name: i18n.format('mongo.datasource.name'),
            service: MagicMongoDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicMongoDatasourceInfo
        }]
    }
}