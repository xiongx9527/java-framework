import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicPulsarSubscribe from './service/magic-pulsar-subscribe.js'
import MagicPulsarDataSource from './service/magic-pulsar-datasource.js'
import MagicPulsarDatasourceInfo from './components/magic-pulsar-datasource-info.vue'
import MagicPulsarSubscribeInfo from './components/magic-pulsar-subscribe-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol class="icon" viewBox="0 0 1024 1024" id="magic-pulsar-pulsar"><path d="M1185.553067 271.428267h-289.6896C817.493333 118.6816 734.651733 0 621.909333 0c-146.773333 0-215.722667 130.218667-282.385066 257.058133l-5.802667 11.127467H0v75.332267h194.389333c68.608 0 82.602667 23.517867 82.602667 23.517866-53.828267 84.992-118.5792 169.0624-226.5088 169.0624H0v78.984534h50.926933c209.169067 0 300.475733-172.578133 373.828267-311.125334 67.242667-127.6928 114.141867-204.970667 197.290667-204.970666 86.357333 0 177.083733 191.044267 256.9216 359.697066 11.605333 24.4736 23.552 49.5616 35.566933 74.478934-306.312533 9.8304-452.266667 82.773333-571.016533 142.336-86.869333 43.485867-155.477333 97.416533-268.3904 97.416533H0v79.121067h74.410667c136.021333 0 220.842667-42.666667 311.057066-87.995734 124.928-62.498133 266.581333-133.3248 600.576-133.3248h193.024V551.594667h-109.021866c-9.0112 0.682667-17.988267-0.682667-26.282667-4.232534a57.2416 57.2416 0 0 1-21.742933-15.633066 3481.6 3481.6 0 0 1-56.0128-116.701867l-25.326934-52.6336s18.090667-19.729067 68.1984-19.729067h176.674134V271.428267z"/></symbol>'
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
        resource: [{
            type: 'pulsar-subscribe',
            icon: '#magic-pulsar-pulsar',
            title: 'pulsar.subscribe.name',
            service: MagicPulsarSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'pulsar-datasource',
            icon: '#magic-pulsar-pulsar',
            title: i18n.format('pulsar.datasource.title'),
            name: i18n.format('pulsar.datasource.name'),
            service: MagicPulsarDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicPulsarDatasourceInfo
        }],
        toolbars: [{
            type: 'pulsar-subscribe',
            title: 'pulsar.subscribe.title',
            icon: '#magic-pulsar-pulsar',
            component: MagicPulsarSubscribeInfo
        }]
    }
}