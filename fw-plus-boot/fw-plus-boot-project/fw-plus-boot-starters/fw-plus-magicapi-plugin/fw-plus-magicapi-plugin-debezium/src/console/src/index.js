import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicDebeziumDataSource from './service/magic-debezium-datasource.js'
import MagicDebeziumDatasourceInfo from './components/magic-debezium-datasource-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 160 160" id="magic-debezium-debezium"><path d="M20.86 93.8a16.64 16.64 0 0 1 16.62 16.44v20.71H16a16.628 16.628 0 0 1-16-16.5V93.8h20.86Zm-.27-46.89a16.65 16.65 0 0 1 16.57 16.63v3.9A26.39 26.39 0 0 0 63.53 93.8h3.9A16.64 16.64 0 0 1 84 110.19v20.75H63.54a16.65 16.65 0 0 1-16.63-16.63v-3.89a26.39 26.39 0 0 0-26.36-26.36h-3.91A16.65 16.65 0 0 1 0 67.44V46.91h20.59ZM20.53 0a16.651 16.651 0 0 1 16.63 16.62v3.9a26.39 26.39 0 0 0 26.35 26.37h3.9A16.65 16.65 0 0 1 84 63.51v3.89a26.39 26.39 0 0 0 26.41 26.36h3.91a16.649 16.649 0 0 1 16.63 16.63v20.53h-20.53a16.647 16.647 0 0 1-16.63-16.63v-3.93A26.39 26.39 0 0 0 67.43 84h-3.9A16.651 16.651 0 0 1 46.9 67.4v-3.89a26.39 26.39 0 0 0-26.36-26.36h-3.91A16.65 16.65 0 0 1 0 20.52V0h20.53Zm46.88 0A16.65 16.65 0 0 1 84 16.62v3.9a26.391 26.391 0 0 0 26.4 26.36h3.91a16.65 16.65 0 0 1 16.629 16.63V84H110.36a16.65 16.65 0 0 1-16.57-16.6v-3.89a26.39 26.39 0 0 0-26.36-26.36h-3.91a16.64 16.64 0 0 1-16.62-16.4V0h20.51ZM115 0a16.632 16.632 0 0 1 15.94 16.49v20.65h-20.85A16.64 16.64 0 0 1 93.47 20.7V0H115Z"/></symbol>'
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
            icon: '#magic-debezium-debezium',
            title: 'Debezium',
            displayKey: 'debezium',
            order: 700,
            onClick: () => window.open('https://debezium.io/', '_blank')
        }],
        datasources: [{
            type: 'debezium-datasource',
            icon: '#magic-debezium-debezium',
            title: i18n.format('debezium.datasource.title'),
            name: i18n.format('debezium.datasource.name'),
            service: MagicDebeziumDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicDebeziumDatasourceInfo
        }]
    }
}
