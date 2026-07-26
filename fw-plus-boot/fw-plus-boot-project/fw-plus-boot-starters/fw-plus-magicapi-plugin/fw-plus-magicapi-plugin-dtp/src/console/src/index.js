import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicDtpDataSource from './service/magic-dtp-datasource.js'
import MagicDtpDatasourceInfo from './components/magic-dtp-datasource-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 24 24" id="magic-dtp-dtp"><path d="M22.6 5.5a9.91 9.91 0 0 0-3.88-4.04A11.18 11.18 0 0 0 13.08.04H.18l6.91 4h5.99c1.94 0 3.41.62 4.42 1.88C18.5 7.17 19 9 19 11.43v1.27c-.01 2.34-.53 4.14-1.55 5.4-1.02 1.26-2.5 1.89-4.42 1.89H6.86L0 23.95h13.13c2.09 0 3.97-.49 5.63-1.44 1.66-.95 2.95-2.3 3.87-4.02S24 14.79 24 12.56v-1.1c0-2.22-.47-4.2-1.4-5.96zM.13 3.96v16.1L14.07 12z"/></symbol>'
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
        statusBarItems: [{
            icon: '#magic-dtp-dtp',
            title: 'Dynamictp',
            displayKey: 'dtp',
            order: 600,
            onClick: () => window.open('https://dynamictp.cn/', '_blank')
        }],
        datasources: [{
            type: 'dtp-datasource',
            icon: '#magic-dtp-dtp',
            title: i18n.format('dtp.datasource.title'),
            name: i18n.format('dtp.datasource.name'),
            service: MagicDtpDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicDtpDatasourceInfo
        }]
    }
}
