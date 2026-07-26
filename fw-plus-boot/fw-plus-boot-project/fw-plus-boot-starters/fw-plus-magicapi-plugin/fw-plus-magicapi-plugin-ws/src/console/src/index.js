import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicWsSubscribe from './service/magic-ws-subscribe.js'
import MagicWsDataSource from './service/magic-ws-datasource.js'
import MagicWsDatasourceInfo from './components/magic-ws-datasource-info.vue'
import MagicWsSubscribeInfo from './components/magic-ws-subscribe-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    let loadSvg = function() {
        var body = document.body
        var svgDom = document.getElementById('__svg__icons__dom__1717728641709__ws')
        if (!svgDom) {
            svgDom = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
            svgDom.style.position = 'absolute'
            svgDom.style.width = '0'
            svgDom.style.height = '0'
            svgDom.id = '__svg__icons__dom__1717728641709__ws'
            svgDom.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
            svgDom.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        }
        svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 222 97" id="magic-ws-ws"><path d="M221.875 69.2188C221.875 76.0938 219.427 81.9271 214.531 86.7188C207.448 93.4896 197.292 96.875 184.062 96.875C172.708 96.875 163.021 94.2188 155 88.9062C155 90.6771 154.896 92.0833 154.688 93.125C154.167 94.7917 152.5 95.625 149.688 95.625V62.5H155.781C156.198 68.0208 157.344 72.9167 159.219 77.1875C163.698 87.2917 171.458 92.3438 182.5 92.3438C185.417 92.3438 188.073 91.9271 190.469 91.0938C197.135 88.5938 200.469 83.5938 200.469 76.0938C200.469 73.9062 200 71.7708 199.062 69.6875C196.875 65.3125 191.302 60.7812 182.344 56.0938C172.344 50.9896 166.719 48.0208 165.469 47.1875C155.573 41.0417 150.625 33.6458 150.625 25C150.625 19.1667 152.656 14.1146 156.719 9.84375C163.073 3.28125 172.552 0 185.156 0C194.74 0 203.854 2.03125 212.5 6.09375C212.5 5.15625 212.552 4.32292 212.656 3.59375C213.177 1.61458 214.896 0.625 217.812 0.625V28.9062H211.719C211.615 23.2812 210.26 18.4375 207.656 14.375C203.385 7.91667 196.979 4.6875 188.438 4.6875C186.25 4.6875 184.167 5 182.188 5.625C176.25 7.60417 173.281 11.6667 173.281 17.8125C173.281 20.2083 173.802 22.5 174.844 24.6875C177.031 29.1667 182.5 33.6979 191.25 38.2812C200.833 43.3854 206.302 46.3021 207.656 47.0312C217.135 53.1771 221.875 60.5729 221.875 69.2188Z" fill="#6E6E6E"/><path d="M103.438 95.3125H100.312C98.5417 95.3125 97.0833 95 95.9375 94.375C94.0625 93.3333 91.875 89.6354 89.375 83.2812L72.5 39.375L51.875 95.3125H48.75C46.9792 95.3125 45.5208 95 44.375 94.375C42.5 93.3333 40.3125 89.6354 37.8125 83.2812L7.8125 5.78125H0V1.5625H42.3438V5.78125H29.8438L54.375 74.0625L69.6875 32.3438L57.8125 1.5625H94.0625V5.78125L81.5625 5.625L105.781 72.6562L127.812 5.78125H115.312V1.5625H141.094V5.78125H133.281L103.438 95.3125Z" fill="#6E6E6E"/></symbol>'
        body.insertBefore(svgDom, body.firstChild)
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadSvg)
    } else {
        loadSvg()
    }
}

const openWs = () => {
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
    const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
    if (baseURL.startsWith('http')) {
        link = baseURL
    } else if (baseURL.startsWith('./')) {
        link = window.location.pathname.replace(/\/index\.html$/, '')
    }
    const druidUrl = link + '/ws/index.html'
    window.open(druidUrl, '_blank')
}

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        statusBarItems: [{
            icon: '#magic-ws-ws',
            title: 'Websocket client',
            displayKey: 'ws',
            order: 900,
            onClick: () => openWs()
        }],
        resource: [{
            type: 'ws-subscribe',
            icon: '#magic-ws-ws',
            title: 'ws.subscribe.name',
            service: MagicWsSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'ws-datasource',
            icon: '#magic-ws-ws',
            title: i18n.format('ws.datasource.title'),
            name: i18n.format('ws.datasource.name'),
            service: MagicWsDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicWsDatasourceInfo
        }],
        toolbars: [{
            type: 'ws-subscribe',
            title: 'ws.subscribe.title',
            icon: '#magic-ws-ws',
            order: 1400,
            component: MagicWsSubscribeInfo
        }]
    }
}
